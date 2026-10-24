package tools;

import java.io.InputStream;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.net.http.HttpRequest;
import java.util.HashMap;
import java.util.Map;

import org.springframework.context.ApplicationContext;

import jakarta.servlet.ServletContext;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpSession;
import jakarta.servlet.http.Part;
import session.MySession;

public class Convertion {
    public static Object convertType(String value, Class<?> targetType) {
        if (value == null || value.trim().isEmpty()) {
            // (int, double, boolean...), on retourne leur valeur par défaut (0, 0.0, false)
            if (targetType.isPrimitive()) {
                return Convertion.getDefaultValue(targetType);
            }
            // (String, Integer, Double...), on retourne null
            return null;
        }
        if (targetType == String.class) {
            return value;
        }
        if (value == null || value.trim().isEmpty()) {
            return null;
        }
        if (targetType == int.class || targetType == Integer.class) {
            return Integer.parseInt(value);
        }
        if (targetType == long.class || targetType == Long.class) {
            return Long.parseLong(value);
        }
        if (targetType == double.class || targetType == Double.class) {
            return Double.parseDouble(value);
        }
        if (targetType == float.class || targetType == Float.class) {
            return Float.parseFloat(value);
        }
        if (targetType == boolean.class || targetType == Boolean.class) {
            return Boolean.parseBoolean(value);
        }
        return null;
    }

    public static Object getDefaultValue(Class<?> type) {
        if (type == int.class) {
            return 0;
        }
        if (type == long.class) {
            return 0L;
        }
        if (type == double.class) {
            return 0.0d;
        }
        if (type == float.class) {
            return 0.0f;
        }
        if (type == boolean.class) {
            return false;
        }
        if (type == char.class) {
            return '\0';
        }
        return null;
    }

    public static Object defineObject(String value, Class<?> typeObject) {
        if (value == null || value.isEmpty()) {
            return typeObject.isPrimitive() ? Convertion.getDefaultValue(typeObject) : null;
        }
        return Convertion.convertType(value, typeObject);
    }

    public static MySession gererSession(HttpServletRequest req, Map<String, MySession> sessions) {
        HttpSession sessionHttp = req.getSession();
        String sessionId = sessionHttp.getId();
        System.out.println("Session id : " + sessionId);

        MySession mySession = sessions.get(sessionId);
        if (mySession != null) {
            System.out.println("la session est deja presente");
        }
        if (mySession == null) {
            System.out.println("Initialisation de la session");
            mySession = new MySession();
            mySession.chargerSession(sessionHttp);
            sessions.put(sessionId, mySession);
        }

        return mySession;
    }

    private static void uploadFile(Class<?> type, MapFile mapFile, HttpServletRequest req, String paramName)
            throws Exception {
        Part filePart = req.getPart(paramName);

        if (filePart != null && filePart.getSize() > 0) {
            // 1. Récupérer le nom original du fichier
            String fileName = filePart.getSubmittedFileName();
            System.out.println(fileName);
            String[] parts = fileName.split("\\.");

            byte[] fileBytes;
            try (InputStream inputStream = filePart.getInputStream()) {
                fileBytes = inputStream.readAllBytes();
            }

            mapFile.setFileName(parts[0]);
            mapFile.setExtension(parts[1]);
            mapFile.setBytes(fileBytes);
        }
    }

