package gui;

import converter.DirectoryWatcher;
import converter.Log;
import converter.Engine;
import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.stage.Modality;
import javafx.stage.Stage;

import java.awt.*;
import java.io.File;
import java.io.IOException;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;

/**
 * Controller principale della UI per la gestione del monitoraggio cartelle e conversione file.
 */
public class MainViewController {

    // ==========================
    // FXML UI references
    // ==========================
    @FXML private Label statusIndicator;
    @FXML private Label monitoringStatusLabel;
    @FXML private Label applicationLogArea;
    @FXML private Label detectedFilesCounter;
    @FXML private Label successfulConversionsCounter;
    @FXML private Label failedConversionsCounter;

    @FXML private Button MonitoringBtn;
    @FXML private Button configBtn;
    @FXML private Button exitBtn;
    @FXML private Button caricaFileBtn;
    @FXML private Button fileConvertitiBtn;
    @FXML private Button conversioniFalliteBtn;

    // ==========================
    // Application state variables
    // ==========================
    private boolean isMonitoring = false;
    private int fileRicevuti = 0;
    private int fileConvertiti = 0;
    private int fileScartati = 0;

    private String monitoredFolderPath = "Non configurata";
    private String convertedFolderPath = "Non configurata";
    private String failedFolderPath = "Non configurata";
    private boolean monitorAtStart;

    private Engine engine;
    private Thread watcherThread;

    private MainApp mainApp;

    @FXML
    private void initialize() throws IOException {
        engine = new Engine();

        setupEventHandlers();
        updateMonitoringStatus();
        addLogMessage("Applicazione avviata.");
        addLogMessage("Caricamento configurazione...");
        loadConfiguration();

        if (monitorAtStart) {
            toggleMonitoring();
        }
    }

    private void setupEventHandlers() {
        MonitoringBtn.setOnAction(e -> {
            try {
                toggleMonitoring();
            } catch (IOException ex) {
                Log.addMessage("ERRORE: monitoraggio fallito : " + ex.getMessage());
                launchAlertError("Errore durante il monitoraggio: " + ex.getMessage());
            }
        });
        AtomicReference<Stage> stage = new AtomicReference<>();
        configBtn.setOnAction(e -> openConfigurationWindow());
        stage.get().setOnCloseRequest(e -> exitApplication());
        caricaFileBtn.setOnAction(e -> openFolder(monitoredFolderPath));
        fileConvertitiBtn.setOnAction(e -> openFolder(convertedFolderPath));
        conversioniFalliteBtn.setOnAction(e -> openFolder(failedFolderPath));
    }

    @FXML
    private void toggleMonitoring() throws IOException {
        if (engine == null || monitoredFolderPath == null) {
            Log.addMessage("ERRORE: Engine o cartella monitorata non inizializzati");
            launchAlertError("Engine o cartella monitorata non inizializzati.");
            return;
        }

        if (isMonitoring) {
            Log.addMessage("Monitoraggio fermato");
            addLogMessage("Monitoraggio fermato.");
            resetCounters();
            if (watcherThread != null && watcherThread.isAlive()) {
                watcherThread.interrupt();
            }
        } else {
            Log.addMessage("Monitoraggio avviato per: " + monitoredFolderPath);
            addLogMessage("Monitoraggio avviato per: " + monitoredFolderPath);
            watcherThread = new Thread(new DirectoryWatcher(monitoredFolderPath, this));
            watcherThread.setDaemon(true);
            watcherThread.start();
            resetCounters();
        }
        isMonitoring = !isMonitoring;
        updateMonitoringStatus();
    }

    private void resetCounters() {
        fileRicevuti = 0;
        fileConvertiti = 0;
        fileScartati = 0;
        Platform.runLater(() -> {
            detectedFilesCounter.setText("0");
            successfulConversionsCounter.setText("0");
            failedConversionsCounter.setText("0");
        });
    }

    private void updateMonitoringStatus() {
        Platform.runLater(() -> {
            if (isMonitoring) {
                monitoringStatusLabel.setText("Monitoraggio: Attivo");
                statusIndicator.setTextFill(javafx.scene.paint.Color.web("#27ae60"));
                MonitoringBtn.setText("Ferma Monitoraggio");
                MonitoringBtn.setStyle("-fx-background-color: #e74c3c; -fx-text-fill: white; -fx-background-radius: 5;");
            } else {
                monitoringStatusLabel.setText("Monitoraggio: Fermo");
                statusIndicator.setTextFill(javafx.scene.paint.Color.web("#e74c3c"));
                MonitoringBtn.setText("Avvia Monitoraggio");
                MonitoringBtn.setStyle("-fx-background-color: #27ae60; -fx-text-fill: white; -fx-background-radius: 5;");
            }
        });
    }

