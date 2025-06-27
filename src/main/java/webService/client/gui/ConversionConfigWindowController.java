package webService.client.gui;

import webService.client.configuration.configHandlers.conversionContext.*;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.Locale;
import java.util.ResourceBundle;

/**
 * Controller modernizzato della finestra di configurazione conversione.
 * Gestisce i parametri di conversione con supporto per temi dark/light e dialog moderni.
 */
public class ConversionConfigWindowController {

    @FXML public TextField unionField;
    @FXML public TextField zippedOutputField;
    @FXML public TextField txtPassword;
    @FXML public TextField protectedOutputField;
    @FXML public TextField txtWatermark;

    @FXML public Button toggleUnionBtn;
    @FXML public Button toggleZippedOutputBtn;
    @FXML public Button toggleProtectedOutputBtn;

    @FXML public Button saveButton;
    @FXML public Button cancelButton;

    @FXML public Label conversionContextTitle;
    @FXML public Label conversionContextDesc;
    @FXML public Label configurationParameters;
    @FXML public Label passwordLabel;
    @FXML public Label watermarkLabel;
    @FXML public Label unionLabel;
    @FXML public Label zippedLabel;
    @FXML public Label protectedLabel;

    @FXML private VBox conversionConfigHeaderContainer;

    private Stage dialogStage;
    private static final Logger logger = LogManager.getLogger(ConversionConfigWindowController.class);
    private boolean union;
    private boolean zippedOutput;
    private boolean protectedOutput;
    private static Locale locale = null;
    private static ResourceBundle bundle;

    /**
     * Inizializza il controller della finestra di configurazione.
     */
    @FXML
    private void initialize() {
        // Non più stili inline - tutto gestito da CSS
        if (locale == null || !locale.getLanguage().equals(MainApp.getCurrentLocale().getLanguage())){
            locale = MainApp.getCurrentLocale();
            bundle = ResourceBundle.getBundle("languages.MessagesBundle", locale);
        }
        loadCurrentConfiguration();
        refreshUITexts(locale);
    }

    public void refreshUITexts(Locale locale) {
        conversionContextTitle.setText(bundle.getString("label.conversionContextTitle"));
        conversionContextDesc.setText(bundle.getString("label.conversionContextDesc"));
        configurationParameters.setText(bundle.getString("label.configurationParameters"));
        passwordLabel.setText(bundle.getString("label.passwordLabel"));
        txtPassword.setPromptText(bundle.getString("field.txtPassword"));
        watermarkLabel.setText(bundle.getString("label.watermarkLabel"));
        txtWatermark.setPromptText(bundle.getString("field.txtWatermark"));
        unionLabel.setText(bundle.getString("label.unionLabel"));
        zippedLabel.setText(bundle.getString("label.zippedLabel"));
        protectedLabel.setText(bundle.getString("label.protectedLabel"));
        saveButton.setText(bundle.getString("btn.saveButton"));
        cancelButton.setText(bundle.getString("btn.closeButton"));
    }

    /**
     * Imposta lo stage e configura il drag della finestra
     */
    public void setDialogStage(Stage dialogStage) {
        this.dialogStage = dialogStage;

        // Configura il drag dell'header come nella ConfigWindow
        if (conversionConfigHeaderContainer != null) {
            Delta dragDelta = new Delta();

            conversionConfigHeaderContainer.setOnMousePressed(event -> {
                dragDelta.x = event.getSceneX();
                dragDelta.y = event.getSceneY();
            });

            conversionConfigHeaderContainer.setOnMouseDragged(event -> {
                dialogStage.setX(event.getScreenX() - dragDelta.x);
                dialogStage.setY(event.getScreenY() - dragDelta.y);
            });

            // Cursore di movimento
            conversionConfigHeaderContainer.setOnMouseEntered(event -> {
                conversionConfigHeaderContainer.setCursor(javafx.scene.Cursor.MOVE);
            });

            conversionConfigHeaderContainer.setOnMouseExited(event -> {
                conversionConfigHeaderContainer.setCursor(javafx.scene.Cursor.DEFAULT);
            });
        }
    }

    /**
     * Carica e mostra la configurazione corrente nei campi dell'interfaccia.
     */
    private void loadCurrentConfiguration() {
        // Carica i campi principali dalla configurazione
        txtPassword.setText(ConversionContextReader.getPassword());

        union = ConversionContextReader.getIsUnion();
        unionField.setText(String.valueOf(union));
        updateUnionToggleButton();

        zippedOutput = ConversionContextReader.getIsZippedOutput();
        zippedOutputField.setText(String.valueOf(zippedOutput));
        updateZippedOutputToggleButton();

        // DA FINIRE PER PROTECTED OUTPUT

        logger.info("Configurazione conversione caricata correttamente");
    }

    /**
     * Cambia il valore del flag zippedOutput.
     */
    @FXML
    public void zippedOutput(ActionEvent actionEvent) {
        zippedOutput = !zippedOutput;
        zippedOutputField.setText(String.valueOf(zippedOutput));
        updateZippedOutputToggleButton();
    }

    @FXML
    public void protectedOutput(ActionEvent actionEvent) {
        protectedOutput = !protectedOutput;
        protectedOutputField.setText(String.valueOf(protectedOutput));
        updateProtectedOutputToggleButton();
    }

