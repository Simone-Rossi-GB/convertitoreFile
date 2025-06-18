package converter;

import java.io.*;
import java.text.SimpleDateFormat;
import java.util.Date;

public class Log {
    private static File logFile = null;
    private static PrintWriter writer = null;
    private static final File logDir = new File("src/logs");

    public static void addMessage(String message) {
        if (logFile == null){
            try {
                String fileName = "log_" + getDateOnly() + ".txt";
                logFile = new File(logDir, fileName);
                writer = new PrintWriter(new BufferedWriter(new FileWriter(logFile, true)));
            } catch (IOException e){
                System.out.println("ERRORE CREAZIONE FILE DI LOG");
            }
        }
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