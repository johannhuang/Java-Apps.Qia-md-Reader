package de.qiasoft.qiamdreader;

import java.util.Arrays;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.logging.Level;

import java.io.File;

import java.awt.Desktop;
import java.awt.Toolkit;
import java.awt.EventQueue;
import java.awt.event.InvocationEvent;

import javafx.application.Platform;

public class Program {
    private static final Object lock = new Object();
    private static boolean toolkitInitialized = false;

    private static final ConcurrentLinkedQueue<File> fileQueue = new ConcurrentLinkedQueue<>();

    static { // Listen to OPEN_FILE events as soon as possible (event may also arise while the program is running)
        QiaLogging.logger.log(Level.FINE, "Program::static: BEGIN");
        if (Desktop.getDesktop().isSupported(Desktop.Action.APP_OPEN_FILE)) {
            QiaLogging.logger.log(Level.FINE, "Program::static: BEGIN setting OpenFileHandler");
            Desktop.getDesktop().setOpenFileHandler(event -> {
                QiaLogging.logger.log(Level.FINE, "Program::static: Received an OpenFilesEvent");
                // fileQueue.addAll(event.getFiles());
                for (File file : event.getFiles()) {
                    QiaLogging.logger.log(Level.INFO, String.format("Program::static::OpenFilesEvent: %s", file.getAbsolutePath()));
                    fileQueue.add(file);
                }
                // FXApplication.update(fileQueue);
                Platform.runLater(() -> {
                    FXApplication.updateFileQueue(fileQueue);
                });
            });
            QiaLogging.logger.log(Level.FINE, "Program::static: END setting OpenFileHandler");
        }
        QiaLogging.logger.log(Level.FINE, "Program::static: END");
    }

    public static void ensureToolkitInitialized() {
        if (Platform.isFxApplicationThread()) {
            // Toolkit is already initialized if we're on the FX thread
            return;
        }

        synchronized (lock) {
            if (!toolkitInitialized) {
                Platform.startup(() -> {
                    synchronized (lock) {
                        toolkitInitialized = true;
                        lock.notifyAll(); // Notify waiting threads
                    }
                });

                // Wait for the toolkit to initialize
                while (!toolkitInitialized) {
                    try {
                        lock.wait();
                    } catch (InterruptedException e) {
                        Thread.currentThread().interrupt(); // Restore interrupt status
                        throw new IllegalStateException("Interrupted while waiting for Toolkit initialization", e);
                    }
                }
            }
        }
    }

    public static void main(String[] args) {
        QiaLogging.logger.log(Level.FINE, "Program::static.main: ARGS:" + Arrays.toString(args));
        QiaLogging.logger.log(Level.FINE, "Program::static.main: BEGIN");

        ensureToolkitInitialized();
        QiaLogging.logger.log(Level.FINE, "Program::static.main: Toolkit Initialized");

        // Platform.setImplicitExit(false);
        Platform.runLater(() -> {
            FXApplication.init(args);
        });
        QiaLogging.logger.log(Level.FINE, "Program::static.main: FXApplication init Scheduled");

        EventQueue eventQueue = Toolkit.getDefaultToolkit().getSystemEventQueue();
        try {
            while (true) {
                InvocationEvent event = (InvocationEvent) eventQueue.getNextEvent();
                QiaLogging.logger.log(Level.FINE, "Program::static.main: Received an event" + event);
            }
        } catch (InterruptedException e) {
            QiaLogging.logger.log(Level.FINE, "Program::static.main: Received an InterruptedException");
            QiaLogging.logger.log(Level.FINE, "Program::static.main: EXIT");
            System.exit(0);
        }

        QiaLogging.logger.log(Level.FINE, "Program::static.main: END");
    }
}
