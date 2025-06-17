package gui;

import Converters.EMLtoPDF;
import Converters.MSGtoPDF;
import javafx.concurrent.Task;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.stage.DirectoryChooser;
import javafx.stage.FileChooser;
import javafx.stage.Stage;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;
import java.util.List;

public class MainAppController {

    @FXML
    private ListView<File> fileListView;

    @FXML
    private TextField outputDirectoryField;

    @FXML
    private Button selectFilesButton;

    @FXML
    private Button selectOutputButton;

    @FXML
    private Button convertButton;

    @FXML
    private Button removeFileButton;

    @FXML
    private Button clearAllButton;

    @FXML
    private ProgressBar progressBar;

    @FXML
    private Label statusLabel;

    @FXML
    private TextArea logTextArea;

    // Riferimento all'applicazione principale
    private MainApp mainApp;

    /**
     * Inizializza il controller. Viene chiamato automaticamente dopo il caricamento del file FXML.
     */
    @FXML
    private void initialize() {
        // Configura la ListView per mostrare solo il nome del file
        fileListView.setCellFactory(listView -> new ListCell<File>() {
            @Override
            protected void updateItem(File file, boolean empty) {
                super.updateItem(file, empty);
                if (empty || file == null) {
                    setText(null);
                } else {
                    setText(file.getName());
                }
            }
        });

        // Abilita/disabilita pulsanti in base alla selezione
        removeFileButton.setDisable(true);
        convertButton.setDisable(true);

        // Listener per abilitare il pulsante rimuovi quando si seleziona un file
        fileListView.getSelectionModel().selectedItemProperty().addListener(
                (observable, oldValue, newValue) -> removeFileButton.setDisable(newValue == null)
        );

        // Listener per abilitare il pulsante converti
        fileListView.getItems().addListener((javafx.collections.ListChangeListener<File>) change ->
                updateConvertButtonState()
        );

        outputDirectoryField.textProperty().addListener((observable, oldValue, newValue) ->
                updateConvertButtonState()
        );

        // Inizializza lo status
        statusLabel.setText("Pronto");
        progressBar.setVisible(false);
    }

    /**
     * Aggiorna lo stato del pulsante converti
     */
    private void updateConvertButtonState() {
        boolean hasFiles = !fileListView.getItems().isEmpty();
        boolean hasOutputDir = !outputDirectoryField.getText().trim().isEmpty();
        convertButton.setDisable(!(hasFiles && hasOutputDir));
    }

    /**
     * Gestisce la selezione dei file da convertire
     */
    @FXML
    private void handleSelectFiles() {
        FileChooser fileChooser = new FileChooser();
        fileChooser.setTitle("Seleziona file email da convertire");

        // Filtri per i tipi di file supportati
        fileChooser.getExtensionFilters().addAll(
                new FileChooser.ExtensionFilter("File Email", "*.eml", "*.msg"),
                new FileChooser.ExtensionFilter("File EML", "*.eml"),
                new FileChooser.ExtensionFilter("File MSG", "*.msg"),
                new FileChooser.ExtensionFilter("Tutti i file", "*.*")
        );

        List<File> selectedFiles = fileChooser.showOpenMultipleDialog(getPrimaryStage());
        if (selectedFiles != null) {
            for (File file : selectedFiles) {
                if (!fileListView.getItems().contains(file)) {
                    fileListView.getItems().add(file);
                }
            }
            logTextArea.appendText("Aggiunti " + selectedFiles.size() + " file\n");
        }
    }

    /**
     * Gestisce la selezione della cartella di output
     */
    @FXML
    private void handleSelectOutput() {
        DirectoryChooser directoryChooser = new DirectoryChooser();
        directoryChooser.setTitle("Seleziona cartella di destinazione");

        File selectedDirectory = directoryChooser.showDialog(getPrimaryStage());
        if (selectedDirectory != null) {
            outputDirectoryField.setText(selectedDirectory.getAbsolutePath());
            logTextArea.appendText("Cartella di output: " + selectedDirectory.getAbsolutePath() + "\n");
        }
    }

    /**
     * Rimuove il file selezionato dalla lista
     */
    @FXML
    private void handleRemoveFile() {
        File selectedFile = fileListView.getSelectionModel().getSelectedItem();
        if (selectedFile != null) {
            fileListView.getItems().remove(selectedFile);
            logTextArea.appendText("Rimosso: " + selectedFile.getName() + "\n");
        }
    }

