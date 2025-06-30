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
import javafx.scene.shape.Shape;
import javafx.scene.shape.Rectangle;
import javafx.scene.shape.Path;
import javafx.scene.shape.Circle;


import java.util.List;

public class VisualGuide {
    private final Pane overlayPane;
    private final List<GuideStep> steps;
    private int currentStep = 0;
    private Shape spotlight;
    private Label messageLabel;
    private Button nextButton;

    public VisualGuide(Pane overlayPane, List<GuideStep> steps) {
        this.overlayPane = overlayPane;
        this.steps = steps;
        initOverlay();
    }

    private void initOverlay() {
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

        // Crea spotlight dinamico
        spotlight = createSpotlight(rootBounds);

        messageLabel.setText(step.message);
        messageLabel.setLayoutX(rootBounds.getMaxX() + 10);
        messageLabel.setLayoutY(rootBounds.getMinY());

        nextButton.setLayoutX(messageLabel.getLayoutX());
        nextButton.setLayoutY(messageLabel.getLayoutY() + 47);

        overlayPane.getChildren().setAll(spotlight, messageLabel, nextButton);
    }
    private Shape createSpotlight(Bounds targetBounds) {
        double width = overlayPane.getWidth();
        double height = overlayPane.getHeight();

        Rectangle fullScreen = new Rectangle(width, height);
        Rectangle hole = new Rectangle(
                targetBounds.getMinX() + 4.5,
                targetBounds.getMinY() + 2,
                targetBounds.getWidth() - 8.5,
                targetBounds.getHeight() - 7
        );

        hole.setArcWidth(20);
        hole.setArcHeight(20);

        Shape spotlightShape = Shape.subtract(fullScreen, hole);
        spotlightShape.getStyleClass().add("visual-guide-spotlight");
        return spotlightShape;
    }
}