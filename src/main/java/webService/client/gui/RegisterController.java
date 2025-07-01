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
import javafx.stage.Stage;
import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.LogManager;

import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.regex.Pattern;

public class RegisterController {

    @FXML private Label titleLabel;
    @FXML private Label subtitleLabel;
    @FXML private Label fullNameLabel;
    @FXML private Label usernameLabel;
    @FXML private Label emailLabel;
    @FXML private Label passwordLabel;
    @FXML private Label confirmPasswordLabel;
    @FXML private Label noteLabel;
    @FXML private Label errorLabel;
    @FXML private Label successLabel;

    @FXML private TextField fullNameField;
    @FXML private TextField usernameField;
    @FXML private TextField emailField;
    @FXML private PasswordField passwordField;
    @FXML private PasswordField confirmPasswordField;

    @FXML private Button registerButton;
    @FXML private Button backToLoginButton;
    @FXML private ToggleButton themeToggle;
    @FXML private Button langButton;

    private static final Logger logger = LogManager.getLogger(RegisterController.class);
    private Class<?> mainApp;
    private AuthManager authManager;
    private ResourceBundle bundle;
    private final ContextMenu languageMenu = new ContextMenu();

    // Pattern per validazione email
    private static final Pattern EMAIL_PATTERN = Pattern.compile(
            "^[a-zA-Z0-9._%+-]+@[a-zA-Z0-9.-]+\\.[a-zA-Z]{2,}$"
    );

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

