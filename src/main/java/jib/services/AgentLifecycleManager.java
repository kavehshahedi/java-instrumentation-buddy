package jib.services;

import java.io.IOException;

import jib.models.AgentInfo;
import jib.models.Configuration;
import jib.services.external.TraceConverter;
import jib.utils.Time;

public class AgentLifecycleManager {
    private static Thread shutdownHook;

    private AgentLifecycleManager() {
    }

    public static void trackLifetime(Configuration config) {
        long startTime = FastAsyncLogger.getStartTime();
        registerShutdownHook(startTime, config);
    }

    public static void stopTracking() {
        if (Runtime.getRuntime().removeShutdownHook(shutdownHook)) {
            shutdownHook.run();
        }
    }

    private static void registerShutdownHook(long startTime, Configuration config) {
        shutdownHook = new Thread(() -> {
            long endTime = Time.getTimeNanoSeconds();

            if (FastAsyncLogger.close()) {
                saveAgentData(startTime, endTime, config);
                convertLogFileToJson(config);
            }
        });
        Runtime.getRuntime().addShutdownHook(shutdownHook);
    }

    private static void saveAgentData(long startTime, long endTime, Configuration config) {
        AgentInfo agentInfo = new AgentInfo(
                startTime,
                endTime,
                config.getLogging().isOptimizeTimestamp() ? Time.OPTIMIZED_OFFSET : 0,
                FastAsyncLogger.getDroppedEvents(),
                FastAsyncLogger.getMethodSignatureHashJson());

        try {
            JsonFileHandler.writeJsonObjectToFile(config.getLogging().getMetadataFile(), agentInfo);
        } catch (IOException e) {
            System.err.println("Error writing agent info: " + e.getMessage());
        }
    }

    private static void convertLogFileToJson(Configuration config) {
        if (!config.getMisc().isConvertToJson()) {
            return;
        }

        new TraceConverter(config, config.getLogging().getFile(), config.getLogging().getJsonFile()).convert();
    }
}