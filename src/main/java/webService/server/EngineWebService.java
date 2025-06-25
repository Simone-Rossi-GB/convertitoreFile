package webService.server;

import com.twelvemonkeys.util.convert.ConversionException;
import converters.exception.IllegalExtensionException;
import webService.Utility;
import webService.configuration.configHandlers.conversionContext.ConversionContextWriter;
import converters.Converter;
import webService.configuration.configHandlers.config.ConfigReader;
import objects.Log;
import java.io.*;
import java.lang.reflect.InvocationTargetException;
import java.nio.file.*;
import java.util.*;
import converters.Zipper;

import converters.exception.FileMoveException;
import converters.exception.FormatsException;
import converters.exception.UnsupportedConversionException;
import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.LogManager;

/**
 * Engine per il WebService - versione modificata dell'Engine locale
 * Gestisce le conversioni senza spostare automaticamente i file
 */
public class EngineWebService {
    private static final Logger logger = LogManager.getLogger(EngineWebService.class);


    /**
     * Ritorna i formati in cui un file può essere convertito
     * @param extension Estensione di cui controllare i formati convertibili possibili
     * @return Lista contenente tutti i formati in cui si puo convertire il file
     * @throws NullPointerException Se viene passata una stringa nulla o non esiste il config
     * @throws IllegalArgumentException Non viene passata un'estensione
     */
    public List<String> getPossibleConversions(String extension) throws IllegalArgumentException, NullPointerException, UnsupportedConversionException {
        if (extension == null) {
            logger.error("Parametro extension nullo");
            throw new IllegalArgumentException("L'oggetto extension non esiste");
        }
        if (ConfigReader.getConversions() == null) {
            logger.error("Configurazione mancante");
            throw new NullPointerException("Config mancante");
        }
        if(!ConfigReader.getConversions().containsKey(extension)) {
            logger.error("Conversione non supportata");
            throw new UnsupportedConversionException("Formato di partenza non supportato");
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
    public File conversione(String srcExt, String outExt, File srcFile) throws IOException, ConversionException, FileMoveException,UnsupportedConversionException  {
        File outFile;
        try {
            //Controlla se deve eseguire una conversione multipla
            if(ConfigReader.getIsMultipleConversionEnabled() && Utility.getExtension(srcFile).equals("zip"))
                outFile = conversioneMultipla(Zipper.extractFileExstension(srcFile), outExt, srcFile);
            else
                outFile = conversioneSingola(srcExt, outExt, srcFile);
            return outFile;
        } catch (ClassNotFoundException | NoSuchMethodException | InvocationTargetException | IllegalAccessException | InstantiationException e) {
            throw new ConversionException("Errore nel caricamento del convertitore");
        }catch (IOException e){
            throw new FileMoveException("Errore nella gestione del file temporaneo");
        } catch (FormatsException | IllegalExtensionException e){
            throw new UnsupportedConversionException(e.getMessage());
        }
    }

    /**
     *
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
    private File conversioneMultipla(String srcExt, String outExt, File srcFile) throws IOException, ClassNotFoundException, InvocationTargetException, NoSuchMethodException, InstantiationException, IllegalAccessException, FileMoveException, IllegalExtensionException {
        ArrayList<File> zippedFiles = Zipper.unzip(srcFile);
        ArrayList<File> convertedFiles = new ArrayList<>();
        for (File f : zippedFiles){
            convertedFiles.add(conversioneSingola(srcExt, outExt, f));
        }
        //zippo i file convertiti
        return Zipper.compressioneFile(convertedFiles, Utility.getBaseName(srcFile));
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
        //Istanzia il convertitore adatto
        String converterClassName = checkParameters(srcExt, outExt, srcFile); // ritorna il nome del convertitore
        Class<?> clazz = Class.forName(converterClassName); // tramite il nome trova la classe

        //tramite l'interfaccia istanziamo il convertitore adatto ottenendo l'istanza dal costruttore
        Converter converter = (Converter) clazz.getDeclaredConstructor().newInstance();

        File outFile;

        //Crea un file temp per la conversione
        File tempFile = new File("src/temp", srcFile.getName());
        System.out.println("Oggetto temp creato");

        //copiamo il file originale nella destinazione del file temporaneo sostituendolo
        Files.copy(srcFile.toPath(), tempFile.toPath(), StandardCopyOption.REPLACE_EXISTING);

        //Elimina il file temp al termine del'applicazione
        tempFile.deleteOnExit();

        //impostiamo il formato di conversione finale
        ConversionContextWriter.setDestinationFormat(outExt);

        try {
            // convertiamo il file
            outFile = converter.conversione(tempFile);
        } catch (Exception e) {
            throw new ConversionException(e.getMessage());
        }

        //eliminiamo preventivamente il file temporaneo anche se viene eliminato
        // alla chiusura della JVM
        Files.deleteIfExists(tempFile.toPath());

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
            Log.addMessage("ERRORE: srcExt nullo");
            throw new IllegalArgumentException("L'oggetto srcExt non esiste");
        }
        if (outExt == null) {
            Log.addMessage("ERRORE: outExt nullo");
            throw new IllegalArgumentException("L'oggetto outExt non esiste");
        }
        if (srcFile == null) {
            Log.addMessage("ERRORE: srcFile nullo");
            throw new IllegalArgumentException("L'oggetto srcFile non esiste");
        }

        // leggiamo dal config file le configurazoni ottenendo una mappa dove
        // formato originale --> formato finale, nome convertitore
        Map<String, Map<String, String>> conversions = ConfigReader.getConversions();

        if (conversions == null || !conversions.containsKey(srcExt)) {
            Log.addMessage("ERRORE: Conversione da " + srcExt + " non supportata");
            throw new UnsupportedConversionException(srcExt + " non supportato per la conversione");
        }

        //otteniamo una mappa formato finale, nome convertitore tramite chiave = formato originale
        Map<String, String> possibleConversions = conversions.get(srcExt);

        // otteniamo il nome della classe del convertitore per la conversione
        // chiave = formato di destinazione - ritorno funzione: converterClassName
        String converterClassName = possibleConversions.get(outExt);

        if (converterClassName == null) { // se non esiste un convertitore lanciamo un eccezione
            Log.addMessage("ERRORE: Conversione da " + srcExt + " a " + outExt + " non supportata");
            throw new UnsupportedConversionException("Impossibile convertire un file " + srcExt + " in uno " + outExt);
        }

        Log.addMessage("Parametri validi. Conversione da " + srcExt + " a " + outExt + " tramite " + converterClassName);

        // ritorniamo il nome della classe del convertitore per la conversione
        return converterClassName;
    }
}