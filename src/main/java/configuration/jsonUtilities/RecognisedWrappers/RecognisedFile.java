package configuration.jsonUtilities.RecognisedWrappers;

import java.io.File;

public class RecognisedFile implements RecognisedInput {
    private final File file;

    public RecognisedFile(File file) {
        this.file = file;
    }

    public File getValue() {
        return file;
    }
}
