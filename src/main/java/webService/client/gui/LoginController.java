package webService.client.gui;

import javafx.scene.Parent;
import webService.client.auth.AuthManager;
import webService.client.gui.jsonHandler.*;
import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.image.ImageView;
import javafx.scene.layout.HBox;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.geometry.Side;
import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.LogManager;

import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.ResourceBundle;
import java.util.concurrent.CompletableFuture;

public class LoginController {

    @FXML private Label titleLabel;
    @FXML private Label subtitleLabel;
    @FXML private Label usernameLabel;
    @FXML private Label passwordLabel;
    @FXML private Label errorLabel;
    @FXML private Label versionLabel;
    @FXML private TextField usernameField;
    @FXML private PasswordField passwordField;
    @FXML private Button loginButton;
    @FXML private ToggleButton themeToggle;
    @FXML private Button langButton;
    @FXML private Button registerButton;

    private static final Logger logger = LogManager.getLogger(LoginController.class);
    private Class<?> mainApp;
    private AuthManager authManager;
    private ResourceBundle bundle;
    private final ContextMenu languageMenu = new ContextMenu();

    private final Map<String, Locale> localeMap = new HashMap<>();
    {
        localeMap.put("it.png", new Locale("it", "IT"));
        localeMap.put("en.png", new Locale("en", "EN"));
        localeMap.put("de.png", new Locale("de", "DE"));
    }

    @FXML
    private void initialize() {
        try {
            authManager = MainApp.getAuthManager();
            bundle = ResourceBundle.getBundle("languages.MessagesBundle", MainApp.getCurrentLocale());

            setupUI();
            setupEventHandlers();
            refreshTexts();
            initializeLanguageMenu();
            updateLangButtonGraphic(MainApp.getCurrentLocale());

            logger.info("LoginController inizializzato");
        } catch (Exception e) {
            logger.error("Errore nell'inizializzazione LoginController: " + e.getMessage());
        }
    }

    private void setupUI() {
        // Setup tema iniziale
        JsonConfig config = ConfigManager.readConfig();
        boolean isLight = "light".equals(config.getTheme());
        themeToggle.setSelected(isLight);

        // Focus iniziale sul campo username
        Platform.runLater(() -> usernameField.requestFocus());

        // Enter key per login
        usernameField.setOnAction(e -> passwordField.requestFocus());
        passwordField.setOnAction(e -> handleLogin());
    }

    private void setupEventHandlers() {
        // Theme toggle
        themeToggle.selectedProperty().addListener((obs, oldV, newV) -> {
            Parent root = themeToggle.getScene().getRoot();
            root.getStyleClass().removeAll("dark", "light");

            if (newV) {
                root.getStyleClass().add("light");
            } else {
                root.getStyleClass().add("dark");
            }
        });
    }

    @FXML
    private void handleLogin() {
        String username = usernameField.getText().trim();
        String password = passwordField.getText();

        if (authManager.login(username, password)) {
            // LOGIN OK → apri app principale
            MainApp.showMainApplication();
        } else {
            // LOGIN FALLITO → mostra errore
            showError("Credenziali non valide");
        }
    }

    @FXML
    private void handleRegister() {
        logger.info("Apertura schermata registrazione");
        MainApp.showRegisterScreen();
    }

    @FXML
    private void toggleTheme() {
        boolean isLight = themeToggle.isSelected();
        String newTheme = isLight ? "light" : "dark";

        // Salva la preferenza
        JsonConfig config = ConfigManager.readConfig();
        ConfigManager.writeConfig(new JsonConfig(newTheme, config.getLang()));

        logger.info("Tema cambiato a: {}", newTheme);
    }

    @FXML
    private void showLanguageMenu() {
        if (languageMenu.isShowing()) {
            languageMenu.hide();
        } else {
            languageMenu.show(langButton, Side.RIGHT, 0, 0);
        }
    }

    private void initializeLanguageMenu() {
        languageMenu.getStyleClass().add("context-menu");

        for (Map.Entry<String, Locale> entry : localeMap.entrySet()) {
            String imageFile = entry.getKey();
            Locale locale = entry.getValue();

            ImageView icon = new ImageView(getClass().getResource("/flags/" + imageFile).toExternalForm());
            icon.setFitWidth(24);
            icon.setFitHeight(20);
            icon.getStyleClass().add("flag-icon");

            Label label = new Label(locale.getCountry());
            label.getStyleClass().add("flag-name");

            HBox content = new HBox(icon, label);
            content.getStyleClass().add("lang-menu-item");
            content.setAlignment(Pos.CENTER_LEFT);
            content.setSpacing(6);
            content.setPadding(new Insets(4, 4, 4, 4));

            CustomMenuItem item = new CustomMenuItem(content, false);
            item.setOnAction(ev -> {
                MainApp.setCurrentLocale(locale);
                updateLangButtonGraphic(locale);
                bundle = ResourceBundle.getBundle("languages.MessagesBundle", locale);
                refreshTexts();
                languageMenu.hide();

                // Salva la preferenza lingua
                JsonConfig config = ConfigManager.readConfig();
                ConfigManager.writeConfig(new JsonConfig(config.getTheme(), locale.getLanguage()));
            });

            languageMenu.getItems().add(item);
        }
    }

    private void updateLangButtonGraphic(Locale locale) {
        String imageFile = getFlagFileForLocale(locale);
        ImageView icon = new ImageView(getClass().getResource("/flags/" + imageFile).toExternalForm());
        icon.setFitWidth(24);
        icon.setFitHeight(20);
        icon.getStyleClass().add("flag-icon");
        langButton.setGraphic(icon);
    }

    private String getFlagFileForLocale(Locale locale) {
        return localeMap.entrySet()
                .stream()
                .filter(e -> e.getValue().equals(locale))
                .map(Map.Entry::getKey)
                .findFirst()
                .orElse("it.png");
    }

    private void refreshTexts() {
        titleLabel.setText(bundle.getString("login.title"));
        subtitleLabel.setText(bundle.getString("login.subtitle"));
        usernameLabel.setText(bundle.getString("login.username"));
        passwordLabel.setText(bundle.getString("login.password"));
        loginButton.setText(bundle.getString("login.button.text"));
        versionLabel.setText(bundle.getString("login.version"));

        // Placeholder texts
        usernameField.setPromptText(bundle.getString("login.username.placeholder"));
        passwordField.setPromptText(bundle.getString("login.password.placeholder"));
    }

    private void showError(String message) {
        errorLabel.setText(message);
        errorLabel.setVisible(true);
    }

    private void hideError() {
        errorLabel.setVisible(false);
    }

    public void setMainApp(Class<?> mainApp) {
        this.mainApp = mainApp;
    }
}