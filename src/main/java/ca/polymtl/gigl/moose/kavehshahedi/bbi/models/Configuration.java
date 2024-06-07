package ca.polymtl.gigl.moose.kavehshahedi.bbi.models;

import java.util.Collections;
import java.util.List;

public class Configuration {
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

    public static class Logging {
        private String level = "INFO"; // Default logging level
        private String file = "app.log"; // Default log file

        public Logging() {
        }

        public Logging(String level, String file) {
            this.level = level != null ? level : "INFO";
            this.file = file != null ? file : "app.log";
        }
    
        // Getters and setters
        public String getLevel() {
            return level != null ? level : "INFO";
        }
    
        public void setLevel(String level) {
            this.level = level != null ? level : "INFO";
        }
    
        public String getFile() {
            return file != null ? file : "app.log";
        }
    
        public void setFile(String file) {
            this.file = file != null ? file : "app.log";
        }
    }

    public static class Instrumentation {
        private String targetPackage = ""; // Default target package
        private TargetMethods targetMethods = new TargetMethods();

        public Instrumentation() {
        }

        public Instrumentation(String targetPackage, TargetMethods targetMethods) {
            this.targetPackage = targetPackage != null ? targetPackage : "";
            this.targetMethods = targetMethods != null ? targetMethods : new TargetMethods();
        }

        // Getters and setters
        public String getTargetPackage() {
            return targetPackage != null ? targetPackage : "";
        }
    
        public void setTargetPackage(String targetPackage) {
            this.targetPackage = targetPackage != null ? targetPackage : "";
        }
    
        public TargetMethods getTargetMethods() {
            return targetMethods != null ? targetMethods : new TargetMethods();
        }
    
        public void setTargetMethods(TargetMethods targetMethods) {
            this.targetMethods = targetMethods != null ? targetMethods : new TargetMethods();
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
        }
    }
}
