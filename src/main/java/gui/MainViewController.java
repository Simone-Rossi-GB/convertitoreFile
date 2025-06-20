package gui;

import com.azul.crs.client.service.GCLogMonitor;
import converter.DirectoryWatcher;
import converter.Log;
import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.LogManager;
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
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicBoolean;
import java.nio.file.StandardCopyOption;

import WebService.client.ConverterWebServiceClient;
import WebService.client.ConversionResult;

/**
 * Controller principale della UI per la gestione del monitoraggio cartelle e conversione file.
 */
public class MainViewController {

    @FXML
    private Label statusIndicator;
    @FXML
    private Label monitoringStatusLabel;
    @FXML
    private Label applicationLogArea;
    @FXML
    private Label detectedFilesCounter;
    @FXML
    private Label successfulConversionsCounter;
    @FXML
    private Label failedConversionsCounter;
    @FXML
    private Button MonitoringBtn;
    @FXML
    private Button configBtn;
    @FXML
    private Button exitBtn;
    @FXML
    private Button caricaFileBtn;
    @FXML
    private Button fileConvertitiBtn;
    @FXML
    private Button conversioniFalliteBtn;

    // Riferimento all'applicazione principale
    private MainApp mainApp;
    private static final Logger logger = LogManager.getLogger(MainViewController.class);

    // Variabili di stato
    private boolean isMonitoring = false;
    private int fileRicevuti = 0;
    private int fileConvertiti = 0;
    private int fileScartati = 0;

    private ConverterWebServiceClient webServiceClient;
    private boolean useWebService = false;

    private String monitoredFolderPath = "Non configurata";
    private String convertedFolderPath = "Non configurata";
    private String failedFolderPath = "Non configurata";
    private boolean monitorAtStart;

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
        logger.info("Applicazione avviata.");
        Log.addMessage("Applicazione avviata.");
        logger.info("Caricamento configurazione...");
        Log.addMessage("Caricamento configurazione...");

        webServiceClient = new ConverterWebServiceClient("http://localhost:8080");

        loadConfiguration();

