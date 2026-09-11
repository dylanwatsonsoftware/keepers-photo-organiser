package com.keepers.photoorganiser;

import java.util.List;

public final class FaceDisplayWindow {
    public record Result(List<FaceObservation> faces, boolean hasMore, int remaining) {}

    private FaceDisplayWindow() {}

    public static Result limit(List<FaceObservation> faces, int limit) {
        int shown = Math.min(Math.max(0, limit), faces.size());
        return new Result(List.copyOf(faces.subList(0, shown)), shown < faces.size(),
                faces.size() - shown);
    }
}
