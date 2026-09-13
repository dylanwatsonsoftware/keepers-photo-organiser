package com.keepers.photoorganiser;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

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
