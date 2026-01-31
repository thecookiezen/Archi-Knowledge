package com.thecookiezen.archiledge.infrastructure.config;

import org.neo4j.configuration.connectors.BoltConnector;
import org.neo4j.configuration.helpers.SocketAddress;
import org.neo4j.dbms.api.DatabaseManagementService;
import org.neo4j.dbms.api.DatabaseManagementServiceBuilder;
import org.neo4j.graphdb.GraphDatabaseService;
import org.neo4j.harness.Neo4j;
import org.neo4j.harness.Neo4jBuilders;
import org.neo4j.driver.AuthTokens;
import org.neo4j.driver.Driver;
import org.neo4j.driver.GraphDatabase;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.net.ServerSocket;
import java.net.URI;
import java.nio.file.Files;
import java.nio.file.Path;

@Configuration
@Profile("neo4j")
class EmbeddedNeo4jConfig {

    private static final Logger logger = LoggerFactory.getLogger(EmbeddedNeo4jConfig.class);

    @Value("${memory.neo4j.data-dir:}")
    private String dataDir;

    @Bean(destroyMethod = "close")
    @ConditionalOnProperty(name = "spring.neo4j.uri", matchIfMissing = true, havingValue = "embedded")
    public Neo4j embeddedNeo4jServer() throws IOException {
        if (dataDir != null && !dataDir.isBlank()) {
            return new PersistentEmbeddedNeo4j(Path.of(dataDir));
        }

        return Neo4jBuilders.newInProcessBuilder()
                .withDisabledServer()
                .withFixture("MERGE (n:Entity {name: 'Root', type: 'System'})")
                .build();
    }

    @Bean
    @ConditionalOnProperty(name = "spring.neo4j.uri", matchIfMissing = true, havingValue = "embedded")
    public Driver neo4jDriver(Neo4j neo4j) {
        return GraphDatabase.driver(neo4j.boltURI(), AuthTokens.none());
    }

    private static class PersistentEmbeddedNeo4j implements Neo4j {
        private final DatabaseManagementService managementService;
        private final GraphDatabaseService defaultDatabase;
        private final URI boltUri;

        PersistentEmbeddedNeo4j(Path dataDir) throws IOException {
            boolean isNewDatabase = !Files.exists(dataDir.resolve("neostore.id"))
                    && !Files.exists(dataDir.resolve("data"));
            Files.createDirectories(dataDir);

            int boltPort = findAvailablePort();

            this.managementService = new DatabaseManagementServiceBuilder(dataDir)
                    .setConfig(BoltConnector.enabled, true)
                    .setConfig(BoltConnector.listen_address, new SocketAddress("localhost", boltPort))
                    .build();

            this.boltUri = URI.create("bolt://localhost:" + boltPort);
            this.defaultDatabase = managementService.database("neo4j");

            if (isNewDatabase) {
                logger.info("Creating new persistent Neo4j database at: {}", dataDir.toAbsolutePath());
                try (var tx = defaultDatabase.beginTx()) {
                    tx.execute("MERGE (n:Entity {name: 'Root', type: 'System'})");
                    tx.commit();
                }
            } else {
                logger.info("Loaded existing Neo4j database from: {}", dataDir.toAbsolutePath());
            }
        }

        private static int findAvailablePort() throws IOException {
            try (ServerSocket socket = new ServerSocket(0)) {
                return socket.getLocalPort();
            }
        }

        @Override
        public URI boltURI() {
            return boltUri;
        }

        @Override
        public URI httpURI() {
            throw new UnsupportedOperationException("HTTP is disabled for embedded Neo4j");
        }

        @Override
        public URI httpsURI() {
            throw new UnsupportedOperationException("HTTPS is disabled for embedded Neo4j");
        }

        @Override
        public GraphDatabaseService defaultDatabaseService() {
            return defaultDatabase;
        }

        @Override
        public DatabaseManagementService databaseManagementService() {
            return managementService;
        }

        @Override
        public org.neo4j.graphdb.config.Configuration config() {
            return org.neo4j.graphdb.config.Configuration.EMPTY;
        }

        @Override
        public void printLogs(java.io.PrintStream out) {
            out.println("Persistent embedded Neo4j - logs stored in data directory");
        }

        @Override
        public void close() {
            managementService.shutdown();
        }
    }
}
