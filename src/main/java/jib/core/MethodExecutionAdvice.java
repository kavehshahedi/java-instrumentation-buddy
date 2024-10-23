package jib.core;

import net.bytebuddy.asm.Advice;

import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.LongAdder;

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

    @Advice.OnMethodEnter
    public static boolean onEnter(@Advice.Origin String methodSignature) {
        if (!isLimited) {
            logEntry(methodSignature);
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
            logEntry(methodSignature);
            return true;
        }
        return false;
    }

    @Advice.OnMethodExit
    public static void onExit(@Advice.Origin String methodSignature, @Advice.Enter boolean wasLogged) {
        if (wasLogged) {
            logExit(methodSignature);
        }
    }

    public static void logEntry(String methodSignature) {
        if (!isOnlyCheckVisited || visitedMethods.add(methodSignature)) {
            Logger.logTime(methodSignature, "S");
        }
    }

    public static void logExit(String methodSignature) {
        if (!isOnlyCheckVisited) {
            Logger.logTime(methodSignature, "E");
        }
    }
}