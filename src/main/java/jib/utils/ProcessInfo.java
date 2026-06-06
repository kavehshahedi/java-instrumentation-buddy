package jib.utils;

import java.lang.management.ManagementFactory;

public class ProcessInfo {

    public static final String PID = ManagementFactory.getRuntimeMXBean().getName().split("@")[0];

    private ProcessInfo() {
    }
}
