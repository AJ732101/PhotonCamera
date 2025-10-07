package com.particlesdevs.photoncamera.util;

public class Log {
    private static java.io.File logDir = null;
    private static final int LOG_RETENTION_DAYS = 10;
    private static String currentLogFileName = null;
    private static final Object fileLock = new Object();

    public static void setLogFile(java.io.File folder) {
        if (folder != null && folder.isDirectory()) {
            logDir = folder;
        } else {
            logDir = null;
        }
        cleanupOldLogs();
    }

    private static java.io.File getLogFile() {
        if (logDir == null) return null;
        String today = new java.text.SimpleDateFormat("yyyy-MM-dd", java.util.Locale.US)
                .format(new java.util.Date());
        String fileName = "log-" + today + ".txt";
        currentLogFileName = fileName;
        return new java.io.File(logDir, fileName);
    }

    private static void cleanupOldLogs() {
        if (logDir == null) return;
        java.io.File[] files = logDir.listFiles();
        if (files == null) return;
        long now = System.currentTimeMillis();
        long retentionMillis = LOG_RETENTION_DAYS * 24L * 60L * 60L * 1000L;
        for (java.io.File file : files) {
            if (file.isFile() && file.getName().startsWith("log-") && file.getName().endsWith(".txt")) {
                long lastModified = file.lastModified();
                if (now - lastModified > retentionMillis) {
                    //noinspection ResultOfMethodCallIgnored
                    file.delete();
                }
            }
        }
    }

    private static void writeToFile(String level, String tag, String message) {
        java.io.File file = getLogFile();
        if (file == null) return;
        String time = new java.text.SimpleDateFormat("yyyy-MM-dd HH:mm:ss.SSS", java.util.Locale.US)
                .format(new java.util.Date());
        String logEntry = String.format("%s %s/%s: %s\n", time, level, tag, message);
        synchronized (fileLock) {
            try (java.io.FileWriter fw = new java.io.FileWriter(file, true)) {
                fw.write(logEntry);
            } catch (Exception e) {
                // Optionally, handle file write errors
            }
        }
    }

    public static void d(String tag, String message) {
        android.util.Log.d(tag, message);
        writeToFile("D", tag, message);
    }

    public static void w(String tag, String message) {
        android.util.Log.w(tag, message);
        writeToFile("W", tag, message);
    }
    public static void w(String tag, String message, Throwable tr) {
        android.util.Log.w(tag, message, tr);
        writeToFile("W", tag, message + "\n" + android.util.Log.getStackTraceString(tr));
    }

    public static void e(String tag, String message) {
        android.util.Log.e(tag, message);
        writeToFile("E", tag, message);
    }
    public static void e(String tag, String message, Throwable tr) {
        android.util.Log.e(tag, message, tr);
        writeToFile("E", tag, message + "\n" + android.util.Log.getStackTraceString(tr));
    }

    public static void i(String tag, String message) {
        android.util.Log.i(tag, message);
        writeToFile("I", tag, message);
    }

    public static void v(String tag, String s) {
        android.util.Log.v(tag, s);
        writeToFile("V", tag, s);
    }

    public static String getStackTraceString(Exception e) {
        StringBuilder sb = new StringBuilder();
        sb.append(e.toString()).append("\n");
        for (StackTraceElement element : e.getStackTrace()) {
            sb.append("\tat ").append(element).append("\n");
        }
        String stackTrace = sb.toString();
        e.printStackTrace();
        writeToFile("E", "Exception", stackTrace);
        return stackTrace;
    }
}
