package com.keepers.photoorganiser;

import android.graphics.Bitmap;
import java.util.List;
import java.util.function.Consumer;

public interface FaceAnalyzer extends AutoCloseable {
    void analyze(String photoId, Bitmap bitmap, Consumer<List<FaceObservation>> result);
    @Override default void close() {}
}
