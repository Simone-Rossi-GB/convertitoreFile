package webService.client.gui.tutorial;

import javafx.scene.Node;

public class GuideStep {
    Node target;
    String message;

    public GuideStep(Node target, String message) {
        this.target = target;
        this.message = message;
    }
}