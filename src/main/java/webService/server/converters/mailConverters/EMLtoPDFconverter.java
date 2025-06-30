package webService.server.converters.mailConverters;

import webService.client.configuration.configHandlers.conversionContext.ConversionContextReader;
import webService.server.configuration.configHandlers.serverConfig.ConfigReader;
import webService.server.converters.Converter;
import org.apache.james.mime4j.dom.*;
import org.apache.james.mime4j.dom.field.ContentTypeField;
import org.apache.james.mime4j.message.DefaultMessageBuilder;
import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.LogManager;
import webService.server.converters.PDFWatermarkApplier;
import webService.server.converters.exception.WatermarkException;

import java.awt.Desktop;
import java.io.*;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Convertitore EML to PDF usando Chrome/Chromium headless nativo.
 * Implementa l'approccio: EML → HTML → PDF tramite Chrome --headless.
 * Supporta immagini embedded, diversi encoding e gestisce automaticamente
 * la pulizia dei file temporanei.
 */
public class EMLtoPDFconverter extends Converter {

    private static final Logger logger = LogManager.getLogger(EMLtoPDFconverter.class);
    private Map<String, String> embeddedImages = new HashMap<String, String>();
    private File tempDir;
    private String originalBaseName; // Campo per memorizzare il nome base originale del file EML

    private static final boolean DEBUG_OPEN_HTML_IN_BROWSER = false;
    private static final boolean DEBUG_KEEP_TEMP_FILES = true; // Mantenuto TRUE per debugging

    /**
     * Converte un file EML in PDF utilizzando Chrome Headless.
     * Il processo include parsing dell'email, estrazione immagini embedded,
     * generazione HTML e conversione finale in PDF.
     *
     * @param emlFile Il file EML da convertire
     * @return Il file PDF generato
     * @throws IOException Se si verificano errori durante la conversione
     */
    @Override
    public File convert(File emlFile) throws IOException, WatermarkException {
        if (emlFile == null || !emlFile.exists()) {
            throw new FileNotFoundException("File EML non trovato: " + emlFile);
        }

        logger.info("Inizio conversione EML to PDF con Chrome Headless: {}", emlFile.getName());

        tempDir = new File(emlFile.getParent());

        try {
            Message message = parseEmailMessage(emlFile);
            extractEmbeddedImages(message.getBody());
            File htmlFile = generateCompleteHtml(message);

            File pdfFile = convertHtmlToPdfWithChrome(htmlFile);

            logger.info("Conversione completata: {}", pdfFile.getName());

            if (!ConversionContextReader.getWatermark().isEmpty()) {
                logger.info("Applying watermark to PDF...");

                // Crea un file temporaneo per il PDF con watermark nella stessa directory
                File tempFile = new File(pdfFile.getParent(), "watermarked_" + pdfFile.getName());

                logger.info("Original file: {}", pdfFile.getAbsolutePath());
                logger.info("Temp file for watermark: {}", tempFile.getAbsolutePath());

                try {
                    boolean success = PDFWatermarkApplier.applyWatermark(
                            pdfFile,
                            tempFile,
                            ConversionContextReader.getWatermark()
                    );

                    logger.info("Watermark application completed, success: {}", success);

                    if (success && tempFile.exists() && tempFile.length() > 0) {
                        logger.info("Watermark applied successfully, replacing original file");

                        // Usa Files.move() per sostituzione atomica
                        try {
                            Files.move(tempFile.toPath(), pdfFile.toPath(),
                                    StandardCopyOption.REPLACE_EXISTING);
                            logger.info("File watermarkato sostituito correttamente");
                            //logger.info("tempfile: {}, pdffile: {}", tempFile.);

                            return pdfFile; // Ritorna sempre pdfFile
                        } catch (IOException e) {
                            logger.warn("Impossibile sostituire il file: {}", e.getMessage());
                            throw new WatermarkException("Impossibile sostituire il file con watermark: " + e.getMessage());
                        }
                    } else {
                        logger.warn("Watermark application failed - temp file not created or empty");
                        throw new WatermarkException("Watermark non applicato correttamente");
                    }
                } catch (Exception e) {
                    throw new WatermarkException("Impossibile applicare il watermark: " + e.getMessage());
                }
            }

            return pdfFile;

        } catch (Exception e) {
            logger.error("Errore durante la conversione", e);
            throw new IOException("Errore durante la conversione: " + e.getMessage(), e);
        }
//        finally {
//            if (!DEBUG_KEEP_TEMP_FILES) {
//                cleanup();
//                logger.info("File temporanei eliminati.");
//            } else {
//                logger.info("File temporanei mantenuti per debugging nella directory: {}", tempDir.getAbsolutePath());
//            }
//        }
    }


