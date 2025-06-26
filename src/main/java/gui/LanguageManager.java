package gui;

import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.layout.Region;
import javafx.scene.paint.Color;
import javafx.scene.shape.Rectangle;
import javafx.stage.Stage;
import javafx.stage.StageStyle;

import java.io.IOException;
import java.util.Locale;
import java.util.ResourceBundle;

public class LanguageManager {
    public static void switchLanguage(Stage stage, Locale locale) throws IOException {
        ResourceBundle bundle = ResourceBundle.getBundle("languages.MessagesBundle", locale);
        FXMLLoader loader = new FXMLLoader(LanguageManager.class.getResource("/GraphicalMenu.fxml"), bundle);

        Region root = loader.load();

        Rectangle clip = new Rectangle();
        clip.setArcWidth(20);
        clip.setArcHeight(20);
        clip.widthProperty().bind(root.widthProperty());
        clip.heightProperty().bind(root.heightProperty());
        root.setClip(clip);

        Scene newScene = new Scene(root);
        newScene.getStylesheets().addAll(
                LanguageManager.class.getResource("/styles/modern-main-theme.css").toExternalForm()
        );

        newScene.setFill(Color.TRANSPARENT);
        stage.setScene(newScene);
    }
}