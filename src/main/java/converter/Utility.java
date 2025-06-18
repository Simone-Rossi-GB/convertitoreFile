package converter;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class Utility {

    public static String estraiEstensioneInterna(File file) {
        String nomeFile = file.getName();
        Pattern pattern = Pattern.compile("-\\$\\$(.*?)\\$\\$-");
        Matcher matcher = pattern.matcher(nomeFile);
        String extracted = null;
        if (matcher.find()) {
            extracted = matcher.group(1);
        }
        return extracted;
    }

    public static String estraiNomePiuEstensioneFile(File file){
        String nomeFile = file.getName();
        String estensioneInterna = estraiEstensioneInterna(file);
        return nomeFile.replace("-$$" + estensioneInterna + "$$-", "");
    }

    /**
     * Copia il contenuto da un InputStream a un OutputStream
     */
    public static void copy(InputStream in, OutputStream out) throws IOException {
        byte[] buffer = new byte[4096];
        int bytesRead;
        while ((bytesRead = in.read(buffer)) != -1) {
            out.write(buffer, 0, bytesRead);
        }
    }
}
