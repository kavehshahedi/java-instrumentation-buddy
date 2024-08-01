package kavehshahedi.jib.services;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.core.LoggerContext;

import kavehshahedi.jib.models.Configuration;

public class Logger {

    private static org.apache.logging.log4j.Logger logger;

    public Logger() {
    }

    public Logger(Configuration.Logging loggingInfo) {
        String logFileName = loggingInfo.getFile();
        System.setProperty("logFilename", logFileName);

        LoggerContext ctx = (LoggerContext) LogManager.getContext(false);
        ctx.reconfigure();

        logger = LogManager.getLogger(Logger.class);
    }

    public void info(String message) {
        logger.info(message);
    }

    static final String LOG_PATTERN = "[%d] %s %s";

    public void logTime(String methodSignature, String type) {
        String message = String.format(LOG_PATTERN,
                Time.getTimeNanoSeconds(),
                type,
                methodSignature.replace("$", "."));

        logger.info(message);
    }

}
