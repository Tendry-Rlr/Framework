package tools;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.lang.reflect.Parameter;

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
                } else if (!fieldType.isPrimitive() && fieldType != String.class
                        && !Number.class.isAssignableFrom(fieldType)) {
                    // Objet complexe imbriqué
                    Object nestedInstance = bindComplexObject(fieldType, req, paramName);
                    try {
                        Method setter = type.getMethod(setterName, fieldType);
                        setter.invoke(instance, nestedInstance);
                    } catch (NoSuchMethodException e) {
                        // Setter non trouvé
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