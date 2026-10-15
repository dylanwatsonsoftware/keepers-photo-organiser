package com.keepers.photoorganiser;

/** Video evidence never enters the still-photo preference model. Comments stay on device. */
public record VideoFeedback(VideoFeatures features, int rating, String comment) {
    public static final int LOVED = 1;
    public static final int NOT_FOR_ME = -1;
    public static final int HIDDEN = -2;

    public VideoFeedback {
        if (features == null || features.schemaVersion() != VideoFeatures.SCHEMA_VERSION)
            throw new IllegalArgumentException("video schema");
        if (rating != LOVED && rating != NOT_FOR_ME && rating != HIDDEN)
            throw new IllegalArgumentException("rating");
        double[] signals = {features.detail(), features.focus(), features.exposure(),
                features.composition(), features.frameStability(), features.blackFrameRate(),
                features.frozenFrameRate()};
        for (double value : signals)
            if (!Double.isFinite(value) || value < 0 || value > 1)
                throw new IllegalArgumentException("signal");
        comment = comment == null ? "" : comment.trim();
    }
}
