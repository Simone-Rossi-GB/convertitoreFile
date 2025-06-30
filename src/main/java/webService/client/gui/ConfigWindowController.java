package webService.client.gui;

import javafx.scene.Parent;
import javafx.scene.layout.Pane;
import webService.client.configuration.configExceptions.JsonStructureException;
import webService.client.configuration.configHandlers.config.*;
import javafx.scene.layout.VBox;
import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.LogManager;
import javafx.application.Platform;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.stage.DirectoryChooser;
import javafx.stage.Stage;

import com.google.gson.JsonParser;
import com.google.gson.JsonSyntaxException;
import webService.client.gui.tutorial.GuideStep;
import webService.client.gui.tutorial.VisualGuide;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import java.util.ResourceBundle;


/**
 * Controller della finestra di configurazione dell'applicazione.
 * Gestisce le directory di monitoraggio, successo, errore, il flag di avvio automatico
 * e la visualizzazione della sezione "conversions" del file di configurazione JSON.
 */
public class ConfigWindowController {

    // Campi per le directory
    @FXML private TextField monitoredDirField;
    @FXML private TextField successDirField;
    @FXML private TextField errorDirField;

    // Pulsanti per sfogliare le directory
    @FXML private Button browseMonitoredBtn;
    @FXML private Button browseSuccessBtn;
    @FXML private Button browseErrorBtn;
    @FXML private Button toggleMonitorBtn;
    @FXML private Button tutorialConfigPage;

    @FXML private Label configTitle;
    @FXML private Label configTitleDesc;
    @FXML private Label directoryLabel;
    @FXML private Label monitoredDirectoryLabel;
    @FXML private Label successfulDirectoryLabel;
    @FXML private Label errorDirectoryLabel;
    @FXML private Label startMonitoringLabel;

    // Area per le conversioni JSON (sola lettura)
    @FXML private Button saveButton;
    @FXML private Button cancelButton;

    @FXML private VBox configHeaderContainer;

    private Stage dialogStage;
    private boolean monitorAtStart = false;
    private static final Logger logger = LogManager.getLogger(ConfigWindowController.class);
    private static Locale locale = null;
    private static ResourceBundle bundle;

    private Pane overlayPane;
    @FXML
    private Parent root;


    /**
     * Inizializza il controller della finestra di configurazione.
     */
    @FXML
    private void initialize() {
        // Configura il TextArea per la visualizzazione (sola lettura)
        // Configura i campi di testo per le directory
        setupDirectoryFields();
        loadCurrentConfiguration();
        if (locale == null || !locale.getLanguage().equals(MainApp.getCurrentLocale().getLanguage())){
            locale = MainApp.getCurrentLocale();
            bundle = ResourceBundle.getBundle("languages.MessagesBundle", locale);
        }
        updateMonitorToggleButton();
        refreshUITexts(locale);
        tutorialConfigPage.setOnAction(e -> {
            avviaGuida();
        });
    }

    public void avviaGuida(){

        List<GuideStep> steps = Arrays.asList(
                new GuideStep(monitoredDirField, bundle.getString("tutorial.config.step1.message")),
                new GuideStep(successDirField, bundle.getString("tutorial.config.step3.message")),
                new GuideStep(errorDirField, bundle.getString("tutorial.config.step5.message")),
                new GuideStep(toggleMonitorBtn, bundle.getString("tutorial.config.step7.message"))
        );
        System.out.println("OverlayPane: " + overlayPane);
        System.out.println("OverlayPane in scene: " + (overlayPane.getScene() != null));
        System.out.println("Target bounds: " + monitoredDirField.localToScene(monitoredDirField.getBoundsInLocal()));


        VisualGuide guida = new VisualGuide(overlayPane, steps);
        guida.start();
    }

