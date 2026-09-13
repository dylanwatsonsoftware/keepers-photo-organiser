package com.keepers.photoorganiser;

import static org.junit.Assert.assertEquals;

import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;
import org.junit.Test;

public class PhotoAnalysisJoinTest {
    @Test public void completesOnceAfterFacesAndLabelsArriveInEitherOrder() {
        AtomicInteger completions = new AtomicInteger();
        AtomicReference<PhotoAnalysisSignals> result = new AtomicReference<>();
        PhotoAnalysisJoin join = new PhotoAnalysisJoin(signals -> {
            completions.incrementAndGet();
            result.set(signals);
        });
        List<ImageLabelSignal> labels = List.of(new ImageLabelSignal("Dog", .9));

        join.acceptLabels(labels);
        assertEquals(0, completions.get());
        join.acceptFaces(List.of());
        join.acceptFaces(List.of());

        assertEquals(1, completions.get());
        assertEquals(labels, result.get().labels());
        assertEquals(List.of(), result.get().faces());
    }
}
