package converter;

import java.io.*;
import java.text.SimpleDateFormat;
import java.util.Date;

public class Log {
    private final File logFile;
    private final PrintWriter writer;

    public Log() throws IOException {
        File logDir = new File("src/logs");
        if (!logDir.exists()) logDir.mkdirs();

        String fileName = "log_" + getDateOnly() + ".txt"; // log_2025-06-18.txt
        this.logFile = new File(logDir, fileName);

        this.writer = new PrintWriter(new BufferedWriter(new FileWriter(logFile, true)));
    }

    public void addMessage(String message) {
        String time = getTimeOnly();
        writer.println("[" + time + "] " + message);
        writer.flush();
    }

    public void close() {
        writer.close();
    }

    public File getLogFile() {
        return logFile;
    }

    private String getTimeOnly() {
        return new SimpleDateFormat("HH:mm:ss").format(new Date());
    }

    private String getDateOnly() {
        return new SimpleDateFormat("yyyy-MM-dd").format(new Date());
    }
}