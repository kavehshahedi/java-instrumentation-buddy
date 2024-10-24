package jib.services;

import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import jib.utils.IJsonSerializable;

public class JsonFileHandler {
    private static final String INDENT = "    "; // 4 spaces
    
    public static void writeJsonArrayToFile(String filePath, List<? extends IJsonSerializable> items) throws IOException {
        try (BufferedWriter writer = new BufferedWriter(new FileWriter(filePath))) {
            writer.write("[\n");
            
            for (int i = 0; i < items.size(); i++) {
                writer.write(items.get(i).toJson(1));
                if (i < items.size() - 1) {
                    writer.write(",");
                }
                writer.write("\n");
            }
            
            writer.write("]");
        }
    }
    
    public static void writeJsonArrayInBatches(String filePath, Iterator<? extends IJsonSerializable> itemIterator, 
                                             int batchSize) throws IOException {
        try (BufferedWriter writer = new BufferedWriter(new FileWriter(filePath))) {
            writer.write("[\n");
            
            boolean first = true;
            List<IJsonSerializable> batch = new ArrayList<>();
            
            while (itemIterator.hasNext()) {
                batch.clear();
                while (batch.size() < batchSize && itemIterator.hasNext()) {
                    batch.add(itemIterator.next());
                }
                
                if (!batch.isEmpty()) {
                    if (!first) {
                        writer.write(",\n");
                    }
                    writeJsonBatch(writer, batch, 1); // 1 level of indentation
                    first = false;
                }
            }
            
            writer.write("\n]");
        }
    }
    
    public static void writeJsonObjectToFile(String filePath, IJsonSerializable object) throws IOException {
        try (BufferedWriter writer = new BufferedWriter(new FileWriter(filePath))) {
            writer.write(object.toJson(0)); 
        }
    }
    
    private static void writeJsonBatch(BufferedWriter writer, List<? extends IJsonSerializable> batch, 
                                     int indentLevel) throws IOException {
        for (int i = 0; i < batch.size(); i++) {
            writer.write(batch.get(i).toJson(indentLevel));
            if (i < batch.size() - 1) {
                writer.write(",\n");
            }
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