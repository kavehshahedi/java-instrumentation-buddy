package jib.services;

import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.IOException;

import jib.utils.IJsonSerializable;

public class JsonFileHandler {
    private static final String INDENT = "    "; // 4 spaces

    public static void writeJsonObjectToFile(String filePath, IJsonSerializable object) throws IOException {
        try (BufferedWriter writer = new BufferedWriter(new FileWriter(filePath))) {
            writer.write(object.toJson(0));
        }
    }

    public static String getIndent(int level) {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < level; i++) {
            sb.append(INDENT);
        }
        return sb.toString();
    }
    
    public static String escapeJsonString(String input) {
        if (input == null) {
            return "null";
        }
        
        StringBuilder sb = new StringBuilder();
        for (char c : input.toCharArray()) {
            switch (c) {
                case '"' : sb.append("\\\"");
                    break;
                case '\\' : sb.append("\\\\");
                    break;
                case '\b' : sb.append("\\b");
                    break;
                case '\f' : sb.append("\\f");
                    break;
                case '\n' : sb.append("\\n");
                    break;
                case '\r' : sb.append("\\r");
                    break;
                case '\t' : sb.append("\\t");
                    break;
                default : 
                    if (c < ' ') {
                        sb.append(String.format("\\u%04x", (int) c));
                    } else {
                        sb.append(c);
                    }
                    break;
            }
        }
        return sb.toString();
    }
}