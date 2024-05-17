# Java Bytecode Instrumentation Agent
This Java agent aims to automatically instrument the functions of compiled Java applications (i.e., bytecode). By using the [Byte Buddy](https://github.com/raphw/byte-buddy/tree/master) agent as its core, it is able to perform lightweight application-level function instrumentation.

## Building the agent
You may need to have Maven in your system in order to build and install the agent.
```console
mvn clean install
```

Also, since you need to attach the agent to your program in order to instrument it, you will need to build a `.jar` file of the agent. You may use the following command to achieve that:
```console
mvn clean package
```
The above command will create a `.jar` file named `java-instrumentation-agent-VERSION.jar` in the `.\target\` directory.

## Usage
You can attach the agent to your Java application when executing it. Here is a sample structure for attaching the agent to your Java program.
```console
# Running the program normally
java [OPTIONS] -jar <program commands>

# Attaching the agent to your program
java [OPTIONS] -javaagent:path/to/the/java-instrumentation-agent-VERSION.jar=options -jar <program commands>
```

**P.S.:If you want to use the asynchronous file logging (from log4j2), you need to pass the following system property when running the program:**
```console
-Dlog4j2.contextSelector=org.apache.logging.log4j.core.async.AsyncLoggerContextSelector
```

### Options

- **package:** Indicates the target package that the agent will use to instrument their corresponding classes. For instance, if you set `package=com.your.company`, the agent will instrument all the functions declared within this package's classes. If you leave this field, the agent will instrument all the functions that it can visit.
```console
java -javaagent:path/to/the/java-instrumentation-agent-VERSION.jar=package=com.your.company
```
- **checkOnlyVisited:** This option is useful when you want to determine which functions have been invoked when running your application. When using this option, the agent will only log the functions once, each indicating the method signature and the time of their first entry.
```console
java -javaagent:path/to/the/java-instrumentation-agent-VERSION.jar=checkOnlyVisited=true|false
```
- **logFileName:** If you want to set a custom log file name, you can use this option to pass the file name. All the logs will be stored within the `.\logs\` directory.
```console
java -javaagent:path/to/the/java-instrumentation-agent-VERSION.jar=logFileName=myCustomLogName.log
```

You may use multiple options together by separating them using `,`.

## Result Analysis
After the instrumentation, the agent will store all the logs in a single file. Each line shows a function entry or exit along with the entry/exit time and the function's signature.
```
[TIME_NANO_SECONDS] ENTER|EXIT FUNCTION_SIGNATURE
```
```
[357745830874300] ENTER public static void com.your.company.Main.main(java.lang.String[])
[357745848404300] ENTER private static java.lang.String com.your.company.Main.doNothing(java.lang.String)
[357745848613100] ENTER private static void com.your.company.Main.doSomethingMultipleTimes(int,float)
[357745849231200] EXIT private static void com.your.company.Main.doSomethingMultipleTimes(int,float)
[357745850023300] ENTER private static void com.your.company.Main.doSomethingMultipleTimes(int,float)
[357745850443300] EXIT private static void com.your.company.Main.doSomethingMultipleTimes(int,float)
[357745850607200] EXIT private static java.lang.String com.your.company.Main.doNothing(java.lang.String)
[357745850990400] EXIT public static void com.your.company.Main.main(java.lang.String[])
```