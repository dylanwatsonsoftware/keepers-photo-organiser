package com.keepers.photoorganiser;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

import android.view.LayoutInflater;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
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

    @Test public void controlsFloatOverAFullHeightPhoto() {
        View layout = LayoutInflater.from(RuntimeEnvironment.getApplication())
                .inflate(R.layout.activity_preview, null);

        View stage = layout.findViewById(R.id.preview_stage);
        View controls = layout.findViewById(R.id.preview_controls);
        FrameLayout.LayoutParams controlsParams =
                (FrameLayout.LayoutParams) controls.getLayoutParams();

        assertEquals(ViewGroup.LayoutParams.MATCH_PARENT, stage.getLayoutParams().height);
        assertEquals(Gravity.BOTTOM, controlsParams.gravity & Gravity.BOTTOM);
        assertNotNull(layout.findViewById(R.id.preview_hint));
        assertNotNull(layout.findViewById(R.id.preview_recommendation).getBackground());
        assertNotNull(layout.findViewById(R.id.preview_keeper).getBackground());
    }
}
