package jib.utils;

import java.lang.management.ManagementFactory;

public class ProcessInfo {

    public static final String PID = currentPid();

    private ProcessInfo() {
    }

    private static String currentPid() {
        try {
            Class<?> processHandle = Class.forName("java.lang.ProcessHandle");
            return String.valueOf(processHandle.getMethod("pid").invoke(processHandle.getMethod("current").invoke(null)));
        } catch (ReflectiveOperationException e) {
            return ManagementFactory.getRuntimeMXBean().getName().split("@")[0];
        }
    }
}
