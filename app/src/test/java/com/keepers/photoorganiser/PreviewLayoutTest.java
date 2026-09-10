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
import android.widget.HorizontalScrollView;
import android.widget.TextView;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.RuntimeEnvironment;

@RunWith(RobolectricTestRunner.class)
public class PreviewLayoutTest {
    @Test public void previewIncludesHiddenQuickReviewDecisionIndicators() {
        View layout = LayoutInflater.from(RuntimeEnvironment.getApplication())
                .inflate(R.layout.activity_preview, null);

        TextView keep = layout.findViewById(R.id.quick_review_keep_indicator);
        TextView pass = layout.findViewById(R.id.quick_review_pass_indicator);
        assertNotNull(keep);
        assertNotNull(pass);
        assertEquals(View.GONE, keep.getVisibility());
        assertEquals(View.GONE, pass.getVisibility());
        assertTrue(keep.getText().toString().contains("✓"));
        assertTrue(pass.getText().toString().contains("✕"));
        assertNotNull(keep.getBackground());
        assertNotNull(pass.getBackground());
    }
    @Test public void metadataSheetExplainsHorizontalPhotoNavigation() {
        View layout = LayoutInflater.from(RuntimeEnvironment.getApplication())
                .inflate(R.layout.activity_preview, null);

        TextView hint = layout.findViewById(R.id.preview_analysis_navigation_hint);
        assertNotNull(hint);
        assertTrue(hint.getText().toString().contains("Swipe left or right"));
    }
    @Test public void analysisOffersAStyledRecommendationFeedbackAction() {
        View layout = LayoutInflater.from(RuntimeEnvironment.getApplication())
                .inflate(R.layout.activity_preview, null);

        View feedback = layout.findViewById(R.id.preview_feedback);
        assertNotNull(feedback);
        assertTrue(feedback instanceof TextView);
        assertTrue(!(feedback instanceof android.widget.Button));
        assertTrue(feedback.isClickable());
        assertNotNull(feedback.getBackground());
    }
    @Test public void analysisBreakdownIsScrollable() {
        View layout = LayoutInflater.from(RuntimeEnvironment.getApplication())
                .inflate(R.layout.activity_preview, null);

        assertEquals(ScrollView.class, layout.findViewById(R.id.preview_analysis_sheet).getClass());
    }

    @Test public void analysisSheetStartsWithAFaceGrid() {
        View layout = LayoutInflater.from(RuntimeEnvironment.getApplication())
                .inflate(R.layout.activity_preview, null);
        int gridId = layout.getResources().getIdentifier(
                "preview_analysis_faces", "id", layout.getContext().getPackageName());

        assertTrue(gridId != 0);
        assertTrue(layout.findViewById(gridId) instanceof android.widget.GridLayout);
    }

    @Test public void analysisSheetIncludesAPhotoDetailsCard() {
        View layout = LayoutInflater.from(RuntimeEnvironment.getApplication())
                .inflate(R.layout.activity_preview, null);

        assertNotNull(layout.findViewById(R.id.preview_metadata_section));
        assertNotNull(layout.findViewById(R.id.preview_metadata_date));
        assertNotNull(layout.findViewById(R.id.preview_metadata_caption));
        assertNotNull(layout.findViewById(R.id.preview_metadata_location));
        assertNotNull(layout.findViewById(R.id.preview_metadata_technical));
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

    @Test public void photoViewIncludesBottomStackCarousel() {
        View layout = LayoutInflater.from(RuntimeEnvironment.getApplication())
                .inflate(R.layout.activity_preview, null);

        assertTrue(layout.findViewById(R.id.preview_stack_carousel)
                instanceof HorizontalScrollView);
        assertNotNull(layout.findViewById(R.id.preview_stack_thumbnails));
    }

    @Test public void recommendationUsesAFixedSlotSoCarouselNeverMoves() {
        View layout = LayoutInflater.from(RuntimeEnvironment.getApplication())
                .inflate(R.layout.activity_preview, null);
        View slot = layout.findViewById(R.id.preview_recommendation_slot);
        TextView recommendation = layout.findViewById(R.id.preview_recommendation);

        assertEquals(Math.round(36 * layout.getResources().getDisplayMetrics().density),
                slot.getLayoutParams().height);
        assertEquals(View.INVISIBLE, recommendation.getVisibility());
    }

    @Test public void fullscreenHintMakesPinchZoomDiscoverable() {
        View layout = LayoutInflater.from(RuntimeEnvironment.getApplication())
                .inflate(R.layout.activity_preview, null);
        TextView hint = layout.findViewById(R.id.preview_hint);

        assertTrue(hint.getText().toString().contains("Pinch to zoom"));
    }
}
