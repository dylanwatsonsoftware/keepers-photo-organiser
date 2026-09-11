package com.keepers.photoorganiser;

import static org.junit.Assert.assertEquals;

import org.junit.Test;

public class FeedbackSyncPolicyTest {
    private static RecommendationFeedbackSnapshot snapshot(int evidenceCount, int jsonBytes) {
        return new RecommendationFeedbackSnapshot("source", 1, "0.1-poc", evidenceCount,
                "x".repeat(jsonBytes));
    }

    @Test public void requiresConsentConfigurationAndEvidenceBeforeUploading() {
        assertEquals(FeedbackSyncPolicy.Action.SKIP_DISABLED,
                FeedbackSyncPolicy.decide(false, true, snapshot(1, 10)));
        assertEquals(FeedbackSyncPolicy.Action.WAIT_FOR_CONFIGURATION,
                FeedbackSyncPolicy.decide(true, false, snapshot(1, 10)));
        assertEquals(FeedbackSyncPolicy.Action.SKIP_EMPTY,
                FeedbackSyncPolicy.decide(true, true, snapshot(0, 10)));
        assertEquals(FeedbackSyncPolicy.Action.UPLOAD,
                FeedbackSyncPolicy.decide(true, true, snapshot(1, 10)));
    }

    @Test public void keepsFirestoreDocumentsBelowTheServiceLimit() {
        assertEquals(FeedbackSyncPolicy.Action.PAYLOAD_TOO_LARGE,
                FeedbackSyncPolicy.decide(true, true, snapshot(1, 900_001)));
    }
}
