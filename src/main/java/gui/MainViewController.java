package gui;

import converter.ConverterConfig;
import converter.DirectoryWatcher;
import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.stage.DirectoryChooser;
import javafx.stage.Stage;
import java.awt.Desktop;
import java.io.File;
import java.io.IOException;
import converter.Engine;

public class MainViewController {

    // Riferimenti FXML agli elementi dell'interfaccia
    @FXML private Label statusIndicator;
    @FXML private Label monitoringStatusLabel;
    @FXML private Label applicationLogArea;
    @FXML private Label detectedFilesCounter;
    @FXML private Label successfulConversionsCounter;
    @FXML private Label failedConversionsCounter;
    @FXML private Button toggleMonitoringBtn;
    @FXML private Button configBtn;
    @FXML private Button exitBtn;
    @FXML private Button openMonitoredFolderBtn;
    @FXML private Button openConvertedFolderBtn;
    @FXML private Button openFailedFolderBtn;

    // Riferimento all'applicazione principale
    private MainApp mainApp;

    // Variabili di stato
    private boolean isMonitoring = false;
    private int detectedFiles = 0;
    private int successfulConversions = 0;
    private int failedConversions = 0;

    // Percorsi delle cartelle (verranno caricati dal JSON)
    private String monitoredFolderPath = "Non configurata";
    private String convertedFolderPath = "Non configurata";
    private String failedFolderPath = "Non configurata";
    private Engine engine;
    private DirectoryWatcher directoryWatcher;

    @FXML
    private void initialize() throws IOException {
        engine = new Engine();
        // Inizializza l'interfaccia
        setupEventHandlers();
        updateMonitoringStatus();
        addLogMessage("Applicazione avviata.");
        addLogMessage("Caricamento configurazione...");

        // TODO: Carica configurazione dal JSON
        loadConfiguration();
        directoryWatcher = new DirectoryWatcher(monitoredFolderPath);
    }

    private void setupEventHandlers() {
        // Handler per il pulsante toggle monitoraggio
        toggleMonitoringBtn.setOnAction(e -> toggleMonitoring());

        // Handler per il pulsante configurazione
        configBtn.setOnAction(e -> openConfigurationWindow());

        // Handler per il pulsante exit
        exitBtn.setOnAction(e -> exitApplication());

        // Handler per i pulsanti "Apri cartella"
        openMonitoredFolderBtn.setOnAction(e -> openFolder(monitoredFolderPath));
        openConvertedFolderBtn.setOnAction(e -> openFolder(convertedFolderPath));
        openFailedFolderBtn.setOnAction(e -> openFolder(failedFolderPath));
    }

    private void toggleMonitoring() {
        isMonitoring = !isMonitoring;
        updateMonitoringStatus();

        if (isMonitoring) {
            addLogMessage("Monitoraggio avviato per: " + monitoredFolderPath);
            // TODO: Avvia converter.DirectoryWatcher
        } else {
            addLogMessage("Monitoraggio fermato.");
            // TODO: Ferma converter.DirectoryWatcher
        }
    }

    private void updateMonitoringStatus() {
        if (isMonitoring) {
            // Stato ATTIVO
            monitoringStatusLabel.setText("Monitoraggio: Attivo");
            statusIndicator.setTextFill(javafx.scene.paint.Color.web("#27ae60")); // Verde
            toggleMonitoringBtn.setText("Ferma Monitoraggio");
            toggleMonitoringBtn.setStyle("-fx-background-color: #e74c3c; -fx-text-fill: white; -fx-background-radius: 5;");
        } else {
            // Stato FERMO
            monitoringStatusLabel.setText("Monitoraggio: Fermo");
            statusIndicator.setTextFill(javafx.scene.paint.Color.web("#e74c3c")); // Rosso
            toggleMonitoringBtn.setText("Avvia Monitoraggio");
            toggleMonitoringBtn.setStyle("-fx-background-color: #27ae60; -fx-text-fill: white; -fx-background-radius: 5;");
        }
    }

    private void openConfigurationWindow() {
        // TODO: Implementa finestra di configurazione
        addLogMessage("Apertura finestra configurazione...");
        showAlert("Configurazione", "Finestra di configurazione non ancora implementata.");
    }

    private void openFolder(String folderPath) {
        if (folderPath.equals("Non configurata")) {
            showAlert("Cartella non configurata", "La cartella non è stata ancora configurata.");
            return;
        }

        try {
            File folder = new File(folderPath);
            if (folder.exists() && folder.isDirectory()) {
                Desktop.getDesktop().open(folder);
                addLogMessage("Cartella aperta: " + folderPath);
            } else {
                showAlert("Errore", "La cartella non esiste: " + folderPath);
            }
        } catch (IOException e) {
            showAlert("Errore", "Impossibile aprire la cartella: " + e.getMessage());
        }
    }

    private void loadConfiguration() {
        try {
            monitoredFolderPath = engine.getConverterConfig().getMonitoredDir();
            convertedFolderPath = engine.getConverterConfig().getSuccessOutputDir();
            failedFolderPath = engine.getConverterConfig().getErrorOutputDir();

            addLogMessage("Configurazione caricata da config.json");
            addLogMessage("Cartella monitorata: " + monitoredFolderPath);
        } catch (Exception e) {
            addLogMessage("Errore nel caricamento configurazione: " + e.getMessage());
            showAlert("Errore Configurazione", "Impossibile caricare la configurazione: " + e.getMessage());
        }
    }

    public void addLogMessage(String message) {
        Platform.runLater(() -> {
            String timestamp = java.time.LocalTime.now().toString().substring(0, 8);
            String logEntry = "[" + timestamp + "] " + message;

            if (applicationLogArea.getText().equals("Log dell'applicazione...")) {
                applicationLogArea.setText(logEntry);
            } else {
                applicationLogArea.setText(applicationLogArea.getText() + "\n" + logEntry);
            }
        });
    }

    public void updateCounters(int detected, int successful, int failed) {
        Platform.runLater(() -> {
            this.detectedFiles = detected;
            this.successfulConversions = successful;
            this.failedConversions = failed;

            detectedFilesCounter.setText(detected == 0 ? "N/A" : String.valueOf(detected));
            successfulConversionsCounter.setText(successful == 0 ? "N/A" : String.valueOf(successful));
            failedConversionsCounter.setText(failed == 0 ? "N/A" : String.valueOf(failed));
        });
    }

    public void incrementDetectedFiles() {
        updateCounters(detectedFiles + 1, successfulConversions, failedConversions);
    }

    public void incrementSuccessfulConversions() {
        updateCounters(detectedFiles, successfulConversions + 1, failedConversions);
    }

    public void incrementFailedConversions() {
        updateCounters(detectedFiles, successfulConversions, failedConversions + 1);
    }

    private void exitApplication() {
        addLogMessage("Chiusura applicazione...");
        Platform.exit();
    }

    private void showAlert(String title, String message) {
        Platform.runLater(() -> {
            Alert alert = new Alert(Alert.AlertType.INFORMATION);
            alert.setTitle(title);
            alert.setHeaderText(null);
            alert.setContentText(message);
            alert.showAndWait();
        });
    }

    private Stage getPrimaryStage() {
        return mainApp.getPrimaryStage();
    }

    public void setMainApp(MainApp mainApp) {
        this.mainApp = mainApp;
    }
}