package Converters;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import converter.Log;
import converter.Utility;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;

public class CSVtoJSONconverter implements Converter {
    /**
     * Converte un file CSV in un file JSON con delimitatore rilevato automaticamente.
     * Ogni riga del CSV diventa un oggetto JSON.
     */
    public ArrayList<File> convert(File srcFile) throws IOException {
        Log.addMessage("Inizio conversione CSV: " + srcFile.getName() + " → .json");
        List<String> lines = Files.readAllLines(srcFile.toPath(), StandardCharsets.UTF_8);
        if (lines.isEmpty()) {
            Log.addMessage("Errore: Il file CSV è vuoto → " + srcFile.getName());
            throw new IOException("Il file CSV è vuoto: " + srcFile.getName());
        }

        String headerLine = removeBOM(lines.get(0));
        String delimiter = detectDelimiter(headerLine);
        String[] headers = splitCsvLine(headerLine, delimiter);

        ObjectMapper mapper = new ObjectMapper();
        ArrayNode jsonArray = mapper.createArrayNode();

        for (int i = 1; i < lines.size(); i++) {
            String rawLine = lines.get(i).trim();
            if (rawLine.isEmpty()) continue;

            String[] values = splitCsvLine(rawLine, delimiter);
            if (values.length < headers.length) {
                Log.addMessage("Attenzione: Riga " + (i + 1) + " con celle insufficienti → Skippata");
                continue;
            }

            if (values.length > headers.length) {
                Log.addMessage("Attenzione: Riga " + (i + 1) + " con celle extra → Verranno ignorate");
            }

            ObjectNode obj = mapper.createObjectNode();
            for (int j = 0; j < headers.length; j++) {
                String key = stripQuotes(headers[j]);
                String value = stripQuotes(values[j]);

                if (value.matches("^-?\\d+(\\.\\d+)?$")) {
                    obj.put(key, Double.parseDouble(value));
                } else {
                    obj.put(key, value);
                }
            }
            jsonArray.add(obj);
        }

        File output = new File(srcFile.getParent(), getBaseName(srcFile) + ".json");
        try {
            mapper.writerWithDefaultPrettyPrinter().writeValue(output, jsonArray);
        } catch (IOException e) {
            Log.addMessage("Errore durante la scrittura del file JSON: " + output.getName());
            throw e;
        }

        Log.addMessage("File JSON creato: " + output.getName());
        return new ArrayList<File>() {{
            add(output);
        }};
    }

    /**
     * Rimuove il carattere BOM all'inizio della stringa, se presente.
     */
    private static String removeBOM(String line) {
        return line.replace("\uFEFF", "");
    }

    /**
     * Rileva il delimitatore più probabile analizzando la riga dell'header.
     */
    private static String detectDelimiter(String line) {
        char[] delimiters = {',', ';', '\t', '|'};
        int maxCount = 0;
        String selected = ",";
        for (char delim : delimiters) {
            int count = line.split(Pattern.quote(String.valueOf(delim)), -1).length - 1;
            if (count > maxCount) {
                maxCount = count;
                selected = String.valueOf(delim);
            }
        }
        return selected;
    }

    /**
     * Divide una riga CSV nei suoi campi, rispettando eventuali virgolette.
     */
    private static String[] splitCsvLine(String line, String delimiter) {
        List<String> result = new ArrayList<>();
        boolean inQuotes = false;
        StringBuilder sb = new StringBuilder();

        for (int i = 0; i < line.length(); i++) {
            char c = line.charAt(i);
            if (c == '"') {
                inQuotes = !inQuotes;
            } else if (String.valueOf(c).equals(delimiter) && !inQuotes) {
                result.add(sb.toString());
                sb.setLength(0);
            } else {
                sb.append(c);
            }
        }
        result.add(sb.toString());
        return result.toArray(new String[0]);
    }

    /**
     * Rimuove virgolette iniziali/finali da una stringa, se presenti.
     */
    private static String stripQuotes(String s) {
        if (s == null) return "";
        s = s.trim();
        if (s.startsWith("\"") && s.endsWith("\"") && s.length() > 1) {
            return s.substring(1, s.length() - 1);
        }
        return s;
    }

    /**
     * Estrae il nome base di un file (senza estensione).
     */
    private static String getBaseName(File file) {
        String name = file.getName();
        int lastDot = name.lastIndexOf('.');
        return (lastDot == -1) ? name : name.substring(0, lastDot);
    }
}

