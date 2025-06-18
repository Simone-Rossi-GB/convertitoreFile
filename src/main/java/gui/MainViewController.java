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

    // Riferimenti FXML agli elementi dell'interfaccia - CORRETTI
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

    // Pulsanti con nomi corretti dall'FXML
    @FXML private Button caricaFileBtn;
    @FXML private Button fileConvertitiBtn;
    @FXML private Button conversioniFalliteBtn;

    // Riferimento all'applicazione principale
    private MainApp mainApp;

    // Variabili di stato
    private boolean isMonitoring = false;
    private int fileRicevuti = 0;
    private int fileConvertiti = 0;
    private int fileScartati = 0;

    // Percorsi delle cartelle (caricati dal JSON)
    private String monitoredFolderPath = "Non configurata";
    private String convertedFolderPath = "Non configurata";
    private String failedFolderPath = "Non configurata";
    private Engine engine;
    private Thread watcherThread;

    /**
     * Metodo invocato automaticamente da JavaFX dopo il caricamento del FXML.
     * Inizializza il controller, i listener e carica la configurazione.
     */
    @FXML
    private void initialize() throws IOException {
        engine = new Engine();
        // Inizializza l'interfaccia
        setupEventHandlers();
        updateMonitoringStatus();
        addLogMessage("Applicazione avviata.");
        addLogMessage("Caricamento configurazione...");

        // Carica configurazione dal JSON
        loadConfiguration();
        if (isMonitoring) {
            watcherThread = new Thread(new DirectoryWatcher(monitoredFolderPath, this));
            watcherThread.setDaemon(true);
            watcherThread.start();
        }
    }

    /**
     * Restituisce l'estensione del file.
     * @param file file da cui estrarre l'estensione
     * @return estensione in minuscolo o stringa vuota se non presente
     */
    public static String getExtension(File file) {
        String name = file.getName();
        int lastDot = name.lastIndexOf('.');
        if (lastDot == -1 || lastDot == name.length() - 1) {
            return ""; // nessuna estensione
        }
        return name.substring(lastDot + 1).toLowerCase();
    }

    /**
     * Configura i listener degli eventi sui pulsanti.
     */
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

        // Handler per i pulsanti "Apri cartella" - CORRETTI
        caricaFileBtn.setOnAction(e -> openFolder(monitoredFolderPath));
        fileConvertitiBtn.setOnAction(e -> openFolder(convertedFolderPath));
        conversioniFalliteBtn.setOnAction(e -> openFolder(failedFolderPath));
    }

    /**
     * Attiva o disattiva il monitoraggio della cartella.
     */
    @FXML
    private void toggleMonitoring() throws IOException {
        if (isMonitoring) {
            addLogMessage("Monitoraggio fermato.");
            resetCounters();
            if (watcherThread != null && watcherThread.isAlive()) {
                watcherThread.interrupt();
            }
        } else {
            addLogMessage("Monitoraggio avviato per: " + monitoredFolderPath);
            // Avvia DirectoryWatcher
            watcherThread = new Thread(new DirectoryWatcher(monitoredFolderPath, this));
            watcherThread.setDaemon(true);
            watcherThread.start();
        }
        isMonitoring = !isMonitoring;
        updateMonitoringStatus();
    }

    /**
     * Resetta i contatori di file rilevati e convertiti.
     */
    private void resetCounters() {
        fileRicevuti = 0;
        fileConvertiti = 0;
        fileScartati = 0;

        detectedFilesCounter.setText("N/A");
        successfulConversionsCounter.setText("N/A");
        failedConversionsCounter.setText("N/A");
    }

    /**
     * Aggiorna l'indicatore visivo dello stato di monitoraggio.
     */
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

    /**
     * Apre la finestra di configurazione (ancora da implementare).
     */
    private void openConfigurationWindow() {
        addLogMessage("Apertura finestra configurazione...");
        showAlert("Configurazione", "Finestra di configurazione non ancora implementata.");
    }

    /**
     * Apre la cartella specificata nel file explorer di sistema.
     * @param folderPath percorso della cartella da aprire
     */
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

    /**
     * Verifica che la cartella esista, e in caso la crea.
     * @param directoryPath percorso della cartella
     * @return true se la cartella esiste o è stata creata correttamente, false altrimenti
     */
    public static boolean ensureDirectoryExists(String directoryPath) {
        try {
            Path path = Paths.get(directoryPath);
            if (!Files.exists(path)) {
                Files.createDirectories(path);
                System.out.println("Cartella creata: " + directoryPath);
                return true;
            } else if (Files.isDirectory(path)) {
                System.out.println("Cartella già esistente: " + directoryPath);
                return true;
            } else {
                System.err.println("ERRORE: " + directoryPath + " esiste ma non è una cartella!");
                return false;
            }
        } catch (IOException | SecurityException e) {
            System.err.println("ERRORE durante la creazione di: " + directoryPath + " - " + e.getMessage());
            return false;
        }
    }

    /**
     * Carica la configurazione da file JSON tramite Engine.
     */
    private void loadConfiguration() {
        try {
            monitoredFolderPath = engine.getConverterConfig().getMonitoredDir();
            convertedFolderPath = engine.getConverterConfig().getSuccessOutputDir();
            failedFolderPath = engine.getConverterConfig().getErrorOutputDir();

            if (ensureDirectoryExists(monitoredFolderPath)) {
                cartellaMonitorataLabel.setText(monitoredFolderPath);
            }

            if (ensureDirectoryExists(convertedFolderPath)) {
                cartellaFileConvertitiLabel.setText(convertedFolderPath);
            }

            if (ensureDirectoryExists(failedFolderPath)) {
                cartellaFileFallitiLabel.setText(failedFolderPath);
            }

            addLogMessage("Configurazione caricata da config.json");
            addLogMessage("Cartella monitorata: " + monitoredFolderPath);
            addLogMessage("Cartella file convertiti: " + convertedFolderPath);
            addLogMessage("Cartella file falliti: " + failedFolderPath);
        } catch (Exception e) {
            addLogMessage("Errore nel caricamento configurazione: " + e.getMessage());
            showAlert("Errore Configurazione", "Impossibile caricare la configurazione: " + e.getMessage());
        }
    }

    /**
     * Aggiunge un messaggio al log dell'applicazione.
     * @param message messaggio da aggiungere
     */
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

    /**
     * Termina l'applicazione JavaFX.
     */
    private void exitApplication() {
        addLogMessage("Chiusura applicazione...");
        Platform.exit();
    }

    /**
     * Avvia il dialog per la selezione del formato di conversione di un file.
     * @param srcFile file sorgente da convertire
     */
    public void launchDialogConversion(File srcFile) {
        Platform.runLater(() -> fileRicevuti++);

        String srcExtension = getExtension(srcFile);
        System.out.println("Estensione file sorgente: " + srcExtension);
        List<String> formats = null;
        try {
            formats = engine.getPossibleConversions(srcExtension);
        } catch (Exception e) {
            launchAlertError("Conversione di " + srcFile.getName() + " non supportata");
            Platform.runLater(() -> {
                fileScartati++;
                stampaRisultati();
            });
            return;
        }
        System.out.println("prima parte finita");
        List<String> finalFormats = formats;
        Platform.runLater(() -> {
            ChoiceDialog<String> dialog = new ChoiceDialog<>(finalFormats.get(0), finalFormats);
            dialog.setTitle("Seleziona Formato");
            dialog.setHeaderText("Converti " + srcFile.getName() + " in...");
            dialog.setContentText("Formato desiderato:");

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

    /**
     * Aggiorna i contatori dei risultati a schermo.
     */
    public void stampaRisultati() {
        detectedFilesCounter.setText(Integer.toString(fileRicevuti));
        successfulConversionsCounter.setText(Integer.toString(fileConvertiti));
        failedConversionsCounter.setText(Integer.toString(fileScartati));
    }

    /**
     * Mostra un alert di successo conversione.
     * @param file file convertito con successo
     */
    public void launchAlertSuccess(File file) {
        Platform.runLater(() -> {
            Alert alert = new Alert(Alert.AlertType.INFORMATION);
            alert.setTitle("Conversione eseguita");
            alert.setHeaderText(null);
            alert.setContentText("Conversione di " + file.getName() + " completata con successo!");
            alert.showAndWait();
        });
    }

    /**
     * Mostra un alert di errore conversione.
     * @param message messaggio di errore da mostrare
     */
    public void launchAlertError(String message) {
        Platform.runLater(() -> {
            Alert alert = new Alert(Alert.AlertType.INFORMATION);
            alert.setTitle("Conversione interrotta");
            alert.setHeaderText(null);
            alert.setContentText(message);
            alert.showAndWait();
        });
    }

    /**
     * Mostra un alert informativo generico.
     * @param title titolo finestra alert
     * @param message messaggio alert
     */
    private void showAlert(String title, String message) {
        Platform.runLater(() -> {
            Alert alert = new Alert(Alert.AlertType.INFORMATION);
            alert.setTitle(title);
            alert.setHeaderText(null);
            alert.setContentText(message);
            alert.showAndWait();
        });
    }

    /**
     * Ritorna lo stage principale dell'applicazione.
     * @return stage principale
     */
    private Stage getPrimaryStage() {
        return mainApp.getPrimaryStage();
    }

    /**
     * Setta il riferimento all'app principale.
     * @param mainApp istanza MainApp
     */
    public void setMainApp(MainApp mainApp) {
        this.mainApp = mainApp;
    }

    // Metodi FXML - implementati correttamente
    @FXML
    public void openConfig(ActionEvent actionEvent) {
        openConfigurationWindow();
    }

    @FXML
    public void loadFilesFolder(ActionEvent actionEvent) {
        openFolder(monitoredFolderPath);
    }

    @FXML
    public void openConvertedFolder(ActionEvent actionEvent) {
        openFolder(convertedFolderPath);
    }

    @FXML
    public void openFailedConvertionsFolder(ActionEvent actionEvent) {
        openFolder(failedFolderPath);
    }

    // Metodo alternativo per ottenere estensione da Path (non usato attualmente)
    private String getExtension(Path filePath){
        String path = filePath.toAbsolutePath().toString();
        int lastDotIndex = path.lastIndexOf(".");
        return path.substring(lastDotIndex + 1);
    }
}