package gui;

import javafx.scene.Parent;
import javafx.fxml.FXML;
import javafx.scene.control.ToggleButton;
import configuration.configHandlers.config.ConfigData;
import configuration.configHandlers.config.ConfigInstance;
import configuration.configHandlers.config.ConfigReader;
import converters.exception.ConversionException;
import converters.exception.FileCreationException;
import converters.exception.IllegalExtensionException;
import objects.DirectoryWatcher;
import objects.Log;
import converters.Zipper;
import objects.Utility;
import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.LogManager;
import objects.Engine;
import javafx.application.Platform;
import javafx.fxml.FXMLLoader;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.stage.Modality;
import javafx.stage.Stage;

import java.awt.*;
import javafx.scene.control.TextArea;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;

import webService.client.ConverterWebServiceClient;
import webService.client.ConversionResult;

/**
 * Controller principale della UI per la gestione del monitoraggio cartelle e conversione file.
 */
public class MainViewController {

    @FXML
    private Label statusIndicator;
    @FXML
    private Label monitoringStatusLabel;
    @FXML
    private TextArea applicationLogArea;
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
    @FXML
    private ToggleButton themeToggle;

    // Riferimento all'applicazione principale
    private MainApp mainApp;
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

    private Engine engine;
    private Thread watcherThread;
    private final String configFile = "src/main/java/configuration/configFiles/config.json";

    /**
     * Metodo invocato automaticamente da JavaFX dopo il caricamento del FXML.
     * Inizializza il controller, i listener e carica la configurazione.
     */
    @FXML
    private void initialize() throws IOException {
        // 1) impostiamo subito l’aspetto del toggle
        themeToggle.setSelected(false);    // false → dark di default
        themeToggle.selectedProperty().addListener((obs, oldV, newV) -> {
            Parent root = themeToggle.getScene().getRoot();
            root.getStyleClass().removeAll("dark", "light");
            root.getStyleClass().add(newV ? "light" : "dark");
            // qui potresti salvare nelle Preferences la scelta dell’utente
        });

        // 2) inizializzo engine e client
        engine = new Engine();
        webServiceClient = new ConverterWebServiceClient("http://localhost:8080");

        // 3) UI setup e configurazione
        setupEventHandlers();
        logger.info("Applicazione avviata. Caricamento configurazione...");

        ConfigInstance ci = new ConfigInstance(new File(configFile));
        ConfigData.update(ci);
        loadConfiguration();

        // 4) se nel config ho il monitor a start, lo avvio
        if (monitorAtStart) {
            toggleMonitoring();
        }
    }


