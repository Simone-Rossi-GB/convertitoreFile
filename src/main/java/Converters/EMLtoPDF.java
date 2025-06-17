package Converters;

import com.itextpdf.text.Document;
import com.itextpdf.text.DocumentException;
import com.itextpdf.text.Font;
import com.itextpdf.text.FontFactory;
import com.itextpdf.text.Paragraph;
import com.itextpdf.text.pdf.PdfWriter;
import com.itextpdf.tool.xml.XMLWorkerHelper;

import org.apache.james.mime4j.dom.Message;
import org.apache.james.mime4j.dom.Entity;
import org.apache.james.mime4j.dom.TextBody;
import org.apache.james.mime4j.dom.Multipart;
import org.apache.james.mime4j.dom.Body;
import org.apache.james.mime4j.dom.Header;
import org.apache.james.mime4j.dom.address.Mailbox;
import org.apache.james.mime4j.dom.field.ContentTypeField;
import org.apache.james.mime4j.dom.field.DateTimeField;
import org.apache.james.mime4j.dom.field.MailboxListField;
import org.apache.james.mime4j.message.DefaultMessageBuilder;

import org.jsoup.Jsoup;
import org.jsoup.safety.Safelist;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

public class EMLtoPDF implements Converter {

    private static final Font HEADER_FONT = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 12);
    private static final Font CONTENT_FONT = FontFactory.getFont(FontFactory.HELVETICA, 10);

    /**
     * Implementazione dell'interfaccia Converter
     * Converte un file EML in PDF e ritorna la lista dei file creati
     *
     * @param srcFile file EML di input
     * @return ArrayList contenente il file PDF creato
     * @throws IOException se ci sono problemi di I/O o conversione
     */
    @Override
    public ArrayList<File> convert(File srcFile) throws IOException {
        ArrayList<File> resultFiles = new ArrayList<>();

        try {
            // Genera il nome del file PDF di output
            String fileName = srcFile.getName();
            String baseName = fileName.substring(0, fileName.lastIndexOf('.'));
            String outputDir = srcFile.getParent(); // Stessa directory del file sorgente
            String outputPath = outputDir + File.separator + baseName + ".pdf";

            // Converte usando il metodo esistente
            File pdfFile = convertEmlToPdf(srcFile, outputPath);
            resultFiles.add(pdfFile);

        } catch (DocumentException e) {
            throw new IOException("Errore nella creazione del PDF: " + e.getMessage(), e);
        }

        return resultFiles;
    }

    /**
     * Converte un file EML in PDF (metodo legacy per compatibilità)
     * @param fileEML percorso del file EML di input
     * @param outputPdfPath percorso del file PDF di output
     * @return File oggetto che rappresenta il PDF creato
     * @throws IOException se ci sono problemi di I/O
     * @throws DocumentException se ci sono problemi nella creazione del PDF
     */
    public static File convertEmlToPdf(File fileEML, String outputPdfPath)
            throws IOException, DocumentException {

        File emlFile = fileEML;
        if (!emlFile.exists()) {
            throw new FileNotFoundException("File EML non trovato: " + emlFile.getName());
        }

        // Crea le directory parent se non esistono
        File outputPdfFile = new File(outputPdfPath);
        File parentDir = outputPdfFile.getParentFile();
        if (parentDir != null && !parentDir.exists()) {
            parentDir.mkdirs();
        }

        DefaultMessageBuilder builder = new DefaultMessageBuilder();
        Message mime4jMessage;
        try (InputStream is = new FileInputStream(emlFile)) {
            mime4jMessage = builder.parseMessage(is);
        }

        Document document = new Document();
        PdfWriter writer = null;
        try {
            writer = PdfWriter.getInstance(document, new FileOutputStream(outputPdfPath));
            document.open();

            addEmlHeadersToPdf(mime4jMessage, document);
            document.add(new Paragraph("\n"));

            processMime4jBody(mime4jMessage.getBody(), document, writer);

        } finally {
            if (document.isOpen()) {
                document.close();
            }
            if (writer != null) {
                writer.close();
            }
        }

        // Verifica che il file sia stato creato correttamente
        if (!outputPdfFile.exists()) {
            throw new IOException("Errore nella creazione del file PDF: " + outputPdfPath);
        }

        // Ritorna il file creato per permettere all'engine di spostarlo
        return outputPdfFile;
    }

    private static void addEmlHeadersToPdf(Message message, Document document) throws DocumentException {
        document.add(new Paragraph("Email Headers:", HEADER_FONT));
        document.add(new Paragraph("----------------------------------------", HEADER_FONT));

        /*
        A (To): I destinatari principali dell'email.
        Tutti i destinatari (in "A", "Cc" e "Bcc")
        possono vedere chi è nel campo "A".

        Cc (Carbon Copy): Destinatari che ricevono una copia dell'email per informazione.
        Anche in questo caso, tutti i destinatari (in "A", "Cc" e "Bcc") possono vedere chi è
        nel campo "Cc".

        Bcc (Blind Carbon Copy): Destinatari che ricevono una copia dell'email,
        ma i cui indirizzi sono nascosti a tutti gli altri destinatari dell'email.
        Solo il mittente può vedere chi è stato inserito nel campo "Bcc".

        */

        addFieldToPdf(document, "Da: ", message.getFrom());
        addFieldToPdf(document, "A: ", message.getTo());
        addFieldToPdf(document, "Cc: ", message.getCc());
        addFieldToPdf(document, "Bcc: ", message.getBcc());
        addFieldToPdf(document, "Oggetto: ", message.getSubject());

        DateTimeField dateTimeField = (DateTimeField) message.getHeader().getField("Date");
        if (dateTimeField != null) {
            SimpleDateFormat sdf = new SimpleDateFormat("dd/MM/yyyy HH:mm:ss");
            addFieldToPdf(document, "Date", sdf.format(dateTimeField.getDate()));
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

    private static void addFieldToPdf(Document document, String fieldName, Object fieldValue) throws DocumentException {
        if (fieldValue != null) {
            String valueString = "";
            if (fieldValue instanceof List) {
                // Handle List<Mailbox> (from getTo(), getCc(), getBcc())
                /*
                @SuppressWarnings("unchecked") è un modo per dire:
                "Lo so che questo cast potrebbe essere pericoloso,
                ma nel mio caso specifico è sicuro,
                quindi non mostrarmi il warning."
                 */
                @SuppressWarnings("unchecked")
                List<Mailbox> mailboxList = (List<Mailbox>) fieldValue;
                StringBuilder sb = new StringBuilder();
                for (Mailbox mailbox : mailboxList) {
                    if (sb.length() > 0) {
                        sb.append("; ");
                    }
                    sb.append(mailbox.getAddress());
                }
                valueString = sb.toString();
            } else if (fieldValue instanceof Mailbox) {
                // Handle single Mailbox (from getFrom())
                valueString = ((Mailbox) fieldValue).getAddress();
            } else if (fieldValue instanceof String) {
                valueString = (String) fieldValue;
            } else {
                valueString = fieldValue.toString();
            }

            if (!valueString.isEmpty()) {
                document.add(new Paragraph(fieldName + ": " + valueString, CONTENT_FONT));
            }
        }
    }

    private static void processMime4jBody(Body body, Document document, PdfWriter writer) throws IOException, DocumentException {
        if (body instanceof TextBody) {
            TextBody textBody = (TextBody) body;
            String mimeType = getContentTypeFromTextBody(textBody);
            String content = readTextBodyContent(textBody);

            if (mimeType.contains("text/html")) {
                org.jsoup.nodes.Document jsoupDoc = Jsoup.parse(content);
                jsoupDoc.outputSettings().syntax(org.jsoup.nodes.Document.OutputSettings.Syntax.xml); // XHTML
                jsoupDoc.outputSettings().charset(StandardCharsets.UTF_8);
                jsoupDoc.outputSettings().escapeMode(org.jsoup.nodes.Entities.EscapeMode.xhtml);

                // Ora il contenuto è XHTML valido per iText
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

    private static String getContentTypeFromTextBody(TextBody textBody) {
        ContentTypeField contentTypeField = (ContentTypeField) textBody.getParent().getHeader().getField("Content-Type");
        return contentTypeField != null ? contentTypeField.getMimeType() : "text/plain";
    }

    private static String readTextBodyContent(TextBody textBody) throws IOException {
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