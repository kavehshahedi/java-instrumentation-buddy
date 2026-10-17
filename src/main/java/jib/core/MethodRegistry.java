package jib.core;

import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.atomic.AtomicLong;

public class MethodRegistry {
    private static final Map<String, Integer> ids = new HashMap<>();
    private static volatile String[] names = new String[1024];
    private static volatile AtomicLong[] calls = new AtomicLong[1024];

    private MethodRegistry() {
    }

    public static synchronized int register(String signature) {
        Integer id = ids.get(signature);
        if (id == null) {
            id = ids.size();
            String[] currentNames = names;
            AtomicLong[] currentCalls = calls;
            if (id == currentNames.length) {
                currentNames = Arrays.copyOf(currentNames, id * 2);
                currentCalls = Arrays.copyOf(currentCalls, id * 2);
            }
            currentNames[id] = signature;
            currentCalls[id] = new AtomicLong();
            names = currentNames;
            calls = currentCalls;
            ids.put(signature, id);
        }
        return id;
    }

    public static String name(int id) {
        return names[id];
    }

    public static AtomicLong calls(int id) {
        return calls[id];
    }
}
