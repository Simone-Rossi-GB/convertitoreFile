package webService.server.converters;

import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.pdmodel.PDPageContentStream;
import org.apache.pdfbox.pdmodel.font.PDFont;
import org.apache.pdfbox.pdmodel.font.PDType1Font;
import org.apache.pdfbox.pdmodel.graphics.state.PDExtendedGraphicsState;
import java.awt.Color;
import java.io.File;
import java.io.IOException;

import webService.server.config.configHandlers.Config;
import webService.server.converters.exception.WatermarkException;

public class PDFWatermarkApplier {

    /**
     * Applica un watermark testuale a ogni pagina di un documento PDF esistente.
     *
     * @param inputPdf  File PDF di input.
     * @param outputPdf File PDF di output con il watermark.
     * @param watermarkText Il testo da usare come watermark.
     * @return true se il watermark è stato applicato con successo, false altrimenti.
     */
    public static boolean applyWatermark(File inputPdf, File outputPdf, String watermarkText) throws WatermarkException{

        try (PDDocument document = PDDocument.load(inputPdf)) {

            // Imposta font, dimensioni e colore per il watermark
            PDFont font = PDType1Font.HELVETICA_BOLD;
            float fontSize = 60.0f;
            Color textColor = Color.LIGHT_GRAY;

            // Imposta la trasparenza (opacità) del watermark
            PDExtendedGraphicsState graphicsState = new PDExtendedGraphicsState();
            graphicsState.setNonStrokingAlphaConstant(0.4f); // 40% di opacità
            graphicsState.setAlphaSourceFlag(true);

            // Scorri tutte le pagine del documento e aggiungi il watermark
            for (PDPage page : document.getPages()) {

                // Usa APPEND per aggiungere contenuto senza cancellare quello esistente
                try (PDPageContentStream contentStream = new PDPageContentStream(document, page, PDPageContentStream.AppendMode.APPEND, true, true)) {

                    // Applica lo stato grafico per la trasparenza
                    contentStream.setGraphicsStateParameters(graphicsState);
                    contentStream.setNonStrokingColor(textColor);
                    contentStream.setFont(font, fontSize);

                    // Calcola le dimensioni della pagina
                    float pageHeight = page.getMediaBox().getHeight();
                    float pageWidth = page.getMediaBox().getWidth();

                    // Calcola la larghezza del testo
                    float stringWidth = font.getStringWidth(watermarkText) * fontSize / 1000;

                    // Calcola il centro della pagina
                    float centerX = pageWidth / 2;
                    float centerY = pageHeight / 2;

                    // Angolo di rotazione in radianti (45 gradi)
                    double angle = Math.toRadians(45);

                    // Per centrare il testo rotato, dobbiamo sottrarre metà della larghezza del testo
                    // considerando la rotazione
                    float offsetX = (float) (stringWidth * Math.cos(angle) / 2);
                    float offsetY = (float) (stringWidth * Math.sin(angle) / 2);

                    // Posizione finale del watermark (centrato e rotato)
                    float x = centerX - offsetX;
                    float y = centerY - offsetY;

                    // Applica il watermark con la matrice di trasformazione corretta
                    contentStream.beginText();
                    contentStream.setTextMatrix(
                            (float) Math.cos(angle),    // a: cos(θ)
                            (float) Math.sin(angle),    // b: sin(θ)
                            (float) -Math.sin(angle),   // c: -sin(θ)
                            (float) Math.cos(angle),    // d: cos(θ)
                            x,                          // e: posizione X
                            y                           // f: posizione Y
                    );
                    contentStream.showText(watermarkText);
                    contentStream.endText();
                }
            }

            // Salva il documento modificato
            document.save(outputPdf);
            System.out.println("Watermark aggiunto con successo a " + outputPdf.getPath());
            return true; // Ritorna true se tutto è andato a buon fine

        } catch (IOException e) {
            // In caso di errore (es. file non trovato), stampa l'errore e ritorna false
            System.err.println("Errore durante l'applicazione del watermark: " + e.getMessage());
            e.printStackTrace();
            return false; // Ritorna false in caso di eccezione
        }
    }
}