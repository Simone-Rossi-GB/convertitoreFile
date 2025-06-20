package WebService;

import Converters.Converter;
import converter.ConverterConfig;
import Converters.ConverterContext;
import converter.Log;
import com.google.gson.Gson;
import java.io.*;
import java.nio.file.*;
import java.util.*;
import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.LogManager;

/**
 * Engine per il WebService - versione con controlli automatici sui convertitori
 */
public class EngineWebService {
    private ConverterConfig config;
    private static final String CONFIG_FILE_PATH = "src/main/java/converter/config/config.json";
    private static final Logger logger = LogManager.getLogger(EngineWebService.class);

    public EngineWebService() {
        // Imposta la proprietà di sistema per indicare che siamo nel web service
        System.setProperty("converter.environment", "webservice");
        setConfig();
    }

    public void setConfig() {
        try (FileReader reader = new FileReader(CONFIG_FILE_PATH)) {
            Gson gson = new Gson();
            config = gson.fromJson(reader, ConverterConfig.class);
            if (config == null) {
                logger.error("WebService: l'oggetto config non esiste");
                Log.addMessage("ERRORE WebService: l'oggetto config non esiste");
                throw new NullPointerException("L'oggetto config non esiste");
            }
            logger.info("WebService: Configurazione caricata correttamente da config.json");
            Log.addMessage("WebService: Configurazione caricata correttamente da config.json");
        } catch (Exception e) {
            logger.error("WebService: Lettura del file di configurazione fallita");
            Log.addMessage("ERRORE WebService: Lettura del file di configurazione fallita");
            throw new RuntimeException("Errore nella lettura del file di configurazione", e);
        }
    }

    public List<String> getPossibleConversions(String extension) throws Exception {
        if (extension == null) {
            logger.error("WebService: Parametro extension nullo");
            Log.addMessage("ERRORE WebService: Parametro extension nullo");
            throw new NullPointerException("L'oggetto extension non esiste");
        }

        if (config == null || config.getConversions() == null || !config.getConversions().containsKey(extension)) {
            logger.error("WebService: Configurazione mancante o conversione non supportata per: {}", extension);
            Log.addMessage("ERRORE WebService: Configurazione mancante o conversione non supportata per: " + extension);
            throw new Exception("Config assente o conversione non supportata");
        }

        logger.info("WebService: Formati disponibili per la conversione da {} ottenuti con successo", extension);
        Log.addMessage("WebService: Formati disponibili per la conversione da " + extension + " ottenuti con successo");
        return new ArrayList<>(config.getConversions().get(extension).keySet());
    }

    /**
     * Verifica se un file può essere convertito senza parametri aggiuntivi
     * @param srcExt Estensione del file sorgente
     * @param srcFile File sorgente
     * @return ConversionInfo con informazioni sui parametri richiesti
     */
    public ConversionInfo checkConversionRequirements(String srcExt, File srcFile) throws Exception {
        String converterClassName = getConverterClassName(srcExt);

        try {
            Class<?> clazz = Class.forName(converterClassName);
            Converter converter = (Converter) clazz.getDeclaredConstructor().newInstance();

            boolean canConvertWithoutParams = converter.canConvertWithoutParameters(srcFile);
            boolean requiresPassword = converter.requiresPassword(srcFile);
            boolean supportsBooleanOption = converter.supportsBooleanOption(srcFile);
            String passwordDescription = converter.getStringParameterDescription();
            String booleanDescription = converter.getBooleanOptionDescription();

            return new ConversionInfo(
                    canConvertWithoutParams,
                    requiresPassword,
                    supportsBooleanOption,
                    passwordDescription,
                    booleanDescription
            );

        } catch (Exception e) {
            logger.error("WebService: Errore nel controllo dei requisiti di conversione: {}", e.getMessage());
            Log.addMessage("ERRORE WebService: Errore nel controllo dei requisiti di conversione: " + e.getMessage());
            throw new Exception("Errore nel controllo dei requisiti: " + e.getMessage());
        }
    }

    /**
     * Conversione intelligente che determina automaticamente i parametri necessari
     */
    public File conversione(String srcExt, String outExt, File srcFile, File outputDirectory) throws Exception {
        return conversione(srcExt, outExt, srcFile, outputDirectory, null, false);
    }

