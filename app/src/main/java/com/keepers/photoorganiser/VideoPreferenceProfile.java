package com.keepers.photoorganiser;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;

/** Conservative independent clip ranking; 0–100 expresses quality, not keeper probability. */
public final class VideoPreferenceProfile {
    private static final String[] LABELS = {"Useful detail", "Focus", "Exposure", "Composition",
            "Frame stability"};
    private final double[] weights;
    private final double strength;
    private final int count;

    private VideoPreferenceProfile(double[] weights, double strength, int count) {
        this.weights = weights;
        this.strength = strength;
        this.count = count;
    }

    public static VideoPreferenceProfile learn(List<VideoFeedback> evidence) {
        double[] positive = new double[5], negative = new double[5], weights = new double[5];
        int positives = 0, negatives = 0;
        // One current vote per clip; repeated imports must not multiply evidence.
        LinkedHashMap<String, VideoFeedback> unique = new LinkedHashMap<>();
        for (VideoFeedback item : evidence) unique.put(item.features().id(), item);
        for (VideoFeedback item : unique.values()) {
            boolean loved = item.rating() == VideoFeedback.LOVED;
            if (loved) positives++; else negatives++;
            double[] values = values(item.features());
            for (int i = 0; i < values.length; i++)
                (loved ? positive : negative)[i] += values[i];
        }
        int balanced = 2 * Math.min(positives, negatives);
        for (int i = 0; i < weights.length; i++)
            weights[i] = balanced == 0 ? 1 : Math.max(.1,
                    1 + 2 * (positive[i] / positives - negative[i] / negatives));
        return new VideoPreferenceProfile(weights, balanced / (balanced + 20d), unique.size());
    }

    public double score(VideoFeatures video) {
        double[] values = values(video);
        double weighted = 0, total = 0, baseline = 0;
        for (int i = 0; i < values.length; i++) {
            weighted += weights[i] * values[i];
            total += weights[i];
            baseline += values[i] / values.length;
        }
        return Math.max(0, Math.min(1, baseline * (1 - strength) + weighted / total * strength
                - video.blackFrameRate() * .35 - video.frozenFrameRate() * .08));
    }

    public Ranking ranking(VideoFeatures video) {
        double[] values = values(video);
        double total = java.util.Arrays.stream(weights).sum();
        int limiting = 0;
        double penalty = -1;
        for (int i = 0; i < values.length; i++) {
            double candidate = (1 - values[i]) * ((1 - strength) / 5 + strength * weights[i] / total);
            if (candidate > penalty) { penalty = candidate; limiting = i; }
        }
        String signal = LABELS[limiting];
        if (video.blackFrameRate() * .35 > penalty) {
            signal = "Visible frames";
            penalty = video.blackFrameRate() * .35;
        }
        if (video.frozenFrameRate() * .08 > penalty) signal = "Changing scene";
        String explanation = (strength == 0 ? "Technical score; more balanced video feedback is needed."
                : "Personalised using " + count + " video choices.")
                + " Main limiting signal: " + signal
                + ". This quality score is not a probability that you will keep the clip.";
        return new Ranking((int) Math.round(score(video) * 100), explanation);
    }

    /** Deterministic held-out evaluation by clip ID; no held-out labels enter training. */
    public static Evaluation evaluate(List<VideoFeedback> evidence) {
        LinkedHashMap<String, VideoFeedback> unique = new LinkedHashMap<>();
        for (VideoFeedback item : evidence) unique.put(item.features().id(), item);
        List<VideoFeedback> sorted = unique.values().stream()
                .sorted(Comparator.comparing(item -> item.features().id())).toList();
        ArrayList<VideoFeedback> training = new ArrayList<>(), heldOut = new ArrayList<>();
        for (int i = 0; i < sorted.size(); i++)
            (i % 5 == 0 ? heldOut : training).add(sorted.get(i));
        VideoPreferenceProfile learned = learn(training), baseline = learn(List.of());
        double learnedCorrect = 0, baselineCorrect = 0;
        int pairs = 0;
        for (VideoFeedback positive : heldOut) if (positive.rating() == VideoFeedback.LOVED)
            for (VideoFeedback negative : heldOut) if (negative.rating() != VideoFeedback.LOVED) {
                pairs++;
                learnedCorrect += accuracy(learned.score(positive.features()), learned.score(negative.features()));
                baselineCorrect += accuracy(baseline.score(positive.features()), baseline.score(negative.features()));
            }
        return new Evaluation(training.size(), heldOut.size(), pairs,
                pairs == 0 ? Double.NaN : baselineCorrect / pairs,
                pairs == 0 ? Double.NaN : learnedCorrect / pairs);
    }

    private static double accuracy(double positive, double negative) {
        return Math.abs(positive - negative) < 1e-9 ? .5 : positive > negative ? 1 : 0;
    }
    private static double[] values(VideoFeatures video) {
        return new double[]{video.detail(), video.focus(), video.exposure(), video.composition(),
                video.frameStability()};
    }
    public record Ranking(int score, String explanation) {}
    public record Evaluation(int trainingCount, int heldOutCount, int pairCount,
            double baselinePairAccuracy, double learnedPairAccuracy) {}
}
