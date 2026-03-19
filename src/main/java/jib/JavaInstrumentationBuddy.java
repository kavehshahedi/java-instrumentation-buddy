package jib;

import jib.core.Instrumentation;
import jib.core.MethodExecutionAdvice;
import jib.services.AgentLifecycleManager;
import jib.services.ConfigurationLoader;
import jib.models.Configuration;
import jib.services.FastAsyncLogger;

public class JavaInstrumentationBuddy {

    private JavaInstrumentationBuddy() {
    }

    public static void premain(String args, java.lang.instrument.Instrumentation inst) {
        Configuration config = ConfigurationLoader.load(args);
        FastAsyncLogger.initialize(config.getLogging(), MethodExecutionAdvice.PROCESS_ID);
        Instrumentation.setup(inst, config);
        AgentLifecycleManager.trackLifetime(config);
    }
}