    /**
     * Pulisce tutta la lista dei file
     */
    @FXML
    private void handleClearAll() {
        int count = fileListView.getItems().size();
        fileListView.getItems().clear();
        logTextArea.appendText("Rimossi tutti i " + count + " file dalla lista\n");
    }

    /**
     * Avvia la conversione dei file
     */
    @FXML
    private void handleConvert() {
        if (fileListView.getItems().isEmpty()) {
            showAlert("Errore", "Nessun file selezionato per la conversione.");
            return;
        }

        String outputDir = outputDirectoryField.getText().trim();
        if (outputDir.isEmpty()) {
            showAlert("Errore", "Seleziona una cartella di destinazione.");
            return;
        }

        // Disabilita i controlli durante la conversione
        setControlsDisabled(true);
        progressBar.setVisible(true);
        progressBar.setProgress(0);
        statusLabel.setText("Conversione in corso...");

        // Crea un task per la conversione in background
        Task<Void> conversionTask = new Task<Void>() {
            @Override
            protected Void call() throws Exception {
                int totalFiles = fileListView.getItems().size();
                int processedFiles = 0;

                for (File file : fileListView.getItems()) {
                    if (isCancelled()) {
                        break;
                    }

                    try {
                        String fileName = file.getName();
                        String baseName = fileName.substring(0, fileName.lastIndexOf('.'));
                        String outputPath = outputDir + File.separator + baseName + ".pdf";

                        // Determina il tipo di file e usa il convertitore appropriato
                        File resultFile;
                        if (fileName.toLowerCase().endsWith(".eml")) {
                            // Usa il metodo statico legacy che accetta (File, String)
                            resultFile = EMLtoPDF.convertEmlToPdf(file, outputPath);
                        } else if (fileName.toLowerCase().endsWith(".msg")) {
                            // Usa il metodo statico legacy che accetta (File, String)
                            resultFile = MSGtoPDF.convertMsgToPdf(file, outputPath);
                        } else {
                            throw new IllegalArgumentException("Tipo di file non supportato: " + fileName);
                        }

                        // Log del successo
                        javafx.application.Platform.runLater(() ->
                                logTextArea.appendText("✓ Convertito: " + fileName + " → " + resultFile.getName() + "\n")
                        );

                        processedFiles++;
                        final double progress = (double) processedFiles / totalFiles;
                        javafx.application.Platform.runLater(() -> progressBar.setProgress(progress));

                    } catch (Exception e) {
                        final String errorMsg = "✗ Errore convertendo " + file.getName() + ": " + e.getMessage();
                        javafx.application.Platform.runLater(() ->
                                logTextArea.appendText(errorMsg + "\n")
                        );
                    }
                }

                return null;
            }

            @Override
            protected void succeeded() {
                javafx.application.Platform.runLater(() -> {
                    setControlsDisabled(false);
                    progressBar.setVisible(false);
                    statusLabel.setText("Conversione completata!");
                    logTextArea.appendText("=== Conversione terminata ===\n");
                });
            }

            @Override
            protected void failed() {
                javafx.application.Platform.runLater(() -> {
                    setControlsDisabled(false);
                    progressBar.setVisible(false);
                    statusLabel.setText("Errore durante la conversione");
                    logTextArea.appendText("=== Conversione fallita ===\n");
                });
            }
        };

        // Avvia il task in un thread separato
        Thread conversionThread = new Thread(conversionTask);
        conversionThread.setDaemon(true);
        conversionThread.start();
    }

    /**
     * Abilita/disabilita i controlli dell'interfaccia
     */
    private void setControlsDisabled(boolean disabled) {
        selectFilesButton.setDisable(disabled);
        selectOutputButton.setDisable(disabled);
        convertButton.setDisable(disabled);
        removeFileButton.setDisable(disabled);
        clearAllButton.setDisable(disabled);
        fileListView.setDisable(disabled);
    }

    /**
     * Mostra un alert di errore
     */
    private void showAlert(String title, String message) {
        Alert alert = new Alert(Alert.AlertType.ERROR);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }

    /**
     * Ritorna il primary stage
     */
    private Stage getPrimaryStage() {
        return mainApp.getPrimaryStage();
    }

    /**
     * Chiamato dall'applicazione principale per passare il riferimento
     */
    public void setMainApp(MainApp mainApp) {
        this.mainApp = mainApp;
    }
}