package Converters;

import com.itextpdf.text.Document;
import com.itextpdf.text.DocumentException;
import com.itextpdf.text.Font;
import com.itextpdf.text.FontFactory;
import com.itextpdf.text.Paragraph;
import com.itextpdf.text.pdf.PdfWriter;
import com.itextpdf.tool.xml.XMLWorkerHelper;

import org.apache.poi.hsmf.MAPIMessage;
import org.apache.poi.hsmf.exceptions.ChunkNotFoundException;

import org.jsoup.Jsoup;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;

public class MSGtoPDFconverter implements Converter {

    private static final Font HEADER_FONT = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 12);
    private static final Font CONTENT_FONT = FontFactory.getFont(FontFactory.HELVETICA, 10);

    /**
     * Implementazione dell'interfaccia Converter
     * Converte un file MSG in PDF e ritorna la lista dei file creati
     *
     * @param srcFile file MSG di input
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
            File pdfFile = convertMsgToPdf(srcFile, outputPath);
            resultFiles.add(pdfFile);

        } catch (DocumentException e) {
            throw new IOException("Errore nella creazione del PDF: " + e.getMessage(), e);
        }

        return resultFiles;
    }

    /**
     * Converte un file MSG di Outlook in un file PDF.
     * Estrae intestazioni e corpo (testo semplice o HTML) e li formatta nel PDF.
     *
     * @param fileMSG   Il file MSG di input.
     * @param outputPdfPath Il percorso completo dove salvare il file PDF di output.
     * @return File oggetto che rappresenta il PDF creato
     * @throws IOException      Se si verifica un errore di lettura/scrittura del file.
     * @throws DocumentException Se si verifica un errore durante la creazione del PDF con iText.
     */
    public static File convertMsgToPdf(File fileMSG, String outputPdfPath)
            throws IOException, DocumentException {
        if (!fileMSG.exists()) {
            throw new FileNotFoundException("File MSG non trovato: " + fileMSG.getName());
        }

        // Crea le directory parent se non esistono
        File outputPdfFile = new File(outputPdfPath);
        File parentDir = outputPdfFile.getParentFile();
        if (parentDir != null && !parentDir.exists()) {
            if (!parentDir.mkdirs()) {
                System.err.println("Failed to create directories: " + parentDir.getAbsolutePath());
            }
        }

        MAPIMessage msg = new MAPIMessage(fileMSG.getAbsolutePath());

        // Creazione documento PDF
        Document document = new Document();
        PdfWriter writer = null;
        try {
            writer = PdfWriter.getInstance(document, Files.newOutputStream(Paths.get(outputPdfPath)));
            document.open();

            // Aggiunta intestazioni dell'email al PDF
            addMsgHeadersToPdf(msg, document);
            document.add(new Paragraph("\n")); // Spazio dopo le intestazioni

            // conversione corpo dell'email
            addMsgBodyToPdf(msg, document, writer);

        } finally {
            if (document.isOpen()) {
                document.close();
            }
            if (writer != null) {
                writer.close();
            }
            // Chiusura il MAPIMessage per liberare le risorse
            try {
                msg.close();
            } catch (IOException e) {
                System.err.println("Warning: Impossibile chiudere il MAPIMessage: " + e.getMessage());
            }
        }

        // Verifica che il file sia stato creato correttamente
        if (!outputPdfFile.exists()) {
            throw new IOException("Errore nella creazione del file PDF: " + outputPdfPath);
        }

        return outputPdfFile;
    }

    private static void addMsgHeadersToPdf(MAPIMessage msg, Document document) throws DocumentException {
        document.add(new Paragraph("Email Headers:", HEADER_FONT));
        document.add(new Paragraph("----------------------------------------", HEADER_FONT));

        // Mittente
        try {
            String displayFrom = msg.getDisplayFrom();
            if (displayFrom != null && !displayFrom.trim().isEmpty()) {
                document.add(new Paragraph("Da: " + displayFrom, CONTENT_FONT));
            }
        } catch (ChunkNotFoundException ignored) {
        }

        // Destinatario
        try {
            String displayTo = msg.getDisplayTo();
            if (displayTo != null && !displayTo.trim().isEmpty()) {
                document.add(new Paragraph("A: " + displayTo, CONTENT_FONT));
            }
        } catch (ChunkNotFoundException ignored) {
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
                document.add(new Paragraph("Data: " + messageDate, CONTENT_FONT));
            }
        } catch (ChunkNotFoundException e) {
            // Ignora se non trovato
        }

        document.add(new Paragraph("----------------------------------------\n", HEADER_FONT));
    }

    private static void addMsgBodyToPdf(MAPIMessage msg, Document document, PdfWriter writer) throws DocumentException {
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
                        System.err.println("Warning: Errore nel leggere il nome dell'allegato: " + e.getMessage());
                    }
                }
            }
        } catch (Exception e) {
            // Ignora errori nella lettura degli allegati
            System.err.println("Warning: Errore nel leggere gli allegati: " + e.getMessage());
        }
    }
}