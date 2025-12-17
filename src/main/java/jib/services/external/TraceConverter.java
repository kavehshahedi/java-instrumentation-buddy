package jib.services.external;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import jib.models.Configuration;
import jib.models.TraceEntry;
import jib.services.JsonFileHandler;
import jib.utils.Time;

public class TraceConverter {

    private Configuration config;

    private static final Pattern REGEX = Pattern.compile("\\[(\\d+)\\] (S|E) \\[(\\d+)\\]\\[(\\d+)\\] \"([\\w\\W\\s.]+)\"");
    private final String inputPath;
    private final String outputPath;
    private final int batchSize;

    public TraceConverter(Configuration config, String inputPath, String outputPath, int batchSize) {
        this.config = config;
        this.inputPath = inputPath;
        this.outputPath = outputPath;
        this.batchSize = batchSize;
    }

    public TraceConverter(Configuration config, String inputPath, String outputPath) {
        this(config, inputPath, outputPath, 1000);
    }

    private TraceEntry processLine(String line) {
        Matcher matcher = REGEX.matcher(line);
        if (matcher.matches()) {
            long timestamp = Long.parseLong(matcher.group(1));
            if (config.getLogging().isOptimizeTimestamp()) {
                timestamp = timestamp + Time.getOptimizedTimeOffset();
            }
            String phase = matcher.group(2).equals("S") ? "B" : "E";
            int processId = Integer.parseInt(matcher.group(3));
            int threadId = Integer.parseInt(matcher.group(4));
            String name = matcher.group(5).trim();
            return new TraceEntry(timestamp, phase, name, processId, threadId);
        }
        return null;
    }

    public void convert() {
        List<TraceEntry> entries = new ArrayList<>();
        try (BufferedReader reader = new BufferedReader(new FileReader(inputPath))) {
            String line;
            while ((line = reader.readLine()) != null) {
                TraceEntry entry = processLine(line);
                if (entry != null) {
                    entries.add(entry);
                }
            }

            if (!entries.isEmpty()) {
                JsonFileHandler.writeJsonArrayInBatches(outputPath, entries.iterator(), batchSize);
            }
        } catch (IOException e) {
            System.err.println("Error converting log file: " + e.getMessage());
        } catch (Exception e) {
            System.err.println("Unexpected error during conversion: " + e.getMessage());
        }
    }
}