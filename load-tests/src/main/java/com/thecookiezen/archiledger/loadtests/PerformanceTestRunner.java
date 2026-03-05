package com.thecookiezen.archiledger.loadtests;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.stream.IntStream;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

import com.thecookiezen.archiledger.application.service.MemoryNoteService;
import com.thecookiezen.archiledger.domain.model.MemoryNote;
import com.thecookiezen.archiledger.domain.model.MemoryNoteId;

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

    private final MemoryNoteService memoryNoteService;

    @Value("${loadtest.scenario.name:Manual Run}")
    private String scenarioName;

    @Value("${loadtest.note-count:1000}")
    private int noteCount;

    @Value("${loadtest.links-per-note:10}")
    private int linksPerNote;

    @Value("${loadtest.batch-size:25}")
    private int batchSize;

    public PerformanceTestRunner(MemoryNoteService memoryNoteService) {
        this.memoryNoteService = memoryNoteService;
    }

    @Override
    public void run(String... args) {
        log.info("Starting Performance Test Runner...");

        PerformanceScenario scenario = new PerformanceScenario(scenarioName, noteCount, linksPerNote,
                batchSize);
        PerformanceReport report = new PerformanceReport();

        runScenario(scenario, report);

        System.out.println(report.generateMarkdownTable());

        System.exit(0);
    }

    private void runScenario(PerformanceScenario scenario, PerformanceReport report) {
        log.info("--------------------------------------------------");
        log.info("Running Scenario: {}", scenario.name());
        log.info("Notes: {}, Links/Note: {}", scenario.noteCount(), scenario.linksPerNote());

        long startTime = System.currentTimeMillis();

        try {
            processBatches(scenario);
        } catch (Exception e) {
            log.error("Scenario {} failed", scenario.name(), e);
        }

        long endTime = System.currentTimeMillis();
        long duration = endTime - startTime;

        log.info("Finished Scenario: {} in {} ms", scenario.name(), duration);
        report.addResult(scenario.name(), scenario.noteCount(), scenario.totalLinks(), duration);
    }

    private String generateRandomContent() {
        java.util.concurrent.ThreadLocalRandom random = java.util.concurrent.ThreadLocalRandom.current();
        String sentence = String.format("%s %s %s %s.",
                SUBJECTS[random.nextInt(SUBJECTS.length)],
                VERBS[random.nextInt(VERBS.length)],
                OBJECTS[random.nextInt(OBJECTS.length)],
                ADVERBS[random.nextInt(ADVERBS.length)]);
        if (random.nextBoolean()) {
            sentence += " " + String.format("%s %s %s %s.",
                    SUBJECTS[random.nextInt(SUBJECTS.length)],
                    VERBS[random.nextInt(VERBS.length)],
                    OBJECTS[random.nextInt(OBJECTS.length)],
                    ADVERBS[random.nextInt(ADVERBS.length)]);
        }
        return sentence;
    }

    private void processBatches(PerformanceScenario scenario) {
        log.info("Generating and saving data in batches...");
        int batches = (int) Math.ceil((double) scenario.noteCount() / scenario.batchSize());
        int linksPerNote = scenario.linksPerNote();

        for (int i = 0; i < batches; i++) {
            long batchStartTime = System.currentTimeMillis();
            int start = i * scenario.batchSize();
            int end = Math.min(start + scenario.batchSize(), scenario.noteCount());

            List<MemoryNote> batchNotes = IntStream.range(start, end)
                    .mapToObj(idx -> {
                        String uuid = UUID.randomUUID().toString();
                        return new MemoryNote(
                                new MemoryNoteId(uuid),
                                generateRandomContent(),
                                List.of("load-test", "benchmark"),
                                "load-test-scenario",
                                List.of("test"),
                                List.of(),
                                Instant.now().toString(),
                                0,
                                null);
                    })
                    .toList();

            memoryNoteService.createNotes(batchNotes);

            final int currentBatchSize = batchNotes.size();
            int createdLinksCount = 0;
            if (currentBatchSize > 0 && linksPerNote > 0) {
                List<MemoryNoteId[]> linkPairs = new ArrayList<>();
                for (int j = 0; j < currentBatchSize; j++) {
                    MemoryNote source = batchNotes.get(j);
                    for (int k = 0; k < linksPerNote; k++) {
                        int targetIndex = (j + k + 1) % currentBatchSize;
                        MemoryNote target = batchNotes.get(targetIndex);

                        if (!source.id().equals(target.id())) {
                            linkPairs.add(new MemoryNoteId[] { source.id(), target.id() });
                        }
                    }
                }
                for (MemoryNoteId[] pair : linkPairs) {
                    memoryNoteService.addLink(pair[0], pair[1], "RELATED_TO");
                }
                createdLinksCount = linkPairs.size();
            }

            long batchEndTime = System.currentTimeMillis();
            long batchDuration = batchEndTime - batchStartTime;
            int totalOps = currentBatchSize + createdLinksCount;
            double throughput = batchDuration > 0 ? (double) totalOps / (batchDuration / 1000.0) : 0.0;

            log.info("Batch {}/{}: {} notes, {} links in {} ms ({} ops/sec)",
                    i + 1, batches, currentBatchSize, createdLinksCount, batchDuration,
                    String.format("%.2f", throughput));
        }
    }
}
