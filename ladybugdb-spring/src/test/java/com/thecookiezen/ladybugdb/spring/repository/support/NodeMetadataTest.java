package com.thecookiezen.ladybugdb.spring.repository.support;

import com.thecookiezen.ladybugdb.spring.annotation.NodeEntity;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.data.annotation.Id;

import static org.junit.jupiter.api.Assertions.*;

class NodeMetadataTest {

    @Nested
    class IdFieldDetection {

        @Test
        void shouldFindSpringDataIdAnnotation() {
            NodeMetadata<EntityWithSpringId> metadata = new NodeMetadata<>(EntityWithSpringId.class);

            assertNotNull(metadata.getIdType());
            assertEquals(String.class, metadata.getIdType());
            assertEquals("id", metadata.getIdPropertyName());
        }

        @Test
        void shouldReturnNullWhenNoIdField() {
            NodeMetadata<EntityWithoutId> metadata = new NodeMetadata<>(EntityWithoutId.class);

            assertNull(metadata.getIdType());
        }
    }

    @Nested
    class NodeLabelDerivation {

        @Test
        void shouldDeriveSimpleClassName() {
            NodeMetadata<Person> metadata = new NodeMetadata<>(Person.class);

            assertEquals("Person", metadata.getNodeLabel());
        }

        @Test
        void shouldRemoveEntitySuffix() {
            NodeMetadata<UserEntity> metadata = new NodeMetadata<>(UserEntity.class);

            assertEquals("User", metadata.getNodeLabel());
        }

        @Test
        void shouldUseAnnotationLabel() {
            NodeMetadata<AnnotatedNode> metadata = new NodeMetadata<>(AnnotatedNode.class);

            assertEquals("CustomLabel", metadata.getNodeLabel());
        }
    }

    @Nested
    class IdExtraction {

        @Test
        void shouldExtractIdValue() {
            NodeMetadata<EntityWithSpringId> metadata = new NodeMetadata<>(EntityWithSpringId.class);
            EntityWithSpringId entity = new EntityWithSpringId();
            entity.id = "test-123";

            Object id = metadata.getId(entity);

            assertEquals("test-123", id);
        }

        @Test
        void shouldReturnNullForNullId() {
            NodeMetadata<EntityWithSpringId> metadata = new NodeMetadata<>(EntityWithSpringId.class);
            EntityWithSpringId entity = new EntityWithSpringId();
            entity.id = null;

            Object id = metadata.getId(entity);

            assertNull(id);
        }
    }

    static class EntityWithSpringId {
        @Id
        String id;
        String name;
    }

    static class EntityWithoutId {
        String name;
        int value;
    }

    static class Person {
        @Id
        String id;
    }

    static class UserEntity {
        @Id
        String id;
    }

    @NodeEntity(label = "CustomLabel")
    static class AnnotatedNode {
        @Id
        String id;
    }
}
