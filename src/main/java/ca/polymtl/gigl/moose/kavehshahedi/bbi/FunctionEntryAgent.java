package ca.polymtl.gigl.moose.kavehshahedi.bbi;

import net.bytebuddy.agent.builder.AgentBuilder;
import net.bytebuddy.asm.Advice;
import net.bytebuddy.matcher.ElementMatchers;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.Writer;
import java.lang.instrument.Instrumentation;
import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.concurrent.CompletableFuture;
import java.util.function.Supplier;

public class FunctionEntryAgent {

    public static void premain(String args, Instrumentation inst) throws SecurityException, IOException {
        clearLogFile();

        String targetPackage = args;
        
        new AgentBuilder.Default()
                .type(ElementMatchers.nameStartsWith(targetPackage))
                .transform(new AgentBuilder.Transformer.ForAdvice()
                        .advice(ElementMatchers.isMethod(),
                                MethodExecutionTime.class.getName()))
                .installOn(inst);
    }

    public static class MethodExecutionTime {
        private static final String LOG_FILE = "method_logs.log";

        @Advice.OnMethodEnter
        static void enter(@Advice.Origin Method method) {
            logTime(method, "ENTER");
        }

        @Advice.OnMethodExit
        static void exit(@Advice.Origin Method method) {
            logTime(method, "EXIT");
        }

        public static void logTime(Method method, String type) {
            StringBuilder messageBuilder = new StringBuilder();
            messageBuilder.append("[").append(System.nanoTime()).append("] ")
                    .append(type).append(" ")
                    .append(method.getDeclaringClass().getName()).append(".").append(method.getName()).append(" ")
                    .append("(").append(Arrays.toString(method.getParameterTypes())).append(") ")
                    .append(method.getReturnType().getName());

            CompletableFuture.runAsync(() -> writeLog(messageBuilder.toString()));
            // writeLog(messageBuilder.toString()); // If the java version for compiling is 1.6 or 1.7, use this line instead of the above line
        }

        public static class LoggerSupplier implements Supplier<Void> {
            private final String message;

            public LoggerSupplier(String message) {
                this.message = message;
            }

            @Override
            public Void get() {
                try (Writer writer = new BufferedWriter(new FileWriter(LOG_FILE, true))) {
                    writer.write(message + "\n");
                } catch (IOException e) {
                    e.printStackTrace();
                }

                return null;
            }
        }
    }

    public static void writeLog(String message) {
        try (Writer writer = new BufferedWriter(new FileWriter(MethodExecutionTime.LOG_FILE, true))) {
            writer.write(message + "\n");
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public static void clearLogFile() {
        // Remove the log file if it exists from the system
        File file = new File("method_logs.log");
        if (file.exists()) {
            file.delete();
        }
    }
}