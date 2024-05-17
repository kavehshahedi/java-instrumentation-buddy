package ca.polymtl.gigl.moose.kavehshahedi.bbi;

import net.bytebuddy.agent.builder.AgentBuilder;
import net.bytebuddy.asm.Advice;
import net.bytebuddy.matcher.ElementMatchers;

import java.io.IOException;
import java.lang.instrument.Instrumentation;
import java.util.Set;

import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.core.LoggerContext;
import org.apache.logging.log4j.LogManager;

public class FunctionEntryAgent {

    public static Logger logger;

    public static boolean onlyCheckVisited = false;
    public static String logFileName = "logs.log";
    
    public static final long TIME_OFFSET;
    static {
        long currentTimeMillis = System.currentTimeMillis();
        long currentNanoTime = System.nanoTime();
        TIME_OFFSET = (currentTimeMillis * 1_000_000) - currentNanoTime;
    }

    public static void premain(String args, Instrumentation inst) throws SecurityException, IOException {
        String targetPackage = "";

        String options[];
        if (args != null && args.length() > 0) {
            options = args.split(",");
            if (options.length > 0) {
                for (String option : options) {
                    if (option.contains("=")) {
                        String key = option.split("=")[0];
                        String value = option.split("=")[1];

                        switch (key) {
                            case "package":
                                targetPackage = value;
                                break;

                            case "onlyCheckVisited":
                                onlyCheckVisited = Boolean.parseBoolean(value);
                                break;

                            case "logFileName":
                                logFileName = value;
                                break;

                            default:
                                break;
                        }
                    }
                }
            }
        }

        configureLoggerName(logFileName);

        new AgentBuilder.Default()
                .type(ElementMatchers.nameStartsWith(targetPackage))
                .transform(new AgentBuilder.Transformer.ForAdvice()
                        .advice(ElementMatchers.isMethod(),
                                MethodExecutionTime.class.getName()))
                .installOn(inst);
    }

    public static void configureLoggerName(String loggerName) {
        System.setProperty("logFilename", "logs/" + loggerName);

        LoggerContext ctx = (LoggerContext) LogManager.getContext(false);
        ctx.reconfigure();

        logger = LogManager.getLogger(FunctionEntryAgent.class);
    }

    public static long getTimeNanoSeconds() {
        return System.nanoTime() + TIME_OFFSET;
    }

    public static class MethodExecutionTime {
        public static final Set<String> visitedMethods = new java.util.HashSet<String>();

        @Advice.OnMethodEnter
        static void enter(@Advice.Origin String methodSignature) {
            if (onlyCheckVisited) {
                if (!visitedMethods.contains(methodSignature)) {
                    visitedMethods.add(methodSignature);
                    logTime(methodSignature, "ENTER");
                }
            } else {
                logTime(methodSignature, "ENTER");
            }
        }

        @Advice.OnMethodExit
        static void exit(@Advice.Origin String methodSignature) {
            if (onlyCheckVisited)
                return;

            logTime(methodSignature, "EXIT");
        }

        static final String LOG_PATTERN = "[%d] %s %s";

        public static void logTime(String methodSignature, String type) {
            String message = String.format(LOG_PATTERN,
                    getTimeNanoSeconds(),
                    type,
                    methodSignature);

            logger.info(message);
        }
    }
}