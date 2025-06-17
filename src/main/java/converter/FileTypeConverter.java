package converter;

import java.io.File;
import java.io.IOException;
import java.nio.file.Path;
import java.util.Scanner;

public class FileTypeConverter {
    Engine engine = new Engine();
    public static void main(String[] args) throws IOException {
        ConverterConfig config = new ConverterConfig();
        Thread watcherThread = new Thread(new DirectoryWatcher("src\\input"));
        watcherThread.setDaemon(true);
        watcherThread.start();
        while(true){}
    }




}
