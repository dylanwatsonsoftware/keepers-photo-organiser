package com.keepers.photoorganiser;

import static org.junit.Assert.assertEquals;

import java.util.List;
import org.junit.Test;

public class FaceSignalsTest {
    @Test public void summarizesExpressionsAcrossEveryDetectedFace() {
        FaceSignals signals = FaceSignals.from(List.of(
                observation(0.9, 0.8, 0.7), observation(0.5, 0.95, 0.9)));

        assertEquals(2, signals.faceCount());
        assertEquals(0.7, signals.averageSmile(), 0.001);
        assertEquals(0.7, signals.minimumEyeOpen(), 0.001);
    }

    @Test public void cameraFacingUsesTheLeastForwardFacingPerson() {
        FaceSignals signals = FaceSignals.from(List.of(
                observation(0.9, 0.8, 0.7, 0),
                observation(0.5, 0.95, 0.9, 30)));

        assertEquals(1.0 / 3.0, signals.minimumCameraFacing(), 0.001);
    }

    private static FaceObservation observation(double smile, double leftEye, double rightEye) {
        return observation(smile, leftEye, rightEye, 0);
    }

    private static FaceObservation observation(double smile, double leftEye, double rightEye,
            double yaw) {
        return new FaceObservation("photo", 0, 0.1, 0.1, 0.3, 0.4,
                smile, leftEye, rightEye, yaw, 0);
    }
}
