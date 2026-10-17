package it;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayDeque;
import java.util.Deque;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Stream;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class AgentIT {

    private static final Pattern EVENT = Pattern.compile("\\[\\d+\\] ([SE]) \\[\\d+\\]\\[(\\d+)\\] \"(.+)\"");

    @TempDir
    Path dir;

    @Test
    void tracesEveryCallWithAMatchingExit() throws Exception {
        run(dir, "logging.file=trace.log\nmisc.convertToJson=true\n", ",instrumentation.targetPackage=it");

        List<String> events = Files.readAllLines(dir.resolve("trace.log"), StandardCharsets.UTF_8);
        Map<String, Deque<String>> stacks = new HashMap<>();
        for (String event : events) {
            Matcher matcher = EVENT.matcher(event);
            assertTrue(matcher.matches(), event);
            Deque<String> stack = stacks.computeIfAbsent(matcher.group(2), thread -> new ArrayDeque<>());
            if (matcher.group(1).equals("S")) {
                stack.push(matcher.group(3));
            } else {
                assertEquals(stack.pop(), matcher.group(3));
            }
        }

        assertEquals(10 + 4 * Sample.CALLS, events.size());
        assertTrue(stacks.values().stream().allMatch(Deque::isEmpty));
        assertTrue(events.stream().anyMatch(event -> event.length() > 512));
        String json = new String(Files.readAllBytes(dir.resolve("trace.log.json")), StandardCharsets.UTF_8);
        assertEquals(events.size(), json.split("\"ph\"").length - 1);
    }

    @Test
    void badConfigurationNeverStopsTheApplication() throws Exception {
        run(Files.createDirectory(dir.resolve("empty")), "", "");
        String output = run(Files.createDirectory(dir.resolve("typo")), "instrumentation.targetPackages=it\n", "");

        assertTrue(output.contains("the agent is disabled"), output);
        try (Stream<Path> files = Files.list(dir.resolve("typo"))) {
            assertFalse(files.anyMatch(file -> file.getFileName().toString().startsWith("jib-")));
        }
    }

    private static String run(Path workDir, String configuration, String options) throws Exception {
        Path config = Files.write(workDir.resolve("jib.properties"), configuration.getBytes(StandardCharsets.UTF_8));
        Path output = workDir.resolve("output.txt");
        String classes = Paths.get(Sample.class.getProtectionDomain().getCodeSource().getLocation().toURI()).toString();
        Process process = new ProcessBuilder(System.getProperty("jib.it.java"),
                "-javaagent:" + System.getProperty("jib.agent") + "=config=" + config + options, "-cp", classes, Sample.class.getName())
                .directory(workDir.toFile()).redirectErrorStream(true).redirectOutput(output.toFile()).start();

        assertTrue(process.waitFor(60, TimeUnit.SECONDS));
        assertEquals(0, process.exitValue());
        return new String(Files.readAllBytes(output), StandardCharsets.UTF_8);
    }

    public static class Sample {
        static final int CALLS = 1000;

        public static void main(String[] args) throws Exception {
            try {
                fail();
            } catch (IllegalStateException expected) {
            }
            wide(null, null, null, null, null, null, null, null, null, null, null, null, null, null);
            Thread worker = new Thread(Sample::work);
            worker.start();
            work();
            worker.join();
        }

        static void fail() {
            throw new IllegalStateException();
        }

        static void wide(ConcurrentHashMap<?, ?> a1, ConcurrentHashMap<?, ?> a2, ConcurrentHashMap<?, ?> a3, ConcurrentHashMap<?, ?> a4,
                ConcurrentHashMap<?, ?> a5, ConcurrentHashMap<?, ?> a6, ConcurrentHashMap<?, ?> a7, ConcurrentHashMap<?, ?> a8,
                ConcurrentHashMap<?, ?> a9, ConcurrentHashMap<?, ?> a10, ConcurrentHashMap<?, ?> a11, ConcurrentHashMap<?, ?> a12,
                ConcurrentHashMap<?, ?> a13, ConcurrentHashMap<?, ?> a14) {
        }

        static void work() {
            for (int i = 0; i < CALLS; i++) {
                tiny(i);
            }
        }

        static int tiny(int value) {
            return value + 1;
        }
    }
}
