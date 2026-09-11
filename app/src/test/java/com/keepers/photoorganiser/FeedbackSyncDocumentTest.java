package com.keepers.photoorganiser;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.util.Map;
import org.junit.Test;

public class FeedbackSyncDocumentTest {
    @Test public void onlyNewerSnapshotsReplaceADeviceSnapshot() {
        assertTrue(FeedbackSyncDocument.shouldReplace(null, 1));
        assertTrue(FeedbackSyncDocument.shouldReplace(6L, 7));
        assertFalse(FeedbackSyncDocument.shouldReplace(7L, 7));
        assertFalse(FeedbackSyncDocument.shouldReplace(8L, 7));
    }

    @Test public void cloudDocumentKeepsOwnershipAndMergeMetadata() {
        RecommendationFeedbackSnapshot snapshot = new RecommendationFeedbackSnapshot(
                "anonymous-source", 4, "0.1-poc", 3, "{\"schemaVersion\":5}");

        Map<String, Object> fields = FeedbackSyncDocument.fields(snapshot, "anonymous-user", 99);

        assertEquals("anonymous-source", fields.get("sourceId"));
        assertEquals(4L, fields.get("snapshotSequence"));
        assertEquals("anonymous-user", fields.get("ownerUid"));
        assertEquals("latest_snapshot_per_source", fields.get("mergePolicy"));
        assertEquals("unknown", fields.get("missingEvidence"));
        assertEquals(99L, fields.get("clientUpdatedAtMillis"));
    }
}
