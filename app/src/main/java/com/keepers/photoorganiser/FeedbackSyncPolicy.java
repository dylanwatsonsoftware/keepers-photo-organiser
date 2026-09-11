package com.keepers.photoorganiser;

import java.nio.charset.StandardCharsets;

public final class FeedbackSyncPolicy {
    static final int MAX_PAYLOAD_BYTES = 900_000;

    public enum Action {
        SKIP_DISABLED,
        WAIT_FOR_CONFIGURATION,
        SKIP_EMPTY,
        PAYLOAD_TOO_LARGE,
        UPLOAD
    }

    private FeedbackSyncPolicy() {}

    public static Action decide(boolean enabled, boolean configured,
            RecommendationFeedbackSnapshot snapshot) {
        if (!enabled) return Action.SKIP_DISABLED;
        if (!configured) return Action.WAIT_FOR_CONFIGURATION;
        if (snapshot.evidenceCount() == 0) return Action.SKIP_EMPTY;
        if (snapshot.json().getBytes(StandardCharsets.UTF_8).length > MAX_PAYLOAD_BYTES) {
            return Action.PAYLOAD_TOO_LARGE;
        }
        return Action.UPLOAD;
    }
}
