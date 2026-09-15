package com.keepers.photoorganiser;

import java.util.List;

public record FaceSignals(int faceCount, double averageSmile, double minimumEyeOpen,
        double minimumCameraFacing) {
    public static FaceSignals from(List<FaceObservation> observations) {
        if (observations.isEmpty()) return new FaceSignals(0, -1, -1, -1);
        double smiles = 0;
        int smileCount = 0;
        double minimumEye = 1;
        boolean hasEye = false;
        double minimumCameraFacing = 1;
        for (FaceObservation face : observations) {
            if (face.smile() >= 0) { smiles += face.smile(); smileCount++; }
            double eyeTotal = 0;
            int eyeCount = 0;
            if (face.leftEyeOpen() >= 0) { eyeTotal += face.leftEyeOpen(); eyeCount++; }
            if (face.rightEyeOpen() >= 0) { eyeTotal += face.rightEyeOpen(); eyeCount++; }
            if (eyeCount > 0) {
                minimumEye = Math.min(minimumEye, eyeTotal / eyeCount);
                hasEye = true;
            }
            minimumCameraFacing = Math.min(minimumCameraFacing,
                    Math.max(0, 1 - Math.abs(face.yaw()) / 45.0));
        }
        return new FaceSignals(observations.size(), smileCount == 0 ? -1 : smiles / smileCount,
                hasEye ? minimumEye : -1, minimumCameraFacing);
    }
}
