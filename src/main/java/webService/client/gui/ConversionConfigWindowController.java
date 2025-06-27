package webService.client.gui;

import webService.client.configuration.configHandlers.config.ConfigData;
import webService.client.configuration.configHandlers.config.ConfigInstance;
import webService.client.configuration.configHandlers.config.InstanceConfigWriter;
import webService.client.configuration.configHandlers.conversionContext.*;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import javax.crypto.spec.PSource;
import java.util.Locale;
import java.util.ResourceBundle;

/**
 * Controller modernizzato della finestra di configurazione conversione.
 * Gestisce i parametri di conversione con supporto per temi dark/light e dialog moderni.
 */
public class ConversionConfigWindowController {

    @FXML public TextField txtPassword;
    @FXML public TextField txtWatermark;

    @FXML public Button toggleUnionBtn;
    @FXML public Button toggleZippedOutputBtn;
    @FXML public Button toggleProtectedOutputBtn;
    @FXML public Button toggleMultipleConversionBtn;

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
    private boolean multipleConversions;
    private static Locale locale = null;
    private static ResourceBundle bundle;

    /**
     * Inizializza il controller della finestra di configurazione.
     */
    @FXML
    private void initialize() {
        // Non più stili inline - tutto gestito da CSS
        System.out.println("A");
        if (locale == null || !locale.getLanguage().equals(MainApp.getCurrentLocale().getLanguage())){
            locale = MainApp.getCurrentLocale();
            bundle = ResourceBundle.getBundle("languages.MessagesBundle", locale);
        }
        System.out.println("B");
        loadCurrentConfiguration();
        System.out.println("C");
        refreshUITexts();
        System.out.println("D");
    }

    public void refreshUITexts() {
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
        System.out.println("bombo");
        // Carica i campi principali dalla configurazione
        txtPassword.setText(ConversionContextReader.getPassword());
        System.out.println("dio");

        txtWatermark.setText(ConversionContextReader.getWatermark());
        System.out.println("sulla");

        union = ConversionContextReader.getIsUnion();
        System.out.println(union);
        System.out.println("sedia");
        updateUnionToggleButton();
        System.out.println("mentre");

        zippedOutput = ConversionContextReader.getIsZippedOutput();
        System.out.println("cago");
        updateZippedOutputToggleButton();
        System.out.println("E");
        protectedOutput = ConversionContextReader.getProtected();
        System.out.println("E2");
        updateProtectedOutputToggleButton();
        System.out.println("F");
        multipleConversions = ConversionContextReader.getIsMultipleConversionEnabled();
        System.out.println("F2");
        updateMultipleConversionButton();

        logger.info("Configurazione conversione caricata correttamente");
    }

    /**
     * Cambia il valore del flag zippedOutput.
     */
    @FXML
    public void zippedOutput(ActionEvent actionEvent) {
        zippedOutput = !zippedOutput;
        updateZippedOutputToggleButton();
    }

    @FXML
    public void protectedOutput(ActionEvent actionEvent) {
        protectedOutput = !protectedOutput;
        updateProtectedOutputToggleButton();
    }

    /**
     * Cambia il valore del flag union.
     */
    @FXML
    private void toggleUnion(ActionEvent actionEvent) {
        union = !union;
        updateUnionToggleButton();
    }

    @FXML
    private void toggleMultipleConversion(ActionEvent actionEvent) {
        multipleConversions = !multipleConversions;
        updateMultipleConversionButton();
    }

    /**
     * Aggiorna l'interfaccia in base allo stato del flag union usando CSS classes dinamiche.
     */
    private void updateUnionToggleButton() {
        // Rimuovi tutte le classi di stato precedenti
        toggleUnionBtn.getStyleClass().removeAll("activate-state", "deactivate-state");
        System.out.println("cago");

        if (union) {
            // STATO ATTIVO - mostra "Disattiva" - pulsante AZZURRO
            toggleUnionBtn.setText(bundle.getString("btn.inactive"));
            toggleUnionBtn.getStyleClass().add("deactivate-state");
        } else {
            // STATO SPENTO - mostra "Attiva" - pulsante GRIGIO
            toggleUnionBtn.setText(bundle.getString("btn.active"));
            toggleUnionBtn.getStyleClass().add("activate-state");
            System.out.println("ciao bro");
        }
    }

