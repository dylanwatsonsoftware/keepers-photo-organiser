package com.keepers.photoorganiser;

public record RecommendationFeedback(PhotoFeatures features, int rating, String comment) {
    public static final int NOT_FOR_ME = -1;
    public static final int LOVED = 1;

    public RecommendationFeedback {
        if (rating != LOVED && rating != NOT_FOR_ME) throw new IllegalArgumentException("rating");
        comment = comment == null ? "" : comment.trim();
    }

    public static RecommendationFeedback from(PhotoFeatures features, int rating, String comment) {
        return new RecommendationFeedback(features, rating, comment);
    }
}