    private void loadConfiguration() {
        if (engine == null) {
            Log.addMessage("ERRORE: Engine non inizializzato.");
            launchAlertError("Engine non inizializzato.");
            return;
        }
        try {
            if (engine.getConverterConfig() == null) {
                Log.addMessage("ERRORE: Configurazione non trovata.");
                launchAlertError("Configurazione non trovata.");
                return;
            }
            monitoredFolderPath = engine.getConverterConfig().getMonitoredDir();
            convertedFolderPath = engine.getConverterConfig().getSuccessOutputDir();
            failedFolderPath = engine.getConverterConfig().getErrorOutputDir();
            monitorAtStart = engine.getConverterConfig().getMonitorAtStart();

            addLogMessage("Configurazione caricata da config.json");
            addLogMessage("Cartella monitorata: " + monitoredFolderPath);
            addLogMessage("Cartella file convertiti: " + convertedFolderPath);
            addLogMessage("Cartella file falliti: " + failedFolderPath);
            Log.addMessage("Configurazione caricata da config.json");
            Log.addMessage("Cartella monitorata: " + monitoredFolderPath);
            Log.addMessage("Cartella file convertiti: " + convertedFolderPath);
            Log.addMessage("Cartella file falliti: " + failedFolderPath);
        } catch (Exception e) {
            Log.addMessage("ERRORE: caricamento della configurazione fallita:" + e.getMessage());
            addLogMessage("Errore nel caricamento configurazione: " + e.getMessage());
            launchAlertError("Impossibile caricare la configurazione: " + e.getMessage());
        }
    }

