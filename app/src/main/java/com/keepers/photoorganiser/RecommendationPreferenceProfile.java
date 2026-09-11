package com.keepers.photoorganiser;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

public final class RecommendationPreferenceProfile {
    private static final double[] DEFAULT_WEIGHTS = { 1, 1, 1, 1, 1, 1.5, 1 };
    private final double[] weights;
    private final double strength;
    private final int feedbackCount;

    private RecommendationPreferenceProfile(double[] weights, double strength, int feedbackCount) {
        this.weights = weights;
        this.strength = strength;
        this.feedbackCount = feedbackCount;
    }

    public static RecommendationPreferenceProfile learn(List<RecommendationFeedback> feedback) {
        return learn(feedback, List.of(), List.of());
    }

    public static RecommendationPreferenceProfile learn(List<RecommendationFeedback> feedback,
            List<StackPreferenceComparison> comparisons) {
        return learn(feedback, comparisons, List.of());
    }

    public static RecommendationPreferenceProfile learn(List<RecommendationFeedback> feedback,
            List<StackPreferenceComparison> comparisons, List<PhotoFeatures> hiddenPhotos) {
        double[] loved = new double[7];
        double[] rejected = new double[7];
        double[] lovedSignalCounts = new double[7];
        double[] rejectedSignalCounts = new double[7];
        double evidenceCount = 0;
        Set<String> hiddenIds = new HashSet<>();
        for (PhotoFeatures hidden : hiddenPhotos) hiddenIds.add(hidden.id());
        for (StackPreferenceComparison comparison : comparisons) {
            if (hiddenIds.contains(comparison.preferred().id())
                    || hiddenIds.contains(comparison.alternative().id())) continue;
            addSignals(loved, lovedSignalCounts, comparison.preferred(), comparison.weight());
            addSignals(rejected, rejectedSignalCounts, comparison.alternative(),
                    comparison.weight());
            evidenceCount += comparison.weight();
        }
        double[] weights = new double[7];
        for (int index = 0; index < weights.length; index++) {
            double positiveMean = lovedSignalCounts[index] == 0
                    ? .5 : loved[index] / lovedSignalCounts[index];
            double negativeMean = rejectedSignalCounts[index] == 0
                    ? .5 : rejected[index] / rejectedSignalCounts[index];
            weights[index] = DEFAULT_WEIGHTS[index]
                    * Math.max(.05, 1 + 2 * (positiveMean - negativeMean));
        }
        double strength = evidenceCount / (evidenceCount + 5.0);
        return new RecommendationPreferenceProfile(weights, strength,
                (int) Math.round(evidenceCount));
    }

    public double score(PhotoFeatures photo) {
        double[] values = values(photo);
        double baseline = weightedAverage(values, DEFAULT_WEIGHTS);
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
                photo.motionStability(), photo.eyesOpen() < 0 ? Double.NaN : photo.eyesOpen(),
                photo.cameraFacing() < 0 ? Double.NaN : photo.cameraFacing() };
    }

    private static void addSignals(double[] totals, double[] counts, PhotoFeatures photo,
            double evidenceWeight) {
        double[] values = values(photo);
        for (int index = 0; index < values.length; index++) {
            if (Double.isNaN(values[index])) continue;
            totals[index] += values[index] * evidenceWeight;
            counts[index] += evidenceWeight;
        }
    }

    private static double weightedAverage(double[] values, double[] signalWeights) {
        double total = 0;
        double totalWeight = 0;
        for (int index = 0; index < values.length; index++) {
            if (Double.isNaN(values[index])) continue;
            total += values[index] * signalWeights[index];
            totalWeight += signalWeights[index];
        }
        return totalWeight == 0 ? 0 : total / totalWeight;
    }
}