    /**
     * Aggiorna lo stile del pulsante monitoraggio in base allo stato
     */
    private void updateMonitoringButtonStyle() {
        if (isMonitoring) {
            // Quando monitora -> colore acquamarina come Directory
            MonitoringBtn.getStyleClass().removeAll("standard-btn");
            MonitoringBtn.getStyleClass().add("accent-btn");
            MonitoringBtn.setText("[ Ferma Monitoraggio ]");
        } else {
            // Quando non monitora -> colore grigio standard
            MonitoringBtn.getStyleClass().removeAll("accent-btn");
            MonitoringBtn.getStyleClass().add("standard-btn");
            MonitoringBtn.setText("[ Monitoraggio ]");
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
        if (engine == null || monitoredFolderPath == null) {
            launchAlertError("Engine o cartella monitorata non inizializzati.");
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
        updateMonitoringButtonStyle(); // Aggiorna lo stile del pulsante
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


    private void loadConfiguration() {
        if (engine == null) {
            launchAlertError("Engine non inizializzato.");
            return;
        }
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

    // Metodo che controlla l'esistenza di una directory e se non esiste la crea
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


    private void openConfigurationWindow() {
        if (engine == null) {
            logger.error("Engine non inizializzato.");
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
            // Mostra la finestra e attendi la chiusura
            addLogMessage("Editor configurazione aperto");
            configStage.showAndWait();

            // Ricarica la configurazione dopo la chiusura della finestra
            addLogMessage("Editor configurazione chiuso");
            loadConfiguration();
            interruptWatcher();
            watcherThread = new Thread(new DirectoryWatcher(monitoredFolderPath, this));
            watcherThread.start();
        } catch (IOException e) {
            launchAlertError("Impossibile aprire l'editor di configurazione: " + e.getMessage());
        }
    }

    private void openConversionConfigurationWindow() {
        if (engine == null) {
            launchAlertError("Engine non inizializzato.");
            return;
        }
        try {
            addLogMessage("Apertura editor configurazione della conversione...");

            FXMLLoader loader = new FXMLLoader();
            loader.setLocation(getClass().getResource("/ConversionConfigWindow.fxml"));
            Parent configWindow = loader.load();

            // Crea lo stage per la finestra di configurazione
            Stage configStage = new Stage();
            configStage.setTitle("Editor Configurazione Conversione");
            configStage.initModality(Modality.WINDOW_MODAL);
            configStage.initOwner(MainApp.getPrimaryStage());
            configStage.setResizable(true);
            configStage.setMinWidth(700);
            configStage.setMinHeight(600);
            configStage.setScene(new Scene(configWindow));

            // Ottiene il controller e passa i riferimenti necessari
            ConversionConfigWindowController controller = loader.getController();
            controller.setDialogStage(configStage);
            // Mostra la finestra e attendi la chiusura
            logger.info("Editor configurazione conversione aperto");
            configStage.showAndWait();
        } catch (IOException e) {
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

    public void addLogMessage(String message) {
        Platform.runLater(() -> {
            String timestamp = java.time.LocalTime.now().toString().substring(0, 8);
            String logEntry = "• [" + timestamp + "] " + message;

            // Per TextArea, aggiungi alla fine del testo esistente
            String currentText = applicationLogArea.getText();
            if (currentText == null || currentText.isEmpty() || currentText.equals("• [hh:mm:ss] Avvio monitoraggio…\n• [hh:mm:ss] File convertiti…\n• …")) {
                applicationLogArea.setText(logEntry);
            } else {
                applicationLogArea.setText(currentText + "\n" + logEntry);
            }
        });
    }

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
     * Fa comparire il dialog per selezionare il formato nel quale convertire il/i file
     * @param srcFile file di partenza
     */
    public void launchDialogConversion(File srcFile) {
        if (srcFile == null || engine == null) {
            logger.error("File sorgente o Engine non valido.");
            launchAlertError("File sorgente o Engine non valido.");
            return;
        }

        Platform.runLater(() -> fileRicevuti++);
        String srcExtension;
        srcExtension = Utility.getExtension(srcFile);

        //Se c'è il flag prende l'estensione dei file contenuti e chiede il formato di destinazione uguale per tutti
        if(ConfigReader.getIsMultipleConversionEnabled() && Utility.getExtension(srcFile).equals("zip"))
            try{
                srcExtension = Zipper.extractFileExstension(srcFile);
            } catch (IOException e) {
                launchAlertError("Impossibile decomprimere il file");
            }catch (IllegalExtensionException e){
                launchAlertError("I file hanno formati diversi");
            }

        List<String> formats;
        try {
            // Prova prima il webservice per ottenere i formati
            if (webServiceClient.isServiceAvailable()) {
                try {
                    logger.info("Formati ottenuti da web service per {}", srcFile.getName());
                    formats = webServiceClient.getPossibleConversions(srcExtension);
                    addLogMessage("Formati ottenuti da web service per " + srcFile.getName());
                } catch (Exception wsError) { // TOFIX
                    logger.info("Errore web service per formati, uso engine locale: {}", wsError.getMessage());
                    addLogMessage("Errore web service per formati, uso engine locale: " + wsError.getMessage());
                    formats = engine.getPossibleConversions(srcExtension);
                }
            } else {
                logger.warn("Web service non disponibile, uso engine locale per {}", srcFile.getName());
                addLogMessage("Web service non disponibile, uso engine locale per " + srcFile.getName());
                formats = engine.getPossibleConversions(srcExtension);
            }
        } catch (Exception e) { // Exception ammessa nel programma
            launchAlertError(e.getMessage());
            Platform.runLater(() -> {
                fileScartati++;
                stampaRisultati();
            });
            return;
        }

        List<String> finalFormats = formats;
        String finalSrcExtension = srcExtension;
        //Mostra il dialog per selezionare il formato di output
        Platform.runLater(() -> {
            ChoiceDialog<String> dialog = new ChoiceDialog<>(finalFormats.get(0), finalFormats);
            dialog.setTitle("Seleziona Formato");
            dialog.setHeaderText("Converti " + srcFile.getName() + " in...");
            dialog.setContentText("Formato desiderato:");
            Optional<String> result = dialog.showAndWait();
            //Se il dialog ha ritornato un formato per la conversione, viene istanziato un nuovo thread che se ne occupa
            result.ifPresent(chosenFormat -> {
                new Thread(() -> performConversionWithFallback(srcFile, chosenFormat, finalSrcExtension)).start();
            });
        });
    }

    private void performConversionWithFallback(File srcFile, String targetFormat, String srcExtension) {

        String outputFileName = srcFile.getName().replaceFirst("\\.[^\\.]+$", "") + "." + targetFormat;
        File outputDestinationFile = new File(convertedFolderPath, outputFileName);
        // PRIMO TENTATIVO: USA WEBSERVICE
        boolean webServiceSuccess = false;
        if (webServiceClient.isServiceAvailable()) {
            try {
                addLogMessage("Tentativo conversione tramite web service...");
                ConversionResult result = webServiceClient.convertFile(srcFile, targetFormat, outputDestinationFile/*, password, mergeImages*/);

                if (result.isSuccess()) {
                    // Verifica che il file convertito sia stato effettivamente salvato
                    if (outputDestinationFile.exists()) {
                        addLogMessage("Conversione WEB SERVICE riuscita: " + result.getMessage());
                        webServiceSuccess = true;
                    } else {
                        throw new FileCreationException("Il file convertito non è stato salvato correttamente dal web service");
                    }
                } else {
                    throw new ConversionException("Web service ha restituito errore: " + result.getError());
                }
            } catch (Exception wsError) { // TOFIX
                addLogMessage("Web service fallito: " + wsError.getMessage());

                // Pulisci eventuale file parzialmente creato
                if (outputDestinationFile.exists()) {
                    try {
                        Files.delete(outputDestinationFile.toPath());
                        addLogMessage("File parziale eliminato per retry con engine locale");
                    } catch (IOException cleanupError) {
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
                engine.conversione(srcExtension, targetFormat, srcFile);

                addLogMessage("Conversione ENGINE LOCALE riuscita");

                // Per l'engine locale, il file originale è già stato gestito automaticamente
                Platform.runLater(() -> {
                    fileConvertiti++;
                    stampaRisultati();
                    launchAlertSuccess(srcFile);
                });
                return; // Esci qui se engine locale ha successo

            } catch (Exception engineError) { // Exception ammessa nel programma
                launchAlertError(engineError.getMessage());
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
    }

    private void moveOriginalFileAfterSuccess(File originalFile) {
        try {
            if (originalFile.exists()) {
                // Elimina il file originale dalla cartella monitorata
                Files.delete(originalFile.toPath());
                addLogMessage("File originale eliminato dalla cartella monitorata: " + originalFile.getName());
            }
        } catch (IOException e) {
            addLogMessage("Errore nella gestione del file originale: " + e.getMessage());
        }
    }

    /**
     * Lancia un alert di errore col messaggio desiderato e aggiunge la relativa voce al log
     * @param message
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

    public static boolean launchDialogUnisci() throws Exception {
        CompletableFuture<Boolean> union = new CompletableFuture<>();

        Platform.runLater(() -> {
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

            union.complete(unisci);
        });

        try {
            return union.get(); // blocca finché la finestra non è chiusa
        } catch (Exception e) { // Exception ammessa nel programma
            throw new Exception("Impossibile ottenere il parametro Boolean");
        }
    }

    public static String launchDialogStringParameter() throws Exception {

        CompletableFuture<String> extraParameter = new CompletableFuture<>();
        Platform.runLater(() ->{
            TextInputDialog dialog = new TextInputDialog();
            dialog.setTitle("Password PDF");
            dialog.setHeaderText("Inserisci la password per il PDF (se richiesta)");
            dialog.setContentText("Password:");

            Optional<String> result = dialog.showAndWait();
            result.ifPresent(pwd -> Log.addMessage("Password ricevuta: " + (pwd.isEmpty() ? "(vuota)" : "(nascosta)")));
            String parameter = result.orElse(null);
            logger.info("Password ricevuta: {}", parameter == null || parameter.isEmpty() ? "(vuota)" : "(nascosta)");
            extraParameter.complete(parameter);
        });
        try {
            return extraParameter.get(); // blocca finché la finestra non è chiusa
        } catch (Exception e) { // Exception ammessa nel programma
            throw new Exception("Impossibile ottenere il parametro String");
        }
    }

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
