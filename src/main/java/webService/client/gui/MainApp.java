package webService.client.gui;

import javafx.scene.image.Image;
import javafx.scene.layout.StackPane;
import webService.client.gui.jsonHandler.*;
import webService.client.auth.AuthManager;

import javafx.scene.paint.Color;
import javafx.scene.shape.Rectangle;
import javafx.stage.StageStyle;
import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.LogManager;
import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Scene;
import javafx.scene.layout.Pane;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;

import java.util.Locale;
import java.util.Objects;
import java.util.ResourceBundle;

public class MainApp extends Application {

    private static Stage primaryStage;
    private static Stage loginStage;
    private static Locale currentLocale;
    private static final Logger logger = LogManager.getLogger(MainApp.class);
    private static AuthManager authManager;

    @Override
    public void start(Stage stage) throws Exception {
        JsonConfig config = ConfigManager.readConfig();
        MainApp.primaryStage = stage;
        MainApp.authManager = new AuthManager("http://172.20.10.3:8080");

        currentLocale = new Locale(config.getLang(), config.getLang().toUpperCase());

        // AVVIA PRIMA LA SCHERMATA DI LOGIN
        showLoginScreen();
    }

    public String getToken(){
        return authManager.getJwtToken();
    }

    /**
     * Mostra la schermata di login
     */
    public static void showLoginScreen() {
        try {
            logger.info("======== AVVIO SCHERMATA LOGIN ========");

            // Carica il bundle per la lingua
            ResourceBundle bundle = ResourceBundle.getBundle("languages.MessagesBundle", currentLocale);

            // Carica l'FXML del login
            FXMLLoader loader = new FXMLLoader(MainApp.class.getResource("/LoginScreen.fxml"), bundle);
            VBox loginRoot = loader.load();

            // Ottieni il controller del login
            LoginController loginController = loader.getController();
            loginController.setMainApp(MainApp.class); // Passa riferimento per callback

            // Crea lo stage del login
            loginStage = new Stage();
            loginStage.setTitle("ByteBridge - Login");
            loginStage.setResizable(false);
            loginStage.initStyle(StageStyle.TRANSPARENT);

            // Crea la scena del login
            Scene loginScene = new Scene(loginRoot);
            loginScene.setFill(Color.TRANSPARENT);

            // Applica il tema
            JsonConfig config = ConfigManager.readConfig();
            loginRoot.getStyleClass().add(config.getTheme());

            // Carica CSS per login
            try {
                loginScene.getStylesheets().add(MainApp.class.getResource("/styles/login-theme.css").toExternalForm());
                logger.info("CSS login caricato");
            } catch (Exception cssError) {
                logger.warn("Impossibile caricare CSS login: " + cssError.getMessage());
            }

            // Angoli arrotondati
            Rectangle clip = new Rectangle();
            clip.setArcWidth(20);
            clip.setArcHeight(20);
            clip.widthProperty().bind(loginRoot.widthProperty());
            clip.heightProperty().bind(loginRoot.heightProperty());
            loginRoot.setClip(clip);

            // Configura e mostra lo stage del login
            loginStage.setScene(loginScene);
            loginStage.setWidth(450);
            loginStage.setHeight(550);
            loginStage.centerOnScreen();
            loginStage.show();

            // Nasconde il main stage se è visibile
            if (primaryStage != null && primaryStage.isShowing()) {
                primaryStage.hide();
            }

        } catch (Exception e) {
            logger.error("Errore nell'avvio della schermata di login: " + e.getMessage());
            e.printStackTrace();
        }
    }

