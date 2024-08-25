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

    public Logger() {
    }

    public static void initialize(Configuration.Logging loggingInfo) {
        String logFileName = loggingInfo.getFile();
        System.setProperty("logFilename", logFileName);

        LoggerContext ctx = (LoggerContext) LogManager.getContext(false);
        ctx.reconfigure();

        logger = LogManager.getLogger(Logger.class);

        useHash = loggingInfo.isUseHash();
    }

    private static final String LOG_PATTERN = "[%s] %s %s";
    private static final AtomicInteger counter = new AtomicInteger(0);
    private static final int ALPHABET_SIZE = 26;
    private static final char[] ALPHABET = "ABCDEFGHIJKLMNOPQRSTUVWXYZ".toCharArray();
    private static final Map<String, String> methodSignatureToHash = new ConcurrentHashMap<>();

    private static String generateHash(int number) {
        StringBuilder hash = new StringBuilder();
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
        String message = String.format(LOG_PATTERN,
                String.valueOf(Time.getTimeNanoSeconds()).substring(4),
                type,
                useHash ? getMethodHash(methodSignature) : methodSignature);

        logger.info(message);
    }

    public static String getMethodSignatureHashJson() {
        StringBuilder sb = new StringBuilder();
        sb.append("{\n");

        boolean firstEntry = true;
        for (Map.Entry<String, String> entry : methodSignatureToHash.entrySet()) {
            if (!firstEntry) {
                sb.append(",\n");
            } else {
                firstEntry = false;
            }
            String methodSignature = entry.getKey().replace("\"", "\\\"");
            String hash = entry.getValue().replace("\"", "\\\"");
            sb.append(String.format("  \"%s\": \"%s\"", methodSignature, hash));
        }

        sb.append("\n}");

        return sb.toString();
    }

    public static void close() {
        LogManager.shutdown();
    }

}
