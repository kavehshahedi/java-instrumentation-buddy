package jib.services;

import java.io.FileWriter;
import java.io.IOException;

import jib.models.Configuration;
import jib.services.external.TraceConverter;
import jib.utils.Time;

public class AgentLifecycleManager {
    private AgentLifecycleManager() {}

    public static void trackLifetime(Configuration config) {
        long startTime = Time.getTimeNanoSeconds();

        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            long endTime = Time.getTimeNanoSeconds();
            String info = createAgentInfoJson(startTime, endTime);
            writeAgentInfoToFile(info, config.getLogging().getFile());
            Logger.close();

            // Convert and save the log file to a JSON file
            TraceConverter converter = new TraceConverter(config.getLogging().getFile(), config.getLogging().getFile().replace(".log", ".log.json"));
            converter.convert();
        }));
    }

    private static String createAgentInfoJson(long startTime, long endTime) {
        return String.format("{" +
                        "\"start_time\": %d," +
                        "\"end_time\": %d," +
                        "\"log_time_difference\": %d," +
                        "\"method_signature_hash\": %s" +
                        "}",
                startTime, endTime, Time.getOptimizedTimeOffset(),
                Logger.getMethodSignatureHashJson());
    }

    private static void writeAgentInfoToFile(String info, String logFile) {
        String fileName = logFile.replace(".log", ".json");
        try (FileWriter fileWriter = new FileWriter(fileName)) {
            fileWriter.write(info);
        } catch (IOException e) {
            System.err.println("Error writing agent info: " + e.getMessage());
        }
    }
}