package route;

import java.util.Map;

import jakarta.servlet.http.HttpSession;

public class MySession {
    private Map<String, Object> attributes;

    public MySession() {
    }

    public void setAttributes(Map<String, Object> attributes) {
        this.attributes = attributes;
    }

    public Map<String, Object> getAttributes() {
        return attributes;
    }

    public void addAttribute(String key, Object value) {
        this.attributes.put(key, value);
    }

    public void removeAttribute(String key) {
        this.attributes.remove(key);
    }

    public Object getValue(String key) {
        return this.attributes.get(key);
    }

    public void chargerSession(HttpSession session) {
        if (session != null) {
            java.util.Enumeration<String> attributeNames = session.getAttributeNames();
            while (attributeNames.hasMoreElements()) {
                String key = attributeNames.nextElement();
                Object value = session.getAttribute(key);
                this.attributes.put(key, value);
            }
        }
    }
}