    private void openConfigurationWindow() {
        if (engine == null) {
            Log.addMessage("ERRORE: Engine non inizializzato.");
            launchAlertError("Engine non inizializzato.");
            return;
        }
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/ConfigWindow.fxml"));
            Parent configWindow = loader.load();

            Stage configStage = new Stage();
            configStage.setTitle("Editor Configurazione");
            configStage.initModality(Modality.WINDOW_MODAL);
            configStage.initOwner(getPrimaryStage());
            configStage.setResizable(true);
            configStage.setMinWidth(700);
            configStage.setMinHeight(600);
            configStage.setScene(new Scene(configWindow));

            ConfigWindowController controller = loader.getController();
            controller.setDialogStage(configStage);
            controller.setEngine(engine, this);

            configStage.showAndWait();
            loadConfiguration();
        } catch (IOException e) {
            Log.addMessage("ERRORE: Impossibile aprire l'editor di configurazione: " + e.getMessage());
            launchAlertError("Impossibile aprire l'editor di configurazione: " + e.getMessage());
        }
    }

    private void openFolder(String folderPath) {
        if (folderPath == null || "Non configurata".equals(folderPath)) {
            Log.addMessage("ERRORE: La cartella non è stata ancora configurata.");
            launchAlertError("La cartella non è stata ancora configurata.");
            return;
        }
        File folder = new File(folderPath);
        if (!folder.exists() || !folder.isDirectory()) {
            Log.addMessage("ERRORE: La cartella non esiste " + folderPath);
            launchAlertError("La cartella non esiste: " + folderPath);
            return;
        }
        try {
            Desktop.getDesktop().open(folder);
        } catch (IOException e) {
            Log.addMessage("ERRORE: Impossibile aprire la cartella: " + e.getMessage());
            launchAlertError("Impossibile aprire la cartella: " + e.getMessage());
        }
    }

    public void addLogMessage(String message) {
        Platform.runLater(() -> {
            String timestamp = java.time.LocalTime.now().toString().substring(0, 8);
            String logEntry = "[" + timestamp + "] " + message;
            if ("Log dell'applicazione...".equals(applicationLogArea.getText())) {
                applicationLogArea.setText(logEntry);
            } else {
                applicationLogArea.setText(applicationLogArea.getText() + "\n" + logEntry);
            }
        });
    }

    private void exitApplication() {
        addLogMessage("Chiusura applicazione...");
        watcherThread.interrupt();
        try {
            watcherThread.join();

        } catch (InterruptedException e) {
            String msg = "Thread in background interrotto in maniera anomala";
            launchAlertError(msg);
            Log.addMessage(msg);
        }

        Platform.exit();
    }

    public static String getExtension(File file) {
        if (file == null) return "";
        String name = file.getName();
        int lastDot = name.lastIndexOf('.');
        if (lastDot == -1 || lastDot == name.length() - 1) return "";
        return name.substring(lastDot + 1).toLowerCase();
    }

    public void launchDialogConversion(File srcFile) {
        List<String> formatiImmagini = Arrays.asList("jpeg", "png", "bmp", "gif", "tiff", "jpg", "webp", "psd", "ico", "icns", "tga", "pnm", "pbm", "pgm", "ppm", "pam", "iff", "xwd");
        if (srcFile == null || engine == null) {
            Log.addMessage("ERRORE: File sorgente o Engine non valido.");
            launchAlertError("File sorgente o Engine non valido.");
            return;
        }

        AtomicBoolean unisci = new AtomicBoolean(false);
        Platform.runLater(() -> fileRicevuti++);

        String srcExtension = getExtension(srcFile);
        List<String> formats;
        try {
            formats = engine.getPossibleConversions(srcExtension);
        } catch (Exception e) {
            Log.addMessage("ERRORE: Conversione non supportata per il file " + srcFile.getName() + " (" + srcExtension + ")");
            launchAlertError("Conversione di " + srcFile.getName() + " non supportata");
            Platform.runLater(() -> {
                fileScartati++;
                stampaRisultati();
            });
            return;
        }

        Platform.runLater(() -> {
            ChoiceDialog<String> dialog = new ChoiceDialog<>(formats.get(0), formats);
            dialog.setTitle("Seleziona Formato");
            dialog.setHeaderText("Converti " + srcFile.getName() + " in...");
            dialog.setContentText("Formato desiderato:");

            Optional<String> result = dialog.showAndWait();
            result.ifPresent(format -> {
                Log.addMessage("Formato selezionato: " + format + " per il file " + srcFile.getName());
                try {
                    if ("pdf".equals(srcExtension)) {
                        if ("jpg".equals(format)) {
                            unisci.set(launchDialogUnisci());
                            Log.addMessage("Unione JPG selezionata: " + unisci.get());
                        }
                        String password = launchDialogPdf();
                        if (password != null) {
                            Log.addMessage("Password inserita per PDF: " + (password.isEmpty() ? "(vuota)" : "(oculta)"));
                            if ("jpg".equals(format))
                                engine.conversione(srcExtension, format, srcFile, password, unisci.get());
                            else
                                engine.conversione(srcExtension, format, srcFile, password);
                        } else {
                            Log.addMessage("Nessuna password inserita per il PDF.");
                            if ("jpg".equals(format))
                                engine.conversione(srcExtension, format, srcFile, "", unisci.get());
                            else
                                engine.conversione(srcExtension, format, srcFile);
                        }
                    } else {
                        if(formatiImmagini.contains(srcExtension)){
                            engine.conversione(srcExtension, format, srcFile, format);
                        }else {
                            engine.conversione(srcExtension, format, srcFile);
                        }
                    }
                    Log.addMessage("Conversione completata: " + srcFile.getName() + " → " + format);
                    fileConvertiti++;
                    stampaRisultati();
                    launchAlertSuccess(srcFile);
                } catch (Exception e) {
                    e.printStackTrace();
                    Log.addMessage("ERRORE: Impossibile convertire " + srcFile.getName() + ": " + e.getMessage());
                    launchAlertError(e.getMessage());
                    fileScartati++;
                    stampaRisultati();
                }
            });
        });
    }

    public void launchAlertError(String message) {
        Log.addMessage("ERRORE: " + message);
        showAlert("Errore", message, Alert.AlertType.ERROR);
    }

    public void launchAlertSuccess(File file) {
        String message = "Conversione di " + file.getName() + " riuscita";
        Log.addMessage(message);
        showAlert("Conversione riuscita", message, Alert.AlertType.INFORMATION);
    }


    private void showAlert(String title, String message, Alert.AlertType tipo) {
        Platform.runLater(() -> {
            Alert alert = new Alert(tipo);
            alert.setTitle(title);
            alert.setHeaderText(null);
            alert.setContentText(message);
            alert.showAndWait();
        });
    }

    public boolean launchDialogUnisci() {
        AtomicBoolean unisci = new AtomicBoolean(false);

        Alert alert = new Alert(Alert.AlertType.CONFIRMATION);
        alert.setTitle("Unisci PDF");
        alert.setHeaderText(null);
        alert.setContentText("Vuoi unire le pagine in un'unica immagine JPG?");

        ButtonType siBtn = new ButtonType("Si");
        ButtonType noBtn = new ButtonType("No");
        alert.getButtonTypes().setAll(siBtn, noBtn);

        Optional<ButtonType> result = alert.showAndWait();
        if (result.isPresent() && result.get() == siBtn) {
            unisci.set(true);
        }
        Log.addMessage("Scelta unione JPG: " + unisci.get());

        return unisci.get();
    }


    public String launchDialogPdf() {
        TextInputDialog dialog = new TextInputDialog();
        dialog.setTitle("Password PDF");
        dialog.setHeaderText("Inserisci la password per il PDF (se richiesta)");
        dialog.setContentText("Password:");

        Optional<String> result = dialog.showAndWait();
        result.ifPresent(pwd -> Log.addMessage("Password ricevuta: " + (pwd.isEmpty() ? "(vuota)" : "(oculta)")));
        return result.orElse(null);
    }


    public void stampaRisultati() {
        Log.addMessage("Stato: ricevuti=" + fileRicevuti + ", convertiti=" + fileConvertiti + ", scartati=" + fileScartati);
        Platform.runLater(() -> {
            detectedFilesCounter.setText(String.valueOf(fileRicevuti));
            successfulConversionsCounter.setText(String.valueOf(fileConvertiti));
            failedConversionsCounter.setText(String.valueOf(fileScartati));
        });
    }


    private Stage getPrimaryStage() {
        return (Stage) MonitoringBtn.getScene().getWindow();
    }

    public void setMainApp(MainApp mainApp) {
        this.mainApp = mainApp;
    }
}
