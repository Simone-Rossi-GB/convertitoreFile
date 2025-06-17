package gui;

import Converters.EMLtoPDF;
import Converters.MSGtoPDF;
import javafx.concurrent.Task;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.stage.DirectoryChooser;
import javafx.stage.FileChooser;
import javafx.stage.Stage;

import java.io.File;
import java.util.List;

public class MainViewController {

    // Riferimento all'applicazione principale
    private MainApp mainApp;

    @FXML
    private void initialize() {

    }

    private void showAlert(String title, String message) {
        Alert alert = new Alert(Alert.AlertType.ERROR);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }

    private Stage getPrimaryStage() {
        return mainApp.getPrimaryStage();
    }
}