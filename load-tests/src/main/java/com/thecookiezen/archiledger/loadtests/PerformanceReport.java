package com.thecookiezen.archiledger.loadtests;

import java.util.ArrayList;
import java.util.List;

public class PerformanceReport {

    private final List<Result> results = new ArrayList<>();

    public void addResult(String scenarioName, long noteCount, long linkCount, long durationMs) {
        results.add(new Result(scenarioName, noteCount, linkCount, durationMs));
    }

    public String generateMarkdownTable() {
        StringBuilder sb = new StringBuilder();
        sb.append("\n# Performance Test Results\n\n");
        sb.append("| Scenario | Notes | Links | Duration (ms) | Throughput (ops/sec) |\n");
        sb.append("|----------|-------|-------|---------------|----------------------|\n");

        for (Result r : results) {
            long totalOps = r.noteCount + r.linkCount;
            double throughput = (double) totalOps / (r.durationMs / 1000.0);

            sb.append(String.format("| %-8s | %-5d | %-5d | %-13d | %-20.2f |\n",
                    r.scenarioName, r.noteCount, r.linkCount, r.durationMs, throughput));
        }
        sb.append("\n");
        return sb.toString();
    }

    private record Result(String scenarioName, long noteCount, long linkCount, long durationMs) {
    }
}
