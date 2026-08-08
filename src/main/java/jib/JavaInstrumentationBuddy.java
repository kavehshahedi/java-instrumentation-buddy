package jib;

import java.io.File;
import java.lang.reflect.InvocationTargetException;
import java.net.URL;
import java.net.URLClassLoader;

import jib.core.Instrumentation;
import jib.services.AgentLifecycleManager;
import jib.services.ConfigurationLoader;
import jib.models.Configuration;
import jib.services.FastAsyncLogger;
import jib.utils.ProcessInfo;

public class JavaInstrumentationBuddy {

    private static boolean installed;

    private JavaInstrumentationBuddy() {
    }

    public static void premain(String args, java.lang.instrument.Instrumentation inst) {
        install(args, inst);
    }

    public static void agentmain(String args, java.lang.instrument.Instrumentation inst) {
        install(args, inst);
    }

    public static void main(String[] args) throws Exception {
        if (args.length < 1 || args.length > 2) {
            System.err.println("Usage: java -jar jib.jar <pid> [config file]");
            System.exit(1);
        }

        String agentJar = new File(JavaInstrumentationBuddy.class.getProtectionDomain().getCodeSource().getLocation().toURI()).getPath();
        String agentArgs = args.length == 2 ? "config=" + new File(args[1]).getAbsolutePath() : null;

        try {
            Class<?> virtualMachine = loadAttachApi();
            Object vm = virtualMachine.getMethod("attach", String.class).invoke(null, args[0]);
            try {
                virtualMachine.getMethod("loadAgent", String.class, String.class).invoke(vm, agentJar, agentArgs);
            } finally {
                virtualMachine.getMethod("detach").invoke(vm);
            }
        } catch (ClassNotFoundException e) {
            System.err.println("The attach API is not available, run the launcher with a JDK");
            System.exit(1);
        } catch (InvocationTargetException e) {
            System.err.println("Failed to attach to process " + args[0] + ": " + e.getCause());
            System.exit(1);
        }
    }

    private static Class<?> loadAttachApi() throws Exception {
        try {
            return Class.forName("com.sun.tools.attach.VirtualMachine");
        } catch (ClassNotFoundException e) {
            URL toolsJar = new File(System.getProperty("java.home"), "../lib/tools.jar").toURI().toURL();
            return Class.forName("com.sun.tools.attach.VirtualMachine", true, new URLClassLoader(new URL[] { toolsJar }));
        }
    }

    private static synchronized void install(String args, java.lang.instrument.Instrumentation inst) {
        if (installed) {
            System.err.println("The agent is already running in this JVM");
            return;
        }

        Configuration config = ConfigurationLoader.load(args);
        if (config == null) {
            return;
        }
        FastAsyncLogger.initialize(config.getLogging(), ProcessInfo.PID);
        Instrumentation.setup(inst, config);
        AgentLifecycleManager.trackLifetime(config);
        installed = true;
    }
}