    /**
     * Conversione con parametri opzionali
     */
    public File conversione(String srcExt, String outExt, File srcFile, File outputDirectory,
                            String password, boolean booleanOption) throws Exception {

        // Imposta il contesto del web service per questo thread
        ConverterContext.setEnvironment(ConverterContext.Environment.WEBSERVICE);

        try {
            String converterClassName = checkParameters(srcExt, outExt, srcFile);
            Class<?> clazz = Class.forName(converterClassName);
            Converter converter = (Converter) clazz.getDeclaredConstructor().newInstance();

            // Verifica automaticamente i requisiti del converter
            ConversionInfo conversionInfo = checkConversionRequirements(srcExt, srcFile);

            // Se il file richiede una password ma non è stata fornita, lancia un'eccezione specifica
            if (conversionInfo.requiresPassword && (password == null || password.isEmpty())) {
                throw new PasswordRequiredException("Il file richiede una password per essere convertito");
            }

            // Crea directory temporanea per questa conversione specifica
            Path conversionTempDir = Files.createTempDirectory("webservice_conversion_");
            logger.info("WebService: Creata directory temporanea: {}", conversionTempDir);
            Log.addMessage("WebService: Creata directory temporanea: " + conversionTempDir);

            try {
                // Copia il file nella directory temporanea
                Path tempPath = conversionTempDir.resolve(srcFile.getName());
                Files.copy(srcFile.toPath(), tempPath, StandardCopyOption.REPLACE_EXISTING);
                File tempFile = tempPath.toFile();

                logger.info("WebService: Avvio conversione con: {}", converterClassName);
                Log.addMessage("WebService: Avvio conversione con: " + converterClassName);

                List<File> outFiles;

                // Chiama il metodo appropriato del converter in base ai parametri disponibili
                if (password != null && !password.isEmpty() && conversionInfo.supportsBooleanOption) {
                    outFiles = converter.convert(tempFile, password, booleanOption);
                } else if (password != null && !password.isEmpty()) {
                    outFiles = converter.convert(tempFile, password);
                } else if (conversionInfo.supportsBooleanOption) {
                    outFiles = converter.convert(tempFile, booleanOption);
                } else {
                    outFiles = converter.convert(tempFile);
                }

                // Verifica che abbiamo almeno un file di output
                if (outFiles == null || outFiles.isEmpty()) {
                    throw new Exception("Il converter non ha prodotto file di output validi");
                }

                // Prende il primo file convertito
                File convertedFile = outFiles.get(0);

                // Crea il file di output finale
                String outputFileName = generateOutputFileName(srcFile.getName(), outExt);
                File finalOutputFile = new File(outputDirectory, outputFileName);

                // Sposta il file convertito nella directory di output specificata
                Files.move(convertedFile.toPath(), finalOutputFile.toPath(), StandardCopyOption.REPLACE_EXISTING);

                logger.info("WebService: Conversione completata con successo: {} -> {}",
                        srcFile.getName(), finalOutputFile.getName());
                Log.addMessage("WebService: Conversione completata con successo: " +
                        srcFile.getName() + " -> " + finalOutputFile.getName());

                return finalOutputFile;

            } catch (PasswordRequiredException e) {
                throw e; // Rilancia l'eccezione specifica per la password
            } catch (Exception e) {
                logger.error("ERRORE WebService: Errore durante la conversione del file {}: {}",
                        srcFile.getName(), e.getMessage());
                Log.addMessage("ERRORE WebService: Errore durante la conversione del file " +
                        srcFile.getName() + ": " + e.getMessage());
                throw new Exception("Errore durante la conversione: " + e.getMessage(), e);
            } finally {
                // Pulizia finale: elimina la directory temporanea e tutto il suo contenuto
                try {
                    deleteDirectoryRecursively(conversionTempDir);
                    logger.info("WebService: Directory temporanea eliminata: {}", conversionTempDir);
                    Log.addMessage("WebService: Directory temporanea eliminata: " + conversionTempDir);
                } catch (Exception e) {
                    logger.error("WebService: Errore eliminazione directory temporanea: {}", e.getMessage());
                    Log.addMessage("WebService: Errore eliminazione directory temporanea: " + e.getMessage());
                }
            }
        } finally {
            // Pulisci il contesto del thread
            ConverterContext.clearEnvironment();
        }
    }

    private String generateOutputFileName(String originalName, String newExtension) {
        int lastDot = originalName.lastIndexOf('.');
        if (lastDot == -1) {
            return originalName + "." + newExtension;
        }
        return originalName.substring(0, lastDot) + "." + newExtension;
    }

    private String getConverterClassName(String srcExt) throws Exception {
        Map<String, Map<String, String>> conversions = config.getConversions();
        if (conversions == null || !conversions.containsKey(srcExt)) {
            throw new Exception("Conversione da " + srcExt + " non supportata");
        }
        return conversions.get(srcExt).values().iterator().next();
    }

    private String checkParameters(String srcExt, String outExt, File srcFile) throws Exception {
        if (srcExt == null) {
            throw new NullPointerException("L'oggetto srcExt non esiste");
        }
        if (outExt == null) {
            throw new NullPointerException("L'oggetto outExt non esiste");
        }
        if (srcFile == null) {
            throw new NullPointerException("L'oggetto srcFile non esiste");
        }

        Map<String, Map<String, String>> conversions = config.getConversions();
        if (conversions == null || !conversions.containsKey(srcExt)) {
            throw new Exception("Conversione da " + srcExt + " non supportata");
        }

        Map<String, String> possibleConversions = conversions.get(srcExt);
        String converterClassName = possibleConversions.get(outExt);
        if (converterClassName == null) {
            throw new Exception("Conversione da " + srcExt + " a " + outExt + " non supportata");
        }

        return converterClassName;
    }

    private void deleteDirectoryRecursively(Path directory) throws IOException {
        if (Files.exists(directory)) {
            Files.walk(directory)
                    .sorted(Comparator.reverseOrder())
                    .map(Path::toFile)
                    .forEach(File::delete);
        }
    }

    /**
     * Classe per contenere informazioni sui requisiti di conversione
     */
    public static class ConversionInfo {
        public final boolean canConvertWithoutParams;
        public final boolean requiresPassword;
        public final boolean supportsBooleanOption;
        public final String passwordDescription;
        public final String booleanDescription;

        public ConversionInfo(boolean canConvertWithoutParams, boolean requiresPassword,
                              boolean supportsBooleanOption, String passwordDescription,
                              String booleanDescription) {
            this.canConvertWithoutParams = canConvertWithoutParams;
            this.requiresPassword = requiresPassword;
            this.supportsBooleanOption = supportsBooleanOption;
            this.passwordDescription = passwordDescription;
            this.booleanDescription = booleanDescription;
        }
    }

    /**
     * Eccezione specifica per file che richiedono password
     */
    public static class PasswordRequiredException extends Exception {
        public PasswordRequiredException(String message) {
            super(message);
        }
    }
}