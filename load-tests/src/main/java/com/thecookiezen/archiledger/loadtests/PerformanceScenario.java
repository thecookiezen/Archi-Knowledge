package com.thecookiezen.archiledger.loadtests;

public record PerformanceScenario(
        String name,
        int noteCount,
        int linksPerNote,
        int batchSize) {
    public int totalLinks() {
        return noteCount * linksPerNote;
    }
}
