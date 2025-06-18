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
import java.util.UUID;

public class Engine {
    private ConverterConfig config;
    // Per un'applicazione Spring Boot, è preferibile non usare percorsi relativi all'applicazione come "src/main/java/converter/config/config.json"
    // Bensì Paths.get("config", "config.json") o caricarlo dal classpath.
    // Per ora lo lascio così per compatibilità con il tuo codice, ma è un punto da migliorare.
    private static final String CONFIG_FILE_PATH = "src/main/java/converter/config/config.json";

    public Engine() {
        setConfig();
    }

    public void setConfig() {
        try (FileReader reader = new FileReader(CONFIG_FILE_PATH)) {
            Gson gson = new Gson();
            config = gson.fromJson(reader, ConverterConfig.class);
        } catch (Exception e) {
            throw new RuntimeException("Errore nella lettura del file di configurazione", e);
        }
    }

    public String getConfigAsJson() throws Exception {
        try {
            return new String(Files.readAllBytes(Paths.get(CONFIG_FILE_PATH)));
        } catch (IOException e) {
            throw new Exception("Errore nella lettura del file di configurazione: " + e.getMessage(), e);
        }
    }

    public void setConfigFromJson(String jsonText) throws Exception {
        try {
            try (FileWriter writer = new FileWriter(CONFIG_FILE_PATH)) {
                writer.write(jsonText);
            }
            setConfig();
        } catch (IOException e) {
            throw new Exception("Errore nella scrittura del file di configurazione: " + e.getMessage(), e);
        } catch (RuntimeException e) {
            throw new Exception("Errore nel caricamento della nuova configurazione: " + e.getMessage(), e);
        }
    }

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

    // --- NUOVI METODI `conversione` CHE RESTITUISCONO IL FILE E PRENDONO DIRECTORY DI OUTPUT ---

    /**
     * Esegue la conversione del file da srcExt a outExt usando la classe converter appropriata.
     * Il file convertito viene salvato in outputDirectory e restituito.
     * @param srcExt estensione sorgente
     * @param outExt estensione di destinazione
     * @param srcFile file sorgente da convertire (già temporaneo e accessibile)
     * @param outputDirectory directory dove salvare il file convertito
     * @return il file convertito
     * @throws Exception se la conversione non è supportata o fallisce
     */
    public File conversione(String srcExt, String outExt, File srcFile, File outputDirectory) throws Exception {
        System.out.println("entro in conversione (web service)");
        Map<String, Map<String, String>> conversions = config.getConversions();

        if (!conversions.containsKey(srcExt))
            throw new Exception("Conversione non supportata per estensione sorgente: " + srcExt);

        Map<String, String> possibleConversions = conversions.get(srcExt);

        if (possibleConversions.containsKey(outExt)) {
            String converterClassName = possibleConversions.get(outExt);
            System.out.println(srcExt + " -> " + outExt + " tramite " + converterClassName);

            File convertedFile = null; // Il file convertito che verrà restituito
            try {
                // Carica dinamicamente la classe converter
                Class<?> clazz = Class.forName(converterClassName);
                Converter converter = (Converter) clazz.getDeclaredConstructor().newInstance();

                // Assicurati che la directory di output esista
                if (!outputDirectory.exists()) {
                    outputDirectory.mkdirs();
                }

                // Genera un nome unico per il file di output per evitare conflitti, anche se outputDirectory è unica.
                // Questo è il file che il 'Converter' creerà.
                String outputFileName = srcFile.getName().replaceFirst("\\.[^\\.]+$", "") + UUID.randomUUID().toString() + "." + outExt;
                File tempOutputForConverter = new File(outputDirectory, outputFileName);


                // Esegue la conversione vera e propria.
                // Il tuo 'Converter' deve supportare di scrivere in una specifica directory/file.
                // Se i tuoi convertitori interni restituiscono una List<File>,
                // dovrai gestire quale file restituire (es. il primo se è singola conversione).
                // Per semplicità, ipotizzo che convert.convert(srcFile) ora scriva direttamente in tempOutputForConverter
                // O che tu adatterai i tuoi converter per restituire il singolo file di output.
                // ATTENZIONE: Questo è un punto cruciale. Se converter.convert() non scrive nel file,
                // dovrai modificarlo per farlo o per restituire il file creato.

                // Esempio adattato: Se convert.convert(srcFile) restituisce una List<File> che
                // sono già stati creati nel sistema, dovrai copiarli nella outputDirectory.
                List<File> outFilesFromConverter = converter.convert(srcFile); // Assumiamo convert() crea files in una sua temp dir

                if (outFilesFromConverter == null || outFilesFromConverter.isEmpty()) {
                    throw new Exception("Il converter non ha prodotto alcun file di output.");
                }

                // Scegli il primo file convertito (presumendo una conversione 1:1 per il web service)
                File primaryConvertedFile = outFilesFromConverter.get(0);

                // Sposta il file convertito dal percorso temporaneo del converter alla nostra outputDirectory
                // con un nome pulito (senza suffissi extra che il converter potrebbe aver aggiunto)
                String finalOutputFileName = srcFile.getName().replaceFirst("\\.[^\\.]+$", "") + "." + outExt;
                convertedFile = new File(outputDirectory, finalOutputFileName);
                Files.move(primaryConvertedFile.toPath(), convertedFile.toPath(), StandardCopyOption.REPLACE_EXISTING);

                // Qui è dove pulisci eventuali file temporanei extra creati dal tuo 'Converter' interno
                // Se il tuo converter crea una List<File> e li lascia in una sua temp dir, devi pulire.
                for (File f : outFilesFromConverter) {
                    if (!f.equals(primaryConvertedFile)) { // Non cancellare quello che abbiamo appena spostato
                        Files.deleteIfExists(f.toPath());
                    }
                }

                // Rimuovi anche il file sorgente temporaneo che è stato copiato
                Files.deleteIfExists(srcFile.toPath());


            } catch (Exception e) {
                // Logga l'errore per il debug sul server
                e.printStackTrace();
                throw new Exception("Errore interno di conversione: " + e.getMessage(), e);
            }
            return convertedFile; // Restituisce il file convertito
        } else {
            throw new Exception("Formato di destinazione non supportato per questa estensione sorgente: " + outExt);
        }
    }


