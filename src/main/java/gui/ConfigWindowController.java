package gui;

import converter.Engine;
import javafx.application.Platform;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.TextArea;
import javafx.stage.Stage;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.JsonNode;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;

public class ConfigWindowController {

    @FXML private TextArea configTextArea;
    @FXML private Label statusLabel;
    @FXML private Button saveButton;
    @FXML private Button cancelButton;
    @FXML private Button validateButton;

    private Engine engine;
    private Stage dialogStage;
    private MainViewController mainController;

    /**
     * Inizializza il controller della finestra di configurazione.
     */
    @FXML
    private void initialize() {
        // Configura il TextArea per una migliore esperienza di editing
        configTextArea.setStyle("-fx-font-family: 'Consolas', 'Monaco', monospace; " +
                "-fx-font-size: 13px; " +
                "-fx-background-color: #f8f9fa; " +
                "-fx-text-fill: #2c3e50; " +
                "-fx-border-color: transparent;");
    }

    /**
     * Imposta l'engine e il controller principale.
     * @param engine istanza dell'engine
     * @param mainController controller principale
     */
    public void setEngine(Engine engine, MainViewController mainController) {
        this.engine = engine;
        this.mainController = mainController;
        loadCurrentConfiguration();
    }

    /**
     * Imposta lo stage della finestra di dialogo.
     * @param dialogStage stage della finestra
     */
    public void setDialogStage(Stage dialogStage) {
        this.dialogStage = dialogStage;
    }

    /**
     * Carica la configurazione corrente nel TextArea.
     */
    private void loadCurrentConfiguration() {
        try {
            // Ottieni la configurazione corrente in formato JSON
            String currentConfig = engine.getConfigAsJson();
            configTextArea.setText(currentConfig);
            updateStatus("Configurazione caricata", false);
        } catch (Exception e) {
            updateStatus("Errore nel caricamento: " + e.getMessage(), true);
            showAlert("Errore", "Impossibile caricare la configurazione corrente: " + e.getMessage(), Alert.AlertType.ERROR);
        }
    }

    /**
     * Salva la configurazione modificata.
     */
    @FXML
    private void saveConfiguration(ActionEvent actionEvent) {
        try {
            String configText = configTextArea.getText().trim();

            // Valida prima il JSON
            if (!isValidJson(configText)) {
                updateStatus("JSON non valido! Controlla la sintassi.", true);
                showAlert("Errore di validazione", "Il JSON inserito non è valido. Controlla la sintassi.", Alert.AlertType.ERROR);
                return;
            }

            engine.setConfigFromJson(configText);

            updateStatus("Configurazione salvata con successo!", false);

            // Aggiorna il log nel controller principale
            if (mainController != null) {
                mainController.addLogMessage("Configurazione aggiornata dall'editor");
            }

            // Mostra conferma e chiudi
            showAlert("Successo", "Configurazione salvata con successo!", Alert.AlertType.INFORMATION);

            // Chiudi la finestra dopo un breve delay
            Platform.runLater(() -> {
                try {
                    Thread.sleep(1000);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                }
                dialogStage.close();
            });

        } catch (Exception e) {
            updateStatus("Errore nel salvataggio: " + e.getMessage(), true);
            showAlert("Errore", "Impossibile salvare la configurazione: " + e.getMessage(), Alert.AlertType.ERROR);
        }
    }

    /**
     * Valida il JSON inserito dall'utente.
     */
    @FXML
    private void validateJson(ActionEvent actionEvent) {
        String configText = configTextArea.getText().trim();

        if (configText.isEmpty()) {
            updateStatus("Il campo è vuoto", true);
            return;
        }

        if (isValidJson(configText)) {
            updateStatus("JSON valido ✓", false);
            showAlert("Validazione", "Il JSON è sintatticamente corretto!", Alert.AlertType.INFORMATION);
        } else {
            updateStatus("JSON non valido ✗", true);
            showAlert("Validazione", "Il JSON contiene errori di sintassi. Controlla parentesi, virgole e apici.", Alert.AlertType.WARNING);
        }
    }

    /**
     * Chiude la finestra senza salvare.
     */
    @FXML
    private void cancelAndClose(ActionEvent actionEvent) {
        // Controlla se ci sono modifiche non salvate
        try {
            String currentText = configTextArea.getText().trim();
            String originalConfig = engine.getConfigAsJson();

            if (!currentText.equals(originalConfig)) {
                Alert confirmAlert = new Alert(Alert.AlertType.CONFIRMATION);
                confirmAlert.setTitle("Modifiche non salvate");
                confirmAlert.setHeaderText("Ci sono modifiche non salvate");
                confirmAlert.setContentText("Sei sicuro di voler chiudere senza salvare?");

                confirmAlert.showAndWait().ifPresent(response -> {
                    if (response.getButtonData().isDefaultButton()) {
                        dialogStage.close();
                    }
                });
            } else {
                dialogStage.close();
            }
        } catch (Exception e) {
            dialogStage.close();
        }
    }

    /**
     * Verifica se il testo è un JSON valido.
     * @param jsonText testo da validare
     * @return true se è JSON valido, false altrimenti
     */
    private boolean isValidJson(String jsonText) {
        try {
            ObjectMapper mapper = new ObjectMapper();
            JsonNode node = mapper.readTree(jsonText);
            return node != null;
        } catch (Exception e) {
            return false;
        }
    }

    /**
     * Aggiorna il label di stato.
     * @param message messaggio da visualizzare
     * @param isError true se è un messaggio di errore
     */
    private void updateStatus(String message, boolean isError) {
        Platform.runLater(() -> {
            statusLabel.setText(message);
            if (isError) {
                statusLabel.setStyle("-fx-text-fill: #e74c3c; -fx-font-weight: bold;");
            } else {
                statusLabel.setStyle("-fx-text-fill: #27ae60; -fx-font-weight: bold;");
            }
        });
    }

    /**
     * Mostra un alert.
     * @param title titolo dell'alert
     * @param message messaggio dell'alert
     * @param alertType tipo di alert
     */
    private void showAlert(String title, String message, Alert.AlertType alertType) {
        Platform.runLater(() -> {
            Alert alert = new Alert(alertType);
            alert.setTitle(title);
            alert.setHeaderText(null);
            alert.setContentText(message);
            alert.showAndWait();
        });
    }
}