            logger.info("RegisterController inizializzato");
        } catch (Exception e) {
            logger.error("Errore nell'inizializzazione RegisterController: " + e.getMessage());
        }
    }

    private void setupUI() {
        // Setup tema iniziale
        JsonConfig config = ConfigManager.readConfig();
        boolean isLight = "light".equals(config.getTheme());
        themeToggle.setSelected(isLight);

        // Focus iniziale
        Platform.runLater(() -> fullNameField.requestFocus());

        // Tab navigation
        fullNameField.setOnAction(e -> usernameField.requestFocus());
        usernameField.setOnAction(e -> emailField.requestFocus());
        emailField.setOnAction(e -> passwordField.requestFocus());
        passwordField.setOnAction(e -> confirmPasswordField.requestFocus());
        confirmPasswordField.setOnAction(e -> handleRegister());
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

        // Real-time validation feedback
        usernameField.textProperty().addListener((obs, oldText, newText) -> {
            if (newText.length() > 0 && newText.length() < 3) {
                usernameField.setStyle("-fx-border-color: orange;");
            } else if (newText.length() >= 3) {
                usernameField.setStyle("-fx-border-color: green;");
            } else {
                usernameField.setStyle("");
            }
        });

        emailField.textProperty().addListener((obs, oldText, newText) -> {
            if (newText.length() > 0) {
                if (EMAIL_PATTERN.matcher(newText).matches()) {
                    emailField.setStyle("-fx-border-color: green;");
                } else {
                    emailField.setStyle("-fx-border-color: orange;");
                }
            } else {
                emailField.setStyle("");
            }
        });

        passwordField.textProperty().addListener((obs, oldText, newText) -> {
            validatePasswordMatch();
        });

        confirmPasswordField.textProperty().addListener((obs, oldText, newText) -> {
            validatePasswordMatch();
        });
    }

    private void validatePasswordMatch() {
        String password = passwordField.getText();
        String confirmPassword = confirmPasswordField.getText();

        if (confirmPassword.length() > 0) {
            if (password.equals(confirmPassword)) {
                confirmPasswordField.setStyle("-fx-border-color: green;");
            } else {
                confirmPasswordField.setStyle("-fx-border-color: red;");
            }
        } else {
            confirmPasswordField.setStyle("");
        }
    }

    @FXML
    private void handleRegister() {
        // Validazione campi
        if (!validateFields()) {
            return;
        }

        String fullName = fullNameField.getText().trim();
        String username = usernameField.getText().trim();
        String email = emailField.getText().trim();
        String password = passwordField.getText();
        String role = "USER"; // Sempre USER per default

        // Disabilita il pulsante durante la registrazione
        registerButton.setDisable(true);
        registerButton.setText(bundle.getString("register.button.loading"));
        hideMessages();

        // Registrazione asincrona con SimpleAuthManager
        CompletableFuture.supplyAsync(() -> {
            // Chiamata diretta al SimpleAuthManager - niente classi intermedie
            return authManager.register(fullName, username, email, password, role);
        }).thenAccept(success -> {
            Platform.runLater(() -> {
                if (success) {
                    logger.info("Registrazione riuscita per utente: {}", username);
                    showSuccess(bundle.getString("register.success.message"));

                    // Pulisce i campi
                    clearFields();

                    // Re-abilita il pulsante
                    registerButton.setDisable(false);
                    registerButton.setText(bundle.getString("register.button.text"));

                    // Torna al login dopo 2 secondi
                    Timer timer = new Timer();
                    timer.schedule(new TimerTask() {
                        @Override
                        public void run() {
                            Platform.runLater(() -> {
                                handleBackToLogin();
                                timer.cancel();
                            });
                        }
                    }, 2000);

                } else {
                    logger.warn("Registrazione fallita per utente: {}", username);
                    showError(bundle.getString("register.error.failed"));

                    // Re-abilita il pulsante
                    registerButton.setDisable(false);
                    registerButton.setText(bundle.getString("register.button.text"));
                }
            });
        }).exceptionally(throwable -> {
            Platform.runLater(() -> {
                logger.error("Errore durante registrazione per utente {}: {}", username, throwable.getMessage());
                showError(bundle.getString("register.error.connection"));

                // Re-abilita il pulsante
                registerButton.setDisable(false);
                registerButton.setText(bundle.getString("register.button.text"));
            });
            return null;
        });
    }

    private boolean validateFields() {
        String fullName = fullNameField.getText().trim();
        String username = usernameField.getText().trim();
        String email = emailField.getText().trim();
        String password = passwordField.getText();
        String confirmPassword = confirmPasswordField.getText();

        // Verifica campi vuoti
        if (fullName.isEmpty() || username.isEmpty() || email.isEmpty() ||
                password.isEmpty() || confirmPassword.isEmpty()) {
            showError(bundle.getString("register.error.emptyFields"));
            return false;
        }

        // Verifica lunghezza username
        if (username.length() < 3) {
            showError(bundle.getString("register.error.usernameShort"));
            return false;
        }

        // Verifica formato email
        if (!EMAIL_PATTERN.matcher(email).matches()) {
            showError(bundle.getString("register.error.invalidEmail"));
            return false;
        }

        // Verifica lunghezza password
        if (password.length() < 6) {
            showError(bundle.getString("register.error.passwordShort"));
            return false;
        }

        // Verifica corrispondenza password
        if (!password.equals(confirmPassword)) {
            showError(bundle.getString("register.error.passwordMismatch"));
            return false;
        }

        return true;
    }

    private void clearFields() {
        fullNameField.clear();
        usernameField.clear();
        emailField.clear();
        passwordField.clear();
        confirmPasswordField.clear();

        // Reset stili
        fullNameField.setStyle("");
        usernameField.setStyle("");
        emailField.setStyle("");
        passwordField.setStyle("");
        confirmPasswordField.setStyle("");
    }

    @FXML
    private void handleBackToLogin() {
        logger.info("Ritorno alla schermata di login");

        // Chiudi la finestra di registrazione
        Stage currentStage = (Stage) backToLoginButton.getScene().getWindow();
        currentStage.close();
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
        titleLabel.setText(bundle.getString("register.title"));
        subtitleLabel.setText(bundle.getString("register.subtitle"));
        fullNameLabel.setText(bundle.getString("register.fullName"));
        usernameLabel.setText(bundle.getString("register.username"));
        emailLabel.setText(bundle.getString("register.email"));
        passwordLabel.setText(bundle.getString("register.password"));
        confirmPasswordLabel.setText(bundle.getString("register.confirmPassword"));
        registerButton.setText(bundle.getString("register.button.text"));
        backToLoginButton.setText(bundle.getString("register.backToLogin"));
        noteLabel.setText(bundle.getString("register.note"));

        // Placeholder texts
        fullNameField.setPromptText(bundle.getString("register.fullName.placeholder"));
        usernameField.setPromptText(bundle.getString("register.username.placeholder"));
        emailField.setPromptText(bundle.getString("register.email.placeholder"));
        passwordField.setPromptText(bundle.getString("register.password.placeholder"));
        confirmPasswordField.setPromptText(bundle.getString("register.confirmPassword.placeholder"));
    }

    private void showError(String message) {
        errorLabel.setText(message);
        errorLabel.setVisible(true);
        successLabel.setVisible(false);
    }

    private void showSuccess(String message) {
        successLabel.setText(message);
        successLabel.setVisible(true);
        errorLabel.setVisible(false);
    }

    private void hideMessages() {
        errorLabel.setVisible(false);
        successLabel.setVisible(false);
    }

    public void setMainApp(Class<?> mainApp) {
        this.mainApp = mainApp;
    }
}