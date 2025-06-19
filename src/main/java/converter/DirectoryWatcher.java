package converter;

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

/**
 * Questa classe osserva ricorsivamente una directory e tutte le sue sottodirectory,
 * eseguendo un'azione (conversione) quando viene creato un nuovo file.
 */
public class DirectoryWatcher implements Runnable {

    private final Path dir;
    private final WatchService watchService;
    private final ExecutorService executor;
    private final Map<WatchKey, Path> watchKeyToPath;
    private final MainViewController controller;

    /**
     * Costruttore che inizializza il watcher e registra tutte le sottodirectory.
     *
     * @param directoryPath percorso della directory da monitorare
     * @param controller riferimento al controller per eseguire la conversione
     * @throws IOException in caso di errore nella registrazione delle directory
     */
    public DirectoryWatcher(String directoryPath, MainViewController controller) throws IOException {
        if (directoryPath == null) {
            Log.addMessage("ERRORE: directoryPath nullo");
            throw new NullPointerException("L'oggetto directoryPath non esiste");
        }
        if (controller == null) {
            Log.addMessage("ERRORE: controller nullo");
            throw new NullPointerException("L'oggetto controller non esiste");
        }

        this.dir = Paths.get(directoryPath);
        if (!Files.exists(this.dir) || !Files.isDirectory(this.dir)) {
            Log.addMessage("ERRORE: percorso non valido o non directory - " + directoryPath);
            throw new IllegalArgumentException("Il percorso " + directoryPath + " è sbagliato o non è una directory");
        }

        this.executor = Executors.newCachedThreadPool();
        this.watchService = FileSystems.getDefault().newWatchService();
        this.controller = controller;
        this.watchKeyToPath = new HashMap<>();

        Log.addMessage("Inizializzazione DirectoryWatcher per: " + directoryPath);
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
            Log.addMessage("ERRORE: start nullo");
            throw new NullPointerException("L'oggetto start non esiste");
        }

        Files.walkFileTree(start, new SimpleFileVisitor<Path>() {
            @Override
            public FileVisitResult preVisitDirectory(Path dir, BasicFileAttributes attrs) throws IOException {
                if (dir == null) {
                    Log.addMessage("ERRORE: directory da registrare nulla");
                    throw new NullPointerException("L'oggetto dir non esiste");
                }

                WatchKey key = dir.register(watchService, ENTRY_CREATE);
                watchKeyToPath.put(key, dir);
                Log.addMessage("Registrata directory per il monitoraggio: " + dir.toString());
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
        Log.addMessage("DirectoryWatcher avviato per: " + dir.toString());

        while (!Thread.currentThread().isInterrupted()) {
            WatchKey key;
            try {
                key = watchService.take();
            } catch (InterruptedException e) {
                Log.addMessage("Thread interrotto, chiusura DirectoryWatcher");
                Thread.currentThread().interrupt();
                break;
            }

            Path parentDir = watchKeyToPath.get(key);
            if (parentDir == null) {
                Log.addMessage("ERRORE: chiave sconosciuta nel watchKeyToPath");
                continue;
            }

            for (WatchEvent<?> event : key.pollEvents()) {
                WatchEvent.Kind<?> kind = event.kind();

                if (kind == OVERFLOW) {
                    Log.addMessage("Overflow rilevato, evento ignorato");
                    continue;
                }

                WatchEvent<Path> ev = (WatchEvent<Path>) event;
                Path fileName = ev.context();
                if (fileName == null) {
                    Log.addMessage("ERRORE: nome file nullo nell'evento");
                    continue;
                }

                Path fullPath = parentDir.resolve(fileName);
                Log.addMessage("Nuovo file/directory rilevato: " + fullPath.toString());

                if (kind == ENTRY_CREATE) {
                    if (Files.isDirectory(fullPath)) {
                        try {
                            registerAll(fullPath);
                        } catch (IOException e) {
                            Log.addMessage("ERRORE: registrazione sottocartella fallita - " + fullPath.toString());
                        }
                    } else {
                        File file = fullPath.toFile();
                        if (file != null) {
                            Log.addMessage("Avvio conversione automatica per: " + file.getAbsolutePath());
                            executor.submit(() -> controller.launchDialogConversion(file));
                        } else {
                            Log.addMessage("ERRORE: file nullo da convertire");
                        }
                    }
                }
            }

            boolean valid = key.reset();
            if (!valid) {
                Path removed = watchKeyToPath.remove(key);
                Log.addMessage("Chiave non più valida, rimossa directory: " + (removed != null ? removed.toString() : "sconosciuta"));
                if (watchKeyToPath.isEmpty()) {
                    Log.addMessage("Nessuna directory rimanente da monitorare. Uscita DirectoryWatcher.");
                    break;
                }
            }
        }

        executor.shutdown();
        Log.addMessage("Executor arrestato, DirectoryWatcher terminato");
    }

    /**
     * Restituisce il percorso della directory monitorata.
     *
     * @return directory monitorata
     */
    public String getWatchedDir() {
        return dir.toString();
    }
}
