package configuration.configHandlers.conversionContext;

public class ConversionContextReader extends ConversionContextData {
    public static String getDestinationFormat() {
        return context.get().get("destinationFormat").toString();
    }
    public static String getPassword() {
        return context.get().get("password").toString();
    }
    public static boolean getIsUnion() {
        return (boolean) context.get().get("union");
    }
    public static boolean getIsZippedOutput() {
        return  (boolean) context.get().get("zippedOutput");
    }
}
