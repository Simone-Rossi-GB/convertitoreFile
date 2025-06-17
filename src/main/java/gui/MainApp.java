package gui;

import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Scene;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;

import java.io.IOException;

public class MainApp extends Application {

    private Stage primaryStage;
    private BorderPane rootLayout;

    @Override
    public void start(Stage primaryStage) {
        this.primaryStage = primaryStage;
        this.primaryStage.setTitle("Convertitore Email - EML/MSG to PDF");

        showMainView();
    }

    /**
     * Mostra la vista principale dentro il layout principale
     */
    public void showMainView() {
        try {
            // Carica la vista principale - percorso corretto
            FXMLLoader loader = new FXMLLoader();
            loader.setLocation(MainApp.class.getResource("view/MainView.fxml"));

            // Se MainView.fxml non esiste, crea una vista semplice
            VBox mainView;
            MainAppController controller;

            try {
                mainView = (VBox) loader.load();
                controller = loader.getController();

                // Imposta la vista principale al centro del layout principale
                if (rootLayout != null) {
                    rootLayout.setCenter(mainView);
                } else {
                    Scene scene = new Scene(mainView, 800, 600);
                    primaryStage.setScene(scene);
                }

                // Configura il controller
                if (controller != null) {
                    controller.setMainApp(this);
                }
            } catch (Exception e) {
                // Crea una vista semplice se il file FXML non esiste
                System.err.println("MainView.fxml non trovato, creo vista semplice");
            }

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    /**
     * Crea una vista principale semplice se il file FXML non esiste
     */
    private VBox createSimpleMainView() {
        VBox vbox = new VBox();
        vbox.setSpacing(10);
        vbox.setStyle("-fx-padding: 20");

        javafx.scene.control.Label titleLabel = new javafx.scene.control.Label("Convertitore Email to PDF");
        titleLabel.setStyle("-fx-font-size: 18px; -fx-font-weight: bold;");

        javafx.scene.control.TextField pathField = new javafx.scene.control.TextField();
        pathField.setPromptText("Seleziona cartella di output");

        javafx.scene.control.Button selectButton = new javafx.scene.control.Button("Seleziona File");
        javafx.scene.control.Button convertButton = new javafx.scene.control.Button("Converti");

        javafx.scene.control.TextArea logArea = new javafx.scene.control.TextArea();
        logArea.setPrefRowCount(15);

        vbox.getChildren().addAll(titleLabel, pathField, selectButton, convertButton, logArea);

        return vbox;
    }

    /**
     * Ritorna il stage principale
     */
    public Stage getPrimaryStage() {
        return primaryStage;
    }

    public static void main(String[] args) {
        launch(args);
    }
}