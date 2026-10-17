# JIB: Java Instrumentation Buddy

JIB is a lightweight Java agent designed to seamlessly instrument functions in compiled Java applications at the bytecode level. Leveraging the power of [Byte Buddy](https://github.com/raphw/byte-buddy/tree/master) as its core, JIB offers efficient, lightweight, and application-level function instrumentation with minimal overhead.

## Features

- Non-intrusive bytecode manipulation
- Configurable instrumentation targets
- Flexible logging options
- Method-level granularity for instrumentation
- Seamless integration with existing Java applications
- Attaching to an already running JVM without restarting it
- Compatible with Java 8 and above

## Building

To build and install JIB, ensure you have Maven and JDK 9 or newer installed on your system (the agent itself runs on Java 8 and above). Then, execute the following command in the project root directory:

```console
mvn clean install
```

This command compiles the source code, runs the integration tests, and installs the artifact in your local Maven repository. The integration tests run the agent on the JDK used by Maven. To run them on another JDK, pass its `java` executable:

```console
mvn clean verify -Djib.it.java=/path/to/jdk/bin/java
```

### Creating a Deployable JAR

To use JIB as an agent attached to your Java application, you'll need to package it as a JAR file. Generate the JAR with this command:

```console
mvn clean package
```

Upon successful execution, you'll find the `jib.jar` file in the `/target` directory, ready for deployment.

## Usage
You can attach the agent to your Java application when executing it. Here is a sample structure for attaching the agent to your Java program.
```console
# Running the program normally
java [OPTIONS] -jar <program commands>

# Attaching the agent to your program with a configuration file
java [OPTIONS] -javaagent:path/to/the/jib.jar=config=configuration.properties -jar <program commands>

# Or passing the options directly
java [OPTIONS] -javaagent:path/to/the/jib.jar=instrumentation.targetPackage=com.example,logging.file=trace.log -jar <program commands>
```

Both can be combined, and options passed directly override the ones of the configuration file (e.g., `config=configuration.properties,logging.file=other.log`). Since the agent needs no file this way, it can also be enabled through the `JAVA_TOOL_OPTIONS` environment variable without changing the command of the application:

```console
export JAVA_TOOL_OPTIONS="-javaagent:path/to/the/jib.jar=instrumentation.targetPackage=com.example"
```

Every JVM started with this variable loads the agent, and the JVM prints a `Picked up JAVA_TOOL_OPTIONS` line when it starts. To trace only your application, and not the build tools or other JVMs started from the same environment, set `misc.targetMainClass` (see below).

Without any option, the agent uses the default values described below. An empty configuration file also uses the default values. If the configuration file cannot be read, or an option is unknown or has an invalid value, the agent prints an error and stays disabled, and the application runs normally.

### Attaching to a Running JVM

You can also load the agent into a JVM that is already running, using its process id:

```console
java -jar path/to/the/jib.jar <pid> [configuration.properties]
```

Classes that are already loaded are instrumented as well. Relative paths inside the configuration file (such as the log file) are resolved against the working directory of the target JVM. The launcher must run on a JDK, and the agent can only be loaded once per JVM. Starting with JDK 21, the target JVM prints a warning when an agent is loaded this way, and future JDK releases are expected to require starting the JVM with `-XX:+EnableDynamicAgentLoading` for it to work.

To stop tracing without restarting the application, detach the agent:

```console
java -jar path/to/the/jib.jar <pid> detach
```

The agent restores the original code of the instrumented classes, and writes the log, metadata and JSON files right away instead of at exit. This works whether the agent was attached at runtime or started with `-javaagent`, so you can attach, trace for a while, and detach. Calls that are still running when the agent detaches only have their start in the log. Once detached, the agent cannot be loaded again in the same JVM.

### Class Loaders and Application Servers

The agent adds itself to the bootstrap class path, so that it can also trace classes of isolated class loaders, which do not delegate to the system class loader (e.g., plugin systems). Because of this, recent JDKs (e.g., 17, 21 and 25) print `Sharing is only supported for boot loader classes because bootstrap classpath has been appended` when the agent starts, and class data sharing then only applies to the classes of the JDK core. This does not happen in JVMs where `misc.targetMainClass` keeps the agent inactive.

Module systems that only expose some packages of the bootstrap class path to their modules also need the `jib` packages to be exposed, such as `-Dorg.osgi.framework.bootdelegation=jib.*` for OSGi frameworks, or `-Djboss.modules.system.pkgs=jib` for JBoss Modules (e.g., WildFly).

### Agent Configuration

The configuration file is a standard Java properties file, with one `key=value` option per line. Lines starting with `#` are comments, and a long value can continue on the next line by ending the line with `\`. A backslash starts an escape sequence in properties files, so write Windows paths with `/` or `\\` (e.g., `C:/traces/trace.log`). Options passed directly to the agent are separated by commas. Below are the available options:

#### Logging

The `logging` options control how the agent generates log files:

- `logging.file`: Specifies the path to the log file where the agent will write its output. `%p` in the path is replaced by the process id, so that several JVMs can run with the same configuration. The file is created when the first event is logged, and an existing file with the same name is overwritten.
  - Default: `jib-%p.log`

- `logging.addTimestampToFileNames`: When set to `true`, adds a timestamp to the log file name, right before its extension (e.g., `trace_1792265583180000709.log`).
  - Default: `false`

- `logging.useHash`: If `true`, the agent uses hashing for method signatures in the log file. This is useful for reducing the size of the log file when there are a large number of method logs with long signatures. The mapping of the hashes is stored in the metadata file, and the JSON conversion writes the full signatures.
  - Default: `false`

- `logging.optimizeTimestamp`: When set to `true`, optimizes timestamp handling for improved performance. Basically, it removes the first 4 digits of the timestamp to reduce the log file size. The removed part is stored as `log_time_difference` in the metadata file.
  - Default: `false`

- `logging.batchSize`: Specifies the number of log entries in each buffer of a thread (16 bytes per entry). Each thread fills its own buffers without locking, starting with a small one, and a full buffer is handed to the writer. Larger values can improve throughput but increase the memory used by each thread.
  - Default: `4096`

- `logging.queueDepth`: Sets the maximum number of full buffers waiting to be written. This acts as a buffer between log generation and disk I/O. When it is reached, new events are dropped instead of slowing down the application, and the number of dropped events is printed at exit and stored as `dropped_events` in the metadata file. Increase this value if you experience dropped logs under high load.
  - Default: `64`

- `logging.flushIntervalMs`: Controls how long (in milliseconds) the logger waits for a full buffer before writing partially filled buffers. Lower values reduce latency but may increase I/O overhead.
  - Default: `100`

- `logging.bufferSize`: Sets the size of the buffer used for writing log entries to disk (in bytes). Larger buffers can improve write performance but consume more memory.
  - Default: `2097152` (2MB)

When the application exits, the agent also writes a metadata file next to the log file, named after it with a `.json` extension (e.g., `trace.json` for `trace.log`). It contains the start and end times of the agent, the `log_time_difference` of `logging.optimizeTimestamp`, the number of dropped events, and the mapping of the method hashes.

#### Instrumentation

The `instrumentation` options define which parts of your code the agent will instrument:

- `instrumentation.targetPackage`: Specifies which package to instrument, including its subpackages. Use a comma-separated list to instrument several packages (e.g., `com.example.app, com.example.lib`).
  - Default: `*` (all packages)

- `instrumentation.onlyCheckVisited`: When set to `true`, the agent only logs the entry of each function the first time it runs. This is useful for checking code coverage.
  - Default: `false`

- `instrumentation.instrumentMainMethod`: If `true`, the agent will instrument the main method of the main class.
  - Default: `false`

- `instrumentation.maxNumberOfInstrumentations`: Sets a limit on the number of calls logged for each method. Use -1 for unlimited.
  - Default: `-1`

- `instrumentation.targetMethods.instrument`: A comma-separated list of methods to instrument. If specified, only these methods will be instrumented.
  - Example:
    ```properties
    instrumentation.targetMethods.instrument=private static void com.example.MainClass.methodName(int a, java.lang.String b), \
        public int com.example.MainClass.otherMethod()
    ```

- `instrumentation.targetMethods.ignore`: A comma-separated list of methods to exclude from instrumentation. If specified, all methods except these will be instrumented.
  - Example:
    ```properties
    instrumentation.targetMethods.ignore=protected java.lang.String com.example.MainClass.ignoredMethodName(float a)
    ```

#### Misc

The `misc` options contain additional settings:

- `misc.convertToJson`: When set to `true`, converts the output logs to JSON format when the application exits, next to the log file (e.g., `trace.log.json` for `trace.log`). This format is supported by visualization tools like Eclipse Trace Compass.
  - Default: `false`

- `misc.targetMainClass`: A comma-separated list of main classes or jar files (e.g., `com.example.Main, app.jar`). When set, the agent is only enabled in JVMs that run one of them, and stays inactive in every other JVM without instrumenting anything. A jar file matches with or without its directory.
  - Default: empty (every JVM)

##### Method Structure

When specifying methods in the `instrument` or `ignore` lists, use the following format:

```
[visibility] [static] return-type [declaring-class.]method-name(args)
```

Where:
- `[visibility]` is one of (required):
    - `public`
    - `protected`
    - `private`
    - (empty for package-protected)
- `[static]` is one of (required):
    - `static`
    - (empty for non-static)
- `return-type` is the method's return type
    - it should be the fully qualified class name (e.g., `java.lang.String` or `void`) (required)
- `[declaring-class]` is the fully qualified class name where the method is declared (optional)
- `method-name` is the name of the method (required)
- `(args)` are the method's parameters, with fully qualified types (required)

The return type and the parameter types are matched exactly, so overloaded methods can be told apart. Generic type arguments are ignored, and types are written as they appear in the trace (e.g., `com.example.Outer$Inner` for a nested class). The commas inside the parentheses and the generic type arguments of a method do not separate the methods of the list.

Examples:
```java
private static void com.example.MainClass.methodName(int a, java.lang.String b)
protected java.lang.String ignoredMethodName(float a)
public java.util.List<java.lang.String> getNames()
void processData(byte[] data)
```
Note: You can specify either `instrument` or `ignore`, but not both since it doesn't make sense to instrument and ignore methods at the same time.

Here's an example of a complete configuration file:

```properties
logging.file=trace.log
logging.addTimestampToFileNames=true
logging.useHash=true
logging.optimizeTimestamp=true

instrumentation.targetPackage=com.example
instrumentation.onlyCheckVisited=false
instrumentation.instrumentMainMethod=true
instrumentation.maxNumberOfInstrumentations=1000
instrumentation.targetMethods.instrument=private static void com.example.MainClass.methodName(int a, java.lang.String b)

misc.convertToJson=true
```

#### Migrating from the YAML Configuration

Versions of JIB before 1.5.0 read the configuration from a YAML file. Each YAML option becomes one key, made of its section names and its name joined by dots, and YAML lists become comma-separated values. For instance, this YAML configuration:

```yaml
logging:
  file: trace.log
instrumentation:
  targetPackage: com.example
  targetMethods:
    ignore:
      - void com.example.Main.first()
      - void com.example.Main.second()
```

becomes:

```properties
logging.file=trace.log
instrumentation.targetPackage=com.example
instrumentation.targetMethods.ignore=void com.example.Main.first(), void com.example.Main.second()
```

## Example

Let's consider a simple Java program with the following structure:

```java
package com.example.pkg;

import java.util.HashMap;

public class Main {
    public static void main(String[] args) {
        firstMethod();

        Main main = new Main();
        main.secondMethod();
        main.thirdMethod(1, 2);
        main.fourthMethod();
        fifthMethod(new HashMap<String, Integer>(), "key");
    }

    private static void firstMethod() {
        System.out.println("First Method");
    }

    String secondMethod() {
        return "Second Method";
    }

    public int thirdMethod(int a, int b) {
        return a + b;
    }

    protected Another fourthMethod() {
        return new Another();
    }

    private static void fifthMethod(HashMap<String, Integer> map, String key) {
        if (map.containsKey(key)) {
            System.out.println("Key exists");
        } else {
            System.out.println("Key does not exist");
        }

        return;
    }
}
```

### Full Instrumentation

Suppose we want to instrument the program to log the entry and exit of each method. We can create a configuration file to specify the methods to instrument and the logging options. For now, let's just instrument all the methods in the `com.example.pkg` package and log the output to a file.

#### Configuration File
    
```properties
logging.file=trace.log
instrumentation.targetPackage=com.example.pkg
```

After the instrumentation, the agent will store all the logs in a single file (i.e., `trace.log`). Each line shows a function entry or exit along with the S/E (start or end) time, the process and thread ids, and the function's signature. Exits caused by exceptions are logged as well.
```
[TIME_NANO_SECONDS] S|E [PROCESS_ID][THREAD_ID] "FUNCTION_SIGNATURE"
```
```
[1724605175635717866] S [12345][1] "public static void com.example.pkg.Main.main(java.lang.String[])"
[1724605175637779321] S [12345][1] "private static void com.example.pkg.Main.firstMethod()"
[1724605175637903541] E [12345][1] "private static void com.example.pkg.Main.firstMethod()"
[1724605175637944911] S [12345][1] "java.lang.String com.example.pkg.Main.secondMethod()"
[1724605175637971154] E [12345][1] "java.lang.String com.example.pkg.Main.secondMethod()"
[1724605175637995449] S [12345][1] "public int com.example.pkg.Main.thirdMethod(int,int)"
[1724605175638028792] E [12345][1] "public int com.example.pkg.Main.thirdMethod(int,int)"
[1724605175638069161] S [12345][1] "protected com.example.pkg.Another com.example.pkg.Main.fourthMethod()"
[1724605175640295270] E [12345][1] "protected com.example.pkg.Another com.example.pkg.Main.fourthMethod()"
[1724605175640359832] S [12345][1] "private static void com.example.pkg.Main.fifthMethod(java.util.HashMap,java.lang.String)"
[1724605175640388900] E [12345][1] "private static void com.example.pkg.Main.fifthMethod(java.util.HashMap,java.lang.String)"
[1724605175640410797] E [12345][1] "public static void com.example.pkg.Main.main(java.lang.String[])"
```

### Partial Instrumentation

Now, we may specify the methods to instrument or ignore in the configuration file. For instance, we can instrument only the `main` method and the `thirdMethod` in the `Main` class:

#### Configuration File
    
```properties
logging.file=trace.log
instrumentation.targetPackage=com.example.pkg
instrumentation.instrumentMainMethod=true
instrumentation.targetMethods.instrument=public int com.example.pkg.Main.thirdMethod(int a, int b)
```

After running the program with this configuration, the log file will only contain entries for the `main` and `thirdMethod` functions.
```
[1724605175635717866] S [12345][1] "public static void com.example.pkg.Main.main(java.lang.String[])"
[1724605175637995449] S [12345][1] "public int com.example.pkg.Main.thirdMethod(int,int)"
[1724605175638028792] E [12345][1] "public int com.example.pkg.Main.thirdMethod(int,int)"
[1724605175640410797] E [12345][1] "public static void com.example.pkg.Main.main(java.lang.String[])"
```

### Use Hashing for Method Signatures
If you want to use hashing for method signatures in the log file, you can enable the `logging.useHash` option in the configuration file. This option is useful when there are a large number of method logs with long signatures.

#### Configuration File
    
```properties
logging.file=trace.log
logging.useHash=true
instrumentation.targetPackage=com.example.pkg
```

After running the program with this configuration, the log file will contain hashed method signatures instead of the full method signatures. The mapping of the hashes will be stored in the metadata file.

- Log file:
    ```
    [1724685951931233870] S [12345][1] "A"
    [1724685951933340359] S [12345][1] "B"
    [1724685951933525189] E [12345][1] "B"
    [1724685951933581572] S [12345][1] "C"
    [1724685951933615793] E [12345][1] "C"
    [1724685951933641228] S [12345][1] "D"
    [1724685951933668752] E [12345][1] "D"
    [1724685951933730567] S [12345][1] "E"
    [1724685951936219996] E [12345][1] "E"
    [1724685951936283964] S [12345][1] "F"
    [1724685951936315634] E [12345][1] "F"
    [1724685951936338337] E [12345][1] "A"
    ```

- Log metadata (`trace.json`)
    ```json
    {
        "start_time": 1724685951888001220,
        "end_time": 1724685951936780901,
        "log_time_difference": 0,
        "dropped_events": 0,
        "method_signature_hash": {
            "public int com.example.pkg.Main.thirdMethod(int,int)": "D",
            "java.lang.String com.example.pkg.Main.secondMethod()": "C",
            "protected com.example.pkg.Another com.example.pkg.Main.fourthMethod()": "E",
            "public static void com.example.pkg.Main.main(java.lang.String[])": "A",
            "private static void com.example.pkg.Main.firstMethod()": "B",
            "private static void com.example.pkg.Main.fifthMethod(java.util.HashMap,java.lang.String)": "F"
        }
    }
    ```

## Analysis
There are several ways to analyze the logs generated by the agent. One common approach is to use visualization tools like [Eclipse Trace Compass™](https://eclipse.dev/tracecompass) to gain insights into the program's execution flow and performance characteristics. You can also write custom scripts to parse and analyze the logs based on your specific requirements.

### Analysis with Trace Compass
*Background: [Eclipse Trace Compass™](https://eclipse.dev/tracecompass) is an open source application to solve performance and reliability issues by reading and analyzing logs or traces of a system. Its goal is to provide views, graphs, metrics, and more to help extract useful information from traces, in a way that is more user-friendly and informative than huge text dumps.*

If you want to import the collected instrumentation logs in Trace Compass, you can use the `misc.convertToJson` option in the configuration file to convert the output logs to JSON format. 

**JSON Output Example:**
```json
[
    {
        "ts": 1724686484544229.333,
        "ph": "B",
        "name": "public static void com.example.pkg.Main.main(java.lang.String[])",
        "pid": 12345,
        "tid": 1
    },
    {
        "ts": 1724686484546125.563,
        "ph": "B",
        "name": "private static void com.example.pkg.Main.firstMethod()",
        "pid": 12345,
        "tid": 1
    },
    {
        "ts": 1724686484546247.763,
        "ph": "E",
        "name": "private static void com.example.pkg.Main.firstMethod()",
        "pid": 12345,
        "tid": 1
    },
    {
        "ts": 1724686484546282.093,
        "ph": "B",
        "name": "java.lang.String com.example.pkg.Main.secondMethod()",
        "pid": 12345,
        "tid": 1
    },
    {
        "ts": 1724686484546306.258,
        "ph": "E",
        "name": "java.lang.String com.example.pkg.Main.secondMethod()",
        "pid": 12345,
        "tid": 1
    },
    {
        "ts": 1724686484546331.966,
        "ph": "B",
        "name": "public int com.example.pkg.Main.thirdMethod(int,int)",
        "pid": 12345,
        "tid": 1
    },
    {
        "ts": 1724686484546356.608,
        "ph": "E",
        "name": "public int com.example.pkg.Main.thirdMethod(int,int)",
        "pid": 12345,
        "tid": 1
    },
    {
        "ts": 1724686484546380.330,
        "ph": "B",
        "name": "protected com.example.pkg.Another com.example.pkg.Main.fourthMethod()",
        "pid": 12345,
        "tid": 1
    },
    {
        "ts": 1724686484548543.913,
        "ph": "E",
        "name": "protected com.example.pkg.Another com.example.pkg.Main.fourthMethod()",
        "pid": 12345,
        "tid": 1
    },
    {
        "ts": 1724686484548604.858,
        "ph": "B",
        "name": "private static void com.example.pkg.Main.fifthMethod(java.util.HashMap,java.lang.String)",
        "pid": 12345,
        "tid": 1
    },
    {
        "ts": 1724686484548632.518,
        "ph": "E",
        "name": "private static void com.example.pkg.Main.fifthMethod(java.util.HashMap,java.lang.String)",
        "pid": 12345,
        "tid": 1
    },
    {
        "ts": 1724686484548654.718,
        "ph": "E",
        "name": "public static void com.example.pkg.Main.main(java.lang.String[])",
        "pid": 12345,
        "tid": 1
    }
]
```

Then, you may import the trace file in Trace Compass. Below is a sample visualization of our trace file (i.e., `Flame Chart`)

![jib_flame_chart](https://github.com/user-attachments/assets/2f21644b-f2e3-4412-82a4-9a6b31028df2)

You can see [**this video tutorial on Youtube**](https://www.youtube.com/watch?v=YCdzmcpOrK4) to see how to import the generated json file in Trace Compass.
