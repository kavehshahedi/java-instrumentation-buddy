package jib.core;

import net.bytebuddy.asm.Advice;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.LongAdder;
import java.lang.management.ManagementFactory;
import jib.services.Logger;
import jib.models.Configuration;

public class MethodExecutionAdvice {
    public static final String PROCESS_ID = initializeProcessId();
    public static final ThreadLocal<StringBuilder> contextBuilder = ThreadLocal.withInitial(() -> new StringBuilder(256));

    public static final Set<String> visitedMethods = ConcurrentHashMap.newKeySet();
    public static final ConcurrentHashMap<String, LongAdder> instrumentationCounts = new ConcurrentHashMap<>();
    public static int maxInstrumentations;
    public static boolean isLimited;
    public static boolean isOnlyCheckVisited;

    public static String initializeProcessId() {
        String jvmName = ManagementFactory.getRuntimeMXBean().getName();
        return jvmName.split("@")[0];
    }

    public static void setConfig(Configuration config) {
        maxInstrumentations = config.getInstrumentation().getMaxNumberOfInstrumentations();
        isLimited = maxInstrumentations != -1;
        isOnlyCheckVisited = config.getInstrumentation().isOnlyCheckVisited();
    }

    public static String createExecutionContext(String methodSignature) {
        StringBuilder sb = contextBuilder.get();
        sb.setLength(0);

        sb.append('[')
                .append(PROCESS_ID)
                .append("][")
                .append(Thread.currentThread().getId())
                .append("] ")
                .append(methodSignature);

        return sb.toString();
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

        String contextualSignature = createExecutionContext(methodSignature);
        String plainSignature = methodSignature;

        if (!isOnlyCheckVisited || visitedMethods.add(plainSignature)) {
            Logger.logTime(contextualSignature, "S");
        }
        return true;
    }

    @Advice.OnMethodExit
    public static void onExit(@Advice.Origin String methodSignature, @Advice.Enter boolean wasLogged) {
        if (wasLogged && !isOnlyCheckVisited) {
            Logger.logTime(createExecutionContext(methodSignature), "E");
        }
    }
}