        if (monitorAtStart) {
            toggleMonitoring();
        }
    }

    /**
     * Restituisce l'estensione del file.
     *
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
                logger.error("monitoraggio fallito : {}", ex.getMessage());
                Log.addMessage("ERRORE: monitoraggio fallito : " + ex.getMessage());
                launchAlertError("Errore durante il monitoraggio: " + ex.getMessage());
            }
        });

        Stage stage = MainApp.getPrimaryStage();
        exitBtn.setOnAction(e -> exitApplication());
        stage.setOnCloseRequest(e -> interruptWatcher());
        configBtn.setOnAction(e -> openConfigurationWindow());
        caricaFileBtn.setOnAction(e -> openFolder(monitoredFolderPath));
        fileConvertitiBtn.setOnAction(e -> openFolder(convertedFolderPath));
        conversioniFalliteBtn.setOnAction(e -> openFolder(failedFolderPath));
    }

    /**
     * Attiva o disattiva il monitoraggio della cartella.
     */
    @FXML
    private void toggleMonitoring() throws IOException {
        if (engine == null || monitoredFolderPath == null) {
            logger.error("Engine o cartella monitorata non inizializzati");
            Log.addMessage("ERRORE: Engine o cartella monitorata non inizializzati");
            launchAlertError("Engine o cartella monitorata non inizializzati.");
            return;
        }

        if (isMonitoring) {
            logger.info("Monitoraggio fermato");
            Log.addMessage("Monitoraggio fermato");
            addLogMessage("Monitoraggio fermato.");
            resetCounters();
            if (watcherThread != null && watcherThread.isAlive()) {
                watcherThread.interrupt();
            }
        } else {
            logger.info("Monitoraggio avviato per: {}", monitoredFolderPath);
            Log.addMessage("Monitoraggio avviato per: " + monitoredFolderPath);
            addLogMessage("Monitoraggio avviato per: " + monitoredFolderPath);
            // Avvia DirectoryWatcher
            watcherThread = new Thread(new DirectoryWatcher(monitoredFolderPath, this));
            watcherThread.start();
            resetCounters();
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
        Platform.runLater(() -> {
            detectedFilesCounter.setText("0");
            successfulConversionsCounter.setText("0");
            failedConversionsCounter.setText("0");
        });
    }

    /**
     * Aggiorna l'indicatore visivo dello stato di monitoraggio.
     */
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
            logger.fatal("Engine non inizializzato.");
            Log.addMessage("ERRORE: Engine non inizializzato.");
            launchAlertError("Engine non inizializzato.");
            return;
        }
        try {
            if (engine.getConverterConfig() == null) {
                logger.error("Configurazione non trovata.");
                Log.addMessage("ERRORE: Configurazione non trovata.");
                launchAlertError("Configurazione non trovata.");
                return;
            }
            monitoredFolderPath = engine.getConverterConfig().getMonitoredDir();
            checkAndCreateFolder(monitoredFolderPath);
            convertedFolderPath = engine.getConverterConfig().getSuccessOutputDir();
            checkAndCreateFolder(convertedFolderPath);
            failedFolderPath = engine.getConverterConfig().getErrorOutputDir();
            checkAndCreateFolder(failedFolderPath);
            checkAndCreateFolder("src/temp");
            monitorAtStart = engine.getConverterConfig().getMonitorAtStart();

            addLogMessage("Configurazione caricata da config.json");
            addLogMessage("Cartella monitorata: " + monitoredFolderPath);
            addLogMessage("Cartella file convertiti: " + convertedFolderPath);
            addLogMessage("Cartella file falliti: " + failedFolderPath);
            logger.info("Configurazione caricata da config.json");
            logger.info("Cartella monitorata: " + monitoredFolderPath);
            logger.info("Cartella file convertiti: " + convertedFolderPath);
            logger.info("Cartella file falliti: " + failedFolderPath);
            Log.addMessage("Configurazione caricata da config.json");
            Log.addMessage("Cartella monitorata: " + monitoredFolderPath);
            Log.addMessage("Cartella file convertiti: " + convertedFolderPath);
            Log.addMessage("Cartella file falliti: " + failedFolderPath);
        } catch (Exception e) {
            logger.error("caricamento della configurazione fallita:{}", e.getMessage());
            Log.addMessage("ERRORE: caricamento della configurazione fallita:" + e.getMessage());
            addLogMessage("Errore nel caricamento configurazione: " + e.getMessage());
            launchAlertError("Impossibile caricare la configurazione: " + e.getMessage());
        }
    }
    // Metodo che controlla l'esistenza di una directory e se non esiste la crea
    private void checkAndCreateFolder(String path) {
        File folder = new File(path);
        if (!folder.exists()) {
            boolean created = folder.mkdirs();
            if (created) {
                logger.warn("Cartella mancante creata: {}", path);
                Log.addMessage("Cartella mancante creata: " + path);
            } else {
                logger.error("Impossibile creare la cartella: {}", path);
                Log.addMessage("Impossibile creare la cartella: " + path);
            }
        }
    }


    private void openConfigurationWindow() {
        if (engine == null) {
            logger.fatal("Engine non inizializzato.");
            Log.addMessage("ERRORE: Engine non inizializzato.");
            launchAlertError("Engine non inizializzato.");
            return;
        }
        try {
            addLogMessage("Apertura editor configurazione...");

            FXMLLoader loader = new FXMLLoader();
            loader.setLocation(getClass().getResource("/ConfigWindow.fxml"));
            Parent configWindow = loader.load();

            // Crea lo stage per la finestra di configurazione
            Stage configStage = new Stage();
            configStage.setTitle("Editor Configurazione");
            configStage.initModality(Modality.WINDOW_MODAL);
            configStage.initOwner(MainApp.getPrimaryStage());
            configStage.setResizable(true);
            configStage.setMinWidth(700);
            configStage.setMinHeight(600);
            configStage.setScene(new Scene(configWindow));

            // Ottieni il controller e passa i riferimenti necessari
            ConfigWindowController controller = loader.getController();
            controller.setDialogStage(configStage);
            controller.setEngine(engine, this);

            // Mostra la finestra e attendi la chiusura
            addLogMessage("Editor configurazione aperto");
            configStage.showAndWait();

            // Ricarica la configurazione dopo la chiusura della finestra
            addLogMessage("Editor configurazione chiuso");
            loadConfiguration();
            if (watcherThread != null && watcherThread.isAlive()) {
                watcherThread.interrupt();
                watcherThread = new Thread(new DirectoryWatcher(monitoredFolderPath, this));
                watcherThread.start();
            }

        } catch (IOException e) {
            logger.error("Impossibile aprire l'editor di configurazione: {}", e.getMessage());
            Log.addMessage("ERRORE: Impossibile aprire l'editor di configurazione: " + e.getMessage());
            launchAlertError("Impossibile aprire l'editor di configurazione: " + e.getMessage());
        }
    }

    /**
     * Apre la cartella specificata nel file explorer di sistema.
     *
     * @param folderPath percorso della cartella da aprire
     */
    private void openFolder(String folderPath) {
        if (folderPath == null || "Non configurata".equals(folderPath)) {
            logger.error("La cartella non è stata ancora configurata.");
            Log.addMessage("ERRORE: La cartella non è stata ancora configurata.");
            launchAlertError("La cartella non è stata ancora configurata.");
            return;
        }
        File folder = new File(folderPath);
        if (!folder.exists() || !folder.isDirectory()) {
            logger.error("La cartella non esiste {}", folderPath);
            Log.addMessage("ERRORE: La cartella non esiste " + folderPath);
            launchAlertError("La cartella non esiste: " + folderPath);
            return;
        }
        try {
            Desktop.getDesktop().open(folder);
        } catch (IOException e) {
            logger.error("Impossibile aprire la cartella: {}", e.getMessage());
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

    public void interruptWatcher() {
        addLogMessage("Chiusura applicazione...");
        if (watcherThread != null) {
            watcherThread.interrupt();
            try {
                watcherThread.join();
            } catch (InterruptedException e) {
                String msg = "Thread in background interrotto in maniera anomala";
                launchAlertError(msg);
                logger.warn(msg);
                Log.addMessage(msg);
            }
        }
    }

    private void exitApplication() {
        interruptWatcher();
        Platform.exit();
    }

    public void launchDialogConversion(File srcFile) {
        List<String> formatiImmagini = Arrays.asList("png", "tiff", "gif", "webp", "psd", "icns", "ico", "tga", "iff", "jpeg", "bmp", "jpg", "pnm", "pgm", "pgm", "ppm", "xwd");
        if (srcFile == null || engine == null) {
            logger.error("File sorgente o Engine non valido.");
            Log.addMessage("ERRORE: File sorgente o Engine non valido.");
            launchAlertError("File sorgente o Engine non valido.");
            return;
        }

        Platform.runLater(() -> fileRicevuti++);

        String srcExtension = getExtension(srcFile);
        List<String> formats;
        try {
            // Prova prima il webservice per ottenere i formati
            if (webServiceClient.isServiceAvailable()) {
                try {
                    formats = webServiceClient.getPossibleConversions(srcExtension);
                    addLogMessage("Formati ottenuti da web service per " + srcFile.getName());
                } catch (Exception wsError) {
                    addLogMessage("Errore web service per formati, uso engine locale: " + wsError.getMessage());
                    formats = engine.getPossibleConversions(srcExtension);
                }
            } else {
                addLogMessage("Web service non disponibile, uso engine locale per " + srcFile.getName());
                formats = engine.getPossibleConversions(srcExtension);
            }
        } catch (Exception e) {
            logger.error("Conversione non supportata per il file {} ({})", srcFile.getName(), srcExtension);
            Log.addMessage("ERRORE: Conversione non supportata per il file " + srcFile.getName() + " (" + srcExtension + ")");
            launchAlertError("Conversione di " + srcFile.getName() + " non supportata");
            Platform.runLater(() -> {
                fileScartati++;
                stampaRisultati();
            });
            return;
        }

        List<String> finalFormats = formats;
        Platform.runLater(() -> {
            ChoiceDialog<String> dialog = new ChoiceDialog<>(finalFormats.get(0), finalFormats);
            dialog.setTitle("Seleziona Formato");
            dialog.setHeaderText("Converti " + srcFile.getName() + " in...");
            dialog.setContentText("Formato desiderato:");

            Optional<String> result = dialog.showAndWait();
            result.ifPresent(chosenFormat -> {
                new Thread(() -> performConversionWithFallback(srcFile, chosenFormat)).start();
            });
        });
    }

    private void performConversionWithFallback(File srcFile, String targetFormat) {
        List<String> formatiImmagini = Arrays.asList("png", "tiff", "gif", "webp", "psd", "icns", "ico", "tga", "iff", "jpeg", "bmp", "jpg", "pnm", "pgm", "pgm", "ppm", "xwd");
        String srcExtension = getExtension(srcFile);
        String outputFileName = srcFile.getName().replaceFirst("\\.[^\\.]+$", "") + "." + targetFormat;
        File outputDestinationFile = new File(convertedFolderPath, outputFileName);

        // Variabili per gestire i dialoghi PDF
        String password = null;
        boolean mergeImages = false;

        try {

            // CORREZIONE: Gestione dialoghi per PDF (eseguiti nel thread JavaFX)
            if (srcExtension.equals("pdf")) {
                // Usa CountDownLatch per sincronizzare i thread
                java.util.concurrent.CountDownLatch latch = new java.util.concurrent.CountDownLatch(1);
                java.util.concurrent.atomic.AtomicReference<String> passwordRef = new java.util.concurrent.atomic.AtomicReference<>();
                java.util.concurrent.atomic.AtomicBoolean mergeImagesRef = new java.util.concurrent.atomic.AtomicBoolean(false);

                Platform.runLater(() -> {
                    try {
                        // Chiedi la password nel thread JavaFX
                        passwordRef.set(launchDialogPdfSync());

                        // Se il target è JPG, chiedi se unire le immagini
                        if (targetFormat.equals("jpg") && srcExtension.equals("pdf")) {
                            mergeImagesRef.set(launchDialogUnisciSync());
                        }
                    } finally {
                        latch.countDown();
                    }
                });

                // Aspetta che i dialoghi siano completati
                try {
                    latch.await();
                    password = passwordRef.get();
                    mergeImages = mergeImagesRef.get();
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    throw new Exception("Operazione interrotta dall'utente");
                }
            }

            // PRIMO TENTATIVO: USA WEBSERVICE
            boolean webServiceSuccess = false;
            if (webServiceClient.isServiceAvailable()) {
                try {
                    addLogMessage("Tentativo conversione tramite web service...");
                    ConversionResult result = webServiceClient.convertFile(srcFile, targetFormat, outputDestinationFile, password, mergeImages);

                    if (result.isSuccess()) {
                        // Verifica che il file convertito sia stato effettivamente salvato
                        if (outputDestinationFile.exists()) {
                            addLogMessage("Conversione WEB SERVICE riuscita: " + result.getMessage());
                            webServiceSuccess = true;
                        } else {
                            throw new Exception("Il file convertito non è stato salvato correttamente dal web service");
                        }
                    } else {
                        throw new Exception("Web service ha restituito errore: " + result.getError());
                    }
                } catch (Exception wsError) {
                    addLogMessage("Web service fallito: " + wsError.getMessage());
                    webServiceSuccess = false;

                    // Pulisci eventuale file parzialmente creato
                    if (outputDestinationFile.exists()) {
                        try {
                            Files.delete(outputDestinationFile.toPath());
                            addLogMessage("File parziale eliminato per retry con engine locale");
                        } catch (Exception cleanupError) {
                            addLogMessage("Errore pulizia file parziale: " + cleanupError.getMessage());
                        }
                    }
                }
            } else {
                addLogMessage("Web service non disponibile, passo direttamente a engine locale");
            }

            // SECONDO TENTATIVO: USA ENGINE LOCALE (solo se webservice fallito)
            if (!webServiceSuccess) {
                addLogMessage("Fallback: uso engine locale per conversione...");

                try {
                    // L'engine locale gestisce automaticamente il salvataggio nelle cartelle configurate
                    if (password != null) {
                        if (targetFormat.equals("jpg")) {
                            engine.conversione(srcExtension, targetFormat, srcFile, password, mergeImages);
                        } else {
                            engine.conversione(srcExtension, targetFormat, srcFile, password);
                        }
                    } else {
                        if (targetFormat.equals("jpg") && srcExtension.equals("pdf")) {
                            engine.conversione(srcExtension, targetFormat, srcFile, mergeImages);
                        } else {
                            if(formatiImmagini.contains(srcExtension)){
                                engine.conversione(srcExtension, targetFormat, srcFile, targetFormat);
                            }else {
                                engine.conversione(srcExtension, targetFormat, srcFile);
                            }
                        }
                    }

                    addLogMessage("Conversione ENGINE LOCALE riuscita");

                    // Per l'engine locale, il file originale è già stato gestito automaticamente
                    Platform.runLater(() -> {
                        fileConvertiti++;
                        stampaRisultati();
                        launchAlertSuccess(srcFile);
                    });
                    return; // Esci qui se engine locale ha successo

                } catch (Exception engineError) {
                    addLogMessage("Anche engine locale fallito: " + engineError.getMessage());
                    throw new Exception("Entrambi i metodi di conversione falliti. Web service: fallito. Engine locale: " + engineError.getMessage());
                }
            }

            // Se arriviamo qui, il web service ha avuto successo
            if (webServiceSuccess) {
                addLogMessage("File convertito salvato in: " + outputDestinationFile.getAbsolutePath());

                // Gestisci il file originale dopo successo web service
                moveOriginalFileAfterSuccess(srcFile);

                Platform.runLater(() -> {
                    fileConvertiti++;
                    stampaRisultati();
                    launchAlertSuccess(outputDestinationFile);
                });
            }

        } catch (Exception e) {
            addLogMessage("ERRORE FINALE: " + e.getMessage());
            moveFileToErrorFolder(srcFile);
            Platform.runLater(() -> {
                fileScartati++;
                stampaRisultati();
                launchAlertError("Conversione fallita: " + e.getMessage());
            });
        }
    }

    private void moveOriginalFileAfterSuccess(File originalFile) {
        try {
            if (originalFile.exists()) {
                // Elimina il file originale dalla cartella monitorata
                Files.delete(originalFile.toPath());
                addLogMessage("File originale eliminato dalla cartella monitorata: " + originalFile.getName());
            }
        } catch (Exception e) {
            addLogMessage("Errore nella gestione del file originale: " + e.getMessage());
        }
    }

    private void moveFileToErrorFolder(File originalMonitoredFile) {
        try {
            // Solo se il file esiste ancora nella cartella monitorata, spostalo
            if (originalMonitoredFile.exists()) {
                Path srcPath = originalMonitoredFile.toPath();
                Path destPath = Paths.get(failedFolderPath, originalMonitoredFile.getName());
                Files.move(srcPath, destPath, StandardCopyOption.REPLACE_EXISTING);
                addLogMessage("File originale spostato in cartella errori: " + destPath);
            }
        } catch (Exception e) {
            addLogMessage("Errore nello spostamento file originale in cartella errori: " + e.getMessage());
        }
    }

    public void launchAlertError(String message) {
        showAlert("Errore", message, Alert.AlertType.ERROR);
    }

    public void launchAlertSuccess(File file) {
        String message = "Conversione di " + file.getName() + " riuscita";
        Log.addMessage(message);
        logger.info(message);
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

    private boolean launchDialogUnisciSync() {
        Alert alert = new Alert(Alert.AlertType.CONFIRMATION);
        alert.setTitle("Unisci PDF");
        alert.setHeaderText(null);
        alert.setContentText("Vuoi unire le pagine in un'unica immagine JPG?");

        ButtonType siBtn = new ButtonType("Si");
        ButtonType noBtn = new ButtonType("No");
        alert.getButtonTypes().setAll(siBtn, noBtn);

        Optional<ButtonType> result = alert.showAndWait();
        boolean unisci = result.isPresent() && result.get() == siBtn;
        Log.addMessage("Scelta unione JPG: " + unisci);
        logger.info("Scelta unione JPG: {}", unisci);
        return unisci;
    }

    private String launchDialogPdfSync() {
        TextInputDialog dialog = new TextInputDialog();
        dialog.setTitle("Password PDF");
        dialog.setHeaderText("Inserisci la password per il PDF (se richiesta)");
        dialog.setContentText("Password:");

        Optional<String> result = dialog.showAndWait();
        String password = result.orElse(null);
        Log.addMessage("Password ricevuta: " + (password == null || password.isEmpty() ? "(vuota)" : "(nascosta)"));
        logger.info("Password ricevuta: {}", password == null || password.isEmpty() ? "(vuota)" : "(nascosta)");
        return password;
    }

    public void stampaRisultati() {
        Log.addMessage("Stato: ricevuti=" + fileRicevuti + ", convertiti=" + fileConvertiti + ", scartati=" + fileScartati);
        logger.info("Stato: ricevuti={}, convertiti={}, scartati={}", fileRicevuti, fileConvertiti, fileScartati);
        Platform.runLater(() -> {
            detectedFilesCounter.setText(String.valueOf(fileRicevuti));
            successfulConversionsCounter.setText(String.valueOf(fileConvertiti));
            failedConversionsCounter.setText(String.valueOf(fileScartati));
        });
    }

    /**
     * Setta il riferimento all'app principale.
     *
     * @param mainApp istanza MainApp
     */
    public void setMainApp(MainApp mainApp) {
        this.mainApp = mainApp;
    }

    @FXML
    public void openConfig(javafx.event.ActionEvent actionEvent) {
        openConfigurationWindow();
    }

    @FXML
    public void loadFilesFolder(javafx.event.ActionEvent actionEvent) {
        openFolder(monitoredFolderPath);
    }

    @FXML
    public void openConvertedFolder(javafx.event.ActionEvent actionEvent) {
        openFolder(convertedFolderPath);
    }

    @FXML
    public void openFailedConvertionsFolder(javafx.event.ActionEvent actionEvent) {
        openFolder(failedFolderPath);
    }
}