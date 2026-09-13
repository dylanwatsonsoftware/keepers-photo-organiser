package com.keepers.photoorganiser;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.StringJoiner;

public final class GalleryMetadataOverlay {
    private GalleryMetadataOverlay() {}

    public static String topSignals(PhotoFeatures photo, int limit) {
        List<Signal> signals = new ArrayList<>(List.of(
                new Signal("Focus", photo.focus(), 0),
                new Signal("Exposure", photo.exposure(), 1),
                new Signal("Composition", photo.composition(), 2),
                new Signal("Motion", photo.motionStability(), 3),
                new Signal("Detail", photo.quality(), 4)));
        if (photo.eyesOpen() >= 0) signals.add(new Signal("Eyes open", photo.eyesOpen(), 5));
        if (photo.cameraFacing() >= 0)
            signals.add(new Signal("Facing camera", photo.cameraFacing(), 6));
        signals.sort(Comparator.comparingDouble(Signal::value).reversed()
                .thenComparingInt(Signal::order));
        return signals.stream().limit(Math.max(0, limit))
                .map(signal -> signal.label() + " "
                        + Math.round(Math.max(0, Math.min(1, signal.value())) * 100) + "%")
                .collect(java.util.stream.Collectors.joining("\n"));
    }

    public static String topSignals(PhotoFeatures photo, PhotoContext context, int limit) {
        return contextSummary(context) + "\n" + topSignals(photo, limit);
    }

    public static String rankingDetails(PhotoFeatures photo, PhotoContext context) {
        StringJoiner signals = new StringJoiner(" · ", "Ranking inputs · ", "");
        signals.add(percent("Detail", photo.quality()));
        signals.add(percent("Focus", photo.focus()));
        signals.add(percent("Exposure", photo.exposure()));
        signals.add(percent("Composition", photo.composition()));
        signals.add(percent("Motion", photo.motionStability()));
        if (photo.eyesOpen() >= 0) signals.add(percent("Eyes open", photo.eyesOpen()));
        if (photo.cameraFacing() >= 0)
            signals.add(percent("Facing camera", photo.cameraFacing()));
        return contextSummary(context) + "\n" + signals;
    }

    private static String contextSummary(PhotoContext context) {
        List<Map.Entry<PhotoContextType, Double>> types = context.probabilities().entrySet()
                .stream().filter(entry -> entry.getKey() != PhotoContextType.GENERAL
                        && entry.getValue() > 0)
                .sorted(Map.Entry.<PhotoContextType, Double>comparingByValue().reversed()
                        .thenComparing(entry -> entry.getKey().ordinal()))
                .toList();
        if (types.isEmpty()) return "Type · " + PhotoContextType.GENERAL.displayName();
        return (types.size() == 1 ? "Type · " : "Types · ") + types.stream()
                .map(entry -> percent(entry.getKey().displayName(), entry.getValue()))
                .collect(java.util.stream.Collectors.joining(" · "));
    }

    private static String percent(String label, double value) {
        return label + " " + Math.round(Math.max(0, Math.min(1, value)) * 100) + "%";
    }

    public static String topSignals(VideoFeatures video, int limit) {
        List<Signal> signals = new ArrayList<>(List.of(
                new Signal("Focus", video.focus(), 0),
                new Signal("Exposure", video.exposure(), 1),
                new Signal("Composition", video.composition(), 2),
                new Signal("Stability", video.frameStability(), 3),
                new Signal("Detail", video.detail(), 4)));
        signals.sort(Comparator.comparingDouble(Signal::value).reversed()
                .thenComparingInt(Signal::order));
        return signals.stream().limit(Math.max(0, limit))
                .map(signal -> signal.label() + " "
                        + Math.round(Math.max(0, Math.min(1, signal.value())) * 100) + "%")
                .collect(java.util.stream.Collectors.joining("\n"));
    }

    private record Signal(String label, double value, int order) {}
}
