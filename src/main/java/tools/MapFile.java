package tools;

public class MapFile {
    private String fileName;
    private String extension;
    byte[] bytes;

    public MapFile() {

    }

    public String getFileName() {
        return fileName;
    }

    public void setFileName(String fileName) {
        this.fileName = fileName;
    }

    public String getExtension() {
        return extension;
    }

    public void setExtension(String extension) {
        this.extension = extension;
    }

    public byte[] getBytes() {
        return bytes;
    }

    public void setBytes(byte[] bytes) {
        this.bytes = bytes;
    }

    public String getFileNameExtension() {
        return this.fileName + "." + this.extension;
    }
}
