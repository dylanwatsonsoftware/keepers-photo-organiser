package com.keepers.photoorganiser;

import static org.junit.Assert.assertEquals;
import org.junit.Test;

public class DragTransformTest {
    @Test public void horizontalDragMovesOnlyAcrossAndStaysOpaque() {
        DragTransform transform = DragTransform.from(120, 30, 600);
        assertEquals(120f, transform.x(), 0.001f);
        assertEquals(0f, transform.y(), 0.001f);
        assertEquals(1f, transform.alpha(), 0.001f);
    }

    @Test public void downwardDragMovesVerticallyAndFades() {
        DragTransform transform = DragTransform.from(20, 300, 600);
        assertEquals(0f, transform.x(), 0.001f);
        assertEquals(300f, transform.y(), 0.001f);
        assertEquals(0.75f, transform.alpha(), 0.001f);
    }

    @Test public void upwardDragDoesNotMoveThePhoto() {
        DragTransform transform = DragTransform.from(10, -200, 600);
        assertEquals(0f, transform.x(), 0.001f);
        assertEquals(0f, transform.y(), 0.001f);
    }
}