    // Ripeti il pattern per tutti gli overload di `conversione`
    public File conversione(String srcExt, String outExt, File srcFile, String password, File outputDirectory) throws Exception {
        System.out.println("entro in conversione password (web service)");
        Map<String, Map<String, String>> conversions = config.getConversions();

        if (!conversions.containsKey(srcExt))
            throw new Exception("Conversione non supportata per estensione sorgente: " + srcExt);

        Map<String, String> possibleConversions = conversions.get(srcExt);

        if (possibleConversions.containsKey(outExt)) {
            String converterClassName = possibleConversions.get(outExt);
            System.out.println(srcExt + " -> " + outExt + " tramite " + converterClassName);

            File convertedFile = null;
            try {
                Class<?> clazz = Class.forName(converterClassName);
                Converter converter = (Converter) clazz.getDeclaredConstructor().newInstance();

                if (!outputDirectory.exists()) {
                    outputDirectory.mkdirs();
                }

                // Chiamata al converter con password
                List<File> outFilesFromConverter = converter.convert(srcFile, password); // Assumiamo convert() crea files

                if (outFilesFromConverter == null || outFilesFromConverter.isEmpty()) {
                    throw new Exception("Il converter non ha prodotto alcun file di output.");
                }

                File primaryConvertedFile = outFilesFromConverter.get(0);
                String finalOutputFileName = srcFile.getName().replaceFirst("\\.[^\\.]+$", "") + "." + outExt;
                convertedFile = new File(outputDirectory, finalOutputFileName);
                Files.move(primaryConvertedFile.toPath(), convertedFile.toPath(), StandardCopyOption.REPLACE_EXISTING);

                for (File f : outFilesFromConverter) {
                    if (!f.equals(primaryConvertedFile)) {
                        Files.deleteIfExists(f.toPath());
                    }
                }
                Files.deleteIfExists(srcFile.toPath());

            } catch (Exception e) {
                e.printStackTrace();
                throw new Exception("Errore interno di conversione con password: " + e.getMessage(), e);
            }
            return convertedFile;
        } else {
            throw new Exception("Formato di destinazione non supportato per questa estensione sorgente: " + outExt);
        }
    }

