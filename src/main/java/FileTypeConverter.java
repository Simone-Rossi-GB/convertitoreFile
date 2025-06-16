import java.io.IOException;

public class FileTypeConverter {
    public static void main(String[] args) throws IOException {
        ConverterConfig config = new ConverterConfig();
        Thread watcherThread = new Thread(new DirectoryWatcher("percorso"));
        watcherThread.setDaemon(true);
        watcherThread.start();
        while(true){}
    }
}
