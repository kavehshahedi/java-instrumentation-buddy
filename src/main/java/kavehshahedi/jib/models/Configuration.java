package kavehshahedi.jib.models;

import java.util.Collections;
import java.util.List;

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

        public Logging() {
        }

        public Logging(String level, String file) {
            this.level = level != null ? level : DEFAULT_LOGGING_LEVEL;
            this.file = file != null ? file : DEFAULT_LOG_FILE;
        }
    
        // Getters and setters
        public String getLevel() {
            return level != null ? level : DEFAULT_LOGGING_LEVEL;
        }
    
        public void setLevel(String level) {
            this.level = level != null ? level : DEFAULT_LOGGING_LEVEL;
        }
    
        public String getFile() {
            return file != null ? file : DEFAULT_LOG_FILE;
        }
    
        public void setFile(String file) {
            this.file = file != null ? file : DEFAULT_LOG_FILE;
        }

        @Override
        public String toString() {
            return "\t\tLevel: " + level + "\n\t\tFile: " + file;
        }
    }

    public static class Instrumentation {
        private String targetPackage = "*"; // Default target package
        private TargetMethods targetMethods = new TargetMethods();
        private boolean onlyCheckVisited = false;
        private boolean instrumentMainMethod = false;

        public Instrumentation() {
        }

        public Instrumentation(String targetPackage, TargetMethods targetMethods, boolean onlyCheckVisited, boolean instrumentMainMethod) {
            this.targetPackage = targetPackage != null ? targetPackage : "";
            this.targetMethods = targetMethods != null ? targetMethods : new TargetMethods();
            this.onlyCheckVisited = onlyCheckVisited;
            this.instrumentMainMethod = instrumentMainMethod;
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

        @Override
        public String toString() {
            return "\t\tTarget Package: " + targetPackage +
            "\n\t\tOnly Check Visited: " + onlyCheckVisited +
            "\n\t\tInstrument Main Method: " + instrumentMainMethod +
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
