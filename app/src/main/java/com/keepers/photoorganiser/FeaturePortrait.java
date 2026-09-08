package com.keepers.photoorganiser;

import android.graphics.Bitmap;
import java.util.List;
import java.util.Map;

final class FeaturePortrait {
    static final int PREVIEW_PIXELS = 640;
    static final int FULL_PIXELS = 1800;
    private static final double FACE_MARGIN = .90;

    private FeaturePortrait() {}

    static FaceObservation resolve(String personId, List<FaceObservation> faces,
            Map<String, String> featureKeys) {
        String featureKey = featureKeys.getOrDefault(personId, "");
        return faces.stream().filter(face -> FaceCorrectionStore.key(face).equals(featureKey))
                .findFirst().orElse(null);
    }

    static Bitmap crop(Bitmap bitmap, FaceObservation face) {
        if (bitmap == null) return null;
        double marginX = (face.right() - face.left()) * FACE_MARGIN;
        double marginY = (face.bottom() - face.top()) * FACE_MARGIN;
        int left = Math.max(0, (int) Math.round(
                (face.left() - marginX) * bitmap.getWidth()));
        int top = Math.max(0, (int) Math.round(
                (face.top() - marginY) * bitmap.getHeight()));
        int right = Math.min(bitmap.getWidth(), Math.max(left + 1,
                (int) Math.round((face.right() + marginX) * bitmap.getWidth())));
        int bottom = Math.min(bitmap.getHeight(), Math.max(top + 1,
                (int) Math.round((face.bottom() + marginY) * bitmap.getHeight())));
        return Bitmap.createBitmap(bitmap, left, top, right - left, bottom - top);
    }
}
