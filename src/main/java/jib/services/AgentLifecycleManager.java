package jib.services;

import java.io.IOException;

import jib.models.AgentInfo;
import jib.models.Configuration;
import jib.services.external.TraceConverter;

public class AgentLifecycleManager {

    private AgentLifecycleManager() {
    }

    public static void trackLifetime(Configuration config) {
        long startTime = FastAsyncLogger.getStartTime();
        registerShutdownHook(startTime, config);
    }

    private static void registerShutdownHook(long startTime, Configuration config) {
        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            long endTime = System.nanoTime() + getTimeOffset();

            FastAsyncLogger.close();

            saveAgentData(startTime, endTime, config);
            convertLogFileToJson(config);
        }));
    }

    private static long getTimeOffset() {
        long currentTimeMillis = System.currentTimeMillis();
        long currentNanoTime = System.nanoTime();
        return (currentTimeMillis * 1_000_000) - currentNanoTime;
    }

    private static void saveAgentData(long startTime, long endTime, Configuration config) {
        if (config.getLogging().isUseHash()) {
            AgentInfo agentInfo = new AgentInfo(
                    startTime,
                    endTime,
                    config.getLogging().isOptimizeTimestamp() ? getOptimizedTimeOffset() : 0,
                    FastAsyncLogger.getMethodSignatureHashJson());

            try {
                String jsonFile = config.getLogging().getFile().replace(".log", ".json");
                JsonFileHandler.writeJsonObjectToFile(jsonFile, agentInfo);
            } catch (IOException e) {
                System.err.println("Error writing agent info: " + e.getMessage());
            }
        }
    }

    private static long getOptimizedTimeOffset() {
        return Long.parseLong(String.valueOf(System.currentTimeMillis()).substring(0, 4) + "000000000000000");
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