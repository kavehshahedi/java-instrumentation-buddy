package jib.core;

import net.bytebuddy.agent.builder.AgentBuilder;
import net.bytebuddy.agent.builder.ResettableClassFileTransformer;
import net.bytebuddy.description.method.MethodDescription;
import net.bytebuddy.description.type.TypeDescription;
import net.bytebuddy.dynamic.Nexus;
import net.bytebuddy.matcher.ElementMatcher;
import net.bytebuddy.utility.JavaModule;

import jib.models.Configuration;
import jib.helpers.MethodMatcherHelper;

public class Instrumentation {
    private static java.lang.instrument.Instrumentation instrumentation;
    private static ResettableClassFileTransformer transformer;

    private Instrumentation() {
    }

    public static void setup(java.lang.instrument.Instrumentation inst, Configuration config) {
        MethodExecutionAdvice.setConfig(config);

        ElementMatcher.Junction<TypeDescription> targetPackageMatcher = MethodMatcherHelper.createTargetPackageMatcher(config.getInstrumentation());
        ElementMatcher.Junction<MethodDescription> methodMatcher = MethodMatcherHelper.createMethodMatcher(config.getInstrumentation());

        System.setProperty(Nexus.PROPERTY, Boolean.TRUE.toString());
        instrumentation = inst;
        transformer = new AgentBuilder.Default()
                .disableClassFormatChanges()
                .with(AgentBuilder.RedefinitionStrategy.RETRANSFORMATION)
                .with(new AgentBuilder.Listener.Adapter() {
                    @Override
                    public void onError(String typeName, ClassLoader classLoader, JavaModule module, boolean loaded, Throwable throwable) {
                        System.err.println("Failed to instrument " + typeName + ": " + throwable);
                    }
                })
                .type(targetPackageMatcher)
                .transform(new AgentBuilder.Transformer.ForAdvice()
                        .include(ClassLoader.getSystemClassLoader())
                        .advice(methodMatcher, MethodExecutionAdvice.class.getName()))
                .installOn(inst);
    }

    public static void reset() {
        transformer.reset(instrumentation, AgentBuilder.RedefinitionStrategy.RETRANSFORMATION);
    }
}