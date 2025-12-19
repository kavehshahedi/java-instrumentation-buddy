package jib.services;

import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;
import jib.models.Configuration;

public class FastAsyncLogger {
    private static int BATCH_SIZE = 4096; // 4096 entries
    private static int QUEUE_DEPTH = 2000; // 2000 entries
    private static int FLUSH_INTERVAL_MS = 100; // 100ms
    private static int BUFFER_SIZE = 2 * 1024 * 1024; // 2MB

    private static BlockingQueue<List<LogEntry>> queue;
    private static List<LogEntry> buffer;
    private static Thread writerThread;
    private static FileChannel fileChannel;
    private static FileOutputStream fileOutputStream;
    private static volatile boolean running = true;

    private static boolean useHash = false;
    private static boolean optimizeTimestamp = false;
    private static final Map<String, byte[]> methodSignatureToHashBytes = new ConcurrentHashMap<>();
    private static final Map<String, byte[]> methodSignatureToBytes = new ConcurrentHashMap<>();
    private static final AtomicInteger counter = new AtomicInteger(0);
    private static final char[] ALPHABET = "ABCDEFGHIJKLMNOPQRSTUVWXYZ".toCharArray();
    private static final int ALPHABET_SIZE = ALPHABET.length;

    private static long startTime;
    private static Thread flusherThread;
    private static long optimizedOffset;
    private static String processId;
    private static byte[] processIdBytes;

    static class LogEntry {
        final long timestamp;
        final byte type;
        final long threadId;
        final String methodSig;

        LogEntry(long ts, byte type, long tid, String sig) {
            this.timestamp = ts;
            this.type = type;
            this.threadId = tid;
            this.methodSig = sig;
        }
    }

    public static void initialize(Configuration.Logging config, String pid) {
        useHash = config.isUseHash();
        optimizeTimestamp = config.isOptimizeTimestamp();
        BATCH_SIZE = config.getBatchSize();
        QUEUE_DEPTH = config.getQueueDepth();
        FLUSH_INTERVAL_MS = config.getFlushIntervalMs();
        BUFFER_SIZE = config.getBufferSize();
        processId = pid;
        processIdBytes = processId.getBytes(StandardCharsets.UTF_8);
        String logFileName = config.getFile();

        if (optimizeTimestamp) {
            optimizedOffset = Long.parseLong(
                    String.valueOf(System.currentTimeMillis()).substring(0, 4) + "000000000000000");
        }

        buffer = new ArrayList<>(BATCH_SIZE);
        queue = new ArrayBlockingQueue<>(QUEUE_DEPTH);

        try {
            fileOutputStream = new FileOutputStream(logFileName);
            fileChannel = fileOutputStream.getChannel();
        } catch (IOException e) {
            System.err.println("Failed to open log file: " + (e.getMessage() != null ? e.getMessage() : e.getClass().getSimpleName()));
            e.printStackTrace();
            return;
        }

        startTime = System.nanoTime() + getTimeOffset();

        writerThread = new Thread(FastAsyncLogger::writerLoop, "FastAsyncLogger-Writer");
        writerThread.setDaemon(true);
        writerThread.setPriority(Thread.MAX_PRIORITY);
        writerThread.start();

        flusherThread = new Thread(FastAsyncLogger::flusherLoop, "FastAsyncLogger-Flusher");
        flusherThread.setDaemon(true);
        flusherThread.start();
    }

    public static void log(long timestamp, byte type, long threadId, String methodSig) {
        LogEntry entry = new LogEntry(timestamp, type, threadId, methodSig);
        synchronized (buffer) {
            buffer.add(entry);
            if (buffer.size() >= BATCH_SIZE) {
                if (queue.offer(new ArrayList<>(buffer))) {
                    buffer.clear();
                }
            }
        }
    }

    private static void writerLoop() {
        ByteBuffer byteBuffer = ByteBuffer.allocateDirect(BUFFER_SIZE);
        byte[] tempBuffer = new byte[512];

        try {
            while (running || !queue.isEmpty()) {
                List<LogEntry> batch = queue.poll();
                if (batch == null) {
                    if (!running && queue.isEmpty())
                        break;
                    Thread.yield();
                    continue;
                }

                byteBuffer.clear();

                for (LogEntry entry : batch) {
                    int len = formatEntryToBytes(entry, tempBuffer);
                    if (byteBuffer.remaining() < len) {
                        byteBuffer.flip();
                        try {
                            fileChannel.write(byteBuffer);
                        } catch (java.nio.channels.ClosedChannelException e) {
                            return;
                        }
                        byteBuffer.clear();
                    }
                    byteBuffer.put(tempBuffer, 0, len);
                }

                if (byteBuffer.position() > 0) {
                    byteBuffer.flip();
                    try {
                        fileChannel.write(byteBuffer);
                    } catch (java.nio.channels.ClosedChannelException e) {
                        return;
                    }
                }
            }

            if (fileChannel.isOpen()) {
                fileChannel.force(false);
            }
        } catch (IOException e) {
            System.err.println("Writer error: " + e.getMessage());
            e.printStackTrace();
        }
    }

