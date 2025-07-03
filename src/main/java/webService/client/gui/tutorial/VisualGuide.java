package webService.client.gui.tutorial;

import javafx.animation.FadeTransition;
import javafx.animation.ScaleTransition;
import javafx.geometry.Bounds;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Pane;
import javafx.scene.layout.VBox;
import javafx.scene.shape.Rectangle;
import javafx.scene.shape.Shape;
import javafx.util.Duration;
import webService.client.gui.MainApp;

import java.util.List;
import java.util.ResourceBundle;

public class VisualGuide {
    private final Pane overlayPane;
    private final List<GuideStep> steps;
    private int currentStep = 0;
    private Shape spotlight;
    private VBox messageBox;
    private Label messageLabel;
    private Label stepIndicator;
    private Button nextButton;
    private Button skipButton;
    private Button prevButton;
    private ResourceBundle bundle;

    public VisualGuide(Pane overlayPane, List<GuideStep> steps) {
        this.overlayPane = overlayPane;
        this.steps = steps;
        this.bundle = ResourceBundle.getBundle("languages.MessagesBundle", MainApp.getCurrentLocale());
        initOverlay();
        System.out.println("polo");
    }

    private void initOverlay() {
        // Container principale per il messaggio
        messageBox = new VBox(12);
        messageBox.getStyleClass().add("visual-guide-container");
        messageBox.setAlignment(Pos.CENTER_LEFT);
        messageBox.setPadding(new Insets(20));
        messageBox.setMaxWidth(300);

        // CORREZIONE 3: Applica tema automaticamente al container
        applyCurrentTheme();

        // Indicatore del passo
        stepIndicator = new Label();
        stepIndicator.getStyleClass().add("visual-guide-step-indicator");

        // Messaggio principale
        messageLabel = new Label();
        messageLabel.getStyleClass().add("visual-guide-message");
        messageLabel.setWrapText(true);

        // Container per i pulsanti
        HBox buttonBox = new HBox(10);
        buttonBox.setAlignment(Pos.CENTER_RIGHT);

        // Pulsante indietro
        prevButton = new Button(bundle.getString("btn.previous"));
        prevButton.getStyleClass().add("visual-guide-button-secondary");
        prevButton.setOnAction(e -> showPreviousStep());

        // Pulsante avanti
        nextButton = new Button(bundle.getString("btn.next"));
        nextButton.getStyleClass().add("visual-guide-button-primary");
        nextButton.setOnAction(e -> showNextStep());

        // Pulsante salta tutorial
        skipButton = new Button(bundle.getString("btn.skip"));
        skipButton.getStyleClass().add("visual-guide-button-skip");
        skipButton.setOnAction(e -> endTutorial());

        buttonBox.getChildren().addAll(prevButton, nextButton, skipButton);

        messageBox.getChildren().addAll(stepIndicator, messageLabel, buttonBox);
    }

    // TEMA AUTOMATICO MIGLIORATO
    private void applyCurrentTheme() {
        // Rimuovi tutte le classi tema esistenti
        messageBox.getStyleClass().removeAll("light", "dark");

        // Trova la root scene
        System.out.println(overlayPane);
        javafx.scene.Scene scene = overlayPane.getScene();
        if (scene != null && scene.getRoot() != null) {
            Parent root = scene.getRoot();
            if (root.getStyleClass().contains("light")) {
                messageBox.getStyleClass().add("light");
            } else {
                messageBox.getStyleClass().add("dark");
            }
        } else {
            // Fallback: aggiungi dark come default
            messageBox.getStyleClass().add("dark");
        }
    }

    public void start() {
        currentStep = 0;
        overlayPane.getChildren().clear();
        showStep(steps.get(currentStep));

        // Animazione di entrata
        FadeTransition fade = new FadeTransition(Duration.millis(300), overlayPane);
        fade.setFromValue(0.0);
        fade.setToValue(1.0);
        fade.play();
    }

    private void showNextStep() {
        currentStep++;
        if (currentStep < steps.size()) {
            showStep(steps.get(currentStep));
        } else {
            endTutorial();
        }
    }

    private void showPreviousStep() {
        if (currentStep > 0) {
            currentStep--;
            showStep(steps.get(currentStep));
        }
    }

    private void endTutorial() {
        FadeTransition fade = new FadeTransition(Duration.millis(300), overlayPane);
        fade.setFromValue(1.0);
        fade.setToValue(0.0);
        fade.setOnFinished(e -> overlayPane.getChildren().clear());
        fade.play();
    }

