package com.keepers.photoorganiser;

public record FaceObservation(String photoId, int faceIndex, double left, double top,
        double right, double bottom, double smile, double leftEyeOpen, double rightEyeOpen,
        double yaw, double roll, String descriptor) {
    public FaceObservation(String photoId, int faceIndex, double left, double top,
            double right, double bottom, double smile, double leftEyeOpen, double rightEyeOpen,
            double yaw, double roll) {
        this(photoId, faceIndex, left, top, right, bottom, smile, leftEyeOpen, rightEyeOpen,
                yaw, roll, "");
    }
}
