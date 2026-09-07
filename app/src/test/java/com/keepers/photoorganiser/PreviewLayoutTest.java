package com.keepers.photoorganiser;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

import android.view.LayoutInflater;
import android.view.View;
import android.widget.FrameLayout;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.RuntimeEnvironment;

@RunWith(RobolectricTestRunner.class)
public class PreviewLayoutTest {
    @Test public void previewHasCurrentAndDragRevealSurfaces() {
        View layout = LayoutInflater.from(RuntimeEnvironment.getApplication())
                .inflate(R.layout.activity_preview, null);

        FrameLayout stage = layout.findViewById(R.id.preview_stage);

        assertNotNull(stage);
        assertEquals(2, stage.getChildCount());
        assertNotNull(layout.findViewById(R.id.preview_image));
        assertNotNull(layout.findViewById(R.id.preview_adjacent_image));
    }
}
