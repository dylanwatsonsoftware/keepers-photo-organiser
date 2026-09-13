package com.keepers.photoorganiser;

import android.graphics.Bitmap;
import com.google.mlkit.vision.common.InputImage;
import com.google.mlkit.vision.label.ImageLabeler;
import com.google.mlkit.vision.label.ImageLabeling;
import com.google.mlkit.vision.label.defaults.ImageLabelerOptions;
import java.util.ArrayDeque;
import java.util.List;
import java.util.function.Consumer;

public final class MlKitPhotoContextAnalyzer implements PhotoContextAnalyzer {
    private final ImageLabeler labeler;
    private final ArrayDeque<Request> pending = new ArrayDeque<>();
    private boolean running;

    public MlKitPhotoContextAnalyzer() {
        labeler = ImageLabeling.getClient(new ImageLabelerOptions.Builder()
                .setConfidenceThreshold(.5f).build());
    }

    @Override public synchronized void analyze(Bitmap bitmap,
            Consumer<List<ImageLabelSignal>> result) {
        pending.addLast(new Request(bitmap, result));
        startNext();
    }

    private synchronized void startNext() {
        if (running) return;
        Request request = pending.pollFirst();
        if (request == null) return;
        running = true;
        labeler.process(InputImage.fromBitmap(request.bitmap(), 0))
                .addOnSuccessListener(labels -> complete(request, labels.stream()
                        .map(label -> new ImageLabelSignal(label.getText(), label.getConfidence()))
                        .toList()))
                .addOnFailureListener(error -> complete(request, List.of()));
    }

    private void complete(Request request, List<ImageLabelSignal> labels) {
        request.result().accept(labels);
        synchronized (this) {
            running = false;
            startNext();
        }
    }

    @Override public synchronized void close() {
        pending.clear();
        labeler.close();
    }

    private record Request(Bitmap bitmap, Consumer<List<ImageLabelSignal>> result) {}
}
