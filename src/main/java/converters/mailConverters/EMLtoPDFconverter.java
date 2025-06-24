package converters.mailConverters;

import com.itextpdf.text.*;
import com.itextpdf.text.pdf.PdfWriter;
import com.itextpdf.tool.xml.XMLWorkerHelper;
import objects.Log;
import converters.Converter;
import org.apache.james.mime4j.dom.*;
import org.apache.james.mime4j.dom.Header;
import org.apache.james.mime4j.dom.address.Mailbox;
import org.apache.james.mime4j.dom.field.ContentTypeField;
import org.apache.james.mime4j.dom.field.DateTimeField;
import org.apache.james.mime4j.message.DefaultMessageBuilder;
import org.jsoup.Jsoup;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.text.SimpleDateFormat;

import java.util.List;
import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.LogManager;


/**
 * Questa classe si occupa della conversione di email in formato .eml in documenti PDF.
 * Estrae intestazioni, corpo (HTML o testo) e li organizza in un file PDF.
 */
public class EMLtoPDFconverter extends Converter {

    private static final Logger logger = LogManager.getLogger(EMLtoPDFconverter.class);
    private static final Font HEADER_FONT = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 12);
    private static final Font CONTENT_FONT = FontFactory.getFont(FontFactory.HELVETICA, 10);

    /**
     * Converte un file .eml in PDF.
     *
     * @param emlFile File sorgente in formato .eml
     * @return ArrayList contenente il file PDF generato
     * @throws IOException         in caso di errori I/O
     * @throws DocumentException   in caso di errori nella generazione PDF
     * @throws NullPointerException se uno degli oggetti principali è null
     */
    @Override
    public File convert(File emlFile) throws IOException, DocumentException {
        if (emlFile == null) throw new NullPointerException("L'oggetto emlFile non esiste.");
        logger.info("Inizio conversione con parametri: \n | emlFile.getPath() = {}", emlFile.getPath());
        Log.addMessage("Inizio conversione eml: " + emlFile.getName() + " -> .pdf");

        if (!emlFile.exists()) {
            logger.error("File EML non trovato: {}", emlFile);
            Log.addMessage("ERRORE: File EML non trovato " + emlFile);
            throw new FileNotFoundException("File EML non trovato: " + emlFile);
        }

        File outputDir = new File("src/temp");
        if (!outputDir.exists() && !outputDir.mkdirs()) {
            logger.error("Impossibile creare la directory di output: {}", outputDir.getAbsolutePath());
            throw new IOException("Impossibile creare la directory di output: " + outputDir.getAbsolutePath());
        }

        String baseName = emlFile.getName().replaceFirst("[.][^.]+$", "");
        File outputPdfFile = new File(outputDir, baseName + ".pdf");

        DefaultMessageBuilder builder = new DefaultMessageBuilder();
        Message mime4jMessage;
        try (InputStream is = Files.newInputStream(emlFile.toPath())) {
            mime4jMessage = builder.parseMessage(is);
        }

        if (mime4jMessage == null){
            logger.error("L'oggetto Message non esiste.");
            throw new NullPointerException("L'oggetto Message non esiste.");
        }

        Document document = new Document();
        PdfWriter writer = PdfWriter.getInstance(document, new FileOutputStream(outputPdfFile));

        try {
            document.open();
            addEmlHeadersToPdf(mime4jMessage, document);
            document.add(new Paragraph("\n"));
            processMime4jBody(mime4jMessage.getBody(), document, writer);
        } finally {
            if (document.isOpen()) document.close();
            writer.close();
        }

        if (!outputPdfFile.exists()) {
            logger.error("ERRORE: creazione del file PDF fallita: {}", outputPdfFile.getAbsolutePath());
            Log.addMessage("ERRORE: creazione del file PDF fallita: " + outputPdfFile.getAbsolutePath());
            throw new IOException("Errore nella creazione del file PDF: " + outputPdfFile.getAbsolutePath());
        }

        logger.info("Creazione file .pdf completata: {}", outputPdfFile.getName());
        Log.addMessage("Creazione file .pdf completata: " + outputPdfFile.getName());
        return outputPdfFile;
    }

    /**
     * Aggiunge le intestazioni dell'email al documento PDF.
     *
     * @param message  Messaggio MIME4J da cui estrarre i metadati
     * @param document Documento PDF di destinazione
     * @throws DocumentException se si verifica un errore nella scrittura PDF
     */
    private static void addEmlHeadersToPdf(Message message, Document document) throws DocumentException {
        if (message == null) {
            logger.error("L'oggetto message non esiste.");
            throw new NullPointerException("L'oggetto message non esiste.");
        }
        if (document == null) {
            logger.error("L'oggetto document non esiste.");
            throw new NullPointerException("L'oggetto document non esiste.");
        }

        document.add(new Paragraph("Email Headers:", HEADER_FONT));
        document.add(new Paragraph("----------------------------------------", HEADER_FONT));

        addFieldToPdf(document, "Da", message.getFrom());
        addFieldToPdf(document, "A", message.getTo());
        addFieldToPdf(document, "Cc", message.getCc());
        addFieldToPdf(document, "Bcc", message.getBcc());
        addFieldToPdf(document, "Oggetto", message.getSubject());

        DateTimeField dateTimeField = (DateTimeField) message.getHeader().getField("Date");
        if (dateTimeField != null) {
            SimpleDateFormat sdf = new SimpleDateFormat("dd/MM/yyyy HH:mm:ss");
            addFieldToPdf(document, "Data", sdf.format(dateTimeField.getDate()));
        }

        Header header = message.getHeader();
        if (header != null) {
            String messageId = header.getField("Message-ID") != null ? header.getField("Message-ID").getBody() : null;
            if (messageId != null) {
                addFieldToPdf(document, "Message-ID", messageId);
            }
        }

        document.add(new Paragraph("----------------------------------------\n", HEADER_FONT));
    }

    /**
     * Aggiunge un campo (mittente, destinatari, oggetto, ecc.) al PDF in modo formattato.
     *
     * @param document   Il documento PDF
     * @param fieldName  Nome del campo
     * @param fieldValue Valore del campo (può essere Mailbox, List<Mailbox> o String)
     * @throws DocumentException in caso di errore
     */
    private static void addFieldToPdf(Document document, String fieldName, Object fieldValue) throws DocumentException {
        if (document == null) {
            logger.error("L'oggetto document non esiste.");
            throw new NullPointerException("L'oggetto document non esiste.");
        }
        if (fieldValue == null) return;

        String valueString = "";
        if (fieldValue instanceof List) {
            @SuppressWarnings("unchecked")
            List<Mailbox> mailboxList = (List<Mailbox>) fieldValue;
            StringBuilder sb = new StringBuilder();
            for (Mailbox mailbox : mailboxList) {
                if (sb.length() > 0) sb.append("; ");
                sb.append(mailbox.getAddress());
            }
            valueString = sb.toString();
        } else if (fieldValue instanceof Mailbox) {
            valueString = ((Mailbox) fieldValue).getAddress();
        } else {
            valueString = fieldValue.toString();
        }

        if (!valueString.isEmpty()) {
            document.add(new Paragraph(fieldName + ": " + valueString, CONTENT_FONT));
        }
    }

    /**
     * Elabora il corpo dell'email, gestendo HTML, testo semplice e multipart.
     *
     * @param body     Corpo MIME dell'email
     * @param document Documento PDF
     * @param writer   Writer PDF per l'HTML
     * @throws IOException        se si verifica un errore di I/O
     * @throws DocumentException se si verifica un errore nella scrittura PDF
     */
    private static void processMime4jBody(Body body, Document document, PdfWriter writer) throws IOException, DocumentException {
        if (body == null) {
            logger.error("L'oggetto body non esiste.");
            throw new NullPointerException("L'oggetto body non esiste.");
        }
        if (document == null) {
            logger.error("L'oggetto document non esiste.");
            throw new NullPointerException("L'oggetto document non esiste.");
        }
        if (writer == null) {
            logger.error("L'oggetto writer non esiste.");
            throw new NullPointerException("L'oggetto writer non esiste.");
        }

        if (body instanceof TextBody) {
            TextBody textBody = (TextBody) body;
            String mimeType = getContentTypeFromTextBody(textBody);
            String content = readTextBodyContent(textBody);

            if (mimeType.contains("text/html")) {
                org.jsoup.nodes.Document jsoupDoc = Jsoup.parse(content);
                jsoupDoc.outputSettings().syntax(org.jsoup.nodes.Document.OutputSettings.Syntax.xml);
                jsoupDoc.outputSettings().charset(StandardCharsets.UTF_8);
                jsoupDoc.outputSettings().escapeMode(org.jsoup.nodes.Entities.EscapeMode.xhtml);

                String xhtmlContent = jsoupDoc.html();
                XMLWorkerHelper.getInstance().parseXHtml(writer, document,
                        new ByteArrayInputStream(xhtmlContent.getBytes(StandardCharsets.UTF_8)),
                        StandardCharsets.UTF_8);
            } else {
                document.add(new Paragraph(content, CONTENT_FONT));
            }
        } else if (body instanceof Multipart) {
            Multipart multipart = (Multipart) body;
            for (Entity part : multipart.getBodyParts()) {
                processMime4jBody(part.getBody(), document, writer);
            }
        }
    }

    /**
     * Recupera il tipo MIME da un TextBody.
     *
     * @param textBody Il corpo testuale
     * @return MimeType (es. "text/plain", "text/html")
     */
    private static String getContentTypeFromTextBody(TextBody textBody) {
        if (textBody == null) throw new NullPointerException("L'oggetto textBody non esiste.");

        ContentTypeField contentTypeField = (ContentTypeField) textBody.getParent().getHeader().getField("Content-Type");
        return contentTypeField != null ? contentTypeField.getMimeType() : "text/plain";
    }

    /**
     * Legge il contenuto testuale da un TextBody.
     *
     * @param textBody Corpo di testo
     * @return Stringa contenente il testo
     * @throws IOException in caso di problemi di lettura
     */
    private static String readTextBodyContent(TextBody textBody) throws IOException {
        if (textBody == null) throw new NullPointerException("L'oggetto textBody non esiste.");

        try (InputStreamReader reader = new InputStreamReader(textBody.getInputStream(), StandardCharsets.UTF_8)) {
            StringBuilder sb = new StringBuilder();
            char[] buffer = new char[4096];
            int bytesRead;
            while ((bytesRead = reader.read(buffer)) != -1) {
                sb.append(buffer, 0, bytesRead);
            }
            return sb.toString();
        }
    }
}