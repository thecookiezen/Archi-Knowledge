package com.thecookiezen.ladybugdb.spring.repository.support;

import com.ladybugdb.Connection;
import com.ladybugdb.Database;
import com.thecookiezen.ladybugdb.spring.annotation.NodeEntity;
import com.thecookiezen.ladybugdb.spring.annotation.RelationshipEntity;
import com.thecookiezen.ladybugdb.spring.connection.SimpleConnectionFactory;
import com.thecookiezen.ladybugdb.spring.core.LadybugDBTemplate;
import com.thecookiezen.ladybugdb.spring.mapper.EntityWriter;
import com.thecookiezen.ladybugdb.spring.mapper.RowMapper;
import com.thecookiezen.ladybugdb.spring.mapper.ValueMappers;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.springframework.data.annotation.Id;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.StreamSupport;

import static org.junit.jupiter.api.Assertions.*;

class SimpleNodeRepositoryTest {

    private static Database db;
    private static SimpleConnectionFactory connectionFactory;
    private static LadybugDBTemplate template;
    private static SimpleNodeRepository<Person, Follows, String> repository;

    @BeforeAll
    static void setupAll() {
        db = new Database(":memory:");
        connectionFactory = new SimpleConnectionFactory(db);
        template = new LadybugDBTemplate(connectionFactory);

        try (Connection conn = new Connection(db)) {
            conn.query("CREATE NODE TABLE Person(name STRING PRIMARY KEY, age INT64)");
            conn.query("CREATE REL TABLE FOLLOWS(FROM Person TO Person)");
        }

        repository = new SimpleNodeRepository<>(template, Person.class, Follows.class,
                new EntityDescriptor<>(Person.class, personReader, personWriter),
                new EntityDescriptor<>(Follows.class, followsReader, followsWriter));
    }

