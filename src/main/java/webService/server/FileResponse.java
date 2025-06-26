package webService.server;

class FileResponse {
    private String operation;
    private String filename;
    private String contentType;
    private long contentLength;
    private byte[] file;

    public FileResponse(String operation, String filename, String contentType, long contentLength, byte[] file) {
        this.operation = operation;
        this.filename = filename;
        this.contentType = contentType;
        this.contentLength = contentLength;
        this.file = file;
    }

    // Getters
    public String getOperation() { return operation; }
    public String getFilename() { return filename; }
    public String getContentType() { return contentType; }
    public long getContentLength() { return contentLength; }
    public byte[] getFile() { return file; }
}