    /**
     * Aggiorna l'interfaccia in base allo stato del flag multipleConversions usando CSS classes dinamiche.
     */
    private void updateMultipleConversionButton() {
        // Rimuovi tutte le classi di stato precedenti
        toggleMultipleConversionBtn.getStyleClass().removeAll("activate-state", "deactivate-state");

        if (multipleConversions) {
            // STATO ATTIVO - mostra "Disattiva" - pulsante AZZURRO
            toggleMultipleConversionBtn.setText(bundle.getString("btn.inactive"));
            toggleMultipleConversionBtn.getStyleClass().add("deactivate-state");
        } else {
            // STATO SPENTO - mostra "Attiva" - pulsante GRIGIO
            toggleMultipleConversionBtn.setText(bundle.getString("btn.active"));
            toggleMultipleConversionBtn.getStyleClass().add("activate-state");
        }
    }

    /**
     * Aggiorna l'interfaccia in base allo stato del flag zippedOutput usando CSS classes dinamiche.
     */
    private void updateZippedOutputToggleButton() {
        // Rimuovi tutte le classi di stato precedenti
        toggleZippedOutputBtn.getStyleClass().removeAll("activate-state", "deactivate-state");

        if (zippedOutput) {
            // STATO ATTIVO - mostra "Disattiva" - pulsante AZZURRO
            toggleZippedOutputBtn.setText(bundle.getString("btn.inactive"));
            toggleZippedOutputBtn.getStyleClass().add("deactivate-state");
        } else {
            // STATO SPENTO - mostra "Attiva" - pulsante GRIGIO
            toggleZippedOutputBtn.setText(bundle.getString("btn.active"));
            toggleZippedOutputBtn.getStyleClass().add("activate-state");
        }
    }

    /**
     * Aggiorna l'interfaccia in base allo stato del flag protectedOutput usando CSS classes dinamiche.
     */
    private void updateProtectedOutputToggleButton() {
        // Rimuovi tutte le classi di stato precedenti
        toggleProtectedOutputBtn.getStyleClass().removeAll("activate-state", "deactivate-state");

        if (protectedOutput) {
            // STATO ATTIVO - mostra "Disattiva" - pulsante AZZURRO
            toggleProtectedOutputBtn.setText(bundle.getString("btn.inactive"));
            toggleProtectedOutputBtn.getStyleClass().add("deactivate-state");
        } else {
            // STATO SPENTO - mostra "Attiva" - pulsante GRIGIO
            toggleProtectedOutputBtn.setText(bundle.getString("btn.active"));
            toggleProtectedOutputBtn.getStyleClass().add("activate-state");
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
        ConversionContextWriter.setWatermark(txtWatermark.getText());

        logger.info("Configurazione di conversione salvata con successo");

        InstanceConversionContextWriter wr = new InstanceConversionContextWriter(ConversionContextData.getJsonFile());
        wr.writeIsUnionEnabled(union);
        wr.writeIsZippedOutput(zippedOutput);
        wr.writePassword(txtPassword.getText());
        wr.writeProtected(protectedOutput);
        wr.writeWatermark(txtWatermark.getText());

        ConversionContextData.update(new ConversionContextInstance(ConversionContextData.getJsonFile()));

        logger.info("Configurazione salvata con successo");
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
            boolean currentProtectOutput = ConversionContextReader.getProtected();
            boolean currentMultipleConversion = ConversionContextReader.getIsMultipleConversionEnabled();
            String currentWatermark = ConversionContextReader.getWatermark();

            return !currentPassword.equals(txtPassword.getText().trim()) ||
                    currentUnion != union ||
                    currentZippedOutput != zippedOutput ||
                    !currentWatermark.equals(txtWatermark.getText().trim()) ||
                    currentMultipleConversion != multipleConversions ||
                    currentProtectOutput != protectedOutput;
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