    @AfterEach
    void tearDown() {
        template.execute("MATCH (n:Person) DETACH DELETE n");
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
    void save_shouldInsertNewNode() {
        Person person = new Person("Alice", 30);

        Person saved = repository.save(person);

        assertNotNull(saved);
        assertEquals("Alice", saved.name);
        assertEquals(30, saved.age);
    }

    @Test
    void save_shouldUpdateExistingNode() {
        Person person = new Person("Bob", 25);
        repository.save(person);

        person.age = 26;
        Person updated = repository.save(person);

        assertEquals("Bob", updated.name);
        assertEquals(26, updated.age);
    }

    @Test
    void findById_shouldReturnNodeWhenExists() {
        repository.save(new Person("Charlie", 35));

        Optional<Person> found = repository.findById("Charlie");

        assertTrue(found.isPresent());
        assertEquals("Charlie", found.get().name);
        assertEquals(35, found.get().age);
    }

    @Test
    void findById_shouldReturnEmptyWhenNotExists() {
        Optional<Person> found = repository.findById("NonExistent");

        assertTrue(found.isEmpty());
    }

    @Test
    void findAll_shouldReturnAllNodes() {
        repository.save(new Person("Dave", 40));
        repository.save(new Person("Eve", 28));

        List<Person> all = StreamSupport.stream(repository.findAll().spliterator(), false).toList();

        assertEquals(2, all.size());
    }

    @Test
    void findAll_shouldReturnEmptyWhenNoNodes() {
        List<Person> all = StreamSupport.stream(repository.findAll().spliterator(), false).toList();

        assertTrue(all.isEmpty());
    }

    @Test
    void count_shouldReturnNumberOfNodes() {
        repository.save(new Person("Frank", 45));
        repository.save(new Person("Grace", 32));

        long count = repository.count();

        assertEquals(2, count);
    }

    @Test
    void count_shouldReturnZeroWhenEmpty() {
        long count = repository.count();

        assertEquals(0, count);
    }

    @Test
    void existsById_shouldReturnTrueWhenExists() {
        repository.save(new Person("Henry", 50));

        assertTrue(repository.existsById("Henry"));
    }

    @Test
    void existsById_shouldReturnFalseWhenNotExists() {
        assertFalse(repository.existsById("NonExistent"));
    }

    @Test
    void deleteById_shouldRemoveNode() {
        repository.save(new Person("Ivan", 55));
        assertTrue(repository.existsById("Ivan"));

        repository.deleteById("Ivan");

        assertFalse(repository.existsById("Ivan"));
    }

    @Test
    void delete_shouldRemoveNode() {
        Person person = new Person("Julia", 60);
        repository.save(person);

        repository.delete(person);

        assertFalse(repository.existsById("Julia"));
    }

    @Test
    void deleteAll_shouldRemoveAllNodes() {
        repository.save(new Person("Karl", 65));
        repository.save(new Person("Laura", 70));

        repository.deleteAll();

        assertEquals(0, repository.count());
    }

    @Test
    void saveAll_shouldSaveMultipleNodes() {
        List<Person> people = List.of(
                new Person("Mike", 75),
                new Person("Nancy", 80));

        Iterable<Person> saved = repository.saveAll(people);
        List<Person> savedList = StreamSupport.stream(saved.spliterator(), false).toList();

        assertEquals(2, savedList.size());
        assertEquals(2, repository.count());
    }

    @Test
    void findAllById_shouldReturnMatchingNodes() {
        repository.save(new Person("Oscar", 85));
        repository.save(new Person("Paula", 90));
        repository.save(new Person("Quinn", 95));

        List<Person> found = StreamSupport.stream(
                repository.findAllById(List.of("Oscar", "Quinn")).spliterator(), false).toList();

        assertEquals(2, found.size());
    }

    @Test
    void create_shouldCreateRelationship() {
        Person alice = new Person("Alice", 1);
        Person bob = new Person("Bob", 2);
        repository.save(alice);
        repository.save(bob);

        Follows follows = new Follows(alice, bob);
        Follows created = repository.createRelation(alice, bob, follows);

        System.out.println(created);

        assertNotNull(created);
        assertEquals("Alice", created.from.name);
        assertEquals("Bob", created.to.name);
    }

    @Test
    void findById_shouldFindRelationship() {
        Person alice = new Person("Alice", 1);
        Person bob = new Person("Bob", 2);
        repository.save(alice);
        repository.save(bob);

        repository.createRelation(alice, bob, new Follows(alice, bob));

        // Optional<Follows> found = repository.findById(id); ????
        // assertTrue(found.isPresent());
        // assertEquals("Alice", found.get().from.name);
        // assertEquals("Bob", found.get().to.name);
    }

    @Test
    void findBySource_shouldFindRelationships() {
        Person alice = new Person("Alice", 1);
        Person bob = new Person("Bob", 2);
        Person charlie = new Person("Charlie", 3);
        repository.save(alice);
        repository.save(bob);
        repository.save(charlie);

        repository.createRelation(alice, bob, new Follows(alice, bob));
        repository.createRelation(alice, charlie, new Follows(alice, charlie));

        List<Follows> results = repository.findRelationsBySource(alice);
        assertEquals(2, results.size());
    }

    @Test
    void findAll_shouldReturnAll() {
        Person alice = new Person("Alice", 1);
        Person bob = new Person("Bob", 2);
        repository.save(alice);
        repository.save(bob);

        repository.createRelation(alice, bob, new Follows(alice, bob));

        List<Follows> results = repository.findAllRelations();
        assertEquals(1, results.size());
    }

    @Test
    void deleteById_shouldDelete() {
        Person alice = new Person("Alice", 1);
        Person bob = new Person("Bob", 2);
        repository.save(alice);
        repository.save(bob);

        repository.createRelation(alice, bob, new Follows(alice, bob));

        // repository.deleteById(id); ??

        List<Follows> results = repository.findAllRelations();
        assertTrue(results.isEmpty());
    }

    @NodeEntity(label = "Person")
    static class Person {
        @Id
        String name;
        int age;

        Person() {
        }

        Person(String name, int age) {
            this.name = name;
            this.age = age;
        }

        @Override
        public String toString() {
            return "Person{" + "name='" + name + '\'' + ", age=" + age + '}';
        }
    }

    static RowMapper<Person> personReader = (row) -> {
        var p = row.getNode("n");
        String name = ValueMappers.asString(p.get("name"));
        int age = ValueMappers.asInteger(p.get("age"));
        return new Person(name, age);
    };

    static EntityWriter<Person> personWriter = (entity) -> {
        return Map.of("age", entity.age);
    };

    @RelationshipEntity(type = "FOLLOWS", nodeType = Person.class, sourceField = "from", targetField = "to")
    static class Follows {
        Person from;

        Person to;

        Follows() {
        }

        Follows(Person from, Person to) {
            this.from = from;
            this.to = to;
        }

        @Override
        public String toString() {
            return "Follows{" + "from=" + from + ", to=" + to + '}';
        }
    }

    static EntityWriter<Follows> followsWriter = (entity) -> Map.of();

    static RowMapper<Follows> followsReader = (row) -> {
        var s = row.getNode("s");
        var t = row.getNode("t");
        String sourceName = ValueMappers.asString(s.get("name"));
        int sourceAge = ValueMappers.asInteger(s.get("age"));
        String targetName = ValueMappers.asString(t.get("name"));
        int targetAge = ValueMappers.asInteger(t.get("age"));
        return new Follows(new Person(sourceName, sourceAge), new Person(targetName, targetAge));
    };
}
