package configuration.jsonUtilities.RecognisedWrappers;

public class RecognisedString implements RecognisedInput {
    private final String value;

    public RecognisedString(String value) {
        this.value = value;
    }

    public String getValue() {
        return value;
    }

}
