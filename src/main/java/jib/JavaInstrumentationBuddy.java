package jib;

import java.io.File;
import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.net.URISyntaxException;
import java.net.URL;
import java.net.URLClassLoader;
import java.util.List;
import java.util.jar.JarFile;

import jib.core.Instrumentation;
import jib.services.AgentLifecycleManager;
import jib.services.ConfigurationLoader;
import jib.models.Configuration;
import jib.services.FastAsyncLogger;
import jib.utils.ProcessInfo;

public class JavaInstrumentationBuddy {

    private static boolean installed;
    private static boolean detached;

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
            System.err.println("Usage: java -jar jib.jar <pid> [config file | detach]");
            System.exit(1);
        }

        String agentJar = agentJar();
        String agentArgs = args.length < 2 ? null : args[1].equals("detach") ? args[1] : "config=" + new File(args[1]).getAbsolutePath();

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
        if ("detach".equals(args)) {
            detach();
            return;
        }
        if (installed) {
            System.err.println(detached ? "The agent was detached and cannot be loaded again in this JVM" : "The agent is already running in this JVM");
            return;
        }

        if (!isEnabled(args)) {
            return;
        }

        try {
            inst.appendToBootstrapClassLoaderSearch(new JarFile(agentJar()));
        } catch (IOException | URISyntaxException e) {
            System.err.println("Failed to add the agent to the bootstrap class path, classes of isolated class loaders are not traced: " + e);
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

    private static String agentJar() throws URISyntaxException {
        return new File(JavaInstrumentationBuddy.class.getProtectionDomain().getCodeSource().getLocation().toURI()).getPath();
    }

    @SuppressWarnings("unchecked")
    private static boolean isEnabled(String args) {
        try (URLClassLoader loader = new URLClassLoader(new URL[] { new File(agentJar()).toURI().toURL() }, null)) {
            Object config = loader.loadClass("jib.services.ConfigurationLoader").getMethod("load", String.class).invoke(null, args);
            if (config == null) {
                return false;
            }
            Object misc = config.getClass().getMethod("getMisc").invoke(config);
            return isTargetJvm((List<String>) misc.getClass().getMethod("getTargetMainClass").invoke(misc));
        } catch (Exception e) {
            System.err.println("Failed to read the configuration, the agent is disabled: " + e);
            return false;
        }
    }

    private static void detach() {
        if (!installed || detached) {
            System.err.println("The agent is not running in this JVM");
            return;
        }

        Instrumentation.reset();
        AgentLifecycleManager.stopTracking();
        detached = true;
    }

    private static boolean isTargetJvm(List<String> targetMainClasses) {
        String command = System.getProperty("sun.java.command", "") + " ";
        for (String mainClass : targetMainClasses) {
            if (command.startsWith(mainClass + " ") || command.contains("/" + mainClass + " ") || command.contains("\\" + mainClass + " ")) {
                return true;
            }
        }
        return targetMainClasses.isEmpty();
    }
}
