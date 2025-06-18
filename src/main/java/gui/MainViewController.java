package gui;

import converter.ConverterConfig;
import converter.DirectoryWatcher;
import javafx.application.Platform;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.stage.DirectoryChooser;
import javafx.stage.Stage;
import java.awt.Desktop;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.Optional;
import java.util.Scanner;

import converter.Engine;

public class MainViewController {

    // Riferimenti FXML agli elementi dell'interfaccia
    @FXML private Label statusIndicator;
    @FXML private Label monitoringStatusLabel;
    @FXML private Label applicationLogArea;
    @FXML private Label cartellaMonitorataLabel;
    @FXML private Label cartellaFileConvertitiLabel;
    @FXML private Label cartellaFileFallitiLabel;
    @FXML private Label detectedFilesCounter;
    @FXML private Label successfulConversionsCounter;
    @FXML private Label failedConversionsCounter;
    @FXML private Button MonitoringBtn;
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
    private int fileRicevuti = 0;
    private int fileConvertiti = 0;
    private int fileScartati = 0;

    // Percorsi delle cartelle (verranno caricati dal JSON)
    private String monitoredFolderPath = "Non configurata";
    private String convertedFolderPath = "Non configurata";
    private String failedFolderPath = "Non configurata";
    private Engine engine;
    Thread watcherThread;


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
        if (isMonitoring) {
            watcherThread = new Thread(new DirectoryWatcher(monitoredFolderPath, this));
            watcherThread.start();
        }
    }

    public static String getExtension(File file) {
        String name = file.getName();
        int lastDot = name.lastIndexOf('.');
        if (lastDot == -1 || lastDot == name.length() - 1) {
            return ""; // niente estensione
        }
        return name.substring(lastDot + 1).toLowerCase();
    }

    private void setupEventHandlers() {
        // Handler per il pulsante toggle monitoraggio
        MonitoringBtn.setOnAction(e -> {
            try {
                toggleMonitoring();
            } catch (IOException ex) {
                throw new RuntimeException(ex);
            }
        });

        // Handler per il pulsante configurazione
        configBtn.setOnAction(e -> openConfigurationWindow());

        // Handler per il pulsante exit
        exitBtn.setOnAction(e -> exitApplication());

        // Handler per i pulsanti "Apri cartella"
        openMonitoredFolderBtn.setOnAction(e -> openFolder(monitoredFolderPath));
        openConvertedFolderBtn.setOnAction(e -> openFolder(convertedFolderPath));
        openFailedFolderBtn.setOnAction(e -> openFolder(failedFolderPath));
    }

    @FXML
    private void toggleMonitoring() throws IOException {

        if (isMonitoring) {
            addLogMessage("Monitoraggio fermato.");
            System.out.println("Monitoraggio fermato.");

            detectedFiles = 0;
            successfulConversions = 0;
            failedConversions = 0;
            fileRicevuti = 0;
            fileConvertiti = 0;
            fileScartati = 0;
            detectedFilesCounter.setText("N/A");
            successfulConversionsCounter.setText("N/A");
            failedConversionsCounter.setText("N/A");

            // TODO: Ferma converter.DirectoryWatcher
            watcherThread.interrupt();

        } else {
            addLogMessage("Monitoraggio avviato per: " + monitoredFolderPath);
            // TODO: Avvia converter.DirectoryWatcher
            watcherThread = new Thread(new DirectoryWatcher(monitoredFolderPath, this));
            watcherThread.start();
        }
        isMonitoring = !isMonitoring;
        updateMonitoringStatus();
    }

    public void instanceDialogPanel(Path srcPath){
        // codice per istanziare il dialog
    }

    private String getExtension(Path filePath){
        String path = filePath.toAbsolutePath().toString();
        int lastDotIndex = path.lastIndexOf(".");
        return path.substring(lastDotIndex + 1);
    }

    private void updateMonitoringStatus() {
        if (isMonitoring) {
            // Stato ATTIVO
            monitoringStatusLabel.setText("Monitoraggio: Attivo");
            statusIndicator.setTextFill(javafx.scene.paint.Color.web("#27ae60")); // Verde
            MonitoringBtn.setText("Ferma Monitoraggio");
            MonitoringBtn.setStyle("-fx-background-color: #e74c3c; -fx-text-fill: white; -fx-background-radius: 5;");
        } else {
            // Stato FERMO
            monitoringStatusLabel.setText("Monitoraggio: Fermo");
            statusIndicator.setTextFill(javafx.scene.paint.Color.web("#e74c3c")); // Rosso
            MonitoringBtn.setText("Avvia Monitoraggio");
            MonitoringBtn.setStyle("-fx-background-color: #27ae60; -fx-text-fill: white; -fx-background-radius: 5;");
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

    public static boolean ensureDirectoryExists(String directoryPath) {
        try {
            Path path = Paths.get(directoryPath);

            if (!Files.exists(path)) {
                Files.createDirectories(path); // Crea tutte le cartelle padre se necessario
                System.out.println("Cartella creata: " + directoryPath);
                return true;
            } else if (Files.isDirectory(path)) {
                System.out.println("Cartella già esistente: " + directoryPath);
                return true;
            } else {
                System.err.println("ERRORE: " + directoryPath + " esiste ma non è una cartella!");
                return false;
            }

        } catch (IOException e) {
            System.err.println("ERRORE durante la creazione di: " + directoryPath);
            System.err.println("Dettagli: " + e.getMessage());
            return false;
        } catch (SecurityException e) {
            System.err.println("ERRORE: Permessi insufficienti per: " + directoryPath);
            System.err.println("Dettagli: " + e.getMessage());
            return false;
        }
    }

    private void loadConfiguration() {
        try {
            monitoredFolderPath = engine.getConverterConfig().getMonitoredDir();
            convertedFolderPath = engine.getConverterConfig().getSuccessOutputDir();
            failedFolderPath = engine.getConverterConfig().getErrorOutputDir();

            if(ensureDirectoryExists(monitoredFolderPath)){
                cartellaMonitorataLabel.setText(monitoredFolderPath);
            }

            if(ensureDirectoryExists(convertedFolderPath)){
                cartellaFileConvertitiLabel.setText(convertedFolderPath);
            }

            if(ensureDirectoryExists(failedFolderPath)){
                cartellaFileFallitiLabel.setText(failedFolderPath);
            }

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

    public void launchDialogConversion(File srcFile) {
        Platform.runLater(() -> fileRicevuti++);
        engine = new Engine();
        String srcExtension = getExtension(srcFile);
        System.out.println("Estensione file sorgente: " + srcExtension);
        List<String> formats = null;
        try {
            formats = engine.getPossibleConversions(srcExtension);
        } catch (Exception e) {
            Platform.runLater(() -> fileScartati++);
            launchAlertError("Conversione di " + srcFile.getName() + " non supportata");
            stampaRisultati();
            return;
        }
        System.out.println("prima parte finita");
        List<String> finalFormats = formats;
        Platform.runLater(() -> {
            ChoiceDialog<String> dialog = new ChoiceDialog<>(finalFormats.get(0), finalFormats);
            dialog.setTitle("Seleziona Formato");
            dialog.setHeaderText("Converti " + srcFile.getName() + " in...");
            dialog.setContentText("Formato desiderato:");
            System.out.println("dialog mandato");

            Optional<String> result = dialog.showAndWait();
            result.ifPresent(format -> {
                try {
                    engine.conversione(srcExtension, format, srcFile);
                    fileConvertiti++;

                    launchAlertSuccess(srcFile);
                } catch (Exception e) {
                    fileScartati++;
                    launchAlertError("Conversione di " + srcFile.getName() + " interrotta a causa di un errore");
                }
                stampaRisultati();
            });
        });

    }

    public void stampaRisultati(){
        System.out.println("Ricevuti: " + fileRicevuti);
        System.out.println("Scartati: " + fileScartati);
        System.out.println("Convertiti: " + fileConvertiti);
    }

    public void launchAlertSuccess(File file){
        Platform.runLater(() -> {
            Alert alert = new Alert(Alert.AlertType.INFORMATION);
            alert.setTitle("Conversione eseguita");
            alert.setHeaderText(null);
            alert.setContentText("Conversione di " + file.getName() + " completata con successo!");
            alert.showAndWait();
        });
    }

    public void launchAlertError(String message){
        Platform.runLater(() -> {
            Alert alert = new Alert(Alert.AlertType.INFORMATION);
            alert.setTitle("Conversione interrotta");
            alert.setHeaderText(null); // Nessun header
            alert.setContentText(message);
            alert.showAndWait();
        });
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

    @FXML
    public void openConfig(ActionEvent actionEvent) {
        // codice
    }
}