    /**
     * Mostra la schermata principale dopo login riuscito
     */
    public static void showMainApplication() {
        try {
            logger.info("======== AVVIO APPLICAZIONE PRINCIPALE ========");

            // Chiudi la schermata di login
            if (loginStage != null && loginStage.isShowing()) {
                loginStage.close();
            }

            JsonConfig config = ConfigManager.readConfig();
            ResourceBundle bundle = ResourceBundle.getBundle("languages.MessagesBundle", currentLocale);

            // Carica l'FXML principale
            FXMLLoader loader = new FXMLLoader(MainApp.class.getResource("/GraphicalMenu.fxml"), bundle);
            Pane root = loader.load();

            // Passo l'app al controller
            MainViewController controller = loader.getController();
            controller.setMainApp(new MainApp()); // Crea istanza per compatibilità

            // Creo la scena, senza dimensioni hard-coded (l'FXML le contiene)
            Pane overlayPane = new Pane(); // Per la guida visiva
            overlayPane.setPickOnBounds(false); // Lascia passare eventi se vuoi

            // Applico il tema di default
            root.getStyleClass().add(config.getTheme());
            overlayPane.getStyleClass().add(config.getTheme());
            controller.setRoot(root);
            DialogHelper.setRoot(root);

            StackPane layeredRoot = new StackPane(root, overlayPane);
            controller.setOverlayPane(overlayPane);
            System.out.println("Ho appena settato l'overlayPane");

            // Crea la scena principale con StackPane
            Scene scene = new Scene(layeredRoot);

            // Carica i CSS per il tema principale e per i dialog moderni
            scene.getStylesheets().addAll(
                    MainApp.class.getResource("/styles/modern-main-theme.css").toExternalForm(),
                    MainApp.class.getResource("/styles/tutorial-theme.css").toExternalForm()
            );

            // **AGGIUNGIAMO IL CSS PER I DIALOG MODERNI GLOBALMENTE**
            try {
                scene.getStylesheets().add(MainApp.class.getResource("/css/modern-dialogs-theme.css").toExternalForm());
                logger.info("CSS dialog moderni caricato globalmente da /css/");
            } catch (Exception cssError) {
                try {
                    scene.getStylesheets().add(MainApp.class.getResource("/styles/modern-dialogs-theme.css").toExternalForm());
                    logger.info("CSS dialog moderni caricato globalmente da /styles/");
                } catch (Exception cssError2) {
                    logger.warn("Impossibile caricare CSS dialog moderni: " + cssError2.getMessage());
                }
            }

            // Configura l'icona
            primaryStage.getIcons().add(new Image(Objects.requireNonNull(MainApp.class.getResourceAsStream("/icons/app_icon.png"))));

            // Icona per MacOS
            try {
                if (System.getProperty("os.name").toLowerCase().contains("mac")) {
                    Class<?> appClass = Class.forName("com.apple.eawt.Application");
                    Object appInstance = appClass.getMethod("getApplication").invoke(null);
                    Class<?> imageClass = Class.forName("java.awt.Image");
                    java.awt.Image icon = java.awt.Toolkit.getDefaultToolkit().getImage(
                            MainApp.class.getResource("/icons/app_icon.png")
                    );
                    appClass.getMethod("setDockIconImage", imageClass).invoke(appInstance, icon);
                }
            } catch (Exception e) {
                logger.debug("Dock icon non impostata: " + e.getMessage());
            }

            // Configura lo stage principale
            primaryStage.setTitle("ByteBridge");
            primaryStage.setResizable(false);
            primaryStage.initStyle(StageStyle.TRANSPARENT);
            scene.setFill(Color.TRANSPARENT);

            Rectangle clip = new Rectangle();
            clip.setArcWidth(20);
            clip.setArcHeight(20);
            clip.widthProperty().bind(root.widthProperty());
            clip.heightProperty().bind(root.heightProperty());
            root.setClip(clip);

            primaryStage.setWidth(900);
            primaryStage.setHeight(653);
            primaryStage.setScene(scene);
            primaryStage.centerOnScreen();
            primaryStage.show();

            if (ConfigManager.readConfig().getIsFirstStart()) {
                controller.avviaGuida();
            }

            // Handler per logout quando si chiude l'app
            primaryStage.setOnCloseRequest(evt -> {
                authManager.logout();
                logger.info("Applicazione chiusa con logout.");
            });

        } catch (Exception e) {
            logger.error("Errore nell'avvio dell'applicazione principale: " + e.getMessage());
            e.printStackTrace();
        }
    }

    /**
     * Mostra la schermata di registrazione
     */
    public static void showRegisterScreen() {
        try {
            logger.info("======== AVVIO SCHERMATA REGISTRAZIONE ========");

            ResourceBundle bundle = ResourceBundle.getBundle("languages.MessagesBundle", currentLocale);

            // Carica l'FXML della registrazione
            FXMLLoader loader = new FXMLLoader(MainApp.class.getResource("/RegisterScreen.fxml"), bundle);
            VBox registerRoot = loader.load();

            // Ottieni il controller della registrazione
            RegisterController registerController = loader.getController();
            registerController.setMainApp(MainApp.class);

            // Crea una nuova finestra per la registrazione
            Stage registerStage = new Stage();
            registerStage.setTitle("ByteBridge - Register");
            registerStage.setResizable(false);
            registerStage.initStyle(StageStyle.TRANSPARENT);
            registerStage.initOwner(loginStage); // Modale rispetto al login

            Scene registerScene = new Scene(registerRoot);
            registerScene.setFill(Color.TRANSPARENT);

            // Applica tema e CSS
            JsonConfig config = ConfigManager.readConfig();
            registerRoot.getStyleClass().add(config.getTheme());

            try {
                registerScene.getStylesheets().add(MainApp.class.getResource("/styles/login-theme.css").toExternalForm());
            } catch (Exception cssError) {
                logger.warn("Impossibile caricare CSS registrazione: " + cssError.getMessage());
            }

            // Angoli arrotondati
            Rectangle clip = new Rectangle();
            clip.setArcWidth(20);
            clip.setArcHeight(20);
            clip.widthProperty().bind(registerRoot.widthProperty());
            clip.heightProperty().bind(registerRoot.heightProperty());
            registerRoot.setClip(clip);

            registerStage.setScene(registerScene);
            registerStage.setWidth(500);
            registerStage.setHeight(650);
            registerStage.centerOnScreen();
            registerStage.show();

        } catch (Exception e) {
            logger.error("Errore nell'avvio della schermata di registrazione: " + e.getMessage());
            e.printStackTrace();
        }
    }

    // Getters statici
    public static Stage getPrimaryStage() { return primaryStage; }
    public static Stage getLoginStage() { return loginStage; }
    public static Locale getCurrentLocale() { return currentLocale; }
    public static void setCurrentLocale(Locale locale) { currentLocale = locale; }
    public static AuthManager getAuthManager() { return authManager; }

    public static void main(String[] args) {
        launch(args);
    }
}