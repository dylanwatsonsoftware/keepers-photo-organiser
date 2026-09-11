package com.keepers.photoorganiser;

public record RecommendationFeedbackSnapshot(String sourceId, long snapshotSequence,
        String appVersion, int evidenceCount, String json) {
    public RecommendationFeedbackSnapshot {
        if (sourceId == null || sourceId.isBlank()) throw new IllegalArgumentException("sourceId");
        if (snapshotSequence < 1) throw new IllegalArgumentException("snapshotSequence");
        if (appVersion == null) appVersion = "unknown";
        if (evidenceCount < 0) throw new IllegalArgumentException("evidenceCount");
        if (json == null || json.isBlank()) throw new IllegalArgumentException("json");
    }
}
