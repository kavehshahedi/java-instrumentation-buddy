package kavehshahedi.jib;

import kavehshahedi.jib.helpers.MethodMatcherHelper;

import net.bytebuddy.agent.builder.AgentBuilder;
import net.bytebuddy.asm.Advice;
import net.bytebuddy.description.method.MethodDescription;
import net.bytebuddy.description.type.TypeDescription;
import net.bytebuddy.matcher.ElementMatcher;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.lang.instrument.Instrumentation;
import java.nio.file.Files;
import java.util.Set;

import org.yaml.snakeyaml.LoaderOptions;
import org.yaml.snakeyaml.Yaml;
import org.yaml.snakeyaml.constructor.Constructor;

import kavehshahedi.jib.models.Configuration;

import kavehshahedi.jib.services.Logger;
import kavehshahedi.jib.services.Time;

public class JavaInstrumentationBuddy {

    public static Logger logger;

    public static Configuration config = new Configuration();

    private JavaInstrumentationBuddy() {
    }

    public static void premain(String args, Instrumentation inst) {
        String[] options;
        if (args != null && !args.isEmpty()) {
            options = args.split(",");
            for (String option : options) {
                if (option.contains("=")) {
                    String key = option.split("=")[0];
                    String value = option.split("=")[1];

                    if (key.equals("config")) {
                        Constructor constructor = new Constructor(Configuration.class, new LoaderOptions());
                        Yaml yaml = new Yaml(constructor);
                        try (InputStreamReader reader = new InputStreamReader(
                                Files.newInputStream(new File(value).toPath()))) {
                            try {
                                config = yaml.load(reader);
                            } catch (Exception e) {
                                System.err.println("Error while parsing the configuration file");
                            }
                        } catch (IOException e) {
                            System.err.println("Error while reading the configuration file");
                        }
                    }
                }
            }
        }

        logger = new Logger(config.getLogging());

        ElementMatcher.Junction<TypeDescription> targetPackageMatcher = MethodMatcherHelper
                .createTargetPackageMatcher(config.getInstrumentation());
        ElementMatcher.Junction<MethodDescription> methodMatcher = MethodMatcherHelper
                .createMethodMatcher(config.getInstrumentation());

        System.out.println(config);

        new AgentBuilder.Default()
                .type(targetPackageMatcher)
                .transform(new AgentBuilder.Transformer.ForAdvice()
                        .advice(methodMatcher, MethodExecutionTime.class.getName()))
                .installOn(inst);

        trackAgentTime();

        System.out.println("-----------------------");
    }

    public static void trackAgentTime() {
        /*
         * This method is used to track the start and end time of the agent.
         */
        long startTime = Time.getTimeNanoSeconds();

        // Add a shutdown hook to track the end time of the agent.
        Runtime.getRuntime().addShutdownHook(new Thread() {
            public void run() {
                long endTime = Time.getTimeNanoSeconds();

                String info = String.format("{\"start_time\": %d, \"end_time\": %d}",
                        startTime, endTime);

                try (FileWriter fileWriter = new FileWriter(config.getLogging().getFile().replace(".log", ".json"))) {
                    fileWriter.write(info);
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        });
    }

    public static class MethodExecutionTime {
        public static final Set<String> visitedMethods = new java.util.HashSet<>();

        private MethodExecutionTime() {
        }

        @Advice.OnMethodEnter
        static void enter(@Advice.Origin String methodSignature) {
            if (config.getInstrumentation().isOnlyCheckVisited()) {
                if (!visitedMethods.contains(methodSignature)) {
                    visitedMethods.add(methodSignature);
                    logger.logTime(methodSignature, "ENTER");
                }
            } else {
                logger.logTime(methodSignature, "ENTER");
            }
        }

        @Advice.OnMethodExit
        static void exit(@Advice.Origin String methodSignature) {
            if (config.getInstrumentation().isOnlyCheckVisited())
                return;

            logger.logTime(methodSignature, "EXIT");
        }
    }
}