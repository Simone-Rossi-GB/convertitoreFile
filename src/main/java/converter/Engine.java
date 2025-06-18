package converter;

import Converters.Converter;
import com.google.gson.Gson;
import java.io.*;
import java.nio.file.*;
import java.util.*;

public class Engine {
    private ConverterConfig config;
    private static final String CONFIG_FILE_PATH = "src/main/java/converter/config/config.json";

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
            //classe di appoggio per json
            config = gson.fromJson(reader, ConverterConfig.class);
            if (config == null) {
                throw new NullPointerException("L'oggetto config non esiste");
            }
        } catch (Exception e) {
            throw new RuntimeException("Errore nella lettura del file di configurazione", e);
        }
    }

    /**
     * Ritorna la stringa che rappresenta il contenuto del json
     * @return
     * @throws Exception
     */
    public String getConfigAsJson() throws Exception {
        try {
            return new String(Files.readAllBytes(Paths.get(CONFIG_FILE_PATH)));
        } catch (IOException e) {
            throw new Exception("Errore nella lettura del file di configurazione: " + e.getMessage(), e);
        }
    }

    /**
     * Modifica config.json in base alla stringa ricevuta
     * @param jsonText
     * @throws Exception
     */
    public void setConfigFromJson(String jsonText) throws Exception {
        try (FileWriter writer = new FileWriter(CONFIG_FILE_PATH)) {
            writer.write(jsonText);
            setConfig();
        } catch (IOException e) {
            throw new Exception("Errore nella scrittura del file di configurazione: " + e.getMessage(), e);
        } catch (RuntimeException e) {
            throw new Exception("Errore nel caricamento della nuova configurazione: " + e.getMessage(), e);
        }
    }

    /**
     * Ritorna i formati in cui un file può essere convertito
     * @param extension
     * @return
     * @throws Exception
     */
    public List<String> getPossibleConversions(String extension) throws Exception {
        if (extension == null) {
            throw new NullPointerException("L'oggetto extension non esiste");
        }
        if (config == null || config.getConversions() == null || !config.getConversions().containsKey(extension)) {
            throw new Exception("Conversione non supportata");
        }
        return new ArrayList<>(config.getConversions().get(extension).keySet());
    }

    /**
     * Conversione base
     * @param srcExt
     * @param outExt
     * @param srcFile
     * @throws Exception
     */
    public void conversione(String srcExt, String outExt, File srcFile) throws Exception {
        executeConversion(srcExt, outExt, srcFile, null, null);
    }

    /**
     * Conversione PDF protetto
     * @param srcExt
     * @param outExt
     * @param srcFile
     * @param password
     * @throws Exception
     */
    public void conversione(String srcExt, String outExt, File srcFile, String password) throws Exception {
        executeConversion(srcExt, outExt, srcFile, password, null);
    }

    /**
     * Conversione PDF -> JPG unendo le pagine in un'unica immagine
     * @param srcExt
     * @param outExt
     * @param srcFile
     * @param union
     * @throws Exception
     */
    public void conversione(String srcExt, String outExt, File srcFile, boolean union) throws Exception {
        executeConversion(srcExt, outExt, srcFile, null, union);
    }

    /**Conversione PDF protetto -> JPG unendo le pagine in un'unica immagine
     *
     * @param srcExt
     * @param outExt
     * @param srcFile
     * @param password
     * @param union
     * @throws Exception
     */
    public void conversione(String srcExt, String outExt, File srcFile, String password, boolean union) throws Exception {
        executeConversion(srcExt, outExt, srcFile, password, union);
    }

    /**
     * Esecuzione conversione
     * @param srcExt
     * @param outExt
     * @param srcFile
     * @param password
     * @param union
     * @throws Exception
     */
    private void executeConversion(String srcExt, String outExt, File srcFile, String password, Boolean union) throws Exception {
        if (srcExt == null) throw new NullPointerException("L'oggetto srcExt non esiste");
        if (outExt == null) throw new NullPointerException("L'oggetto outExt non esiste");
        if (srcFile == null) throw new NullPointerException("L'oggetto srcFile non esiste");

        Map<String, Map<String, String>> conversions = config.getConversions();
        if (conversions == null || !conversions.containsKey(srcExt)) {
            throw new Exception("Conversione non supportata");
        }

        Map<String, String> possibleConversions = conversions.get(srcExt);
        String converterClassName = possibleConversions.get(outExt);
        if (converterClassName == null) {
            throw new Exception("Conversione non supportata");
        }

        Class<?> clazz = Class.forName(converterClassName);
        Converter converter = (Converter) clazz.getDeclaredConstructor().newInstance();

        try {
            //file temporaneo per evitare conflitti
            Path tempPath = Paths.get("src", "temp", srcFile.getName());
            Files.copy(srcFile.toPath(), tempPath, StandardCopyOption.REPLACE_EXISTING);
            srcFile = tempPath.toFile();

            File renamedFile = giveBackNewFileWithNewName(srcFile.getPath(), "-[[" + outExt + "]]-");
            if (!srcFile.renameTo(renamedFile)) {
                Log.addMessage("ERRORE: Rinominazione file pre-convert fallita: "+srcFile.getName()+" -> "+renamedFile.getName());
                throw new Exception("ERRORE: Rinominazione file pre-convert fallita");
            }
            srcFile = renamedFile;

            List<File> outFiles;
            if (password != null && union != null) {
                outFiles = converter.convert(srcFile, password, union);
            } else if (password != null) {
                outFiles = converter.convert(srcFile, password);
            } else if (union != null) {
                outFiles = converter.convert(srcFile, union);
            } else {
                outFiles = converter.convert(srcFile);
            }

            Files.deleteIfExists(srcFile.toPath());

            for (File f : outFiles) {
                File cleaned = new File(f.getPath().replaceAll("-\\[\\[.*?]]-", ""));
                if (!f.renameTo(cleaned)) {
                    Log.addMessage("ERRORE: Rinominazione file post-convert fallita: "+f.getName()+" -> "+cleaned.getName());
                    throw new Exception("ERRORE: Rinominazione file post-convert fallita");
                }
                //Sposto il file convertito nella directory corretta
                spostaFile(config.getSuccessOutputDir(), cleaned);
            }
        } catch (IOException e) {
            //Sposto il file di origine nella directory degli errori
            spostaFile(config.getErrorOutputDir(), srcFile);
            throw new Exception(e.getMessage());
        }
    }

    /**
     * Sposta il file ricevuto nella directory indicata
     * @param outPath
     * @param file
     * @throws IOException
     */
    private void spostaFile(String outPath, File file) throws IOException {
        if (file == null) throw new NullPointerException("L'oggetto file non esiste");
        if (outPath == null) throw new NullPointerException("L'oggetto outPath non esiste");
        Path dest = Paths.get(outPath, file.getName());
        Files.move(file.toPath(), dest, StandardCopyOption.REPLACE_EXISTING);
        System.out.println("File copiato in " + dest);
    }

    /**
     * Ritorna un file temporaneo identico a quello passato ma con un suffisso, in modo da evitare conflitti con altri file
     * @param filePath
     * @param suffix
     * @return
     */
    private static File giveBackNewFileWithNewName(String filePath, String suffix) {
        if (filePath == null) throw new NullPointerException("L'oggetto filePath non esiste");
        if (suffix == null) throw new NullPointerException("L'oggetto suffix non esiste");

        File file = new File(filePath);
        String name = file.getName();
        int lastDot = name.lastIndexOf(".");

        String newName = (lastDot == -1) ? name + suffix : name.substring(0, lastDot) + suffix + name.substring(lastDot);
        return new File(file.getParent(), newName);
    }

    /**
     * Ritorna la configurazione ottenuta da config.json
     * @return
     */
    public ConverterConfig getConverterConfig() {
        if (config == null) {
            throw new NullPointerException("L'oggetto config non esiste");
        }
        return config;
    }
}
