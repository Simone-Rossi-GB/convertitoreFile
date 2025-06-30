// Versione migliorata di GuideStep.java
package webService.client.gui.tutorial;

import javafx.scene.Node;

public class GuideStep {
    private final Node target;
    private final String message;
    private final String title;
    private final StepType type;
    private final String iconClass;

    public enum StepType {
        INFO,
    }

    // Costruttore semplice (retrocompatibilità)
    public GuideStep(Node target, String message) {
        this(target, null, message, StepType.INFO, null);
    }

    // Costruttore con titolo
    public GuideStep(Node target, String title, String message) {
        this(target, title, message, StepType.INFO, null);
    }

    // Costruttore completo
    public GuideStep(Node target, String title, String message, StepType type, String iconClass) {
        this.target = target;
        this.title = title;
        this.message = message;
        this.type = type;
        this.iconClass = iconClass;
    }

    // Getters
    public Node getTarget() { return target; }
    public String getMessage() { return message; }
    public String getTitle() { return title; }
    public StepType getType() { return type; }
    public String getIconClass() { return iconClass; }

    // Metodi di utilità
    public boolean hasTitle() { return title != null && !title.trim().isEmpty(); }
    public boolean hasIcon() { return iconClass != null && !iconClass.trim().isEmpty(); }
}