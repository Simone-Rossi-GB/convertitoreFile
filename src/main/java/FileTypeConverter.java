import Converters.ACC;
import Converters.PDFtoJPGconverter;

import java.io.File;
import java.io.IOException;

public class FileTypeConverter {
    public static void main(String[] args) throws IOException {
        ConverterConfig config = new ConverterConfig();
        Thread watcherThread = new Thread(new DirectoryWatcher("C:\\Users\\DELLMuletto\\IdeaProjects\\convertitoreFile\\src\\input"));

        //test

        watcherThread.setDaemon(true);
        watcherThread.start();

        while(true){}
    }
}