    public void refreshUITexts(Locale locale) {
        configTitle.setText(bundle.getString("label.configTitle"));
        configTitleDesc.setText(bundle.getString("label.configTitleDesc"));
        directoryLabel.setText(bundle.getString("label.directoryLabel"));
        monitoredDirectoryLabel.setText(bundle.getString("label.monitoredDirectoryLabel"));
        monitoredDirField.setPromptText(bundle.getString("field.monitoredDirField"));
        successfulDirectoryLabel.setText(bundle.getString("label.successfulDirectoryLabel"));
        successDirField.setPromptText(bundle.getString("field.successDirField"));
        errorDirectoryLabel.setText(bundle.getString("label.errorDirectoryLabel"));
        errorDirField.setPromptText(bundle.getString("field.errorDirField"));
        startMonitoringLabel.setText(bundle.getString("label.startMonitoringLabel"));
        browseMonitoredBtn.setText(bundle.getString("btn.browse"));
        browseSuccessBtn.setText(bundle.getString("btn.browse"));
        browseErrorBtn.setText(bundle.getString("btn.browse"));
        saveButton.setText(bundle.getString("btn.saveButton"));
        cancelButton.setText(bundle.getString("btn.closeButton"));
    }

    /**
     * Rimuovi gli stili inline dai campi directory e usa solo CSS
     */
    private void setupDirectoryFields() {
        // Rimuovi gli stili inline - usa solo le classi CSS
        // Non impostare più stili qui, lascia che il CSS gestisca tutto

        // Listener per validazione dei percorsi in tempo reale
        monitoredDirField.textProperty().addListener((obs, oldVal, newVal) -> validateDirectoryPath(newVal, monitoredDirField));
        successDirField.textProperty().addListener((obs, oldVal, newVal) -> validateDirectoryPath(newVal, successDirField));
        errorDirField.textProperty().addListener((obs, oldVal, newVal) -> validateDirectoryPath(newVal, errorDirField));
    }

    public void setOverlayPane(Pane overlayPane) {
        this.overlayPane = overlayPane;
    }

    /**
     * Valida un percorso di directory e applica stili CSS appropriati
     */
    private void validateDirectoryPath(String path, TextField field) {
        if (path == null || path.trim().isEmpty()) {
            // Stato neutro - rimuovi tutte le classi di validazione
            field.getStyleClass().removeAll("valid-path", "invalid-path");
            field.setStyle(""); // Reset dello stile inline
            return;
        }

        File dir = new File(path.trim());
        if (dir.exists() && dir.isDirectory()) {
            // Path valido - AZZURRO (AQUAMARINE)
            field.getStyleClass().removeAll("invalid-path");
            if (!field.getStyleClass().contains("valid-path")) {
                field.getStyleClass().add("valid-path");
            }
            // Stile inline azzurro aquamarine
            field.setStyle("-fx-background-color: rgba(79, 209, 199, 0.1); " +
                    "-fx-border-color: #4FD1C7; " +
                    "-fx-border-width: 2; " +
                    "-fx-border-radius: 8; " +
                    "-fx-background-radius: 8; " +
                    "-fx-effect: dropshadow(gaussian, rgba(79, 209, 199, 0.3), 4, 0, 0, 1);");
        } else {
            // Path non valido - ROSSO
            field.getStyleClass().removeAll("valid-path");
            if (!field.getStyleClass().contains("invalid-path")) {
                field.getStyleClass().add("invalid-path");
            }
            // Stile inline rosso
            field.setStyle("-fx-background-color: rgba(245, 101, 101, 0.1); " +
                    "-fx-border-color: #F56565; " +
                    "-fx-border-width: 2; " +
                    "-fx-border-radius: 8; " +
                    "-fx-background-radius: 8; " +
                    "-fx-effect: dropshadow(gaussian, rgba(245, 101, 101, 0.3), 4, 0, 0, 1);");
        }
    }



    /**
     * Imposta lo stage e configura il drag della finestra
     */
    public void setDialogStage(Stage dialogStage) {
        this.dialogStage = dialogStage;

        // Configura il drag dell'header
        if (configHeaderContainer != null) {
            Delta dragDelta = new Delta();

            configHeaderContainer.setOnMousePressed(event -> {
                dragDelta.x = event.getSceneX();
                dragDelta.y = event.getSceneY();
            });

            configHeaderContainer.setOnMouseDragged(event -> {
                dialogStage.setX(event.getScreenX() - dragDelta.x);
                dialogStage.setY(event.getScreenY() - dragDelta.y);
            });

            // Cursore di movimento
            configHeaderContainer.setOnMouseEntered(event -> {
                configHeaderContainer.setCursor(javafx.scene.Cursor.MOVE);
            });

            configHeaderContainer.setOnMouseExited(event -> {
                configHeaderContainer.setCursor(javafx.scene.Cursor.DEFAULT);
            });
        }
    }

