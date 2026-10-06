package tools;

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

}
