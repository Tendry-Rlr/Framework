package tools;

import java.lang.reflect.Method;

public class MappingUrl {
    Class<?> claz;
    Method method;

    public MappingUrl(Class<?> c, Method m) {
        this.claz = c;
        this.method = m;
    }

    public Class<?> getClaz() {
        return claz;
    }

    public void setClaz(Class<?> claz) {
        this.claz = claz;
    }

    public Method getMethod() {
        return method;
    }

    public void setMethod(Method method) {
        this.method = method;
    }
    
}
