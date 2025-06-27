package webService.client.gui;

import webService.client.configuration.configHandlers.config.*;
import webService.client.configuration.configHandlers.conversionContext.*;
import webService.client.configuration.configExceptions.*;

import webService.client.objects.DirectoryWatcher;
import webService.server.converters.Zipper;
import webService.client.objects.Utility;
import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.LogManager;
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
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.nio.file.StandardCopyOption;

import webService.client.ConverterWebServiceClient;
import webService.client.ConversionResult;
import webService.server.converters.exception.ConversionException;
import webService.server.converters.exception.IllegalExtensionException;

import javax.xml.ws.WebServiceException;

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
    public Button conversionConfigBtn;
    @FXML
    private Button exitBtn;
    @FXML
    private Button caricaFileBtn;
    @FXML
    private Button fileConvertitiBtn;
    @FXML
    private Button conversioniFalliteBtn;

    private static final Logger logger = LogManager.getLogger(MainViewController.class);

    // Variabili di stato
    private boolean isMonitoring = false;
    private int fileRicevuti = 0;
    private int fileConvertiti = 0;
    private int fileScartati = 0;

    private ConverterWebServiceClient webServiceClient;

    private String monitoredFolderPath = "Non configurata";
    private String convertedFolderPath = "Non configurata";
    private String failedFolderPath = "Non configurata";
    private boolean monitorAtStart;
    private String currentProgressLogLine = null; // Tiene traccia della riga di progresso corrente
    private boolean isShowingProgress = false; // Flag per sapere se stiamo mostrando una barra di progresso

    private Thread watcherThread;
    private final String configFile = "src/main/java/webService/client/configuration/configFiles/config.json";
    private final String conversionContextFile = "src/main/java/webService/client/configuration/configFiles/conversionContext.json";
    private InstanceConversionContextWriter iccw = null;
    private MainApp mainApp;

    /**
     * Metodo invocato automaticamente da JavaFX dopo il caricamento dell'FXML.
     * Inizializza il controller, i listener e carica la configurazione.
     */
    @FXML
    private void initialize() throws IOException {
        // Inizializza l'interfaccia
        setupEventHandlers();
        updateMonitoringStatus();
        logger.info("Applicazione avviata.");
        logger.info("Caricamento configurazione...");
        //Inizializza il client webService
        webServiceClient = new ConverterWebServiceClient("http://localhost:8080");
        //Inizializza i gestori dei file di configurazione
        ConfigInstance ci = new ConfigInstance(new File(configFile));
        ConfigData.update(ci);
        loadConfiguration();
        ConversionContextInstance cci = new ConversionContextInstance(new File(conversionContextFile));
        iccw = new InstanceConversionContextWriter(new File(conversionContextFile));
        ConversionContextData.update(cci);
        //Carica la configurazione di base
        loadConfiguration();
        //Invia al server i file di configurazione
        if(webServiceClient.isServiceAvailable()){
            webServiceClient.sendConfigFile(new File(configFile));
            webServiceClient.sendConversionContextFile(new File(conversionContextFile));
        }else
            logger.error("Errore nell'invio iniziale dei file di configurazione");
        if (monitorAtStart) {
            toggleMonitoring();
        }
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
                launchAlertError("Errore durante il monitoraggio: " + ex.getMessage());
            }
        });

        Stage stage = MainApp.getPrimaryStage();
        exitBtn.setOnAction(e -> exitApplication());
        stage.setOnCloseRequest(e -> interruptWatcher());
        configBtn.setOnAction(e -> openConfigurationWindow());
        conversionConfigBtn.setOnAction(e -> openConversionConfigurationWindow());
        caricaFileBtn.setOnAction(e -> openFolder(monitoredFolderPath));
        fileConvertitiBtn.setOnAction(e -> openFolder(convertedFolderPath));
        conversioniFalliteBtn.setOnAction(e -> openFolder(failedFolderPath));
    }

    /**
     * Attiva o disattiva il monitoraggio della cartella.
     */
    @FXML
    private void toggleMonitoring() throws IOException {
        if (monitoredFolderPath == null) {
            launchAlertError("Engine non inizializzato.");
            return;
        }

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

    /**
     * Estrae le informazioni contenute nel file di configurazione
     */
    private void loadConfiguration() {
        //Ottiene i percorsi delle cartelle dal file di config e le crea se non esistono;
        monitoredFolderPath = ConfigReader.getMonitoredDir();
        checkAndCreateFolder(monitoredFolderPath);
        convertedFolderPath = ConfigReader.getSuccessOutputDir();
        checkAndCreateFolder(convertedFolderPath);
        failedFolderPath = ConfigReader.getErrorOutputDir();
        checkAndCreateFolder(failedFolderPath);
        checkAndCreateFolder("src/temp");
        monitorAtStart = ConfigReader.getIsMonitoringEnabledAtStart();

        logger.info("Configurazione caricata da config.json");
        logger.info("Cartella monitorata: {}", monitoredFolderPath);
        logger.info("Cartella file convertiti: {}", convertedFolderPath);
        logger.info("Cartella file falliti: {}", failedFolderPath);
    }

    /**
     * Metodo che controlla l'esistenza di una directory e se non esiste la crea
     */
    private void checkAndCreateFolder(String path) {
        File folder = new File(path);
        if (!folder.exists()) {
            boolean created = folder.mkdirs();
            if (created) {
                logger.info("Cartella mancante creata: {}", path);
            } else {
                logger.error("Impossibile creare la cartella: {}", path);
            }
        }
    }

    /**
     * Apre la finestra per modificare il file config.json
     */
    private void openConfigurationWindow() {
        try {
            addLogMessage("Apertura editor configurazione...");

            //Carica il file FXML
            FXMLLoader loader = new FXMLLoader();
            loader.setLocation(getClass().getResource("/ConfigWindow.fxml"));
            Parent configWindow = loader.load();

            // Crea lo stage per la finestra di configurazione
            Stage configStage = new Stage();
            configStage.setTitle("Editor Configurazione");
            configStage.initModality(Modality.WINDOW_MODAL);
            configStage.initOwner(MainApp.getPrimaryStage());
            configStage.setResizable(false);
            configStage.setScene(new Scene(configWindow));

            // Ottiene il controller e passa i riferimenti necessari
            ConfigWindowController controller = loader.getController();
            controller.setDialogStage(configStage);
            // Mostra la finestra e attende la chiusura
            addLogMessage("Editor configurazione aperto");
            configStage.showAndWait();

            // Ricarica la configurazione dopo la chiusura della finestra
            addLogMessage("Editor configurazione chiuso");
            logger.info("Configurazione inviata al webService");
            webServiceClient.sendConfigFile(new File(configFile));
            loadConfiguration();
            interruptWatcher();
            watcherThread = new Thread(new DirectoryWatcher(monitoredFolderPath, this));
            watcherThread.start();
        } catch (IOException e) {
            launchAlertError("Impossibile aprire l'editor di configurazione: " + e.getMessage());
        }
    }

    /**
     * Apre la finestra per modificare il file conversionContext.json
     */
    private void openConversionConfigurationWindow() {
        try {
            addLogMessage("Apertura editor configurazione della conversione...");

            // Carica il file FXML
            FXMLLoader loader = new FXMLLoader();
            loader.setLocation(getClass().getResource("/ConversionConfigWindow.fxml"));
            Parent configWindow = loader.load();

            // Crea lo stage per la finestra di configurazione
            Stage configStage = new Stage();
            configStage.setTitle("Editor Configurazione Conversione");
            configStage.initModality(Modality.WINDOW_MODAL);
            configStage.initOwner(MainApp.getPrimaryStage());
            configStage.setResizable(false);
            configStage.setScene(new Scene(configWindow));

            // Ottiene il controller e passa i riferimenti necessari
            ConversionConfigWindowController controller = loader.getController();
            controller.setDialogStage(configStage);
            // Mostra la finestra e attende la chiusura
            logger.info("Editor configurazione conversione aperto");
            configStage.showAndWait();
            // Ricarica la configurazione
            logger.info("Configurazione inviata al webService");
            webServiceClient.sendConversionContextFile(new File(conversionContextFile));
        }catch (IOException e) {
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
            launchAlertError("La cartella non è stata ancora configurata.");
            return;
        }
        File folder = new File(folderPath);
        if (!folder.exists() || !folder.isDirectory()) {
            logger.error("La cartella non esiste {}", folderPath);
            launchAlertError("La cartella non esiste: " + folderPath);
            return;
        }
        try {
            Desktop.getDesktop().open(folder);
        } catch (IOException e) {
            logger.error("Impossibile aprire la cartella: {}", e.getMessage());
            launchAlertError("Impossibile aprire la cartella: " + e.getMessage());
        }
    }

    /**
     * Aggiunge un messaggio al log della GUI
     * @param message messaggio da aggiungere
     */
    public void addLogMessage(String message) {
        Platform.runLater(() -> {
            String timestamp = java.time.LocalTime.now().toString().substring(0, 8);
            String logEntry = "[" + timestamp + "] " + message;

            // Se stiamo mostrando una barra di progresso, non aggiungere messaggi normali
            if (isShowingProgress) {
                return;
            }

            if ("Log dell'applicazione...".equals(applicationLogArea.getText())) {
                applicationLogArea.setText(logEntry);
            } else {
                applicationLogArea.setText(applicationLogArea.getText() + "\n" + logEntry);
            }
        });
    }

    /**
     * Interrompe il thread del directory watcher e tutti quelli che ha generato in maniera sicura
     */
    public void interruptWatcher() {
        addLogMessage("Terminazione del watcher...");
        if (watcherThread != null) {
            //Interrompe il watcher
            watcherThread.interrupt();
            try {
                //Attende che il watcher abbia eseguito tutte le operazioni di chiusura (evita interruzioni anomale)
                watcherThread.join();
            } catch (InterruptedException e) {
                String msg = "Thread in background interrotto in maniera anomala";
                launchAlertError(msg);
            }
        }
    }

    /**
     * Termina l'applicazione
     */
    private void exitApplication() {
        addLogMessage("Chiusura dell'applicazione...");
        interruptWatcher();
        Platform.exit();
    }

    /**
     * Fa comparire il dialog per selezionare il formato nel quale convertire il/i file e poi avvia la conversione
     * @param srcFile file di partenza
     */
    public void launchDialogConversion(File srcFile) {
        //Quando viene chiamato incrementa il numero di file ricevuti
        Platform.runLater(() -> fileRicevuti++);
        String srcExtension;
        List<String> formats = null;
        try {
            srcExtension = extractSrcExtension(srcFile);
            //tenta di ottenere i formati dal webService
            formats = getExtensionsFromWebService(srcExtension);
            List<String> finalFormats = formats;
            //Mostra il dialog per selezionare il formato di output
            Platform.runLater(() -> {
                ChoiceDialog<String> dialog = new ChoiceDialog<>(finalFormats.get(0), finalFormats);
                dialog.setTitle("Seleziona Formato");
                dialog.setHeaderText("Converti " + srcFile.getName() + " in...");
                dialog.setContentText("Formato desiderato:");
                Optional<String> result = dialog.showAndWait();
                //Se il dialog ha ritornato un formato per la conversione, viene istanziato un nuovo thread che se ne occupa
                result.ifPresent(chosenFormat -> {
                    new Thread(() -> performConversion(srcFile, chosenFormat)).start();
                });
            });
        } catch (Exception e) { //Intercetta tutte le eccezioni sollevate
            //Sposta il file di partenza nella cartella delle conversioni fallite
            moveFileToErrorFolder(srcFile);
            launchAlertError(e.getMessage());
            aggiornaCounterScartati();
        }
    }

    /**
     * Estrae l'estensione del file da convertire.
     * @param file file di partenza
     * @return l'estensione del file. Se la conversione multipla è impostata ritorna l'estensione dei file contenuti
     * @throws IOException la classe zipper non è riuscita a decomprimere il file nel caso di conversione multipla
     * @throws IllegalExtensionException Il file non ha estensione. Nel caso di conversione multipla può essere lanciata se i file della cartella compressa sono di formati diversi
     */
    private String extractSrcExtension(File file) throws IOException, IllegalExtensionException {
        String srcExtension = Utility.getExtension(file);
        //Se è impostata la conversione multipla prende l'estensione dei file contenuti se è uguale per tutti
        if(ConfigReader.getIsMultipleConversionEnabled() && Utility.getExtension(file).equals("zip")) {
            try {
                srcExtension = Zipper.extractFileExstension(file);
            } catch (IOException e) {
                throw new IOException("Impossibile decomprimere il file");
            }
        }
        return srcExtension;
    }

    /**
     * aggiorna il numero di conversioni fallite
     */
    private void aggiornaCounterScartati(){
        Platform.runLater(() -> {
            fileScartati++;
            stampaRisultati();
        });
    }

    /**
     * Chiede al server i formati in cui il file può essere convertito
     * @param srcExtension formato di partenza
     * @return lista di formati supportati
     * @throws WebServiceException web service non disponibile o errore durante la ricerca dei formati
     */
    private List<String> getExtensionsFromWebService(String srcExtension) throws WebServiceException{
        List<String> formats = null;
        // Controlla se il webService è attivo
        if (webServiceClient.isServiceAvailable()) {
            formats =  webServiceClient.getPossibleConversions(srcExtension);
            logger.info("Formati ottenuti dal web service");
        } else {
            launchAlertError("Il web service non è disponibile");
            throw new WebServiceException("Il web service non è disponibile");
        }
        return formats;
    }

    /**
     * Invia il file al server e attende quello convertito
     * @param srcFile file di partenza
     * @param targetFormat formato di destinazione
     */
    private void performConversion(File srcFile, String targetFormat) throws ConversionException, WebServiceException{
        String filename = srcFile.getName();

        try {
            // Fase 1: Inizializzazione
            updateProgressInLog(filename, 0, "Inizializzazione...");
            Thread.sleep(200); // Simula tempo di elaborazione

            // Fase 2: Verifica servizio
            updateProgressInLog(filename, 10, "Verifica servizio...");
            if (!webServiceClient.isServiceAvailable()) {
                addFinalLogMessage(filename + " - Servizio non disponibile");
                throw new WebServiceException("Il web service non è disponibile");
            }

            // Fase 3: Caricamento file
            updateProgressInLog(filename, 20, "Caricamento file...");
            Thread.sleep(300);

            File outputDestinationFile = new File(convertedFolderPath, srcFile.getName());

            // Fase 4: Invio al server il conversion context
            updateProgressInLog(filename, 40, "Invio paramtri al server...");
            Thread.sleep(200);

            iccw.writeDestinationFormat(targetFormat);
            ConversionContextData.update(new ConversionContextInstance(new File(conversionContextFile)));
            webServiceClient.sendConversionContextFile(new File(conversionContextFile));

            logger.info("Tentativo conversione tramite web service...");

            // Fase 5: Conversione in corso
            updateProgressInLog(filename, 60, "Conversione in corso...");
            ConversionResult result = webServiceClient.convertFile(srcFile, targetFormat, outputDestinationFile);

            // Fase 6: Elaborazione risultato
            updateProgressInLog(filename, 80, "Elaborazione risultato...");
            Thread.sleep(200);

            if (result.isSuccess()) {
                // Verifica che il file convertito sia stato effettivamente salvato
                outputDestinationFile = result.getResult();
                if (outputDestinationFile.exists()) {
                    // Fase 7: Finalizzazione
                    updateProgressInLog(filename, 95, "Finalizzazione...");
                    Thread.sleep(100);

                    // Completamento
                    updateProgressInLog(filename, 100, "Completato!");

                    logger.info("Conversione tramite web service riuscita");
                    addFinalLogMessage(filename + " convertito con successo in " + targetFormat);

                    File finalOutputDestinationFile = outputDestinationFile;
                    Platform.runLater(() -> {
                        fileConvertiti++;
                        stampaRisultati();
                        launchAlertSuccess(finalOutputDestinationFile);
                    });
                } else {
                    addFinalLogMessage(filename + " - File non salvato correttamente");
                    throw new ConversionException("Il file convertito non è stato salvato correttamente dal web service");
                }
            } else {
                addFinalLogMessage(filename + " - Errore server: " + result.getMessage());
                throw new ConversionException("Web service ha restituito errore: " + result.getMessage());
            }

        } catch (Exception e) {
            addFinalLogMessage(filename + " - Conversione fallita: " + e.getMessage());
            moveFileToErrorFolder(srcFile);
            launchAlertError(e.getMessage());
            aggiornaCounterScartati();
        }
    }

    /**
     * Aggiunge un messaggio di completamento/errore e resetta la barra di progresso
     * @param message Messaggio finale da aggiungere
     */
    private void addFinalLogMessage(String message) {
        Platform.runLater(() -> {
            String timestamp = java.time.LocalTime.now().toString().substring(0, 8);
            String logEntry = "[" + timestamp + "] " + message;

            if (!"Log dell'applicazione...".equals(applicationLogArea.getText())) {
                applicationLogArea.setText(applicationLogArea.getText() + "\n" + logEntry);
            } else {
                applicationLogArea.setText(logEntry);
            }

            // Resetta i flag della barra di progresso
            isShowingProgress = false;
            currentProgressLogLine = null;
        });
    }

    /**
     * Aggiorna o aggiunge una barra di progresso nei log
     * @param filename Nome del file in conversione
     * @param progress Progresso da 0 a 100
     * @param status Messaggio di stato
     */
    private void updateProgressInLog(String filename, int progress, String status) {
        Platform.runLater(() -> {
            String timestamp = java.time.LocalTime.now().toString().substring(0, 8);


            StringBuilder progressBar = new StringBuilder();
            progressBar.append("[").append(timestamp).append("] ");
            progressBar.append(filename).append(": [");

            // Barra con caratteri ASCII che funzionano meglio
            int filled = progress / 10; // 0-10
            for (int i = 0; i < 10; i++) {
                if (i < filled) {
                    progressBar.append("■"); // Quadrato pieno (supportato meglio)
                } else {
                    progressBar.append("□"); // Quadrato vuoto
                }
            }

            progressBar.append("] ").append(progress).append("% - ").append(status);

            String newProgressLine = progressBar.toString();

            if (isShowingProgress && currentProgressLogLine != null) {
                // Sostituisce l'ultima riga di progresso
                String currentText = applicationLogArea.getText();
                String updatedText = currentText.replace(currentProgressLogLine, newProgressLine);
                applicationLogArea.setText(updatedText);
            } else {
                // Prima volta che mostriamo il progresso per questo file
                if (!"Log dell'applicazione...".equals(applicationLogArea.getText())) {
                    applicationLogArea.setText(applicationLogArea.getText() + "\n" + newProgressLine);
                } else {
                    applicationLogArea.setText(newProgressLine);
                }
                isShowingProgress = true;
            }

            currentProgressLogLine = newProgressLine;

            // Se completato, resetta i flag
            if (progress >= 100) {
                isShowingProgress = false;
                currentProgressLogLine = null;
            }
        });
    }

    /**
     * Viene chiamato in caso di errore di qualsiasi natura durante la conversione. Sposta il file di partenza in una cartella dedicata alle conversioni fallite
     * @param originalMonitoredFile file di partenza
     */
    private void moveFileToErrorFolder(File originalMonitoredFile) {
        try {
            // Solo se il file esiste ancora nella cartella monitorata, spostalo
            if (originalMonitoredFile.exists()) {
                Path srcPath = originalMonitoredFile.toPath();
                Path destPath = Paths.get(failedFolderPath, originalMonitoredFile.getName());
                Files.move(srcPath, destPath, StandardCopyOption.REPLACE_EXISTING);
                logger.info("File originale spostato in cartella errori: " + destPath);
            }
        } catch (Exception e) {
            logger.error("Errore nello spostamento file originale in cartella errori: " + e.getMessage());
        }
    }

    /**
     * Lancia un alert di errore col messaggio desiderato e aggiunge la relativa voce al log
     * @param message messaggio di errore
     */
    public void launchAlertError(String message) {
        logger.error(message);
        showAlert("Errore", message, Alert.AlertType.ERROR);
    }

    /**
     * Lancia un alert di info col messaggio di conversione riuscita e aggiunge la relativa voce al log
     * @param file File da convertire
     */
    public void launchAlertSuccess(File file) {
        String message = "Conversione di " + file.getName() + " riuscita";
        logger.info(message);
        showAlert("Conversione riuscita", message, Alert.AlertType.INFORMATION);
    }

    /**
     * Mostra un alert
     * @param title titolo scelto
     * @param message messaggio scelto
     * @param tipo tipo di alert scelto
     */
    private void showAlert(String title, String message, Alert.AlertType tipo) {
        Platform.runLater(() -> {
            Alert alert = new Alert(tipo);
            alert.setTitle(title);
            alert.setHeaderText(null);
            alert.setContentText(message);
            alert.showAndWait();
        });
    }

    /**
     * Aggiorna i counter dei file rilevati, convertiti e scartati nella GUI
     */
    public void stampaRisultati() {
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