    public File conversione(String srcExt, String outExt, File srcFile, boolean union, File outputDirectory) throws Exception {
        System.out.println("entro in conversione union (web service)");
        Map<String, Map<String, String>> conversions = config.getConversions();

        if (!conversions.containsKey(srcExt))
            throw new Exception("Conversione non supportata per estensione sorgente: " + srcExt);

        Map<String, String> possibleConversions = conversions.get(srcExt);

        if (possibleConversions.containsKey(outExt)) {
            String converterClassName = possibleConversions.get(outExt);
            System.out.println(srcExt + " -> " + outExt + " tramite " + converterClassName);

            File convertedFile = null;
            try {
                Class<?> clazz = Class.forName(converterClassName);
                Converter converter = (Converter) clazz.getDeclaredConstructor().newInstance();

                if (!outputDirectory.exists()) {
                    outputDirectory.mkdirs();
                }

                // Chiamata al converter con union
                List<File> outFilesFromConverter = converter.convert(srcFile, union); // Assumiamo convert() crea files

                if (outFilesFromConverter == null || outFilesFromConverter.isEmpty()) {
                    throw new Exception("Il converter non ha prodotto alcun file di output.");
                }

                File primaryConvertedFile = outFilesFromConverter.get(0);
                String finalOutputFileName = srcFile.getName().replaceFirst("\\.[^\\.]+$", "") + "." + outExt;
                convertedFile = new File(outputDirectory, finalOutputFileName);
                Files.move(primaryConvertedFile.toPath(), convertedFile.toPath(), StandardCopyOption.REPLACE_EXISTING);

                for (File f : outFilesFromConverter) {
                    if (!f.equals(primaryConvertedFile)) {
                        Files.deleteIfExists(f.toPath());
                    }
                }
                Files.deleteIfExists(srcFile.toPath());

            } catch (Exception e) {
                e.printStackTrace();
                throw new Exception("Errore interno di conversione con unione: " + e.getMessage(), e);
            }
            return convertedFile;
        } else {
            throw new Exception("Formato di destinazione non supportato per questa estensione sorgente: " + outExt);
        }
    }

    public File conversione(String srcExt, String outExt, File srcFile, String password, boolean union, File outputDirectory) throws Exception {
        System.out.println("entro in conversione password + union (web service)");
        Map<String, Map<String, String>> conversions = config.getConversions();

        if (!conversions.containsKey(srcExt))
            throw new Exception("Conversione non supportata per estensione sorgente: " + srcExt);

        Map<String, String> possibleConversions = conversions.get(srcExt);

        if (possibleConversions.containsKey(outExt)) {
            String converterClassName = possibleConversions.get(outExt);
            System.out.println(srcExt + " -> " + outExt + " tramite " + converterClassName);

            File convertedFile = null;
            try {
                Class<?> clazz = Class.forName(converterClassName);
                Converter converter = (Converter) clazz.getDeclaredConstructor().newInstance();
                System.out.println("classe istanziata");

                if (!outputDirectory.exists()) {
                    outputDirectory.mkdirs();
                }

                // Chiamata al converter con password e union
                List<File> outFilesFromConverter = converter.convert(srcFile, password, union); // Assumiamo convert() crea files

                if (outFilesFromConverter == null || outFilesFromConverter.isEmpty()) {
                    throw new Exception("Il converter non ha prodotto alcun file di output.");
                }

                File primaryConvertedFile = outFilesFromConverter.get(0);
                String finalOutputFileName = srcFile.getName().replaceFirst("\\.[^\\.]+$", "") + "." + outExt;
                convertedFile = new File(outputDirectory, finalOutputFileName);
                Files.move(primaryConvertedFile.toPath(), convertedFile.toPath(), StandardCopyOption.REPLACE_EXISTING);

                for (File f : outFilesFromConverter) {
                    if (!f.equals(primaryConvertedFile)) {
                        Files.deleteIfExists(f.toPath());
                    }
                }
                Files.deleteIfExists(srcFile.toPath());

            } catch (Exception e) {
                e.printStackTrace();
                throw new Exception("Errore interno di conversione con password e unione: " + e.getMessage(), e);
            }
            return convertedFile;
        } else {
            throw new Exception("Formato di destinazione non supportato per questa estensione sorgente: " + outExt);
        }
    }

    // --- FINE NUOVI METODI `conversione` ---


    // --- METODI ORIGINALI `conversione` CHE SPOSTANO IN successOutputDir ---
    // Questi metodi sono probabilmente per la modalità di "monitoraggio"
    // Li lascio, ma devi essere consapevole che non sono quelli usati dal web service
    // se segui la mia raccomandazione di modifica per il controller.

