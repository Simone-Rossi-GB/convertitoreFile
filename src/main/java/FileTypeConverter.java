import Converters.ACC;
import Converters.CSVtoJSONconverter;
import Converters.PDFtoJPGconverter;

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

    public void settingsConversione(Path srcPath){
        System.out.println("Formato conversione: ");
        String outExt = new Scanner(System.in).nextLine();
        String srcExt = getExtension(srcPath);
        File srcFile = new File(srcPath.toString());
        try {
            engine.conversione(srcExt, outExt, srcFile);
        } catch (Exception e) {
            System.out.println(e.getMessage());
        }
    }

    private String getExtension(Path filePath){
        String path = filePath.toAbsolutePath().toString();
        int lastDotIndex = path.lastIndexOf(".");
        return path.substring(lastDotIndex + 1);
    }
}
