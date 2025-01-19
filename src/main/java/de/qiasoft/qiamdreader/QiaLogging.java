package de.qiasoft.qiamdreader;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Date;
import java.util.logging.LogRecord;
import java.util.logging.Formatter;

import java.util.logging.FileHandler;
import java.util.logging.Handler;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.logging.Filter;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;

import java.text.SimpleDateFormat;

import java.io.IOException;

class QiaLogFilter implements Filter {

    @Override
    public boolean isLoggable(LogRecord log) {
        return log.getLevel() != Level.CONFIG;
    }

}

class QiaLogFormatter extends Formatter {
    private static final SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");

    @Override
    public String format(LogRecord record) {
        StringBuilder sb = new StringBuilder();
        sb.append(dateFormat.format(new Date(record.getMillis())))
                .append(" [").append(Thread.currentThread().threadId()).append("] ")
                .append(record.getLevel()).append(" ")
                .append(record.getLoggerName()).append(" - ")
                .append(formatMessage(record)).append("\n");
        return sb.toString();
    }

}

public class QiaLogging {
    public static Logger logger = Logger.getLogger(QiaLogging.class.getName());

    static {
        logger.setLevel(Level.FINE);
        try {
            String userHome = System.getProperty("user.home"); // %h
            Path logFilePath = Paths.get(userHome).resolve(".qia").resolve(LocalDate.now().format(DateTimeFormatter.ofPattern("yyyyMMdd")) + ".java.log");
            Path logDirectoryPath = logFilePath.getParent();
            if (!Files.exists(logDirectoryPath)) {
                Files.createDirectories(logDirectoryPath);
            }
            Handler fileHandler = new FileHandler(logFilePath.toString(), true);
            fileHandler.setFormatter(new QiaLogFormatter());
            fileHandler.setFilter(new QiaLogFilter());
            Runtime.getRuntime().addShutdownHook(new Thread(() -> {
                logger.log(Level.FINE, "QiaLogging::static::Runtime.ShutdownHook::Thread");
                fileHandler.close();
            }));

            for (Handler handler : logger.getHandlers()) {
                logger.removeHandler(handler);
            }
            logger.addHandler(fileHandler);
        } catch (SecurityException | IOException e) {
            // e.printStackTrace();
            logger.log(Level.SEVERE, "QiaLogging::static::Exception: " + e);
        }
    }

    public static void _main(String[] args) {
        logger.log(Level.INFO, "Msg INFO");
        logger.log(Level.FINE, "Msg FINE");
        logger.log(Level.CONFIG, "Config data");
    }
}
