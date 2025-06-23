package configuration.configHandlers.conversionContext;

public class ConversionContextWriter extends ConversionContextData {
    public static void setDestinationFormat(String newDestinationFormat) {
        context.get().put("destinationFormat", newDestinationFormat);
    }
    public static void setPassword(String newPassword) {
        context.get().put("password", newPassword);
    }
    public static void setIsUnion(boolean isUnion) {
        context.get().put("union", isUnion);
    }
    public static void setIsZippedOutput(boolean isZippedOutput) {
        context.get().put("zippedOutput", isZippedOutput);
    }
}
