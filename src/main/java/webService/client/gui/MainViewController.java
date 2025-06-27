package webService.client.gui;

import webService.client.gui.jsonHandler.*;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Parent;
import javafx.fxml.FXML;
import javafx.scene.control.ToggleButton;
import webService.server.converters.exception.*;
import javafx.scene.image.ImageView;
import javafx.scene.layout.Background;
import javafx.scene.layout.Border;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.scene.shape.Rectangle;
import javafx.stage.StageStyle;
import webService.client.objects.*;
import webService.server.converters.Zipper;
import webService.server.converters.exception.ConversionException;
import webService.server.converters.exception.IllegalExtensionException;
import webService.client.configuration.configHandlers.config.*;
import webService.client.configuration.configHandlers.conversionContext.*;
import webService.client.configuration.configExceptions.*;

import webService.client.objects.DirectoryWatcher;
import webService.server.converters.Zipper;
import webService.client.objects.Utility;
import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.LogManager;
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
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.*;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;

import webService.client.ConverterWebServiceClient;
import webService.client.ConversionResult;
import webService.server.converters.exception.ConversionException;
import webService.server.converters.exception.IllegalExtensionException;
import javafx.geometry.Side;


import javax.xml.ws.WebServiceException;

/**
 * Controller principale della UI per la gestione del monitoraggio cartelle e conversione file.
 */
public class MainViewController {

    public HBox header;
    @FXML
    private Label mainTitleLabel;
    @FXML
    private Label logAreaTitle;
    @FXML
    private Label conversionSettingsTitle;
    @FXML
    private TextArea applicationLogArea;
    @FXML
    private Label detectedFilesCounter;
    @FXML
    private Label successfulConversionsCounter;
    @FXML
    private Label failedConversionsCounter;
    @FXML
    private Button langButton;
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
    @FXML
    private Label detectedFilesLabel;
    @FXML
    private Label successfulConversionsLabel;
    @FXML
    private Label failedConversionsLabel;
    @FXML
    private Label directoryLabelTitle;

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
    private String currentProgressLogLine = null; // Tiene traccia della riga di progresso corrente
    private boolean isShowingProgress = false; // Flag per sapere se stiamo mostrando una barra di progresso

    private Thread watcherThread;
    private final ContextMenu menu = new ContextMenu();
    private ResourceBundle bundle;

    private final Map<String, Locale> localeMap = new HashMap<>();
    {
        localeMap.put("it.png", new Locale("it", "IT"));
        localeMap.put("en.png", new Locale("en", "EN"));
        localeMap.put("de.png", new Locale("de", "DE"));
    }

    static class Delta {
        double x, y;
    }
    private final String configFile = "src/main/java/webService/client/configuration/configFiles/config.json";
    private final String conversionContextFile = "src/main/java/webService/client/configuration/configFiles/conversionContext.json";
    private InstanceConversionContextWriter iccw = null;
    /**
     * Metodo invocato automaticamente da JavaFX dopo il caricamento dell'FXML.
     * Inizializza il controller, i listener e carica la configurazione.
     */
    @FXML
    private void initialize() throws IOException {
        JsonConfig jsonConfig = ConfigManager.readConfig();
        // 1) impostiamo subito l'aspetto del toggle
        themeToggle.setSelected(jsonConfig.getTheme().equals("light"));
        themeToggle.selectedProperty().addListener((obs, oldV, newV) -> {
            Parent root = themeToggle.getScene().getRoot();
            root.getStyleClass().removeAll("dark", "light");

            // Logica diretta: se è selezionato → light, altrimenti → dark
            if (newV) {
                root.getStyleClass().add("light");
            } else {
                root.getStyleClass().add("dark");
            }
        });
        bundle = ResourceBundle.getBundle("languages.MessagesBundle", MainApp.getCurrentLocale());
        refreshUITexts();
        updateLangButtonGraphic(MainApp.getCurrentLocale());
        initializeLanguageMenu();
        Delta dragDelta = new Delta();

        header.setOnMousePressed(event -> {
            dragDelta.x = event.getSceneX();
            dragDelta.y = event.getSceneY();
        });

        header.setOnMouseDragged(event -> {
            MainApp.getPrimaryStage().setX(event.getScreenX() - dragDelta.x);
            MainApp.getPrimaryStage().setY(event.getScreenY() - dragDelta.y);
        });

        // 2) inizializzo engine e client
        webServiceClient = new ConverterWebServiceClient("http://localhost:8080");

        // 3) UI setup e configurazione
        setupEventHandlers();
        logger.info("Applicazione avviata. Caricamento configurazione...");

        ConfigInstance ci = new ConfigInstance(new File(configFile));
        ConfigData.update(ci);
        loadConfiguration();
        ConversionContextInstance cci = new ConversionContextInstance(new File(conversionContextFile));
        iccw = new InstanceConversionContextWriter(new File(conversionContextFile));
        ConversionContextData.update(cci);
        //Carica la configurazione di base
        loadConfiguration();
        if (monitorAtStart) {
            toggleMonitoring();
        }
    }

