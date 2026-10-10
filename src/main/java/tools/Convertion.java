package tools;

import java.lang.reflect.Field;
import java.lang.reflect.Method;

import jakarta.servlet.http.HttpServletRequest;

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

    public static Object bindComplexObject(Class<?> type, HttpServletRequest req, String prefix) {
        try {
            Object instance = type.getDeclaredConstructor().newInstance();
            Field[] fields = type.getDeclaredFields();

            for (Field field : fields) {
                String fieldName = field.getName();
                // Construction du nom complet du paramètre (ex: "etablissement.nom")
                String paramName = (prefix != null && !prefix.isEmpty()) ? prefix + "." + fieldName : fieldName;
                Class<?> fieldType = field.getType();

                String setterName = "set" + fieldName.substring(0, 1).toUpperCase() + fieldName.substring(1);

                // Si l'attribut est lui-même un objet complexe (ex: Etablissement)
                if (!fieldType.isPrimitive() && fieldType != String.class
                        && !Number.class.isAssignableFrom(fieldType)) {
                    Object nestedInstance = bindComplexObject(fieldType, req, paramName);
                    try {
                        Method setter = type.getMethod(setterName, fieldType);
                        setter.invoke(instance, nestedInstance);
                    } catch (NoSuchMethodException e) {
                        // Setter non trouvé
                    }
                } else {
                    // Champ simple (String, int, etc.)
                    String value = req.getParameter(paramName);
                    if (value != null) {
                        try {
                            Method setter = type.getMethod(setterName, fieldType);
                            Object convertedValue = Convertion.convertType(value, fieldType);
                            setter.invoke(instance, convertedValue);
                        } catch (NoSuchMethodException e) {
                            // Setter non trouvé
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

}
