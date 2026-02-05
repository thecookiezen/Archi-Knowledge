package com.thecookiezen.archiledger.infrastructure.persistence.ladybugdb;

import com.ladybugdb.Connection;
import com.ladybugdb.Database;
import com.ladybugdb.FlatTuple;
import com.ladybugdb.QueryResult;
import com.thecookiezen.archiledger.domain.model.Entity;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.lang.reflect.Method;
import java.util.List;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

public class SpringDataLadybugdbRepositoryTest {

    private SpringDataLadybugdbRepository repository;
    private Connection conn;
    private Database db;

    @BeforeEach
    void setup() {
        db = new Database(":memory:");
        conn = new Connection(db);

        conn.query("CREATE NODE TABLE Entity(name STRING PRIMARY KEY, type STRING, observations STRING[])");
        conn.query("CREATE REL TABLE RelatedTo(name STRING PRIMARY KEY, FROM Entity TO Entity, type STRING)");

        repository = new SpringDataLadybugdbRepository(conn);
    }

    @Test
    void saveAndFindEntity() {
        Entity entity = new Entity("IntegrationTest", "TestType", List.of("Test observation", "Another observation"));
        repository.save(entity);

        List<Entity> results = repository.findAll();
        assertFalse(results.isEmpty());
        boolean found = results.stream().anyMatch(e -> e.name().equals(entity.name()));
        assertTrue(found);
    }
}
