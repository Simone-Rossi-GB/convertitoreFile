package WebService;

import Converters.Converter;
import converter.ConverterConfig;
import converter.Log;
import com.google.gson.Gson;
import java.io.*;
import java.nio.file.*;
import java.util.*;
import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.LogManager;

/**
 * Engine per il WebService - versione modificata dell'Engine locale
 * Gestisce le conversioni senza spostare automaticamente i file
 */
public class EngineWebService {
    private ConverterConfig config;
    private static final String CONFIG_FILE_PATH = "src/main/java/converter/config/config.json";
    private static final Logger logger = LogManager.getLogger(EngineWebService.class);

    /**
     * Costruttore: carica il file config.json
     */
    public EngineWebService() {
        setConfig();
    }

    /**
     * Carica il file config.json
     */
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

    /**
     * Ritorna la stringa che rappresenta il contenuto del json
     */
    public String getConfigAsJson() throws Exception {
        try {
            logger.info("WebService: Lettura del contenuto di config.json eseguita con successo");
            Log.addMessage("WebService: Lettura del contenuto di config.json eseguita con successo");
            return new String(Files.readAllBytes(Paths.get(CONFIG_FILE_PATH)));
        } catch (IOException e) {
            logger.error("WebService: Lettura del file di configurazione non riuscita");
            Log.addMessage("ERRORE WebService: Lettura del file di configurazione non riuscita");
            throw new Exception("Errore nella lettura del file di configurazione: " + e.getMessage(), e);
        }
    }

