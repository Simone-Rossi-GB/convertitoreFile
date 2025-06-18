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
import java.util.concurrent.atomic.AtomicBoolean;

import converter.Engine;

import javax.swing.*;

public class MainViewController {

    // Riferimenti FXML agli elementi dell'interfaccia - CORRETTI
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

    // Pulsanti con nomi corretti dall'FXML
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

    // Percorsi delle cartelle (caricati dal JSON)
    private String monitoredFolderPath = "Non configurata";
    private String convertedFolderPath = "Non configurata";
    private String failedFolderPath = "Non configurata";
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

        // Carica configurazione dal JSON
        loadConfiguration();
        System.out.println(monitorAtStart);
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
     * Apre la finestra di configurazione.
     */
    private void openConfigurationWindow() {
        try {
            addLogMessage("Apertura editor configurazione...");

            // Carica il file FXML per la finestra di configurazione
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

        } catch (IOException e) {
            addLogMessage("Errore nell'apertura dell'editor configurazione: " + e.getMessage());
            showAlert("Errore", "Impossibile aprire l'editor di configurazione: " + e.getMessage());
            e.printStackTrace();
        }
    }

    /**
     * Apre la cartella specificata nel file explorer di sistema.
     *
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
     * Carica la configurazione da file JSON tramite Engine.
     */
    private void loadConfiguration() {
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

    /**
     * Aggiunge un messaggio al log dell'applicazione.
     *
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
     *
     * @param srcFile file sorgente da convertire
     */
    public void launchDialogConversion(File srcFile) {
        AtomicBoolean unisci = new AtomicBoolean(false);
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
        List<String> finalFormats = formats;
        Platform.runLater(() -> {
            ChoiceDialog<String> dialog = new ChoiceDialog<>(finalFormats.get(0), finalFormats);
            dialog.setTitle("Seleziona Formato");
            dialog.setHeaderText("Converti " + srcFile.getName() + " in...");
            dialog.setContentText("Formato desiderato:");
            Optional<String> result = dialog.showAndWait();
            result.ifPresent(format -> {
                try {
                    //dialog per gestire la password
                    if(srcExtension.equals("pdf")){
                        if(format.equals("jpg")){
                            unisci.set(launchDialogUnisci());
                        }
                        String password = launchDialogPdf();
                        if(password != null){
                            if(format.equals("jpg"))
                                engine.conversione(srcExtension, format, srcFile, password, unisci.get());
                            else
                                engine.conversione(srcExtension, format, srcFile, password);
                        }
                        else{
                            if(format.equals("jpg"))
                                engine.conversione(srcExtension, format, srcFile, unisci.get());
                            else
                                engine.conversione(srcExtension, format, srcFile);
                        }
                    }
                    else {
                        engine.conversione(srcExtension, format, srcFile);
                    }
                    fileConvertiti++;
                    launchAlertSuccess(srcFile);
                } catch (Exception e) {
                    fileScartati++;
                    launchAlertError(e.getMessage());
                }
                stampaRisultati();
            });
        });
    }


    private String launchDialogPdf(){
        // Campo password
        JPasswordField passwordField = new JPasswordField(20);

        // Layout del messaggio
        JPanel panel = new JPanel(new BorderLayout(5, 5));
        panel.add(new JLabel("Il PDF è protetto? Inserisci password se sì:"), BorderLayout.NORTH);
        panel.add(passwordField, BorderLayout.CENTER);

        // Dialog con bottoni Sì/No
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
     *
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
     *
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
     *
     * @param title   titolo finestra alert
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
     *
     * @return stage principale
     */
    private Stage getPrimaryStage() {
        return mainApp.getPrimaryStage();
    }

    /**
     * Setta il riferimento all'app principale.
     *
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
}