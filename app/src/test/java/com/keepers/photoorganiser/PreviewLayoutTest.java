package com.keepers.photoorganiser;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import android.view.LayoutInflater;
import android.view.Gravity;
import android.view.View;
import android.widget.ScrollView;
import android.view.ViewGroup;
import android.widget.FrameLayout;
import android.widget.ImageButton;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.RuntimeEnvironment;

@RunWith(RobolectricTestRunner.class)
public class PreviewLayoutTest {
    @Test public void analysisBreakdownIsScrollable() {
        View layout = LayoutInflater.from(RuntimeEnvironment.getApplication())
                .inflate(R.layout.activity_preview, null);

        assertEquals(ScrollView.class, layout.findViewById(R.id.preview_analysis_sheet).getClass());
    }
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

    @Test public void carouselUsesDedicatedPageSurfacesAroundItsImages() {
        View layout = LayoutInflater.from(RuntimeEnvironment.getApplication())
                .inflate(R.layout.activity_preview, null);
        int currentSurface = layout.getResources().getIdentifier(
                "preview_current_surface", "id", RuntimeEnvironment.getApplication().getPackageName());
        int adjacentSurface = layout.getResources().getIdentifier(
                "preview_adjacent_surface", "id", RuntimeEnvironment.getApplication().getPackageName());

        assertTrue(currentSurface != 0);
        assertTrue(adjacentSurface != 0);
        assertTrue(layout.findViewById(currentSurface) instanceof FrameLayout);
        assertTrue(layout.findViewById(adjacentSurface) instanceof FrameLayout);
    }

    @Test public void photoViewHasACompactCloseAction() {
        View layout = LayoutInflater.from(RuntimeEnvironment.getApplication())
                .inflate(R.layout.activity_preview, null);

        View close = layout.findViewById(R.id.preview_close);

        assertEquals(true, close instanceof ImageButton);
        assertEquals("Close photo", close.getContentDescription());
    }

    @Test public void photoViewIncludesAHiddenAnalysisSheet() {
        View layout = LayoutInflater.from(RuntimeEnvironment.getApplication())
                .inflate(R.layout.activity_preview, null);
        int sheetId = layout.getResources().getIdentifier(
                "preview_analysis_sheet", "id",
                RuntimeEnvironment.getApplication().getPackageName());

        assertTrue(sheetId != 0);
        assertEquals(View.GONE, layout.findViewById(sheetId).getVisibility());
    }
}
