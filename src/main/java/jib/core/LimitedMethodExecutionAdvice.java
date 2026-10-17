package jib.core;

import java.util.concurrent.atomic.AtomicLong;

import net.bytebuddy.asm.Advice;
import jib.services.FastAsyncLogger;

public class LimitedMethodExecutionAdvice {
    public static long maxInstrumentations = Long.MAX_VALUE;
    public static boolean isOnlyCheckVisited;

    private LimitedMethodExecutionAdvice() {
    }

    @Advice.OnMethodEnter(suppress = Throwable.class, inline = false)
    public static boolean onEnter(@MethodId int methodId) {
        AtomicLong calls = MethodRegistry.calls(methodId);
        if (isOnlyCheckVisited) {
            if (maxInstrumentations > 0 && calls.get() == 0 && calls.compareAndSet(0, 1)) {
                FastAsyncLogger.log(methodId, 0);
            }
            return false;
        }
        if (calls.get() >= maxInstrumentations || calls.getAndIncrement() >= maxInstrumentations) {
            return false;
        }
        FastAsyncLogger.log(methodId, 0);
        return true;
    }

    @Advice.OnMethodExit(onThrowable = Throwable.class, suppress = Throwable.class, inline = false)
    public static void onExit(@MethodId int methodId, @Advice.Enter boolean wasLogged) {
        if (wasLogged) {
            FastAsyncLogger.log(methodId, 1);
        }
    }
}
