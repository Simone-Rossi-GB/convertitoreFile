import webService.client.objects.Utility;
import webService.server.converters.Converter;
import webService.server.converters.exception.*;

import java.io.*;
import java.lang.reflect.InvocationTargetException;
import java.nio.file.*;
import java.util.*;

import webService.server.converters.Zipper;
import com.twelvemonkeys.util.convert.ConversionException;
import webService.client.configuration.configHandlers.config.ConfigReader;
import webService.client.configuration.configHandlers.conversionContext.ConversionContextWriter;
import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.LogManager;

public class Engine {
    private static final Logger logger = LogManager.getLogger(Engine.class);

    /**
     * Ritorna i formati in cui un file può essere convertito
     * @param extension Estensione di cui controllare i formati convertibili possibili
     * @return Lista contenente tutti i formati in cui si puo convertire il file
     * @throws NullPointerException Se viene passata una stringa nulla
     * @throws IllegalArgumentException Non viene passata un'estensione
     * @throws UnsupportedConversionException Formato non supportato
     */
    @SuppressWarnings("unused") // Metodo pubblico dell'API, potrebbe essere usato in futuro
    public List<String> getPossibleConversions(String extension) throws IllegalArgumentException, NullPointerException, UnsupportedConversionException {
        return validateAndGetConversions(extension);
    }

    /**
     * Metodo helper per validare e ottenere le conversioni possibili
     * Estratto per evitare duplicazione di codice con EngineWebService
     */
    private List<String> validateAndGetConversions(String extension) throws IllegalArgumentException, NullPointerException, UnsupportedConversionException {
        if (extension == null) {
            logger.error("Parametro extension nullo");
            throw new IllegalArgumentException("L'oggetto extension non esiste");
        }

        if (ConfigReader.getConversions() == null) {
            logger.error("Configurazione mancante");
            throw new NullPointerException("Config mancante");
        }

        if (!ConfigReader.getConversions().containsKey(extension)) {
            logger.error("Conversione non supportata per estensione: {}", extension);
            throw new UnsupportedConversionException("Formato di partenza non supportato: " + extension);
        }

        logger.info("Formati disponibili per la conversione da {} ottenuti con successo", extension);
        return new ArrayList<>(ConfigReader.getConversions().get(extension).keySet());
    }

    /**
     * Esecuzione conversione
     * @param srcExt Estensione file iniziale
     * @param outExt Estensione file finale
     * @param srcFile File iniziale
     * @throws IOException Errore nello spostamento dei file convertiti
     * @throws ConversionException Errore nella conversione o nel caricamento del convertitore
     * @throws FileMoveException Errore nella gestione del file temporaneo
     * @throws UnsupportedConversionException conversione non supportata
     */
    public void conversione(String srcExt, String outExt, File srcFile) throws IOException, ConversionException, FileMoveException, UnsupportedConversionException {
        File outFile = null;

        try {
            // Controlla se deve eseguire una conversione multipla
            if (ConfigReader.getIsMultipleConversionEnabled() && Utility.getExtension(srcFile).equals("zip")) {
                outFile = conversioneMultipla(srcExt, outExt, srcFile);
            } else {
                outFile = conversioneSingola(srcExt, outExt, srcFile);
            }

            // Sposta il file convertito nella directory corretta
            spostaFile(ConfigReader.getSuccessOutputDir(), outFile);
            logger.info("Conversione completata con successo: {} -> {}", srcFile.getName(), outFile.getName());

        } catch (ClassNotFoundException | NoSuchMethodException | InvocationTargetException | IllegalAccessException | InstantiationException e) {
            logger.error("Errore nel caricamento del convertitore per {}: {}", srcFile.getName(), e.getMessage());
            spostaFileInErrorFolder(srcFile);
            throw new ConversionException("Errore nel caricamento del convertitore: " + e.getMessage());

        } catch (IOException e) {
            logger.error("Errore I/O durante la conversione di {}: {}", srcFile.getName(), e.getMessage());
            spostaFileInErrorFolder(srcFile);
            throw new FileMoveException("Errore nella gestione del file temporaneo: " + e.getMessage());

        } catch (FormatsException e) {
            logger.error("Errore di formato durante la conversione di {}: {}", srcFile.getName(), e.getMessage());
            spostaFileInErrorFolder(srcFile);
            throw new UnsupportedConversionException(e.getMessage());
        }
    }

    /**
     * Helper method per spostare file in cartella errori con gestione eccezioni
     */
    private void spostaFileInErrorFolder(File srcFile) {
        try {
            spostaFile(ConfigReader.getErrorOutputDir(), srcFile);
        } catch (IOException | IllegalArgumentException ioError) {
            logger.error("Impossibile spostare il file {} nella cartella errori: {}",
                    srcFile.getName(), ioError.getMessage());
        }
    }