    public void conversioneOriginale(String srcExt, String outExt, File srcFile) throws Exception {
        System.out.println("entro in conversione (originale)");
        Map<String, Map<String, String>> conversions = config.getConversions();

        if (!conversions.containsKey(srcExt))
            throw new Exception("Conversione non supportata");

        Map<String, String> possibleConversions = conversions.get(srcExt);

        if (possibleConversions.containsKey(outExt)) {
            String converterClassName = possibleConversions.get(outExt);
            System.out.println(srcExt + " -> " + outExt + " tramite " + converterClassName);

            try {
                Class<?> clazz = Class.forName(converterClassName);
                Converter converter = (Converter) clazz.getDeclaredConstructor().newInstance();

                try {
                    // Questa logica di copia e rinomina è specifica per la modalità "monitoraggio"
                    // dove il file sorgente viene processato e poi il risultato spostato.
                    // Non è adatta per l'interfaccia del web service.
                    Path tempFilePath = Paths.get("src" + File.separator + "temp" + File.separator + srcFile.getName());
                    Files.copy(srcFile.toPath(), tempFilePath, StandardCopyOption.REPLACE_EXISTING);
                    srcFile = tempFilePath.toFile();

                    File newFile = giveBackNewFileWithNewName(srcFile.getPath(), ("-[[" + outExt + "]]-"));
                    if (!srcFile.renameTo(newFile)) {
                        System.err.println("Errore: File già convertito al formato richiesto o rinominazione fallita");
                    }
                    srcFile = newFile;

                    List<File> outFiles = converter.convert(srcFile);

                    Files.delete(srcFile.toPath()); // Elimina il file temporaneo originale

                    for (File f : outFiles) {
                        File returnFile = new File(f.getPath().replaceAll("-\\[\\[.*?]]-", ""));
                        if (!f.renameTo(returnFile)) {
                            System.err.println("Errore: File già convertito al formato richiesto o rinominazione finale fallita");
                        }
                        f = returnFile;
                        spostaFile(config.getSuccessOutputDir(), f); // Sposta nella directory di successo
                    }
                } catch (IOException e) {
                    spostaFile(config.getErrorOutputDir(), srcFile); // Sposta nella directory di errore
                    throw new Exception(e.getMessage());
                }
            } catch (Exception e) {
                System.out.println(e.getMessage());
                throw e;
            }
        } else {
            throw new Exception("Conversione non supportata");
        }
    }

    public void conversioneOriginale(String srcExt, String outExt, File srcFile, String password) throws Exception {
        Map<String, Map<String, String>> conversions = config.getConversions();
        System.out.println("entro in conversione password (originale)");
        if (!conversions.containsKey(srcExt))
            throw new Exception("Conversione non supportata");

        Map<String, String> possibleConversions = conversions.get(srcExt);

        if (possibleConversions.containsKey(outExt)) {
            String converterClassName = possibleConversions.get(outExt);
            System.out.println(srcExt + " -> " + outExt + " tramite " + converterClassName);

            try {
                Class<?> clazz = Class.forName(converterClassName);
                Converter converter = (Converter) clazz.getDeclaredConstructor().newInstance();

                try {
                    List<File> outFiles = converter.convert(srcFile, password); // Assumiamo convert() crea files

                    Files.delete(srcFile.toPath()); // Elimina il file originale

                    for (File f : outFiles) {
                        spostaFile(config.getSuccessOutputDir(), f);
                    }
                } catch (IOException e) {
                    spostaFile(config.getErrorOutputDir(), srcFile);
                    throw new Exception(e.getMessage());
                }
            } catch (Exception e) {
                System.out.println(e.getMessage());
                throw e;
            }
        } else {
            throw new Exception("Conversione non supportata");
        }
    }

