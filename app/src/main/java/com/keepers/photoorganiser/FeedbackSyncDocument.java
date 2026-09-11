package com.keepers.photoorganiser;

import java.util.LinkedHashMap;
import java.util.Map;

public final class FeedbackSyncDocument {
    private FeedbackSyncDocument() {}

    public static boolean shouldReplace(Long existingSequence, long incomingSequence) {
        return existingSequence == null || incomingSequence > existingSequence;
    }

    public static Map<String, Object> fields(RecommendationFeedbackSnapshot snapshot,
            String ownerUid, long clientUpdatedAtMillis) {
        LinkedHashMap<String, Object> fields = new LinkedHashMap<>();
        fields.put("sourceId", snapshot.sourceId());
        fields.put("snapshotSequence", snapshot.snapshotSequence());
        fields.put("appVersion", snapshot.appVersion());
        fields.put("evidenceCount", snapshot.evidenceCount());
        fields.put("schemaVersion", 5L);
        fields.put("kind", "snapshot");
        fields.put("mergePolicy", "latest_snapshot_per_source");
        fields.put("missingEvidence", "unknown");
        fields.put("ownerUid", ownerUid);
        fields.put("clientUpdatedAtMillis", clientUpdatedAtMillis);
        fields.put("payloadJson", snapshot.json());
        return Map.copyOf(fields);
    }
}
