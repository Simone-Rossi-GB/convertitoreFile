package webService.server.converters.mailConverters;

import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.LogManager;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;

/**
 * Gestisce la localizzazione e configurazione di Chrome Headless per Docker.
 * Ottimizzato per container Linux con Chrome preinstallato.
 * Implementa il pattern Singleton e cerca automaticamente l'eseguibile Chrome.
 */
public class ChromeManager {

    private static final Logger logger = LogManager.getLogger(ChromeManager.class);
    private static ChromeManager instance;
    private static String chromePath;
    private static boolean initialized = false;

    /**
     * Costruttore privato per implementare il pattern Singleton.
     */
    private ChromeManager() {}

    /**
     * Restituisce l'istanza singleton di ChromeManager, inizializzandola se necessario.
     *
     * @return L'istanza singleton di ChromeManager
     */
    public static synchronized ChromeManager getInstance() {
        if (instance == null) {
            instance = new ChromeManager();
            initialize();
        }
        return instance;
    }

    /**
     * Inizializza il ChromeManager caricando la configurazione e cercando l'eseguibile Chrome.
     * Ottimizzato per ambiente Docker.
     */
    private static void initialize() {
        if (initialized) return;

        try {
            // Carica configurazione dalle risorse
            Properties config = loadConfig();

            // Trova Chrome (priorità a Docker paths)
            chromePath = findChromeExecutable(config);

            if (chromePath != null) {
                logger.info("Chrome configurato per Docker: {}", chromePath);

                // Test veloce per verificare che Chrome funzioni
                if (testChrome(chromePath)) {
                    logger.info("Chrome validato con successo");
                } else {
                    logger.warn("Chrome trovato ma non risponde correttamente");
                    chromePath = null;
                }
            } else {
                logger.warn("Chrome non trovato - Conversioni PDF non disponibili");
            }

            initialized = true;

        } catch (Exception e) {
            logger.error("Errore nell'inizializzazione Chrome", e);
            chromePath = null;
            initialized = true; // Evita retry infiniti
        }
    }

    /**
     * Carica le proprietà di configurazione da converter.properties nelle risorse.
     *
     * @return Oggetto Properties contenente la configurazione caricata
     * @throws IOException Se si verificano errori durante il caricamento del file
     */
    private static Properties loadConfig() throws IOException {
        Properties config = new Properties();

        // Carica sempre dalle risorse (post-build)
        try (InputStream is = ChromeManager.class.getClassLoader().getResourceAsStream("converter.properties")) {
            if (is != null) {
                config.load(is);
                logger.debug("Configurazione Chrome caricata dalle risorse");
            } else {
                logger.warn("converter.properties non trovato nelle risorse, uso configurazione default");
                // Imposta valori di default per Docker
                setDefaultDockerConfig(config);
            }
        }

        return config;
    }

    /**
     * Imposta la configurazione di default ottimizzata per Docker.
     *
     * @param config Oggetto Properties da popolare
     */
    private static void setDefaultDockerConfig(Properties config) {
        // Percorsi standard per Docker Linux
        config.setProperty("chrome.docker.paths",
                "/usr/bin/google-chrome:/usr/bin/google-chrome-stable:/usr/bin/chromium-browser:/usr/bin/chromium");

        // Argomenti ottimizzati per Docker
        config.setProperty("chrome.args",
                "--headless --disable-gpu --no-sandbox --disable-dev-shm-usage --disable-background-timer-throttling --disable-renderer-backgrounding --disable-features=TranslateUI --remote-debugging-port=0");

        config.setProperty("chrome.timeout.seconds", "30");
        config.setProperty("chrome.temp.cleanup", "true");
    }

    /**
     * Cerca l'eseguibile Chrome utilizzando strategie ottimizzate per Docker.
     * Priorità: variabile d'ambiente → paths Docker → PATH di sistema
     *
     * @param config Configurazione caricata
     * @return Il percorso assoluto dell'eseguibile Chrome, o null se non trovato
     */
    private static String findChromeExecutable(Properties config) {
        // 1. Variabile d'ambiente CHROME_PATH (massima priorità)
        String envPath = System.getenv("CHROME_PATH");
        if (isValidExecutable(envPath)) {
            logger.info("Chrome trovato da variabile d'ambiente: {}", envPath);
            return envPath;
        }

        // 2. Symlink generico /usr/bin/chrome (creato dal Dockerfile)
        String genericPath = "/usr/bin/chrome";
        if (isValidExecutable(genericPath)) {
            logger.info("Chrome trovato tramite symlink generico: {}", genericPath);
            return genericPath;
        }

        // 3. Percorsi Docker standard dalla configurazione
        String dockerPaths = config.getProperty("chrome.docker.paths",
                "/usr/bin/google-chrome-stable:/usr/bin/google-chrome:/usr/bin/chromium-browser:/usr/bin/chromium");

        for (String path : dockerPaths.split(":")) {
            if (isValidExecutable(path.trim())) {
                logger.info("Chrome trovato nei percorsi Docker: {}", path.trim());
                return path.trim();
            }
        }

        // 4. Fallback: cerca nel PATH di sistema
        String pathChrome = findInPath();
        if (pathChrome != null) {
            logger.info("Chrome trovato nel PATH: {}", pathChrome);
            return pathChrome;
        }

        // 5. DEBUG: Lista tutti i possibili eseguibili
        logger.error("Chrome non trovato. Eseguibili disponibili:");
        try {
            Process process = Runtime.getRuntime().exec(new String[]{"ls", "-la", "/usr/bin/"});
            try (java.io.BufferedReader reader = new java.io.BufferedReader(
                    new java.io.InputStreamReader(process.getInputStream()))) {
                reader.lines()
                        .filter(line -> line.contains("chrome") || line.contains("chromium"))
                        .forEach(line -> logger.error("  {}", line));
            }
        } catch (Exception e) {
            logger.error("Impossibile listare /usr/bin/");
        }

        logger.error("Chrome non trovato in nessun percorso standard");
        return null;
    }

