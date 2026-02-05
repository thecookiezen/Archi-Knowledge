package com.thecookiezen.archiledger.infrastructure.persistence.ladybugdb;

import com.ladybugdb.Connection;
import com.ladybugdb.LbugList;
import com.ladybugdb.QueryResult;
import com.ladybugdb.Value;
import com.thecookiezen.archiledger.domain.model.Entity;
import com.thecookiezen.archiledger.infrastructure.persistence.neo4j.model.Neo4jEntity;
import com.thecookiezen.archiledger.infrastructure.persistence.neo4j.repository.RelationProjection;
import org.neo4j.cypherdsl.core.Cypher;
import org.neo4j.cypherdsl.core.Node;
import org.neo4j.cypherdsl.core.Relationship;
import org.neo4j.cypherdsl.core.Statement;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Example;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.repository.CrudRepository;
import org.springframework.data.repository.PagingAndSortingRepository;
import org.springframework.data.repository.query.FluentQuery.FetchableFluentQuery;
import org.springframework.data.repository.query.QueryByExampleExecutor;
import org.springframework.stereotype.Repository;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.StreamSupport;

@Repository
public class SpringDataLadybugdbRepository {

    private final Logger logger = LoggerFactory.getLogger(SpringDataLadybugdbRepository.class);

    private final Connection connection;

    public SpringDataLadybugdbRepository(Connection connection) {
        this.connection = connection;
    }

    public List<Entity> findAllEntitiesWithRelations() {
        throw new UnsupportedOperationException("Unimplemented method 'findAllEntitiesWithRelations'");
        // Node n = Cypher.node("Entity").named("n");
        // Node m = Cypher.node("Entity").named("m");
        // Relationship r = n.relationshipTo(m).named("r");

        // // This query returns a mix of nodes and relationships.
        // // For simplicity in this adaptation, we will focus on returning the start
        // nodes
        // // 'n'.
        // // To strictly match "RETURN n, r, m", we would need a more complex mapping
        // // structure.
        // // Assuming the method wants a list of entities (potentially with relations
        // // populated).

        // Statement statement = Cypher.match(r)
        // .returning(n)
        // .build();

        // return executeAndMapNodes(statement.getCypher());
    }

    public List<Entity> findByType(String type) {
        throw new UnsupportedOperationException("Unimplemented method 'findByType'");
        // Node n = Cypher.node("Entity").named("n");
        // Statement statement = Cypher.match(n)
        // .where(n.property("type").isEqualTo(Cypher.literalOf(type)))
        // .returning(n)
        // .build();

        // return executeAndMapNodes(statement.getCypher());
    }

    public List<RelationProjection> findRelationsForEntity(String entityName) {
        throw new UnsupportedOperationException("Unimplemented method 'findRelationsForEntity'");
        // Node source = Cypher.node("Entity").named("source");
        // Node target = Cypher.node("Entity").named("target");
        // Relationship r = source.relationshipTo(target, "RELATED_TO").named("r");

        // Statement statement = Cypher.match(r)
        // .where(source.property("name").isEqualTo(Cypher.literalOf(entityName))
        // .or(target.property("name").isEqualTo(Cypher.literalOf(entityName))))
        // .returning(
        // source.property("name").as("fromName"),
        // target.property("name").as("toName"),
        // r.property("relationType").as("relationType"))
        // .build();

        // return executeAndMapRelations(statement.getCypher());
    }

    public List<RelationProjection> findRelationsByRelationType(String relationType) {
        throw new UnsupportedOperationException("Unimplemented method 'findRelationsByRelationType'");
        // Node source = Cypher.node("Entity").named("source");
        // Node target = Cypher.node("Entity").named("target");
        // Relationship r = source.relationshipTo(target, "RELATED_TO").named("r");

        // Statement statement = Cypher.match(r)
        // .where(r.property("relationType").isEqualTo(Cypher.literalOf(relationType)))
        // .returning(
        // source.property("name").as("fromName"),
        // target.property("name").as("toName"),
        // r.property("relationType").as("relationType"))
        // .build();

        // return executeAndMapRelations(statement.getCypher());
    }

