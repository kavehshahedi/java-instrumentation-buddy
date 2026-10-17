package it;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.lang.management.ManagementFactory;
import java.net.URL;
import java.net.URLClassLoader;
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
        run(dir, "logging.file=trace.log\nmisc.convertToJson=true\nmisc.targetMainClass=" + Sample.class.getName() + "\n",
                ",instrumentation.targetPackage=it");

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

        assertEquals(12 + 4 * Sample.CALLS, events.size());
        assertTrue(stacks.values().stream().allMatch(Deque::isEmpty));
        assertTrue(events.stream().anyMatch(event -> event.length() > 512));
        String json = new String(Files.readAllBytes(dir.resolve("trace.log.json")), StandardCharsets.UTF_8);
        assertEquals(events.size(), json.split("\"ph\"").length - 1);
    }

    @Test
    void inactiveAgentLeavesTheApplicationAlone() throws Exception {
        run(Files.createDirectory(dir.resolve("empty")), "", "");
        String output = run(Files.createDirectory(dir.resolve("typo")), "instrumentation.targetPackages=it\n", "");
        String other = run(Files.createDirectory(dir.resolve("other")), "misc.targetMainClass=com.example.Main, app.jar\n", "");

        assertTrue(output.contains("the agent is disabled"), output);
        assertTrue(other.isEmpty(), other);
        for (String run : new String[] { "typo", "other" }) {
            try (Stream<Path> files = Files.list(dir.resolve(run))) {
                assertFalse(files.anyMatch(file -> file.getFileName().toString().startsWith("jib-")));
            }
        }
    }

    @Test
    void detachStopsTracingAndWritesTheTrace() throws Exception {
        String java = System.getProperty("jib.it.java");
        String agent = System.getProperty("jib.agent");
        Path config = Files.write(dir.resolve("jib.properties"), "logging.file=trace.log\ninstrumentation.targetPackage=it\n".getBytes(StandardCharsets.UTF_8));
        Process app = new ProcessBuilder(java, "-XX:+IgnoreUnrecognizedVMOptions", "-XX:+EnableDynamicAgentLoading",
                "-javaagent:" + agent + "=config=" + config, "-cp", classes(), Detached.class.getName())
                .directory(dir.toFile()).redirectError(dir.resolve("error.txt").toFile()).start();
        String pid = new BufferedReader(new InputStreamReader(app.getInputStream(), StandardCharsets.UTF_8)).readLine();

        Process launcher = new ProcessBuilder(java, "-jar", agent, pid, "detach").inheritIO().start();
        assertTrue(launcher.waitFor(60, TimeUnit.SECONDS));
        assertEquals(0, launcher.exitValue());
        app.getOutputStream().close();
        assertTrue(app.waitFor(60, TimeUnit.SECONDS));
        assertEquals(0, app.exitValue());

        List<String> events = Files.readAllLines(dir.resolve("trace.log"), StandardCharsets.UTF_8);
        assertEquals(2 * Sample.CALLS, events.stream().filter(event -> event.contains("tiny")).count());
        assertTrue(Files.exists(dir.resolve("trace.json")));
    }

    private static String classes() throws Exception {
        return Paths.get(Sample.class.getProtectionDomain().getCodeSource().getLocation().toURI()).toString();
    }

    private static String run(Path workDir, String configuration, String options) throws Exception {
        Path config = Files.write(workDir.resolve("jib.properties"), configuration.getBytes(StandardCharsets.UTF_8));
        Path output = workDir.resolve("output.txt");
        Process process = new ProcessBuilder(System.getProperty("jib.it.java"),
                "-javaagent:" + System.getProperty("jib.agent") + "=config=" + config + options, "-cp", classes(), Sample.class.getName())
                .directory(workDir.toFile()).redirectErrorStream(true).redirectOutput(output.toFile()).start();

        assertTrue(process.waitFor(60, TimeUnit.SECONDS));
        assertEquals(0, process.exitValue());
        return new String(Files.readAllBytes(output), StandardCharsets.UTF_8);
    }

    public static class Isolated {
        public static void run() {
        }
    }

    public static class Detached {
        public static void main(String[] args) throws Exception {
            Sample.work();
            System.out.println(ManagementFactory.getRuntimeMXBean().getName().split("@")[0]);
            System.in.read();
            Sample.work();
        }
    }

    public static class Sample {
        static final int CALLS = 1000;

        public static void main(String[] args) throws Exception {
            try {
                fail();
            } catch (IllegalStateException expected) {
            }
            wide(null, null, null, null, null, null, null, null, null, null, null, null, null, null);
            URL classes = Sample.class.getProtectionDomain().getCodeSource().getLocation();
            new URLClassLoader(new URL[] { classes }, null).loadClass(Isolated.class.getName()).getMethod("run").invoke(null);
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
