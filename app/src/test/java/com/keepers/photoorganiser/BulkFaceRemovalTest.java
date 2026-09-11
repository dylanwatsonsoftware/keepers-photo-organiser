package com.keepers.photoorganiser;

import static org.junit.Assert.assertEquals;

import java.util.Map;
import java.util.Set;
import org.junit.Test;

public class BulkFaceRemovalTest {
    @Test public void removesSelectedFacesWithoutChangingOtherConfirmedFaces() {
        Map<String, String> result = BulkFaceRemoval.apply("ada", Set.of("a#0", "b#0"),
                Map.of("a#0", "ada", "b#0", "ada", "c#0", "ada", "d#0", "ben"));

        assertEquals(FaceCorrectionStore.IGNORE, result.get("a#0"));
        assertEquals(FaceCorrectionStore.IGNORE, result.get("b#0"));
        assertEquals("ada", result.get("c#0"));
        assertEquals("ben", result.get("d#0"));
    }
}
