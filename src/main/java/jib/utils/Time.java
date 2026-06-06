package jib.utils;

public class Time {

    public static final long TIME_OFFSET = calibrateOffset();
    public static final long OPTIMIZED_OFFSET = getTimeNanoSeconds() / 1_000_000_000_000_000L * 1_000_000_000_000_000L;

    private Time() {
    }

    public static long getTimeNanoSeconds() {
        return System.nanoTime() + TIME_OFFSET;
    }

    private static long calibrateOffset() {
        long millis = System.currentTimeMillis();
        long next;
        do {
            next = System.currentTimeMillis();
        } while (next == millis);
        return next * 1_000_000 - System.nanoTime();
    }
}
