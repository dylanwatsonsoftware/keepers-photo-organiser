package com.keepers.photoorganiser;

import android.app.Activity;
import android.net.Uri;
import android.os.Bundle;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.HorizontalScrollView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.view.MotionEvent;
import android.view.View;
import java.util.ArrayList;
import java.util.List;

public final class PreviewActivity extends Activity {
    private AsyncThumbnailLoader loader;
    private KeeperSelectionStore store;
    private Uri photo;
    private PhotoNavigator navigator;
    private float touchStartX;
    private float touchStartY;
    private SuggestionStore suggestionStore;
    private ImageView frontImage;
    private ImageView adjacentImage;
    private FrameLayout currentSurface;
    private FrameLayout adjacentSurface;
    private CarouselPagePair pages;
    private View analysisSheet;
    private View previewControls;
    private View previewClose;
    private boolean analysisDragStarted;
    private boolean analysisWasOpen;
    private FrameLayout previewStage;
    private Uri dragPreviewPhoto;
    private List<Uri> allPhotos = List.of();
    private float analysisPullStartY;
    private boolean analysisPulling;

    @Override protected void onCreate(Bundle state) {
        super.onCreate(state);
        setContentView(R.layout.activity_preview);
        photo = getIntent().getData();
        store = new KeeperSelectionStore(this);
        suggestionStore = new SuggestionStore(this);
        loader = AsyncThumbnailLoader.forResolver(getContentResolver());
        int limit = getIntent().getIntExtra(ReviewActivity.EXTRA_REVIEW_LIMIT,
                ReviewWindow.PAGE_SIZE);
        ArrayList<Uri> photos = new ArrayList<>();
        for (RecentPhoto recent : RecentCameraQuery.loadRecent(getContentResolver(), limit)) {
            photos.add(recent.uri());
        }
        if (photos.isEmpty()) photos.add(photo);
        allPhotos = List.copyOf(photos);
        navigator = new PhotoNavigator(photos, photo);
        frontImage = findViewById(R.id.preview_image);
        adjacentImage = findViewById(R.id.preview_adjacent_image);
        currentSurface = findViewById(R.id.preview_current_surface);
        adjacentSurface = findViewById(R.id.preview_adjacent_surface);
        pages = new CarouselPagePair(currentSurface, frontImage, adjacentSurface, adjacentImage);
        analysisSheet = findViewById(R.id.preview_analysis_sheet);
        analysisSheet.setOnTouchListener((view, event) -> handleAnalysisScroll(event));
        previewStage = findViewById(R.id.preview_stage);
        previewControls = findViewById(R.id.preview_controls);
        previewClose = findViewById(R.id.preview_close);
        previewStage.setOnTouchListener((view, event) -> handleSwipe(event));
        findViewById(R.id.preview_close).setOnClickListener(view -> finish());
        loadCurrent();
        findViewById(R.id.preview_keeper).setOnClickListener(view -> {
            store.toggle(photo);
            updateButton();
        });
        updateButton();
    }

    private void updateButton() {
        ((TextView) findViewById(R.id.preview_keeper)).setText(store.load().contains(photo.toString())
                ? "♥ Keeper — tap to remove" : "♡ Mark as keeper");
    }

    private boolean handleAnalysisScroll(MotionEvent event) {
        if (event.getAction() == MotionEvent.ACTION_DOWN) {
            analysisPullStartY = event.getY();
            analysisPulling = false;
            return false;
        }
        float pull = event.getY() - analysisPullStartY;
        if (event.getAction() == MotionEvent.ACTION_MOVE) {
            if (analysisSheet.getScrollY() > 0 || pull <= dp(4)) return false;
            analysisPulling = true;
            AnalysisSheetTransform transform = AnalysisSheetTransform.fromOpenPull(pull,
                    previewStage.getHeight(), dp(72));
            setPhotoChromeTranslation(transform.photoTranslationY());
            analysisSheet.setTranslationY(transform.sheetTranslationY());
            return true;
        }
        if (!analysisPulling) return false;
        if (event.getAction() == MotionEvent.ACTION_UP) {
            if (AnalysisSheetTransform.shouldClose(pull, dp(64))) hideAnalysis();
            else openAnalysis();
            analysisPulling = false;
            return true;
        }
        if (event.getAction() == MotionEvent.ACTION_CANCEL) {
            openAnalysis();
            analysisPulling = false;
            return true;
        }
        return false;
    }

