package gui;

import converter.DirectoryWatcher;
import converter.Log;
import javafx.application.Platform;
import javafx.event.ActionEvent;
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
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicBoolean;

import converter.Engine;

import javax.swing.*;

import WebService.client.ConverterWebServiceClient;
import WebService.client.ConversionResult;
import java.nio.file.StandardCopyOption;


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

    private MainApp mainApp;

    private boolean isMonitoring = false;
    private int fileRicevuti = 0;
    private int fileConvertiti = 0;
    private int fileScartati = 0;

    private ConverterWebServiceClient webServiceClient;
    private boolean useWebService = false;

    private String monitoredFolderPath = "Non configurata";
    private String convertedFolderPath = "Non configurata";
    private String failedFolderPath = "Non configurata";
    private Engine engine;
    private Thread watcherThread;
    private boolean monitorAtStart;

    @FXML
    private void initialize() throws IOException {
        engine = new Engine();
        setupEventHandlers();
        updateMonitoringStatus();
        Log.addMessage("Applicazione avviata.");
        Log.addMessage("Caricamento configurazione...");

        webServiceClient = new ConverterWebServiceClient("http://localhost:8080");

        loadConfiguration();
        System.out.println(monitorAtStart);
        if (monitorAtStart) {
            toggleMonitoring();
        }
    }

    public static String getExtension(File file) {
        String name = file.getName();
        int lastDot = name.lastIndexOf('.');
        if (lastDot == -1 || lastDot == name.length() - 1) {
            return "";
        }
        return name.substring(lastDot + 1).toLowerCase();
    }

    private void setupEventHandlers() {
        MonitoringBtn.setOnAction(e -> {
            try {
                toggleMonitoring();
            } catch (IOException ex) {
                throw new RuntimeException(ex);
            }
        });

        configBtn.setOnAction(e -> openConfigurationWindow());
        exitBtn.setOnAction(e -> exitApplication());

        caricaFileBtn.setOnAction(e -> openFolder(monitoredFolderPath));
        fileConvertitiBtn.setOnAction(e -> openFolder(convertedFolderPath));
        conversioniFalliteBtn.setOnAction(e -> openFolder(failedFolderPath));
    }

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
            watcherThread = new Thread(new DirectoryWatcher(monitoredFolderPath, this));
            watcherThread.setDaemon(true);
            watcherThread.start();

            detectedFilesCounter.setText("0");
            successfulConversionsCounter.setText("0");
            failedConversionsCounter.setText("0");
            fileRicevuti = 0;
            fileConvertiti = 0;
            fileScartati = 0;
        }
        isMonitoring = !isMonitoring;
        updateMonitoringStatus();
    }

    private void resetCounters() {
        fileRicevuti = 0;
        fileConvertiti = 0;
        fileScartati = 0;

        detectedFilesCounter.setText("N/A");
        successfulConversionsCounter.setText("N/A");
        failedConversionsCounter.setText("N/A");
    }

    private void updateMonitoringStatus() {
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
    }

    private void openConfigurationWindow() {
        try {
            addLogMessage("Apertura editor configurazione...");

            FXMLLoader loader = new FXMLLoader();
            loader.setLocation(getClass().getResource("/ConfigWindow.fxml"));
            Parent configWindow = loader.load();

            Stage configStage = new Stage();
            configStage.setTitle("Editor Configurazione");
            configStage.initModality(Modality.WINDOW_MODAL);
            configStage.initOwner(getPrimaryStage());
            configStage.setResizable(true);
            configStage.setMinWidth(700);
            configStage.setMinHeight(600);

            Scene scene = new Scene(configWindow);
            configStage.setScene(scene);

            ConfigWindowController controller = loader.getController();
            controller.setDialogStage(configStage);
            controller.setEngine(engine, this);

            addLogMessage("Editor configurazione aperto");
            configStage.showAndWait();

            addLogMessage("Editor configurazione chiuso");
            loadConfiguration();

        } catch (IOException e) {
            addLogMessage("Errore nell'apertura dell'editor configurazione: " + e.getMessage());
            showAlert("Errore", "Impossibile aprire l'editor di configurazione: " + e.getMessage());
            e.printStackTrace();
        }
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

            if (applicationLogArea.getText().equals("Log dell'applicazione...")) {
                applicationLogArea.setText(logEntry);
            } else {
                applicationLogArea.setText(applicationLogArea.getText() + "\n" + logEntry);
            }
        });
    }

    private void exitApplication() {
        addLogMessage("Chiusura applicazione...");
        Platform.exit();
    }

    public void launchDialogConversion(File srcFile) { // srcFile è già final o effectively final qui
        Platform.runLater(() -> fileRicevuti++);

        String srcExtension = getExtension(srcFile);
        System.out.println("Estensione file sorgente: " + srcExtension);
        List<String> formats = null;

        try {
            // Prova prima il webservice, se fallisce usa l'engine locale
            if (webServiceClient.isServiceAvailable()) {
                formats = webServiceClient.getPossibleConversions(srcExtension);
                useWebService = true;
                addLogMessage("Usando web service per conversione di " + srcFile.getName());
            } else {
                formats = engine.getPossibleConversions(srcExtension);
                useWebService = false;
                addLogMessage("Web service non disponibile, usando engine locale per " + srcFile.getName());
            }
        } catch (Exception e) {
            launchAlertError("Conversione di " + srcFile.getName() + " non supportata: " + e.getMessage());
            moveFileToErrorFolder(srcFile);
            Platform.runLater(() -> {
                fileScartati++;
                stampaRisultati();
            });
            return;
        }

        final List<String> finalFormats = formats; // Rendi final la lista dei formati
        Platform.runLater(() -> {
            ChoiceDialog<String> dialog = new ChoiceDialog<>(finalFormats.get(0), finalFormats);
            dialog.setTitle("Seleziona Formato");
            dialog.setHeaderText("Converti " + srcFile.getName() + " in...");
            dialog.setContentText("Formato desiderato:");

            Optional<String> result = dialog.showAndWait();
            result.ifPresent(chosenFormat -> { // Usa un nuovo nome per la variabile lambda
                // srcFile è già effectively final, quindi non serve dichiararlo final qui
                new Thread(() -> performConversion(srcFile, chosenFormat)).start();
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
                addLogMessage("Avvio conversione tramite engine locale...");

                Path engineTempOutputDirectory = Files.createTempDirectory("engine_output_" + UUID.randomUUID().toString());
                final File engineTempOutputDirFile = engineTempOutputDirectory.toFile(); // Dichiara final se necessario

                File convertedFileFromEngine;

                if (password != null) {
                    if (mergeImages && targetFormat.equals("jpg")) {
                        convertedFileFromEngine = engine.conversione(srcExtension, targetFormat, tempInputFile, password, mergeImages, engineTempOutputDirFile);
                    } else {
                        convertedFileFromEngine = engine.conversione(srcExtension, targetFormat, tempInputFile, password, engineTempOutputDirFile);
                    }
                } else {
                    if (mergeImages && targetFormat.equals("jpg")) {
                        convertedFileFromEngine = engine.conversione(srcExtension, targetFormat, tempInputFile, mergeImages, engineTempOutputDirFile);
                    } else {
                        convertedFileFromEngine = engine.conversione(srcExtension, targetFormat, tempInputFile, engineTempOutputDirFile);
                    }
                }

                if (convertedFileFromEngine != null && convertedFileFromEngine.exists()) {
                    Files.move(convertedFileFromEngine.toPath(), finalOutputDestinationFile.toPath(), StandardCopyOption.REPLACE_EXISTING);
                    addLogMessage("Conversione completata tramite engine locale. File salvato in: " + finalOutputDestinationFile.getAbsolutePath());
                    moveFileToSuccessFolder(finalSrcFile); // Usa la variabile final
                    Platform.runLater(() -> {
                        fileConvertiti++;
                        stampaRisultati();
                        launchAlertSuccess(finalOutputDestinationFile); // Usa la variabile final
                    });
                } else {
                    throw new Exception("L'engine locale non ha prodotto un file di output valido.");
                }

                Files.delete(engineTempOutputDirectory);
            }

        } catch (Exception e) {
            addLogMessage("Errore durante conversione: " + e.getMessage());
            moveFileToErrorFolder(srcFile); // srcFile è effettivamente final qui
            final String errorMessage = e.getMessage(); // Cattura il messaggio di errore
            Platform.runLater(() -> {
                fileScartati++;
                stampaRisultati();
                launchAlertError("Errore: " + errorMessage); // Usa la variabile final
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


    private String launchDialogPdf(){
        JPasswordField passwordField = new JPasswordField(20);

        JPanel panel = new JPanel(new BorderLayout(5, 5));
        panel.add(new JLabel("Il PDF è protetto? Inserisci password se sì:"), BorderLayout.NORTH);
        panel.add(passwordField, BorderLayout.CENTER);

        int scelta = JOptionPane.showConfirmDialog(
                null,
                panel,
                "Protezione PDF",
                JOptionPane.YES_NO_OPTION,
                JOptionPane.QUESTION_MESSAGE
        );

        if (scelta == JOptionPane.YES_OPTION)
            return new String(passwordField.getPassword());
        return null;
    }

    private boolean launchDialogUnisci() {
        int scelta = JOptionPane.showConfirmDialog(
                null,
                "Vuoi unire tutte le immagini in un'unica immagine verticale?",
                "Unione immagini",
                JOptionPane.YES_NO_OPTION,
                JOptionPane.QUESTION_MESSAGE
        );

        if (scelta == JOptionPane.YES_OPTION) {
            System.out.println("Hai scelto SÌ: unire le immagini.");
            return true;
        } else if (scelta == JOptionPane.NO_OPTION) {
            System.out.println("Hai scelto NO: immagini separate.");
            return false;
        } else {
            System.out.println("Dialog chiuso senza scelta, si assume NO.");
            return false;
        }
    }

    public void stampaRisultati() {
        detectedFilesCounter.setText(Integer.toString(fileRicevuti));
        successfulConversionsCounter.setText(Integer.toString(fileConvertiti));
        failedConversionsCounter.setText(Integer.toString(fileScartati));
    }

    public void launchAlertSuccess(File file) {
        Platform.runLater(() -> {
            Alert alert = new Alert(Alert.AlertType.INFORMATION);
            alert.setTitle("Conversione eseguita");
            alert.setHeaderText(null);
            alert.setContentText("Conversione di " + file.getName() + " completata con successo!");
            alert.showAndWait();
        });
    }

    public void launchAlertError(String message) {
        Platform.runLater(() -> {
            Alert alert = new Alert(Alert.AlertType.INFORMATION);
            alert.setTitle("Conversione interrotta");
            alert.setHeaderText(null);
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