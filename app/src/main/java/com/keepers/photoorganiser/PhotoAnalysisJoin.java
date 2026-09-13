package com.keepers.photoorganiser;

import java.util.List;
import java.util.function.Consumer;

public final class PhotoAnalysisJoin {
    private final Consumer<PhotoAnalysisSignals> completion;
    private List<FaceObservation> faces;
    private List<ImageLabelSignal> labels;
    private boolean delivered;

    public PhotoAnalysisJoin(Consumer<PhotoAnalysisSignals> completion) {
        this.completion = completion;
    }

    public synchronized void acceptFaces(List<FaceObservation> result) {
        if (delivered || faces != null) return;
        faces = List.copyOf(result);
        deliverIfComplete();
    }

    public synchronized void acceptLabels(List<ImageLabelSignal> result) {
        if (delivered || labels != null) return;
        labels = List.copyOf(result);
        deliverIfComplete();
    }

    private void deliverIfComplete() {
        if (faces == null || labels == null) return;
        delivered = true;
        completion.accept(new PhotoAnalysisSignals(faces, labels));
    }
}
