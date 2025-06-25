package gui;

import configuration.configExceptions.JsonStructureException;
import configuration.configHandlers.config.*;
import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.LogManager;
import javafx.application.Platform;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.TextArea;
import javafx.scene.control.TextField;
import javafx.stage.DirectoryChooser;
import javafx.stage.Stage;

import com.google.gson.JsonParser;
import com.google.gson.JsonSyntaxException;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;


/**
 * Controller della finestra di configurazione dell'applicazione.
 * Gestisce le directory di monitoraggio, successo, errore, il flag di avvio automatico
 * e la visualizzazione della sezione "conversions" del file di configurazione JSON.
 */
public class ConfigWindowController {

    // Campi per le directory
    @FXML private TextField monitoredDirField;
    @FXML private TextField successDirField;
    @FXML private TextField errorDirField;
    @FXML private TextField monitorAtStartField;

    // Pulsanti per sfogliare le directory
    @FXML private Button browseMonitoredBtn;
    @FXML private Button browseSuccessBtn;
    @FXML private Button browseErrorBtn;
    @FXML private Button toggleMonitorBtn;

    // Area per le conversioni JSON (sola lettura)
    @FXML private TextArea conversionsTextArea;
    @FXML private Label monitoringStatusLabel;
    @FXML private Button saveButton;
    @FXML private Button cancelButton;

    private Stage dialogStage;
    private boolean monitorAtStart = false;
    private static final Logger logger = LogManager.getLogger(ConfigWindowController.class);

    /**
     * Inizializza il controller della finestra di configurazione.
     */
    @FXML
    private void initialize() {
        // Configura il TextArea per la visualizzazione (sola lettura)
        conversionsTextArea.setStyle("-fx-font-family: 'Consolas', 'Monaco', monospace; " +
                "-fx-font-size: 13px; " +
                "-fx-background-color: #f8f9fa; " +
                "-fx-text-fill: #2c3e50; " +
                "-fx-border-color: transparent;");
        conversionsTextArea.setEditable(false);

        // Configura i campi di testo per le directory
        setupDirectoryFields();
        loadCurrentConfiguration();
    }

    /**
     * Applica stile e validazione ai campi directory.
     */
    private void setupDirectoryFields() {
        monitoredDirField.setStyle("-fx-background-color: #ffffff; -fx-border-color: #bdc3c7; -fx-border-radius: 5;");
        successDirField.setStyle("-fx-background-color: #ffffff; -fx-border-color: #bdc3c7; -fx-border-radius: 5;");
        errorDirField.setStyle("-fx-background-color: #ffffff; -fx-border-color: #bdc3c7; -fx-border-radius: 5;");

        // Listener per validazione dei percorsi in tempo reale
        monitoredDirField.textProperty().addListener((obs, oldVal, newVal) -> validateDirectoryPath(newVal, monitoredDirField));
        successDirField.textProperty().addListener((obs, oldVal, newVal) -> validateDirectoryPath(newVal, successDirField));
        errorDirField.textProperty().addListener((obs, oldVal, newVal) -> validateDirectoryPath(newVal, errorDirField));
    }

    /**
     * Valida un percorso di directory e applica uno stile coerente al campo.
     */
    private void validateDirectoryPath(String path, TextField field) {
        if (path == null || path.trim().isEmpty()) {
            field.setStyle("-fx-background-color: #ffffff; -fx-border-color: #bdc3c7; -fx-border-radius: 5;");
            return;
        }
        File dir = new File(path.trim());
        if (dir.exists() && dir.isDirectory()) {
            field.setStyle("-fx-background-color: #d5f4e6; -fx-border-color: #27ae60; -fx-border-radius: 5;");
        } else {
            field.setStyle("-fx-background-color: #fadbd8; -fx-border-color: #e74c3c; -fx-border-radius: 5;");
        }
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
        monitoredDirField.setText(ConfigReader.getMonitoredDir());
        successDirField.setText(ConfigReader.getSuccessOutputDir());
        errorDirField.setText(ConfigReader.getErrorOutputDir());

        monitorAtStart = ConfigReader.getIsMonitoringEnabledAtStart();
        monitorAtStartField.setText(String.valueOf(monitorAtStart));
        updateMonitorToggleButton();

        updateStatus("Configurazione caricata", false);
        logger.info("Configurazione caricata correttamente");
    }

    /**
     * Sfoglia per selezionare la directory monitorata.
     */
    @FXML
    private void browseMonitoredDirectory(ActionEvent event) {
        browseDirectory("Seleziona Cartella da Monitorare", monitoredDirField);
    }

    /**
     * Sfoglia per selezionare la directory di successo.
     */
    @FXML
    private void browseSuccessDirectory(ActionEvent event) {
        browseDirectory("Seleziona Cartella File Convertiti", successDirField);
    }

    /**
     * Sfoglia per selezionare la directory degli errori.
     */
    @FXML
    private void browseErrorDirectory(ActionEvent event) {
        browseDirectory("Seleziona Cartella Errori", errorDirField);
    }

    /**
     * Mostra un DirectoryChooser e imposta il percorso selezionato nel campo corrispondente.
     */
    private void browseDirectory(String title, TextField targetField) {
        DirectoryChooser directoryChooser = new DirectoryChooser();
        directoryChooser.setTitle(title);

        // Imposta la directory iniziale se il campo contiene un percorso valido
        String currentPath = targetField.getText().trim();
        if (!currentPath.isEmpty()) {
            File currentDir = new File(currentPath);
            if (currentDir.exists() && currentDir.isDirectory()) {
                directoryChooser.setInitialDirectory(currentDir);
            }
        }

        File selectedDirectory = directoryChooser.showDialog(dialogStage);
        if (selectedDirectory != null) {
            logger.info("Selezionata directory: {}", selectedDirectory.getAbsolutePath());
            targetField.setText(selectedDirectory.getAbsolutePath());
        }
    }

