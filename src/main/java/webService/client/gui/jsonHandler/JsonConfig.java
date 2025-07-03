package webService.client.gui.jsonHandler;

public class JsonConfig {
    private String theme;
    private String lang;
    private boolean isFirstStart;

    // Costruttore vuoto obbligatorio per Jackson
    public JsonConfig() {}

    public JsonConfig(String theme, String lang, boolean isFirstStart) {
        this.theme = theme;
        this.lang = lang;
        this.isFirstStart = isFirstStart;
    }

    public String getTheme() {
        return theme;
    }

    public void setTheme(String theme) {
        this.theme = theme;
    }

    public String getLang() {
        return lang;
    }

    public void setLang(String lang) {
        this.lang = lang;
    }

    public boolean getIsFirstStart() {
        return isFirstStart;
    }

    public void setIsFirstStart(boolean firstStart) {
        isFirstStart = firstStart;
    }

    @Override
    public String toString() {
        return "Config{theme='" + theme + "', lang='" + lang + "', isFirstStart='" + isFirstStart + "'}";
    }
}

