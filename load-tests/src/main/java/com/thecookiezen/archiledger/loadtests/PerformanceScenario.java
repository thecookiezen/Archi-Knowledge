package com.thecookiezen.archiledger.loadtests;

public record PerformanceScenario(
        String name,
        int entityCount,
        int relationsPerEntity,
        int batchSize) {
    public int totalRelations() {
        return entityCount * relationsPerEntity;
    }
}
