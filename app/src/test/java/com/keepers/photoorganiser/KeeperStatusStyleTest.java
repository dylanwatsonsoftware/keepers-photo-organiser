package com.keepers.photoorganiser;

import static org.junit.Assert.assertEquals;

import android.graphics.Color;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;

@RunWith(RobolectricTestRunner.class)
public class KeeperStatusStyleTest {
    @Test public void newAndSavedKeepersUseDistinctColours() {
        assertEquals(Color.rgb(234, 67, 53), KeeperStatusStyle.heartColor(false));
        assertEquals(Color.rgb(66, 133, 244), KeeperStatusStyle.heartColor(true));
    }
}
