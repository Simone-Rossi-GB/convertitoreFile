package converter;

import Converters.Converter;
import com.google.gson.Gson;
import java.io.*;
import java.nio.file.*;
import java.util.*;
import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.LogManager;


public class Engine {
    private ConverterConfig config;
    private static final String CONFIG_FILE_PATH = System.getProperty("user.dir") + "/src/main/java/converter/config/config.json";

    /**
     * Costruttore: carica il file config.json
     */
    public Engine() {
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
                Log.addMessage("ERRORE: l'oggetto config non esiste");
                throw new NullPointerException("L'oggetto config non esiste");
            }
            Log.addMessage("Configurazione caricata correttamente da config.json");
        } catch (Exception e) {
            Log.addMessage("ERRORE: Lettura del file di configurazione fallita");
            throw new RuntimeException("Errore nella lettura del file di configurazione", e);
        }
    }

    /**
     * Ritorna la stringa che rappresenta il contenuto del json
     * @return contenuto del json di ritorno
     * @throws Exception Nel caso di errore nella lettura del file di configurazione
     */
    public String getConfigAsJson() throws Exception {
        try {
            Log.addMessage("Lettura del contenuto di config.json eseguita con successo");
            return new String(Files.readAllBytes(Paths.get(CONFIG_FILE_PATH)));
        } catch (IOException e) {
            Log.addMessage("ERRORE: Lettura del file di configurazione non riuscita");
            throw new Exception("Errore nella lettura del file di configurazione: " + e.getMessage(), e);
        }
    }

    /**
     * @param jsonText testo da aggiungere a config.json
     * @throws Exception Errore scrittura sul file di configurazione
     */
    public void setConfigFromJson(String jsonText) throws Exception {
        // Prima valida che il JSON sia corretto
        try {
            Gson gson = new Gson();
            ConverterConfig testConfig = gson.fromJson(jsonText, ConverterConfig.class);
            if (testConfig == null) {
                throw new Exception("JSON non valido: impossibile parsare la configurazione");
            }
        } catch (Exception e) {
            Log.addMessage("ERRORE: JSON non valido");
            throw new Exception("Il JSON fornito non è valido: " + e.getMessage(), e);
        }

        // Scrivi il file
        try (FileWriter writer = new FileWriter(CONFIG_FILE_PATH)) {
            writer.write(jsonText);
            writer.flush(); // Assicurati che i dati siano scritti
            Log.addMessage("Scrittura su config.json completata");
        } catch (IOException e) {
            Log.addMessage("ERRORE: Scrittura su config.json fallita");
            throw new Exception("Errore nella scrittura del file di configurazione: " + e.getMessage(), e);
        }

        // Ricarica la configurazione
        try {
            setConfig();
            Log.addMessage("Configurazione ricaricata con successo");
        } catch (Exception e) {
            Log.addMessage("ERRORE: Caricamento della nuova configurazione fallito");
            throw new Exception("Errore nel caricamento della nuova configurazione: " + e.getMessage(), e);
        }
    }


    /**
     * Ritorna i formati in cui un file può essere convertito
     * @param extension Estensione di cui controllare i formati convertibili possibili
     * @return Lista contenente tutti i formati in cui si puo convertire il file
     * @throws NullPointerException Se viene passata una stringa nulla
     * @throws Exception Il file config non esiste oppure la conversione non e supportata
     */
    public List<String> getPossibleConversions(String extension) throws Exception {
        if (extension == null) {
            Log.addMessage("ERRORE: Parametro extension nullo");
            throw new NullPointerException("L'oggetto extension non esiste");
        }

        if (config == null || config.getConversions() == null || !config.getConversions().containsKey(extension)) {
            Log.addMessage("ERRORE: Configurazione mancante o conversione non supportata per: " + extension);
            throw new Exception("Config assente o conversione non supportata");
        }

        Log.addMessage("Formati disponibili per la conversione da " + extension + " ottenuti con successo");
        return new ArrayList<>(config.getConversions().get(extension).keySet());
    }

    /**
     * Conversione base
     * @param srcExt Estensione file iniziale
     * @param outExt Estensione file finale
     * @param srcFile File iniziale
     * @throws Exception Errore nella rinomina del file
     */
    public void conversione(String srcExt, String outExt, File srcFile) throws Exception {
        executeConversion(srcExt, outExt, srcFile, null, null);
    }

    /**
     * Conversione PDF protetto
     * @param srcExt Estensione file iniziale
     * @param outExt Estensione file finale
     * @param srcFile File iniziale
     * @param extraParam parametro Extra
     * @throws Exception Errore nella rinomina del file
     */
    public void conversione(String srcExt, String outExt, File srcFile, String extraParam) throws Exception {
        executeConversion(srcExt, outExt, srcFile, extraParam, null);
    }

    /**
     * Conversione PDF -> JPG unendo le pagine in un'unica immagine
     * @param srcExt Estensione file iniziale
     * @param outExt Estensione file finale
     * @param srcFile File iniziale
     * @param union Flag che indica l'unione o meno delle immagini estratte dal PDF
     * @throws Exception Errore nella rinomina del file
     */
    public void conversione(String srcExt, String outExt, File srcFile, boolean union) throws Exception {
        executeConversion(srcExt, outExt, srcFile, null, union);
    }

    /**Conversione PDF protetto -> JPG unendo le pagine in un'unica immagine
     *
     * @param srcExt Estensione file iniziale
     * @param outExt Estensione file finale
     * @param srcFile File iniziale
     * @param password Password per file criptati
     * @param union Flag che indica l'unione o meno delle immagini estratte dal PDF
     * @throws Exception Errore nella rinomina del file
     */
    public void conversione(String srcExt, String outExt, File srcFile, String password, boolean union) throws Exception {
        executeConversion(srcExt, outExt, srcFile, password, union);
    }

    /**
     * Esecuzione conversione
     * @param srcExt Estensione file iniziale
     * @param outExt Estensione file finale
     * @param srcFile File iniziale
     * @param parameter Parametro extra
     * @param union Flag che indica l'unione o meno delle immagini estratte dal PDF
     * @throws Exception Errore nella rinomina del file
     */
    private void executeConversion(String srcExt, String outExt, File srcFile, String parameter, Boolean union) throws Exception {
        String converterClassName = checkParameters(srcExt, outExt, srcFile);
        Class<?> clazz = Class.forName(converterClassName);
        Converter converter = (Converter) clazz.getDeclaredConstructor().newInstance();
        List<File> outFiles;
        File tempFile = new File("src/temp/" + srcFile.getName());
        Files.copy(srcFile.toPath(), tempFile.toPath(), StandardCopyOption.REPLACE_EXISTING);

        try {
            if (parameter != null && union != null) {
                outFiles = converter.convert(srcFile, parameter, union);
            } else if (parameter != null) {
                outFiles = converter.convert(srcFile, parameter);
            } else if (union != null) {
                outFiles = converter.convert(tempFile, union);
                System.out.println("dio porco");
            } else {
                outFiles = converter.convert(tempFile);
            }

            Files.deleteIfExists(tempFile.toPath());
            Log.addMessage("File temporaneo eliminato: " + srcFile.getPath());

            for (File f : outFiles) {
                //Sposto il file convertito nella directory corretta
                spostaFile(config.getSuccessOutputDir(), f);
            }
            Log.addMessage("Conversione completata con successo: " + srcFile.getName() + " -> " + outExt);

        } catch (IOException e) {
            Log.addMessage("ERRORE: Errore durante la conversione o lo spostamento del file " + srcFile.getName());
            spostaFile(config.getErrorOutputDir(), srcFile);
            throw new Exception(e.getMessage());
        }
    }

    /**
     * Controllo dell'esistenza dei parametri
     * @param srcExt Estensione file iniziale
     * @param outExt Estensione file finale
     * @param srcFile File iniziale
     * @throws NullPointerException Ritorna il primo parametro inesistente trovato
     */
    private String checkParameters(String srcExt, String outExt, File srcFile) throws Exception {
        if (srcExt == null) {
            Log.addMessage("ERRORE: srcExt nullo");
            throw new NullPointerException("L'oggetto srcExt non esiste");
        }
        if (outExt == null) {
            Log.addMessage("ERRORE: outExt nullo");
            throw new NullPointerException("L'oggetto outExt non esiste");
        }
        if (srcFile == null) {
            Log.addMessage("ERRORE: srcFile nullo");
            throw new NullPointerException("L'oggetto srcFile non esiste");
        }

        Map<String, Map<String, String>> conversions = config.getConversions();
        if (conversions == null || !conversions.containsKey(srcExt)) {
            Log.addMessage("ERRORE: Conversione da " + srcExt + " non supportata");
            throw new Exception("Conversione non supportata");
        }

        Map<String, String> possibleConversions = conversions.get(srcExt);
        String converterClassName = possibleConversions.get(outExt);
        if (converterClassName == null) {
            Log.addMessage("ERRORE: Conversione da " + srcExt + " a " + outExt + " non supportata");
            throw new Exception("Conversione non supportata");
        }

        Log.addMessage("Parametri validi. Conversione da " + srcExt + " a " + outExt + " tramite " + converterClassName);
        return converterClassName;
    }

    /**
     * Sposta il file ricevuto nella directory indicata
     * @param outPath Percorso di destinazione
     * @param file File da spostare
     * @throws IOException Errore sull'istruzione Files.move()
     */
    private void spostaFile(String outPath, File file) throws IOException {
        if (file == null) throw new NullPointerException("L'oggetto file non esiste");
        if (outPath == null) throw new NullPointerException("L'oggetto outPath non esiste");
        Path dest = Paths.get(outPath, file.getName());
        Files.move(file.toPath(), dest, StandardCopyOption.REPLACE_EXISTING);
        Log.addMessage("File spostato in: " + dest);
    }


    /**
     * Ritorna la configurazione ottenuta da config.json
     * @return Configurazione estratta dal file json
     * @throws NullPointerException Variabile config nulla
     */
    public ConverterConfig getConverterConfig() throws NullPointerException{
        if (config == null) {
            throw new NullPointerException("L'oggetto config non esiste");
        }
        return config;
    }
}
