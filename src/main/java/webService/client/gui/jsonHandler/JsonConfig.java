package gui.jsonHandler;

public class JsonConfig {
    private String theme;
    private String lang;

    // Costruttore vuoto obbligatorio per Jackson
    public JsonConfig() {}

    public JsonConfig(String theme, String lang) {
        this.theme = theme;
        this.lang = lang;
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

    @Override
    public String toString() {
        return "Config{theme='" + theme + "', lang='" + lang + "'}";
    }
}

