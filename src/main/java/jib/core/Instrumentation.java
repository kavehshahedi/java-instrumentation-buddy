package jib.core;

import net.bytebuddy.agent.builder.AgentBuilder;
import net.bytebuddy.description.method.MethodDescription;
import net.bytebuddy.description.type.TypeDescription;
import net.bytebuddy.matcher.ElementMatcher;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.Future;
import java.security.ProtectionDomain;
import java.lang.instrument.ClassFileTransformer;

import jib.models.Configuration;
import jib.helpers.MethodMatcherHelper;

public class Instrumentation {
    private static final ExecutorService instrumentationExecutor = Executors.newSingleThreadExecutor(r -> {
        Thread t = new Thread(r, "jib-instrumentation-thread");
        t.setDaemon(true);
        t.setPriority(Thread.MIN_PRIORITY);
        return t;
    });

    private Instrumentation() {
    }

    public static void setup(java.lang.instrument.Instrumentation inst, Configuration config) {
        MethodExecutionAdvice.setConfig(config);

        ElementMatcher.Junction<TypeDescription> targetPackageMatcher = MethodMatcherHelper.createTargetPackageMatcher(config.getInstrumentation());
        ElementMatcher.Junction<MethodDescription> methodMatcher = MethodMatcherHelper.createMethodMatcher(config.getInstrumentation());

        new AgentBuilder.Default()
                .type(targetPackageMatcher)
                .transform(new AgentBuilder.Transformer.ForAdvice()
                        .advice(methodMatcher, MethodExecutionAdvice.class.getName()))
                .installOn(inst);

        inst.addTransformer(new ClassFileTransformer() {
            @Override
            public byte[] transform(ClassLoader loader,
                    String className,
                    Class<?> classBeingRedefined,
                    ProtectionDomain protectionDomain,
                    byte[] classfileBuffer) {
                if (className == null)
                    return null;

                Future<byte[]> future = instrumentationExecutor.submit(() -> {
                    return classfileBuffer;
                });

                try {
                    return future.get(1, TimeUnit.SECONDS);
                } catch (Exception e) {
                    return classfileBuffer;
                }
            }
        }, true);
    }

    public static void shutdown() {
        instrumentationExecutor.shutdown();
        try {
            if (!instrumentationExecutor.awaitTermination(5, TimeUnit.SECONDS)) {
                instrumentationExecutor.shutdownNow();
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            instrumentationExecutor.shutdownNow();
        }
    }
}