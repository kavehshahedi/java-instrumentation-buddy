package jib.services;

import java.io.IOException;
import java.io.Reader;
import java.lang.reflect.Method;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;

import jib.models.Configuration;

public class ConfigurationLoader {
    private ConfigurationLoader() {}

    public static Configuration load(String args) {
        try {
            Map<String, String> options = parseArguments(args);
            Properties properties = new Properties();
            String configPath = options.remove("config");
            if (configPath != null) {
                try (Reader reader = Files.newBufferedReader(Paths.get(configPath), StandardCharsets.UTF_8)) {
                    properties.load(reader);
                }
            }

            Configuration config = new Configuration();
            for (String key : properties.stringPropertyNames()) {
                set(config, key, properties.getProperty(key));
            }
            for (Map.Entry<String, String> option : options.entrySet()) {
                set(config, option.getKey(), option.getValue());
            }
            return config;
        } catch (IOException e) {
            System.err.println("Error reading configuration file, the agent is disabled: " + e);
        } catch (IllegalArgumentException e) {
            System.err.println("Invalid configuration, the agent is disabled: " + e.getMessage());
        }
        return null;
    }

    private static Map<String, String> parseArguments(String args) {
        Map<String, String> options = new LinkedHashMap<>();
        String lastKey = null;
        for (String option : split(args == null ? "" : args)) {
            int separator = option.indexOf('=');
            if (separator > 0) {
                lastKey = option.substring(0, separator).trim();
                options.put(lastKey, option.substring(separator + 1));
            } else if (lastKey != null) {
                options.put(lastKey, options.get(lastKey) + "," + option);
            } else {
                throw new IllegalArgumentException("expected key=value but got " + option);
            }
        }
        return options;
    }

    private static void set(Configuration config, String key, String value) {
        String[] path = key.split("\\.", -1);
        Object target = config;
        try {
            for (int i = 0; i < path.length - 1; i++) {
                Method getter = target.getClass().getMethod("get" + capitalize(path[i]));
                if (!getter.getReturnType().getName().startsWith(Configuration.class.getName() + "$")) {
                    throw new NoSuchMethodException();
                }
                target = getter.invoke(target);
            }
            for (Method setter : target.getClass().getMethods()) {
                if (setter.getName().equals("set" + capitalize(path[path.length - 1])) && setter.getParameterCount() == 1) {
                    Object converted = convert(setter.getParameterTypes()[0], key, value.trim());
                    if (converted != null) {
                        setter.invoke(target, converted);
                        return;
                    }
                }
            }
        } catch (ReflectiveOperationException e) {
            throw new IllegalArgumentException("unknown option " + key);
        }
        throw new IllegalArgumentException("unknown option " + key);
    }

    private static Object convert(Class<?> type, String key, String value) {
        if (type == String.class) {
            return value;
        }
        if (type == List.class) {
            return split(value);
        }
        if (type == boolean.class && (value.equalsIgnoreCase("true") || value.equalsIgnoreCase("false"))) {
            return Boolean.parseBoolean(value);
        }
        if (type == int.class && value.matches("-?\\d{1,9}")) {
            return Integer.parseInt(value);
        }
        if (type == boolean.class || type == int.class) {
            throw new IllegalArgumentException("invalid value for " + key + ": " + value);
        }
        return null;
    }

    private static List<String> split(String list) {
        List<String> items = new ArrayList<>();
        int depth = 0;
        int start = 0;
        for (int i = 0; i <= list.length(); i++) {
            char c = i < list.length() ? list.charAt(i) : ',';
            if (c == '(' || c == '<') {
                depth++;
            } else if (c == ')' || c == '>') {
                depth--;
            } else if (c == ',' && depth <= 0) {
                String item = list.substring(start, i).trim();
                if (!item.isEmpty()) {
                    items.add(item);
                }
                start = i + 1;
            }
        }
        return items;
    }

    private static String capitalize(String name) {
        return name.isEmpty() ? name : Character.toUpperCase(name.charAt(0)) + name.substring(1);
    }
}
