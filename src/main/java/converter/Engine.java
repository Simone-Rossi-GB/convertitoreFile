package converter;

import Converters.Converter;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class Engine {
    private ConverterConfig config;
    private static final String CONFIG_FILE_PATH = "src/main/java/converter/config/config.json";

    /**
     * Costruttore: legge la configurazione dal file JSON.
     * @throws RuntimeException se il file di configurazione non è leggibile o è corrotto.
     */
    public Engine() {
        setConfig();
    }

    /**
     * Legge la configurazione dal file JSON.
     * @throws RuntimeException se il file di configurazione non è leggibile o è corrotto.
     */
    public void setConfig(){
        try (FileReader reader = new FileReader(CONFIG_FILE_PATH)) {
            Gson gson = new Gson();
            config = gson.fromJson(reader, ConverterConfig.class);
        } catch (Exception e) {
            throw new RuntimeException("Errore nella lettura del file di configurazione", e);
        }
    }

    /**
     * Restituisce il contenuto del file di configurazione JSON come stringa.
     * @return contenuto del file config.json come stringa
     * @throws Exception se si verifica un errore durante la lettura del file
     */
    public String getConfigAsJson() throws Exception {
        try {
            return new String(Files.readAllBytes(Paths.get(CONFIG_FILE_PATH)));
        } catch (IOException e) {
            throw new Exception("Errore nella lettura del file di configurazione: " + e.getMessage(), e);
        }
    }

    /**
     * Scrive il testo JSON nel file di configurazione e ricarica la configurazione.
     * @param jsonText testo JSON da scrivere nel file config.json
     * @throws Exception se si verifica un errore durante la scrittura o il caricamento
     */
    public void setConfigFromJson(String jsonText) throws Exception {
        try {
            // Scrive il testo nel file config.json
            try (FileWriter writer = new FileWriter(CONFIG_FILE_PATH)) {
                writer.write(jsonText);
            }

            // Ricarica la configurazione usando il metodo esistente
            setConfig();

        } catch (IOException e) {
            throw new Exception("Errore nella scrittura del file di configurazione: " + e.getMessage(), e);
        } catch (RuntimeException e) {
            throw new Exception("Errore nel caricamento della nuova configurazione: " + e.getMessage(), e);
        }
    }

    /**
     * Restituisce la lista dei formati di conversione possibili per una data estensione.
     * @param extension estensione sorgente del file
     * @return lista di estensioni di output possibili
     * @throws Exception se la conversione non è supportata
     */
    public List<String> getPossibleConversions(String extension) throws Exception {
        if (!config.getConversions().containsKey(extension))
            throw new Exception("Conversione non supportata");

        List<String> possibleExtensions = new ArrayList<>(config.getConversions().get(extension).keySet());

        // Stampa a console per debug
        for (String e : possibleExtensions)
            System.out.println(e);

        System.out.println("Lista ottenuta\n");
        return possibleExtensions;
    }

    /**
     * Esegue la conversione del file da srcExt a outExt usando la classe converter appropriata.
     * @param srcExt estensione sorgente
     * @param outExt estensione di destinazione
     * @param srcFile file sorgente da convertire
     * @throws Exception se la conversione non è supportata o fallisce
     */
    public void conversione(String srcExt, String outExt, File srcFile) throws Exception {
        System.out.println("entro in conversione");
        Map<String, Map<String, String>> conversions = config.getConversions();

        if (!conversions.containsKey(srcExt))
            throw new Exception("Conversione non supportata");

        Map<String, String> possibleConversions = conversions.get(srcExt);

        if (possibleConversions.containsKey(outExt)) {
            String converterClassName = possibleConversions.get(outExt);
            System.out.println(srcExt + " -> " + outExt + " tramite " + converterClassName);

            try {
                // Carica dinamicamente la classe converter
                Class<?> clazz = Class.forName(converterClassName);
                Converter converter = (Converter) clazz.getDeclaredConstructor().newInstance();

                try {
                    // Copia il file sorgente in una cartella temporanea
                    Path tempFilePath = Paths.get("src" + File.separator + "temp" + File.separator + srcFile.getName());
                    Files.copy(srcFile.toPath(), tempFilePath, StandardCopyOption.REPLACE_EXISTING);
                    srcFile = tempFilePath.toFile();

                    // Rinomina il file temporaneo con un suffisso per evitare conflitti
                    File newFile = giveBackNewFileWithNewName(srcFile.getPath(), ("-[[" + outExt + "]]-"));
                    if (!srcFile.renameTo(newFile)) {
                        System.err.println("Errore: File già convertito al formato richiesto");
                    }
                    srcFile = newFile;

                    // Esegue la conversione vera e propria
                    List<File> outFiles = converter.convert(srcFile);

                    // Elimina il file temporaneo originale
                    Files.delete(srcFile.toPath());

                    // Per ogni file convertito, rimuove il suffisso e lo sposta nella cartella di successo
                    for (File f : outFiles) {
                        File returnFile = new File(f.getPath().replaceAll("-\\[\\[.*?]]-", ""));
                        if (!f.renameTo(returnFile)) {
                            System.err.println("Errore: File già convertito al formato richiesto");
                        }
                        f = returnFile;
                        spostaFile(config.getSuccessOutputDir(), f);
                    }
                } catch (IOException e) {
                    // Se c'è un errore durante la conversione, sposta il file nella cartella errori
                    spostaFile(config.getErrorOutputDir(), srcFile);
                    throw new Exception(e.getMessage());
                }
            } catch (Exception e) {
                System.out.println(e.getMessage());
                throw e; // rilancio eccezione per segnalarla in alto
            }
        } else {
            throw new Exception("Conversione non supportata");
        }
    }

    public void conversione(String srcExt, String outExt, File srcFile, String password) throws Exception {
        Map<String, Map<String, String>> conversions = config.getConversions();
        System.out.println("entro in conversione password");
        if (!conversions.containsKey(srcExt))
            throw new Exception("Conversione non supportata");

        Map<String, String> possibleConversions = conversions.get(srcExt);

        if (possibleConversions.containsKey(outExt)) {
            String converterClassName = possibleConversions.get(outExt);
            System.out.println(srcExt + " -> " + outExt + " tramite " + converterClassName);

            try {
                // Carica dinamicamente la classe converter
                Class<?> clazz = Class.forName(converterClassName);
                Converter converter = (Converter) clazz.getDeclaredConstructor().newInstance();

                try {
                    // Esegue la conversione vera e propria
                    List<File> outFiles = converter.convert(srcFile, password);

                    // Elimina il file originale
                    Files.delete(srcFile.toPath());

                    // Per ogni file convertito, rimuove il suffisso e lo sposta nella cartella di successo
                    for (File f : outFiles) {
                        spostaFile(config.getSuccessOutputDir(), f);
                    }
                } catch (IOException e) {
                    // Se c'è un errore durante la conversione, sposta il file nella cartella errori
                    spostaFile(config.getErrorOutputDir(), srcFile);
                    throw new Exception(e.getMessage());
                }
            } catch (Exception e) {
                System.out.println(e.getMessage());
                throw e; // rilancio eccezione per segnalarla in alto
            }
        } else {
            throw new Exception("Conversione non supportata");
        }
    }
    public void conversione(String srcExt, String outExt, File srcFile, boolean union) throws Exception {
        System.out.println("entro in conversione");
        Map<String, Map<String, String>> conversions = config.getConversions();

        if (!conversions.containsKey(srcExt))
            throw new Exception("Conversione non supportata");

        Map<String, String> possibleConversions = conversions.get(srcExt);

        if (possibleConversions.containsKey(outExt)) {
            String converterClassName = possibleConversions.get(outExt);
            System.out.println(srcExt + " -> " + outExt + " tramite " + converterClassName);

            try {
                // Carica dinamicamente la classe converter
                Class<?> clazz = Class.forName(converterClassName);
                Converter converter = (Converter) clazz.getDeclaredConstructor().newInstance();

                try {
                    // Copia il file sorgente in una cartella temporanea
                    Path tempFilePath = Paths.get("src" + File.separator + "temp" + File.separator + srcFile.getName());
                    Files.copy(srcFile.toPath(), tempFilePath, StandardCopyOption.REPLACE_EXISTING);
                    srcFile = tempFilePath.toFile();

                    // Rinomina il file temporaneo con un suffisso per evitare conflitti
                    File newFile = giveBackNewFileWithNewName(srcFile.getPath(), ("-[[" + outExt + "]]-"));
                    if (!srcFile.renameTo(newFile)) {
                        System.err.println("Errore: File già convertito al formato richiesto");
                    }
                    srcFile = newFile;

                    // Esegue la conversione vera e propria
                    List<File> outFiles = converter.convert(srcFile, union);

                    // Elimina il file temporaneo originale
                    Files.delete(srcFile.toPath());

                    // Per ogni file convertito, rimuove il suffisso e lo sposta nella cartella di successo
                    for (File f : outFiles) {
                        File returnFile = new File(f.getPath().replaceAll("-\\[\\[.*?]]-", ""));
                        if (!f.renameTo(returnFile)) {
                            System.err.println("Errore: File già convertito al formato richiesto");
                        }
                        f = returnFile;
                        spostaFile(config.getSuccessOutputDir(), f);
                    }
                } catch (IOException e) {
                    // Se c'è un errore durante la conversione, sposta il file nella cartella errori
                    spostaFile(config.getErrorOutputDir(), srcFile);
                    throw new Exception(e.getMessage());
                }
            } catch (Exception e) {
                System.out.println(e.getMessage());
                throw e; // rilancio eccezione per segnalarla in alto
            }
        } else {
            throw new Exception("Conversione non supportata");
        }
    }

    public void conversione(String srcExt, String outExt, File srcFile, String password, boolean union) throws Exception {
        System.out.println("entro in conversione");
        Map<String, Map<String, String>> conversions = config.getConversions();

        if (!conversions.containsKey(srcExt))
            throw new Exception("Conversione non supportata");

        Map<String, String> possibleConversions = conversions.get(srcExt);

        if (possibleConversions.containsKey(outExt)) {
            String converterClassName = possibleConversions.get(outExt);
            System.out.println(srcExt + " -> " + outExt + " tramite " + converterClassName);

            try {
                // Carica dinamicamente la classe converter
                Class<?> clazz = Class.forName(converterClassName);
                Converter converter = (Converter) clazz.getDeclaredConstructor().newInstance();
                System.out.println("classe istanziata");
                try {
                    // Copia il file sorgente in una cartella temporanea
                    Path tempFilePath = Paths.get("src" + File.separator + "temp" + File.separator + srcFile.getName());
                    Files.copy(srcFile.toPath(), tempFilePath, StandardCopyOption.REPLACE_EXISTING);
                    srcFile = tempFilePath.toFile();

                    // Rinomina il file temporaneo con un suffisso per evitare conflitti
                    File newFile = giveBackNewFileWithNewName(srcFile.getPath(), ("-[[" + outExt + "]]-"));
                    if (!srcFile.renameTo(newFile)) {
                        System.err.println("Errore: File già convertito al formato richiesto");
                    }
                    srcFile = newFile;

                    // Esegue la conversione vera e propria
                    List<File> outFiles = converter.convert(srcFile, password, union);

                    // Elimina il file temporaneo originale
                    Files.delete(srcFile.toPath());

                    // Per ogni file convertito, rimuove il suffisso e lo sposta nella cartella di successo
                    for (File f : outFiles) {
                        File returnFile = new File(f.getPath().replaceAll("-\\[\\[.*?]]-", ""));
                        if (!f.renameTo(returnFile)) {
                            System.err.println("Errore: File già convertito al formato richiesto");
                        }
                        f = returnFile;
                        spostaFile(config.getSuccessOutputDir(), f);
                    }
                } catch (IOException e) {
                    // Se c'è un errore durante la conversione, sposta il file nella cartella errori
                    spostaFile(config.getErrorOutputDir(), srcFile);
                    throw new Exception(e.getMessage());
                }
            } catch (Exception e) {
                System.out.println(e.getMessage());
                throw e; // rilancio eccezione per segnalarla in alto
            }
        } else {
            throw new Exception("Conversione non supportata");
        }
    }

    /**
     * Sposta il file specificato nella cartella di destinazione.
     * @param outPath percorso della cartella di destinazione
     * @param file file da spostare
     * @throws IOException in caso di errore durante lo spostamento
     */
    private void spostaFile(String outPath, File file) throws IOException {
        String fileName = file.getName();
        Path srcPath = Paths.get(file.getAbsolutePath());
        Path destPath = Paths.get(outPath + File.separator + fileName);
        Files.move(srcPath, destPath, StandardCopyOption.REPLACE_EXISTING);
        System.out.println("File copiato in " + destPath);
    }

    /**
     * Restituisce un nuovo file con un nome modificato aggiungendo un suffisso prima dell'estensione.
     * @param filePath percorso originale del file
     * @param suffix suffisso da aggiungere al nome del file
     * @return nuovo file con nome modificato
     */
    private static File giveBackNewFileWithNewName(String filePath, String suffix) {
        File file = new File(filePath);
        String name = file.getName();

        int lastDot = name.lastIndexOf(".");

        // Se non c'è estensione, aggiunge solo il suffisso
        if (lastDot == -1) {
            return new File(file.getParent() + File.separator + name + suffix);
        }

        String baseName = name.substring(0, lastDot);
        String extension = name.substring(lastDot); // include il punto

        return new File(file.getParent() + File.separator + baseName + suffix + extension);
    }

    /**
     * Restituisce la configurazione corrente.
     * @return oggetto ConverterConfig
     */
    public ConverterConfig getConverterConfig() {
        return config;
    }
}