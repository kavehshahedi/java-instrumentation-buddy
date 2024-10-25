package jib.core;

import net.bytebuddy.asm.Advice;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.LongAdder;
import java.lang.management.ManagementFactory;
import jib.services.Logger;
import jib.models.Configuration;

public class MethodExecutionAdvice {
    public static final Set<String> visitedMethods = ConcurrentHashMap.newKeySet();
    public static final ConcurrentHashMap<String, LongAdder> instrumentationCounts = new ConcurrentHashMap<>();
    public static int maxInstrumentations;
    public static boolean isLimited;
    public static boolean isOnlyCheckVisited;

    public static void setConfig(Configuration config) {
        maxInstrumentations = config.getInstrumentation().getMaxNumberOfInstrumentations();
        isLimited = maxInstrumentations != -1;
        isOnlyCheckVisited = config.getInstrumentation().isOnlyCheckVisited();
    }

    public static String getProcessId() {
        String jvmName = ManagementFactory.getRuntimeMXBean().getName();
        return jvmName.split("@")[0];
    }

    public static String getThreadId() {
        return String.valueOf(Thread.currentThread().getId());
    }

    public static String createExecutionContext(String methodSignature) {
        // Format: [PID][TID] methodSignature
        return String.format("[%s] [%s] %s", 
            getProcessId(), 
            getThreadId(), 
            methodSignature);
    }

    @Advice.OnMethodEnter
    public static boolean onEnter(@Advice.Origin String methodSignature) {
        String contextualSignature = createExecutionContext(methodSignature);
        
        if (!isLimited) {
            logEntry(contextualSignature);
            return true;
        }

        LongAdder counter = instrumentationCounts.get(methodSignature);
        if (counter == null) {
            counter = new LongAdder();
            LongAdder existingCounter = instrumentationCounts.putIfAbsent(methodSignature, counter);
            if (existingCounter != null) {
                counter = existingCounter;
            }
        }
        
        long count = counter.sum();
        if (count < maxInstrumentations) {
            counter.increment();
            logEntry(contextualSignature);
            return true;
        }
        return false;
    }

    @Advice.OnMethodExit
    public static void onExit(@Advice.Origin String methodSignature, @Advice.Enter boolean wasLogged) {
        if (wasLogged) {
            logExit(createExecutionContext(methodSignature));
        }
    }

    public static void logEntry(String contextualSignature) {
        String methodSignature = contextualSignature.substring(contextualSignature.lastIndexOf("]") + 2);
        if (!isOnlyCheckVisited || visitedMethods.add(methodSignature)) {
            Logger.logTime(contextualSignature, "S");
        }
    }

    public static void logExit(String contextualSignature) {
        if (!isOnlyCheckVisited) {
            Logger.logTime(contextualSignature, "E");
        }
    }
}