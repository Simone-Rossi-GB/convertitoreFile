package webService.server.converters;

import org.apache.poi.ss.usermodel.Workbook;
import org.odftoolkit.simple.SpreadsheetDocument;

public class ODSStructureBuilder {
    private Workbook poiWorkbook;
    private SpreadsheetDocument odsDocument;
    private String format;

    public ODSStructureBuilder(Workbook workbook, String format) {
        this.poiWorkbook = workbook;
        this.format = format;
    }

    public ODSStructureBuilder(SpreadsheetDocument document, String format) {
        this.odsDocument = document;
        this.format = format;
    }

    public boolean isOds() {
        return "ods".equalsIgnoreCase(format);
    }

    public Workbook getPoiWorkbook() {
        return poiWorkbook;
    }

    public SpreadsheetDocument getOdsDocument() {
        return odsDocument;
    }

    public String getFormat() {
        return format;
    }
}
