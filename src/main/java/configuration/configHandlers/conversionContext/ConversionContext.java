package configuration.configHandlers.conversionContext;

import java.util.Map;

import com.fasterxml.jackson.databind.ObjectMapper;

import java.io.InputStream;
import java.util.HashMap;

public class ConversionContext {
    private static final Map<String, Object> baseTemplate = loadTemplate();
    private static final ThreadLocal<Map<String, Object>> context =
            ThreadLocal.withInitial(() -> new HashMap<>(baseTemplate));

    public static void set(String key, Object value) {
        context.get().put(key, value);
    }

    public static Object get(String key) {
        return context.get().get(key);
    }

    public static boolean contains(String key) {
        return context.get().containsKey(key);
    }

    public static void clear() {
        context.remove();
    }

    private static Map<String, Object> loadTemplate() {
        try (InputStream is = ConversionContext.class.getResourceAsStream("src/main/java/converter/config/conversionContext.json")) {
            if (is == null) throw new IllegalStateException("default-context.json non trovato nel classpath");
            return new ObjectMapper().readValue(is, Map.class);
        } catch (Exception e) {
            System.err.println("Errore nel caricamento del contesto base: " + e.getMessage());
            return new HashMap<>(); // fallback
        }
    }
}