    public static Object bindComplexObject(Class<?> type, HttpServletRequest req, String prefix) {
        try {
            Object instance = type.getDeclaredConstructor().newInstance();
            prefix = resolveObjectPrefix(type, req, prefix);
            Field[] fields = type.getDeclaredFields();

            for (Field field : fields) {
                String fieldName = field.getName();
                // Construction du nom complet du paramètre
                String paramName = (prefix != null && !prefix.isEmpty()) ? prefix + "." + fieldName : fieldName;
                Class<?> fieldType = field.getType();

                String setterName = "set" + fieldName.substring(0, 1).toUpperCase() + fieldName.substring(1);

                if (fieldType.isArray()) {
                    Object arrayValue = Convertion.bindList(fieldType, req, paramName);
                    try {
                        Method setter = type.getMethod(setterName, fieldType);
                        setter.invoke(instance, arrayValue);
                    } catch (NoSuchMethodException e) {
                        // Setter non trouvé
                    }
                }

                System.out.println("nom : " + fieldType.getSimpleName());
                if (fieldType.getSimpleName().equalsIgnoreCase("MapFile")) {

                    System.out.println("mapfile found");
                    MapFile mapFile = new MapFile();

                    try {
                        uploadFile(type, mapFile, req, paramName);

                        Method setter = type.getMethod(setterName, fieldType);
                        setter.invoke(instance, mapFile);
                    } catch (NoSuchMethodException e) {
                        e.printStackTrace();
                    }

                    continue;
                }

                if (!fieldType.isPrimitive() && fieldType != String.class
                        && !Number.class.isAssignableFrom(fieldType) && !fieldType.isArray()) {

                    // Objet complexe imbriqué
                    Object nestedInstance = bindComplexObject(fieldType, req, paramName);
                    try {
                        Method setter = type.getMethod(setterName, fieldType);
                        setter.invoke(instance, nestedInstance);
                    } catch (NoSuchMethodException e) {
                        // Setter non trouvé
                        e.printStackTrace();
                    }
                } else {
                    String value = req.getParameter(paramName);
                    if (value != null) {
                        try {
                            Method setter = type.getMethod(setterName, fieldType);
                            Object convertedValue = Convertion.convertType(value, fieldType);
                            setter.invoke(instance, convertedValue);
                        } catch (NoSuchMethodException e) {
                            // Setter non trouvé
                            e.printStackTrace();
                        }
                    }
                }
            }
            return instance;
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }

    public static Object bindList(Class<?> type, HttpServletRequest req, String paramNamePrefix) {
        Class<?> componentType = type.getComponentType();

        if (isSimpleType(componentType)) {
            String[] rawValues = req.getParameterValues(paramNamePrefix);
            if (rawValues == null) {
                return java.lang.reflect.Array.newInstance(componentType, 0);
            }

            Object arrayInstance = java.lang.reflect.Array.newInstance(componentType, rawValues.length);
            for (int j = 0; j < rawValues.length; j++) {
                java.lang.reflect.Array.set(arrayInstance,
                        j, Convertion.convertType(rawValues[j], componentType));
            }
            return arrayInstance;
        }

        int maxIndex = -1;
        java.util.Enumeration<String> parameterNames = req.getParameterNames();
        String prefix = paramNamePrefix + "[";

        while (parameterNames.hasMoreElements()) {
            String paramName = parameterNames.nextElement();
            if (paramName.startsWith(prefix)) {
                try {
                    int closingIndex = paramName.indexOf(']', prefix.length());
                    if (closingIndex != -1) {
                        String indexStr = paramName.substring(prefix.length(), closingIndex);
                        int index = Integer.parseInt(indexStr);
                        if (index > maxIndex) {
                            maxIndex = index;
                        }
                    }
                } catch (NumberFormatException e) {
                    // Ignore si l'indice n'est pas un nombre valide
                }
            }
        }

        if (maxIndex == -1) {
            return java.lang.reflect.Array.newInstance(componentType, 0);
        }

        // Création dynamique du tableau pour les objets indicés
        Object arrayInstance = java.lang.reflect.Array.newInstance(componentType, maxIndex + 1);

        for (int j = 0; j <= maxIndex; j++) {
            String fullPrefix = paramNamePrefix + "[" + j + "]";
            Object value;

            value = Convertion.bindComplexObject(componentType, req, fullPrefix);

            java.lang.reflect.Array.set(arrayInstance, j, value);
        }

        return arrayInstance;
    }

    private static boolean isSimpleType(Class<?> type) {
        return type == String.class
                || type == Character.class
                || type == Boolean.class
                || Number.class.isAssignableFrom(type)
                || type.isPrimitive();
    }

    private static String resolveObjectPrefix(Class<?> type, HttpServletRequest req, String prefix) {
        if (prefix == null || prefix.isEmpty() || hasParameterPrefix(req, prefix + ".")) {
            return prefix;
        }

        for (Field field : type.getDeclaredFields()) {
            if (hasParameterPrefix(req, field.getName()) || req.getParameter(field.getName()) != null) {
                return "";
            }
        }
        return prefix;
    }

    private static boolean hasParameterPrefix(HttpServletRequest req, String prefix) {
        java.util.Enumeration<String> parameterNames = req.getParameterNames();
        while (parameterNames.hasMoreElements()) {
            if (parameterNames.nextElement().startsWith(prefix)) {
                return true;
            }
        }
        return false;
    }
}