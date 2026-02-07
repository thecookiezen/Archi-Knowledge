package com.thecookiezen.archiledger.loadtests;

import java.util.ArrayList;
import java.util.List;

public class PerformanceReport {

    private final List<Result> results = new ArrayList<>();

    public void addResult(String scenarioName, long entityCount, long relationCount, long durationMs) {
        results.add(new Result(scenarioName, entityCount, relationCount, durationMs));
    }

    public String generateMarkdownTable() {
        StringBuilder sb = new StringBuilder();
        sb.append("\n# Performance Test Results\n\n");
        sb.append("| Scenario | Entities | Relations | Duration (ms) | Throughput (ops/sec) |\n");
        sb.append("|----------|----------|-----------|---------------|----------------------|\n");

        for (Result r : results) {
            long totalOps = r.entityCount + r.relationCount;
            double throughput = (double) totalOps / (r.durationMs / 1000.0);

            sb.append(String.format("| %-8s | %-8d | %-9d | %-13d | %-20.2f |\n",
                    r.scenarioName, r.entityCount, r.relationCount, r.durationMs, throughput));
        }
        sb.append("\n");
        return sb.toString();
    }

    private record Result(String scenarioName, long entityCount, long relationCount, long durationMs) {
    }
}
