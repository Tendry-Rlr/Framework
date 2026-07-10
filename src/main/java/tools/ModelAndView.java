package tools;

import java.util.HashMap;

public class ModelAndView {
    private String view;
    private HashMap<String, Object> modele;

    public ModelAndView() {
        this.modele = new HashMap<>();
    }

    public void setView(String view) {
        this.view = view;
    }

    public HashMap<String, Object> getModele() {
        return modele;
    }

    public void setModele(HashMap<String, Object> modele) {
        this.modele = modele;
    }

    public void addAttribute(String nomVariable, Object object) {
        this.modele.put(nomVariable, object);
    }

    public String getView() {
        return view;
    }

    
}
