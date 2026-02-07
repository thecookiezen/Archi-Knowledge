package com.thecookiezen.ladybugdb.spring.annotation;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Marks a class as a LadybugDB relationship entity.
 * Relationship entities connect two node entities and have internally-generated
 * IDs.
 */
@Documented
@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
public @interface RelationshipEntity {

    /**
     * The relationship type name. Defaults to the simple class name.
     */
    String type() default "";

    /**
     * The type of the node entity that is the source and destination of this
     * relationship.
     * Source and destination must be of the same type.
     */
    Class<?> nodeType();

    /**
     * The name of the field in the relationship entity that holds the source node.
     */
    String sourceField();

    /**
     * The name of the field in the relationship entity that holds the target node.
     */
    String targetField();
}
