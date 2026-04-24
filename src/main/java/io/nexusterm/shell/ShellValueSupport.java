package io.nexusterm.shell;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.time.Instant;
import java.util.Locale;

public final class ShellValueSupport {
    private ShellValueSupport() {}

    public static Object readProperty(Object target, String propertyName) {
        if (target == null) {
            return null;
        }

        Method method = findMethod(target.getClass(), propertyName);
        if (method != null) {
            try {
                method.setAccessible(true);
                return method.invoke(target);
            } catch (ReflectiveOperationException ignored) {
                // Fall back to field lookup.
            }
        }

        Field field = findField(target.getClass(), propertyName);
        if (field == null) {
            return null;
        }

        try {
            field.setAccessible(true);
            return field.get(target);
        } catch (ReflectiveOperationException e) {
            return null;
        }
    }

    public static Object parseLiteral(String raw) {
        String value = raw.trim();
        String lower = value.toLowerCase(Locale.ROOT);

        if ("true".equals(lower) || "false".equals(lower)) {
            return Boolean.parseBoolean(lower);
        }

        Long sized = parseSizeLiteral(lower);
        if (sized != null) {
            return sized;
        }

        try {
            if (value.contains(".")) {
                return Double.parseDouble(value);
            }
            return Long.parseLong(value);
        } catch (NumberFormatException ignored) {
            // Fall through.
        }

        try {
            return Instant.parse(value);
        } catch (Exception ignored) {
            return value;
        }
    }

    public static int compare(Object left, Object right) {
        if (left instanceof Number leftNumber && right instanceof Number rightNumber) {
            return Double.compare(leftNumber.doubleValue(), rightNumber.doubleValue());
        }
        if (left instanceof Instant leftInstant && right instanceof Instant rightInstant) {
            return leftInstant.compareTo(rightInstant);
        }
        if (left instanceof Boolean leftBoolean && right instanceof Boolean rightBoolean) {
            return Boolean.compare(leftBoolean, rightBoolean);
        }
        return String.valueOf(left).compareToIgnoreCase(String.valueOf(right));
    }

    private static Field findField(Class<?> type, String name) {
        Class<?> current = type;
        while (current != null) {
            try {
                return current.getDeclaredField(name);
            } catch (NoSuchFieldException ignored) {
                current = current.getSuperclass();
            }
        }
        return null;
    }

    private static Method findMethod(Class<?> type, String propertyName) {
        String capitalized = propertyName.substring(0, 1).toUpperCase(Locale.ROOT) + propertyName.substring(1);
        String[] candidates = new String[] {propertyName, "get" + capitalized, "is" + capitalized};
        for (String candidate : candidates) {
            try {
                return type.getMethod(candidate);
            } catch (NoSuchMethodException ignored) {
                // Try next candidate.
            }
        }
        return null;
    }

    private static Long parseSizeLiteral(String value) {
        long multiplier = 1;
        String numeric = value;

        if (value.endsWith("kb")) {
            multiplier = 1024L;
            numeric = value.substring(0, value.length() - 2);
        } else if (value.endsWith("mb")) {
            multiplier = 1024L * 1024L;
            numeric = value.substring(0, value.length() - 2);
        } else if (value.endsWith("gb")) {
            multiplier = 1024L * 1024L * 1024L;
            numeric = value.substring(0, value.length() - 2);
        } else {
            return null;
        }

        try {
            return Long.parseLong(numeric.trim()) * multiplier;
        } catch (NumberFormatException e) {
            return null;
        }
    }
}
