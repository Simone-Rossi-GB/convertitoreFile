package converters.mailConverters;

import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.LogManager;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;

/**
 * Gestisce la localizzazione e configurazione di Chrome per la conversione PDF
 */
public class ChromeManager {

    private static final Logger logger = LogManager.getLogger(ChromeManager.class);
    private static ChromeManager instance;
    private static String chromePath;
    private static boolean initialized = false;

    private ChromeManager() {}

    public static synchronized ChromeManager getInstance() {
        if (instance == null) {
            instance = new ChromeManager();
            initialize();
        }
        return instance;
    }

    private static void initialize() {
        if (initialized) return;

        try {
            // Carica configurazione
            Properties config = loadConfig();

            // Trova Chrome
            chromePath = findChromeExecutable(config);

            if (chromePath != null) {
                logger.info("Chrome configurato: {}", chromePath);
            } else {
                logger.warn("Chrome non trovato - EMLChromeHeadlessConverter non disponibile");
            }

            initialized = true;

        } catch (Exception e) {
            logger.error("Errore nell'inizializzazione Chrome", e);
            initialized = true; // Evita retry infiniti
        }
    }

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

    private static String findChromeExecutable(Properties config) {
        String os = System.getProperty("os.name").toLowerCase();

        // 1. Prova variabile d'ambiente
        String envPath = System.getenv("CHROME_PATH");
        if (isValidExecutable(envPath)) {
            return envPath;
        }

        // 2. Prova Chrome bundled
        String bundledPath = getBundledChromePath(os, config);
        if (isValidExecutable(bundledPath)) {
            return bundledPath;
        }

        // 3. Prova Chrome di sistema
        String systemPath = findSystemChrome(os, config);
        if (isValidExecutable(systemPath)) {
            return systemPath;
        }

        return null;
    }

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

            // Per macOS, verifica che sia un bundle valido
            if (os.contains("mac") && relativePath.endsWith(".app")) {
                File chromeExecutable = new File(chromeFile, "Contents/MacOS/Google Chrome");
                if (chromeExecutable.exists()) {
                    return chromeExecutable.getAbsolutePath();
                }
                // Prova con Chromium
                chromeExecutable = new File(chromeFile, "Contents/MacOS/Chromium");
                if (chromeExecutable.exists()) {
                    return chromeExecutable.getAbsolutePath();
                }
            }

            return chromeFile.getAbsolutePath();
        }

        return null;
    }

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

        // Prova anche PATH
        return findInPath(os);
    }

    private static String findInPath(String os) {
        try {
            String[] commands;
            if (os.contains("win")) {
                commands = new String[]{"where", "chrome.exe"};
            } else {
                commands = new String[]{"which", "google-chrome"};
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

    private static boolean isValidExecutable(String path) {
        if (path == null || path.trim().isEmpty()) {
            return false;
        }

        File file = new File(path);
        return file.exists() && file.canExecute();
    }

    public boolean isChromeAvailable() {
        return chromePath != null;
    }

    public String getChromePath() {
        return chromePath;
    }

    public void validateChrome() throws IOException {
        if (!isChromeAvailable()) {
            throw new IOException("Chrome non disponibile per conversione PDF");
        }

        // Test veloce
        try {
            Process process = new ProcessBuilder(chromePath, "--version").start();
            int exitCode = process.waitFor();
            if (exitCode != 0) {
                throw new IOException("Chrome non risponde correttamente");
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new IOException("Test Chrome interrotto", e);
        }
    }
}