package gui;

import javafx.scene.layout.HBox;
import javafx.scene.paint.Color;
import javafx.scene.shape.Rectangle;
import javafx.stage.StageStyle;
import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.LogManager;
import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Scene;
import javafx.scene.layout.Pane;
import javafx.stage.Stage;

public class MainApp extends Application {

    private static Stage primaryStage;
    private static final Logger logger = LogManager.getLogger(MainApp.class);

    static class Delta {
        double x, y;
    }

    @Override
    public void start(Stage stage) throws Exception {
        // Salvo lo stage primario per eventuali dialog/modal
        MainApp.primaryStage = stage;

        // Carico l'FXML principale
        FXMLLoader loader = new FXMLLoader(getClass().getResource("/GraphicalMenu.fxml"));
        Pane root = loader.load();
        HBox header = (HBox) root.lookup("#header");

        // Passo l'app al controller
        MainViewController controller = loader.getController();
        controller.setMainApp(this);

        // Creo la scena, senza dimensioni hard-coded (l'FXML le contiene)
        Scene scene = new Scene(root);

        // Carico entrambi i CSS (scaricati in src/main/resources/styles/)
        scene.getStylesheets().addAll(
                getClass().getResource("/styles/modern-main-theme.css").toExternalForm()
        );

        // Applico il tema di default
        root.getStyleClass().add("root-dark");

        // Configuro lo stage
        stage.setTitle("File Converter Manager");
        stage.setResizable(false);
        stage.initStyle(StageStyle.TRANSPARENT);
        scene.setFill(Color.TRANSPARENT);

        Rectangle clip = new Rectangle();
        clip.setArcWidth(20);
        clip.setArcHeight(20);
        clip.widthProperty().bind(root.widthProperty());
        clip.heightProperty().bind(root.heightProperty());
        root.setClip(clip);

        stage.setWidth(900);       // Larghezza esatta
        stage.setHeight(653);
        stage.setScene(scene);
        stage.show();

        Delta dragDelta = new Delta();

        header.setOnMousePressed(event -> {
            dragDelta.x = event.getSceneX();
            dragDelta.y = event.getSceneY();
        });

        header.setOnMouseDragged(event -> {
            primaryStage.setX(event.getScreenX() - dragDelta.x);
            primaryStage.setY(event.getScreenY() - dragDelta.y);
        });

        // Log di chiusura
        stage.setOnCloseRequest(evt -> logger.info("Applicazione chiusa."));
        logger.info("======== APPLICAZIONE AVVIATA ========");
    }

    public static Stage getPrimaryStage() {
        return primaryStage;
    }

    public static void main(String[] args) {
        launch(args);
    }
}
