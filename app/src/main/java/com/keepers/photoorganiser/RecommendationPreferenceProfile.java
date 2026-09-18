package com.keepers.photoorganiser;

import java.util.ArrayList;
import java.util.EnumMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

public final class RecommendationPreferenceProfile {
    // Paired keeper choices consistently favour open eyes over modest sharpness gains.
    private static final double[] DEFAULT_WEIGHTS = { 1, 1, 1, 1, 1, 1.9, 1 };
    private static final String[] SIGNAL_LABELS = {
            "Detail", "Focus", "Exposure", "Composition", "Motion", "Eyes open",
            "Facing camera"
    };
    private final double[] weights;
    private final double strength;
    private final int feedbackCount;
    private final Map<String, PhotoContext> contexts;
    private final List<RecommendationFeedback> absoluteFeedback;
    private final Map<PhotoContextType, Double> contextBiases;
    private final boolean contextPreferencesLearned;

    private RecommendationPreferenceProfile(double[] weights, double strength, int feedbackCount) {
        this(weights, strength, feedbackCount, Map.of(), List.of(), Map.of(), false);
    }

    private RecommendationPreferenceProfile(double[] weights, double strength, int feedbackCount,
            Map<String, PhotoContext> contexts, List<RecommendationFeedback> absoluteFeedback,
            Map<PhotoContextType, Double> contextBiases, boolean contextPreferencesLearned) {
        this.weights = weights;
        this.strength = strength;
        this.feedbackCount = feedbackCount;
        this.contexts = Map.copyOf(contexts);
        this.absoluteFeedback = List.copyOf(absoluteFeedback);
        this.contextBiases = Map.copyOf(contextBiases);
        this.contextPreferencesLearned = contextPreferencesLearned;
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
        List<RecommendationFeedback> usableFeedback = new ArrayList<>();
        for (RecommendationFeedback item : feedback)
            if (!hiddenIds.contains(item.features().id())) usableFeedback.add(item);
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
                (int) Math.round(evidenceCount), Map.of(), usableFeedback, Map.of(), false);
    }

    public double score(PhotoFeatures photo) {
        double[] values = values(photo);
        PhotoContext context = contexts.getOrDefault(photo.id(), PhotoContext.general());
        double[] baselineWeights = contextualWeights(DEFAULT_WEIGHTS, context);
        double baseline = weightedAverage(values, baselineWeights);
        double technicalScore = baseline;
        if (feedbackCount > 0) {
            double[] contextualWeights = contextualWeights(weights, context);
            double weighted = 0;
            double totalWeight = 0;
            for (int index = 0; index < weights.length; index++) {
                if (Double.isNaN(values[index])) continue;
                weighted += contextualWeights[index] * values[index];
                totalWeight += contextualWeights[index];
            }
            technicalScore = baseline * (1 - strength) + weighted / totalWeight * strength;
        }
        return clamp(technicalScore + contextPreference(context));
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
        Map<PhotoContextType, Double> biases = contextPreferencesLearned
                ? contextBiases : learnContextBiases(absoluteFeedback, contexts);
        return new RecommendationPreferenceProfile(weights, strength, feedbackCount, contexts,
                absoluteFeedback, biases, true);
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

    private double contextPreference(PhotoContext context) {
        double preference = 0;
        for (Map.Entry<PhotoContextType, Double> entry : context.probabilities().entrySet())
            preference += entry.getValue() * contextBiases.getOrDefault(entry.getKey(), 0.0);
        return Math.max(-.06, Math.min(.06, preference));
    }

    private static Map<PhotoContextType, Double> learnContextBiases(
            List<RecommendationFeedback> feedback, Map<String, PhotoContext> contexts) {
        EnumMap<PhotoContextType, Double> lovedTotals = new EnumMap<>(PhotoContextType.class);
        EnumMap<PhotoContextType, Double> rejectedTotals = new EnumMap<>(PhotoContextType.class);
        int lovedCount = 0;
        int rejectedCount = 0;
        for (RecommendationFeedback item : feedback) {
            PhotoContext context = contexts.get(item.features().id());
            if (context == null) continue;
            boolean loved = item.rating() == RecommendationFeedback.LOVED;
            if (loved) lovedCount++; else rejectedCount++;
            Map<PhotoContextType, Double> totals = loved ? lovedTotals : rejectedTotals;
            for (PhotoContextType type : PhotoContextType.values()) {
                if (type == PhotoContextType.GENERAL || type == PhotoContextType.LOW_LIGHT)
                    continue;
                totals.merge(type, context.probability(type), Double::sum);
            }
        }
        if (lovedCount == 0 || rejectedCount == 0) return Map.of();
        double balancedEvidence = 2.0 * Math.min(lovedCount, rejectedCount);
        double evidenceStrength = balancedEvidence / (balancedEvidence + 20.0);
        EnumMap<PhotoContextType, Double> result = new EnumMap<>(PhotoContextType.class);
        for (PhotoContextType type : PhotoContextType.values()) {
            if (type == PhotoContextType.GENERAL || type == PhotoContextType.LOW_LIGHT) continue;
            double lovedMean = lovedTotals.getOrDefault(type, 0.0) / lovedCount;
            double rejectedMean = rejectedTotals.getOrDefault(type, 0.0) / rejectedCount;
            double bias = Math.max(-.06, Math.min(.06,
                    (lovedMean - rejectedMean) * .10 * evidenceStrength));
            if (Math.abs(bias) >= .001) result.put(type, bias);
        }
        return result;
    }

    private static double clamp(double value) {
        return Math.max(0, Math.min(1, value));
    }

    public record Ranking(int score, String limitingSignal, int limitingPercent) {}
}
