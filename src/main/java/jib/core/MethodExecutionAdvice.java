package jib.core;

import net.bytebuddy.asm.Advice;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.LongAdder;
import java.lang.management.ManagementFactory;
import jib.services.FastAsyncLogger;
import jib.models.Configuration;

public class MethodExecutionAdvice {
    public static final String PROCESS_ID = initializeProcessId();
    public static final ThreadLocal<Long> threadIdCache = ThreadLocal.withInitial(() -> Thread.currentThread().getId());

    public static final Set<String> visitedMethods = ConcurrentHashMap.newKeySet();
    public static final ConcurrentHashMap<String, LongAdder> instrumentationCounts = new ConcurrentHashMap<>();
    public static int maxInstrumentations;
    public static boolean isLimited;
    public static boolean isOnlyCheckVisited;

    public static final long TIME_OFFSET;
    static {
        long currentTimeMillis = System.currentTimeMillis();
        long currentNanoTime = System.nanoTime();
        TIME_OFFSET = (currentTimeMillis * 1_000_000) - currentNanoTime;
    }

    public static String initializeProcessId() {
        String jvmName = ManagementFactory.getRuntimeMXBean().getName();
        return jvmName.split("@")[0];
    }

    public static void setConfig(Configuration config) {
        maxInstrumentations = config.getInstrumentation().getMaxNumberOfInstrumentations();
        isLimited = maxInstrumentations != -1;
        isOnlyCheckVisited = config.getInstrumentation().isOnlyCheckVisited();
    }

    @Advice.OnMethodEnter
    public static boolean onEnter(@Advice.Origin String methodSignature) {
        if (isLimited) {
            LongAdder counter = instrumentationCounts.get(methodSignature);
            if (counter == null) {
                counter = instrumentationCounts.computeIfAbsent(methodSignature, k -> new LongAdder());
            }
            if (counter.sum() >= maxInstrumentations) {
                return false;
            }
            counter.increment();
        }

        if (!isOnlyCheckVisited || visitedMethods.add(methodSignature)) {
            long timestamp = System.nanoTime() + TIME_OFFSET;
            FastAsyncLogger.log(timestamp, (byte) 0, threadIdCache.get(), methodSignature);
        }
        return true;
    }

    @Advice.OnMethodExit
    public static void onExit(@Advice.Origin String methodSignature, @Advice.Enter boolean wasLogged) {
        if (wasLogged && !isOnlyCheckVisited) {
            long timestamp = System.nanoTime() + TIME_OFFSET;
            FastAsyncLogger.log(timestamp, (byte) 1, threadIdCache.get(), methodSignature);
        }
    }
}