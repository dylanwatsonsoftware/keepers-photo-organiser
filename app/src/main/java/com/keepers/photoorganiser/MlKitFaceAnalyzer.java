package com.keepers.photoorganiser;

import android.graphics.Bitmap;
import android.graphics.Rect;
import com.google.mlkit.vision.common.InputImage;
import com.google.mlkit.vision.face.Face;
import com.google.mlkit.vision.face.FaceDetection;
import com.google.mlkit.vision.face.FaceDetector;
import com.google.mlkit.vision.face.FaceDetectorOptions;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

public final class MlKitFaceAnalyzer implements FaceAnalyzer {
    private final FaceDetector detector;

    public MlKitFaceAnalyzer() {
        FaceDetectorOptions options = new FaceDetectorOptions.Builder()
                .setPerformanceMode(FaceDetectorOptions.PERFORMANCE_MODE_ACCURATE)
                .setLandmarkMode(FaceDetectorOptions.LANDMARK_MODE_ALL)
                .setClassificationMode(FaceDetectorOptions.CLASSIFICATION_MODE_ALL)
                .setMinFaceSize(0.05f).build();
        detector = FaceDetection.getClient(options);
    }

    @Override public void analyze(String photoId, Bitmap bitmap,
            Consumer<List<FaceObservation>> result) {
        detector.process(InputImage.fromBitmap(bitmap, 0))
                .addOnSuccessListener(faces -> result.accept(map(photoId, bitmap, faces)))
                .addOnFailureListener(error -> result.accept(List.of()));
    }

    private static List<FaceObservation> map(String photoId, Bitmap bitmap, List<Face> faces) {
        ArrayList<FaceObservation> observations = new ArrayList<>();
        for (int index = 0; index < faces.size(); index++) {
            Face face = faces.get(index);
            Rect box = face.getBoundingBox();
            observations.add(new FaceObservation(photoId, index,
                    clamp(box.left / (double) bitmap.getWidth()),
                    clamp(box.top / (double) bitmap.getHeight()),
                    clamp(box.right / (double) bitmap.getWidth()),
                    clamp(box.bottom / (double) bitmap.getHeight()),
                    value(face.getSmilingProbability()), value(face.getLeftEyeOpenProbability()),
                    value(face.getRightEyeOpenProbability()), face.getHeadEulerAngleY(),
                    face.getHeadEulerAngleZ()));
        }
        return observations;
    }

    private static double value(Float value) { return value == null ? -1 : value; }
    private static double clamp(double value) { return Math.max(0, Math.min(1, value)); }
    @Override public void close() { detector.close(); }
}
