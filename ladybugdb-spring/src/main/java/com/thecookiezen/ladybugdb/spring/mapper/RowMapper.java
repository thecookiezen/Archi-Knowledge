package com.thecookiezen.ladybugdb.spring.mapper;

/**
 * Functional interface for mapping a row from a LadybugDB query result to a
 * domain object.
 * <p>
 * The {@link QueryRow} provides typed access to nodes, relationships, and
 * scalar values within the row.
 *
 * @param <T> the type of object to map to
 */
@FunctionalInterface
public interface RowMapper<T> {

    /**
     * Maps a single row of the query result to a domain object.
     *
     * @param row the QueryRow representing the current row
     * @return the mapped object
     * @throws Exception if an error occurs during mapping
     */
    T mapRow(QueryRow row) throws Exception;
}
