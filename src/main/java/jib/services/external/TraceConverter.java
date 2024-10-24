package jib.services.external;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import jib.models.TraceEntry;
import jib.services.JsonFileHandler;
import jib.utils.Time;

public class TraceConverter {

    private static final Pattern REGEX = Pattern.compile("\\[(\\d+)\\] (S|E) ([\\w\\W\\s.]+)");
    private final String inputPath;
    private final String outputPath;
    private final int batchSize;

    public TraceConverter(String inputPath, String outputPath, int batchSize) {
        this.inputPath = inputPath;
        this.outputPath = outputPath;
        this.batchSize = batchSize;
    }

    public TraceConverter(String inputPath, String outputPath) {
        this(inputPath, outputPath, 1000);
    }

    private TraceEntry processLine(String line) {
        Matcher matcher = REGEX.matcher(line);
        if (matcher.matches()) {
            long timestamp = Long.parseLong(matcher.group(1)) + Time.getOptimizedTimeOffset();
            String phase = matcher.group(2).equals("S") ? "B" : "E";
            String name = matcher.group(3).trim();
            return new TraceEntry(timestamp, phase, name);
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

            JsonFileHandler.writeJsonArrayInBatches(outputPath, entries.iterator(), batchSize);
        } catch (IOException e) {
            System.err.println("Error reading file: " + e.getMessage());
        }
    }
}