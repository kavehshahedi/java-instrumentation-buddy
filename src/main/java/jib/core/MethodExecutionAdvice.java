package jib.core;

import net.bytebuddy.asm.Advice;
import jib.services.FastAsyncLogger;

public class MethodExecutionAdvice {
    private MethodExecutionAdvice() {
    }

    @Advice.OnMethodEnter(suppress = Throwable.class, inline = false)
    public static void onEnter(@MethodId int methodId) {
        FastAsyncLogger.log(methodId, 0);
    }

    @Advice.OnMethodExit(onThrowable = Throwable.class, suppress = Throwable.class, inline = false)
    public static void onExit(@MethodId int methodId) {
        FastAsyncLogger.log(methodId, 1);
    }
}
