package jib.core;

import net.bytebuddy.agent.builder.AgentBuilder;
import net.bytebuddy.description.method.MethodDescription;
import net.bytebuddy.description.type.TypeDescription;
import net.bytebuddy.matcher.ElementMatcher;

import jib.models.Configuration;
import jib.helpers.MethodMatcherHelper;

public class Instrumentation {
    private Instrumentation() {}

    public static void setup(java.lang.instrument.Instrumentation inst, Configuration config) {
        MethodExecutionAdvice.setConfig(config);

        ElementMatcher.Junction<TypeDescription> targetPackageMatcher =
                MethodMatcherHelper.createTargetPackageMatcher(config.getInstrumentation());
        ElementMatcher.Junction<MethodDescription> methodMatcher =
                MethodMatcherHelper.createMethodMatcher(config.getInstrumentation());

        new AgentBuilder.Default()
                .type(targetPackageMatcher)
                .transform(new AgentBuilder.Transformer.ForAdvice()
                        .advice(methodMatcher, MethodExecutionAdvice.class.getName()))
                .installOn(inst);
    }
}
