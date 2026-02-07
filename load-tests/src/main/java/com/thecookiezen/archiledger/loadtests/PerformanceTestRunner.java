package com.thecookiezen.archiledger.loadtests;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.stream.IntStream;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

import com.thecookiezen.archiledger.application.service.KnowledgeGraphService;
import com.thecookiezen.archiledger.domain.model.Entity;
import com.thecookiezen.archiledger.domain.model.EntityId;
import com.thecookiezen.archiledger.domain.model.EntityType;
import com.thecookiezen.archiledger.domain.model.Relation;
import com.thecookiezen.archiledger.domain.model.RelationType;

@Component
public class PerformanceTestRunner implements CommandLineRunner {

    private static final Logger log = LoggerFactory.getLogger(PerformanceTestRunner.class);

    private static final String[] SUBJECTS = {
            "The system", "A user", "The database", "External service", "The algorithm",
            "Network latency", "Memory usage", "CPU load", "The application", "A microservice"
    };

    private static final String[] VERBS = {
            "processes", "stores", "retrieves", "analyzes", "connects to",
            "disconnects from", "optimizes", "calculates", "validates", "updates"
    };

    private static final String[] OBJECTS = {
            "the data", "secure credentials", "user profiles", "transaction logs", "cached items",
            "configuration files", "search results", "analytics metrics", "backup archives", "api responses"
    };

    private static final String[] ADVERBS = {
            "quickly", "efficiently", "securely", "slowly", "redundantly",
            "automatically", "manually", "consistently", "periodically", "asynchronously"
    };

    private final KnowledgeGraphService knowledgeGraphService;

    @Value("${loadtest.scenario.name:Manual Run}")
    private String scenarioName;

    @Value("${loadtest.entity-count:1000}")
    private int entityCount;

    @Value("${loadtest.relations-per-entity:10}")
    private int relationsPerEntity;

    @Value("${loadtest.batch-size:25}")
    private int batchSize;

    public PerformanceTestRunner(KnowledgeGraphService knowledgeGraphService) {
        this.knowledgeGraphService = knowledgeGraphService;
    }

    @Override
    public void run(String... args) {
        log.info("Starting Performance Test Runner...");

        PerformanceScenario scenario = new PerformanceScenario(scenarioName, entityCount, relationsPerEntity,
                batchSize);
        PerformanceReport report = new PerformanceReport();

        runScenario(scenario, report);

        System.out.println(report.generateMarkdownTable());

        System.exit(0);
    }

    private void runScenario(PerformanceScenario scenario, PerformanceReport report) {
        log.info("--------------------------------------------------");
        log.info("Running Scenario: {}", scenario.name());
        log.info("Entities: {}, Relations/Entity: {}", scenario.entityCount(), scenario.relationsPerEntity());

        long startTime = System.currentTimeMillis();

        try {
            processBatches(scenario);
        } catch (Exception e) {
            log.error("Scenario {} failed", scenario.name(), e);
        }

        long endTime = System.currentTimeMillis();
        long duration = endTime - startTime;

        log.info("Finished Scenario: {} in {} ms", scenario.name(), duration);
        report.addResult(scenario.name(), scenario.entityCount(), scenario.totalRelations(), duration);
    }

    private String generateRandomObservation() {
        java.util.concurrent.ThreadLocalRandom random = java.util.concurrent.ThreadLocalRandom.current();
        return String.format("%s %s %s %s.",
                SUBJECTS[random.nextInt(SUBJECTS.length)],
                VERBS[random.nextInt(VERBS.length)],
                OBJECTS[random.nextInt(OBJECTS.length)],
                ADVERBS[random.nextInt(ADVERBS.length)]);
    }

    private void processBatches(PerformanceScenario scenario) {
        log.info("Generating and saving data in batches...");
        int batches = (int) Math.ceil((double) scenario.entityCount() / scenario.batchSize());
        int relationsPerEntity = scenario.relationsPerEntity();

        for (int i = 0; i < batches; i++) {
            long batchStartTime = System.currentTimeMillis();
            int start = i * scenario.batchSize();
            int end = Math.min(start + scenario.batchSize(), scenario.entityCount());

            List<Entity> batchEntities = IntStream.range(start, end)
                    .mapToObj(idx -> {
                        String uuid = UUID.randomUUID().toString();
                        String observation = generateRandomObservation();
                        if (java.util.concurrent.ThreadLocalRandom.current().nextBoolean()) {
                            observation += " " + generateRandomObservation();
                        }

                        return new Entity(
                                new EntityId(uuid),
                                new EntityType("TestEntity"),
                                List.of(observation));
                    })
                    .toList();

            knowledgeGraphService.createEntities(batchEntities);

            final int currentBatchSize = batchEntities.size();
            int createdRelationsCount = 0;
            if (currentBatchSize > 0 && relationsPerEntity > 0) {
                List<Relation> batchRelations = new ArrayList<>();
                for (int j = 0; j < currentBatchSize; j++) {
                    Entity source = batchEntities.get(j);
                    for (int k = 0; k < relationsPerEntity; k++) {
                        int targetIndex = (j + k + 1) % currentBatchSize;
                        Entity target = batchEntities.get(targetIndex);

                        if (!source.name().equals(target.name())) {
                            batchRelations.add(new Relation(
                                    source.name(),
                                    target.name(),
                                    new RelationType("RELATED_TO")));
                        }
                    }
                }
                if (!batchRelations.isEmpty()) {
                    knowledgeGraphService.createRelations(batchRelations);
                    createdRelationsCount = batchRelations.size();
                }
            }

            long batchEndTime = System.currentTimeMillis();
            long batchDuration = batchEndTime - batchStartTime;
            int totalOps = currentBatchSize + createdRelationsCount;
            double throughput = batchDuration > 0 ? (double) totalOps / (batchDuration / 1000.0) : 0.0;

            log.info("Batch {}/{}: {} entities, {} relations in {} ms ({} ops/sec)",
                    i + 1, batches, currentBatchSize, createdRelationsCount, batchDuration,
                    String.format("%.2f", throughput));
        }
    }
}
