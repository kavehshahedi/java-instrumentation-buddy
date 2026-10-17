package jib.services;

import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Queue;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.atomic.AtomicLongArray;
import java.util.concurrent.locks.LockSupport;
import jib.core.MethodRegistry;
import jib.models.Configuration;
import jib.utils.Time;

public class FastAsyncLogger {
    private static final int FIRST_CHUNK_SIZE = 16;
    private static final int MAX_ENTRY_OVERHEAD = 64;
    private static final int CACHE_LINE_PADDING = 16;
    private static final int DROP_RETRY_INTERVAL = 1023;
    private static final int CLEANUP_INTERVAL = 1023;
    private static final int STARTED = 1;
    private static final int STOPPED = 2;
    private static final byte[] DIGITS = new byte[200];

    private static int batchSize = 4096;
    private static int queueDepth = 64;
    private static long flushIntervalNanos = 100_000_000L;
    private static int bufferSize = 2 * 1024 * 1024;

    private static final ThreadLocal<ThreadTrace> traces = new ThreadLocal<>();
    private static final Queue<ThreadTrace> threads = new ConcurrentLinkedQueue<>();
    private static final Queue<Chunk> pool = new ConcurrentLinkedQueue<>();
    private static final AtomicInteger pendingChunks = new AtomicInteger();
    private static final AtomicInteger registeredThreads = new AtomicInteger();
    private static final AtomicLong droppedEvents = new AtomicLong();
    private static volatile int state;
    private static Thread writerThread;
    private static FileChannel fileChannel;
    private static FileOutputStream fileOutputStream;
    private static String logFileName;
    private static byte[] output;
    private static int outputPosition;
    private static long cachedSeconds = -1;
    private static final byte[] cachedSecondsDigits = new byte[20];
    private static int cachedSecondsLength;

    private static boolean useHash = false;
    private static boolean optimizeTimestamp = false;
    private static final Map<String, byte[]> methodSignatureToHashBytes = new ConcurrentHashMap<>();
    private static byte[][] signatureBytes = new byte[1024][];
    private static int hashCounter;
    private static final char[] ALPHABET = "ABCDEFGHIJKLMNOPQRSTUVWXYZ".toCharArray();
    private static final int ALPHABET_SIZE = ALPHABET.length;

    private static long startTime;
    private static String processId;

    static {
        for (int i = 0; i < 100; i++) {
            DIGITS[i * 2] = (byte) ('0' + i / 10);
            DIGITS[i * 2 + 1] = (byte) ('0' + i % 10);
        }
    }

    private static final class Chunk {
        final long[] events;
        final AtomicLongArray count;
        final int countIndex;
        volatile Chunk next;
        int written;

        Chunk(int capacity) {
            events = new long[capacity * 2];
            countIndex = capacity == batchSize ? CACHE_LINE_PADDING : 0;
            count = new AtomicLongArray(countIndex * 2 + 1);
        }

        int capacity() {
            return events.length >> 1;
        }

        int size() {
            return (int) count.get(countIndex);
        }
    }

    private static final class ThreadTrace {
        final Thread owner;
        final long threadId;
        final AtomicLong dropped = new AtomicLong();
        Chunk current;
        Chunk head;
        byte[] prefix;

        ThreadTrace(Thread owner, Chunk chunk) {
            this.owner = owner;
            this.threadId = owner.getId();
            this.current = chunk;
            this.head = chunk;
        }
    }

    public static void initialize(Configuration.Logging config, String pid) {
        useHash = config.isUseHash();
        optimizeTimestamp = config.isOptimizeTimestamp();
        batchSize = config.getBatchSize();
        queueDepth = config.getQueueDepth();
        flushIntervalNanos = config.getFlushIntervalMs() * 1_000_000L;
        bufferSize = config.getBufferSize();
        processId = pid;
        logFileName = config.getFile();
        startTime = Time.getTimeNanoSeconds();
    }

    private static synchronized boolean start() {
        if (state == 0) {
            try {
                fileOutputStream = new FileOutputStream(logFileName);
                fileChannel = fileOutputStream.getChannel();
                writerThread = new Thread(FastAsyncLogger::writerLoop, "FastAsyncLogger-Writer");
                writerThread.setDaemon(true);
                state = STARTED;
                writerThread.start();
            } catch (IOException e) {
                System.err.println("Failed to open log file: " + (e.getMessage() != null ? e.getMessage() : e.getClass().getSimpleName()));
                e.printStackTrace();
                state = STOPPED;
            }
        }
        return state == STARTED;
    }

