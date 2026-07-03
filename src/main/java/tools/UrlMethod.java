package tools;

import java.util.Objects;

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

    //comparer les attributs au lieu des references(adresses memoires)
    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        UrlMethod urlMethod = (UrlMethod) o;
        return Objects.equals(url, urlMethod.url) && 
               Objects.equals(typeMethode, urlMethod.typeMethode);
    }

    @Override
    public int hashCode() {
        return Objects.hash(url, typeMethode);
    }
}
