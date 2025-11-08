package org.redes.Utils;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

public class Logger {
    private static final DateTimeFormatter fmt = DateTimeFormatter.ofPattern("HH:mm:ss.SSS");

    public static synchronized void log(String message) {
        String timestamp = LocalDateTime.now().format(fmt);
        System.out.println("[" + timestamp + "] " + message);
    }

    public static synchronized void logError(String message, Exception e) {
        String timestamp = LocalDateTime.now().format(fmt);
        System.err.println("[" + timestamp + "] ERRO: " + message);
        e.printStackTrace(System.err);
    }
}