    @FXML
    private void showLanguageMenu() {
        if (menu.isShowing()) {
            menu.hide();
        } else {
            menu.show(langButton, Side.RIGHT, 0, 0);
        }
    }

    private void initializeLanguageMenu() {
        menu.getStyleClass().add("context-menu");
        for (Map.Entry<String, Locale> entry : localeMap.entrySet()) {
            String imageFile = entry.getKey();
            Locale locale = entry.getValue();
            ImageView icon = new ImageView(getClass().getResource("/flags/" + imageFile).toExternalForm());
            icon.setFitWidth(24);
            icon.setFitHeight(20);
            icon.getStyleClass().addAll("flag-icon");

            // Etichetta della lingua (es: "IT")
            Label label = new Label(locale.getCountry());
            label.getStyleClass().addAll("flag-name");

            // Layout orizzontale: bandiera + sigla
            HBox content = new HBox(icon, label);
            content.getStyleClass().add("lang-menu-item");

            content.setAlignment(Pos.CENTER_LEFT);

            content.setSpacing(6);
            content.setPadding(new Insets(4, 4, 4, 4));

            CustomMenuItem item = new CustomMenuItem(content, false);

            item.setOnAction(ev -> {
                MainApp.setCurrentLocale(locale);
                updateLangButtonGraphic(locale);
                bundle = ResourceBundle.getBundle("languages.MessagesBundle", locale);
                refreshUITexts();
                menu.hide();
            });

            menu.getItems().add(item);
        }
    }

    public void refreshUITexts() {
        mainTitleLabel.setText(bundle.getString("label.mainTitle"));
        logAreaTitle.setText(bundle.getString("label.logAreaTitle"));
        detectedFilesLabel.setText(bundle.getString("label.detectedFilesLabel"));
        successfulConversionsLabel.setText(bundle.getString("label.successfulConversionsLabel"));
        failedConversionsLabel.setText(bundle.getString("label.failedConversionsLabel"));
        directoryLabelTitle.setText(bundle.getString("label.directoryLabelTitle"));
        caricaFileBtn.setText(bundle.getString("btn.caricaFileBtn"));
        fileConvertitiBtn.setText(bundle.getString("btn.fileConvertitiBtn"));
        conversioniFalliteBtn.setText(bundle.getString("btn.conversioniFalliteBtn"));
        conversionSettingsTitle.setText(bundle.getString("label.conversionSettingsTitle"));
        configBtn.setText(bundle.getString("btn.configBtn"));
        conversionConfigBtn.setText(bundle.getString("btn.conversionConfigBtn"));
        if (isMonitoring){
            MonitoringBtn.setText(bundle.getString("btn.MonitoringBtn") + " ON");
        } else {
            MonitoringBtn.setText(bundle.getString("btn.MonitoringBtn") + " OFF");
        }
    }

