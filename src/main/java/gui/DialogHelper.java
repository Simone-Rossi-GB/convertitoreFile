package gui;

import javafx.scene.control.*;
import javafx.stage.Stage;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

/**
 * Helper class per applicare stili moderni ai dialog JavaFX.
 * Applica automaticamente il tema corrente (dark/light) e gli stili CSS moderni.
 */
public class DialogHelper {

    private static final Logger logger = LogManager.getLogger(DialogHelper.class);

    /**
     * Applica lo stile moderno a un dialog in base al tema corrente.
     *
     * @param dialog Il dialog da stilizzare
     * @param isLightTheme Se true applica il tema light, altrimenti dark
     */
    public static void applyModernStyle(Dialog<?> dialog, boolean isLightTheme) {
        try {
            // Ottieni il DialogPane
            DialogPane dialogPane = dialog.getDialogPane();

            // Applica la classe per il tema
            if (isLightTheme) {
                dialogPane.getStyleClass().add("light");
            } else {
                dialogPane.getStyleClass().add("dark");
            }

            // Applica classi specifiche per il tipo di dialog
            if (dialog instanceof Alert) {
                Alert alert = (Alert) dialog;
                switch (alert.getAlertType()) {
                    case ERROR:
                        dialogPane.getStyleClass().add("error");
                        break;
                    case INFORMATION:
                        dialogPane.getStyleClass().add("information");
                        break;
                    case WARNING:
                        dialogPane.getStyleClass().add("warning");
                        break;
                    case CONFIRMATION:
                        dialogPane.getStyleClass().add("confirmation");
                        break;
                }
            }

            // Carica il CSS moderno per i dialog
            try {
                dialogPane.getStylesheets().add(
                        DialogHelper.class.getResource("/css/modern-dialogs-theme.css").toExternalForm()
                );
                logger.debug("CSS dialog moderno caricato da /css/");
            } catch (Exception cssError) {
                try {
                    dialogPane.getStylesheets().add(
                            DialogHelper.class.getResource("/styles/modern-dialogs-theme.css").toExternalForm()
                    );
                    logger.debug("CSS dialog moderno caricato da /styles/");
                } catch (Exception cssError2) {
                    logger.warn("Impossibile caricare CSS moderno per dialog: " + cssError2.getMessage());
                }
            }

            // Imposta l'owner se disponibile
            if (MainApp.getPrimaryStage() != null) {
                Stage dialogStage = (Stage) dialogPane.getScene().getWindow();
                if (dialogStage != null) {
                    dialogStage.initOwner(MainApp.getPrimaryStage());
                }
            }

        } catch (Exception e) {
            logger.error("Errore nell'applicazione dello stile moderno al dialog: " + e.getMessage());
        }
    }

    /**
     * Crea un Alert moderno con stile automatico.
     *
     * @param alertType Tipo di alert
     * @param title Titolo del dialog
     * @param message Messaggio del dialog
     * @param isLightTheme Se true applica il tema light
     * @return Alert stilizzato
     */
    public static Alert createModernAlert(Alert.AlertType alertType, String title, String message, boolean isLightTheme) {
        Alert alert = new Alert(alertType);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(message);

        // Applica lo stile moderno
        applyModernStyle(alert, isLightTheme);

        return alert;
    }

    /**
     * Crea un ChoiceDialog moderno con stile automatico.
     *
     * @param defaultChoice Scelta di default
     * @param choices Lista delle scelte disponibili
     * @param title Titolo del dialog
     * @param headerText Testo dell'header
     * @param contentText Testo del contenuto
     * @param isLightTheme Se true applica il tema light
     * @return ChoiceDialog stilizzato
     */
    public static <T> ChoiceDialog<T> createModernChoiceDialog(T defaultChoice, java.util.Collection<T> choices,
                                                               String title, String headerText, String contentText,
                                                               boolean isLightTheme) {
        ChoiceDialog<T> dialog = new ChoiceDialog<>(defaultChoice, choices);
        dialog.setTitle(title);
        dialog.setHeaderText(headerText);
        dialog.setContentText(contentText);

        // Applica lo stile moderno
        applyModernStyle(dialog, isLightTheme);

        return dialog;
    }

    /**
     * Crea un TextInputDialog moderno con stile automatico.
     *
     * @param defaultText Testo di default
     * @param title Titolo del dialog
     * @param headerText Testo dell'header
     * @param contentText Testo del contenuto
     * @param isLightTheme Se true applica il tema light
     * @return TextInputDialog stilizzato
     */
    public static TextInputDialog createModernTextInputDialog(String defaultText, String title,
                                                              String headerText, String contentText,
                                                              boolean isLightTheme) {
        TextInputDialog dialog = new TextInputDialog(defaultText);
        dialog.setTitle(title);
        dialog.setHeaderText(headerText);
        dialog.setContentText(contentText);

        // Applica lo stile moderno
        applyModernStyle(dialog, isLightTheme);

        return dialog;
    }
}