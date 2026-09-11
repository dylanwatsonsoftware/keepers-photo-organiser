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
import android.widget.ImageView;
import android.widget.HorizontalScrollView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.VideoView;
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
    @Test public void fullscreenOffersAStyledQuickReviewEntryPoint() {
        View layout = LayoutInflater.from(RuntimeEnvironment.getApplication())
                .inflate(R.layout.activity_preview, null);

        View quickReview = layout.findViewById(R.id.preview_start_quick_review);
        assertNotNull(quickReview);
        assertTrue(quickReview instanceof TextView);
        assertTrue(!(quickReview instanceof android.widget.Button));
        TextView label = (TextView) quickReview;
        assertEquals("Review", label.getText().toString());
        assertNotNull(label.getCompoundDrawablesRelative()[0]);
        assertTrue(quickReview.isClickable());
        assertNotNull(quickReview.getBackground());
        assertEquals("Start quick review", quickReview.getContentDescription());
        FrameLayout.LayoutParams params = (FrameLayout.LayoutParams) quickReview.getLayoutParams();
        assertEquals(Gravity.TOP, params.gravity & Gravity.TOP);
        assertEquals(Gravity.RIGHT, params.gravity & Gravity.RIGHT);
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
        assertTrue(layout.findViewById(R.id.preview_video) instanceof VideoView);
        assertTrue(layout.findViewById(R.id.preview_adjacent_video) instanceof VideoView);
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

    @Test public void hideSkipAndKeeperActionsShareTheDecisionRow() {
        View layout = LayoutInflater.from(RuntimeEnvironment.getApplication())
                .inflate(R.layout.activity_preview, null);
        View hide = layout.findViewById(R.id.preview_hide);
        View skip = layout.findViewById(R.id.preview_skip);
        View keeper = layout.findViewById(R.id.preview_keeper);
        LinearLayout.LayoutParams hideParams = (LinearLayout.LayoutParams) hide.getLayoutParams();
        LinearLayout.LayoutParams skipParams = (LinearLayout.LayoutParams) skip.getLayoutParams();
        LinearLayout.LayoutParams keeperParams = (LinearLayout.LayoutParams) keeper.getLayoutParams();

        assertTrue(hide instanceof FrameLayout);
        assertTrue(!(hide instanceof android.widget.Button));
        assertNotNull(hide.getBackground());
        LinearLayout hideContent = (LinearLayout) ((FrameLayout) hide).getChildAt(0);
        assertEquals(Gravity.CENTER, hideContent.getGravity());
        assertTrue(hideContent.getChildAt(0) instanceof ImageView);
        assertTrue(hideContent.getChildAt(1) instanceof TextView);
        assertEquals("Hide", ((TextView) hideContent.getChildAt(1)).getText().toString());
        assertTrue(skip instanceof TextView);
        assertTrue(!(skip instanceof android.widget.Button));
        assertNotNull(skip.getBackground());
        assertEquals(1f, hideParams.weight, 0f);
        assertEquals(1f, skipParams.weight, 0f);
        assertEquals(1f, keeperParams.weight, 0f);
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

    @Test public void photoViewHasACompactBackChevronAction() {
        View layout = LayoutInflater.from(RuntimeEnvironment.getApplication())
                .inflate(R.layout.activity_preview, null);

        View close = layout.findViewById(R.id.preview_close);

        assertEquals(true, close instanceof ImageButton);
        assertEquals("Back to gallery", close.getContentDescription());
        assertNotNull(((ImageButton) close).getDrawable());
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