    private void updateLangButtonGraphic(Locale locale) {
        String imageFile = getFlagFileForLocale(locale);

        ImageView icon = new ImageView(getClass().getResource("/flags/" + imageFile).toExternalForm());
        icon.setFitWidth(24);
        icon.setFitHeight(20);
        icon.getStyleClass().add("flag-icon");

        langButton.setGraphic(icon);
    }

    // Metodo per ottenere il nome del file immagine (es: "it.png") associato a un dato Locale
    private String getFlagFileForLocale(Locale locale) {
        // Scorre tutte le entry della mappa (file immagine → Locale) come stream
        return localeMap.entrySet()
                // Filtra le entry che hanno un Locale uguale a quello passato come parametro
                .stream()
                .filter(e -> e.getValue().equals(locale))
                // Trasforma l'entry nella sua chiave, ovvero il nome del file immagine
                .map(Map.Entry::getKey)
                // Restituisce il primo match trovato, se c'è
                .findFirst()
                // Altrimenti, se non c'è match, ritorna "it.png" come default
                .orElse("it.png");
    }

    /**
     * Aggiorna lo stile del pulsante monitoraggio in base allo stato
     */
    private void updateMonitoringButtonStyle() {
        if (isMonitoring) {
            // Quando monitora -> colore acquamarina come Directory
            MonitoringBtn.getStyleClass().removeAll("standard-btn");
            MonitoringBtn.getStyleClass().add("accent-btn");
            MonitoringBtn.setText(bundle.getString("btn.MonitoringBtn")+" ON");
        } else {
            // Quando non monitora -> colore grigio standard
            MonitoringBtn.getStyleClass().removeAll("accent-btn");
            MonitoringBtn.getStyleClass().add("standard-btn");
            MonitoringBtn.setText(bundle.getString("btn.MonitoringBtn")+" OFF");
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
            VBox configWindow = loader.load();

            // Crea lo stage per la finestra di configurazione
            Stage configStage = new Stage();
            configStage.setTitle("Editor Configurazione");
            configStage.initModality(Modality.WINDOW_MODAL);
            configStage.initOwner(MainApp.getPrimaryStage());
            configStage.setResizable(false);

            // DIMENSIONI PIÙ PICCOLE
            configStage.setWidth(598);
            configStage.setHeight(780);

            // Crea la scene
            Scene scene = new Scene(configWindow);

            configStage.initStyle(StageStyle.TRANSPARENT);
            scene.setFill(Color.TRANSPARENT);

            javafx.scene.shape.Rectangle clip = new Rectangle();
            clip.setArcWidth(20);
            clip.setArcHeight(20);
            clip.widthProperty().bind(configWindow.widthProperty());
            clip.heightProperty().bind(configWindow.heightProperty());
            configWindow.setClip(clip);

            // **APPLICA IL TEMA CORRENTE ALLA CONFIG WINDOW**
            boolean isLightTheme = themeToggle.isSelected();
            // **APPLICA IL TEMA CORRENTE ALLA CONVERSION CONFIG WINDOW**
            if (isLightTheme) {
                configWindow.getStyleClass().add("light");
            } else {
                configWindow.getStyleClass().add("dark");
            }

            // **Carica il CSS per il tema moderno**
            try {
                scene.getStylesheets().add(getClass().getResource("/styles/modern-config-theme.css").toExternalForm());
                logger.info("CSS moderno caricato per la finestra di configurazione");
            } catch (Exception cssError) {
                logger.warn("Impossibile caricare il CSS moderno: " + cssError.getMessage());
                // Fallback - prova percorso alternativo
                try {
                    scene.getStylesheets().add(getClass().getResource("/styles/modern-config-theme.css").toExternalForm());
                    logger.info("CSS moderno caricato da percorso alternativo");
                } catch (Exception cssError2) {
                    logger.error("CSS non trovato in nessun percorso: " + cssError2.getMessage());
                }
            }

            configStage.setScene(scene);

            // Ottiene il controller e passa i riferimenti necessari
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

    /**
     * Apre la finestra per modificare il file conversionContext.json
     */
    private void openConversionConfigurationWindow() {
        try {
            addLogMessage("Apertura editor configurazione della conversione...");

            // Carica il file FXML
            FXMLLoader loader = new FXMLLoader();
            loader.setLocation(getClass().getResource("/ConversionConfigWindow.fxml"));
            VBox configWindow = loader.load(); // CAMBIATO: VBox invece di Parent

            // Crea lo stage per la finestra di configurazione
            Stage configStage = new Stage();
            configStage.setTitle("Editor Configurazione Conversione");
            configStage.initModality(Modality.WINDOW_MODAL);
            configStage.initOwner(MainApp.getPrimaryStage());
            configStage.setResizable(false);

            // DIMENSIONI AGGIORNATE
            configStage.setWidth(750);
            configStage.setHeight(880);

            // Crea la scene
            Scene scene = new Scene(configWindow);

            // STILE TRASPARENTE E ANGOLI ARROTONDATI
            configStage.initStyle(StageStyle.TRANSPARENT);
            scene.setFill(Color.TRANSPARENT);

            javafx.scene.shape.Rectangle clip = new Rectangle();
            clip.setArcWidth(20);
            clip.setArcHeight(20);
            clip.widthProperty().bind(configWindow.widthProperty());
            clip.heightProperty().bind(configWindow.heightProperty());
            configWindow.setClip(clip);
            boolean isLightTheme = themeToggle.isSelected();
            // **APPLICA IL TEMA CORRENTE ALLA CONVERSION CONFIG WINDOW**
            if (isLightTheme) {
                configWindow.getStyleClass().add("light");
            } else {
                configWindow.getStyleClass().add("dark");
            }

            // **Carica il CSS MODERNO con fallback come per la ConfigWindow**
            try {
                scene.getStylesheets().add(getClass().getResource("/styles/modern-conversion-config-theme.css").toExternalForm());
                logger.info("CSS moderno caricato per la finestra di configurazione conversione");
            } catch (Exception cssError) {
                logger.warn("Impossibile caricare il CSS moderno: " + cssError.getMessage());
                // Fallback - prova percorso alternativo
                try {
                    scene.getStylesheets().add(getClass().getResource("/styles/modern-conversion-config-theme.css").toExternalForm());
                    logger.info("CSS moderno caricato da percorso alternativo");
                } catch (Exception cssError2) {
                    logger.error("CSS non trovato in nessun percorso: " + cssError2.getMessage());
                }
            }

            configStage.setScene(scene);

            // Ottieni il controller e passa i riferimenti necessari
            ConversionConfigWindowController controller = loader.getController();
            controller.setDialogStage(configStage);
            // Mostra la finestra e attendi la chiusura
            logger.info("Editor configurazione conversione aperto");
            configStage.showAndWait();
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
        boolean isLightTheme = themeToggle.isSelected();
        // **APPLICA IL TEMA CORRENTE ALLA CONVERSION CONFIG WINDOW**
        if (isLightTheme) {
            ConfigManager.writeConfig(new JsonConfig("light",MainApp.getCurrentLocale().getLanguage()));
        } else {
            ConfigManager.writeConfig(new JsonConfig("dark" ,MainApp.getCurrentLocale().getLanguage()));
        }
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
        Platform.runLater(() -> {fileRicevuti++; stampaRisultati();});
        String srcExtension;

        List<String> formats = null;
        try {
            srcExtension = extractSrcExtension(srcFile);
            //tenta di ottenere i formati dal webService
            formats = getExtensionsFromWebService(srcExtension);
            List<String> finalFormats = formats;
            //Mostra il dialog per selezionare il formato di output
            Platform.runLater(() -> {
                // Usa il metodo helper per rilevare automaticamente il tema
                boolean isLightTheme = DialogHelper.detectCurrentTheme();

                ChoiceDialog<String> dialog = DialogHelper.createModernChoiceDialog(
                        finalFormats.get(0),
                        finalFormats,
                        "Seleziona Formato",
                        bundle.getString("label.formatChoiceMsg").replace("$", srcFile.getName()),
                        bundle.getString("label.formatChoiceComboBox"),
                        isLightTheme
                );

                Optional<String> result = dialog.showAndWait();
                //Se il dialog ha ritornato un formato per la conversione, viene istanziato un nuovo thread che se ne occupa
                result.ifPresent(chosenFormat -> {
                    new Thread(() -> performConversion(srcFile, chosenFormat)).start();
                });
            });
        } catch (Exception e) { // Exception ammessa nel programma
            launchAlertError(e.getMessage());
            Platform.runLater(() -> {
                fileScartati++;
                stampaRisultati();
            });
            return;
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
        if(webService.server.configuration.configHandlers.conversionContext.ConversionContextReader.getIsMultipleConversionEnabled() && Utility.getExtension(file).equals("zip")) {
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
    private void performConversion(File srcFile, String targetFormat) throws ConversionException, WebServiceException {
        String filename = srcFile.getName();

        try {
            // Fase 1: Inizializzazione
            updateProgressInLog(filename, 0, "Inizializzazione...");
            Thread.sleep(200);

            // Fase 2: Verifica servizio
            updateProgressInLog(filename, 10, "Verifica servizio...");
            if (!webServiceClient.isServiceAvailable()) {
                addFinalLogMessage(filename + " - Servizio non disponibile");
                aggiornaCounterScartati();
                moveFileToErrorFolder(srcFile);
                return;
            }

            // Fase 3: Caricamento file
            updateProgressInLog(filename, 20, "Caricamento file...");
            Thread.sleep(300);

            // CORREZIONE: Calcola il nome del file convertito con la nuova estensione
            String baseFileName = srcFile.getName();
            int lastDotIndex = baseFileName.lastIndexOf('.');
            if (lastDotIndex > 0) {
                baseFileName = baseFileName.substring(0, lastDotIndex);
            }
            String convertedFileName = baseFileName + "." + targetFormat;
            File outputDestinationFile = new File(convertedFolderPath, convertedFileName);

            // Fase 4: Invio al server il conversion context
            updateProgressInLog(filename, 40, "Invio parametri al server...");
            Thread.sleep(200);

            iccw.writeDestinationFormat(targetFormat);
            ConversionContextData.update(new ConversionContextInstance(new File(conversionContextFile)));
            logger.info("Tentativo conversione tramite web service...");

            // Fase 5: Conversione in corso
            updateProgressInLog(filename, 60, "Conversione in corso...");
            ConversionResult result = webServiceClient.convertFile(srcFile, targetFormat, outputDestinationFile, conversionContextFile);

            // Fase 6: Elaborazione risultato
            updateProgressInLog(filename, 80, "Elaborazione risultato...");
            Thread.sleep(200);

            if (result.isSuccess()) {
                // Fase 7: Verifica file salvato
                updateProgressInLog(filename, 90, "Verifica file salvato...");
                Thread.sleep(100);

                // CORREZIONE: Verifica se esiste il file convertito con il nome corretto
                boolean fileExists = outputDestinationFile.exists();

                // DEBUGGING: Log per capire cosa sta succedendo
                logger.info("Verifica file convertito:");
                logger.info("  - Nome originale: {}", srcFile.getName());
                logger.info("  - Nome convertito atteso: {}", convertedFileName);
                logger.info("  - Percorso completo: {}", outputDestinationFile.getAbsolutePath());
                logger.info("  - File esiste: {}", fileExists);

                // Se il file non esiste con il nome calcolato, prova a cercarlo nella cartella
                if (!fileExists) {
                    logger.info("File non trovato con nome calcolato, cerco nella cartella...");
                    File convertedDir = new File(convertedFolderPath);
                    File[] files = convertedDir.listFiles();
                    if (files != null) {
                        for (File f : files) {
                            logger.info("  - File trovato: {}", f.getName());
                            // Verifica se il file inizia con lo stesso nome base
                            if (f.getName().startsWith(baseFileName + ".")) {
                                outputDestinationFile = f;
                                fileExists = true;
                                logger.info("  - MATCH! Trovato file convertito: {}", f.getName());
                                break;
                            }
                        }
                    }
                }

                if (fileExists) {
                    // Fase 8: Completamento
                    updateProgressInLog(filename, 100, "Completato!");
                    Thread.sleep(100);

                    addFinalLogMessage("✅ Conversione riuscita: " + filename + " → " + outputDestinationFile.getName());
                    launchAlertSuccess(srcFile); // Alert success

                    // AGGIORNA CONTATORE SUCCESSI
                    Platform.runLater(() -> {
                        fileConvertiti++;
                        stampaRisultati();
                    });

                } else {
                    // File non salvato correttamente
                    updateProgressInLog(filename, 100, "Errore: file non trovato");
                    addFinalLogMessage("❌ Errore: " + filename + " - File convertito non trovato in " + convertedFolderPath);
                    aggiornaCounterScartati();
                    moveFileToErrorFolder(srcFile);
                }
            } else {
                // Web service ha restituito errore
                updateProgressInLog(filename, 100, "Errore dal server");
                addFinalLogMessage("❌ Errore server: " + filename + " - " + result.getError());
                aggiornaCounterScartati();
                moveFileToErrorFolder(srcFile);
            }

        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            updateProgressInLog(filename, 100, "Interrotto");
            addFinalLogMessage("⚠️ Conversione interrotta: " + filename);
            aggiornaCounterScartati();
            moveFileToErrorFolder(srcFile);

        } catch (Exception e) {
            updateProgressInLog(filename, 100, "Errore imprevisto");
            addFinalLogMessage("❌ Errore imprevisto: " + filename + " - " + e.getMessage());
            logger.error("Errore durante conversione di " + filename, e);
            aggiornaCounterScartati();
            moveFileToErrorFolder(srcFile);
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
                    progressBar.append("■"); // Quadrato pieno
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
                if (!"Log dell'applicazione...".equals(applicationLogArea.getText()) &&
                        !applicationLogArea.getText().isEmpty()) {
                    applicationLogArea.setText(applicationLogArea.getText() + "\n" + newProgressLine);
                } else {
                    applicationLogArea.setText(newProgressLine);
                }
                isShowingProgress = true;
            }

            currentProgressLogLine = newProgressLine;

            // Se completato, resetta i flag DOPO un breve delay
            if (progress >= 100) {
                Platform.runLater(() -> {
                            isShowingProgress = false;
                            currentProgressLogLine = null;
                });
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
     * @param message
     */
    /**
     * Lancia un alert di errore col messaggio desiderato e aggiunge la relativa voce al log
     * @param message messaggio di errore
     */
    public void launchAlertError(String message) {
        logger.error(message);
        Platform.runLater(() -> {
            // Usa il metodo helper per rilevare automaticamente il tema
            boolean isLightTheme = DialogHelper.detectCurrentTheme();

            Alert alert = DialogHelper.createModernAlert(
                    Alert.AlertType.ERROR,
                    "Errore",
                    message,
                    isLightTheme
            );

            alert.showAndWait();
        });
    }

    /**
     * Lancia un alert di info col messaggio di conversione riuscita e aggiunge la relativa voce al log
     * @param file File da convertire
     */
    public void launchAlertSuccess(File file) {
        String message = bundle.getString("label.conversionSuccessMsg");
        message = message.replace("$", file.getName());
        logger.info(message);
        String finalMessage = message;
        Platform.runLater(() -> {
            // Usa il metodo helper per rilevare automaticamente il tema
            boolean isLightTheme = DialogHelper.detectCurrentTheme();

            Alert alert = DialogHelper.createModernAlert(
                    Alert.AlertType.INFORMATION,
                    bundle.getString("label.conversionSuccess"),
                    finalMessage,
                    isLightTheme
            );

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