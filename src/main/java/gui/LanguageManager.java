package gui;

import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Stage;
import javafx.stage.Window;

import java.io.IOException;
import java.util.Locale;
import java.util.ResourceBundle;

public class LanguageManager {
    public static void switchLanguage(Stage stage, Locale locale) throws IOException {
        ResourceBundle bundle = ResourceBundle.getBundle("languages.MessagesBundle", locale);
        FXMLLoader loader = new FXMLLoader(LanguageManager.class.getResource("/GraphicalMenu.fxml"), bundle);

        Parent root = loader.load();

        Scene newScene = new Scene(root);
        newScene.getStylesheets().addAll(
                LanguageManager.class.getResource("/styles/modern-dark-theme.css").toExternalForm()
        );
        stage.setScene(newScene);
    }
}