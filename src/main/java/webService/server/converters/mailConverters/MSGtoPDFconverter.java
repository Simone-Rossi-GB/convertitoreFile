package webService.server.converters.mailConverters;

import webService.server.configuration.configHandlers.conversionContext.ConversionContextReader;
import webService.server.converters.Converter;
import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.LogManager;
import org.apache.poi.hsmf.MAPIMessage;
import org.apache.poi.hsmf.datatypes.AttachmentChunks;
import org.apache.poi.hsmf.exceptions.ChunkNotFoundException;
import webService.server.converters.PDFWatermarkApplier;
import webService.server.converters.exception.WatermarkException;

import java.awt.Desktop;
import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Convertitore MSG to PDF usando Chrome/Chromium headless nativo.
 * Implementa l'approccio: MSG → HTML → PDF tramite Chrome --headless.
 * Utilizza Apache POI per il parsing dei file MSG di Outlook e supporta
 * l'estrazione di immagini embedded e diversi tipi di contenuto (HTML, RTF, plain text).
 */
public class MSGtoPDFconverter extends Converter {

    private static final Logger logger = LogManager.getLogger(MSGtoPDFconverter.class);
    private Map<String, String> embeddedImages = new HashMap<String, String>();
    private File tempDir;
    private String originalBaseName;

    private static final boolean DEBUG_OPEN_HTML_IN_BROWSER = false;
    private static final boolean DEBUG_KEEP_TEMP_FILES = true; // Mantenuto TRUE per evitare il cleanup

