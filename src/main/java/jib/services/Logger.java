package jib.services;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;
import jib.utils.Time;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.core.LoggerContext;
import jib.models.Configuration;

public class Logger {
    private static org.apache.logging.log4j.Logger logger;
    private static boolean useHash = false;
    private static boolean optimizeTimestamp = false;

    private static final int INITIAL_BUFFER_SIZE = 128;
    private static final ThreadLocal<StringBuilder> logBuilder = ThreadLocal.withInitial(() -> new StringBuilder(INITIAL_BUFFER_SIZE));

    private static final Map<String, String> methodSignatureToHash = new ConcurrentHashMap<>();
    private static final AtomicInteger counter = new AtomicInteger(0);
    private static final char[] ALPHABET = "ABCDEFGHIJKLMNOPQRSTUVWXYZ".toCharArray();
    private static final int ALPHABET_SIZE = ALPHABET.length;

    public static void initialize(Configuration.Logging loggingInfo) {
        String logFileName = loggingInfo.getFile();
        System.setProperty("logFilename", logFileName);

        LoggerContext ctx = (LoggerContext) LogManager.getContext(false);
        ctx.reconfigure();

        logger = LogManager.getLogger(Logger.class);

        useHash = loggingInfo.isUseHash();
        optimizeTimestamp = loggingInfo.isOptimizeTimestamp();
    }

    private static String generateHash(int number) {
        StringBuilder hash = new StringBuilder(4);
        while (number >= 0) {
            hash.insert(0, ALPHABET[number % ALPHABET_SIZE]);
            number = number / ALPHABET_SIZE - 1;
        }
        return hash.toString();
    }

    private static String getMethodHash(String methodSignature) {
        return methodSignatureToHash.computeIfAbsent(methodSignature, key -> generateHash(counter.getAndIncrement()));
    }

    public static void logTime(String methodSignature, String type) {
        StringBuilder sb = logBuilder.get();
        sb.setLength(0);

        // Append timestamp
        long timestamp = Time.getTimeNanoSeconds();
        if (optimizeTimestamp) {
            String timestampStr = String.valueOf(timestamp);
            sb.append('[').append(timestampStr, 4, timestampStr.length()).append(']');
        } else {
            sb.append('[').append(timestamp).append(']');
        }

        // Append type
        sb.append(' ').append(type).append(' ');

        // Append method signature or hash
        if (useHash) {
            sb.append(getMethodHash(methodSignature));
        } else {
            sb.append(methodSignature);
        }

        logger.info(sb.toString());
    }

    public static String getMethodSignatureHashJson() {
        StringBuilder sb = new StringBuilder(methodSignatureToHash.size() * 64);
        sb.append("{\n");

        boolean firstEntry = true;
        for (Map.Entry<String, String> entry : methodSignatureToHash.entrySet()) {
            if (!firstEntry) {
                sb.append(",\n");
            } else {
                firstEntry = false;
            }

            sb.append("  \"")
                    .append(entry.getKey().replace("\"", "\\\""))
                    .append("\": \"")
                    .append(entry.getValue().replace("\"", "\\\""))
                    .append("\"");
        }

        sb.append("\n}");
        return sb.toString();
    }

    public static void close() {
        LogManager.shutdown();
    }
}