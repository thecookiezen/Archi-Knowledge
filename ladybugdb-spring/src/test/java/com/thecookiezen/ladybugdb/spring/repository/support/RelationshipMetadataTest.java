package com.thecookiezen.ladybugdb.spring.repository.support;

import com.thecookiezen.ladybugdb.spring.annotation.RelationshipEntity;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class RelationshipMetadataTest {

    @Nested
    class TypeNameDerivation {

        @Test
        void shouldConvertCamelCaseToUpperSnakeCase() {
            RelationshipMetadata<FollowedBy> metadata = new RelationshipMetadata<>(FollowedBy.class);

            assertEquals("FOLLOWED_BY", metadata.getRelationshipTypeName());
        }

        @Test
        void shouldRemoveRelationshipSuffix() {
            RelationshipMetadata<LikesRelationship> metadata = new RelationshipMetadata<>(LikesRelationship.class);

            assertEquals("LIKES", metadata.getRelationshipTypeName());
        }

        @Test
        void shouldUseAnnotationType() {
            RelationshipMetadata<AnnotatedRelation> metadata = new RelationshipMetadata<>(AnnotatedRelation.class);

            assertEquals("CUSTOM_TYPE", metadata.getRelationshipTypeName());
        }
    }

    @RelationshipEntity(nodeType = Object.class, sourceField = "source", targetField = "target")
    static class FollowedBy {
        Object source;
        Object target;
    }

    @RelationshipEntity(nodeType = Object.class, sourceField = "source", targetField = "target")
    static class LikesRelationship {
        Object source;
        Object target;
    }

    @RelationshipEntity(type = "CUSTOM_TYPE", nodeType = Object.class, sourceField = "source", targetField = "target")
    static class AnnotatedRelation {
        Object source;
        Object target;
    }

    @RelationshipEntity(nodeType = String.class, sourceField = "from", targetField = "to")
    static class RelWithAnnotations {
        String from;
        String to;
    }
}
