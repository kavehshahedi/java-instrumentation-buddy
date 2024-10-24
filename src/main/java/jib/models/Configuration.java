package jib.models;

import java.util.Collections;
import java.util.List;

import jib.utils.Time;

public class Configuration {

    // Constants
    public static final String DEFAULT_LOGGING_LEVEL = "INFO";
    public static final String DEFAULT_LOG_FILE = "app.log";

    private Logging logging = new Logging();
    private Instrumentation instrumentation = new Instrumentation();

    public Configuration() {
    }

    public Configuration(Logging logging, Instrumentation instrumentation) {
        this.logging = logging != null ? logging : new Logging();
        this.instrumentation = instrumentation != null ? instrumentation : new Instrumentation();
    }

    // Getters and setters
    public Logging getLogging() {
        return logging != null ? logging : new Logging();
    }

    public void setLogging(Logging logging) {
        this.logging = logging != null ? logging : new Logging();
    }

    public Instrumentation getInstrumentation() {
        return instrumentation != null ? instrumentation : new Instrumentation();
    }

    public void setInstrumentation(Instrumentation instrumentation) {
        this.instrumentation = instrumentation != null ? instrumentation : new Instrumentation();
    }

    @Override
    public String toString() {
        return "Instrumentation Configuration:\n\tLogging:\n" + logging.toString() + "\n\tInstrumentation:\n" + instrumentation.toString();
    }

    public static class Logging {
        private String level = DEFAULT_LOGGING_LEVEL;
        private String file = DEFAULT_LOG_FILE;
        private boolean addTimestampToFileNames = false;
        private long timestamp = -1;
        private boolean useHash = false;
        private boolean optimizeTimestamp = false;

        public Logging() {
        }

        public Logging(String level, String file, boolean addTimestampToFileNames, boolean useHash, boolean optimizeTimestamp) {
            this.level = level != null ? level : DEFAULT_LOGGING_LEVEL;
            this.file = file != null ? file : DEFAULT_LOG_FILE;
            this.addTimestampToFileNames = addTimestampToFileNames;
            this.useHash = useHash;
            this.optimizeTimestamp = optimizeTimestamp;
        }
    
        // Getters and setters
        public String getLevel() {
            return level != null ? level : DEFAULT_LOGGING_LEVEL;
        }
    
        public void setLevel(String level) {
            this.level = level != null ? level : DEFAULT_LOGGING_LEVEL;
        }
    
        public String getFile() {
            String fileName = file != null ? file : DEFAULT_LOG_FILE;
            if (addTimestampToFileNames) {
                if (timestamp == -1)
                    timestamp = Time.getTimeNanoSeconds();
                    
                fileName = fileName.replace(".log", "_" + timestamp + ".log");
            }

            return fileName;
        }
    
        public void setFile(String file) {
            this.file = file != null ? file : DEFAULT_LOG_FILE;
        }

        public boolean isAddTimestampToFileNames() {
            return addTimestampToFileNames;
        }

        public void setAddTimestampToFileNames(boolean addTimestampToFileNames) {
            this.addTimestampToFileNames = addTimestampToFileNames;
        }

        public boolean isUseHash() {
            return useHash;
        }

        public void setUseHash(boolean useHash) {
            this.useHash = useHash;
        }

        public boolean isOptimizeTimestamp() {
            return optimizeTimestamp;
        }

        public void setOptimizeTimestamp(boolean optimizeTimestamp) {
            this.optimizeTimestamp = optimizeTimestamp;
        }

        @Override
        public String toString() {
            return "\t\tLevel: " + level +
            "\n\t\tFile: " + file +
            "\n\t\tAdd Timestamp To File Names: " + addTimestampToFileNames +
            "\n\t\tUse Hash: " + useHash + 
            "\n\t\tOptimize Timestamp: " + optimizeTimestamp;
        }
    }

