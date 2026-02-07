package com.thecookiezen.ladybugdb.spring.core;

import com.ladybugdb.Connection;
import com.ladybugdb.Database;
import com.thecookiezen.ladybugdb.spring.connection.SimpleConnectionFactory;
import com.thecookiezen.ladybugdb.spring.mapper.ValueMappers;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;

class LadybugDBTemplateTest {

    private LadybugDBTemplate template;
    private static SimpleConnectionFactory connectionFactory;
    private static Database db;

    @BeforeAll
    static void setupAll() {
        db = new Database(":memory:");
        connectionFactory = new SimpleConnectionFactory(db);
    }

    @BeforeEach
    void setup() {
        template = new LadybugDBTemplate(connectionFactory);
        try (Connection conn = new Connection(db)) {
            conn.query("CREATE NODE TABLE Person(name STRING PRIMARY KEY, age INT64)");
        }
    }

    @AfterEach
    void tearDown() {
        template.execute("MATCH (n) DETACH DELETE n");
    }

    @AfterAll
    static void tearDownAll() {
        if (connectionFactory != null) {
            connectionFactory.close();
        }
        if (db != null) {
            db.close();
        }
    }

    @Test
    void execute_shouldRunWriteQuery() {
        template.execute("CREATE (p:Person {name: 'Alice', age: 30})");

        List<String> names = template.queryForStringList("MATCH (p:Person) RETURN p.name AS name", "name");
        assertEquals(1, names.size());
        assertEquals("Alice", names.get(0));
    }

    @Test
    void query_shouldMapResultsUsingRowMapper() {
        template.execute("CREATE (p:Person {name: 'Bob', age: 25})");
        template.execute("CREATE (p:Person {name: 'Charlie', age: 35})");

        List<PersonRecord> people = template.query(
                "MATCH (p:Person) RETURN p ORDER BY p.name",
                (row) -> {
                    var p = row.getNode("p");
                    return new PersonRecord(
                            ValueMappers.asString(p.get("name")),
                            ValueMappers.asInteger(p.get("age")));
                });

        assertEquals(2, people.size());
        assertEquals("Bob", people.get(0).name());
        assertEquals(25, people.get(0).age());
        assertEquals("Charlie", people.get(1).name());
        assertEquals(35, people.get(1).age());
    }

    @Test
    void queryForObject_shouldReturnSingleResult() {
        template.execute("CREATE (p:Person {name: 'David', age: 40})");

        Optional<PersonRecord> result = template.queryForObject(
                "MATCH (p:Person) WHERE p.name = 'David' RETURN p",
                (row) -> {
                    var p = row.getNode("p");
                    return new PersonRecord(
                            ValueMappers.asString(p.get("name")),
                            ValueMappers.asInteger(p.get("age")));
                });

        assertTrue(result.isPresent());
        assertEquals("David", result.get().name());
        assertEquals(40, result.get().age());
    }

    @Test
    void queryForObject_shouldReturnEmptyWhenNoResults() {
        Optional<PersonRecord> result = template.queryForObject(
                "MATCH (p:Person) WHERE p.name = 'NonExistent' RETURN p",
                (row) -> {
                    var p = row.getNode("p");
                    return new PersonRecord(
                            ValueMappers.asString(p.get("name")),
                            ValueMappers.asInteger(p.get("age")));
                });

        assertTrue(result.isEmpty());
    }

    @Test
    void queryForStringList_shouldReturnListOfStrings() {
        template.execute("CREATE (p:Person {name: 'Eve', age: 28})");
        template.execute("CREATE (p:Person {name: 'Frank', age: 33})");

        List<String> names = template.queryForStringList(
                "MATCH (p:Person) RETURN p.name AS name ORDER BY p.name", "name");

        assertEquals(2, names.size());
        assertEquals("Eve", names.get(0));
        assertEquals("Frank", names.get(1));
    }

    @Test
    void query_shouldProvideRowNumberToMapper() {
        template.execute("CREATE (p:Person {name: 'G1', age: 1})");
        template.execute("CREATE (p:Person {name: 'G2', age: 2})");
        template.execute("CREATE (p:Person {name: 'G3', age: 3})");

        List<String> rowNumbers = template.query(
                "MATCH (p:Person) RETURN p ORDER BY p.name",
                (row) -> ValueMappers.asString(row.getNode("p").get("name")));

        assertEquals(List.of("G1", "G2", "G3"), rowNumbers);
    }

    record PersonRecord(String name, int age) {
    }
}
