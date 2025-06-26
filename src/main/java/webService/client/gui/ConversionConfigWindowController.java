package webService.client.gui;

import webService.client.configuration.configHandlers.conversionContext.*;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.stage.Stage;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.File;


/**
 * Controller della finestra di configurazione dell'applicazione.
 * Gestisce le directory di monitoraggio, successo, errore, il flag di avvio automatico
 * e la visualizzazione della sezione "conversions" del file di configurazione JSON.
 */
public class ConversionConfigWindowController {

    @FXML public TextField unionField;
    @FXML public TextField zippedOutputField;
    @FXML public TextField txtPassword;

    @FXML public Button toggleUnionBtn;
    @FXML public Button toggleZippedOutputBtn;


    private Stage dialogStage;
    private static final Logger logger = LogManager.getLogger(ConversionConfigWindowController.class);
    private boolean union;
    private boolean zippedOutput;

    /**
     * Inizializza il controller della finestra di configurazione.
     */
    @FXML
    private void initialize() {
        // Configura il campo di testo per la password
        txtPassword.setStyle("-fx-background-color: #ffffff; -fx-border-color: #bdc3c7; -fx-border-radius: 5;");
        loadCurrentConfiguration();
    }


    /**
     * Imposta lo stage per la finestra.
     */
    public void setDialogStage(Stage dialogStage) {
        this.dialogStage = dialogStage;
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
        logger.info("Configurazione caricata correttamente");
    }

    /**
     * Cambia il valore del flag zippedOutput.
     * @param actionEvent pulsante toggleZippedOutput cliccato
     */
    @FXML
    public void zippedOutput(ActionEvent actionEvent) {
        zippedOutput = !zippedOutput;
        zippedOutputField.setText(String.valueOf(zippedOutput));
        updateZippedOutputToggleButton();
    }

    /**
     * Cambia il valore del flag union.
     * @param actionEvent pulsante toggleUnion cliccato
     */
    @FXML
    private void toggleUnion(ActionEvent actionEvent) {
        union = !union;
        unionField.setText(String.valueOf(union));
        updateUnionToggleButton();
    }

    /**
     * Aggiorna l'interfaccia in base allo stato del flag union.
     */
    private void updateUnionToggleButton() {
        if (union) {
            toggleUnionBtn.setText("Disabilita");
            toggleUnionBtn.setStyle("-fx-background-color: #e74c3c; -fx-text-fill: white; -fx-background-radius: 5;");
            unionField.setStyle("-fx-background-color: #d5f4e6; -fx-text-fill: #27ae60; -fx-font-weight: bold;");
        } else {
            toggleUnionBtn.setText("Abilita");
            toggleUnionBtn.setStyle("-fx-background-color: #27ae60; -fx-text-fill: white; -fx-background-radius: 5;");
            unionField.setStyle("-fx-background-color: #fadbd8; -fx-text-fill: #e74c3c; -fx-font-weight: bold;");
        }
    }

    /**
     * Aggiorna l'interfaccia in base allo stato del flag union.
     */
    private void updateZippedOutputToggleButton() {
        if (zippedOutput) {
            toggleZippedOutputBtn.setText("Disabilita");
            toggleZippedOutputBtn.setStyle("-fx-background-color: #e74c3c; -fx-text-fill: white; -fx-background-radius: 5;");
            zippedOutputField.setStyle("-fx-background-color: #d5f4e6; -fx-text-fill: #27ae60; -fx-font-weight: bold;");
        } else {
            toggleZippedOutputBtn.setText("Abilita");
            toggleZippedOutputBtn.setStyle("-fx-background-color: #27ae60; -fx-text-fill: white; -fx-background-radius: 5;");
            zippedOutputField.setStyle("-fx-background-color: #fadbd8; -fx-text-fill: #e74c3c; -fx-font-weight: bold;");
        }
    }

    /**
     * Salva la configurazione modificata.
     */
    @FXML
    private void saveConfiguration(ActionEvent event) {
        //Aggiorna le voci nel JSON
        InstanceConversionContextWriter ccw = new InstanceConversionContextWriter(new File("src/main/java/webService/client/configuration/configFiles/conversionContext.json"));
        ccw.writeIsUnionEnabled(union);
        ccw.writeIsZippedOutput(zippedOutput);
        ccw.writePassword(txtPassword.getText());
        ConversionContextData.update(new ConversionContextInstance(new File("src/main/java/webService/client/configuration/configFiles/conversionContext.json")));
        logger.info("Configurazione di conversione salvata");
        // Chiude la finestra
        dialogStage.close();
    }

    /**
     * Chiude la finestra senza salvare.
     */
    @FXML
    private void cancelAndClose(ActionEvent event) {
        // Controlla se ci sono modifiche non salvate
        if (hasUnsavedChanges()) {
            logger.info("Tentativo di chiusura con modifiche non salvate");
            Alert confirmAlert = new Alert(Alert.AlertType.CONFIRMATION);
            confirmAlert.setTitle("Modifiche non salvate");
            confirmAlert.setHeaderText("Ci sono modifiche non salvate");
            confirmAlert.setContentText("Sei sicuro di voler chiudere senza salvare?");

            confirmAlert.showAndWait().ifPresent(response -> {
                if (response.getButtonData().isDefaultButton()) {
                    dialogStage.close();
                }
            });
        } else {
            logger.info("Finestra di configurazione chiusa senza modifiche");
            dialogStage.close();
        }
    }

    /**
     * Verifica se ci sono modifiche non salvate.
     */
    private boolean hasUnsavedChanges() {
        // Controlla i campi modificabili
        String currentPassword = ConversionContextReader.getPassword();
        boolean currentUnion = ConversionContextReader.getIsUnion();
        boolean currentZippedOutput = ConversionContextReader.getIsZippedOutput();

        return !currentPassword.equals(txtPassword.getText().trim()) ||
                currentUnion != union ||
                currentZippedOutput != zippedOutput;

    }
}
