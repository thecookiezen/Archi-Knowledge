package com.thecookiezen.ladybugdb.spring.mapper;

import com.ladybugdb.DataTypeID;
import com.ladybugdb.LbugStruct;
import com.ladybugdb.Value;
import com.ladybugdb.ValueRelUtil;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;

/**
 * Default implementation of {@link QueryRow} that wraps the raw column-to-value
 * map from a query result.
 * <p>
 * Uses {@link LbugStruct} to extract node properties and {@link ValueRelUtil}
 * to extract relationship data.
 */
public final class DefaultQueryRow implements QueryRow {

    private final Map<String, Value> columns;

    /**
     * Creates a QueryRow from the raw column map.
     *
     * @param columns the map of column names to raw Values
     */
    public DefaultQueryRow(Map<String, Value> columns) {
        this.columns = columns;
    }

    @Override
    public Value getValue(String column) {
        return columns.get(column);
    }

    @Override
    public boolean containsKey(String column) {
        return columns.containsKey(column);
    }

    @Override
    public boolean isNode(String column) {
        Value value = columns.get(column);
        return value != null && value.getDataType().getID() == DataTypeID.NODE;
    }

    @Override
    public boolean isRelationship(String column) {
        Value value = columns.get(column);
        return value != null && value.getDataType().getID() == DataTypeID.REL;
    }

    @Override
    public Map<String, Value> getNode(String column) {
        Value value = columns.get(column);
        if (value == null) {
            throw new IllegalArgumentException("Column '" + column + "' does not exist");
        }
        if (value.getDataType().getID() != DataTypeID.NODE) {
            throw new IllegalArgumentException(
                    "Column '" + column + "' is not a NODE (type: " + value.getDataType().getID() + ")");
        }
        try (LbugStruct struct = new LbugStruct(value)) {
            return struct.toMap();
        }
    }

    @Override
    public RelationshipData getRelationship(String column) {
        Value value = columns.get(column);
        if (value == null) {
            throw new IllegalArgumentException("Column '" + column + "' does not exist");
        }
        if (value.getDataType().getID() != DataTypeID.REL) {
            throw new IllegalArgumentException(
                    "Column '" + column + "' is not a REL (type: " + value.getDataType().getID() + ")");
        }

        var id = ValueRelUtil.getID(value);
        var labelName = ValueRelUtil.getLabelName(value);
        var sourceId = ValueRelUtil.getSrcID(value);
        var targetId = ValueRelUtil.getDstID(value);

        long propertySize = ValueRelUtil.getPropertySize(value);
        Map<String, Value> properties = new HashMap<>();
        for (long i = 0; i < propertySize; i++) {
            String propName = ValueRelUtil.getPropertyNameAt(value, i);
            Value propValue = ValueRelUtil.getPropertyValueAt(value, i);
            properties.put(propName, propValue);
        }

        return new RelationshipData(id, labelName, sourceId, targetId, properties);
    }

    @Override
    public Set<String> keySet() {
        return columns.keySet();
    }
}