    public static void log(int methodId, int type) {
        int currentState = state;
        if (currentState != STARTED && (currentState == STOPPED || !start())) {
            return;
        }

        ThreadTrace trace = traces.get();
        if (trace == null) {
            trace = register();
        }
        Chunk chunk = trace.current;
        int size = chunk.size();
        if (size == chunk.capacity()) {
            long dropped = trace.dropped.get();
            chunk = (dropped & DROP_RETRY_INTERVAL) == 0 ? nextChunk(trace) : null;
            if (chunk == null) {
                trace.dropped.lazySet(dropped + 1);
                return;
            }
            size = 0;
        }
        chunk.events[size * 2] = Time.getTimeNanoSeconds();
        chunk.events[size * 2 + 1] = (long) methodId << 1 | type;
        chunk.count.lazySet(chunk.countIndex, size + 1);
    }

    private static ThreadTrace register() {
        ThreadTrace trace = new ThreadTrace(Thread.currentThread(), new Chunk(Math.min(FIRST_CHUNK_SIZE, batchSize)));
        traces.set(trace);
        threads.add(trace);
        if ((registeredThreads.incrementAndGet() & CLEANUP_INTERVAL) == 0) {
            LockSupport.unpark(writerThread);
        }
        return trace;
    }

    private static Chunk nextChunk(ThreadTrace trace) {
        if (pendingChunks.get() >= queueDepth) {
            return null;
        }
        Chunk chunk = pool.poll();
        if (chunk == null) {
            chunk = new Chunk(batchSize);
        }
        pendingChunks.incrementAndGet();
        trace.current.next = chunk;
        trace.current = chunk;
        LockSupport.unpark(writerThread);
        return chunk;
    }

    private static void writerLoop() {
        output = new byte[bufferSize];
        try {
            while (state == STARTED) {
                if (!writeThreads(false)) {
                    LockSupport.parkNanos(flushIntervalNanos);
                    if (!writeThreads(false)) {
                        writeThreads(true);
                        flushOutput();
                    }
                }
            }

            writeThreads(true);
            flushOutput();
            if (fileChannel.isOpen()) {
                fileChannel.force(false);
            }
        } catch (IOException e) {
            System.err.println("Writer error: " + e.getMessage());
            e.printStackTrace();
        }
    }

    private static boolean writeThreads(boolean partial) throws IOException {
        boolean consumedFullChunk = false;
        for (Iterator<ThreadTrace> iterator = threads.iterator(); iterator.hasNext();) {
            ThreadTrace trace = iterator.next();
            boolean alive = trace.owner.isAlive();
            Chunk chunk = trace.head;
            while (true) {
                Chunk next = chunk.next;
                if (next == null && !partial) {
                    break;
                }
                int size = chunk.size();
                if (chunk.written < size) {
                    writeEvents(trace, chunk, size);
                }
                if (next == null) {
                    break;
                }
                pendingChunks.decrementAndGet();
                reuse(chunk);
                chunk = next;
                trace.head = chunk;
                consumedFullChunk = true;
            }

            if (!alive && partial) {
                iterator.remove();
                droppedEvents.addAndGet(trace.dropped.get());
                reuse(chunk);
            }
        }
        return consumedFullChunk;
    }

    private static void reuse(Chunk chunk) {
        if (chunk.capacity() == batchSize) {
            chunk.count.set(chunk.countIndex, 0);
            chunk.written = 0;
            chunk.next = null;
            pool.offer(chunk);
        }
    }

    private static void writeEvents(ThreadTrace trace, Chunk chunk, int size) throws IOException {
        if (trace.prefix == null) {
            trace.prefix = ("[" + processId + "][" + trace.threadId + "] \"").getBytes(StandardCharsets.UTF_8);
        }
        byte[] prefix = trace.prefix;
        long[] events = chunk.events;

        for (int i = chunk.written; i < size; i++) {
            long event = events[i * 2 + 1];
            byte[] signature = signatureBytes((int) (event >>> 1));
            int maxLength = MAX_ENTRY_OVERHEAD + prefix.length + signature.length;
            if (outputPosition + maxLength > output.length) {
                flushOutput();
                if (maxLength > output.length) {
                    output = new byte[maxLength];
                }
            }

            byte[] buffer = output;
            int pos = outputPosition;
            buffer[pos++] = '[';
            pos = writeTimestamp(optimizeTimestamp ? events[i * 2] - Time.OPTIMIZED_OFFSET : events[i * 2], buffer, pos);
            buffer[pos++] = ']';
            buffer[pos++] = ' ';
            buffer[pos++] = (event & 1) == 0 ? (byte) 'S' : (byte) 'E';
            buffer[pos++] = ' ';
            System.arraycopy(prefix, 0, buffer, pos, prefix.length);
            pos += prefix.length;
            System.arraycopy(signature, 0, buffer, pos, signature.length);
            pos += signature.length;
            buffer[pos++] = '"';
            buffer[pos++] = '\n';
            outputPosition = pos;
        }
        chunk.written = size;
    }

