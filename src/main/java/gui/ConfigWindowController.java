package gui;

import converter.Engine;
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

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonObject;
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

    @FXML private TextField monitoredDirField;
    @FXML private TextField successDirField;
    @FXML private TextField errorDirField;
    @FXML private TextField monitorAtStartField;
    @FXML private Button browseMonitoredBtn;
    @FXML private Button browseSuccessBtn;
    @FXML private Button browseErrorBtn;
    @FXML private Button toggleMonitorBtn;
    @FXML private TextArea conversionsTextArea;
    @FXML private Label statusLabel;
    @FXML private Button saveButton;
    @FXML private Button cancelButton;

    private Engine engine;
    private Stage dialogStage;
    private MainViewController mainController;
    private boolean monitorAtStart = false;

    /**
     * Inizializza il controller della finestra di configurazione.
     */
    @FXML
    private void initialize() {
        setupTextArea();
        setupDirectoryFields();
    }

    /**
     * Configura l'aspetto del campo conversionsTextArea.
     */
    private void setupTextArea() {
        conversionsTextArea.setStyle("-fx-font-family: 'Consolas'; -fx-font-size: 13px; -fx-background-color: #f8f9fa; -fx-text-fill: #2c3e50;");
        conversionsTextArea.setEditable(false);
    }

    /**
     * Applica stile e validazione ai campi directory.
     */
    private void setupDirectoryFields() {
        TextField[] fields = {monitoredDirField, successDirField, errorDirField};
        for (TextField field : fields) {
            field.setStyle("-fx-background-color: #ffffff; -fx-border-color: #bdc3c7; -fx-border-radius: 5;");
            field.textProperty().addListener((obs, oldVal, newVal) -> validateDirectoryPath(newVal, field));
        }
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
     * Imposta l'istanza dell'engine e il controller principale.
     */
    public void setEngine(Engine engine, MainViewController mainController) {
        this.engine = engine;
        this.mainController = mainController;
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
        try {
            monitoredDirField.setText(engine.getConverterConfig().getMonitoredDir());
            successDirField.setText(engine.getConverterConfig().getSuccessOutputDir());
            errorDirField.setText(engine.getConverterConfig().getErrorOutputDir());

            monitorAtStart = engine.getConverterConfig().getMonitorAtStart();
            monitorAtStartField.setText(String.valueOf(monitorAtStart));
            updateMonitorToggleButton();

            String fullConfig = engine.getConfigAsJson();
            JsonObject configJson = JsonParser.parseString(fullConfig).getAsJsonObject();

            if (configJson.has("conversions")) {
                Gson gson = new GsonBuilder().setPrettyPrinting().create();
                conversionsTextArea.setText(gson.toJson(configJson.get("conversions")));
            }

            updateStatus("Configurazione caricata", false);
        } catch (Exception e) {
            updateStatus("Errore nel caricamento: " + e.getMessage(), true);
            showAlert("Errore", "Impossibile caricare la configurazione: " + e.getMessage(), Alert.AlertType.ERROR);
        }
    }

    @FXML
    private void browseMonitoredDirectory(ActionEvent event) {
        browseDirectory("Seleziona Cartella da Monitorare", monitoredDirField);
    }

    @FXML
    private void browseSuccessDirectory(ActionEvent event) {
        browseDirectory("Seleziona Cartella File Convertiti", successDirField);
    }

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

        File currentDir = new File(targetField.getText().trim());
        if (currentDir.exists()) {
            directoryChooser.setInitialDirectory(currentDir);
        }

        File selected = directoryChooser.showDialog(dialogStage);
        if (selected != null) {
            targetField.setText(selected.getAbsolutePath());
        }
    }

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
        toggleMonitorBtn.setText(monitorAtStart ? "Disabilita" : "Abilita");
        toggleMonitorBtn.setStyle(monitorAtStart ? "-fx-background-color: #e74c3c; -fx-text-fill: white;" : "-fx-background-color: #27ae60; -fx-text-fill: white;");
        monitorAtStartField.setStyle(monitorAtStart ? "-fx-background-color: #d5f4e6; -fx-text-fill: #27ae60;" : "-fx-background-color: #fadbd8; -fx-text-fill: #e74c3c;");
    }

    /**
     * Verifica che tutte le directory siano valide o tenti di crearle se mancanti.
     */
    private boolean validateDirectories() {
        String[][] dirData = {
                {monitoredDirField.getText().trim(), "Cartella Monitorata"},
                {successDirField.getText().trim(), "Cartella Success"},
                {errorDirField.getText().trim(), "Cartella Error"}
        };

        for (String[] pair : dirData) {
            String path = pair[0];
            String label = pair[1];

            if (path.isEmpty()) {
                updateStatus(label + " non specificata!", true);
                showAlert("Errore di validazione", label + " deve essere specificata.", Alert.AlertType.ERROR);
                return false;
            }
            try {
                Path dirPath = Paths.get(path);
                if (!Files.exists(dirPath)) {
                    Files.createDirectories(dirPath);
                    updateStatus("Creata directory: " + path, false);
                }
            } catch (IOException e) {
                updateStatus("Errore nella creazione directory: " + path, true);
                showAlert("Errore", "Impossibile creare la directory: " + path, Alert.AlertType.ERROR);
                return false;
            }
        }
        return true;
    }

    @FXML
    private void saveConfiguration(ActionEvent event) {
        if (!validateDirectories()) return;
        try {
            JsonObject configJson = JsonParser.parseString(engine.getConfigAsJson()).getAsJsonObject();
            configJson.addProperty("successOutputDir", successDirField.getText().trim());
            configJson.addProperty("errorOutputDir", errorDirField.getText().trim());
            configJson.addProperty("monitoredDir", monitoredDirField.getText().trim());
            configJson.addProperty("monitorAtStart", monitorAtStart);

            String jsonOut = new GsonBuilder().setPrettyPrinting().create().toJson(configJson);
            engine.setConfigFromJson(jsonOut);

            updateStatus("Configurazione salvata con successo!", false);
            if (mainController != null) mainController.addLogMessage("Configurazione aggiornata");
            showAlert("Successo", "Configurazione salvata con successo!", Alert.AlertType.INFORMATION);

            Platform.runLater(() -> {
                try { Thread.sleep(1000); } catch (InterruptedException e) { Thread.currentThread().interrupt(); }
                dialogStage.close();
            });

        } catch (Exception e) {
            updateStatus("Errore nel salvataggio: " + e.getMessage(), true);
            showAlert("Errore", "Impossibile salvare: " + e.getMessage(), Alert.AlertType.ERROR);
        }
    }

    @FXML
    private void cancelAndClose(ActionEvent event) {
        if (hasUnsavedChanges()) {
            Alert alert = new Alert(Alert.AlertType.CONFIRMATION);
            alert.setTitle("Modifiche non salvate");
            alert.setContentText("Chiudere senza salvare?");
            alert.showAndWait().ifPresent(response -> {
                if (response.getButtonData().isDefaultButton()) dialogStage.close();
            });
        } else {
            dialogStage.close();
        }
    }

    /**
     * Verifica se ci sono modifiche non salvate nella configurazione.
     */
    private boolean hasUnsavedChanges() {
        try {
            return !engine.getConverterConfig().getMonitoredDir().equals(monitoredDirField.getText().trim()) ||
                    !engine.getConverterConfig().getSuccessOutputDir().equals(successDirField.getText().trim()) ||
                    !engine.getConverterConfig().getErrorOutputDir().equals(errorDirField.getText().trim()) ||
                    engine.getConverterConfig().getMonitorAtStart() != monitorAtStart;
        } catch (Exception e) {
            return true;
        }
    }

    /**
     * Mostra un messaggio nello statusLabel.
     */
    private void updateStatus(String message, boolean isError) {
        Platform.runLater(() -> {
            statusLabel.setText(message);
            statusLabel.setStyle("-fx-text-fill: " + (isError ? "#e74c3c" : "#27ae60") + "; -fx-font-weight: bold;");
        });
    }

    /**
     * Mostra un alert generico.
     */
    private void showAlert(String title, String message, Alert.AlertType type) {
        Platform.runLater(() -> {
            Alert alert = new Alert(type);
            alert.setTitle(title);
            alert.setHeaderText(null);
            alert.setContentText(message);
            alert.showAndWait();
        });
    }
}