    /**
     * Carica e mostra la configurazione corrente nei campi dell'interfaccia.
     */
    private void loadCurrentConfiguration() {
        // Carica i campi principali dalla configurazione
        monitoredDirField.setText(ConfigReader.getMonitoredDir());
        successDirField.setText(ConfigReader.getSuccessOutputDir());
        errorDirField.setText(ConfigReader.getErrorOutputDir());

        monitorAtStart = ConfigReader.getIsMonitoringEnabledAtStart();

        logger.info("Configurazione caricata correttamente");
    }

    /**
     * Sfoglia per selezionare la directory monitorata.
     */
    @FXML
    private void browseMonitoredDirectory(ActionEvent event) {
        browseDirectory("Seleziona Cartella da Monitorare", monitoredDirField);
    }

    /**
     * Sfoglia per selezionare la directory di successo.
     */
    @FXML
    private void browseSuccessDirectory(ActionEvent event) {
        browseDirectory("Seleziona Cartella File Convertiti", successDirField);
    }

    /**
     * Sfoglia per selezionare la directory degli errori.
     */
    @FXML
    private void browseErrorDirectory(ActionEvent event) {
        browseDirectory("Seleziona Cartella Errori", errorDirField);
    }

    /**
     * Mostra un DirectoryChooser e imposta il percorso selezionato nel campo corrispondente.
     */
    private void browseDirectory(String title, TextField targetField) {
        DirectoryChooser directoryChooser = new DirectoryChooser();
        directoryChooser.setTitle(title);

        // Imposta la directory iniziale se il campo contiene un percorso valido
        String currentPath = targetField.getText().trim();
        if (!currentPath.isEmpty()) {
            File currentDir = new File(currentPath);
            if (currentDir.exists() && currentDir.isDirectory()) {
                directoryChooser.setInitialDirectory(currentDir);
            }
        }

        File selectedDirectory = directoryChooser.showDialog(dialogStage);
        if (selectedDirectory != null) {
            logger.info("Selezionata directory: {}", selectedDirectory.getAbsolutePath());
            targetField.setText(selectedDirectory.getAbsolutePath());
        }
    }

    /**
     * Cambia il valore del monitoraggio all'avvio.
     */
    @FXML
    private void toggleMonitorAtStart(ActionEvent event) {
        monitorAtStart = !monitorAtStart;
        updateMonitorToggleButton();
    }

    private void updateMonitorToggleButton() {
        ResourceBundle bundle = ResourceBundle.getBundle("languages.MessagesBundle", locale);
        if (monitorAtStart) {
            // ATTIVO - Mostra "ATTIVO" e applica stile azzurro
            toggleMonitorBtn.setText(bundle.getString("btn.active"));

            // Rimuovi la classe normale e aggiungi quella attiva
            toggleMonitorBtn.getStyleClass().removeAll("config-toggle-btn");
            if (!toggleMonitorBtn.getStyleClass().contains("active-state")) {
                toggleMonitorBtn.getStyleClass().add("active-state");
            }
        } else {
            // INATTIVO - Mostra "INATTIVO" e applica stile grigio
            toggleMonitorBtn.setText(bundle.getString("btn.inactive"));

            // Rimuovi la classe attiva e usa quella normale
            toggleMonitorBtn.getStyleClass().removeAll("active-state");
            if (!toggleMonitorBtn.getStyleClass().contains("config-toggle-btn")) {
                toggleMonitorBtn.getStyleClass().add("config-toggle-btn");
            }
        }
    }


