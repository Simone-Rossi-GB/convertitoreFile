package gui;

import converter.Log;
import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.LogManager;
import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Scene;
import javafx.scene.layout.Pane;
import javafx.stage.Stage;

import java.io.IOException;

public class MainApp extends Application {

    private static Stage primaryStage;
    private static final Logger logger = LogManager.getLogger(MainApp.class);

    @Override
    public void start(Stage primaryStage) {

        Log.addMessage("");
        Log.addMessage("Applicazione avviata");
        this.primaryStage = primaryStage;
        this.primaryStage.setTitle("File Converter Manager");
        this.primaryStage.setResizable(false); // Opzionale: impedisce il ridimensionamento

        loadMainView();


    }

    private void loadMainView() {
        try {
            // Carica il file FXML
            FXMLLoader loader = new FXMLLoader();
            loader.setLocation(MainApp.class.getResource("/GraphicalMenu.fxml"));

            // Carica come Pane (non VBox) per corrispondere all'FXML
            Pane mainView = loader.load();

            // Ottieni il controller e passagli il riferimento all'app
            MainViewController controller = loader.getController();
            controller.setMainApp(this);

            // Crea e mostra la scena con le dimensioni dell'FXML
            Scene scene = new Scene(mainView, 990, 770);
            primaryStage.setScene(scene);
            primaryStage.show();

            primaryStage.setOnCloseRequest(event -> {
                Log.addMessage("Applicazione chiusa.");
                Log.close();
            });

        } catch (IOException e) {
            e.printStackTrace();
            System.err.println("Errore nel caricare GraphicalMenu.fxml: " + e.getMessage());

            // Mostra errore dettagliato
            showErrorDialog(
                    "Impossibile caricare GraphicalMenu.fxml\n" +
                            "Verifica che il file sia in src/main/resources/GraphicalMenu.fxml\n\n" +
                            "Errore: " + e.getMessage());
        }
    }

    /**
     * Mostra un dialog di errore dettagliato
     */
    private void showErrorDialog(String message) {
        javafx.scene.control.Alert alert = new javafx.scene.control.Alert(javafx.scene.control.Alert.AlertType.ERROR);
        alert.setTitle("Errore FXML");
        alert.setHeaderText("Errore di configurazione");
        alert.setContentText(message);
        alert.showAndWait();

        System.exit(1); // Termina l'applicazione in caso di errore critico
    }

    public static Stage getPrimaryStage() {
        return primaryStage;
    }

    public static void main(String[] args) {
        launch(args);
    }
}