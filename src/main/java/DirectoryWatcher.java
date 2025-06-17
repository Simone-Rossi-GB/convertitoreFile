import java.io.IOException;
import java.nio.file.*;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import static java.nio.file.StandardWatchEventKinds.ENTRY_CREATE;
import static java.nio.file.StandardWatchEventKinds.OVERFLOW;

public class DirectoryWatcher implements Runnable {

    private final Path dir;
    private final WatchService watchService;
    private final ExecutorService executor;
    private final Map<WatchKey, Path> watchKeyToPath;
    private FileTypeConverter controller;

    public DirectoryWatcher(String directoryPath) throws IOException {
        this.dir = Paths.get(directoryPath);
        this.executor = Executors.newCachedThreadPool();
        this.watchService = FileSystems.getDefault().newWatchService();
        this.controller = new FileTypeConverter();
        this.watchKeyToPath = new HashMap<>();
        registerAll(dir); // Registra tutte le sottocartelle esistenti
    }

    // Metodo per registrare ricorsivamente tutte le sottodirectory
    private void registerAll(final Path start) throws IOException {
        Files.walkFileTree(start, new SimpleFileVisitor<Path>() {
            @Override
            public FileVisitResult preVisitDirectory(Path dir, BasicFileAttributes attrs) throws IOException {
                WatchKey key = dir.register(watchService, ENTRY_CREATE);
                watchKeyToPath.put(key, dir);
                return FileVisitResult.CONTINUE;
            }
        });
    }

    @Override
    public void run() {
        System.out.println("In ascolto ricorsivo sulla directory: " + dir.toAbsolutePath());

        while (true) {
            WatchKey key;
            try {
                key = watchService.take(); // Bloccante
            } catch (InterruptedException e) {
                return;
            }

            Path parentDir = watchKeyToPath.get(key);
            if (parentDir == null) continue;

            for (WatchEvent<?> event : key.pollEvents()) {
                WatchEvent.Kind<?> kind = event.kind();
                if (kind == OVERFLOW) continue;

                WatchEvent<Path> ev = (WatchEvent<Path>) event;
                Path fileName = ev.context();
                Path fullPath = parentDir.resolve(fileName);

                if (kind == ENTRY_CREATE) {
                    if (Files.isDirectory(fullPath)) {
                        // Registra la nuova directory creata
                        try {
                            registerAll(fullPath);
                        } catch (IOException e) {
                            e.printStackTrace();
                        }
                    } else {
                        executor.submit(() -> {
                            try {
                                controller.settingsConversione(fullPath);
                            } catch (Exception e) {
                                throw new RuntimeException(e);
                            }
                        });
                    }
                }
            }

            boolean valid = key.reset();
            if (!valid) {
                watchKeyToPath.remove(key);
                if (watchKeyToPath.isEmpty()) break;
            }
        }
    }
}
