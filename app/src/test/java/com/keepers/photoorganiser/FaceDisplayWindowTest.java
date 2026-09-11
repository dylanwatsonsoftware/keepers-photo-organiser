package com.keepers.photoorganiser;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.util.stream.IntStream;
import org.junit.Test;

public class FaceDisplayWindowTest {
    @Test public void limitsLargePeopleAndReportsMoreFaces() {
        var faces = IntStream.range(0, 75).mapToObj(index -> new FaceObservation(
                "photo-" + index, 0, 0, 0, 1, 1, -1, -1, -1, 0, 0, ""))
                .toList();

        FaceDisplayWindow.Result result = FaceDisplayWindow.limit(faces, 40);

        assertEquals(40, result.faces().size());
        assertTrue(result.hasMore());
        assertEquals(35, result.remaining());
    }
}