    /**
     * Cambia il valore del monitoraggio all'avvio.
     */
    @FXML
    private void toggleMonitorAtStart(ActionEvent event) {
        monitorAtStart = !monitorAtStart;
        monitorAtStartField.setText(String.valueOf(monitorAtStart));
        updateMonitorToggleButton();
    }

    /**
     * Aggiorna l'interfaccia in base allo stato monitorAtStart.
     */
    private void updateMonitorToggleButton() {
        if (monitorAtStart) {
            toggleMonitorBtn.setText("Disabilita");
            toggleMonitorBtn.setStyle("-fx-background-color: #e74c3c; -fx-text-fill: white; -fx-background-radius: 5;");
            monitorAtStartField.setStyle("-fx-background-color: #d5f4e6; -fx-text-fill: #27ae60; -fx-font-weight: bold;");
        } else {
            toggleMonitorBtn.setText("Abilita");
            toggleMonitorBtn.setStyle("-fx-background-color: #27ae60; -fx-text-fill: white; -fx-background-radius: 5;");
            monitorAtStartField.setStyle("-fx-background-color: #fadbd8; -fx-text-fill: #e74c3c; -fx-font-weight: bold;");
        }
    }

    /**
     * Verifica che tutte le directory siano valide o tenti di crearle se mancanti.
     */
    private boolean validateDirectories() throws IOException {
        String[] directories = {
                monitoredDirField.getText().trim(),
                successDirField.getText().trim(),
                errorDirField.getText().trim()
        };

        String[] labels = {
                "Cartella Monitorata",
                "Cartella Success",
                "Cartella Error"
        };

        for (int i = 0; i < directories.length; i++) {
            if (directories[i].isEmpty()) {
                updateStatus(labels[i] + " non specificata!", true);
                logger.error("{} non specificata", labels[i]);
                showAlert("Errore di validazione", labels[i] + " deve essere specificata.", Alert.AlertType.ERROR);
                return false;
            }

            // Crea la directory se non esiste
            try {
                Path path = Paths.get(directories[i]);
                if (!Files.exists(path)) {
                    Files.createDirectories(path);
                    logger.warn("Creata directory mancante: {}", path);
                    updateStatus("Creata directory: " + directories[i], false);
                }
            } catch (IOException e) {
                throw new IOException("Impossibile creare la cartella");
            }
        }
        return true;
    }

    /**
     * Salva la configurazione modificata.
     */
    @FXML
    private void saveConfiguration(ActionEvent event) throws IOException {
        // Valida le directory prima di salvare
        if (!validateDirectories()) {
            return;
        }

        InstanceConfigWriter wr = new InstanceConfigWriter(ConfigData.getJsonFile());
        wr.writeMonitoredDir(monitoredDirField.getText());
        wr.writeErrorOutputDir(errorDirField.getText());
        wr.writeSuccessOutputDir(successDirField.getText());
        wr.writeIsMonitoringEnabledAtStart(monitorAtStart);
        ConfigData.update(new ConfigInstance(ConfigData.getJsonFile()));
        logger.info("Configurazione salvata con successo");
        updateStatus("Configurazione salvata con successo!", false);
        dialogStage.close();

    }

    /**
     * Verifica se una stringa è un JSON valido.
     */
    private boolean isValidJson(String jsonString) {
        try {
            JsonParser.parseString(jsonString);
            return true;
        } catch (JsonSyntaxException e) {
            return false;
        }
    }



    /**
     * Chiude la finestra senza salvare.
     */
    @FXML
    private void cancelAndClose(ActionEvent event) {
        try {
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
        } catch (NullPointerException e) {
            dialogStage.close();
            showAlert("Attenzione", "Impossibile verificare le modifiche. Chiudo la finestra.", Alert.AlertType.WARNING);
        }
    }

    /**
     * Verifica se ci sono modifiche non salvate.
     */
    private boolean hasUnsavedChanges() {
        try {
            // Controlla i campi directory
            String currentMonitored = ConfigReader.getMonitoredDir();
            String currentSuccess = ConfigReader.getSuccessOutputDir();
            String currentError = ConfigReader.getErrorOutputDir();
            boolean currentMonitorAtStart = ConfigReader.getIsMonitoringEnabledAtStart();

            return !currentMonitored.equals(monitoredDirField.getText().trim()) ||
                    !currentSuccess.equals(successDirField.getText().trim()) ||
                    !currentError.equals(errorDirField.getText().trim()) ||
                    currentMonitorAtStart != monitorAtStart;

        } catch (JsonStructureException e) {
            return true; // In caso di errore, assumi che ci siano modifiche
        }
    }

    /**
     * Aggiorna il label di stato.
     */
    private void updateStatus(String message, boolean isError) {
        Platform.runLater(() -> {
            monitoringStatusLabel.setText(message);
            if (isError) {
                monitoringStatusLabel.setStyle("-fx-text-fill: #e74c3c; -fx-font-weight: bold;");
            } else {
                monitoringStatusLabel.setStyle("-fx-text-fill: #27ae60; -fx-font-weight: bold;");
            }
        });
    }

    /**
     * Mostra un alert.
     */
    private void showAlert(String title, String message, Alert.AlertType alertType) {
        Platform.runLater(() -> {
            Alert alert = new Alert(alertType);
            alert.setTitle(title);
            alert.setHeaderText(null);
            alert.setContentText(message);
            alert.showAndWait();
        });
    }
}