    private boolean handleSwipe(MotionEvent event) {
        View image = currentSurface;
        if (event.getAction() == MotionEvent.ACTION_DOWN) {
            image.animate().cancel();
            adjacentSurface.animate().cancel();
            adjacentSurface.setVisibility(View.INVISIBLE);
            dragPreviewPhoto = null;
            analysisDragStarted = false;
            analysisWasOpen = analysisSheet.getVisibility() == View.VISIBLE;
            touchStartX = event.getX();
            touchStartY = event.getY();
            // Once open, let the analysis ScrollView handle its long factor breakdown.
            return AnalysisGestureRouting.handleAsPhotoGesture(analysisWasOpen);
        }
        if (!AnalysisGestureRouting.handleAsPhotoGesture(analysisWasOpen)) return false;
        if (event.getAction() == MotionEvent.ACTION_MOVE) {
            float deltaX = event.getX() - touchStartX;
            float deltaY = event.getY() - touchStartY;
            if (!analysisWasOpen && deltaY < 0 && Math.abs(deltaY) > Math.abs(deltaX)
                    && Math.abs(deltaY) > dp(8)) {
                if (!analysisDragStarted) {
                    showAnalysis();
                    analysisDragStarted = true;
                }
                AnalysisSheetTransform sheet = AnalysisSheetTransform.from(deltaY,
                        previewStage.getHeight(), dp(72));
                setPhotoChromeTranslation(sheet.photoTranslationY());
                analysisSheet.setTranslationY(sheet.sheetTranslationY());
                return true;
            }
            if (Math.abs(deltaX) > Math.abs(deltaY) && Math.abs(deltaX) > dp(8)) {
                showDragPreview(deltaX < 0 ? navigator.peekNext() : navigator.peekPrevious());
                CarouselTransform carousel = CarouselTransform.from(deltaX,
                        previewStage.getWidth(), dp(8));
                image.setTranslationX(carousel.currentX());
                image.setTranslationY(0);
                image.setAlpha(1);
                adjacentSurface.setTranslationX(carousel.adjacentX());
                return true;
            }
            DragTransform drag = DragTransform.from(deltaX, deltaY, image.getHeight());
            image.setTranslationX(drag.x());
            image.setTranslationY(drag.y());
            image.setAlpha(drag.alpha());
            return true;
        }
        if (event.getAction() != MotionEvent.ACTION_UP) return true;
        SwipeDirection direction = SwipeDirection.classify(event.getX() - touchStartX,
                event.getY() - touchStartY, dp(64));
        if (direction == SwipeDirection.BACK) {
            if (analysisSheet.getVisibility() == View.VISIBLE) {
                hideAnalysis();
                resetPosition(image);
                return true;
            }
            adjacentSurface.setVisibility(View.INVISIBLE);
            image.animate().translationY(image.getHeight()).alpha(0.5f).setDuration(160)
                    .withEndAction(this::finish).start();
            return true;
        }
        if (direction == SwipeDirection.DETAILS) {
            resetPosition(image);
            showAnalysis();
            openAnalysis();
            return true;
        }
        if (direction == SwipeDirection.NONE) {
            if (analysisDragStarted) hideAnalysis();
            resetPosition(image);
            return true;
        }
        Uri target = direction == SwipeDirection.NEXT
                ? navigator.peekNext() : navigator.peekPrevious();
        if (target.equals(photo)) { resetPosition(image); return true; }
        if (adjacentSurface.getVisibility() != View.VISIBLE
                || !target.equals(dragPreviewPhoto)) {
            resetPosition(image);
            return true;
        }
        photo = direction == SwipeDirection.NEXT ? navigator.next() : navigator.previous();
        setIntent(PreviewPageRequest.forPhoto(getIntent(), photo));
        float pageDistance = previewStage.getWidth() + dp(8);
        float exit = direction == SwipeDirection.NEXT ? -pageDistance : pageDistance;
        image.animate().translationX(exit).setDuration(140).start();
        adjacentSurface.animate().translationX(0).setDuration(140)
                .withEndAction(this::promoteAdjacentPage).start();
        return true;
    }

