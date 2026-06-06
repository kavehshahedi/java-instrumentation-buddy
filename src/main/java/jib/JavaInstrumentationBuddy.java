package jib;

import jib.core.Instrumentation;
import jib.services.AgentLifecycleManager;
import jib.services.ConfigurationLoader;
import jib.models.Configuration;
import jib.services.FastAsyncLogger;
import jib.utils.ProcessInfo;

public class JavaInstrumentationBuddy {

    private JavaInstrumentationBuddy() {
    }

    public static void premain(String args, java.lang.instrument.Instrumentation inst) {
        Configuration config = ConfigurationLoader.load(args);
        if (config == null) {
            return;
        }
        FastAsyncLogger.initialize(config.getLogging(), ProcessInfo.PID);
        Instrumentation.setup(inst, config);
        AgentLifecycleManager.trackLifetime(config);
    }
}