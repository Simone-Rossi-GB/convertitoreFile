package converter;

import java.io.File;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class Utility {

    public static String estraiEstensioneInterna(File file) {
        String nomeFile = file.getName();
        Pattern pattern = Pattern.compile("\\[\\[(.*?)]]");
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
        return nomeFile.replace("-[[" + estensioneInterna + "]]-", "");
    }
}
