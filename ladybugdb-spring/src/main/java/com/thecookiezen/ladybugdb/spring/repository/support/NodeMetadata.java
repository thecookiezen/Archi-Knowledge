package com.thecookiezen.ladybugdb.spring.repository.support;

import com.thecookiezen.ladybugdb.spring.annotation.NodeEntity;

import java.lang.annotation.Annotation;
import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodHandles;
import java.lang.reflect.Field;
import java.util.Arrays;
import java.util.List;

public class NodeMetadata<T> {

    private final Class<T> entityType;
    private final String nodeLabel;
    private final MethodHandle idGetter;
    private final MethodHandle idSetter;
    private final String idPropertyName;
    private final Class<?> idType;
    private final List<String> propertyNames;

    public NodeMetadata(Class<T> entityType) {
        this.entityType = entityType;
        this.nodeLabel = determineNodeLabel(entityType);
        Field idField = findIdField(entityType);
        this.idPropertyName = idField != null ? idField.getName() : "id";
        this.idType = idField != null ? idField.getType() : null;
        this.propertyNames = extractPropertyNames(entityType);

        try {
            if (idField != null) {
                MethodHandles.Lookup lookup = MethodHandles.lookup();
                this.idGetter = lookup.unreflectGetter(idField);
                this.idSetter = lookup.unreflectSetter(idField);
            } else {
                this.idGetter = null;
                this.idSetter = null;
            }
        } catch (IllegalAccessException e) {
            throw new RuntimeException("Failed to create method handles for ID field", e);
        }
    }

    private List<String> extractPropertyNames(Class<T> entityType) {
        return Arrays.stream(entityType.getDeclaredFields())
                .map(Field::getName)
                .toList();
    }

    public List<String> getPropertyNames() {
        return propertyNames;
    }

    private String determineNodeLabel(Class<T> entityType) {
        NodeEntity annotation = entityType.getAnnotation(NodeEntity.class);
        if (annotation != null && !annotation.label().isEmpty()) {
            return annotation.label();
        }
        String className = entityType.getSimpleName();
        if (className.endsWith("Entity") && className.length() > 6) {
            return className.substring(0, className.length() - 6);
        }
        return className;
    }

    private Field findIdField(Class<T> entityType) {
        for (Field field : entityType.getDeclaredFields()) {
            for (Annotation annotation : field.getAnnotations()) {
                String annotationName = annotation.annotationType().getSimpleName();
                if ("Id".equals(annotationName) || "PrimaryKey".equals(annotationName)) {
                    field.setAccessible(true);
                    return field;
                }
            }
        }
        try {
            Field idField = entityType.getDeclaredField("id");
            idField.setAccessible(true);
            return idField;
        } catch (NoSuchFieldException e) {
            return null;
        }
    }

    public Class<T> getEntityType() {
        return entityType;
    }

    public String getNodeLabel() {
        return nodeLabel;
    }

    public String getIdPropertyName() {
        return idPropertyName;
    }

    public Class<?> getIdType() {
        return idType;
    }

    @SuppressWarnings("unchecked")
    public <ID> ID getId(T entity) {
        if (idGetter == null) {
            return null;
        }
        try {
            return (ID) idGetter.invoke(entity);
        } catch (Throwable e) {
            throw new RuntimeException("Failed to access ID field", e);
        }
    }

    public void setId(T entity, Object id) {
        if (idSetter == null) {
            throw new IllegalStateException("No ID field found for entity type: " + entityType.getName());
        }
        try {
            idSetter.invoke(entity, id);
        } catch (Throwable e) {
            throw new RuntimeException("Failed to set ID field", e);
        }
    }
}
