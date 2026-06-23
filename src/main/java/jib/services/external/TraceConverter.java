package jib.services.external;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

import jib.models.Configuration;
import jib.models.TraceEntry;
import jib.utils.Time;

public class TraceConverter {

    private Configuration config;

    private static final Pattern REGEX = Pattern.compile("\\[(\\d+)\\] (S|E) \\[(\\d+)\\]\\[(\\d+)\\] \"([\\w\\W\\s.]+)\"");
    private final String inputPath;
    private final String outputPath;

    public TraceConverter(Configuration config, String inputPath, String outputPath) {
        this.config = config;
        this.inputPath = inputPath;
        this.outputPath = outputPath;
    }

    private TraceEntry processLine(String line) {
        Matcher matcher = REGEX.matcher(line);
        if (matcher.matches()) {
            long timestamp = Long.parseLong(matcher.group(1));
            if (config.getLogging().isOptimizeTimestamp()) {
                timestamp = timestamp + Time.OPTIMIZED_OFFSET;
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
        try (BufferedReader reader = Files.newBufferedReader(Paths.get(inputPath), StandardCharsets.UTF_8);
                BufferedWriter writer = Files.newBufferedWriter(Paths.get(outputPath), StandardCharsets.UTF_8)) {
            writer.write("[\n");
            boolean first = true;
            String line;
            while ((line = reader.readLine()) != null) {
                TraceEntry entry = processLine(line);
                if (entry != null) {
                    if (!first) {
                        writer.write(",\n");
                    }
                    writer.write(entry.toJson(1));
                    first = false;
                }
            }
            writer.write("\n]");
        } catch (IOException e) {
            System.err.println("Error converting log file: " + e.getMessage());
        } catch (Exception e) {
            System.err.println("Unexpected error during conversion: " + e.getMessage());
        }
    }
}