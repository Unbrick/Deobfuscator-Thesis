package org.thesis;

import com.jfoenix.controls.JFXTextArea;
import javafx.application.Platform;

import java.sql.Timestamp;

public class Logger {

    private static JFXTextArea textArea;
    private static Level displayLevel = Level.INFO;

    public static void create(JFXTextArea logTextArea) {
        Logger.textArea = logTextArea;
    }

    public enum Level {
        DEBUG("Debug",0),
        INFO("Info",1),
        ERROR("Error",2);

        private String level;
        private int importancy;

        Level(String level, int importancy) {
            this.level = level;
        }

        public String getLevel() {
            return level;
        }
    }


    private Logger() {}

    public static void setDisplayLevel(Level level) {
        Logger.displayLevel = level;
    }

    public static void debug(String module, String message) {
        log(Level.DEBUG, module, message);
    }

    public static void info(String module, String message) {
        log(Level.INFO, module, message);
    }

    public static void error(String module, String message) {
        log(Level.ERROR, module, message);
    }

    public static void error(String module, Exception e) {
        log(Level.ERROR, module, e.getMessage());
        e.printStackTrace();
    }

    public static void log(Level level, String module, String message) {
        String timestamp = new Timestamp(System.currentTimeMillis()).toString();
        String _message = level.name() + " - " + timestamp + " - " + module + ": " + message + "\r\n";
        if (level.equals(Level.ERROR)) {
            System.err.print(_message);
        } else {
            System.out.print(_message);
        }
        if (textArea != null && (level.equals(Level.INFO) || level.equals(Level.ERROR))) {
            Runnable log = () -> textArea.appendText(_message);
            Platform.runLater(log);
        }
    }

}