    /**
     * Verifica che tutte le directory siano valide o tenti di crearle se mancanti.
     */
    private boolean validateDirectories() throws IOException {
        String[] directories = {
                monitoredDirField.getText().trim(),
                successDirField.getText().trim(),
                errorDirField.getText().trim()
        };

        String[] labels = {
                "Cartella Monitorata",
                "Cartella Success",
                "Cartella Error"
        };

        for (int i = 0; i < directories.length; i++) {
            if (directories[i].isEmpty()) {
                logger.error("{} non specificata", labels[i]);
                showAlert("Errore di validazione", labels[i] + " deve essere specificata.", Alert.AlertType.ERROR);
                return false;
            }

            // Crea la directory se non esiste
            try {
                Path path = Paths.get(directories[i]);
                if (!Files.exists(path)) {
                    Files.createDirectories(path);
                    logger.warn("Creata directory mancante: {}", path);
                }
            } catch (IOException e) {
                throw new IOException("Impossibile creare la cartella");
            }
        }
        return true;
    }

    /**
     * Salva la configurazione modificata.
     */
    @FXML
    private void saveConfiguration(ActionEvent event) throws IOException {
        // Valida le directory prima di salvare
        if (!validateDirectories()) {
            return;
        }

        InstanceConfigWriter wr = new InstanceConfigWriter(ConfigData.getJsonFile());
        wr.writeMonitoredDir(monitoredDirField.getText());
        wr.writeErrorOutputDir(errorDirField.getText());
        wr.writeSuccessOutputDir(successDirField.getText());
        wr.writeIsMonitoringEnabledAtStart(monitorAtStart);
        ConfigData.update(new ConfigInstance(ConfigData.getJsonFile()));
        logger.info("Configurazione salvata con successo");
        dialogStage.close();

    }

    /**
     * Verifica se una stringa è un JSON valido.
     */
    private boolean isValidJson(String jsonString) {
        try {
            JsonParser.parseString(jsonString);
            return true;
        } catch (JsonSyntaxException e) {
            return false;
        }
    }



    /**
     * Chiude la finestra senza salvare.
     */
    /**
     * Chiude la finestra senza salvare.
     */
    @FXML
    private void cancelAndClose(ActionEvent event) {
        try {
            // Controlla se ci sono modifiche non salvate
            if (hasUnsavedChanges()) {
                logger.info("Tentativo di chiusura con modifiche non salvate");

                // Usa il metodo helper per rilevare automaticamente il tema
                boolean isLightTheme = DialogHelper.detectCurrentTheme();

                Alert confirmAlert = DialogHelper.createModernAlert(
                        Alert.AlertType.CONFIRMATION,
                        "Unsaved Changes",
                        bundle.getString("label.closeWOsaving"),
                        isLightTheme
                );

                confirmAlert.showAndWait().ifPresent(response -> {
                    if (response.getButtonData().isDefaultButton()) {
                        dialogStage.close();
                    }
                });
            } else {
                logger.info("Finestra di configurazione chiusa senza modifiche");
                dialogStage.close();
            }
        } catch (NullPointerException e) {
            dialogStage.close();
            showAlert("Attenzione", "Impossibile verificare le modifiche. Chiudo la finestra.", Alert.AlertType.WARNING);
        }
    }

    /**
     * Verifica se ci sono modifiche non salvate.
     */
    private boolean hasUnsavedChanges() {
        try {
            // Controlla i campi directory
            String currentMonitored = ConfigReader.getMonitoredDir();
            String currentSuccess = ConfigReader.getSuccessOutputDir();
            String currentError = ConfigReader.getErrorOutputDir();
            boolean currentMonitorAtStart = ConfigReader.getIsMonitoringEnabledAtStart();

            return !currentMonitored.equals(monitoredDirField.getText().trim()) ||
                    !currentSuccess.equals(successDirField.getText().trim()) ||
                    !currentError.equals(errorDirField.getText().trim()) ||
                    currentMonitorAtStart != monitorAtStart;

        } catch (JsonStructureException e) {
            return true; // In caso di errore, assumi che ci siano modifiche
        }
    }

    /**
     * Mostra un alert.
     */
    private void showAlert(String title, String message, Alert.AlertType alertType) {
        Platform.runLater(() -> {
            // Usa il metodo helper per rilevare automaticamente il tema
            boolean isLightTheme = DialogHelper.detectCurrentTheme();

            Alert alert = DialogHelper.createModernAlert(alertType, title, message, isLightTheme);
            alert.showAndWait();
        });
    }


    static class Delta {
        double x, y;
    }
}
