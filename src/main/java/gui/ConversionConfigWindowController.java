package gui;

import configuration.configHandlers.conversionContext.ConversionContextReader;
import configuration.configHandlers.conversionContext.ConversionContextWriter;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

/**
 * Controller modernizzato della finestra di configurazione conversione.
 * Gestisce i parametri di conversione con supporto per temi dark/light e dialog moderni.
 */
public class ConversionConfigWindowController {

    @FXML public TextField unionField;
    @FXML public TextField zippedOutputField;
    @FXML public TextField txtPassword;

    @FXML public Button toggleUnionBtn;
    @FXML public Button toggleZippedOutputBtn;

    @FXML public Button saveButton;
    @FXML public Button cancelButton;

    @FXML private VBox conversionConfigHeaderContainer;

    private Stage dialogStage;
    private static final Logger logger = LogManager.getLogger(ConversionConfigWindowController.class);
    private boolean union;
    private boolean zippedOutput;

    /**
     * Inizializza il controller della finestra di configurazione.
     */
    @FXML
    private void initialize() {
        // Non più stili inline - tutto gestito da CSS
        loadCurrentConfiguration();
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
            toggleUnionBtn.setText("Disabilita");

            // NON cambiare colore del pulsante - rimane grigio
            // toggleUnionBtn mantiene solo la classe base

            // Campo readonly diventa attivo (azzurro)
            unionField.getStyleClass().removeAll("active-state");
            unionField.getStyleClass().add("active-state");

        } else {
            // STATO SPENTO
            toggleUnionBtn.setText("Abilita");

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
            toggleZippedOutputBtn.setText("Disabilita");

            // NON cambiare colore del pulsante - rimane grigio
            // toggleZippedOutputBtn mantiene solo la classe base

            // Campo readonly diventa attivo (azzurro)
            zippedOutputField.getStyleClass().removeAll("active-state");
            zippedOutputField.getStyleClass().add("active-state");

        } else {
            // STATO SPENTO
            toggleZippedOutputBtn.setText("Abilita");

            // NON cambiare colore del pulsante - rimane grigio
            // toggleZippedOutputBtn mantiene solo la classe base

            // Campo readonly diventa normale (grigio)
            zippedOutputField.getStyleClass().removeAll("active-state");
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

            // Determina il tema corrente dalla finestra padre
            boolean isLightTheme = dialogStage.getScene().getRoot().getStyleClass().contains("light");

            Alert confirmAlert = DialogHelper.createModernAlert(
                    Alert.AlertType.CONFIRMATION,
                    "Modifiche non salvate",
                    "Ci sono modifiche non salvate. Sei sicuro di voler chiudere senza salvare?",
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