package webService.server.converters.mailConverters;

import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.LogManager;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;

/**
 * Gestisce la localizzazione e configurazione di Chrome Headless Shell per la conversione PDF.
 * Implementa il pattern Singleton e cerca automaticamente l'eseguibile Chrome nei percorsi
 * standard del sistema e nelle configurazioni personalizzate.
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
     * Questo metodo viene chiamato automaticamente al primo accesso all'istanza singleton.
     */
    private static void initialize() {
        if (initialized) return;

        try {
            // Carica configurazione
            Properties config = loadConfig();

            // Trova Chrome Headless Shell
            chromePath = findChromeExecutable(config);

            if (chromePath != null) {
                logger.info("Chrome Headless Shell configurato: {}", chromePath);
            } else {
                logger.warn("Chrome Headless Shell non trovato - Converter non disponibile");
            }

            initialized = true;

        } catch (Exception e) {
            logger.error("Errore nell'inizializzazione Chrome Headless Shell", e);
            initialized = true; // Evita retry infiniti
        }
    }

    /**
     * Carica le proprietà di configurazione da file converter.properties.
     * Cerca prima nel classpath, poi nel filesystem locale.
     *
     * @return Oggetto Properties contenente la configurazione caricata
     * @throws IOException Se si verificano errori durante il caricamento del file
     */
    private static Properties loadConfig() throws IOException {
        Properties config = new Properties();

        // Prova a caricare dal classpath
        try (InputStream is = ChromeManager.class.getClassLoader().getResourceAsStream("converter.properties")) {
            if (is != null) {
                config.load(is);
            }
        }

        // Prova a caricare converter.properties
        File configFile = new File("src/main/java/converters/mailConverters/converter.properties");
        if (configFile.exists()) {
            try (InputStream is = new java.io.FileInputStream(configFile)) {
                config.load(is);
            }
        }

        return config;
    }

    /**
     * Cerca l'eseguibile Chrome utilizzando diverse strategie di ricerca.
     * Prova in ordine: variabile d'ambiente, versione bundled, installazione di sistema.
     *
     * @param config Configurazione caricata contenente i percorsi personalizzati
     * @return Il percorso assoluto dell'eseguibile Chrome, o null se non trovato
     */
    private static String findChromeExecutable(Properties config) {
        String os = System.getProperty("os.name").toLowerCase();

        // 1. Prova variabile d'ambiente
        String envPath = System.getenv("CHROME_PATH");
        if (isValidExecutable(envPath)) {
            return envPath;
        }

        // 2. Prova Chrome Headless Shell bundled
        String bundledPath = getBundledChromePath(os, config);
        if (isValidExecutable(bundledPath)) {
            return bundledPath;
        }

        // 3. Prova Chrome di sistema (fallback)
        String systemPath = findSystemChrome(os, config);
        if (isValidExecutable(systemPath)) {
            return systemPath;
        }

        return null;
    }

    /**
     * Ottiene il percorso dell'eseguibile Chrome bundled con l'applicazione.
     * Usa la configurazione per determinare il percorso relativo specifico per il sistema operativo.
     *
     * @param os Nome del sistema operativo in lowercase
     * @param config Configurazione contenente i percorsi per ogni OS
     * @return Il percorso assoluto del Chrome bundled, o null se non configurato/trovato
     */
    private static String getBundledChromePath(String os, Properties config) {
        String configKey;
        if (os.contains("win")) {
            configKey = "chrome.windows.path";
        } else if (os.contains("mac")) {
            configKey = "chrome.mac.path";
        } else {
            configKey = "chrome.linux.path";
        }

        String relativePath = config.getProperty(configKey);
        if (relativePath != null) {
            // Converti in percorso assoluto dalla directory dell'applicazione
            File baseDir = new File(System.getProperty("user.dir"));
            File chromeFile = new File(baseDir, relativePath);

            return chromeFile.getAbsolutePath();
        }

        // Se non c'è config, prova percorsi predefiniti per Headless Shell
        return getDefaultHeadlessShellPath(os);
    }

    /**
     * Restituisce il percorso predefinito di Chrome Headless Shell per il sistema operativo corrente.
     * Cerca nella struttura di directory standard lib/[os]/chrome-headless-shell.
     *
     * @param os Nome del sistema operativo in lowercase
     * @return Il percorso assoluto del Chrome Headless Shell predefinito, o null se non trovato
     */
    private static String getDefaultHeadlessShellPath(String os) {
        File baseDir = new File(System.getProperty("user.dir"));

        if (os.contains("win")) {
            // Windows: lib/windows/chrome-headless-shell.exe
            File chromeExe = new File(baseDir, "lib/windows/chrome-headless-shell.exe");
            if (chromeExe.exists()) {
                return chromeExe.getAbsolutePath();
            }
        } else if (os.contains("mac")) {
            // macOS: lib/mac/chrome-headless-shell
            File chromeExe = new File(baseDir, "lib/mac/chrome-headless-shell");
            if (chromeExe.exists()) {
                return chromeExe.getAbsolutePath();
            }
        } else {
            // Linux: lib/linux/chrome-headless-shell
            File chromeExe = new File(baseDir, "lib/linux/chrome-headless-shell");
            if (chromeExe.exists()) {
                return chromeExe.getAbsolutePath();
            }
        }

        return null;
    }

    /**
     * Cerca l'installazione di Chrome di sistema utilizzando i percorsi di fallback configurati.
     * Se la configurazione non specifica percorsi, cerca automaticamente nel PATH.
     *
     * @param os Nome del sistema operativo in lowercase
     * @param config Configurazione contenente i percorsi di fallback per ogni OS
     * @return Il percorso dell'installazione Chrome di sistema, o null se non trovata
     */
    private static String findSystemChrome(String os, Properties config) {
        String configKey;
        if (os.contains("win")) {
            configKey = "chrome.fallback.windows";
        } else if (os.contains("mac")) {
            configKey = "chrome.fallback.mac";
        } else {
            configKey = "chrome.fallback.linux";
        }

        String fallbackPaths = config.getProperty(configKey, "");
        String[] paths = fallbackPaths.split(";");

        for (String path : paths) {
            if (isValidExecutable(path.trim())) {
                return path.trim();
            }
        }

        // Prova anche PATH per Chrome Headless Shell e Chrome normale
        return findInPath(os);
    }

    /**
     * Cerca Chrome nel PATH di sistema utilizzando i comandi where/which del sistema operativo.
     * Prova prima Chrome Headless Shell, poi fallback su Chrome normale.
     *
     * @param os Nome del sistema operativo in lowercase
     * @return Il percorso di Chrome trovato nel PATH, o null se non trovato
     */
    private static String findInPath(String os) {
        try {
            String[] commands;
            if (os.contains("win")) {
                // Prova prima headless shell, poi chrome normale
                String[] headlessCommands = {"where", "chrome-headless-shell.exe"};
                String[] chromeCommands = {"where", "chrome.exe"};

                // Prova headless shell
                Process process = Runtime.getRuntime().exec(headlessCommands);
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

                // Fallback su chrome normale
                commands = chromeCommands;
            } else {
                // Unix: prova prima headless shell, poi chrome normale
                String[] headlessCommands = {"which", "chrome-headless-shell"};
                String[] chromeCommands = {"which", "google-chrome"};

                // Prova headless shell
                Process process = Runtime.getRuntime().exec(headlessCommands);
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

                // Fallback su chrome normale
                commands = chromeCommands;
            }

            Process process = Runtime.getRuntime().exec(commands);
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
            logger.debug("Errore ricerca Chrome in PATH", e);
        }

        return null;
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
     * Valida che Chrome sia disponibile e funzionante eseguendo un test rapido.
     * Esegue il comando chrome --version per verificare che l'eseguibile risponda correttamente.
     *
     * @throws IOException Se Chrome non è disponibile o non risponde correttamente
     */
    public void validateChrome() throws IOException {
        if (!isChromeAvailable()) {
            throw new IOException("Chrome Headless Shell non disponibile per conversione PDF");
        }

        // Test veloce
        try {
            Process process = new ProcessBuilder(chromePath, "--version").start();
            int exitCode = process.waitFor();
            if (exitCode != 0) {
                throw new IOException("Chrome Headless Shell non risponde correttamente");
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new IOException("Test Chrome Headless Shell interrotto", e);
        }
    }
}