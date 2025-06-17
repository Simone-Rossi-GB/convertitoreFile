package gui;

import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Scene;
import javafx.scene.layout.BorderPane;
import javafx.stage.Stage;

import java.io.IOException;

public class MainApp extends Application {

    private Stage primaryStage;
    private BorderPane rootLayout;

    @Override
    public void start(Stage primaryStage) {
        this.primaryStage = primaryStage;
        this.primaryStage.setTitle("Convertitore Email - EML/MSG to PDF");

        initRootLayout();
        showMainView();
    }

    /**
     * Inizializza il layout principale dell'applicazione
     */
    public void initRootLayout() {
        try {
            // Carica il layout principale dal file FXML
            FXMLLoader loader = new FXMLLoader();
            loader.setLocation(MainApp.class.getResource("/view/RootLayout.fxml"));
            rootLayout = (BorderPane) loader.load();

            // Mostra la scena con il layout principale
            Scene scene = new Scene(rootLayout);
            primaryStage.setScene(scene);
            primaryStage.show();

        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    /**
     * Mostra la vista principale dentro il layout principale
     */
    public void showMainView() {
        try {
            // Carica la vista principale
            FXMLLoader loader = new FXMLLoader();
            loader.setLocation(MainApp.class.getResource("src/main/java/gui/view/MainView.fxml"));
            BorderPane mainView = (BorderPane) loader.load();

            // Imposta la vista principale al centro del layout principale
            rootLayout.setCenter(mainView);

            // Accesso al controller per eventuali configurazioni
            MainAppController controller = loader.getController();
            controller.setMainApp(this);

        } catch (IOException e) {
            e.printStackTrace();
        }
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