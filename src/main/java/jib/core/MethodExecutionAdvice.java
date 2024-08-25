package jib.core;

import net.bytebuddy.asm.Advice;

import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

import jib.services.Logger;
import jib.models.Configuration;

public class MethodExecutionAdvice {
    public static final Set<String> visitedMethods = ConcurrentHashMap.newKeySet();
    public static Configuration config;

    public static void setConfig(Configuration config) {
        MethodExecutionAdvice.config = config;
    }

    @Advice.OnMethodEnter
    public static void onEnter(@Advice.Origin String methodSignature) {
        if (config.getInstrumentation().isOnlyCheckVisited()) {
            if (visitedMethods.add(methodSignature)) {
                Logger.logTime(methodSignature, "S");
            }
        } else {
            Logger.logTime(methodSignature, "S");
        }
    }

    @Advice.OnMethodExit
    public static void onExit(@Advice.Origin String methodSignature) {
        if (!config.getInstrumentation().isOnlyCheckVisited()) {
            Logger.logTime(methodSignature, "E");
        }
    }
}