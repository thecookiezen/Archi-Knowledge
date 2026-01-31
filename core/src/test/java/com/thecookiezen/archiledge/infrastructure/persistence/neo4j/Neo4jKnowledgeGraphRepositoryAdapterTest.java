package com.thecookiezen.archiledge.infrastructure.persistence.neo4j;

import com.thecookiezen.archiledge.domain.model.Entity;
import com.thecookiezen.archiledge.domain.model.EntityId;
import com.thecookiezen.archiledge.domain.model.Relation;
import com.thecookiezen.archiledge.domain.model.RelationType;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.Neo4jContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertFalse;

@SpringBootTest
@ActiveProfiles("neo4j")
@Testcontainers
class Neo4jKnowledgeGraphRepositoryAdapterTest {

    @org.springframework.boot.SpringBootConfiguration
    @org.springframework.boot.autoconfigure.EnableAutoConfiguration
    @org.springframework.context.annotation.ComponentScan(basePackages = "com.thecookiezen.archiledge.infrastructure.persistence.neo4j")
    static class TestConfig {
    }

    @Container
    static Neo4jContainer<?> neo4jContainer = new Neo4jContainer<>("neo4j:5")
            .withoutAuthentication();

    @DynamicPropertySource
    static void neo4jProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.neo4j.uri", neo4jContainer::getBoltUrl);
        registry.add("spring.neo4j.authentication.username", () -> "neo4j");
        registry.add("spring.neo4j.authentication.password", neo4jContainer::getAdminPassword);
    }

    @AfterAll
    static void tearDown() {
        if (neo4jContainer != null) {
            neo4jContainer.close();
        }
    }

    @Autowired
    private Neo4jKnowledgeGraphRepositoryAdapter repository;

    @Test
    void saveAndFindEntity() {
        Entity entity = new Entity("IntegrationTest", "TestType");
        repository.saveEntity(entity);

        List<Entity> results = repository.findAllEntities();
        assertFalse(results.isEmpty());
        boolean found = results.stream().anyMatch(e -> e.name().equals(new EntityId("IntegrationTest")));
        assert (found);
    }

    @Test
    void saveAndFindRelation() {
        Entity e1 = new Entity("N1", "T");
        Entity e2 = new Entity("N2", "T");
        repository.saveEntity(e1);
        repository.saveEntity(e2);

        Relation relation = new Relation("N1", "N2", "TEST_REL");
        repository.saveRelation(relation);

        List<Relation> relations = repository.findAllRelations();
        boolean found = relations.stream()
                .anyMatch(r -> r.from().equals(new EntityId("N1")) && r.to().equals(new EntityId("N2"))
                        && r.relationType().equals(new RelationType("TEST_REL")));
        assert (found);
    }
}
