package Converters;

import com.itextpdf.text.Document;
import com.itextpdf.text.DocumentException;
import com.itextpdf.text.Font;
import com.itextpdf.text.FontFactory;
import com.itextpdf.text.Paragraph;
import com.itextpdf.text.pdf.PdfWriter;
import com.itextpdf.tool.xml.XMLWorkerHelper;

import org.apache.poi.hsmf.MAPIMessage;
import org.apache.poi.hsmf.datatypes.Chunk;
import org.apache.poi.hsmf.datatypes.MAPIProperty;
import org.apache.poi.hsmf.datatypes.RecipientChunks;
import org.apache.poi.hsmf.exceptions.ChunkNotFoundException;

import org.jsoup.Jsoup;
import org.jsoup.safety.Safelist;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.util.Calendar;
import java.util.Date;

public class MSGtoPDF {

    private static final Font HEADER_FONT = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 12);
    private static final Font CONTENT_FONT = FontFactory.getFont(FontFactory.HELVETICA, 10);

    /**
     * Converte un file MSG di Outlook in un file PDF.
     * Estrae intestazioni e corpo (testo semplice o HTML) e li formatta nel PDF.
     *
     * @param msgFilePath   Il percorso completo del file MSG di input.
     * @param outputPdfPath Il percorso completo dove salvare il file PDF di output.
     * @throws IOException      Se si verifica un errore di lettura/scrittura del file.
     * @throws DocumentException Se si verifica un errore durante la creazione del PDF con iText.
     */
    public static void convert(String msgFilePath, String outputPdfPath)
            throws IOException, DocumentException {

        File msgFile = new File(msgFilePath);
        if (!msgFile.exists()) {
            throw new FileNotFoundException("File MSG non trovato: " + msgFilePath);
        }

        MAPIMessage msg = new MAPIMessage(msgFile.getAbsolutePath());

        // 1. Creazione del documento PDF
        Document document = new Document();
        PdfWriter writer = null;
        try {
            writer = PdfWriter.getInstance(document, new FileOutputStream(outputPdfPath));
            document.open();

            // 2. Aggiungi intestazioni dell'email al PDF
            addMsgHeadersToPdf(msg, document);
            document.add(new Paragraph("\n")); // Spazio dopo le intestazioni

            // 3. Processa il corpo dell'email
            addMsgBodyToPdf(msg, document, writer);

        } finally {
            if (document.isOpen()) {
                document.close();
            }
            if (writer != null) {
                writer.close();
            }
        }
    }

    private static void addMsgHeadersToPdf(MAPIMessage msg, Document document) throws DocumentException, IOException {
        document.add(new Paragraph("Email Headers:", HEADER_FONT));
        document.add(new Paragraph("----------------------------------------", HEADER_FONT));

        // Mittente
        try {
            document.add(new Paragraph("Da: " + msg.getDisplayFrom(), CONTENT_FONT));
        } catch (ChunkNotFoundException e) {
            // Ignora se non trovato
        }

        // Destinatario
        try {
            document.add(new Paragraph("A: " + msg.getDisplayTo(), CONTENT_FONT));
        } catch (ChunkNotFoundException e) {
            // Ignora se non trovato
        }

        // Cc
        /*
        Destinatari che ricevono una copia dell'email per informazione. Anche in questo caso,
        tutti i destinatari (in "A", "Cc" e "Bcc") possono vedere chi è nel campo "Cc".
         */
        try {
            document.add(new Paragraph("Cc: " + msg.getDisplayCC(), CONTENT_FONT));
        } catch (ChunkNotFoundException e) {
            // Ignora se non trovato
        }

        // Bcc
        /*
        Serve per inviare una copia di un'email a uno o più destinatari,
        ma in modo che gli altri destinatari (quelli nei campi "A" e "Cc")
        non possano vedere che queste persone hanno ricevuto il messaggio.
         */

        try {
            document.add(new Paragraph("Bcc: " + msg.getDisplayBCC(), CONTENT_FONT));
        } catch (ChunkNotFoundException e) {
            // Ignora se non trovato
        }

        // Oggetto
        try {
            document.add(new Paragraph("Oggetto: " + msg.getSubject(), CONTENT_FONT));
        } catch (ChunkNotFoundException e) {
            // Ignora se non trovato
        }

        // Date
        try {
            Calendar messageCalendar = msg.getMessageDate();
            if (messageCalendar != null) {
                Date messageDate = messageCalendar.getTime();
                document.add(new Paragraph("Data: " + messageDate.toString(), CONTENT_FONT));
            }
        } catch (ChunkNotFoundException e) {
            // Ignora se non trovato
        }

        document.add(new Paragraph("----------------------------------------\n", HEADER_FONT));
    }

    private static void addMsgBodyToPdf(MAPIMessage msg, Document document, PdfWriter writer) throws DocumentException, IOException {
        String htmlBody = null;
        String textBody = null;

        try {
            htmlBody = msg.getHtmlBody();
        } catch (ChunkNotFoundException e) {
            // Nessun corpo HTML
        }

        if (htmlBody != null && !htmlBody.trim().isEmpty()) {
            String cleanHtml = Jsoup.clean(htmlBody, Safelist.relaxed());
            XMLWorkerHelper.getInstance().parseXHtml(writer, document,
                    new ByteArrayInputStream(cleanHtml.getBytes(StandardCharsets.UTF_8)));
        } else {
            try {
                textBody = msg.getTextBody();
            } catch (ChunkNotFoundException e) {
                // Nessun corpo di testo
            }

            if (textBody != null && !textBody.trim().isEmpty()) {
                document.add(new Paragraph(textBody, CONTENT_FONT));
            } else {
                document.add(new Paragraph("Nessun contenuto del corpo disponibile per questa email.", CONTENT_FONT));
            }
        }

        // Gestione degli allegati
        if (msg.getAttachmentFiles() != null && msg.getAttachmentFiles().length > 0) {
            document.add(new Paragraph("\nAllegati:", HEADER_FONT));
            for (org.apache.poi.hsmf.datatypes.AttachmentChunks attachment : msg.getAttachmentFiles()) {
                String fileName = null;
                if (attachment.getAttachLongFileName() != null) {
                    fileName = attachment.getAttachLongFileName().toString();
                } else if (attachment.getAttachFileName() != null) {
                    fileName = attachment.getAttachFileName().toString();
                }
                if (fileName != null) {
                    document.add(new Paragraph("- " + fileName, CONTENT_FONT));
                }
            }
        }
    }
}