package jib.services.external;

import java.io.*;
import java.util.*;
import java.util.regex.*;

import jib.utils.Time;

public class TraceConverter {

    public static final Pattern REGEX = Pattern.compile("\\[(\\d+)\\] (S|E) ([\\w\\W\\s.]+)");
    public final String inputPath;
    public final String outputPath;
    public final int batchSize;

    public TraceConverter(String inputPath, String outputPath, int batchSize) {
        this.inputPath = inputPath;
        this.outputPath = outputPath;
        this.batchSize = batchSize;
    }

    public TraceConverter(String inputPath, String outputPath) {
        this(inputPath, outputPath, 1000); // Default batch size of 1000
    }

    public static class TraceEntry {
        final long timestamp;
        final String phase;
        final String name;

        TraceEntry(long timestamp, String phase, String name) {
            this.timestamp = timestamp + Time.getOptimizedTimeOffset();
            this.phase = phase;
            this.name = name;
        }

        String toJson(int indent) {
            StringBuilder sb = new StringBuilder();
            String indentStr = "";
            for (int i = 0; i < indent; i++) {
                indentStr += " ";
            }
            sb.append(indentStr).append("{\n");
            sb.append(indentStr).append("    \"ts\": ").append(timestamp).append(",\n");
            sb.append(indentStr).append("    \"ph\": \"").append(phase).append("\",\n");
            sb.append(indentStr).append("    \"name\": \"").append(escapeJsonString(name)).append("\"\n");
            sb.append(indentStr).append("}");
            return sb.toString();
        }

        public String escapeJsonString(String input) {
            StringBuilder sb = new StringBuilder();
            for (char c : input.toCharArray()) {
                switch (c) {
                    case '"':
                        sb.append("\\\"");
                        break;
                    case '\\':
                        sb.append("\\\\");
                        break;
                    case '\b':
                        sb.append("\\b");
                        break;
                    case '\f':
                        sb.append("\\f");
                        break;
                    case '\n':
                        sb.append("\\n");
                        break;
                    case '\r':
                        sb.append("\\r");
                        break;
                    case '\t':
                        sb.append("\\t");
                        break;
                    default:
                        if (c < ' ') {
                            sb.append(String.format("\\u%04x", (int) c));
                        } else {
                            sb.append(c);
                        }
                }
            }
            return sb.toString();
        }
    }

    public TraceEntry processLine(String line) {
        Matcher matcher = REGEX.matcher(line);
        if (matcher.matches()) {
            long timestamp = Long.parseLong(matcher.group(1));
            String phase = matcher.group(2).equals("S") ? "B" : "E";
            String name = matcher.group(3).trim();
            return new TraceEntry(timestamp, phase, name);
        }
        return null;
    }

    public void convert() {
        try (BufferedReader reader = new BufferedReader(new FileReader(inputPath));
             BufferedWriter writer = new BufferedWriter(new FileWriter(outputPath))) {
            
            writer.write("[\n");
            
            List<TraceEntry> batch = new ArrayList<>();
            boolean first = true;
            String line;
            
            while ((line = reader.readLine()) != null) {
                TraceEntry result = processLine(line);
                if (result != null) {
                    batch.add(result);
                    
                    if (batch.size() >= batchSize) {
                        writeJsonBatch(writer, batch, !first);
                        first = false;
                        batch.clear();
                    }
                }
            }
            
            // Write any remaining items in the batch
            if (!batch.isEmpty()) {
                writeJsonBatch(writer, batch, !first);
            }
            
            writer.write("\n]");
        } catch (IOException e) {
            System.err.println("Error converting trace file: " + e.getMessage());
        }
    }

    public void writeJsonBatch(BufferedWriter writer, List<TraceEntry> batch, boolean needsComma) 
            throws IOException {
        if (needsComma) {
            writer.write(",\n");
        }
        
        for (int i = 0; i < batch.size(); i++) {
            if (i > 0) {
                writer.write(",\n");
            }
            writer.write(batch.get(i).toJson(4));
        }
    }
}