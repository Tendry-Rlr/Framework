package tools;

public class UrlMethod {
    String url;
    String typeMethode;

    public UrlMethod(String url, String typeMethode) {
        this.url = url;
        this.typeMethode = typeMethode;
    }

    public String getUrl() {
        return url;
    }

    public void setUrl(String url) {
        this.url = url;
    }

    public String getTypeMethode() {
        return typeMethode;
    }

    public void setTypeMethode(String typeMethode) {
        this.typeMethode = typeMethode;
    }
}
