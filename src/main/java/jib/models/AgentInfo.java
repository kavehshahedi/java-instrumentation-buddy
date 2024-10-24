package jib.models;

import jib.utils.IJsonSerializable;
import jib.services.JsonFileHandler;

public class AgentInfo implements IJsonSerializable {
    private final long startTime;
    private final long endTime;
    private final long logTimeDifference;
    private final String methodSignatureHash;

    public AgentInfo(long startTime, long endTime, long logTimeDifference, String methodSignatureHash) {
        this.startTime = startTime;
        this.endTime = endTime;
        this.logTimeDifference = logTimeDifference;
        this.methodSignatureHash = methodSignatureHash;
    }

    @Override
    public String toJson(int indentLevel) {
        String indent = JsonFileHandler.getIndent(indentLevel);
        StringBuilder sb = new StringBuilder();
        sb.append(indent).append("{\n");
        
        String innerIndent = JsonFileHandler.getIndent(indentLevel + 1);
        sb.append(innerIndent).append("\"start_time\": ").append(startTime).append(",\n")
          .append(innerIndent).append("\"end_time\": ").append(endTime).append(",\n")
          .append(innerIndent).append("\"log_time_difference\": ").append(logTimeDifference).append(",\n")
          .append(innerIndent).append("\"method_signature_hash\": ").append(methodSignatureHash).append("\n")
          .append(indent).append("}");
        
        return sb.toString();
    }
}
