package gui;

import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Scene;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;

import java.io.IOException;

public class MainApp extends Application {

    private Stage primaryStage;

    @Override
    public void start(Stage primaryStage) {
        this.primaryStage = primaryStage;
        this.primaryStage.setTitle("Convertitore File");

        loadMainView();
    }

    private void loadMainView() {
        try {
            // Carica il file FXML dal percorso corretto
            FXMLLoader loader = new FXMLLoader();
            // CORREZIONE: usa solo "/MainView.fxml" non il percorso completo
            loader.setLocation(MainApp.class.getResource("/MainView.fxml"));

            System.out.println("Cercando MainView.fxml in: " + MainApp.class.getResource("/MainView.fxml"));

            VBox mainView = loader.load();

            // Ottieni il controller e passagli il riferimento all'app
            MainViewController controller = loader.getController();
            controller.setMainApp(this);

            // Crea e mostra la scena
            Scene scene = new Scene(mainView, 900, 700);
            primaryStage.setScene(scene);
            primaryStage.show();

        } catch (IOException e) {
            e.printStackTrace();
            System.err.println("Errore nel caricare MainView.fxml: " + e.getMessage());
            System.err.println("Percorso cercato: " + MainApp.class.getResource("/MainView.fxml"));
            System.err.println("Assicurati che il file sia in src/main/resources/MainView.fxml");

            // Mostra errore dettagliato
            showErrorDialog(
                    "Impossibile caricare MainView.fxml\n" +
                            "Verifica che il file sia in src/main/resources/MainView.fxml\n\n" +
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

        primaryStage.close();
    }

    public Stage getPrimaryStage() {
        return primaryStage;
    }

    public static void main(String[] args) {
        launch(args);
    }
}