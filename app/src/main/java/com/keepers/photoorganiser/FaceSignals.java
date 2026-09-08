package com.keepers.photoorganiser;

import java.util.List;

public record FaceSignals(int faceCount, double averageSmile, double minimumEyeOpen) {
    public static FaceSignals from(List<FaceObservation> observations) {
        if (observations.isEmpty()) return new FaceSignals(0, -1, -1);
        double smiles = 0;
        int smileCount = 0;
        double minimumEye = 1;
        boolean hasEye = false;
        for (FaceObservation face : observations) {
            if (face.smile() >= 0) { smiles += face.smile(); smileCount++; }
            if (face.leftEyeOpen() >= 0) { minimumEye = Math.min(minimumEye, face.leftEyeOpen()); hasEye = true; }
            if (face.rightEyeOpen() >= 0) { minimumEye = Math.min(minimumEye, face.rightEyeOpen()); hasEye = true; }
        }
        return new FaceSignals(observations.size(), smileCount == 0 ? -1 : smiles / smileCount,
                hasEye ? minimumEye : -1);
    }
}