    /**
     * Cambia il valore del flag union.
     */
    @FXML
    private void toggleUnion(ActionEvent actionEvent) {
        union = !union;
        unionField.setText(String.valueOf(union));
        updateUnionToggleButton();
    }

    /**
     * Aggiorna l'interfaccia in base allo stato del flag union usando CSS classes.
     */
    private void updateUnionToggleButton() {
        if (union) {
            // STATO ATTIVO
            toggleUnionBtn.setText(bundle.getString("btn.deactivate"));

            // NON cambiare colore del pulsante - rimane grigio
            // toggleUnionBtn mantiene solo la classe base

            // Campo readonly diventa attivo (azzurro)
            unionField.getStyleClass().removeAll("active-state");
            unionField.getStyleClass().add("active-state");

        } else {
            // STATO SPENTO
            toggleUnionBtn.setText(bundle.getString("btn.activate"));

            // NON cambiare colore del pulsante - rimane grigio
            // toggleUnionBtn mantiene solo la classe base

            // Campo readonly diventa normale (grigio)
            unionField.getStyleClass().removeAll("active-state");
        }
    }

    /**
     * Aggiorna l'interfaccia in base allo stato del flag zippedOutput usando CSS classes.
     */
    private void updateZippedOutputToggleButton() {
        if (zippedOutput) {
            // STATO ATTIVO
            toggleZippedOutputBtn.setText(bundle.getString("btn.deactivate"));

            // NON cambiare colore del pulsante - rimane grigio
            // toggleZippedOutputBtn mantiene solo la classe base

            // Campo readonly diventa attivo (azzurro)
            zippedOutputField.getStyleClass().removeAll("active-state");
            zippedOutputField.getStyleClass().add("active-state");

        } else {
            // STATO SPENTO
            toggleZippedOutputBtn.setText(bundle.getString("btn.activate"));

            // NON cambiare colore del pulsante - rimane grigio
            // toggleZippedOutputBtn mantiene solo la classe base

            // Campo readonly diventa normale (grigio)
            zippedOutputField.getStyleClass().removeAll("active-state");
        }
    }

    private void updateProtectedOutputToggleButton() {
        if (protectedOutput) {
            // STATO ATTIVO
            toggleProtectedOutputBtn.setText(bundle.getString("btn.deactivate"));

            // NON cambiare colore del pulsante - rimane grigio
            // toggleZippedOutputBtn mantiene solo la classe base

            // Campo readonly diventa attivo (azzurro)
            protectedOutputField.getStyleClass().removeAll("active-state");
            protectedOutputField.getStyleClass().add("active-state");

        } else {
            // STATO SPENTO
            toggleProtectedOutputBtn.setText(bundle.getString("btn.activate"));

            // NON cambiare colore del pulsante - rimane grigio
            // toggleZippedOutputBtn mantiene solo la classe base

            // Campo readonly diventa normale (grigio)
            protectedOutputField.getStyleClass().removeAll("active-state");
        }
    }

    /**
     * Salva la configurazione modificata.
     */
    @FXML
    private void saveConfiguration(ActionEvent event) {
        // Aggiorna le voci nel JSON
        ConversionContextWriter.setIsUnion(union);
        ConversionContextWriter.setIsZippedOutput(zippedOutput);
        ConversionContextWriter.setPassword(txtPassword.getText());

        logger.info("Configurazione di conversione salvata con successo");

        // Chiude la finestra
        dialogStage.close();
    }

    /**
     * Chiude la finestra senza salvare usando dialog moderno.
     */
    @FXML
    private void cancelAndClose(ActionEvent event) {
        // Controlla se ci sono modifiche non salvate
        if (hasUnsavedChanges()) {
            logger.info("Tentativo di chiusura con modifiche non salvate");

            // Usa il metodo helper per rilevare automaticamente il tema
            boolean isLightTheme = DialogHelper.detectCurrentTheme();

            Alert confirmAlert = DialogHelper.createModernAlert(
                    Alert.AlertType.CONFIRMATION,
                    "Unsaved Changes",
                    bundle.getString("label.closeWOsaving"),
                    isLightTheme
            );

            confirmAlert.showAndWait().ifPresent(response -> {
                if (response.getButtonData().isDefaultButton()) {
                    dialogStage.close();
                }
            });
        } else {
            logger.info("Finestra di configurazione conversione chiusa senza modifiche");
            dialogStage.close();
        }
    }

    /**
     * Verifica se ci sono modifiche non salvate.
     */
    private boolean hasUnsavedChanges() {
        try {
            // Controlla i campi modificabili
            String currentPassword = ConversionContextReader.getPassword();
            boolean currentUnion = ConversionContextReader.getIsUnion();
            boolean currentZippedOutput = ConversionContextReader.getIsZippedOutput();

            return !currentPassword.equals(txtPassword.getText().trim()) ||
                    currentUnion != union ||
                    currentZippedOutput != zippedOutput;
        } catch (Exception e) {
            logger.warn("Errore durante la verifica delle modifiche: " + e.getMessage());
            return true; // In caso di errore, assumi che ci siano modifiche
        }
    }

    /**
     * Classe helper per il drag della finestra
     */
    class Delta {
        double x, y;
    }
}