package com.keepers.photoorganiser;

import java.util.List;

public final class RecommendationPreferenceProfile {
    private final double[] weights;
    private final double strength;
    private final int feedbackCount;

    private RecommendationPreferenceProfile(double[] weights, double strength, int feedbackCount) {
        this.weights = weights;
        this.strength = strength;
        this.feedbackCount = feedbackCount;
    }

    public static RecommendationPreferenceProfile learn(List<RecommendationFeedback> feedback) {
        double[] loved = new double[7];
        double[] rejected = new double[7];
        int lovedCount = 0;
        int rejectedCount = 0;
        for (RecommendationFeedback item : feedback) {
            double[] values = values(item.features());
            double[] target = item.rating() == RecommendationFeedback.LOVED ? loved : rejected;
            if (item.rating() == RecommendationFeedback.LOVED) lovedCount++; else rejectedCount++;
            for (int index = 0; index < values.length; index++) target[index] += values[index];
        }
        double[] weights = new double[7];
        for (int index = 0; index < weights.length; index++) {
            double positiveMean = lovedCount == 0 ? .5 : loved[index] / lovedCount;
            double negativeMean = rejectedCount == 0 ? .5 : rejected[index] / rejectedCount;
            weights[index] = Math.max(.05, 1 + 2 * (positiveMean - negativeMean));
        }
        int count = feedback.size();
        double strength = lovedCount == 0 || rejectedCount == 0 ? count / (count + 12.0)
                : count / (count + 5.0);
        return new RecommendationPreferenceProfile(weights, strength, count);
    }

    public double score(PhotoFeatures photo) {
        if (feedbackCount == 0) return photo.quality();
        double weighted = 0;
        double totalWeight = 0;
        double[] values = values(photo);
        for (int index = 0; index < weights.length; index++) {
            weighted += weights[index] * values[index];
            totalWeight += weights[index];
        }
        return photo.quality() * (1 - strength) + weighted / totalWeight * strength;
    }

    public int feedbackCount() { return feedbackCount; }

    private static double[] values(PhotoFeatures photo) {
        return new double[] { photo.quality(), photo.focus(), photo.exposure(), photo.composition(),
                photo.motionStability(), photo.smile() < 0 ? .5 : photo.smile(),
                photo.eyesOpen() < 0 ? .5 : photo.eyesOpen() };
    }
}