    private static void flusherLoop() {
        while (running) {
            try {
                Thread.sleep(FLUSH_INTERVAL_MS);
                synchronized (buffer) {
                    if (!buffer.isEmpty() && buffer.size() < BATCH_SIZE) {
                        if (queue.offer(new ArrayList<>(buffer))) {
                            buffer.clear();
                        }
                    }
                }
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                break;
            }
        }
    }

    private static int formatEntryToBytes(LogEntry entry, byte[] buffer) {
        int pos = 0;

        // [
        buffer[pos++] = '[';

        // timestamp
        long ts = entry.timestamp;
        if (optimizeTimestamp) {
            ts = ts - optimizedOffset;
        }
        pos = writeLong(ts, buffer, pos);

        // ]
        buffer[pos++] = ']';
        buffer[pos++] = ' ';

        // S or E
        buffer[pos++] = entry.type == 0 ? (byte) 'S' : (byte) 'E';
        buffer[pos++] = ' ';

        // [processId]
        buffer[pos++] = '[';
        System.arraycopy(processIdBytes, 0, buffer, pos, processIdBytes.length);
        pos += processIdBytes.length;
        buffer[pos++] = ']';

        // [threadId]
        buffer[pos++] = '[';
        pos = writeLong(entry.threadId, buffer, pos);
        buffer[pos++] = ']';
        buffer[pos++] = ' ';

        // method signature or hash
        buffer[pos++] = '"';
        byte[] sigBytes;
        if (useHash) {
            sigBytes = getMethodHashBytes(entry.methodSig);
        } else {
            sigBytes = getMethodSigBytes(entry.methodSig);
        }
        System.arraycopy(sigBytes, 0, buffer, pos, sigBytes.length);
        pos += sigBytes.length;
        buffer[pos++] = '"';

        // newline
        buffer[pos++] = '\n';

        return pos;
    }

    private static int writeLong(long value, byte[] buffer, int pos) {
        if (value == 0) {
            buffer[pos++] = '0';
            return pos;
        }

        if (value < 0) {
            buffer[pos++] = '-';
            value = -value;
        }

        long temp = value;
        while (temp > 0) {
            temp /= 10;
            pos++;
        }

        int end = pos - 1;
        while (value > 0) {
            buffer[end--] = (byte) ('0' + (value % 10));
            value /= 10;
        }

        return pos;
    }

    private static String generateHash(int number) {
        StringBuilder hash = new StringBuilder(4);
        while (number >= 0) {
            hash.insert(0, ALPHABET[number % ALPHABET_SIZE]);
            number = number / ALPHABET_SIZE - 1;
        }
        return hash.toString();
    }

    private static byte[] getMethodHashBytes(String methodSignature) {
        return methodSignatureToHashBytes.computeIfAbsent(methodSignature, key -> {
            String hash = generateHash(counter.getAndIncrement());
            return hash.getBytes(StandardCharsets.UTF_8);
        });
    }

    private static byte[] getMethodSigBytes(String methodSignature) {
        return methodSignatureToBytes.computeIfAbsent(methodSignature,
                key -> key.getBytes(StandardCharsets.UTF_8));
    }

    public static String getMethodSignatureHashJson() {
        StringBuilder sb = new StringBuilder(methodSignatureToHashBytes.size() * 64);
        sb.append("{\n");

        boolean firstEntry = true;
        for (Map.Entry<String, byte[]> entry : methodSignatureToHashBytes.entrySet()) {
            if (!firstEntry) {
                sb.append(",\n");
            } else {
                firstEntry = false;
            }

            String hash = new String(entry.getValue(), StandardCharsets.UTF_8);
            sb.append("  \"")
                    .append(entry.getKey().replace("\"", "\\\""))
                    .append("\": \"")
                    .append(hash.replace("\"", "\\\""))
                    .append("\"");
        }

        sb.append("\n}");
        return sb.toString();
    }

    public static long getStartTime() {
        return startTime;
    }

    private static long getTimeOffset() {
        long currentTimeMillis = System.currentTimeMillis();
        long currentNanoTime = System.nanoTime();
        return (currentTimeMillis * 1_000_000) - currentNanoTime;
    }

    public static void close() {
        running = false;

        synchronized (buffer) {
            if (!buffer.isEmpty()) {
                queue.offer(new ArrayList<>(buffer));
                buffer.clear();
            }
        }

        try {
            writerThread.join();
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }

        try {
            if (fileChannel != null && fileChannel.isOpen()) {
                fileChannel.force(false);
                fileChannel.close();
            }
        } catch (IOException e) {
            System.err.println("Error closing log file channel: " + e.getMessage());
            e.printStackTrace();
        }

        try {
            if (fileOutputStream != null) {
                fileOutputStream.close();
            }
        } catch (IOException e) {
            System.err.println("Error closing log file stream: " + e.getMessage());
            e.printStackTrace();
        }
    }
}