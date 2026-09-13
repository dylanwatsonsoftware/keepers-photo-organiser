package com.keepers.photoorganiser;

import java.util.List;

public record PhotoAnalysisSignals(List<FaceObservation> faces,
        List<ImageLabelSignal> labels) {
    public PhotoAnalysisSignals {
        faces = List.copyOf(faces);
        labels = List.copyOf(labels);
    }
}
