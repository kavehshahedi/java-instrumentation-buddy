package jib.models;

import jib.utils.IJsonSerializable;
import jib.services.JsonFileHandler;

public class TraceEntry implements IJsonSerializable {
    private final long timestamp;
    private final String phase;
    private final String name;

    public TraceEntry(long timestamp, String phase, String name) {
        this.timestamp = timestamp;
        this.phase = phase;
        this.name = name;
    }

    @Override
    public String toJson(int indentLevel) {
        String indent = JsonFileHandler.getIndent(indentLevel);
        StringBuilder sb = new StringBuilder();
        sb.append(indent).append("{\n");
        
        String innerIndent = JsonFileHandler.getIndent(indentLevel + 1);
        sb.append(innerIndent).append("\"ts\": ").append(toMicroseconds(timestamp)).append(",\n")
          .append(innerIndent).append("\"ph\": \"").append(JsonFileHandler.escapeJsonString(phase)).append("\",\n")
          .append(innerIndent).append("\"name\": \"").append(JsonFileHandler.escapeJsonString(name)).append("\"\n")
          .append(indent).append("}");
        
        return sb.toString();
    }

    private String toMicroseconds(long timestamp) {
        long microseconds = timestamp / 1000;
        long nanosRemainder = timestamp % 1000;
        return microseconds + "." + String.format("%03d", nanosRemainder);
    }
}