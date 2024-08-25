package jib.services;

import java.io.*;
import java.nio.file.Files;

import jib.models.Configuration;

import org.yaml.snakeyaml.Yaml;
import org.yaml.snakeyaml.constructor.Constructor;
import org.yaml.snakeyaml.LoaderOptions;

public class ConfigurationLoader {
    private ConfigurationLoader() {}

    public static Configuration load(String args) {
        if (args == null || args.isEmpty()) {
            return new Configuration();
        }

        String configPath = parseConfigPath(args);
        return loadFromFile(configPath);
    }

    private static String parseConfigPath(String args) {
        String[] options = args.split(",");
        for (String option : options) {
            if (option.startsWith("config=")) {
                String[] keyValue = option.split("=", 2);
                if (keyValue.length == 2) {
                    return keyValue[1];
                }
            }
        }
        return null;
    }

    private static Configuration loadFromFile(String filePath) {
        try (InputStreamReader reader = new InputStreamReader(Files.newInputStream(new File(filePath).toPath()))) {
            Yaml yaml = new Yaml(new Constructor(Configuration.class, new LoaderOptions()));
            return yaml.load(reader);
        } catch (IOException e) {
            System.err.println("Error reading configuration file: " + e.getMessage());
        } catch (Exception e) {
            System.err.println("Error parsing configuration file: " + e.getMessage());
        }
        return new Configuration();
    }
}