    public List<Entity> findRelatedEntities(String entityName) {
        throw new UnsupportedOperationException("Unimplemented method 'findRelatedEntities'");
        // Node n = Cypher.node("Entity").named("n");
        // Node m = Cypher.node("Entity").named("m");
        // Relationship r = n.relationshipTo(m, "RELATED_TO").named("r");

        // Statement statement = Cypher.match(r)
        // .where(n.property("name").isEqualTo(Cypher.literalOf(entityName)))
        // .returningDistinct(m)
        // .build();

        // return executeAndMapNodes(statement.getCypher());
    }

    public List<String> findAllEntityTypes() {
        throw new UnsupportedOperationException("Unimplemented method 'findAllEntityTypes'");
        // Node n = Cypher.node("Entity").named("n");
        // Statement statement = Cypher.match(n)
        // .returningDistinct(n.property("type").as("type"))
        // .build();

        // return executeAndMapString(statement.getCypher(), "type");
    }

    public List<String> findAllRelationTypes() {
        throw new UnsupportedOperationException("Unimplemented method 'findAllRelationTypes'");
        // Node n = Cypher.node("Entity").named("n");
        // Node m = Cypher.node("Entity");
        // Relationship r = n.relationshipTo(m, "RELATED_TO").named("r");

        // Statement statement = Cypher.match(r)
        // .returningDistinct(r.property("relationType").as("relationType"))
        // .build();

        // return executeAndMapString(statement.getCypher(), "relationType");
    }

    public Entity save(Entity entity) {
        Node n = Cypher.node("Entity").named("n");

        Statement statement = Cypher.merge(n.withProperties("name", Cypher.literalOf(entity.name().value())))
                .set(
                        n.property("type").to(Cypher.literalOf(entity.type().value())),
                        n.property("observations").to(Cypher.literalOf(entity.observations().toArray(new String[0]))))
                .returning(n)
                .build();

        String cypherQuery = statement.getCypher();
        logger.info("Executing save query: {}", cypherQuery);

        QueryResult result = connection.query(cypherQuery);
        logger.info("Save result: {}", result);

        return entity;
    }

    public Iterable<Entity> saveAll(Iterable<Entity> entities) {
        List<Entity> savedEntities = new ArrayList<>();
        for (Entity entity : entities) {
            savedEntities.add(save(entity));
        }
        return savedEntities;
    }

    public Optional<Entity> findById(String id) {
        throw new UnsupportedOperationException("Unimplemented method 'findById'");
    }

    public boolean existsById(String id) {
        throw new UnsupportedOperationException("Unimplemented method 'existsById'");
    }

    public List<Entity> findAll() {
        Node n = Cypher.node("Entity").named("n");

        Statement statement = Cypher.match(n)
                .returning(
                        n.property("name").as("name"),
                        n.property("type").as("type"),
                        n.property("observations").as("observations"))
                .build();

        String cypherQuery = statement.getCypher();
        logger.info("Executing findAll query: {}", cypherQuery);

        QueryResult result = connection.query(cypherQuery);
        List<Entity> entities = new ArrayList<>();

        while (result.hasNext()) {
            var row = result.getNext();
            String name = row.getValue(0).getValue().toString();
            String type = row.getValue(1).getValue().toString();

            List<String> observations;

            Value obsValue = row.getValue(2);
            try (LbugList lbugList = new LbugList(obsValue)) {

                long size = lbugList.getListSize();
                observations = new ArrayList<>((int) size);
                for (long i = 0; i < size; i++) {
                    Value element = lbugList.getListElement(i);
                    String str = element.getValue();
                    observations.add(str);
                }
            }

            entities.add(new Entity(name, type, observations));
        }

        return entities;
    }

    public Iterable<Entity> findAllById(Iterable<String> ids) {
        throw new UnsupportedOperationException("Unimplemented method 'findAllById'");
    }

    public long count() {
        throw new UnsupportedOperationException("Unimplemented method 'count'");
    }

    public void deleteById(String id) {
        throw new UnsupportedOperationException("Unimplemented method 'deleteById'");
    }

    public void delete(Entity entity) {
        throw new UnsupportedOperationException("Unimplemented method 'delete'");
    }

    public void deleteAllById(Iterable<? extends String> ids) {
        throw new UnsupportedOperationException("Unimplemented method 'deleteAllById'");
    }

    public void deleteAll(Iterable<? extends Entity> entities) {
        throw new UnsupportedOperationException("Unimplemented method 'deleteAll'");
    }

    public void deleteAll() {
        QueryResult result = connection.query("MATCH (n) DETACH DELETE n");
        logger.info("Deleted all entities: {}", result);
    }
}