    private void promoteAdjacentPage() {
        pages.promoteAdjacent();
        currentSurface = pages.currentSurface();
        frontImage = pages.currentImage();
        adjacentSurface = pages.adjacentSurface();
        adjacentImage = pages.adjacentImage();
        dragPreviewPhoto = null;
        updateRecommendation();
        updateButton();
        showStackCarousel();
        if (analysisSheet.getVisibility() == View.VISIBLE) showAnalysis();
    }

    private void loadCurrent() {
        int screen = Math.max(getResources().getDisplayMetrics().widthPixels,
                getResources().getDisplayMetrics().heightPixels);
        PreviewImageSizes sizes = PreviewImageSizes.forScreen(screen);
        loader.loadProgressive(frontImage, photo, sizes.previewPixels(), sizes.fullPixels(), bitmap -> {
            if (bitmap != null) frontImage.setVisibility(View.VISIBLE);
        });
        updateRecommendation();
        showStackCarousel();
    }

    private void updateRecommendation() {
        TextView recommendation = findViewById(R.id.preview_recommendation);
        boolean recommended = suggestionStore.load().contains(photo.toString());
        boolean alternative = suggestionStore.loadAlternatives().contains(photo.toString());
        recommendation.setText(recommended ? "★  Best shot"
                : alternative ? "☆  Good alternative" : "");
        recommendation.setVisibility(recommended || alternative ? View.VISIBLE : View.GONE);
    }

    private void showStackCarousel() {
        List<String> members = new PhotoStackStore(this).load(photo.toString());
        HorizontalScrollView carousel = findViewById(R.id.preview_stack_carousel);
        LinearLayout thumbnails = findViewById(R.id.preview_stack_thumbnails);
        thumbnails.removeAllViews();
        if (members.size() < 2) {
            carousel.setVisibility(View.GONE);
            return;
        }
        for (String member : members) {
            Uri memberUri = Uri.parse(member);
            ImageView thumbnail = new ImageView(this);
            thumbnail.setScaleType(ImageView.ScaleType.CENTER_CROP);
            thumbnail.setContentDescription(memberUri.equals(photo)
                    ? "Current photo in stack" : "Show photo from stack");
            thumbnail.setAlpha(memberUri.equals(photo) ? 1f : 0.72f);
            if (memberUri.equals(photo)) {
                thumbnail.setBackgroundResource(R.drawable.stack_thumbnail_selected);
                thumbnail.setPadding(dp(3), dp(3), dp(3), dp(3));
            }
            LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(dp(64), dp(64));
            params.setMargins(dp(4), dp(4), dp(4), dp(4));
            thumbnails.addView(thumbnail, params);
            loader.load(thumbnail, memberUri, 160);
            thumbnail.setOnClickListener(view -> selectStackPhoto(memberUri));
        }
        carousel.setVisibility(View.VISIBLE);
    }

    private void selectStackPhoto(Uri selected) {
        if (selected.equals(photo)) return;
        photo = selected;
        navigator = new PhotoNavigator(allPhotos, photo);
        setIntent(PreviewPageRequest.forPhoto(getIntent(), photo));
        loadCurrent();
        updateButton();
        if (analysisSheet.getVisibility() == View.VISIBLE) showAnalysis();
    }