    /**
     * Ritorna i formati in cui un file può essere convertito
     */
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
     * Esecuzione conversione per WebService - NON sposta i file automaticamente
     */
    // SOSTITUISCI IL METODO executeConversionWebService con questa versione corretta:
    public File conversione(String srcExt, String outExt, File srcFile, File outputDirectory) throws Exception {
        String converterClassName = checkParameters(srcExt, outExt, srcFile);

        Class<?> clazz = Class.forName(converterClassName);
        Converter converter = (Converter) clazz.getDeclaredConstructor().newInstance();

        // Crea directory temporanea per questa conversione specifica
        Path conversionTempDir = Files.createTempDirectory("webservice_conversion_");
        logger.info("WebService: Creata directory temporanea: {}", conversionTempDir);
        Log.addMessage("WebService: Creata directory temporanea: " + conversionTempDir);

        try {
            // Copia il file nella directory temporanea
            Path tempPath = conversionTempDir.resolve(srcFile.getName());
            Files.copy(srcFile.toPath(), tempPath, StandardCopyOption.REPLACE_EXISTING);
            logger.info("WebService: Copia del file nella cartella temporanea: {}", tempPath);
            Log.addMessage("WebService: Copia del file nella cartella temporanea: " + tempPath);
            File tempFile = tempPath.toFile();

            // Rinomina il file con suffisso per evitare conflitti

            // SALVA IL NOME DEL FILE TEMPORANEO PER USO NELLA LAMBDA (FINAL)
            final String tempFileName = tempFile.getName();

            logger.info("WebService: Avvio conversione con: {}", converterClassName);
            Log.addMessage("WebService: Avvio conversione con: " + converterClassName);
            System.out.println("WebService: File input: " + tempFile.getAbsolutePath());
            System.out.println("WebService: Directory temp: " + conversionTempDir.toAbsolutePath());

            List<File> outFiles;

            // Soluzione: Salva i percorsi originali e li ripristina dopo
            String originalSuccessDir = config.getSuccessOutputDir();
            String originalErrorDir = config.getErrorOutputDir();

            try {
                // Crea una sottodirectory per i file convertiti
                Path tempOutputDir = conversionTempDir.resolve("output");
                Files.createDirectories(tempOutputDir);

                System.setProperty("webservice.temp.output", tempOutputDir.toString());
                outFiles = converter.convert(tempFile);


                // Se il converter non ha prodotto file, proviamo a cercarli nella directory di successo dell'engine
                if (outFiles == null || outFiles.isEmpty()) {
                    logger.warn("WebService: Il converter non ha restituito file, cerco nella directory di successo...");
                    Log.addMessage("WebService: Il converter non ha restituito file, cerco nella directory di successo...");

                    // Cerca i file nella directory di successo dell'engine
                    File successDir = new File(originalSuccessDir);
                    if (successDir.exists() && successDir.isDirectory()) {
                        File[] potentialFiles = successDir.listFiles((dir, name) ->
                                name.toLowerCase().endsWith("." + outExt.toLowerCase())
                        );

                        if (potentialFiles != null && potentialFiles.length > 0) {
                            // Ordina per data di modifica (più recente prima)
                            Arrays.sort(potentialFiles, (a, b) ->
                                    Long.compare(b.lastModified(), a.lastModified())
                            );

                            outFiles = Arrays.asList(potentialFiles);
                            logger.info("WebService: Trovati {} file nella directory di successo", outFiles.size());
                            Log.addMessage("WebService: Trovati " + outFiles.size() + " file nella directory di successo");
                        }
                    }

                    // Se ancora non troviamo file, cerca nella directory temporanea
                    if (outFiles == null || outFiles.isEmpty()) {
                        logger.warn("WebService: Cerco file nella directory temporanea...");
                        Log.addMessage("WebService: Cerco file nella directory temporanea...");

                        // USA LA VARIABILE FINAL tempFileName INVECE DI tempFile.getName()
                        File[] tempFiles = conversionTempDir.toFile().listFiles((dir, name) ->
                                name.toLowerCase().endsWith("." + outExt.toLowerCase()) &&
                                        !name.equals(tempFileName)  // ← CORRETTO: usa tempFileName (final)
                        );

                        if (tempFiles != null && tempFiles.length > 0) {
                            outFiles = Arrays.asList(tempFiles);
                            logger.info("WebService: Trovati {} file nella directory temporanea", outFiles.size());
                            Log.addMessage("WebService: Trovati " + outFiles.size() + " file nella directory temporanea");
                        }
                    }
                }

            } finally {
                // Ripristina le directory originali (se fossero state modificate)
                System.clearProperty("webservice.temp.output");
            }

            // Elimina il file temporaneo di input
            Files.deleteIfExists(tempFile.toPath());
            logger.info("WebService: File temporaneo di input eliminato: {}", tempFile.getPath());
            Log.addMessage("WebService: File temporaneo di input eliminato: " + tempFile.getPath());

            // Verifica che abbiamo almeno un file di output
            if (outFiles == null || outFiles.isEmpty()) {
                throw new Exception("Il converter non ha prodotto file di output validi");
            }

            // Prende il primo file convertito (o l'unico nel caso di merge)
            File convertedFile = outFiles.get(0);
            logger.info("WebService: File convertito trovato: {}", convertedFile.getAbsolutePath());
            Log.addMessage("WebService: File convertito trovato: " + convertedFile.getAbsolutePath());

            // Rimuove il suffisso dal nome se presente
            String cleanName = convertedFile.getName().replaceAll("-\\$\\$.*?\\$\\$-", "");
            File finalOutputFile = new File(outputDirectory, cleanName);

            // Sposta il file convertito nella directory di output specificata
            Files.move(convertedFile.toPath(), finalOutputFile.toPath(), StandardCopyOption.REPLACE_EXISTING);
            logger.info("WebService: File convertito spostato in: {}", finalOutputFile.getAbsolutePath());
            Log.addMessage("WebService: File convertito spostato in: " + finalOutputFile.getAbsolutePath());

            // Pulizia: elimina eventuali altri file di output
            for (int i = 1; i < outFiles.size(); i++) {
                try {
                    Files.deleteIfExists(outFiles.get(i).toPath());
                } catch (Exception e) {
                    logger.error("WebService: Errore eliminazione file extra: {}", e.getMessage());
                    Log.addMessage("WebService: Errore eliminazione file extra: " + e.getMessage());
                }
            }

            logger.info("WebService: Conversione completata con successo: " + srcFile.getName() + " -> " + finalOutputFile.getName());
            Log.addMessage("WebService: Conversione completata con successo: " + srcFile.getName() + " -> " + finalOutputFile.getName());
            return finalOutputFile;

        } catch (Exception e) {
            logger.error("ERRORE WebService: Errore durante la conversione del file {}: {}", srcFile.getName(), e.getMessage());
            Log.addMessage("ERRORE WebService: Errore durante la conversione del file " + srcFile.getName() + ": " + e.getMessage());
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
    }

    private File executeConversionWebService(String srcExt, String outExt, File srcFile, String extraParam, Boolean union) throws Exception {
        String converterClassName = checkParameters(srcExt, outExt, srcFile);

        Class<?> clazz = Class.forName(converterClassName);
        Converter converter = (Converter) clazz.getDeclaredConstructor().newInstance();

        // Crea directory temporanea per questa conversione specifica
        Path conversionTempDir = Files.createTempDirectory("webservice_conversion_");
        logger.info("WebService: Creata directory temporanea: {}", conversionTempDir);
        Log.addMessage("WebService: Creata directory temporanea: " + conversionTempDir);

        try {
            // Copia il file nella directory temporanea
            Path tempPath = conversionTempDir.resolve(srcFile.getName());
            Files.copy(srcFile.toPath(), tempPath, StandardCopyOption.REPLACE_EXISTING);
            logger.info("WebService: Copia del file nella cartella temporanea: {}", tempPath);
            Log.addMessage("WebService: Copia del file nella cartella temporanea: " + tempPath);
            File tempFile = tempPath.toFile();

            // Rinomina il file con suffisso per evitare conflitti
            // SALVA IL NOME DEL FILE TEMPORANEO PER USO NELLA LAMBDA (FINAL)
            final String tempFileName = tempFile.getName();

            logger.info("WebService: Avvio conversione con: {}", converterClassName);
            Log.addMessage("WebService: Avvio conversione con: " + converterClassName);
            System.out.println("WebService: File input: " + tempFile.getAbsolutePath());
            System.out.println("WebService: Directory temp: " + conversionTempDir.toAbsolutePath());

            List<File> outFiles;

            // Soluzione: Salva i percorsi originali e li ripristina dopo
            String originalSuccessDir = config.getSuccessOutputDir();
            String originalErrorDir = config.getErrorOutputDir();

            try {
                // Crea una sottodirectory per i file convertiti
                Path tempOutputDir = conversionTempDir.resolve("output");
                Files.createDirectories(tempOutputDir);

                System.setProperty("webservice.temp.output", tempOutputDir.toString());

                outFiles = converter.convert(tempFile);

                System.out.println("WebService: File di output dal converter: " + outFiles);

                // Se il converter non ha prodotto file, proviamo a cercarli nella directory di successo dell'engine
                if (outFiles == null || outFiles.isEmpty()) {
                    logger.warn("WebService: Il converter non ha restituito file, cerco nella directory di successo...");
                    Log.addMessage("WebService: Il converter non ha restituito file, cerco nella directory di successo...");

                    // Cerca i file nella directory di successo dell'engine
                    File successDir = new File(originalSuccessDir);
                    if (successDir.exists() && successDir.isDirectory()) {
                        File[] potentialFiles = successDir.listFiles((dir, name) ->
                                name.toLowerCase().endsWith("." + outExt.toLowerCase())
                        );

                        if (potentialFiles != null && potentialFiles.length > 0) {
                            // Ordina per data di modifica (più recente prima)
                            Arrays.sort(potentialFiles, (a, b) ->
                                    Long.compare(b.lastModified(), a.lastModified())
                            );

                            outFiles = Arrays.asList(potentialFiles);
                            logger.info("WebService: Trovati {} file nella directory di successo", outFiles.size());
                            Log.addMessage("WebService: Trovati " + outFiles.size() + " file nella directory di successo");
                        }
                    }

                    // Se ancora non troviamo file, cerca nella directory temporanea
                    if (outFiles == null || outFiles.isEmpty()) {
                        logger.info("WebService: Cerco file nella directory temporanea...");
                        Log.addMessage("WebService: Cerco file nella directory temporanea...");

                        // USA LA VARIABILE FINAL tempFileName INVECE DI tempFile.getName()
                        File[] tempFiles = conversionTempDir.toFile().listFiles((dir, name) ->
                                name.toLowerCase().endsWith("." + outExt.toLowerCase()) &&
                                        !name.equals(tempFileName)  // ← CORRETTO: usa tempFileName (final)
                        );

                        if (tempFiles != null && tempFiles.length > 0) {
                            outFiles = Arrays.asList(tempFiles);
                            logger.info("WebService: Trovati {} file nella directory temporanea", outFiles.size());
                            Log.addMessage("WebService: Trovati " + outFiles.size() + " file nella directory temporanea");
                        }
                    }
                }

            } finally {
                // Ripristina le directory originali (se fossero state modificate)
                System.clearProperty("webservice.temp.output");
            }

            // Elimina il file temporaneo di input
            Files.deleteIfExists(tempFile.toPath());
            logger.info("WebService: File temporaneo di input eliminato: {}", tempFile.getPath());
            Log.addMessage("WebService: File temporaneo di input eliminato: " + tempFile.getPath());

            // Verifica che abbiamo almeno un file di output
            if (outFiles == null || outFiles.isEmpty()) {
                throw new Exception("Il converter non ha prodotto file di output validi");
            }

            // Prende il primo file convertito (o l'unico nel caso di merge)
            File convertedFile = outFiles.get(0);
            logger.info("WebService: File convertito trovato: {}", convertedFile.getAbsolutePath());
            Log.addMessage("WebService: File convertito trovato: " + convertedFile.getAbsolutePath());

            // Rimuove il suffisso dal nome se presente

            // Sposta il file convertito nella directory di output specificata
            Files.move(convertedFile.toPath(), convertedFile.toPath(), StandardCopyOption.REPLACE_EXISTING);
            logger.info("WebService: File convertito spostato in: {}", convertedFile.getAbsolutePath());
            Log.addMessage("WebService: File convertito spostato in: " + convertedFile.getAbsolutePath());

            // Pulizia: elimina eventuali altri file di output
            for (int i = 1; i < outFiles.size(); i++) {
                try {
                    Files.deleteIfExists(outFiles.get(i).toPath());
                } catch (Exception e) {
                    logger.error("WebService: Errore eliminazione file extra: {}", e.getMessage());
                    Log.addMessage("WebService: Errore eliminazione file extra: " + e.getMessage());
                }
            }

            logger.info("WebService: Conversione completata con successo: {} -> {}", srcFile.getName(), convertedFile.getName());
            Log.addMessage("WebService: Conversione completata con successo: " + srcFile.getName() + " -> " + convertedFile.getName());
            return convertedFile;

        } catch (Exception e) {
            logger.error("WebService: Errore durante la conversione del file {}: {}", srcFile.getName(), e.getMessage());
            Log.addMessage("ERRORE WebService: Errore durante la conversione del file " + srcFile.getName() + ": " + e.getMessage());
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
    }

    /**
     * Elimina ricorsivamente una directory e tutto il suo contenuto
     */
    private void deleteDirectoryRecursively(Path directory) throws IOException {
        if (Files.exists(directory)) {
            Files.walk(directory)
                    .sorted(Comparator.reverseOrder())
                    .map(Path::toFile)
                    .forEach(File::delete);
        }
    }

    /**
     * Controllo dell'esistenza dei parametri
     */
    private String checkParameters(String srcExt, String outExt, File srcFile) throws Exception {
        if (srcExt == null) {
            logger.error("WebService: srcExt nullo");
            Log.addMessage("ERRORE WebService: srcExt nullo");
            throw new NullPointerException("L'oggetto srcExt non esiste");
        }
        if (outExt == null) {
            logger.error("WebService: outExt nullo");
            Log.addMessage("ERRORE WebService: outExt nullo");
            throw new NullPointerException("L'oggetto outExt non esiste");
        }
        if (srcFile == null) {
            logger.error("WebService: srcFile nullo");
            Log.addMessage("ERRORE WebService: srcFile nullo");
            throw new NullPointerException("L'oggetto srcFile non esiste");
        }

        Map<String, Map<String, String>> conversions = config.getConversions();
        if (conversions == null || !conversions.containsKey(srcExt)) {
            logger.error("WebService: Conversione da {} non supportata", srcExt);
            Log.addMessage("ERRORE WebService: Conversione da " + srcExt + " non supportata");
            throw new Exception("Conversione non supportata");
        }

        Map<String, String> possibleConversions = conversions.get(srcExt);
        String converterClassName = possibleConversions.get(outExt);
        if (converterClassName == null) {
            logger.error("WebService: Conversione da {} a {} non supportata", srcExt, outExt);
            Log.addMessage("ERRORE WebService: Conversione da " + srcExt + " a " + outExt + " non supportata");
            throw new Exception("Conversione non supportata");
        }

        logger.info("WebService: Parametri validi. Conversione da {} a {} tramite {}", srcExt, outExt, converterClassName);
        Log.addMessage("WebService: Parametri validi. Conversione da " + srcExt + " a " + outExt + " tramite " + converterClassName);
        return converterClassName;
    }


    /**
     * Rinomina il file passato a quello di destinazione
     */
    public static void rinominaFile(File startFile, File destFile) throws Exception {
        if (!startFile.renameTo(destFile)) {
            logger.error("WebService: Rinomina file pre-convert fallita: {} -> {}", startFile.getName(), destFile.getName());
            Log.addMessage("ERRORE WebService: Rinomina file pre-convert fallita: " + startFile.getName() + " -> " + destFile.getName());
            throw new Exception("Rinomina file pre-convert fallita");
        }

        logger.info("WebService: Rinomina file: {} -> {}", startFile.getName(), destFile.getName());
        Log.addMessage("WebService: Rinomina file: " + startFile.getName() + " -> " + destFile.getName());
    }

//    public boolean canBeConverted(String extension, String password) {
//
//    }
}