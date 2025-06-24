package objects;

import gui.MainViewController;

import java.io.File;
import java.io.IOException;
import java.nio.file.*;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import static java.nio.file.StandardWatchEventKinds.ENTRY_CREATE;
import static java.nio.file.StandardWatchEventKinds.OVERFLOW;

import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.LogManager;

/**
 * Questa classe osserva ricorsivamente una directory e tutte le sue sottodirectory,
 * eseguendo un'azione (conversione) quando viene creato un nuovo file.
 */
public class DirectoryWatcher implements Runnable {

    private Path dir;
    private final WatchService watchService;
    private final ExecutorService executor;
    private Map<WatchKey, Path> watchKeyToPath;
    private final MainViewController controller;
    private static final Logger logger = LogManager.getLogger(DirectoryWatcher.class);

    /**
     * Costruttore che inizializza il watcher e registra tutte le sottodirectory.
     *
     * @param directoryPath percorso della directory da monitorare
     * @param controller riferimento al controller per eseguire la conversione
     * @throws NullPointerException in caso di parametri null
     * @throws IllegalArgumentException in caso di parametri errati
     * @throws IOException in caso di errori nell'inizializzazione del watchService
     */
    public DirectoryWatcher(String directoryPath, MainViewController controller) throws NullPointerException, IllegalArgumentException, IOException {
        if (directoryPath == null) {
            logger.error("directoryPath nullo");
            throw new NullPointerException("L'oggetto directoryPath non esiste");
        }
        if (controller == null) {
            logger.error("controller nullo");
            throw new NullPointerException("L'oggetto controller non esiste");
        }

        this.dir = Paths.get(directoryPath);
        if (!Files.exists(this.dir) || !Files.isDirectory(this.dir)) {
            logger.error("percorso non valido o non directory - {}", directoryPath);
            throw new IllegalArgumentException("Il percorso " + directoryPath + " è sbagliato o non è una directory");
        }

        this.executor = Executors.newCachedThreadPool();
        this.watchService = FileSystems.getDefault().newWatchService();
        this.controller = controller;
        this.watchKeyToPath = new HashMap<>();

        logger.info("Inizializzazione DirectoryWatcher per: {}", directoryPath);
        registerAll(dir);
    }

    /**
     * Registra ricorsivamente tutte le directory figlie del percorso fornito.
     *
     * @param start directory di partenza
     * @throws IOException in caso di errore nella registrazione
     */
    private void registerAll(final Path start) throws IOException {
        if (start == null) {
            logger.error("start nullo");
            throw new NullPointerException("L'oggetto start non esiste");
        }
        //Funzione eseguita prima di entrare in ciascuna sottocartella
        Files.walkFileTree(start, new SimpleFileVisitor<Path>() {
            @Override
            public FileVisitResult preVisitDirectory(Path dir, BasicFileAttributes attrs) throws IOException {
                if (dir == null) {
                    logger.error("directory da registrare nulla");
                    throw new NullPointerException("L'oggetto dir non esiste");
                }
                //Registra nella mappa dei percorsi quello della nuova cartella
                WatchKey key = dir.register(watchService, ENTRY_CREATE);
                watchKeyToPath.put(key, dir);
                logger.info("Registrata directory per il monitoraggio: {}", dir.toString());
                return FileVisitResult.CONTINUE;
            }
        });
    }

    /**
     * Ciclo principale del watcher. Rimane in ascolto per eventi di creazione (ENTRY_CREATE)
     * e avvia una conversione automatica per ogni nuovo file rilevato.
     * Le directory appena create vengono registrate ricorsivamente.
     */
    @Override
    public void run() {
        logger.info("DirectoryWatcher avviato per: {}", dir.toString());
        while (!Thread.currentThread().isInterrupted()) {
            WatchKey key;
            try {
                //Bloccante in attesa di un evento
                key = watchService.take();
            } catch (InterruptedException e) {
                logger.warn("Thread interrotto, chiusura DirectoryWatcher");
                Thread.currentThread().interrupt();
                break;
            }

            Path parentDir = watchKeyToPath.get(key);
            if (parentDir == null) {
                logger.error("chiave sconosciuta nel watchKeyToPath");
                continue;
            }
            //Controllo tutti gli eventi che sono stati registrati sulla cartella
            for (WatchEvent<?> event : key.pollEvents()) {
                WatchEvent.Kind<?> kind = event.kind();

                if (kind == OVERFLOW) {
                    logger.warn("Overflow rilevato, evento ignorato");
                    continue;
                }
                //Risalgo al percorso del file che ha generato l'evento
                @SuppressWarnings("unchecked")
                WatchEvent<Path> ev = (WatchEvent<Path>) event;
                Path fileName = ev.context();
                if (fileName == null) {
                    logger.error("nome file nullo nell'evento");
                    continue;
                }

                Path fullPath = parentDir.resolve(fileName);
                logger.info("Nuovo file/directory rilevato: {}", fullPath.toString());
                if (kind == ENTRY_CREATE) {
                    //Se si tratta di una directory registro lei e le sottocartelle
                    if (Files.isDirectory(fullPath)) {
                        try {
                            registerAll(fullPath);
                        } catch (IOException e) {
                            logger.error("registrazione sottocartella fallita - {}", fullPath.toString());
                        }
                    }
                    //Se è un file esistente avvio la conversione
                    else {
                        File file = fullPath.toFile();
                        logger.info("Avvio conversione automatica per: {}", file.getAbsolutePath());
                        executor.submit(() -> startConversion(file));
                    }
                }
            }

            boolean valid = key.reset();
            if (!valid) {
                Path removed = watchKeyToPath.remove(key);
                logger.warn("Chiave non più valida, rimossa directory: {}", removed != null ? removed.toString() : "sconosciuta");
                if (watchKeyToPath.isEmpty()) {
                    logger.warn("Nessuna directory rimanente da monitorare. Uscita DirectoryWatcher.");
                    break;
                }
            }
        }
        executor.shutdown();
        logger.info("Executor interrotto");
    }

    /**
     * Restituisce il percorso della directory monitorata.
     *
     */
    private void startConversion(File file){
        controller.launchDialogConversion(file);
    }

}