    public static class Instrumentation {
        private String targetPackage = "*"; // Default target package
        private TargetMethods targetMethods = new TargetMethods();
        private boolean onlyCheckVisited = false;
        private boolean instrumentMainMethod = false;
        private int maxNumberOfInstrumentations = -1;

        public Instrumentation() {
        }

        public Instrumentation(String targetPackage, TargetMethods targetMethods, boolean onlyCheckVisited, boolean instrumentMainMethod, int maxNumberOfInstrumentations) {
            this.targetPackage = targetPackage != null ? targetPackage : "";
            this.targetMethods = targetMethods != null ? targetMethods : new TargetMethods();
            this.onlyCheckVisited = onlyCheckVisited;
            this.instrumentMainMethod = instrumentMainMethod;
            this.maxNumberOfInstrumentations = maxNumberOfInstrumentations;
        }

        // Getters and setters
        public String getTargetPackage() {
            return targetPackage != null ? targetPackage : "*";
        }
    
        public void setTargetPackage(String targetPackage) {
            this.targetPackage = targetPackage != null ? targetPackage : "*";
        }
    
        public TargetMethods getTargetMethods() {
            return targetMethods != null ? targetMethods : new TargetMethods();
        }
    
        public void setTargetMethods(TargetMethods targetMethods) {
            this.targetMethods = targetMethods != null ? targetMethods : new TargetMethods();
        }

        public boolean isOnlyCheckVisited() {
            return onlyCheckVisited;
        }

        public void setOnlyCheckVisited(boolean onlyCheckVisited) {
            this.onlyCheckVisited = onlyCheckVisited;
        }

        public boolean isInstrumentMainMethod() {
            return instrumentMainMethod;
        }

        public void setInstrumentMainMethod(boolean instrumentMainMethod) {
            this.instrumentMainMethod = instrumentMainMethod;
        }

        public int getMaxNumberOfInstrumentations() {
            return maxNumberOfInstrumentations;
        }

        public void setMaxNumberOfInstrumentations(int maxNumberOfInstrumentations) {
            this.maxNumberOfInstrumentations = maxNumberOfInstrumentations;
        }

        @Override
        public String toString() {
            return "\t\tTarget Package: " + targetPackage +
            "\n\t\tOnly Check Visited: " + onlyCheckVisited +
            "\n\t\tInstrument Main Method: " + instrumentMainMethod +
            "\n\t\tMax Number Of Instrumentations: " + (maxNumberOfInstrumentations == -1 ? "N/A" : maxNumberOfInstrumentations) +
            "\n\t\tTarget Methods: " + targetMethods.toString();
        }
    
        public static class TargetMethods {
            private List<String> instrument = Collections.emptyList(); // Default empty list
            private List<String> ignore = Collections.emptyList(); // Default empty list

            public TargetMethods() {
            }

            public TargetMethods(List<String> instrument, List<String> ignore) {
                this.instrument = instrument != null ? instrument : Collections.emptyList();
                this.ignore = ignore != null ? ignore : Collections.emptyList();
            }

            // Getters and setters
            public List<String> getInstrument() {
                return instrument != null ? instrument : Collections.emptyList();
            }
        
            public void setInstrument(List<String> instrument) {
                this.instrument = instrument != null ? instrument : Collections.emptyList();
            }
        
            public List<String> getIgnore() {
                return ignore != null ? ignore : Collections.emptyList();
            }
        
            public void setIgnore(List<String> ignore) {
                this.ignore = ignore != null ? ignore : Collections.emptyList();
            }

            @Override
            public String toString() {
                StringBuilder output = new StringBuilder();
                output.append("\n\t\t\tInstrument:");
                output.append(instrument.isEmpty() ? " *\n" : "\n");
                for (String method : instrument) {
                    output.append("\t\t\t\t- ").append(method).append("\n");
                }

                output.append("\t\t\tIgnore:");
                output.append(ignore.isEmpty() ? " N/A\n" : "\n");
                for (String method : ignore) {
                    output.append("\t\t\t\t- ").append(method).append("\n");
                }

                return output.toString().trim();
            }
        }
    }
}
