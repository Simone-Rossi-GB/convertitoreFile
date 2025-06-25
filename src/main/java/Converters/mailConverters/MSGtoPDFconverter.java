package converters.mailConverters;

import com.itextpdf.text.*;
import com.itextpdf.text.pdf.PdfWriter;
import com.itextpdf.tool.xml.XMLWorkerHelper;
import objects.Log;
import converters.Converter;
import org.apache.poi.hsmf.MAPIMessage;
import org.apache.poi.hsmf.datatypes.AttachmentChunks;
import org.apache.poi.hsmf.exceptions.ChunkNotFoundException;
import org.jsoup.Jsoup;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.util.Calendar;

import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.LogManager;

/**
 * Converte un file email .msg (formato Outlook) in un documento PDF.
 * Include intestazioni, corpo email (HTML o testo semplice) e una lista degli allegati.
 * Utilizza Apache POI HSMF per l'elaborazione del file MSG, iText per la generazione del PDF e JSoup per la pulizia HTML.
 */
public class MSGtoPDFconverter extends Converter {

    private static final Logger logger = LogManager.getLogger(MSGtoPDFconverter.class);
    private static final Font HEADER_FONT = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 12);
    private static final Font CONTENT_FONT = FontFactory.getFont(FontFactory.HELVETICA, 10);

    /**
     * Converte un file .msg in un file PDF con intestazioni, corpo e allegati.
     *
     * @param msgFile File .msg da convertire
     * @return ArrayList contenente un solo file PDF generato
     * @throws IOException in caso di problemi I/O
     * @throws DocumentException in caso di problemi nella generazione del PDF
     * @throws NullPointerException se uno degli oggetti critici è null
     */
    @Override
    public File convert(File msgFile) throws IOException, DocumentException {
        if (msgFile == null) {
            logger.error("File MSG nullo");
            throw new NullPointerException("L'oggetto msgFile non esiste.");
        }

        if (!msgFile.exists()) {
            logger.error("File MSG non trovato: {}", msgFile.getAbsolutePath());
            throw new FileNotFoundException("File MSG non trovato: " + msgFile);
        }

        logger.info("Inizio conversione con parametri: \n | msgFile.getPath() = {}", msgFile.getPath());

        File outputDir = new File(System.getProperty("java.io.tmpdir"), "msg_to_pdf");
        if (!outputDir.exists() && !outputDir.mkdirs()) {
            logger.error("Impossibile creare directory output: {}", outputDir.getAbsolutePath());
            throw new IOException("Impossibile creare la directory di output: " + outputDir.getAbsolutePath());
        }

        String baseName = msgFile.getName().replaceFirst("[.][^.]+$", "");
        File outputPdfFile = new File(outputDir, baseName + ".pdf");

        MAPIMessage msg = new MAPIMessage(msgFile.getAbsolutePath());

        Document document = new Document();
        PdfWriter writer = PdfWriter.getInstance(document, new FileOutputStream(outputPdfFile));

        try {
            document.open();
            addMsgHeadersToPdf(msg, document);
            document.add(new Paragraph("\n"));
            addMsgBodyToPdf(msg, document, writer);
        } finally {
            if (document.isOpen()) document.close();
            writer.close();
            try {
                msg.close();
            } catch (IOException e) {
                logger.warn("Chiusura MAPIMessage fallita: {}", e.getMessage());
            }
        }

        if (!outputPdfFile.exists()) {
            logger.error("Creazione del file PDF fallita: {}", outputPdfFile.getAbsolutePath());
            throw new IOException("Errore nella creazione del file PDF: " + outputPdfFile.getAbsolutePath());
        }

        logger.info("Conversione completata con successo: {}", outputPdfFile.getName());

        return outputPdfFile;
    }

    /**
     * Aggiunge le intestazioni dell'email al documento PDF.
     *
     * @param msg      MAPIMessage da cui estrarre le intestazioni
     * @param document Documento PDF di destinazione
     * @throws DocumentException in caso di errore PDF
     * @throws IOException in caso di problemi I/O
     * @throws NullPointerException se uno degli oggetti è null
     */
    private static void addMsgHeadersToPdf(MAPIMessage msg, Document document) throws DocumentException, IOException {
        if (msg == null) throw new NullPointerException("L'oggetto msg non esiste.");
        if (document == null) throw new NullPointerException("L'oggetto document non esiste.");

        document.add(new Paragraph("Email Headers:", HEADER_FONT));
        document.add(new Paragraph("----------------------------------------", HEADER_FONT));

        try { String from = msg.getDisplayFrom(); if (from != null && !from.trim().isEmpty()) document.add(new Paragraph("Da: " + from, CONTENT_FONT)); } catch (ChunkNotFoundException ignored) {}
        try { String to = msg.getDisplayTo(); if (to != null && !to.trim().isEmpty()) document.add(new Paragraph("A: " + to, CONTENT_FONT)); } catch (ChunkNotFoundException ignored) {}
        try { String cc = msg.getDisplayCC(); if (cc != null && !cc.trim().isEmpty()) document.add(new Paragraph("Cc: " + cc, CONTENT_FONT)); } catch (ChunkNotFoundException ignored) {}
        try { String bcc = msg.getDisplayBCC(); if (bcc != null && !bcc.trim().isEmpty()) document.add(new Paragraph("Bcc: " + bcc, CONTENT_FONT)); } catch (ChunkNotFoundException ignored) {}
        try { String subject = msg.getSubject(); if (subject != null && !subject.trim().isEmpty()) document.add(new Paragraph("Oggetto: " + subject, CONTENT_FONT)); } catch (ChunkNotFoundException ignored) {}
        try { Calendar cal = msg.getMessageDate(); if (cal != null) document.add(new Paragraph("Data: " + cal.getTime(), CONTENT_FONT)); } catch (ChunkNotFoundException ignored) {}

        document.add(new Paragraph("----------------------------------------\n", HEADER_FONT));
    }

    /**
     * Aggiunge il corpo del messaggio (HTML o testo semplice) e gli allegati al PDF.
     *
     * @param msg      MAPIMessage da cui estrarre il corpo
     * @param document Documento PDF di destinazione
     * @param writer   PdfWriter per la scrittura XHTML
     * @throws DocumentException in caso di errore PDF
     * @throws IOException in caso di problemi I/O
     * @throws NullPointerException se uno degli oggetti è null
     */
    private static void addMsgBodyToPdf(MAPIMessage msg, Document document, PdfWriter writer) throws DocumentException, IOException {
        if (msg == null) throw new NullPointerException("L'oggetto msg non esiste.");
        if (document == null) throw new NullPointerException("L'oggetto document non esiste.");
        if (writer == null) throw new NullPointerException("L'oggetto writer non esiste.");

        String htmlBody = null;
        String textBody = null;

        try { htmlBody = msg.getHtmlBody(); } catch (ChunkNotFoundException ignored) {}

        if (htmlBody != null && !htmlBody.trim().isEmpty()) {
            try {
                org.jsoup.nodes.Document jsoupDoc = Jsoup.parse(htmlBody);
                jsoupDoc.outputSettings().syntax(org.jsoup.nodes.Document.OutputSettings.Syntax.xml);
                jsoupDoc.outputSettings().charset(StandardCharsets.UTF_8);
                jsoupDoc.outputSettings().escapeMode(org.jsoup.nodes.Entities.EscapeMode.xhtml);

                String xhtml = jsoupDoc.html();
                XMLWorkerHelper.getInstance().parseXHtml(writer, document,
                        new ByteArrayInputStream(xhtml.getBytes(StandardCharsets.UTF_8)),
                        StandardCharsets.UTF_8);
            } catch (Exception e) {
                logger.warn("Errore nel parsing HTML: {}", e.getMessage());
                document.add(new Paragraph("Contenuto HTML (errore nel parsing):\n" + htmlBody, CONTENT_FONT));
            }
        } else {
            try { textBody = msg.getTextBody(); } catch (ChunkNotFoundException ignored) {}

            if (textBody != null && !textBody.trim().isEmpty()) {
                document.add(new Paragraph(textBody, CONTENT_FONT));
            } else {
                document.add(new Paragraph("Nessun contenuto del corpo disponibile per questa email.", CONTENT_FONT));
            }
        }

        try {
            AttachmentChunks[] attachments = msg.getAttachmentFiles();
            if (attachments != null && attachments.length > 0) {
                document.add(new Paragraph("\nAllegati:", HEADER_FONT));
                for (AttachmentChunks att : attachments) {
                    try {
                        String fileName = null;
                        if (att.getAttachLongFileName() != null) {
                            fileName = att.getAttachLongFileName().toString();
                        } else if (att.getAttachFileName() != null) {
                            fileName = att.getAttachFileName().toString();
                        }
                        if (fileName != null && !fileName.trim().isEmpty()) {
                            document.add(new Paragraph("- " + fileName, CONTENT_FONT));
                        }
                    } catch (Exception e) {
                        logger.warn("Errore nella lettura del nome dell'allegato: {}", e.getMessage());
                    }
                }
            }
        } catch (Exception e) {
            logger.warn("Errore nella lettura degli allegati: {}", e.getMessage());
        }
    }
}
