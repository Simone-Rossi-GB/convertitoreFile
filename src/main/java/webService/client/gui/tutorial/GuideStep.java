package webService.client.gui.tutorial;

import javafx.scene.Node;

class GuideStep {
    Node target;
    String message;

    GuideStep(Node target, String message) {
        this.target = target;
        this.message = message;
    }
}