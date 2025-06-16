import Converters.ACC;

import java.io.File;
import java.io.IOException;

public class FileTypeConverter {
    public static void main(String[] args) throws IOException {
        ConverterConfig config = new ConverterConfig();
        Thread watcherThread = new Thread(new DirectoryWatcher("C:\\Users\\DELLMuletto\\IdeaProjects\\convertitoreFile\\src\\input"));
        ACC.convertiZipToTarGzInCartella(new File("C:\\Users\\DELLMuletto\\IdeaProjects\\convertitoreFile\\src\\input\\prova.zip"), new File("C:\\Users\\DELLMuletto\\IdeaProjects\\convertitoreFile\\src\\output_corretto"));
        watcherThread.setDaemon(true);
        watcherThread.start();
        while(true){}
    }
}