    /**
     * Converte un file MSG in PDF utilizzando Chrome Headless.
     * Il processo include parsing del file MSG, estrazione immagini embedded,
     * generazione HTML e conversione finale in PDF.
     *
     * @param msgFile Il file MSG da convertire
     * @return Il file PDF generato
     * @throws IOException Se si verificano errori durante la conversione
     */
    @Override
    public File convert(File msgFile) throws IOException {
        if (msgFile == null || !msgFile.exists()) {
            throw new FileNotFoundException("File MSG non trovato: " + msgFile);
        }

        logger.info("Inizio conversione MSG to PDF con Chrome Headless: {}", msgFile.getName());

        tempDir = new File(msgFile.getParent());

        try {
            MAPIMessage message = parseMsgMessage(msgFile);
            extractEmbeddedImages(message);
            File htmlFile = generateCompleteHtml(message);

            // Debug: apri HTML nel browser se richiesto
            if (DEBUG_OPEN_HTML_IN_BROWSER) {
                openHtmlInBrowser(htmlFile, ChromeManager.getInstance().getChromePath());
            }

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

                        //Usa Files.move() per sostituzione atomica
                        try {
                            Files.move(tempFile.toPath(), pdfFile.toPath(),
                                    StandardCopyOption.REPLACE_EXISTING);
                            logger.info("File watermarkato sostituito correttamente");
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
            logger.error("Errore durante la conversione MSG", e);
            throw new IOException("Errore durante la conversione MSG: " + e.getMessage(), e);
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
     * Effettua il parsing del file MSG utilizzando Apache POI HSMF.
     *
     * @param msgFile Il file MSG da analizzare
     * @return L'oggetto MAPIMessage contenente i dati parsati dell'email
     * @throws IOException Se si verificano errori durante il parsing
     */
    private MAPIMessage parseMsgMessage(File msgFile) throws IOException {
        try {
            return new MAPIMessage(msgFile.getAbsolutePath());
        } catch (Exception e) {
            throw new IOException("Errore nel parsing del file MSG: " + e.getMessage(), e);
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

        File pdfFile = new File(tempDir, originalBaseName + ".pdf");

        List<String> command = new ArrayList<String>();
        command.add(chromePath);
        command.add("--headless");
        command.add("--no-sandbox");
        command.add("--disable-gpu");
        command.add("--disable-dev-shm-usage");
        command.add("--disable-extensions");
        command.add("--disable-plugins");
        command.add("--run-all-compositor-stages-before-draw");
        command.add("--virtual-time-budget=10000");
        command.add("--print-to-pdf=" + pdfFile.getAbsolutePath());
        command.add("--print-to-pdf-no-header");
        command.add("file://" + htmlFile.getAbsolutePath().replace("\\", "/"));

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
     * Genera il file HTML completo dal messaggio MSG parsato.
     * Include CSS ottimizzato per stampa, gestione responsive e
     * il contenuto dell'email processato per la conversione PDF.
     *
     * @param message L'oggetto MAPIMessage contenente i dati dell'email
     * @return Il file HTML generato e pronto per la conversione
     * @throws IOException Se si verificano errori durante la generazione
     */
    private File generateCompleteHtml(MAPIMessage message) throws IOException {
        StringBuilder html = new StringBuilder();

        // Header HTML identico al convertitore EML
        html.append("<!DOCTYPE html>\n");
        html.append("<html>\n");
        html.append("<head>\n");
        html.append("    <meta charset=\"UTF-8\">\n");
        html.append("    <meta name=\"viewport\" content=\"width=device-width, initial-scale=1\">\n");
        html.append("    <title>Email PDF</title>\n");
        html.append("    <style>\n");
        html.append("        @page {\n");
        html.append("            size: A4;\n");
        html.append("            margin: 0;\n");
        html.append("        }\n");
        html.append("        \n");
        html.append("        html, body {\n");
        html.append("            margin: 0;\n");
        html.append("            padding: 0;\n");
        html.append("            height: 100%;\n");
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
        html.append("        @media print {\n");
        html.append("            body::before, body::after {\n");
        html.append("                content: none !important;\n");
        html.append("                display: none !important;\n");
        html.append("            }\n");
        html.append("            @page {\n");
        html.append("                margin: 0 !important;\n");
        html.append("            }\n");
        html.append("        }\n");
        html.append("    </style>\n");
        html.append("</head>\n");
        html.append("<body>\n");

        // Contenuto email senza header (come nel convertitore EML)
        html.append("<div class=\"email-content\">");
        appendMsgContent(html, message);
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
     * Estrae e formatta il contenuto del messaggio MSG nell'HTML.
     * Prova in ordine: HTML body, RTF body convertito, text body.
     * Gestisce automaticamente i diversi formati di contenuto supportati da Outlook.
     *
     * @param html Il StringBuilder dove aggiungere l'HTML del contenuto
     * @param message L'oggetto MAPIMessage da processare
     * @throws IOException Se si verificano errori durante l'estrazione del contenuto
     */
    private void appendMsgContent(StringBuilder html, MAPIMessage message) throws IOException {
        try {
            // Prova prima HTML body
            String htmlBody = message.getHtmlBody();
            if (htmlBody != null && !htmlBody.trim().isEmpty()) {
                String processedHtml = processCidReferences(htmlBody);
                html.append(processedHtml);
                logger.info("Usato HTML body del MSG");
                return;
            }
        } catch (ChunkNotFoundException e) {
            logger.debug("HTML body non trovato nel MSG, provo RTF body");
        }

        try {
            // Fallback su RTF body convertito in testo
            String rtfBody = message.getRtfBody();
            if (rtfBody != null && !rtfBody.trim().isEmpty()) {
                // Conversione molto semplice RTF -> HTML
                String convertedHtml = convertRtfToHtml(rtfBody);
                html.append(convertedHtml);
                logger.info("Usato RTF body del MSG convertito");
                return;
            }
        } catch (ChunkNotFoundException e) {
            logger.debug("RTF body non trovato nel MSG, provo text body");
        }

        try {
            // Fallback finale su text body
            String textBody = message.getTextBody();
            if (textBody != null && !textBody.trim().isEmpty()) {
                html.append("<pre style=\"white-space: pre-wrap; font-family: inherit;\">")
                        .append(escapeHtml(textBody))
                        .append("</pre>");
                logger.info("Usato text body del MSG");
                return;
            }
        } catch (ChunkNotFoundException e) {
            logger.warn("Nessun body trovato nel MSG");
        }

        // Se non c'è contenuto
        html.append("<p><em>Nessun contenuto disponibile</em></p>");
    }

    /**
     * Estrae le immagini embedded dagli allegati del messaggio MSG.
     * Analizza tutti gli allegati, identifica quelli con MIME type image/*
     * e li salva come file temporanei per il riferimento nell'HTML.
     *
     * @param message L'oggetto MAPIMessage da cui estrarre le immagini
     * @throws IOException Se si verificano errori durante l'estrazione
     * @throws ChunkNotFoundException Se i chunks degli allegati non sono trovati
     */
    private void extractEmbeddedImages(MAPIMessage message) throws IOException, ChunkNotFoundException {
        AttachmentChunks[] attachments = message.getAttachmentFiles();
        if (attachments == null) return;

        for (int i = 0; i < attachments.length; i++) {
            AttachmentChunks attachment = attachments[i];

            try {
                // Controlla se è un'immagine embedded
                String fileName = getAttachmentFileName(attachment);
                String mimeType = getAttachmentMimeType(attachment);

                if (mimeType != null && mimeType.startsWith("image/")) {
                    byte[] attachmentData = attachment.getEmbeddedAttachmentObject();
                    if (attachmentData != null) {
                        saveEmbeddedImageFromAttachment(attachmentData, fileName, mimeType, i);
                    }
                }
            } catch (Exception e) {
                logger.warn("Errore nell'estrazione allegato {}: {}", i, e.getMessage());
            }
        }
    }

    /**
     * Estrae il nome del file da un allegato MSG.
     * Prova prima il nome lungo, poi il nome corto come fallback.
     *
     * @param attachment L'oggetto AttachmentChunks da cui estrarre il nome
     * @return Il nome del file, o "attachment" come default
     */
    private String getAttachmentFileName(AttachmentChunks attachment) {
        try {
            if (attachment.getAttachLongFileName() != null) {
                return attachment.getAttachLongFileName().toString();
            }
            if (attachment.getAttachFileName() != null) {
                return attachment.getAttachFileName().toString();
            }
        } catch (Exception e) {
            logger.debug("Errore nel recupero nome allegato: {}", e.getMessage());
        }
        return "attachment";
    }

    /**
     * Estrae il MIME type da un allegato MSG.
     *
     * @param attachment L'oggetto AttachmentChunks da cui estrarre il MIME type
     * @return Il MIME type dell'allegato, o null se non disponibile
     */
    private String getAttachmentMimeType(AttachmentChunks attachment) {
        try {
            if (attachment.getAttachMimeTag() != null) {
                return attachment.getAttachMimeTag().toString();
            }
        } catch (Exception e) {
            logger.debug("Errore nel recupero MIME type allegato: {}", e.getMessage());
        }
        return null;
    }

    /**
     * Salva un'immagine embedded da un allegato MSG.
     * Crea un file temporaneo con nome sicuro e mappa i vari formati
     * di Content-ID per i riferimenti HTML.
     *
     * @param imageData I dati binari dell'immagine
     * @param fileName Il nome originale del file allegato
     * @param mimeType Il MIME type dell'immagine
     * @param index L'indice dell'allegato per creare nomi univoci
     * @throws IOException Se si verificano errori durante il salvataggio
     */
    private void saveEmbeddedImageFromAttachment(byte[] imageData, String fileName, String mimeType, int index) throws IOException {
        String extension = getImageExtension(mimeType);
        String safeFileName = "embedded_" + index + "." + extension;

        File imageFile = new File(tempDir, safeFileName);
        try (FileOutputStream fos = new FileOutputStream(imageFile)) {
            fos.write(imageData);
        }

        // Mappa per riferimenti CID (usa diversi formati possibili)
        String contentId = fileName.replaceAll("[^a-zA-Z0-9._-]", "_");
        embeddedImages.put(contentId, imageFile.getAbsolutePath());
        embeddedImages.put("cid:" + contentId, imageFile.getAbsolutePath());
        embeddedImages.put("<" + contentId + ">", imageFile.getAbsolutePath());

        logger.info("Immagine MSG salvata: {} -> {}", fileName, safeFileName);
    }

    /**
     * Converte contenuto RTF in HTML usando un approccio semplificato.
     * Rimuove i comandi RTF base e mantiene il testo leggibile,
     * convertendo i line break in tag HTML appropriati.
     *
     * @param rtfBody Il contenuto RTF da convertire
     * @return L'HTML generato dal contenuto RTF
     */
    private String convertRtfToHtml(String rtfBody) {
        // Conversione molto semplice RTF -> HTML
        // Rimuove i comandi RTF base e mantiene il testo
        String text = rtfBody;

        // Rimuove header RTF
        text = text.replaceAll("\\{\\\\rtf1[^}]*\\}", "");

        // Rimuove comandi di formattazione RTF
        text = text.replaceAll("\\\\[a-z]+\\d*", "");
        text = text.replaceAll("\\{|\\}", "");

        // Pulisce spazi multipli
        text = text.replaceAll("\\s+", " ").trim();

        // Converte newline in <br>
        text = text.replace("\n", "<br>");

        return "<div>" + escapeHtml(text) + "</div>";
    }

    /**
     * Processa i riferimenti CID nell'HTML sostituendoli con percorsi file locali.
     * Gestisce diversi formati di riferimento CID utilizzati da Outlook.
     *
     * @param htmlContent Il contenuto HTML da processare
     * @return L'HTML con i riferimenti CID sostituiti con percorsi locali
     */
    private String processCidReferences(String htmlContent) {
        String processed = htmlContent;

        for (Map.Entry<String, String> entry : embeddedImages.entrySet()) {
            String contentId = entry.getKey();
            String localPath = "file:///" + entry.getValue().replace("\\", "/");

            // Sostituisci vari formati di riferimento
            processed = processed.replaceAll(
                    "src=[\"']cid:" + contentId + "[\"']",
                    "src=\"" + localPath + "\""
            );
            processed = processed.replaceAll(
                    "src=[\"']" + contentId + "[\"']",
                    "src=\"" + localPath + "\""
            );
        }

        return processed;
    }

    /**
     * Converte un tipo MIME in estensione file appropriata per le immagini.
     *
     * @param mimeType Il tipo MIME da convertire
     * @return L'estensione file corrispondente, o "img" come default
     */
    private String getImageExtension(String mimeType) {
        if (mimeType == null) return "img";
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
     * Effettua l'escape dei caratteri HTML speciali per evitare problemi di rendering.
     *
     * @param text Il testo da processare
     * @return Il testo con i caratteri HTML escapati, o stringa vuota se null
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