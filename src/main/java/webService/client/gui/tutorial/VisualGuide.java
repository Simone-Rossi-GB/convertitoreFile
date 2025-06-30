package webService.client.gui.tutorial;

import javafx.geometry.Bounds;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.ToggleButton;
import javafx.scene.layout.Pane;
import javafx.scene.paint.Color;
import javafx.scene.shape.Rectangle;

import java.util.List;

public class VisualGuide {
    private final Pane overlayPane;
    private final List<GuideStep> steps;
    private int currentStep = 0;
    private Rectangle highlight;
    private Label messageLabel;
    private Button nextButton;

    public VisualGuide(Pane overlayPane, List<GuideStep> steps, Parent root) {
        this.overlayPane = overlayPane;
        this.steps = steps;
        initOverlay(root);
    }

    private void initOverlay(Parent root) {
        overlayPane.getStyleClass().add("visual-guide-overlay");

        highlight = new Rectangle();
        highlight.getStyleClass().add("visual-guide-highlight");

        messageLabel = new Label();
        messageLabel.getStyleClass().add("visual-guide-message");

        nextButton = new Button("Avanti");
        nextButton.getStyleClass().add("visual-guide-button");
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
            overlayPane.getStyleClass().clear();
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