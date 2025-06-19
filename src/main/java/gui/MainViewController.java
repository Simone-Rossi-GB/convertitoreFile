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
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;

import javax.swing.*;

import WebService.client.ConverterWebServiceClient;
import WebService.client.ConversionResult;
import java.nio.file.StandardCopyOption;


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
    private boolean monitorAtStart;

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
        Log.addMessage("Applicazione avviata.");
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
            addLogMessage("Apertura editor configurazione...");

            FXMLLoader loader = new FXMLLoader();
            loader.setLocation(getClass().getResource("/ConfigWindow.fxml"));
            Parent configWindow = loader.load();

            // Crea lo stage per la finestra di configurazione
            Stage configStage = new Stage();
            configStage.setTitle("Editor Configurazione");
            configStage.initModality(Modality.WINDOW_MODAL);
            configStage.initOwner(getPrimaryStage());
            configStage.setResizable(true);
            configStage.setMinWidth(700);
            configStage.setMinHeight(600);
            configStage.setScene(new Scene(configWindow));

            // Configura la scena
            Scene scene = new Scene(configWindow);
            configStage.setScene(scene);

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
                watcherThread.setDaemon(true);
                watcherThread.start();
            }


        } catch (IOException e) {
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

    public void loadConfiguration() {
        try {
            monitoredFolderPath = engine.getConverterConfig().getMonitoredDir();
            convertedFolderPath = engine.getConverterConfig().getSuccessOutputDir();
            failedFolderPath = engine.getConverterConfig().getErrorOutputDir();
            monitorAtStart = engine.getConverterConfig().getMonitorAtStart();

            addLogMessage("Configurazione caricata da config.json");
            addLogMessage("Cartella monitorata: " + monitoredFolderPath);
            addLogMessage("Cartella file convertiti: " + convertedFolderPath);
            addLogMessage("Cartella file falliti: " + failedFolderPath);



        } catch (Exception e) {
            addLogMessage("Errore nel caricamento configurazione: " + e.getMessage());
            showAlert("Errore Configurazione", "Impossibile caricare la configurazione: " + e.getMessage());
            e.printStackTrace();
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

    public void interruptWatcher(){
        addLogMessage("Chiusura applicazione...");
        watcherThread.interrupt();
        try {
            watcherThread.join();

        } catch (InterruptedException e) {
            String msg = "Thread in background interrotto in maniera anomala";
            launchAlertError(msg);
            Log.addMessage(msg);
        }
    }

    private void exitApplication() {
        interruptWatcher();
        Platform.exit();
    }

    private void performConversionWithFallback(File srcFile, String targetFormat) {
        String srcExtension = getExtension(srcFile);
        String outputFileName = srcFile.getName().replaceFirst("\\.[^\\.]+$", "") + "." + targetFormat;
        File outputDestinationFile = new File(convertedFolderPath, outputFileName);

        // Variabili per gestire i dialoghi PDF
        String password = null;
        boolean mergeImages = false;

        try {
            // Assicurati che la directory di output esista
            if (outputDestinationFile.getParentFile() != null && !outputDestinationFile.getParentFile().exists()) {
                outputDestinationFile.getParentFile().mkdirs();
            }

            // Gestione dialoghi per PDF (fatto una sola volta)
            if (srcExtension.equals("pdf")) {
                password = launchDialogPdf();
                if (targetFormat.equals("jpg")) {
                    mergeImages = launchDialogUnisci();
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
                        if (mergeImages && targetFormat.equals("jpg")) {
                            engine.conversione(srcExtension, targetFormat, srcFile, password, mergeImages);
                        } else {
                            engine.conversione(srcExtension, targetFormat, srcFile, password);
                        }
                    } else {
                        if (mergeImages && targetFormat.equals("jpg")) {
                            engine.conversione(srcExtension, targetFormat, srcFile, mergeImages);
                        } else {
                            engine.conversione(srcExtension, targetFormat, srcFile);
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

    public static String getExtension(File file) {
        if (file == null) return "";
        String name = file.getName();
        int lastDot = name.lastIndexOf('.');
        if (lastDot == -1 || lastDot == name.length() - 1) return "";
        return name.substring(lastDot + 1).toLowerCase();
    }

    public void launchDialogConversion(File srcFile) {
        List<String> formatiImmagini = Arrays.asList("png", "tiff", "gif", "webp", "psd", "icns", "ico", "tga", "iff","jpeg", "bmp", "jpg", "pnm", "pgm", "pgm", "ppm", "xwd");
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
            result.ifPresent(chosenFormat -> {
                new Thread(() -> performConversionWithFallback(srcFile, chosenFormat)).start();
            });
        });
    }

    private void performConversion(File srcFile, String targetFormat) {

        Path tempInputPath = null; // Sarà assegnato una volta
        File tempInputFile = null; // Sarà assegnato una volta
        File outputDestinationFile = null; // Sarà assegnato una volta
        String srcExtension = getExtension(srcFile); // Assegnato una volta
        String outputFileName = srcFile.getName().replaceFirst("\\.[^\\.]+$", "") + "." + targetFormat; // Assegnato una volta

        try {
            // Crea una copia temporanea del file sorgente per la conversione
            tempInputPath = Files.createTempFile("convert_input_", srcFile.getName());
            Files.copy(srcFile.toPath(), tempInputPath, StandardCopyOption.REPLACE_EXISTING);
            tempInputFile = tempInputPath.toFile();

            // Determina il percorso del file di output FINALE
            outputDestinationFile = new File(convertedFolderPath, outputFileName);
            if (outputDestinationFile.getParentFile() != null && !outputDestinationFile.getParentFile().exists()) {
                outputDestinationFile.getParentFile().mkdirs();
            }

            String password = null; // Potrebbe essere riassegnato, ma solo in un blocco if/else,
            // il che lo rende "effectively final" per ogni percorso di esecuzione.
            boolean mergeImages = false; // Stessa logica di password.

            // Gestione dialoghi per PDF
            if (srcExtension.equals("pdf")) {
                password = launchDialogPdf();
                if (targetFormat.equals("jpg")) {
                    mergeImages = launchDialogUnisci();
                }
            }

            // Dichiara `final` le variabili che verranno usate nelle lambda se il compilatore si lamenta,
            // anche se sono già "effectively final". Questo serve come workaround esplicito.
            final File finalOutputDestinationFile = outputDestinationFile;
            final File finalSrcFile = srcFile; // Se srcFile dovesse causare problemi nella catch/finally

            if (useWebService) {
                // USA WEBSERVICE
                addLogMessage("Avvio conversione tramite web service...");
                ConversionResult result = webServiceClient.convertFile(tempInputFile, targetFormat, outputDestinationFile, password, mergeImages);

                if (result.isSuccess()) {
                    addLogMessage("Conversione completata tramite web service: " + result.getMessage());
                    moveFileToSuccessFolder(finalSrcFile); // Usa la variabile final
                    Platform.runLater(() -> {
                        fileConvertiti++;
                        stampaRisultati();
                        launchAlertSuccess(finalOutputDestinationFile); // Usa la variabile final
                    });
                } else {
                    throw new Exception(result.getError());
                }
            } else {
                // USA ENGINE LOCALE
                try {
                    addLogMessage("Avvio conversione tramite engine locale...");

                    Path engineTempOutputDirectory = Files.createTempDirectory("engine_output_" + UUID.randomUUID().toString());
                    final File engineTempOutputDirFile = engineTempOutputDirectory.toFile(); // Dichiara final se necessario

                    File convertedFileFromEngine;

                    if (password != null) {
                        if (mergeImages && targetFormat.equals("jpg")) {
                            engine.conversione(srcExtension, targetFormat, tempInputFile, password, mergeImages);
                        } else {
                            engine.conversione(srcExtension, targetFormat, tempInputFile, password);
                        }
                    } else {
                        if (mergeImages && targetFormat.equals("jpg")) {
                            engine.conversione(srcExtension, targetFormat, tempInputFile, mergeImages);
                        } else {
                            engine.conversione(srcExtension, targetFormat, tempInputFile);
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
                    Log.addMessage("ERRORE: Impossibile convertire " + srcFile.getName() + ": " + e.getMessage());
                    launchAlertError(e.getMessage());
                    fileScartati++;
                    stampaRisultati();
                }
            });
        } finally {
            // Pulizia del file temporaneo di input, INDIPENDENTEMENTE da successo o fallimento
            final Path finalTempInputPath = tempInputPath; // Rendi final per il blocco finally
            try {
                if (finalTempInputPath != null && Files.exists(finalTempInputPath)) {
                    Files.delete(finalTempInputPath);
                }
            } catch (IOException cleanupEx) {
                System.err.println("Errore nella pulizia del file di input temporaneo: " + cleanupEx.getMessage());
            }
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

    // Questi metodi `moveFileTo...Folder` ora spostano il *file originale* dalla monitoredFolderPath
    // Il file convertito/fallito è già stato gestito (salvato dal client o spostato dall'Engine)
    private void moveFileToSuccessFolder(File originalMonitoredFile) {
        try {
            // Solo se il file esiste ancora nella cartella monitorata, spostalo
            if (originalMonitoredFile.exists()) {
                Path srcPath = originalMonitoredFile.toPath();
                // Potresti voler spostare il file originale in una sottocartella "processed" di monitored,
                // o semplicemente eliminarlo se non serve mantenerlo.
                // Per ora, lo elimino dalla monitored folder dato che il risultato è altrove.
                Files.delete(srcPath);
                addLogMessage("File originale spostato (eliminato da monitored): " + srcPath);
            }
        } catch (Exception e) {
            addLogMessage("Errore nello spostamento/eliminazione del file originale da monitored: " + e.getMessage());
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

    /**
     * Ritorna lo stage principale dell'applicazione.
     *
     * @return stage principale
     */
    private Stage getPrimaryStage() {
        return (Stage) MonitoringBtn.getScene().getWindow();
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
}