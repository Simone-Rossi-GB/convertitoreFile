package webService.client.gui;

import javafx.scene.image.Image;
import javafx.scene.layout.StackPane;
import webService.client.gui.jsonHandler.*;

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

import java.util.Locale;
import java.util.Objects;
import java.util.ResourceBundle;

public class MainApp extends Application {

    private static Stage primaryStage;
    private static Locale currentLocale; // valore iniziale
    private static final Logger logger = LogManager.getLogger(MainApp.class);

    @Override
    public void start(Stage stage) throws Exception {
        JsonConfig config = ConfigManager.readConfig();
        // Salvo lo stage primario per eventuali dialog/modal
        MainApp.primaryStage = stage;
        // Carico l'FXML principale
        currentLocale = new Locale(config.getLang(), config.getLang().toUpperCase());
        ResourceBundle bundle = ResourceBundle.getBundle("languages.MessagesBundle", currentLocale);
        FXMLLoader loader = new FXMLLoader(getClass().getResource("/GraphicalMenu.fxml"), bundle);
        Pane root = loader.load();

        // Passo l'app al controller
        MainViewController controller = loader.getController();
        controller.setMainApp(this);

        // Creo la scena, senza dimensioni hard-coded (l'FXML le contiene)
        Pane overlayPane = new Pane(); // Per la guida visiva
        overlayPane.setPickOnBounds(false); // Lascia passare eventi se vuoi

        // Applico il tema di default
        root.getStyleClass().add(config.getTheme());
        overlayPane.getStyleClass().add(config.getTheme());
        controller.setRoot(root);

        StackPane layeredRoot = new StackPane(root, overlayPane);
        controller.setOverlayPane(overlayPane);

        Scene scene = new Scene(layeredRoot);

        // Carico i CSS per il tema principale e per i dialog moderni
        scene.getStylesheets().addAll(
                getClass().getResource("/styles/modern-main-theme.css").toExternalForm(),
                getClass().getResource("/styles/tutorial-theme.css").toExternalForm()
        );

        // **AGGIUNGIAMO IL CSS PER I DIALOG MODERNI GLOBALMENTE**
        try {
            scene.getStylesheets().add(getClass().getResource("/css/modern-dialogs-theme.css").toExternalForm());
            logger.info("CSS dialog moderni caricato globalmente da /css/");
        } catch (Exception cssError) {
            try {
                scene.getStylesheets().add(getClass().getResource("/styles/modern-dialogs-theme.css").toExternalForm());
                logger.info("CSS dialog moderni caricato globalmente da /styles/");
            } catch (Exception cssError2) {
                logger.warn("Impossibile caricare CSS dialog moderni: " + cssError2.getMessage());
            }
        }

        // Configuro lo stage
        primaryStage.getIcons().add(new Image(Objects.requireNonNull(getClass().getResourceAsStream("/icons/app_icon.png"))));

        try { // Icona per MacOS
            if (System.getProperty("os.name").toLowerCase().contains("mac")) {
                Class<?> appClass = Class.forName("com.apple.eawt.Application");
                Object appInstance = appClass.getMethod("getApplication").invoke(null);

                Class<?> imageClass = Class.forName("java.awt.Image");
                java.awt.Image icon = java.awt.Toolkit.getDefaultToolkit().getImage(
                        getClass().getResource("/icons/app_icon.png")
                );

                appClass.getMethod("setDockIconImage", imageClass).invoke(appInstance, icon);
            }
        } catch (Exception e) {
            System.out.println("Dock icon non impostata: " + e.getMessage());
        }

        stage.setTitle("ByteBridge");
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

        // Log di chiusura
        stage.setOnCloseRequest(evt -> logger.info("Applicazione chiusa."));
        logger.info("======== APPLICAZIONE AVVIATA ========");
    }

    public static Stage getPrimaryStage() {
        return primaryStage;
    }

    public static Locale getCurrentLocale() { return currentLocale; }

    public static void setCurrentLocale(Locale locale) { currentLocale = locale; }

    public static void main(String[] args) {
        launch(args);
    }
}