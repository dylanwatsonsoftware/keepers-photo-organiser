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
        return learn(feedback, List.of());
    }

    public static RecommendationPreferenceProfile learn(List<RecommendationFeedback> feedback,
            List<StackPreferenceComparison> comparisons) {
        double[] loved = new double[7];
        double[] rejected = new double[7];
        int[] lovedSignalCounts = new int[7];
        int[] rejectedSignalCounts = new int[7];
        int lovedCount = 0;
        int rejectedCount = 0;
        for (RecommendationFeedback item : feedback) {
            double[] target = item.rating() == RecommendationFeedback.LOVED ? loved : rejected;
            int[] signalCounts = item.rating() == RecommendationFeedback.LOVED
                    ? lovedSignalCounts : rejectedSignalCounts;
            if (item.rating() == RecommendationFeedback.LOVED) lovedCount++; else rejectedCount++;
            addSignals(target, signalCounts, item.features());
        }
        for (StackPreferenceComparison comparison : comparisons) {
            addSignals(loved, lovedSignalCounts, comparison.preferred());
            addSignals(rejected, rejectedSignalCounts, comparison.alternative());
            lovedCount++;
            rejectedCount++;
        }
        double[] weights = new double[7];
        for (int index = 0; index < weights.length; index++) {
            double positiveMean = lovedSignalCounts[index] == 0
                    ? .5 : loved[index] / lovedSignalCounts[index];
            double negativeMean = rejectedSignalCounts[index] == 0
                    ? .5 : rejected[index] / rejectedSignalCounts[index];
            weights[index] = Math.max(.05, 1 + 2 * (positiveMean - negativeMean));
        }
        int count = feedback.size() + comparisons.size();
        double strength = lovedCount == 0 || rejectedCount == 0 ? count / (count + 12.0)
                : count / (count + 5.0);
        return new RecommendationPreferenceProfile(weights, strength, count);
    }

    public double score(PhotoFeatures photo) {
        double[] values = values(photo);
        double baseline = averageAvailable(values);
        if (feedbackCount == 0) return baseline;
        double weighted = 0;
        double totalWeight = 0;
        for (int index = 0; index < weights.length; index++) {
            if (Double.isNaN(values[index])) continue;
            weighted += weights[index] * values[index];
            totalWeight += weights[index];
        }
        return baseline * (1 - strength) + weighted / totalWeight * strength;
    }

    public int feedbackCount() { return feedbackCount; }

    private static double[] values(PhotoFeatures photo) {
        return new double[] { photo.quality(), photo.focus(), photo.exposure(), photo.composition(),
                photo.motionStability(), photo.smile() < 0 ? Double.NaN : photo.smile(),
                photo.eyesOpen() < 0 ? Double.NaN : photo.eyesOpen() };
    }

    private static void addSignals(double[] totals, int[] counts, PhotoFeatures photo) {
        double[] values = values(photo);
        for (int index = 0; index < values.length; index++) {
            if (Double.isNaN(values[index])) continue;
            totals[index] += values[index];
            counts[index]++;
        }
    }

    private static double averageAvailable(double[] values) {
        double total = 0;
        int count = 0;
        for (double value : values) {
            if (Double.isNaN(value)) continue;
            total += value;
            count++;
        }
        return count == 0 ? 0 : total / count;
    }
}