    /**
     * Cerca Chrome nel PATH di sistema usando which command.
     * Ottimizzato per ambiente Linux Docker.
     *
     * @return Il percorso di Chrome trovato nel PATH, o null se non trovato
     */
    private static String findInPath() {
        String[] commands = {
                "google-chrome",
                "google-chrome-stable",
                "chromium-browser",
                "chromium"
        };

        for (String command : commands) {
            try {
                Process process = Runtime.getRuntime().exec(new String[]{"which", command});
                process.waitFor();

                if (process.exitValue() == 0) {
                    try (java.io.BufferedReader reader = new java.io.BufferedReader(
                            new java.io.InputStreamReader(process.getInputStream()))) {
                        String path = reader.readLine();
                        if (path != null && !path.trim().isEmpty()) {
                            return path.trim();
                        }
                    }
                }
            } catch (Exception e) {
                logger.debug("Errore ricerca {} nel PATH: {}", command, e.getMessage());
            }
        }

        return null;
    }

    /**
     * Test rapido per verificare che Chrome sia funzionante.
     *
     * @param chromePath Percorso di Chrome da testare
     * @return true se Chrome risponde correttamente, false altrimenti
     */
    private static boolean testChrome(String chromePath) {
        try {
            ProcessBuilder pb = new ProcessBuilder(chromePath, "--version");
            pb.redirectErrorStream(true);
            Process process = pb.start();

            int exitCode = process.waitFor();
            return exitCode == 0;
        } catch (Exception e) {
            logger.debug("Test Chrome fallito: {}", e.getMessage());
            return false;
        }
    }

    /**
     * Verifica se un percorso rappresenta un eseguibile valido e accessibile.
     *
     * @param path Il percorso da verificare
     * @return true se il percorso è un file eseguibile valido, false altrimenti
     */
    private static boolean isValidExecutable(String path) {
        if (path == null || path.trim().isEmpty()) {
            return false;
        }

        File file = new File(path);
        return file.exists() && file.canExecute();
    }

    /**
     * Verifica se Chrome è disponibile e configurato correttamente.
     *
     * @return true se Chrome è disponibile per l'uso, false altrimenti
     */
    public boolean isChromeAvailable() {
        return chromePath != null;
    }

    /**
     * Restituisce il percorso dell'eseguibile Chrome configurato.
     *
     * @return Il percorso assoluto di Chrome, o null se non disponibile
     */
    public String getChromePath() {
        return chromePath;
    }

    /**
     * Restituisce gli argomenti di Chrome ottimizzati per Docker.
     *
     * @return Array di argomenti per Chrome
     */
    public String[] getChromeArgs() {
        try {
            Properties config = loadConfig();
            String args = config.getProperty("chrome.args",
                    "--headless --disable-gpu --no-sandbox --disable-dev-shm-usage --remote-debugging-port=0");
            return args.split("\\s+");
        } catch (Exception e) {
            logger.warn("Errore caricamento argomenti Chrome, uso default: {}", e.getMessage());
            return new String[]{"--headless", "--disable-gpu", "--no-sandbox", "--disable-dev-shm-usage"};
        }
    }

    /**
     * Valida che Chrome sia disponibile e funzionante eseguendo un test completo.
     *
     * @throws IOException Se Chrome non è disponibile o non risponde correttamente
     */
    public void validateChrome() throws IOException {
        if (!isChromeAvailable()) {
            throw new IOException("Chrome non disponibile per conversione PDF - Verifica installazione Docker");
        }

        // Test più approfondito con timeout
        try {
            ProcessBuilder pb = new ProcessBuilder(chromePath, "--version");
            pb.redirectErrorStream(true);
            Process process = pb.start();

            boolean finished = process.waitFor(10, java.util.concurrent.TimeUnit.SECONDS);

            if (!finished) {
                process.destroyForcibly();
                throw new IOException("Chrome non risponde entro il timeout");
            }

            if (process.exitValue() != 0) {
                throw new IOException("Chrome restituisce codice di errore: " + process.exitValue());
            }

            logger.info("Chrome validato con successo: {}", chromePath);

        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new IOException("Test Chrome interrotto", e);
        }
    }

    /**
     * Crea un ProcessBuilder configurato per Chrome con argomenti ottimizzati per Docker.
     *
     * @param additionalArgs Argomenti aggiuntivi specifici per l'operazione
     * @return ProcessBuilder configurato
     * @throws IOException Se Chrome non è disponibile
     */
    public ProcessBuilder createChromeProcess(String... additionalArgs) throws IOException {
        if (!isChromeAvailable()) {
            throw new IOException("Chrome non disponibile");
        }

        String[] baseArgs = getChromeArgs();
        String[] allArgs = new String[1 + baseArgs.length + additionalArgs.length];

        allArgs[0] = chromePath;
        System.arraycopy(baseArgs, 0, allArgs, 1, baseArgs.length);
        System.arraycopy(additionalArgs, 0, allArgs, 1 + baseArgs.length, additionalArgs.length);

        ProcessBuilder pb = new ProcessBuilder(allArgs);
        pb.redirectErrorStream(true);

        return pb;
    }
}