    /**
     * Conversione multipla di file contenuti in uno zip
     * @param srcExt formato di partenza
     * @param outExt formato di output
     * @param srcFile file di partenza
     * @return file convertiti zippati
     * @throws ClassNotFoundException convertitore non trovato
     * @throws NoSuchMethodException metodo di conversione non trovato
     * @throws InvocationTargetException impossibile istanziare il convertitore
     * @throws InstantiationException classe astratta
     * @throws IllegalAccessException costruttore del convertitore non accessibile
     * @throws IOException errore nella gestione del file temp o dello zip
     */
    private File conversioneMultipla(String srcExt, String outExt, File srcFile) throws IOException, ClassNotFoundException, InvocationTargetException, NoSuchMethodException, InstantiationException, IllegalAccessException {
        ArrayList<File> zippedFiles = Zipper.unzip(srcFile);
        ArrayList<File> convertedFiles = new ArrayList<>();

        for (File f : zippedFiles) {
            convertedFiles.add(conversioneSingola(srcExt, outExt, f));
        }

        // Zippa i file convertiti
        try {
            return Zipper.compressioneFile(convertedFiles, Utility.getBaseName(srcFile));
        } catch (FileMoveException e) {
            logger.error("Impossibile comprimere i file convertiti per {}: {}", srcFile.getName(), e.getMessage());
            throw new IOException("Errore durante la compressione: " + e.getMessage(), e);
        }
    }

    /**
     * Effettua la conversione del singolo file
     * @param srcExt formato di partenza
     * @param outExt formato di output
     * @param srcFile file di partenza
     * @return file convertito
     * @throws ClassNotFoundException convertitore non trovato
     * @throws NoSuchMethodException metodo di conversione non trovato
     * @throws InvocationTargetException impossibile istanziare il convertitore
     * @throws InstantiationException classe astratta
     * @throws IllegalAccessException costruttore del convertitore non accessibile
     * @throws IOException errore nella gestione del file temp
     */
    private File conversioneSingola(String srcExt, String outExt, File srcFile) throws ClassNotFoundException, NoSuchMethodException, InvocationTargetException, InstantiationException, IllegalAccessException, IOException {
        // Istanzia il convertitore adatto
        String converterClassName = checkParameters(srcExt, outExt, srcFile);
        Class<?> clazz = Class.forName(converterClassName);
        Converter converter = (Converter) clazz.getDeclaredConstructor().newInstance();

        File outFile;

        // Crea un file temp per la conversione
        File tempFile = new File("src/temp", srcFile.getName());
        Files.copy(srcFile.toPath(), tempFile.toPath(), StandardCopyOption.REPLACE_EXISTING);

        // Elimina il file temp al termine dell'applicazione
        tempFile.deleteOnExit();

        ConversionContextWriter.setDestinationFormat(outExt);

        try {
            outFile = converter.conversione(tempFile);
            logger.info("Conversione singola completata: {}", outFile.getName());
        } catch (Exception e) { // Exception ammessa nel programma
            logger.error("Errore durante la conversione di {}: {}", srcFile.getName(), e.getMessage());
            throw new ConversionException(e.getMessage());
        }

        // Pulizia sicura del file temporaneo
        try {
            Files.deleteIfExists(tempFile.toPath());
        } catch (IOException e) {
            logger.warn("Impossibile eliminare il file temporaneo {}: {} (non critico)",
                    tempFile.getName(), e.getMessage());
        }

        return outFile;
    }

    /**
     * Controllo dell'esistenza dei parametri
     * @param srcExt Estensione file iniziale
     * @param outExt Estensione file finale
     * @param srcFile File iniziale
     * @throws IllegalArgumentException Parametri null
     * @throws UnsupportedConversionException conversione non supportata
     */
    private String checkParameters(String srcExt, String outExt, File srcFile) throws IllegalArgumentException, UnsupportedConversionException {
        if (srcExt == null) {
            logger.error("L'oggetto srcExt non esiste");
            throw new IllegalArgumentException("L'oggetto srcExt non esiste");
        }
        if (outExt == null) {
            logger.error("outExt nullo");
            throw new IllegalArgumentException("L'oggetto outExt non esiste");
        }
        if (srcFile == null) {
            logger.error("srcFile nullo");
            throw new IllegalArgumentException("L'oggetto srcFile non esiste");
        }

        Map<String, Map<String, String>> conversions = ConfigReader.getConversions();
        if (conversions == null || !conversions.containsKey(srcExt)) {
            logger.error("Conversione da {} non supportata", srcExt);
            throw new UnsupportedConversionException(srcExt + " non supportato per la conversione");
        }

        Map<String, String> possibleConversions = conversions.get(srcExt);
        String converterClassName = possibleConversions.get(outExt);
        if (converterClassName == null) {
            logger.error("Conversione da {} a {} non supportata", srcExt, outExt);
            throw new UnsupportedConversionException("Impossibile convertire un file " + srcExt + " in uno " + outExt);
        }

        logger.info("Parametri validi. Conversione da {} a {} tramite {}", srcExt, outExt, converterClassName);
        return converterClassName;
    }

    /**
     * Sposta il file ricevuto nella directory indicata
     * @param outPath Percorso di destinazione
     * @param file File da spostare
     * @throws IOException Errore sull'istruzione Files.move()
     * @throws IllegalArgumentException Parametri null
     */
    private void spostaFile(String outPath, File file) throws IOException, IllegalArgumentException {
        if (file == null) {
            throw new IllegalArgumentException("L'oggetto file non esiste");
        }
        if (outPath == null) {
            throw new IllegalArgumentException("L'oggetto outPath non esiste");
        }

        Path dest = Paths.get(outPath, file.getName());
        Files.move(file.toPath(), dest, StandardCopyOption.REPLACE_EXISTING);
        logger.info("File spostato in: {}", dest);
    }
}