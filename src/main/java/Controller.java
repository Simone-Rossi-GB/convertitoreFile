

import javafx.application.Platform;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.Alert;
import javafx.scene.control.ChoiceDialog;
import javafx.scene.control.Label;

import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.util.*;

public class Controller implements Initializable {
    private DirectoryWatcher watcher = null;
    private Engine engine = null;
    private int fileRicevuti;
    private int fileConvertiti;
    private int fileScartati;
    public Label txtPathDirectory;
    @FXML
    private Label welcomeText;

    @FXML
    public void onExitButtonClick(ActionEvent actionEvent) {
        System.exit(1);
    }

    @Override
    public void initialize(URL url, ResourceBundle resourceBundle) {
        try {
            Thread watcherThread = new Thread(new DirectoryWatcher("src/input"));
            watcherThread.setDaemon(true);
            watcherThread.start();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
        fileRicevuti = 0;
        fileConvertiti = 0;
        fileScartati = 0;
    }

    public static String getExtension(File file) {
        String name = file.getName();
        int lastDot = name.lastIndexOf('.');
        if (lastDot == -1 || lastDot == name.length() - 1) {
            return ""; // niente estensione
        }
        return name.substring(lastDot + 1).toLowerCase();
    }

    public void launchDialogConversion(File srcFile) {
        Platform.runLater(() -> fileRicevuti++);
        engine = new Engine();
        String srcExtension = getExtension(srcFile);
        System.out.println("Estensione file sorgente: " + srcExtension);
        List<String> formats = null;
        try {
            formats = engine.getPossibleConversions(srcExtension);
        } catch (Exception e) {
            Platform.runLater(() -> fileScartati++);
            launchAlertError("Conversione di " + srcFile.getName() + " non supportata");
            stampaRisultati();
            return;
        }
        System.out.println("prima parte finita");
        List<String> finalFormats = formats;
        Platform.runLater(() -> {
            ChoiceDialog<String> dialog = new ChoiceDialog<>(finalFormats.get(0), finalFormats);
            dialog.setTitle("Seleziona Formato");
            dialog.setHeaderText("Converti " + srcFile.getName() + " in...");
            dialog.setContentText("Formato desiderato:");
            System.out.println("dialog mandato");

            Optional<String> result = dialog.showAndWait();
            result.ifPresent(format -> {
                try {
                    engine.conversione(srcExtension, format, srcFile);
                    fileConvertiti++;
                    launchAlertSuccess(srcFile);
                } catch (Exception e) {
                    fileScartati++;
                    launchAlertError("Conversione di " + srcFile.getName() + " interrotta a causa di un errore");
                }
                stampaRisultati();
            });
        });

    }

public void stampaRisultati(){
    System.out.println("Ricevuti: " + fileRicevuti);
    System.out.println("Scartati: " + fileScartati);
    System.out.println("Convertiti: " + fileConvertiti);
}

    public void launchAlertSuccess(File file){
        Platform.runLater(() -> {
            Alert alert = new Alert(Alert.AlertType.INFORMATION);
            alert.setTitle("Conversione eseguita");
            alert.setHeaderText(null);
            alert.setContentText("Conversione di " + file.getName() + " completata con successo!");
            alert.showAndWait();
        });
    }

    public void launchAlertError(String message){
        Platform.runLater(() -> {
            Alert alert = new Alert(Alert.AlertType.INFORMATION);
            alert.setTitle("Conversione interrotta");
            alert.setHeaderText(null); // Nessun header
            alert.setContentText(message);
            alert.showAndWait();
        });
    }

    public void handleButtonClick(ActionEvent actionEvent) {
    }
}