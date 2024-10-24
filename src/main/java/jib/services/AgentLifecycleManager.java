package jib.services;

import java.io.IOException;

import jib.models.AgentInfo;
import jib.models.Configuration;
import jib.services.external.TraceConverter;
import jib.utils.Time;

public class AgentLifecycleManager {
    
    private AgentLifecycleManager() {}

    public static void trackLifetime(Configuration config) {
        long startTime = Time.getTimeNanoSeconds();
        registerShutdownHook(startTime, config);
    }

    private static void registerShutdownHook(long startTime, Configuration config) {
        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            long endTime = Time.getTimeNanoSeconds();
            
            saveAgentData(startTime, endTime, config);

            convertLogFileToJson(config);

            Logger.close();
        }));
    }

    private static void saveAgentData(long startTime, long endTime, Configuration config) {
        AgentInfo agentInfo = new AgentInfo(
            startTime, 
            endTime, 
            config.getLogging().isOptimizeTimestamp() ? Time.getOptimizedTimeOffset() : 0, 
            Logger.getMethodSignatureHashJson()
        );

        try {
            String jsonFile = config.getLogging().getFile().replace(".log", ".json");
            JsonFileHandler.writeJsonObjectToFile(jsonFile, agentInfo);
        } catch (IOException e) {
            System.err.println("Error writing agent info: " + e.getMessage());
        }
    }

    private static void convertLogFileToJson(Configuration config) {
        if (!config.getMisc().isConvertToJson()) {
            return;
        }
        
        String logFile = config.getLogging().getFile();
        String jsonLogFile = logFile.replace(".log", ".log.json");
        new TraceConverter(config, logFile, jsonLogFile).convert();
    }
}