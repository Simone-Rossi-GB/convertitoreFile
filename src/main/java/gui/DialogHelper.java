package gui;

import gui.jsonHandler.ConfigManager;
import gui.jsonHandler.JsonConfig;
import javafx.scene.control.*;
import javafx.scene.layout.Region;
import javafx.stage.Stage;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.List;

/**
 * Helper class per creare dialog moderni con supporto per temi dark/light
 */
public class DialogHelper {

    private static final Logger logger = LogManager.getLogger(DialogHelper.class);

    /**
     * Crea un Alert moderno con il tema appropriato
     */
    public static Alert createModernAlert(Alert.AlertType alertType, String title, String message, boolean isLightTheme) {
        Alert alert = new Alert(alertType);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(message);

        // Applica il tema al dialog
        applyThemeToDialog(alert, isLightTheme);

        return alert;
    }

    /**
     * Crea un ChoiceDialog moderno con il tema appropriato
     */
    public static ChoiceDialog<String> createModernChoiceDialog(String defaultChoice, List<String> choices,
                                                                String title, String header, String content, boolean isLightTheme) {
        ChoiceDialog<String> dialog = new ChoiceDialog<>(defaultChoice, choices);
        dialog.setTitle(title);
        dialog.setHeaderText(header);
        dialog.setContentText(content);

        // Applica il tema al dialog
        applyThemeToDialog(dialog, isLightTheme);

        return dialog;
    }

    /**
     * Crea un TextInputDialog moderno con il tema appropriato
     */
    public static TextInputDialog createModernTextInputDialog(String defaultText, String title,
                                                              String header, String content, boolean isLightTheme) {
        TextInputDialog dialog = new TextInputDialog(defaultText);
        dialog.setTitle(title);
        dialog.setHeaderText(header);
        dialog.setContentText(content);

        // Applica il tema al dialog
        applyThemeToDialog(dialog, isLightTheme);

        return dialog;
    }

    /**
     * Metodo principale per applicare il tema a qualsiasi dialog
     */
    private static void applyThemeToDialog(Dialog<?> dialog, boolean isLightTheme) {
        try {
            // Ottieni il DialogPane
            DialogPane dialogPane = dialog.getDialogPane();

            // Carica il CSS per i dialog moderni
            String cssPath = DialogHelper.class.getResource("/css/modern-dialogs-theme.css").toExternalForm();
            dialogPane.getStylesheets().add(cssPath);

            // Applica la classe del tema appropriato
            if (isLightTheme) {
                dialogPane.getStyleClass().add("light");
                logger.debug("Applicato tema light al dialog");
            } else {
                dialogPane.getStyleClass().add("dark");
                logger.debug("Applicato tema dark al dialog");
            }

            // Applica classe specifica per il tipo di alert
            if (dialog instanceof Alert) {
                Alert alert = (Alert) dialog;
                switch (alert.getAlertType()) {
                    case ERROR:
                        dialogPane.getStyleClass().add("error");
                        break;
                    case INFORMATION:
                        dialogPane.getStyleClass().add("information");
                        break;
                    case CONFIRMATION:
                        dialogPane.getStyleClass().add("confirmation");
                        break;
                    case WARNING:
                        dialogPane.getStyleClass().add("warning");
                        break;
                }
            }

        } catch (Exception e) {
            logger.warn("Errore nell'applicazione del tema al dialog: " + e.getMessage());
            // Fallback: prova percorso alternativo
            try {
                String fallbackCssPath = DialogHelper.class.getResource("/styles/modern-dialogs-theme.css").toExternalForm();
                dialog.getDialogPane().getStylesheets().add(fallbackCssPath);

                if (isLightTheme) {
                    dialog.getDialogPane().getStyleClass().add("light");
                } else {
                    dialog.getDialogPane().getStyleClass().add("dark");
                }
                logger.debug("Applicato tema con percorso fallback");
            } catch (Exception fallbackError) {
                logger.error("Impossibile applicare il tema al dialog: " + fallbackError.getMessage());
            }
        }
    }

    /**
     * Metodo helper per rilevare automaticamente il tema corrente
     */
    public static boolean detectCurrentTheme() {
        try {
            // Primo tentativo: controlla il primaryStage
            if (MainApp.getPrimaryStage() != null &&
                    MainApp.getPrimaryStage().getScene() != null &&
                    MainApp.getPrimaryStage().getScene().getRoot() != null) {

                return MainApp.getPrimaryStage().getScene().getRoot().getStyleClass().contains("light");
            }
        } catch (Exception e) {
            logger.debug("Impossibile rilevare tema da primaryStage: " + e.getMessage());
        }

        try {
            // Fallback: controlla il config JSON
            JsonConfig config = ConfigManager.readConfig();
            return "light".equals(config.getTheme());
        } catch (Exception e) {
            logger.debug("Impossibile rilevare tema da config: " + e.getMessage());
        }

        // Default: dark theme
        return false;
    }

    /**
     * Versioni semplificate che rilevano automaticamente il tema
     */
    public static Alert createModernAlert(Alert.AlertType alertType, String title, String message) {
        return createModernAlert(alertType, title, message, detectCurrentTheme());
    }

    public static ChoiceDialog<String> createModernChoiceDialog(String defaultChoice, List<String> choices,
                                                                String title, String header, String content) {
        return createModernChoiceDialog(defaultChoice, choices, title, header, content, detectCurrentTheme());
    }

    public static TextInputDialog createModernTextInputDialog(String defaultText, String title,
                                                              String header, String content) {
        return createModernTextInputDialog(defaultText, title, header, content, detectCurrentTheme());
    }
}