    private void showAnalysis() {
        boolean opening = analysisSheet.getVisibility() != View.VISIBLE;
        PhotoInsight insight = new PhotoInsightStore(this).load(photo.toString());
        TextView title = findViewById(R.id.preview_analysis_title);
        TextView body = findViewById(R.id.preview_analysis_body);
        if (insight == null) {
            title.setText("Analysis pending");
            body.setText("This photo has not finished being analysed yet.");
        } else {
            title.setText(insight.recommended() ? "Recommended best shot"
                    : insight.goodAlternative() ? "Good alternative" : "Not recommended");
            String stack = insight.stack() == null ? "Distinct photo"
                    : "Photo " + insight.stack().position() + " of " + insight.stack().size()
                    + " in this detected stack";
            body.setText("Assessment " + insight.assessment().score() + "/100\n" + stack
                    + "\n" + insight.reason() + "\n\n" + insight.assessment().explanation());
        }
        if (opening) analysisSheet.setTranslationY(analysisRevealDistance());
        analysisSheet.setVisibility(View.VISIBLE);
    }

    private void openAnalysis() {
        float openPhotoY = -analysisRevealDistance();
        previewStage.animate().translationY(openPhotoY).setDuration(220).start();
        previewControls.animate().translationY(openPhotoY).setDuration(220).start();
        previewClose.animate().translationY(openPhotoY).setDuration(220).start();
        analysisSheet.animate().translationY(0).setDuration(220).start();
    }

    private void hideAnalysis() {
        previewStage.animate().translationY(0).setDuration(180).start();
        previewControls.animate().translationY(0).setDuration(180).start();
        previewClose.animate().translationY(0).setDuration(180).start();
        analysisSheet.animate().translationY(analysisRevealDistance()).setDuration(180)
                .withEndAction(() -> {
                    analysisSheet.setVisibility(View.GONE);
                    analysisSheet.setTranslationY(0);
                }).start();
    }

    private float analysisRevealDistance() {
        return Math.max(1, previewStage.getHeight() - dp(72));
    }

    private void setPhotoChromeTranslation(float translationY) {
        previewStage.setTranslationY(translationY);
        previewControls.setTranslationY(translationY);
        previewClose.setTranslationY(translationY);
    }

    private void showDragPreview(Uri target) {
        if (target.equals(photo)) {
            adjacentSurface.setVisibility(View.INVISIBLE);
            dragPreviewPhoto = null;
            return;
        }
        if (target.equals(dragPreviewPhoto)) return;
        dragPreviewPhoto = target;
        adjacentSurface.setVisibility(View.INVISIBLE);
        int screen = Math.max(getResources().getDisplayMetrics().widthPixels,
                getResources().getDisplayMetrics().heightPixels);
        PreviewImageSizes sizes = PreviewImageSizes.forScreen(screen);
        loader.loadProgressive(adjacentImage, target, sizes.previewPixels(), sizes.fullPixels(), bitmap -> {
            if (bitmap != null && target.equals(dragPreviewPhoto)) {
                adjacentSurface.setVisibility(View.VISIBLE);
            }
        });
    }

    private void resetPosition(View image) {
        float pageDistance = previewStage.getWidth() + dp(8);
        float adjacentRest = image.getTranslationX() < 0 ? pageDistance : -pageDistance;
        adjacentSurface.animate().translationX(adjacentRest).setDuration(140).start();
        image.animate().translationX(0).translationY(0).alpha(1).setDuration(140)
                .withEndAction(() -> adjacentSurface.setVisibility(View.INVISIBLE)).start();
    }

    private int dp(int value) {
        return Math.round(value * getResources().getDisplayMetrics().density);
    }

    @Override protected void onDestroy() {
        loader.close();
        super.onDestroy();
    }
}
