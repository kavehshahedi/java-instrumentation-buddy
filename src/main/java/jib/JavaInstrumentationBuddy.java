package jib;

import jib.core.Instrumentation;
import jib.services.AgentLifecycleManager;
import jib.services.ConfigurationLoader;
import jib.models.Configuration;
import jib.services.Logger;

public class JavaInstrumentationBuddy {

    private JavaInstrumentationBuddy() {
    }

    public static void premain(String args, java.lang.instrument.Instrumentation inst) {
        Configuration config = ConfigurationLoader.load(args);
        Logger.initialize(config.getLogging());
        Instrumentation.setup(inst, config);
        AgentLifecycleManager.trackLifetime(config);
    }
}