package Converters;

import com.itextpdf.text.Document;
import com.itextpdf.text.DocumentException;
import com.itextpdf.text.Font;
import com.itextpdf.text.FontFactory;
import com.itextpdf.text.Paragraph;
import com.itextpdf.text.pdf.PdfWriter;
import com.itextpdf.tool.xml.XMLWorkerHelper;

import converter.Log;
import converter.Utility;
import org.apache.poi.hsmf.MAPIMessage;
import org.apache.poi.hsmf.datatypes.Chunk;
import org.apache.poi.hsmf.datatypes.MAPIProperty;
import org.apache.poi.hsmf.datatypes.RecipientChunks;
import org.apache.poi.hsmf.exceptions.ChunkNotFoundException;

import org.jsoup.Jsoup;
import org.jsoup.safety.Safelist;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;

public class MSGtoPDFconverter implements Converter{
    private static final Font HEADER_FONT = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 12);
    private static final Font CONTENT_FONT = FontFactory.getFont(FontFactory.HELVETICA, 10);

    /**
     * Converte un file MSG di Outlook in un file PDF.
     * Estrae intestazioni e corpo (testo semplice o HTML) e li formatta nel PDF.
     * @return File oggetto che rappresenta il PDF creato
     * @throws IOException      Se si verifica un errore di lettura/scrittura del file.
     * @throws DocumentException Se si verifica un errore durante la creazione del PDF con iText.
     */
    @Override
    public ArrayList<File> convert(File msgFile) throws IOException, DocumentException {
        Log.addMessage("Inizio conversione msg: "+ Utility.estraiNomePiuEstensioneFile(msgFile) +" -> .pdf");
        if (msgFile == null || !msgFile.exists()) {
            Log.addMessage("ERRORE: File MSG non trovato: " + msgFile);
            throw new FileNotFoundException("File MSG non trovato: " + msgFile);
        }

        // Directory di output fissa
        File outputDir = new File("src/temp");
        if (!outputDir.exists()) {
            outputDir.mkdirs();
        }

        // Crea nome PDF con lo stesso nome del file .msg
        String baseName = msgFile.getName().replaceFirst("[.][^.]+$", ""); // es: messaggio.msg → messaggio
        File outputPdfFile = new File(outputDir, baseName + ".pdf");

        MAPIMessage msg = new MAPIMessage(msgFile.getAbsolutePath());

        Document document = new Document();
        PdfWriter writer = null;

        try {
            writer = PdfWriter.getInstance(document, new FileOutputStream(outputPdfFile));
            document.open();

            addMsgHeadersToPdf(msg, document);
            document.add(new Paragraph("\n"));
            addMsgBodyToPdf(msg, document, writer);

        } finally {
            if (document.isOpen()) {
                document.close();
            }
            if (writer != null) {
                writer.close();
            }
            try {
                msg.close();
            } catch (IOException e) {
                System.err.println("Warning: Impossibile chiudere il MAPIMessage: " + e.getMessage());
            }
        }

        if (!outputPdfFile.exists()) {
            Log.addMessage("ERRORE: creazione del file PDF fallita: " + outputPdfFile.getAbsolutePath());
            throw new IOException("Errore nella creazione del file PDF: " + outputPdfFile.getAbsolutePath());
        }

        ArrayList<File> result = new ArrayList<>();
        result.add(outputPdfFile);
        Log.addMessage("Creazione file .pdf completata: "+outputPdfFile.getName());
        return result;
    }

    private static void addMsgHeadersToPdf(MAPIMessage msg, Document document) throws DocumentException, IOException {
        document.add(new Paragraph("Email Headers:", HEADER_FONT));
        document.add(new Paragraph("----------------------------------------", HEADER_FONT));

        // Mittente
        try {
            String displayFrom = msg.getDisplayFrom();
            if (displayFrom != null && !displayFrom.trim().isEmpty()) {
                document.add(new Paragraph("Da: " + displayFrom, CONTENT_FONT));
            }
        } catch (ChunkNotFoundException e) {
            // Ignora se non trovato
        }

        // Destinatario
        try {
            String displayTo = msg.getDisplayTo();
            if (displayTo != null && !displayTo.trim().isEmpty()) {
                document.add(new Paragraph("A: " + displayTo, CONTENT_FONT));
            }
        } catch (ChunkNotFoundException e) {
            // Ignora se non trovato
        }

        // Cc
        /*
        Destinatari che ricevono una copia dell'email per informazione. Anche in questo caso,
        tutti i destinatari (in "A", "Cc" e "Bcc") possono vedere chi è nel campo "Cc".
         */
        try {
            String displayCC = msg.getDisplayCC();
            if (displayCC != null && !displayCC.trim().isEmpty()) {
                document.add(new Paragraph("Cc: " + displayCC, CONTENT_FONT));
            }
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
            String displayBCC = msg.getDisplayBCC();
            if (displayBCC != null && !displayBCC.trim().isEmpty()) {
                document.add(new Paragraph("Bcc: " + displayBCC, CONTENT_FONT));
            }
        } catch (ChunkNotFoundException e) {
            // Ignora se non trovato
        }

        // Oggetto
        try {
            String subject = msg.getSubject();
            if (subject != null && !subject.trim().isEmpty()) {
                document.add(new Paragraph("Oggetto: " + subject, CONTENT_FONT));
            }
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
            try {
                // Pulisce l'HTML e lo converte in XHTML valido
                org.jsoup.nodes.Document jsoupDoc = Jsoup.parse(htmlBody);
                jsoupDoc.outputSettings().syntax(org.jsoup.nodes.Document.OutputSettings.Syntax.xml); // XHTML
                jsoupDoc.outputSettings().charset(StandardCharsets.UTF_8);
                jsoupDoc.outputSettings().escapeMode(org.jsoup.nodes.Entities.EscapeMode.xhtml);

                String xhtmlContent = jsoupDoc.html();
                XMLWorkerHelper.getInstance().parseXHtml(writer, document,
                        new ByteArrayInputStream(xhtmlContent.getBytes(StandardCharsets.UTF_8)),
                        StandardCharsets.UTF_8);
            } catch (Exception e) {
                // Se il parsing HTML fallisce, usa il testo semplice come fallback
                System.err.println("Warning: Errore nel parsing HTML, uso testo semplice: " + e.getMessage());
                document.add(new Paragraph("Contenuto HTML (errore nel parsing):\n" + htmlBody, CONTENT_FONT));
            }
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
        try {
            if (msg.getAttachmentFiles() != null && msg.getAttachmentFiles().length > 0) {
                document.add(new Paragraph("\nAllegati:", HEADER_FONT));
                for (org.apache.poi.hsmf.datatypes.AttachmentChunks attachment : msg.getAttachmentFiles()) {
                    String fileName = null;
                    try {
                        if (attachment.getAttachLongFileName() != null) {
                            fileName = attachment.getAttachLongFileName().toString();
                        } else if (attachment.getAttachFileName() != null) {
                            fileName = attachment.getAttachFileName().toString();
                        }
                        if (fileName != null && !fileName.trim().isEmpty()) {
                            document.add(new Paragraph("- " + fileName, CONTENT_FONT));
                        }
                    } catch (Exception e) {
                        // Ignora errori sui singoli allegati
                        Log.addMessage("WARNING: Errore nel leggere il nome dell'allegato: " + e.getMessage());
                        System.err.println("Warning: Errore nel leggere il nome dell'allegato: " + e.getMessage());
                    }
                }
            }
        } catch (Exception e) {
            // Ignora errori nella lettura degli allegati
            Log.addMessage("WARNING: Errore nel leggere gli allegati: " + e.getMessage());
            System.err.println("Warning: Errore nel leggere gli allegati: " + e.getMessage());
        }
    }
}