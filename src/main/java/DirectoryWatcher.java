import java.io.IOException;
//classi per gestire i percorsi dei file con watchService
import java.nio.file.*;
//costanti evento per creazione, modifica , ecc..
import static java.nio.file.StandardWatchEventKinds.*;
//classi per gestire i thread
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;




public class DirectoryWatcher implements Runnable{

    private final Path dir;
    private final ExecutorService executor;

    public DirectoryWatcher(String directoryPath){
        //percorso della cartella sulla quale ci si mette in ascolto
        this.dir = Paths.get(directoryPath);
        //executor per i thread che si occupano della conversione
        this.executor = Executors.newCachedThreadPool();
    }

    @Override
    public void run() {
        try {
            //oggetto WatchService per mettersi in ascolto sulla cartella
            WatchService watchService = FileSystems.getDefault().newWatchService();
            //eventi che si vogliono registrare
            // ENTRY_CREATE -> creazione
            // ENTRY_MODIFY -> modifica
            dir.register(watchService, ENTRY_CREATE);

            System.out.println("In ascolto sulla directory: " + dir.toAbsolutePath());

            while (true) {
                //istruzione bloccante che attende un evento
                WatchKey key = watchService.take();

                for (WatchEvent<?> event : key.pollEvents()) {
                    WatchEvent.Kind<?> kind = event.kind();
                    //se si tratta di un errore lo ignora
                    if (kind == OVERFLOW) {
                        continue;
                    }
                    //estrazione del nome e del percorso del file coinvolto
                    WatchEvent<Path> ev = (WatchEvent<Path>) event;
                    Path fileName = ev.context();
                    Path filePath = dir.resolve(fileName);
                    executor.submit(() -> {
                        try {
                            Converter.conversione(filePath);
                        } catch (InterruptedException e) {
                            throw new RuntimeException(e);
                        } catch (IOException e) {
                            throw new RuntimeException(e);
                        }
                    });
                }
                //watchkey libera per ricevere nuovi eventi
                boolean valid = key.reset();
                if (!valid) {
                    break;
                }
            }
        }catch (IOException | InterruptedException e) {
        System.err.println("Errore nel watcher: " + e.getMessage());
    }
    }
}
