public class FileTypeConverter {
    public static void main(String[] args) {
        ConverterConfig config = new ConverterConfig();
        Thread watcherThreader = new Thread(new DirectoryWatcher(directoryPath));
        watcherThreader.setDaemon(true);
        watcherThreader.start();
        while(true){}
    }
}
