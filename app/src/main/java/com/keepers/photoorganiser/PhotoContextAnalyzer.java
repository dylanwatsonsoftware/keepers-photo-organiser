package com.keepers.photoorganiser;

import android.graphics.Bitmap;
import java.util.List;
import java.util.function.Consumer;

public interface PhotoContextAnalyzer extends AutoCloseable {
    void analyze(Bitmap bitmap, Consumer<List<ImageLabelSignal>> result);
    @Override default void close() {}
}