    public void conversioneOriginale(String srcExt, String outExt, File srcFile, boolean union) throws Exception {
        System.out.println("entro in conversione union (originale)");
        Map<String, Map<String, String>> conversions = config.getConversions();

        if (!conversions.containsKey(srcExt))
            throw new Exception("Conversione non supportata");

        Map<String, String> possibleConversions = conversions.get(srcExt);

        if (possibleConversions.containsKey(outExt)) {
            String converterClassName = possibleConversions.get(outExt);
            System.out.println(srcExt + " -> " + outExt + " tramite " + converterClassName);

            try {
                Class<?> clazz = Class.forName(converterClassName);
                Converter converter = (Converter) clazz.getDeclaredConstructor().newInstance();

                try {
                    Path tempFilePath = Paths.get("src" + File.separator + "temp" + File.separator + srcFile.getName());
                    Files.copy(srcFile.toPath(), tempFilePath, StandardCopyOption.REPLACE_EXISTING);
                    srcFile = tempFilePath.toFile();

                    File newFile = giveBackNewFileWithNewName(srcFile.getPath(), ("-[[" + outExt + "]]-"));
                    if (!srcFile.renameTo(newFile)) {
                        System.err.println("Errore: File già convertito al formato richiesto o rinominazione fallita");
                    }
                    srcFile = newFile;

                    List<File> outFiles = converter.convert(srcFile, union);

                    Files.delete(srcFile.toPath());

                    for (File f : outFiles) {
                        File returnFile = new File(f.getPath().replaceAll("-\\[\\[.*?]]-", ""));
                        if (!f.renameTo(returnFile)) {
                            System.err.println("Errore: File già convertito al formato richiesto o rinominazione finale fallita");
                        }
                        f = returnFile;
                        spostaFile(config.getSuccessOutputDir(), f);
                    }
                } catch (IOException e) {
                    spostaFile(config.getErrorOutputDir(), srcFile);
                    throw new Exception(e.getMessage());
                }
            } catch (Exception e) {
                System.out.println(e.getMessage());
                throw e;
            }
        } else {
            throw new Exception("Conversione non supportata");
        }
    }

    public void conversioneOriginale(String srcExt, String outExt, File srcFile, String password, boolean union) throws Exception {
        System.out.println("entro in conversione password + union (originale)");
        Map<String, Map<String, String>> conversions = config.getConversions();

        if (!conversions.containsKey(srcExt))
            throw new Exception("Conversione non supportata");

        Map<String, String> possibleConversions = conversions.get(srcExt);

        if (possibleConversions.containsKey(outExt)) {
            String converterClassName = possibleConversions.get(outExt);
            System.out.println(srcExt + " -> " + outExt + " tramite " + converterClassName);

            try {
                Class<?> clazz = Class.forName(converterClassName);
                Converter converter = (Converter) clazz.getDeclaredConstructor().newInstance();
                System.out.println("classe istanziata");
                try {
                    Path tempFilePath = Paths.get("src" + File.separator + "temp" + File.separator + srcFile.getName());
                    Files.copy(srcFile.toPath(), tempFilePath, StandardCopyOption.REPLACE_EXISTING);
                    srcFile = tempFilePath.toFile();

                    File newFile = giveBackNewFileWithNewName(srcFile.getPath(), ("-[[" + outExt + "]]-"));
                    if (!srcFile.renameTo(newFile)) {
                        System.err.println("Errore: File già convertito al formato richiesto o rinominazione fallita");
                    }
                    srcFile = newFile;

                    List<File> outFiles = converter.convert(srcFile, password, union);

                    Files.delete(srcFile.toPath());

                    for (File f : outFiles) {
                        File returnFile = new File(f.getPath().replaceAll("-\\[\\[.*?]]-", ""));
                        if (!f.renameTo(returnFile)) {
                            System.err.println("Errore: File già convertito al formato richiesto o rinominazione finale fallita");
                        }
                        f = returnFile;
                        spostaFile(config.getSuccessOutputDir(), f);
                    }
                } catch (IOException e) {
                    spostaFile(config.getErrorOutputDir(), srcFile);
                    throw new Exception(e.getMessage());
                }
            } catch (Exception e) {
                System.out.println(e.getMessage());
                throw e;
            }
        } else {
            throw new Exception("Conversione non supportata");
        }
    }

    // --- FINE METODI ORIGINALI `conversione` ---


    private void spostaFile(String outPath, File file) throws IOException {
        String fileName = file.getName();
        Path srcPath = Paths.get(file.getAbsolutePath());
        Path destPath = Paths.get(outPath + File.separator + fileName);
        Files.move(srcPath, destPath, StandardCopyOption.REPLACE_EXISTING);
        System.out.println("File copiato in " + destPath);
    }

    private static File giveBackNewFileWithNewName(String filePath, String suffix) {
        File file = new File(filePath);
        String name = file.getName();

        int lastDot = name.lastIndexOf(".");

        if (lastDot == -1) {
            return new File(file.getParent() + File.separator + name + suffix);
        }

        String baseName = name.substring(0, lastDot);
        String extension = name.substring(lastDot);

        return new File(file.getParent() + File.separator + baseName + suffix + extension);
    }

    public ConverterConfig getConverterConfig() {
        return config;
    }
}