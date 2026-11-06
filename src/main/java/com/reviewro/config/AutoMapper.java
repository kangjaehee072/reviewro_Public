package com.reviewro.config;

import org.hibernate.Hibernate;
import org.springframework.beans.BeanWrapperImpl;

import java.beans.PropertyDescriptor;
import java.lang.reflect.Field;
import java.lang.reflect.ParameterizedType;
import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

public final class AutoMapper {

    private AutoMapper() {}

    public static <S, T> T map(S source, Class<T> targetClass) {
        return mapInternal(source, targetClass, new LinkedHashSet<>());
    }

    public static <S, T> List<T> mapList(List<S> sources, Class<T> targetClass) {
        if (sources == null) return List.of();
        List<T> result = new ArrayList<>(sources.size());
        for (S s : sources) result.add(map(s, targetClass));
        return result;
    }

    public static <S, T> T copy(S source, T target) {
        if (source == null || target == null) return target;
        copyDeep(source, target, false, new LinkedHashSet<>());
        return target;
    }

    public static <S, T> T copyNonNull(S source, T target) {
        if (source == null || target == null) return target;
        copyDeep(source, target, true, new LinkedHashSet<>());
        return target;
    }

    private static <S, T> T mapInternal(S source, Class<T> targetClass, Set<Object> visiting) {
        if (source == null) return null;
        if (visiting.contains(source)) return null;

        try {
            T target = targetClass.getDeclaredConstructor().newInstance();
            visiting.add(source);
            try {
                copyDeep(source, target, false, visiting);
            } finally {
                visiting.remove(source);
            }
            return target;
        } catch (ReflectiveOperationException e) {
            throw new IllegalStateException("기본 생성자 없음: " + targetClass.getName(), e);
        }
    }

    private static void copyDeep(Object source, Object target, boolean skipNull, Set<Object> visiting) {
        var src = new BeanWrapperImpl(source);
        var dst = new BeanWrapperImpl(target);

        for (PropertyDescriptor pd : dst.getPropertyDescriptors()) {
            String name = pd.getName();
            if ("class".equals(name)) continue;
            if (!src.isReadableProperty(name) || !dst.isWritableProperty(name)) continue;

            Object value = src.getPropertyValue(name);

            if (value == null) {
                if (!skipNull) dst.setPropertyValue(name, null);
                continue;
            }

            if (!Hibernate.isInitialized(value)) continue;

            Class<?> targetType = pd.getPropertyType();

            if (value instanceof Collection<?> col && Collection.class.isAssignableFrom(targetType)) {
                Class<?> elemType = resolveElementType(target.getClass(), name);
                if (elemType == null) continue;
                Collection<Object> converted = List.class.isAssignableFrom(targetType)
                        ? new ArrayList<>(col.size())
                        : new LinkedHashSet<>(col.size() * 2);
                for (Object e : col) {
                    converted.add(convertValue(e, elemType, visiting));
                }
                dst.setPropertyValue(name, converted);
            } else {
                Object converted = convertValue(value, targetType, visiting);
                if (converted != null || !skipNull) dst.setPropertyValue(name, converted);
            }
        }
    }

    private static Object convertValue(Object value, Class<?> targetType, Set<Object> visiting) {
        if (value == null) return null;
        Class<?> actual = Hibernate.getClass(value);

        if (targetType.isAssignableFrom(actual)) {
            return Hibernate.unproxy(value);
        }
        if (isSimpleType(targetType) || isSimpleType(actual)) {
            return null;
        }
        return mapInternal(Hibernate.unproxy(value), targetType, visiting);
    }

    private static boolean isSimpleType(Class<?> type) {
        return type.isPrimitive()
                || type.isEnum()
                || type.getName().startsWith("java.")
                || type.getName().startsWith("javax.")
                || type.getName().startsWith("sun.");
    }

    private static Class<?> resolveElementType(Class<?> owner, String fieldName) {
        Field field = findField(owner, fieldName);
        if (field != null
                && field.getGenericType() instanceof ParameterizedType pt
                && pt.getActualTypeArguments().length == 1
                && pt.getActualTypeArguments()[0] instanceof Class<?> c) {
            return c;
        }
        return null;
    }

    private static Field findField(Class<?> type, String name) {
        for (Class<?> c = type; c != null && c != Object.class; c = c.getSuperclass()) {
            try {
                return c.getDeclaredField(name);
            } catch (NoSuchFieldException ignored) {
            }
        }
        return null;
    }
}