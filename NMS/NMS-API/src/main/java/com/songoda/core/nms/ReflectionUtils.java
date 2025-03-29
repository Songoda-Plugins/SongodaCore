package com.songoda.core.nms;

import java.lang.reflect.Field;

public class ReflectionUtils {
    private ReflectionUtils() {
        throw new IllegalStateException("Utility class");
    }

    @SuppressWarnings("deprecation")
    public static Object getFieldValue(Object instance, String fieldName) throws NoSuchFieldException, IllegalAccessException {
        Field f = getField(instance, fieldName);
        boolean accessible = f.isAccessible();

        f.setAccessible(true);

        Object result = f.get(instance);

        f.setAccessible(accessible);

        return result;
    }

    @SuppressWarnings("deprecation")
    public static void setFieldValue(Object instance, String fieldName, Object value) throws NoSuchFieldException, IllegalAccessException {
        Field f = getField(instance, fieldName);
        boolean accessible = f.isAccessible();

        f.setAccessible(true);

        f.set(instance, value);

        f.setAccessible(accessible);
    }

    private static Field getField(Object instance, String fieldName) throws NoSuchFieldException {
        Field f = null;

        Class<?> currClass = instance.getClass();
        do {
            try {
                f = currClass.getDeclaredField(fieldName);
            } catch (NoSuchFieldException ex) {
                currClass = currClass.getSuperclass();

                if (currClass == null) {
                    throw ex;
                }
            }
        } while (f == null);

        return f;
    }
}
