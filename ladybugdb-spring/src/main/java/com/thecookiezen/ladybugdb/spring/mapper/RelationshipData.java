package com.thecookiezen.ladybugdb.spring.mapper;

import com.ladybugdb.InternalID;
import com.ladybugdb.Value;

import java.util.Map;

/**
 * Immutable record holding extracted relationship data from a query result.
 * <p>
 * Contains the relationship's internal ID, label name, source/target node IDs,
 * and all custom properties defined on the relationship.
 *
 * @param id         the internal ID of the relationship
 * @param labelName  the relationship type/label (e.g., "KNOWS", "FOLLOWS")
 * @param sourceId   the internal ID of the source node
 * @param targetId   the internal ID of the target node
 * @param properties the custom properties on the relationship
 */
public record RelationshipData(
        InternalID id,
        String labelName,
        InternalID sourceId,
        InternalID targetId,
        Map<String, Value> properties) {
}
