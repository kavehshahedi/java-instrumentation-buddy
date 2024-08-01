package kavehshahedi.jib.services;

public class Time {

    public static final long TIME_OFFSET;
    static {
        long currentTimeMillis = System.currentTimeMillis();
        long currentNanoTime = System.nanoTime();
        TIME_OFFSET = (currentTimeMillis * 1_000_000) - currentNanoTime;
    }

    private Time() {
    }

    public static long getTimeNanoSeconds() {
        return System.nanoTime() + TIME_OFFSET;
    }
}