    private void showStep(GuideStep step) {
        try {
            // Calcola posizione del target
            Bounds bounds = step.getTarget().localToScene(step.getTarget().getBoundsInLocal());
            Bounds rootBounds = overlayPane.sceneToLocal(bounds);

            // Crea nuovo spotlight
            spotlight = createSpotlight(rootBounds);

            // Aggiorna contenuto
            updateStepContent(step);

            // Riapplica il tema ogni volta (per sincronizzare con cambi tema)
            applyCurrentTheme();

            // Posiziona il messaggio intelligentemente
            positionMessageBox(rootBounds);

            // Aggiorna stato pulsanti
            updateButtonStates();

            // Aggiorna overlay
            overlayPane.getChildren().setAll(spotlight, messageBox);

            // Animazione rimossa come richiesto
            // animateTarget(step.getTarget());

        } catch (Exception e) {
            System.err.println("Errore durante la visualizzazione del passo: " + e.getMessage());
        }
    }

    private void updateStepContent(GuideStep step) {
        stepIndicator.setText(String.format("%s %d/%d",
                bundle.getString("label.step"), currentStep + 1, steps.size()));

        // CORREZIONE 1: Aggiungi controllo per titolo e messaggio
        if (step.hasTitle()) {
            messageLabel.setText(step.getTitle() + "\n\n" + step.getMessage());
        } else {
            messageLabel.setText(step.getMessage());
        }

        // Aggiorna testo del pulsante "Avanti"
        if (currentStep == steps.size() - 1) {
            nextButton.setText(bundle.getString("btn.finish"));
        } else {
            nextButton.setText(bundle.getString("btn.next"));
        }
    }

    private void positionMessageBox(Bounds targetBounds) {
        double overlayWidth = overlayPane.getWidth();
        double overlayHeight = overlayPane.getHeight();
        double messageWidth = 300; // messageBox.maxWidth
        double messageHeight = 150; // Stima altezza

        double x, y;

        Node targetNode = steps.get(currentStep).getTarget();
        boolean isTextField = targetNode.getClass().getSimpleName().equals("TextField");

        if (isTextField) {
            // Posiziona sotto il TextField
            x = targetBounds.getMinX();
            y = targetBounds.getMaxY() + 10;
        } else {
            // Default: prova a posizionare a destra
            if (targetBounds.getMaxX() + messageWidth + 20 <= overlayWidth) {
                x = targetBounds.getMaxX() + 20;
            }
            // Altrimenti a sinistra
            else if (targetBounds.getMinX() - messageWidth - 20 >= 0) {
                x = targetBounds.getMinX() - messageWidth - 50;
            }
            // Altrimenti centrato orizzontalmente
            else {
                x = (overlayWidth - messageWidth) / 2;
            }

            // Verticale: sopra o sotto a seconda dello spazio
            if (targetBounds.getMinY() + messageHeight <= overlayHeight) {
                y = targetBounds.getMinY();
            } else {
                y = targetBounds.getMaxY() - messageHeight;
            }
        }

        // Assicurati che rimanga nei bounds
        x = Math.max(10, Math.min(x, overlayWidth - messageWidth - 10));
        y = Math.max(10, Math.min(y, overlayHeight - messageHeight - 10));

        messageBox.setLayoutX(x);
        messageBox.setLayoutY(y);
    }

    private void updateButtonStates() {
        prevButton.setDisable(currentStep == 0);
        prevButton.setVisible(currentStep > 0);
    }


    private Shape createSpotlight(Bounds targetBounds) {
        double width = overlayPane.getWidth();
        double height = overlayPane.getHeight();

        Rectangle fullScreen = new Rectangle(width, height);
        fullScreen.setArcHeight(20);
        fullScreen.setArcWidth(20);

        // SPOTLIGHT PRECISO - Nessun padding extra
        Rectangle hole = new Rectangle(
                targetBounds.getMinX() + 4.5,
                targetBounds.getMinY() + 2,
                targetBounds.getWidth() - 8.5,
                targetBounds.getHeight() - 8
        );

        // Angoli che seguono esattamente il bottone
        hole.setArcWidth(12); // Stesso radius del bottone
        hole.setArcHeight(12);

        Shape spotlightShape = Shape.subtract(fullScreen, hole);
        spotlightShape.getStyleClass().add("visual-guide-spotlight");
        return spotlightShape;
    }
}