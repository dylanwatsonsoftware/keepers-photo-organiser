package com.keepers.photoorganiser;

import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.Map;

public final class RecommendationPreferenceProfile {
    private static final double[] DEFAULT_WEIGHTS = { 1, 1, 1, 1, 1, 1.5, 1 };
    private static final String[] SIGNAL_LABELS = {
            "Detail", "Focus", "Exposure", "Composition", "Motion", "Eyes open",
            "Facing camera"
    };
    private final double[] weights;
    private final double strength;
    private final int feedbackCount;
    private final Map<String, PhotoContext> contexts;

    private RecommendationPreferenceProfile(double[] weights, double strength, int feedbackCount) {
        this(weights, strength, feedbackCount, Map.of());
    }

    private RecommendationPreferenceProfile(double[] weights, double strength, int feedbackCount,
            Map<String, PhotoContext> contexts) {
        this.weights = weights;
        this.strength = strength;
        this.feedbackCount = feedbackCount;
        this.contexts = Map.copyOf(contexts);
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
        PhotoContext context = contexts.getOrDefault(photo.id(), PhotoContext.general());
        double[] baselineWeights = contextualWeights(DEFAULT_WEIGHTS, context);
        double baseline = weightedAverage(values, baselineWeights);
        if (feedbackCount == 0) return baseline;
        double[] contextualWeights = contextualWeights(weights, context);
        double weighted = 0;
        double totalWeight = 0;
        for (int index = 0; index < weights.length; index++) {
            if (Double.isNaN(values[index])) continue;
            weighted += contextualWeights[index] * values[index];
            totalWeight += contextualWeights[index];
        }
        return baseline * (1 - strength) + weighted / totalWeight * strength;
    }

    public Ranking ranking(PhotoFeatures photo) {
        double[] values = values(photo);
        PhotoContext context = contexts.getOrDefault(photo.id(), PhotoContext.general());
        double[] baselineWeights = contextualWeights(DEFAULT_WEIGHTS, context);
        double baselineTotal = availableWeight(values, baselineWeights);
        double[] learnedWeights = contextualWeights(weights, context);
        double learnedTotal = availableWeight(values, learnedWeights);
        int limiting = -1;
        double largestPenalty = -1;
        for (int index = 0; index < values.length; index++) {
            if (Double.isNaN(values[index])) continue;
            double influence = baselineTotal == 0 ? 0
                    : (1 - strength) * baselineWeights[index] / baselineTotal;
            if (feedbackCount > 0 && learnedTotal > 0)
                influence += strength * learnedWeights[index] / learnedTotal;
            double penalty = influence * (1 - values[index]);
            if (penalty > largestPenalty) {
                largestPenalty = penalty;
                limiting = index;
            }
        }
        return new Ranking(percent(score(photo)), limiting < 0 ? "None" : SIGNAL_LABELS[limiting],
                limiting < 0 ? 0 : percent(values[limiting]));
    }

    public RecommendationPreferenceProfile withContexts(Map<String, PhotoContext> contexts) {
        return new RecommendationPreferenceProfile(weights, strength, feedbackCount, contexts);
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

    private static double availableWeight(double[] values, double[] signalWeights) {
        double total = 0;
        for (int index = 0; index < values.length; index++)
            if (!Double.isNaN(values[index])) total += signalWeights[index];
        return total;
    }

    private static int percent(double value) {
        return (int) Math.round(Math.max(0, Math.min(1, value)) * 100);
    }

    private static double[] contextualWeights(double[] base, PhotoContext context) {
        double[] adjusted = new double[base.length];
        for (int index = 0; index < base.length; index++)
            adjusted[index] = base[index] * context.signalMultiplier(index);
        return adjusted;
    }

    public record Ranking(int score, String limitingSignal, int limitingPercent) {}
}
