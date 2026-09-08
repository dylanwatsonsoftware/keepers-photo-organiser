package com.keepers.photoorganiser;

import static org.junit.Assert.assertEquals;

import org.junit.Test;

public class GestureCoordinatesTest {
    @Test public void deltasStayAnchoredToTheScreenWhileTheTouchedViewMoves() {
        GestureCoordinates gesture = new GestureCoordinates(320f, 700f);

        assertEquals(-24f, gesture.deltaX(296f), 0f);
        assertEquals(-180f, gesture.deltaY(520f), 0f);
        assertEquals(-180f, gesture.deltaY(520f), 0f);
    }
}
