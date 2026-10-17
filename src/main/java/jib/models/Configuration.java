package jib.models;

import java.util.Collections;
import java.util.List;

import jib.utils.ProcessInfo;
import jib.utils.Time;

public class Configuration {

    // Constants
    public static final String DEFAULT_LOG_FILE = "jib-%p.log";

    private Logging logging = new Logging();
    private Instrumentation instrumentation = new Instrumentation();
    private Misc misc = new Misc();

    // Getters and setters
    public Logging getLogging() {
        return logging != null ? logging : new Logging();
    }

    public void setLogging(Logging logging) {
        this.logging = logging != null ? logging : new Logging();
    }

    public Misc getMisc() {
        return misc != null ? misc : new Misc();
    }

    public void setMisc(Misc misc) {
        this.misc = misc != null ? misc : new Misc();
    }

    public Instrumentation getInstrumentation() {
        return instrumentation != null ? instrumentation : new Instrumentation();
    }

    public void setInstrumentation(Instrumentation instrumentation) {
        this.instrumentation = instrumentation != null ? instrumentation : new Instrumentation();
    }

    public static class Logging {
        private String file = DEFAULT_LOG_FILE;
        private boolean addTimestampToFileNames = false;
        private long timestamp = -1;
        private boolean useHash = false;
        private boolean optimizeTimestamp = false;
        private int batchSize = 4096;
        private int queueDepth = 64;
        private int flushIntervalMs = 100;
        private int bufferSize = 2 * 1024 * 1024; // 2MB

        // Getters and setters
        public String getFile() {
            String fileName = (file != null ? file : DEFAULT_LOG_FILE).replace("%p", ProcessInfo.PID);
            if (addTimestampToFileNames) {
                if (timestamp == -1)
                    timestamp = Time.getTimeNanoSeconds();

                int extension = extensionIndex(fileName);
                fileName = fileName.substring(0, extension) + "_" + timestamp + fileName.substring(extension);
            }

            return fileName;
        }

        public String getMetadataFile() {
            String fileName = getFile();
            String metadataFile = fileName.substring(0, extensionIndex(fileName)) + ".json";
            return metadataFile.equals(fileName) || metadataFile.equals(getJsonFile()) ? fileName + ".meta.json" : metadataFile;
        }

        public String getJsonFile() {
            return getFile() + ".json";
        }

        private static int extensionIndex(String fileName) {
            int dot = fileName.lastIndexOf('.');
            int separator = Math.max(fileName.lastIndexOf('/'), fileName.lastIndexOf('\\'));
            return dot > separator + 1 ? dot : fileName.length();
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

        public int getBatchSize() {
            return batchSize;
        }

        public void setBatchSize(int batchSize) {
            this.batchSize = batchSize > 0 ? batchSize : 4096;
        }

        public int getQueueDepth() {
            return queueDepth;
        }

        public void setQueueDepth(int queueDepth) {
            this.queueDepth = queueDepth > 0 ? queueDepth : 64;
        }

        public int getFlushIntervalMs() {
            return flushIntervalMs;
        }

        public void setFlushIntervalMs(int flushIntervalMs) {
            this.flushIntervalMs = flushIntervalMs > 0 ? flushIntervalMs : 100;
        }

        public int getBufferSize() {
            return bufferSize;
        }

        public void setBufferSize(int bufferSize) {
            this.bufferSize = bufferSize > 0 ? bufferSize : 2 * 1024 * 1024;
        }
    }

    public static class Instrumentation {
        private String targetPackage = "*"; // Default target package
        private TargetMethods targetMethods = new TargetMethods();
        private boolean onlyCheckVisited = false;
        private boolean instrumentMainMethod = false;
        private int maxNumberOfInstrumentations = -1;

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
    
        public static class TargetMethods {
            private List<String> instrument = Collections.emptyList(); // Default empty list
            private List<String> ignore = Collections.emptyList(); // Default empty list

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

    public static class Misc {
        private boolean convertToJson = false;
        private List<String> targetMainClass = Collections.emptyList();

        // Getters and setters
        public boolean isConvertToJson() {
            return convertToJson;
        }

        public void setConvertToJson(boolean convertToJson) {
            this.convertToJson = convertToJson;
        }

        public List<String> getTargetMainClass() {
            return targetMainClass;
        }

        public void setTargetMainClass(List<String> targetMainClass) {
            this.targetMainClass = targetMainClass != null ? targetMainClass : Collections.emptyList();
        }
    }
}