    private static void flushOutput() throws IOException {
        ByteBuffer byteBuffer = ByteBuffer.wrap(output, 0, outputPosition);
        while (byteBuffer.hasRemaining()) {
            fileChannel.write(byteBuffer);
        }
        outputPosition = 0;
    }

    private static int writeTimestamp(long timestamp, byte[] buffer, int pos) {
        long seconds = timestamp / 1_000_000_000L;
        if (seconds <= 0) {
            return writeLong(timestamp, buffer, pos);
        }
        if (seconds != cachedSeconds) {
            cachedSeconds = seconds;
            cachedSecondsLength = writeLong(seconds, cachedSecondsDigits, 0);
        }
        System.arraycopy(cachedSecondsDigits, 0, buffer, pos, cachedSecondsLength);
        pos += cachedSecondsLength;

        int nanos = (int) (timestamp - seconds * 1_000_000_000L);
        int end = pos + 9;
        for (int i = end; i > pos + 1; i -= 2) {
            int pair = nanos % 100 * 2;
            nanos /= 100;
            buffer[i - 1] = DIGITS[pair + 1];
            buffer[i - 2] = DIGITS[pair];
        }
        buffer[pos] = (byte) ('0' + nanos);
        return end;
    }

    private static int writeLong(long value, byte[] buffer, int pos) {
        if (value < 0) {
            buffer[pos++] = '-';
            value = -value;
        }

        int length = 1;
        for (long limit = 10; length < 19 && value >= limit; limit *= 10) {
            length++;
        }

        int end = pos + length;
        int i = end;
        while (value >= 100) {
            int pair = (int) (value % 100) * 2;
            value /= 100;
            buffer[--i] = DIGITS[pair + 1];
            buffer[--i] = DIGITS[pair];
        }
        if (value >= 10) {
            buffer[--i] = DIGITS[(int) value * 2 + 1];
            buffer[--i] = DIGITS[(int) value * 2];
        } else {
            buffer[--i] = (byte) ('0' + value);
        }
        return end;
    }

    private static byte[] signatureBytes(int methodId) {
        if (methodId >= signatureBytes.length) {
            signatureBytes = Arrays.copyOf(signatureBytes, Math.max(methodId + 1, signatureBytes.length * 2));
        }
        byte[] bytes = signatureBytes[methodId];
        if (bytes == null) {
            String signature = MethodRegistry.name(methodId);
            if (useHash) {
                bytes = generateHash(hashCounter++).getBytes(StandardCharsets.UTF_8);
                methodSignatureToHashBytes.put(signature, bytes);
            } else {
                bytes = signature.getBytes(StandardCharsets.UTF_8);
            }
            signatureBytes[methodId] = bytes;
        }
        return bytes;
    }

    private static String generateHash(int number) {
        StringBuilder hash = new StringBuilder(4);
        while (number >= 0) {
            hash.insert(0, ALPHABET[number % ALPHABET_SIZE]);
            number = number / ALPHABET_SIZE - 1;
        }
        return hash.toString();
    }

    public static Map<String, String> getMethodSignaturesByHash() {
        Map<String, String> methodSignatures = new HashMap<>();
        for (Map.Entry<String, byte[]> entry : methodSignatureToHashBytes.entrySet()) {
            methodSignatures.put(new String(entry.getValue(), StandardCharsets.UTF_8), entry.getKey());
        }
        return methodSignatures;
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
                    .append(JsonFileHandler.escapeJsonString(entry.getKey()))
                    .append("\": \"")
                    .append(JsonFileHandler.escapeJsonString(hash))
                    .append("\"");
        }

        sb.append("\n}");
        return sb.toString();
    }

    public static long getStartTime() {
        return startTime;
    }

    public static long getDroppedEvents() {
        return droppedEvents.get();
    }

    public static synchronized boolean close() {
        if (state != STARTED) {
            state = STOPPED;
            return false;
        }
        state = STOPPED;
        LockSupport.unpark(writerThread);

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

        for (ThreadTrace trace : threads) {
            droppedEvents.addAndGet(trace.dropped.get());
        }
        threads.clear();
        if (droppedEvents.get() > 0) {
            System.err.println("Dropped " + droppedEvents.get() + " trace events because the writer could not keep up");
        }
        return true;
    }
}
