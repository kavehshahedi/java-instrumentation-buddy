# JIB: Java Instrumentation Buddy

JIB is a lightweight Java agent designed to seamlessly instrument functions in compiled Java applications at the bytecode level. Leveraging the power of [Byte Buddy](https://github.com/raphw/byte-buddy/tree/master) as its core, JIB offers efficient, lightweight, and application-level function instrumentation with minimal overhead.

## Features

- Non-intrusive bytecode manipulation
- Configurable instrumentation targets
- Flexible logging options
- Method-level granularity for instrumentation
- Seamless integration with existing Java applications
- Compatible with Java 8 and above

## Building

To build and install JIB, ensure you have Maven installed on your system. Then, execute the following command in the project root directory:

```console
mvn clean install
```

This command compiles the source code, runs tests, and installs the artifact in your local Maven repository.

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

# Attaching the agent to your program
java [OPTIONS] -javaagent:path/to/the/jib.jar=<config=configuration.yaml> -jar <program commands>
```

Thank you for providing the YAML example. I'll complete the "Agent Configuration" section based on the information you've given. Here's a draft of the content:

### Agent Configuration

The agent configuration is specified in a YAML file. This file allows you to customize various aspects of the agent's behavior. Below are the available configuration options:

#### Logging

The `logging` section controls how the agent generates log files:

- `file`: Specifies the path to the log file where the agent will write its output.
  - Example: `path/to/file.log`

- `addTimestampToFileNames`: When set to `true`, adds a timestamp to the log file name.
  - Default: `false`

- `useHash`: If `true`, the agent uses hashing for method signatures in the log file. This is useful for reducing the size of the log file when there are a large number of method logs with long signatures. The mapping of the hashes will be stored in a separate JSON file.
  - Default: `false`

#### Instrumentation

The `instrumentation` section defines which parts of your code the agent will instrument:

- `targetPackage`: Specifies which package to instrument. If not set, all classes will be instrumented (both within and outside of the program).
  - Example: `com.example`
  - Default: empty (all classes are instrumented)

- `onlyCheckVisited`: When set to `true`, the agent only instruments the entry of each function once. This is useful for checking code coverage.
  - Default: `false`

- `instrumentMainMethod`: If `true`, the agent will instrument the main method of the main class.
  - Default: `false`

- `targetMethods`: Allows you to specify which methods to instrument or ignore. This section has two sub-sections:
  - `instrument`: A list of methods to instrument. If specified, only these methods will be instrumented.
    - Example:
      ```yaml
      instrument:
        - private static void com.example.MainClass.methodName(int a, java.lang.String b)
      ```
  - `ignore`: A list of methods to exclude from instrumentation. If specified, all methods except these will be instrumented.
    - Example:
      ```yaml
      ignore:
        - protected java.lang.String com.example.MainClass.ignoredMethodName(float a)
      ```

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
    - `(args)` are the method's parameters (required)

    Examples:
    ```java
    private static void com.example.MainClass.methodName(int a, java.lang.String b)
    protected java.lang.String ignoredMethodName(float a)
    public java.util.List<java.lang.String> getNames()
    void processData(byte[] data)
    ```
  Note: You can specify either `instrument` or `ignore`, but not both since it doesn't make sense to instrument and ignore methods at the same time.

Here's an example of a complete configuration file:

```yaml
logging:
  file: app.log
  addTimestampToFileNames: false
  useHash: false

instrumentation:
  targetPackage: com.example
  onlyCheckVisited: false
  instrumentMainMethod: true
  targetMethods:
    instrument:
      - private static void com.example.MainClass.methodName(int a, java.lang.String b)
```

To use this configuration, save it to a file (e.g., `configuration.yaml`) and specify it when attaching the agent to your Java application:

```console
java [OPTIONS] -javaagent:path/to/the/jib.jar=config=configuration.yaml -jar <program commands>
```

This configuration allows you to fine-tune the agent's behavior to suit your specific needs, whether you're focusing on particular packages, methods, or adjusting logging options.


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

Suppose we want to instrument the program to log the entry and exit of each method. We can create a YAML configuration file to specify the methods to instrument and the logging options. For now, let's just instrument all the methods in the `com.example.pkg` package and log the output to a file.

#### Configuration File
    
```yaml
logging:
    file: app.log

instrumentation:
    targetPackage: com.example.pkg
```

After the instrumentation, the agent will store all the logs in a single file (i.e., `app.log`). Each line shows a function entry or exit along with the S/E (start or end) time and the function's signature.
```
[TIME_NANO_SECONDS] S|E FUNCTION_SIGNATURE
```
```
[1724605175635717866] S public static void com.example.pkg.Main.main(java.lang.String[])
[1724605175637779321] S private static void com.example.pkg.Main.firstMethod()
[1724605175637903541] E private static void com.example.pkg.Main.firstMethod()
[1724605175637944911] S java.lang.String com.example.pkg.Main.secondMethod()
[1724605175637971154] E java.lang.String com.example.pkg.Main.secondMethod()
[1724605175637995449] S public int com.example.pkg.Main.thirdMethod(int,int)
[1724605175638028792] E public int com.example.pkg.Main.thirdMethod(int,int)
[1724605175638069161] S protected com.example.pkg.Another com.example.pkg.Main.fourthMethod()
[1724605175640295270] E protected com.example.pkg.Another com.example.pkg.Main.fourthMethod()
[1724605175640359832] S private static void com.example.pkg.Main.fifthMethod(java.util.HashMap,java.lang.String)
[1724605175640388900] E private static void com.example.pkg.Main.fifthMethod(java.util.HashMap,java.lang.String)
[1724605175640410797] E public static void com.example.pkg.Main.main(java.lang.String[])
```

### Partial Instrumentation

Now, we may specify the methods to instrument or ignore in the configuration file. For instance, we can instrument only the `main` method and the `thirdMethod` in the `Main` class:

#### Configuration File
    
```yaml
logging:
    file: app.log

instrumentation:
    targetPackage: com.example.pkg
    instrumentMainMethod: true
    targetMethods:
        instrument:
            - public int com.example.pkg.Main.thirdMethod(int a, int b)
```

After running the program with this configuration, the log file will only contain entries for the `main` and `thirdMethod` functions.
```
[1724605175635717866] S public static void com.example.pkg.Main.main(java.lang.String[])
[1724605175637995449] S public int com.example.pkg.Main.thirdMethod(int,int)
[1724605175638028792] E public int com.example.pkg.Main.thirdMethod(int,int)
[1724605175640410797] E public static void com.example.pkg.Main.main(java.lang.String[])
```

### Use Hashing for Method Signatures
If you want to use hashing for method signatures in the log file, you can enable the `useHash` option in the configuration file. This option is useful when there are a large number of method logs with long signatures.

#### Configuration File
    
```yaml
logging:
    file: app.log
    useHash: true

instrumentation:
    targetPackage: com.example.pkg
```

After running the program with this configuration, the log file will contain hashed method signatures instead of the full method signatures. The mapping of the hashes will be stored in a separate JSON file.

- Log file:
    ```
    [1724685951931233870] S A
    [1724685951933340359] S B
    [1724685951933525189] E B
    [1724685951933581572] S C
    [1724685951933615793] E C
    [1724685951933641228] S D
    [1724685951933668752] E D
    [1724685951933730567] S E
    [1724685951936219996] E E
    [1724685951936283964] S F
    [1724685951936315634] E F
    [1724685951936338337] E A
    ```

- Log metadata (`.json`)
    ```json
    {
        "start_time": 1724685951888001220,
        "end_time": 1724685951936780901,
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

If you want to import the collected instrumentation logs in Trace Compass, you can use the `trace_formatter.py` script to re-format the log files, and generate a Trace Compass readable json file.

**Structure:**
```console
python trace_formatter.py [-h] [--batch_size BATCH_SIZE] input output
```
**Example:**
```console
python trace_formatter.py app.log app.json
```
**Output:**
```json
[
    {
        "ts": 1724686484544229.333,
        "ph": "B",
        "name": "public static void com.example.pkg.Main.main(java.lang.String[])"
    },
    {
        "ts": 1724686484546125.563,
        "ph": "B",
        "name": "private static void com.example.pkg.Main.firstMethod()"
    },
    {
        "ts": 1724686484546247.763,
        "ph": "E",
        "name": "private static void com.example.pkg.Main.firstMethod()"
    },
    {
        "ts": 1724686484546282.093,
        "ph": "B",
        "name": "java.lang.String com.example.pkg.Main.secondMethod()"
    },
    {
        "ts": 1724686484546306.258,
        "ph": "E",
        "name": "java.lang.String com.example.pkg.Main.secondMethod()"
    },
    {
        "ts": 1724686484546331.966,
        "ph": "B",
        "name": "public int com.example.pkg.Main.thirdMethod(int,int)"
    },
    {
        "ts": 1724686484546356.608,
        "ph": "E",
        "name": "public int com.example.pkg.Main.thirdMethod(int,int)"
    },
    {
        "ts": 1724686484546380.330,
        "ph": "B",
        "name": "protected com.example.pkg.Another com.example.pkg.Main.fourthMethod()"
    },
    {
        "ts": 1724686484548543.913,
        "ph": "E",
        "name": "protected com.example.pkg.Another com.example.pkg.Main.fourthMethod()"
    },
    {
        "ts": 1724686484548604.858,
        "ph": "B",
        "name": "private static void com.example.pkg.Main.fifthMethod(java.util.HashMap,java.lang.String)"
    },
    {
        "ts": 1724686484548632.518,
        "ph": "E",
        "name": "private static void com.example.pkg.Main.fifthMethod(java.util.HashMap,java.lang.String)"
    },
    {
        "ts": 1724686484548654.718,
        "ph": "E",
        "name": "public static void com.example.pkg.Main.main(java.lang.String[])"
    }
]
```

Then, you may import the trace file in Trace Compass. Below is a sample visualization of our trace file (i.e., `Flame Chart`)

![jib_flame_chart](https://github.com/user-attachments/assets/2f21644b-f2e3-4412-82a4-9a6b31028df2)

You can see [**this video tutorial on Youtube**](https://www.youtube.com/watch?v=YCdzmcpOrK4) to see how to import the generated json file in Trace Compass.
