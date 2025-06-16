import Converters.ACC;
import Converters.CSVtoJSONconverter;
import Converters.PDFtoJPGconverter;

import java.io.File;
import java.io.IOException;

public class FileTypeConverter {
    public static void main(String[] args) throws IOException {
        ConverterConfig config = new ConverterConfig();
        CSVtoJSONconverter conv = new CSVtoJSONconverter();
        conv.convert(new File("C:\\Users\\tipot\\IdeaProjects\\convertitoreFile\\mtcars.csv"));
        Thread watcherThread = new Thread(new DirectoryWatcher("C:\\Users\\DELLMuletto\\IdeaProjects\\convertitoreFile\\src\\input"));
        Engine engine = new Engine();
        //test



        watcherThread.setDaemon(true);
        watcherThread.start();

        while(true){}
    }
}
