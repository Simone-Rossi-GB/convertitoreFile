package webService.client.gui.tutorial;

import javafx.geometry.Bounds;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.layout.Pane;
import javafx.scene.paint.Color;
import javafx.scene.shape.Rectangle;

import java.util.List;

class VisualGuide {
    private final Pane overlayPane;
    private final List<GuideStep> steps;
    private int currentStep = 0;
    private Rectangle highlight;
    private Label messageLabel;
    private Button nextButton;

    public VisualGuide(Pane overlayPane, List<GuideStep> steps) {
        this.overlayPane = overlayPane;
        this.steps = steps;
        initOverlay();
    }

    private void initOverlay() {
        highlight = new Rectangle();
        highlight.setStroke(Color.BLUE);
        highlight.setStrokeWidth(3);
        highlight.setFill(Color.rgb(0, 0, 255, 0.1));

        messageLabel = new Label();
        messageLabel.setStyle("-fx-background-color: white; -fx-padding: 10; -fx-border-color: black;");

        nextButton = new Button("Avanti");
        nextButton.setOnAction(e -> showNextStep());
    }

    public void start() {
        currentStep = 0;
        overlayPane.getChildren().clear();
        showStep(steps.get(currentStep));
    }

    private void showNextStep() {
        currentStep++;
        if (currentStep < steps.size()) {
            showStep(steps.get(currentStep));
        } else {
            overlayPane.getChildren().clear(); // Fine guida
        }
    }

    private void showStep(GuideStep step) {
        Bounds bounds = step.target.localToScene(step.target.getBoundsInLocal());
        Bounds rootBounds = overlayPane.sceneToLocal(bounds);

        highlight.setX(rootBounds.getMinX());
        highlight.setY(rootBounds.getMinY());
        highlight.setWidth(rootBounds.getWidth());
        highlight.setHeight(rootBounds.getHeight());

        messageLabel.setText(step.message);
        messageLabel.setLayoutX(rootBounds.getMaxX() + 10);
        messageLabel.setLayoutY(rootBounds.getMinY());

        nextButton.setLayoutX(messageLabel.getLayoutX());
        nextButton.setLayoutY(messageLabel.getLayoutY() + 40);

        overlayPane.getChildren().setAll(highlight, messageLabel, nextButton);
    }
}