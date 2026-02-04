package com.thecookiezen.archiledger.infrastructure.persistence.ladybugdb;

import com.ladybugdb.Connection;
import com.ladybugdb.QueryResult;
import com.ladybugdb.Value;
import com.thecookiezen.archiledger.infrastructure.persistence.neo4j.model.Neo4jEntity;
import com.thecookiezen.archiledger.infrastructure.persistence.neo4j.repository.RelationProjection;
import org.neo4j.cypherdsl.core.Cypher;
import org.neo4j.cypherdsl.core.Node;
import org.neo4j.cypherdsl.core.Relationship;
import org.neo4j.cypherdsl.core.Statement;
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
public class SpringDataLadybugdbRepository implements PagingAndSortingRepository<Neo4jEntity, String>,
        QueryByExampleExecutor<Neo4jEntity>, CrudRepository<Neo4jEntity, String> {

    private final Connection connection;

    public SpringDataLadybugdbRepository(Connection connection) {
        this.connection = connection;
    }

    public List<Neo4jEntity> searchEntities(String query) {

        QueryResult result = connection
                .query("MATCH (n:Entity) WHERE toLower(n.name) CONTAINS toLower($query) RETURN n");

        Node n = Cypher.node("Entity").named("n");
        Statement statement = Cypher.match(n)
                .where(Cypher.toLower(n.property("name")).contains(Cypher.toLower(Cypher.literalOf(query))))
                .or(Cypher.toLower(n.property("type")).contains(Cypher.toLower(Cypher.literalOf(query))))
                .returning(n)
                .build();

        return executeAndMapNodes(statement.getCypher());
    }

    public List<Neo4jEntity> findAllEntitiesWithRelations() {
        Node n = Cypher.node("Entity").named("n");
        Node m = Cypher.node("Entity").named("m");
        Relationship r = n.relationshipTo(m).named("r");

        // This query returns a mix of nodes and relationships.
        // For simplicity in this adaptation, we will focus on returning the start nodes
        // 'n'.
        // To strictly match "RETURN n, r, m", we would need a more complex mapping
        // structure.
        // Assuming the method wants a list of entities (potentially with relations
        // populated).

        Statement statement = Cypher.match(r)
                .returning(n)
                .build();

        return executeAndMapNodes(statement.getCypher());
    }

    public List<Neo4jEntity> findByType(String type) {
        Node n = Cypher.node("Entity").named("n");
        Statement statement = Cypher.match(n)
                .where(n.property("type").isEqualTo(Cypher.literalOf(type)))
                .returning(n)
                .build();

        return executeAndMapNodes(statement.getCypher());
    }

    public List<RelationProjection> findRelationsForEntity(String entityName) {
        Node source = Cypher.node("Entity").named("source");
        Node target = Cypher.node("Entity").named("target");
        Relationship r = source.relationshipTo(target, "RELATED_TO").named("r");

        Statement statement = Cypher.match(r)
                .where(source.property("name").isEqualTo(Cypher.literalOf(entityName))
                        .or(target.property("name").isEqualTo(Cypher.literalOf(entityName))))
                .returning(
                        source.property("name").as("fromName"),
                        target.property("name").as("toName"),
                        r.property("relationType").as("relationType"))
                .build();

        return executeAndMapRelations(statement.getCypher());
    }

    public List<RelationProjection> findRelationsByRelationType(String relationType) {
        Node source = Cypher.node("Entity").named("source");
        Node target = Cypher.node("Entity").named("target");
        Relationship r = source.relationshipTo(target, "RELATED_TO").named("r");

        Statement statement = Cypher.match(r)
                .where(r.property("relationType").isEqualTo(Cypher.literalOf(relationType)))
                .returning(
                        source.property("name").as("fromName"),
                        target.property("name").as("toName"),
                        r.property("relationType").as("relationType"))
                .build();

        return executeAndMapRelations(statement.getCypher());
    }

    public List<Neo4jEntity> findRelatedEntities(String entityName) {
        Node n = Cypher.node("Entity").named("n");
        Node m = Cypher.node("Entity").named("m");
        Relationship r = n.relationshipTo(m, "RELATED_TO").named("r");

        Statement statement = Cypher.match(r)
                .where(n.property("name").isEqualTo(Cypher.literalOf(entityName)))
                .returningDistinct(m)
                .build();

        return executeAndMapNodes(statement.getCypher());
    }

    public List<String> findAllEntityTypes() {
        Node n = Cypher.node("Entity").named("n");
        Statement statement = Cypher.match(n)
                .returningDistinct(n.property("type").as("type"))
                .build();

        return executeAndMapString(statement.getCypher(), "type");
    }

    public List<String> findAllRelationTypes() {
        Node n = Cypher.node("Entity").named("n");
        Node m = Cypher.node("Entity");
        Relationship r = n.relationshipTo(m, "RELATED_TO").named("r");

        Statement statement = Cypher.match(r)
                .returningDistinct(r.property("relationType").as("relationType"))
                .build();

        return executeAndMapString(statement.getCypher(), "relationType");
    }

    // --- Helper Methods ---

    @SuppressWarnings("unchecked")
    private List<Neo4jEntity> executeAndMapNodes(String cypher) {
        try {
            QueryResult result = connection.execute(cypher);
            List<Neo4jEntity> entities = new ArrayList<>();

            // Best-effort mapping logic
            for (Object item : result) {
                if (item instanceof Map) {
                    Map<String, Object> map = (Map<String, Object>) item;
                    // Logic to extract node `n`
                    Object nodeObj = map.get("n");
                    if (nodeObj instanceof Map) {
                        Map<String, Object> nodeProps = (Map<String, Object>) nodeObj;
                        String name = (String) nodeProps.get("name");
                        String type = (String) nodeProps.get("type");
                        if (name != null) {
                            entities.add(new Neo4jEntity(name, type));
                        }
                    }
                }
            }
            return entities;
        } catch (Exception e) {
            e.printStackTrace();
            return Collections.emptyList();
        }
    }

    @SuppressWarnings("unchecked")
    private List<RelationProjection> executeAndMapRelations(String cypher) {
        try {
            QueryResult result = connection.execute(cypher);
            List<RelationProjection> projections = new ArrayList<>();

            for (Object item : result) {
                if (item instanceof Map) {
                    Map<String, Object> map = (Map<String, Object>) item;
                    String from = (String) map.get("fromName");
                    String to = (String) map.get("toName");
                    String type = (String) map.get("relationType");
                    if (from != null && to != null && type != null) {
                        projections.add(new RelationProjection(from, to, type));
                    }
                }
            }
            return projections;
        } catch (Exception e) {
            e.printStackTrace();
            return Collections.emptyList();
        }
    }

    @SuppressWarnings("unchecked")
    private List<String> executeAndMapString(String cypher, String column) {
        try {
            QueryResult result = connection.execute(cypher);
            List<String> values = new ArrayList<>();

            for (Object item : result) {
                if (item instanceof Map) {
                    Map<String, Object> map = (Map<String, Object>) item;
                    Object val = map.get(column);
                    if (val != null) {
                        values.add(val.toString());
                    }
                }
            }
            return values;
        } catch (Exception e) {
            e.printStackTrace();
            return Collections.emptyList();
        }
    }

    // --- Unimplemented Methods ---

    @Override
    public Iterable<Neo4jEntity> findAll(Sort sort) {
        throw new UnsupportedOperationException("Unimplemented method 'findAll'");
    }

    @Override
    public Page<Neo4jEntity> findAll(Pageable pageable) {
        throw new UnsupportedOperationException("Unimplemented method 'findAll'");
    }

    @Override
    public <S extends Neo4jEntity> S save(S entity) {
        throw new UnsupportedOperationException("Unimplemented method 'save'");
    }

    @Override
    public <S extends Neo4jEntity> Iterable<S> saveAll(Iterable<S> entities) {
        throw new UnsupportedOperationException("Unimplemented method 'saveAll'");
    }

    @Override
    public Optional<Neo4jEntity> findById(String id) {
        throw new UnsupportedOperationException("Unimplemented method 'findById'");
    }

    @Override
    public boolean existsById(String id) {
        throw new UnsupportedOperationException("Unimplemented method 'existsById'");
    }

    @Override
    public Iterable<Neo4jEntity> findAll() {
        throw new UnsupportedOperationException("Unimplemented method 'findAll'");
    }

    @Override
    public Iterable<Neo4jEntity> findAllById(Iterable<String> ids) {
        throw new UnsupportedOperationException("Unimplemented method 'findAllById'");
    }

    @Override
    public long count() {
        throw new UnsupportedOperationException("Unimplemented method 'count'");
    }

    @Override
    public void deleteById(String id) {
        throw new UnsupportedOperationException("Unimplemented method 'deleteById'");
    }

    @Override
    public void delete(Neo4jEntity entity) {
        throw new UnsupportedOperationException("Unimplemented method 'delete'");
    }

    @Override
    public void deleteAllById(Iterable<? extends String> ids) {
        throw new UnsupportedOperationException("Unimplemented method 'deleteAllById'");
    }

    @Override
    public void deleteAll(Iterable<? extends Neo4jEntity> entities) {
        throw new UnsupportedOperationException("Unimplemented method 'deleteAll'");
    }

    @Override
    public void deleteAll() {
        throw new UnsupportedOperationException("Unimplemented method 'deleteAll'");
    }

    @Override
    public <S extends Neo4jEntity> Optional<S> findOne(Example<S> example) {
        throw new UnsupportedOperationException("Unimplemented method 'findOne'");
    }

    @Override
    public <S extends Neo4jEntity> Iterable<S> findAll(Example<S> example) {
        throw new UnsupportedOperationException("Unimplemented method 'findAll'");
    }

    @Override
    public <S extends Neo4jEntity> Iterable<S> findAll(Example<S> example, Sort sort) {
        throw new UnsupportedOperationException("Unimplemented method 'findAll'");
    }

    @Override
    public <S extends Neo4jEntity> Page<S> findAll(Example<S> example, Pageable pageable) {
        throw new UnsupportedOperationException("Unimplemented method 'findAll'");
    }

    @Override
    public <S extends Neo4jEntity> long count(Example<S> example) {
        throw new UnsupportedOperationException("Unimplemented method 'count'");
    }

    @Override
    public <S extends Neo4jEntity> boolean exists(Example<S> example) {
        throw new UnsupportedOperationException("Unimplemented method 'exists'");
    }

    @Override
    public <S extends Neo4jEntity, R> R findBy(Example<S> example, Function<FetchableFluentQuery<S>, R> queryFunction) {
        throw new UnsupportedOperationException("Unimplemented method 'findBy'");
    }
}
