package ca.polymtl.gigl.moose.kavehshahedi.bbi;

import net.bytebuddy.agent.builder.AgentBuilder;
import net.bytebuddy.asm.Advice;
import net.bytebuddy.description.method.MethodDescription;
import net.bytebuddy.description.type.TypeDescription;
import net.bytebuddy.matcher.ElementMatcher;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.lang.instrument.Instrumentation;
import java.util.Set;

import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.core.LoggerContext;
import org.yaml.snakeyaml.LoaderOptions;
import org.yaml.snakeyaml.Yaml;
import org.yaml.snakeyaml.constructor.Constructor;
import org.apache.logging.log4j.LogManager;

import ca.polymtl.gigl.moose.kavehshahedi.bbi.helpers.MethodMatcherHelper;
import ca.polymtl.gigl.moose.kavehshahedi.bbi.models.Configuration;

public class FunctionEntryAgent {

    public static Logger logger;

    public static Configuration config;

    public static final long TIME_OFFSET;
    static {
        long currentTimeMillis = System.currentTimeMillis();
        long currentNanoTime = System.nanoTime();
        TIME_OFFSET = (currentTimeMillis * 1_000_000) - currentNanoTime;
    }

    public static void premain(String args, Instrumentation inst) {
        config = new Configuration();

        String[] options;
        if (args != null && !args.isEmpty()) {
            options = args.split(",");
            for (String option : options) {
                if (option.contains("=")) {
                    String key = option.split("=")[0];
                    String value = option.split("=")[1];

                    switch (key) {
                        case "config":
                            Constructor constructor = new Constructor(Configuration.class, new LoaderOptions());
                            Yaml yaml = new Yaml(constructor);
                            try (InputStreamReader reader = new InputStreamReader(
                                    new FileInputStream(new File(value)))) {
                                try {
                                    config = yaml.load(reader);
                                } catch (Exception e) {
                                    System.err.println("Error while parsing the configuration file");
                                }
                            } catch (IOException e) {
                                System.err.println("Error while reading the configuration file");
                            }

                            break;
                        default:
                            break;
                    }
                }
            }
        }

        configureLoggerName(config.getLogging());

        ElementMatcher.Junction<TypeDescription> targetPackageMatcher = MethodMatcherHelper.createTargetPackageMatcher(config.getInstrumentation());
        ElementMatcher.Junction<MethodDescription> methodMatcher = MethodMatcherHelper.createMethodMatcher(config.getInstrumentation());

        System.out.println(config);

        new AgentBuilder.Default()
                .type(targetPackageMatcher)
                .transform(new AgentBuilder.Transformer.ForAdvice()
                        .advice(methodMatcher, MethodExecutionTime.class.getName()))
                .installOn(inst);

        System.out.println("-".repeat(20));
    }

    public static void configureLoggerName(Configuration.Logging loggingInfo) {
        String logFileName = loggingInfo.getFile();
        System.setProperty("logFilename", logFileName);

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
            if (config.getInstrumentation().isOnlyCheckVisited()) {
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
            if (config.getInstrumentation().isOnlyCheckVisited())
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