    /**
     * Effettua il parsing del file EML utilizzando Apache James Mime4J.
     *
     * @param emlFile Il file EML da analizzare
     * @return L'oggetto Message contenente i dati parsati dell'email
     * @throws IOException Se si verificano errori durante il parsing
     */
    private Message parseEmailMessage(File emlFile) throws IOException {
        DefaultMessageBuilder builder = new DefaultMessageBuilder();
        try (InputStream is = Files.newInputStream(emlFile.toPath())) {
            return builder.parseMessage(is);
        }
    }

    /**
     * Converte il file HTML in PDF utilizzando Chrome Headless.
     * Configura tutti i parametri necessari per una conversione di qualità
     * e gestisce l'esecuzione del processo Chrome.
     *
     * @param htmlFile Il file HTML da convertire
     * @return Il file PDF generato
     * @throws IOException Se Chrome non è disponibile o la conversione fallisce
     * @throws InterruptedException Se il processo viene interrotto
     */
    private File convertHtmlToPdfWithChrome(File htmlFile) throws IOException, InterruptedException {
        String chromePath = ChromeManager.getInstance().getChromePath();
        if (chromePath == null) {
            throw new IOException("Chrome non disponibile tramite ChromeManager");
        }

        // Usa il nome base originale (del file EML) per il nome del file PDF
        File pdfFile = new File(tempDir, originalBaseName + ".pdf");

        List<String> command = new ArrayList<String>();
        command.add(chromePath);
        command.add("--headless");
        command.add("--no-sandbox"); // Necessario in alcuni ambienti Linux/Docker
        command.add("--disable-gpu"); // Necessario per ambienti senza GUI/GPU
        command.add("--disable-dev-shm-usage"); // Utile in ambienti Docker/Linux
        command.add("--disable-extensions");
        command.add("--disable-plugins");
        command.add("--run-all-compositor-stages-before-draw"); // Garantisce rendering completo
        command.add("--virtual-time-budget=10000"); // 10 secondi timeout caricamento pagina
        command.add("--print-to-pdf=" + pdfFile.getAbsolutePath());
        command.add("--print-to-pdf-no-header"); // Flag per rimuovere header/footer predefiniti del browser
        command.add("file://" + htmlFile.getAbsolutePath().replace("\\", "/")); // Percorso del file HTML da convertire

        logger.info("Esecuzione comando Chrome: {}", String.join(" ", command));

        ProcessBuilder pb = new ProcessBuilder(command);
        pb.redirectErrorStream(true);

        Process process = pb.start();

        try (BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()))) {
            String line;
            StringBuilder output = new StringBuilder();
            while ((line = reader.readLine()) != null) {
                output.append(line).append("\n");
            }
            if (output.length() > 0) {
                logger.debug("Output Chrome: {}", output.toString());
            }
        }

        int exitCode = process.waitFor();

        if (exitCode != 0) {
            logger.error("Chrome headless failed with exit code: {}. Command: {}", exitCode, String.join(" ", command));
            throw new IOException("Chrome headless failed with exit code: " + exitCode);
        }

        if (!pdfFile.exists()) {
            logger.error("PDF non generato da Chrome. Percorso atteso: {}", pdfFile.getAbsolutePath());
            throw new IOException("PDF non generato da Chrome");
        }

        return pdfFile;
    }

    /**
     * Apre il file HTML generato nel browser per debug e verifica visuale.
     * Prova prima il browser predefinito del sistema, poi Chrome esplicito.
     * Questo metodo è chiamato solo se DEBUG_OPEN_HTML_IN_BROWSER è true.
     *
     * @param htmlFile Il file HTML da aprire
     * @param chromePath Il percorso di Chrome da usare come fallback
     */
    private void openHtmlInBrowser(File htmlFile, String chromePath) {
        if (!htmlFile.exists()) {
            logger.warn("Impossibile aprire il file HTML, non esiste: {}", htmlFile.getAbsolutePath());
            return;
        }

        try {
            if (Desktop.isDesktopSupported() && Desktop.getDesktop().isSupported(Desktop.Action.BROWSE)) {
                Desktop.getDesktop().browse(htmlFile.toURI());
                logger.info("File HTML aperto con il browser predefinito: {}", htmlFile.getAbsolutePath());
                return;
            }
        } catch (UnsupportedOperationException e) {
            logger.warn("L'operazione 'browse' non è supportata dal Desktop API in questo ambiente. Errore: {}", e.getMessage());
        } catch (IOException e) {
            logger.warn("Errore I/O durante l'apertura del file HTML con il browser predefinito: {}", e.getMessage());
        }

        if (chromePath != null && new File(chromePath).exists()) {
            try {
                List<String> command = new ArrayList<>();
                command.add(chromePath);
                command.add(htmlFile.toURI().toString());

                logger.info("Tentativo di apertura HTML con Chrome esplicito: {}", String.join(" ", command));

                ProcessBuilder pb = new ProcessBuilder(command);
                pb.redirectErrorStream(true);
                Process process = pb.start();
                logger.info("Comando di apertura Chrome avviato per: {}", htmlFile.getAbsolutePath());

            } catch (IOException e) {
                logger.error("Errore durante il tentativo di aprire l'HTML con Chrome esplicito: {}", e.getMessage());
            }
        } else {
            logger.warn("Impossibile aprire l'HTML nel browser: Desktop API non disponibile e percorso Chrome non valido.");
        }
    }

    /**
     * Genera il file HTML completo dall'email parsata.
     * Include CSS ottimizzato per stampa, gestione responsive e
     * il contenuto dell'email processato per la conversione PDF.
     *
     * @param message L'oggetto Message contenente i dati dell'email
     * @return Il file HTML generato e pronto per la conversione
     * @throws IOException Se si verificano errori durante la generazione
     */
    private File generateCompleteHtml(Message message) throws IOException {
        StringBuilder html = new StringBuilder();

        // Header HTML standard
        html.append("<!DOCTYPE html>\n");
        html.append("<html>\n");
        html.append("<head>\n");
        html.append("    <meta charset=\"UTF-8\">\n");
        html.append("    <meta name=\"viewport\" content=\"width=device-width, initial-scale=1\">\n");
        html.append("    <title>Email PDF</title>\n");
        html.append("    <style>\n");
        html.append("        @page {\n");
        html.append("            size: A4;\n");
        html.append("            margin: 0; /* Imposta i margini della pagina di stampa a 0 per eliminare spazi vuoti */\n");
        html.append("        }\n");
        html.append("        \n");
        html.append("        html, body {\n");
        html.append("            margin: 0;\n");
        html.append("            padding: 0;\n");
        html.append("            height: 100%;\n"); // Assicura che html e body coprano l'intera altezza
        html.append("            width: 100%;\n");
        html.append("        }\n");
        html.append("        \n");
        html.append("        * {\n");
        html.append("            box-sizing: border-box;\n");
        html.append("        }\n");
        html.append("        \n");
        html.append("        body {\n");
        html.append("            font-family: 'Segoe UI', Arial, sans-serif;\n");
        html.append("            font-size: 11px;\n");
        html.append("            line-height: 1.4;\n");
        html.append("            color: #333;\n");
        html.append("            background: white;\n");
        html.append("        }\n");

        html.append("        .email-content {\n");
        html.append("            max-width: 100%;\n");
        html.append("            word-wrap: break-word;\n");
        html.append("            overflow-wrap: break-word;\n");
        html.append("        }\n");
        html.append("        \n");
        html.append("        img {\n");
        html.append("            max-width: 100%;\n");
        html.append("            height: auto;\n");
        html.append("            display: block;\n");
        html.append("            margin: 8px 0;\n");
        html.append("        }\n");
        html.append("        \n");
        html.append("        table {\n");
        html.append("            border-collapse: collapse;\n");
        html.append("            width: 100%;\n");
        html.append("            margin: 8px 0;\n");
        html.append("            font-size: inherit;\n");
        html.append("        }\n");
        html.append("        \n");
        html.append("        td, th {\n");
        html.append("            padding: 6px;\n");
        html.append("            vertical-align: top;\n");
        html.append("            border: 1px solid #ddd;\n");
        html.append("            word-wrap: break-word;\n");
        html.append("        }\n");
        html.append("        \n");
        html.append("        .no-print {\n");
        html.append("            display: none !important;\n");
        html.append("        }\n");
        html.append("        \n");
        html.append("        table table {\n");
        html.append("            border: none;\n");
        html.append("        }\n");
        html.append("        \n");
        html.append("        table table td {\n");
        html.append("            border: none;\n");
        html.append("        }\n");
        html.append("        \n");
        // NUOVE REGOLE CSS per nascondere i footer/header di stampa di Chrome
        html.append("        @media print {\n");
        html.append("            /* Nasconde gli header/footer testuali di Chrome */\n");
        html.append("            body::before, body::after {\n");
        html.append("                content: none !important;\n");
        html.append("                display: none !important;\n");
        html.append("            }\n");
        html.append("            /* Forza i margini della pagina a zero per evitare spazi bianchi indesiderati */\n");
        html.append("            @page {\n");
        html.append("                margin: 0 !important;\n");
        html.append("            }\n");
        html.append("        }\n");
        html.append("    </style>\n");
        html.append("</head>\n");
        html.append("<body>\n");

        html.append("<div class=\"email-content\">");
        appendEmailContent(html, message.getBody());
        html.append("</div>");

        html.append("</body></html>");

        File htmlFile = new File(tempDir, "email.html");
        try (OutputStreamWriter writer = new OutputStreamWriter(new FileOutputStream(htmlFile), StandardCharsets.UTF_8)) {
            writer.write(html.toString());
        }

        logger.info("HTML generato: {}", htmlFile.getAbsolutePath());
        return htmlFile;
    }

    /**
     * Aggiunge gli header dell'email (Da, A, Oggetto, Data) all'HTML.
     * Metodo helper per formattare le informazioni di intestazione dell'email.
     *
     * @param html Il StringBuilder dove aggiungere l'HTML degli header
     * @param message L'oggetto Message contenente i dati dell'email
     */
    private void appendEmailHeaders(StringBuilder html, Message message) {
        appendHeaderField(html, "Da:", getFieldValue(message.getFrom()));
        appendHeaderField(html, "A:", getFieldValue(message.getTo()));
        if (message.getCc() != null) {
            appendHeaderField(html, "Cc:", getFieldValue(message.getCc()));
        }
        appendHeaderField(html, "Oggetto:", message.getSubject());
        appendHeaderField(html, "Data:", getFieldValue(message.getDate()));
    }

    /**
     * Aggiunge un singolo campo di header formattato all'HTML.
     *
     * @param html Il StringBuilder dove aggiungere l'HTML
     * @param label L'etichetta del campo (es. "Da:", "A:")
     * @param value Il valore del campo
     */
    private void appendHeaderField(StringBuilder html, String label, String value) {
        html.append("<div class=\"header-field\">")
                .append("<span class=\"header-label\">").append(escapeHtml(label)).append("</span>")
                .append("<span>").append(escapeHtml(value != null ? value : "")).append("</span>")
                .append("</div>");
    }

    /**
     * Processa ricorsivamente il contenuto dell'email e lo converte in HTML.
     * Gestisce sia parti testuali (HTML/plain text) che multipart,
     * processando le immagini embedded e i riferimenti CID.
     *
     * @param html Il StringBuilder dove aggiungere l'HTML del contenuto
     * @param body Il Body dell'email da processare
     * @throws IOException Se si verificano errori durante la lettura del contenuto
     */
    private void appendEmailContent(StringBuilder html, Body body) throws IOException {
        if (body instanceof TextBody) {
            TextBody textBody = (TextBody) body;
            String mimeType = getContentType(textBody);
            String content = readTextContent(textBody);

            if (mimeType.contains("text/html")) {
                String processedHtml = processCidReferences(content);
                html.append(processedHtml);
            } else if (mimeType.contains("text/plain")) {
                // Non generiamo alcun HTML per il contenuto text/plain
                // se non desiderato (come da tua precedente indicazione).
            }
        } else if (body instanceof Multipart) {
            Multipart multipart = (Multipart) body;
            for (Entity part : multipart.getBodyParts()) {
                appendEmailContent(html, part.getBody());
            }
        }
    }

    /**
     * Estrae ricorsivamente le immagini embedded dall'email.
     * Cerca parti con MIME type image/* e Content-ID, salvandole come file temporanei
     * per il riferimento nell'HTML generato.
     *
     * @param body Il Body dell'email da analizzare
     * @throws IOException Se si verificano errori durante l'estrazione
     */
    private void extractEmbeddedImages(Body body) throws IOException {
        if (body instanceof TextBody) {
            TextBody textBody = (TextBody) body;
            String mimeType = getContentType(textBody);

            if (mimeType.startsWith("image/")) {
                String contentId = getContentId(textBody);
                if (contentId != null) {
                    saveEmbeddedImage(textBody, contentId, mimeType);
                }
            }
        } else if (body instanceof Multipart) {
            Multipart multipart = (Multipart) body;
            for (Entity part : multipart.getBodyParts()) {
                extractEmbeddedImages(part.getBody());
            }
        }
    }

    /**
     * Salva un'immagine embedded decodificando il contenuto Base64.
     * Crea un file temporaneo e mappa il Content-ID per i riferimenti HTML.
     *
     * @param textBody Il TextBody contenente i dati dell'immagine
     * @param contentId Il Content-ID dell'immagine per i riferimenti CID
     * @param mimeType Il tipo MIME dell'immagine
     * @throws IOException Se si verificano errori durante il salvataggio
     */
    private void saveEmbeddedImage(TextBody textBody, String contentId, String mimeType) throws IOException {
        String content = readTextContent(textBody);

        try {
            byte[] imageData = java.util.Base64.getDecoder().decode(content.replaceAll("\\s", ""));
            String extension = getImageExtension(mimeType);
            String fileName = contentId.replaceAll("[<>]", "") + "." + extension;

            File imageFile = new File(tempDir, fileName);
            try (FileOutputStream fos = new FileOutputStream(imageFile)) {
                fos.write(imageData);
            }

            embeddedImages.put(contentId, imageFile.getAbsolutePath());
            logger.info("Immagine salvata: {} -> {}", contentId, fileName);

        } catch (IllegalArgumentException e) {
            logger.warn("Impossibile decodificare immagine Base64: {}", contentId);
        }
    }

    /**
     * Processa i riferimenti CID nell'HTML sostituendoli con percorsi file locali.
     * Converte i riferimenti "cid:contentId" in percorsi "file:///" per Chrome.
     *
     * @param htmlContent Il contenuto HTML da processare
     * @return L'HTML con i riferimenti CID sostituiti
     */
    private String processCidReferences(String htmlContent) {
        String processed = htmlContent;

        for (Map.Entry<String, String> entry : embeddedImages.entrySet()) {
            String contentId = entry.getKey().replaceAll("[<>]", "");
            String localPath = "file:///" + entry.getValue().replace("\\", "/");
            processed = processed.replaceAll(
                    "src=[\"']cid:" + contentId + "[\"']",
                    "src=\"" + localPath + "\""
            );
        }

        return processed;
    }

    /**
     * Estrae il Content-Type dal TextBody analizzando gli header della parte.
     *
     * @param textBody Il TextBody da analizzare
     * @return Il tipo MIME, o "text/plain" come default
     */
    private String getContentType(TextBody textBody) {
        Entity parent = textBody.getParent();
        if (parent != null && parent.getHeader() != null) {
            ContentTypeField field = (ContentTypeField) parent.getHeader().getField("Content-Type");
            return field != null ? field.getMimeType() : "text/plain";
        }
        return "text/plain";
    }

    /**
     * Estrae il Content-ID dal TextBody analizzando gli header della parte.
     *
     * @param textBody Il TextBody da analizzare
     * @return Il Content-ID, o null se non presente
     */
    private String getContentId(TextBody textBody) {
        Entity parent = textBody.getParent();
        if (parent != null && parent.getHeader() != null) {
            org.apache.james.mime4j.stream.Field field = parent.getHeader().getField("Content-ID");
            return field != null ? field.getBody() : null;
        }
        return null;
    }

    /**
     * Legge il contenuto testuale di un TextBody rilevando automaticamente l'encoding
     * dall'header Content-Type. Supporta fallback intelligente per encoding non supportati.
     *
     * @param textBody Il TextBody da leggere
     * @return Il contenuto testuale decodificato correttamente
     * @throws IOException Se si verificano errori durante la lettura
     */
    private String readTextContent(TextBody textBody) throws IOException {
        String charset = getCharset(textBody);

        // Prima prova con il charset rilevato
        try (InputStreamReader reader = new InputStreamReader(textBody.getInputStream(), charset)) {
            return readFromReader(reader);
        } catch (UnsupportedEncodingException e) {
            logger.warn("Charset non supportato: {}, tentativo fallback su UTF-8", charset);

            // Fallback 1: UTF-8
            try (InputStreamReader reader = new InputStreamReader(textBody.getInputStream(), StandardCharsets.UTF_8)) {
                return readFromReader(reader);
            } catch (Exception e2) {
                logger.warn("Fallback UTF-8 fallito, tentativo con ISO-8859-1");

                // Fallback 2: ISO-8859-1 (supporta tutti i byte 0-255)
                try (InputStreamReader reader = new InputStreamReader(textBody.getInputStream(), StandardCharsets.ISO_8859_1)) {
                    return readFromReader(reader);
                } catch (Exception e3) {
                    logger.error("Tutti i tentativi di decodifica falliti, uso default system charset");

                    // Fallback finale: charset di sistema
                    try (InputStreamReader reader = new InputStreamReader(textBody.getInputStream())) {
                        return readFromReader(reader);
                    }
                }
            }
        }
    }

    /**
     * Rileva il charset dall'header Content-Type del TextBody.
     *
     * @param textBody Il TextBody da analizzare
     * @return Il charset rilevato o "UTF-8" come default
     */
    private String getCharset(TextBody textBody) {
        Entity parent = textBody.getParent();
        if (parent != null && parent.getHeader() != null) {
            ContentTypeField field = (ContentTypeField) parent.getHeader().getField("Content-Type");
            if (field != null && field.getCharset() != null) {
                String charset = field.getCharset();
                logger.debug("Charset rilevato dall'header: {}", charset);
                return charset;
            }
        }

        // Default fallback
        logger.debug("Nessun charset specificato, uso UTF-8 come default");
        return "UTF-8";
    }

    /**
     * Utility method per leggere da un InputStreamReader.
     *
     * @param reader Il reader da cui leggere
     * @return Il contenuto letto come stringa
     * @throws IOException Se si verificano errori durante la lettura
     */
    private String readFromReader(InputStreamReader reader) throws IOException {
        StringBuilder sb = new StringBuilder();
        char[] buffer = new char[4096];
        int bytesRead;
        while ((bytesRead = reader.read(buffer)) != -1) {
            sb.append(buffer, 0, bytesRead);
        }
        return sb.toString();
    }

    /**
     * Converte un tipo MIME in estensione file appropriata.
     *
     * @param mimeType Il tipo MIME da convertire
     * @return L'estensione file corrispondente
     */
    private String getImageExtension(String mimeType) {
        String lowerType = mimeType.toLowerCase();
        if ("image/gif".equals(lowerType)) {
            return "gif";
        } else if ("image/jpeg".equals(lowerType)) {
            return "jpg";
        } else if ("image/png".equals(lowerType)) {
            return "png";
        } else if ("image/bmp".equals(lowerType)) {
            return "bmp";
        } else if ("image/webp".equals(lowerType)) {
            return "webp";
        } else {
            return "img";
        }
    }

    /**
     * Converte un oggetto field in stringa per la visualizzazione.
     *
     * @param field L'oggetto field da convertire
     * @return La rappresentazione stringa del field, o stringa vuota se null
     */
    private String getFieldValue(Object field) {
        return field != null ? field.toString() : "";
    }

    /**
     * Effettua l'escape dei caratteri HTML speciali per evitare problemi di rendering.
     *
     * @param text Il testo da processare
     * @return Il testo con i caratteri HTML escapati
     */
    private String escapeHtml(String text) {
        if (text == null) return "";
        return text.replace("&", "&amp;")
                .replace("<", "&lt;")
                .replace(">", "&gt;")
                .replace("\"", "&quot;")
                .replace("'", "&#39;");
    }

    @Deprecated
    /**
     * Pulisce i file temporanei e la directory di lavoro.
     * Elimina tutti i file nella directory temporanea e poi la directory stessa.
     * Viene chiamato nel blocco finally se DEBUG_KEEP_TEMP_FILES è false.
     */
    private void cleanup() {
        if (tempDir != null && tempDir.exists()) {
            File[] files = tempDir.listFiles();
            if (files != null) {
                for (File file : files) {
                    if (!file.delete()) {
                        logger.warn("Impossibile eliminare il file temporaneo: {}", file.getAbsolutePath());
                    }
                }
            }
            if (!tempDir.delete()) {
                logger.warn("Impossibile eliminare la directory temporanea: {}", tempDir.getAbsolutePath());
            }
        }
        embeddedImages.clear();
    }
}