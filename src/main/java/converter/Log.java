package converter;

import java.io.*;
import java.text.SimpleDateFormat;
import java.util.Date;

public class Log {
    private static File logFile = null;
    private static PrintWriter writer = null;

    public Log() throws IOException {
        File logDir = new File("src/logs");
        if (!logDir.exists()) logDir.mkdirs();

        String fileName = "log_" + getDateOnly() + ".txt"; // log_2025-06-18.txt
        logFile = new File(logDir, fileName);

        writer = new PrintWriter(new BufferedWriter(new FileWriter(logFile, true)));
    }

    public static void addMessage(String message) {
        String time = getTimeOnly();
        writer.println("[" + time + "] " + message);
        writer.flush();
    }

    public static void close() {
        writer.close();
    }

    public static File getLogFile() {
        return logFile;
    }

    private static String getTimeOnly() {
        return new SimpleDateFormat("HH:mm:ss").format(new Date());
    }

    private static String getDateOnly() {
        return new SimpleDateFormat("yyyy-MM-dd").format(new Date());
    }
}