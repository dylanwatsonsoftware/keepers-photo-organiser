package com.keepers.photoorganiser;

import android.app.Activity;
import android.net.Uri;
import android.os.Bundle;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.TextView;
import android.view.MotionEvent;
import android.view.View;
import java.util.ArrayList;

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
    private FrameLayout previewStage;
    private Uri dragPreviewPhoto;

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
        navigator = new PhotoNavigator(photos, photo);
        frontImage = findViewById(R.id.preview_image);
        adjacentImage = findViewById(R.id.preview_adjacent_image);
        currentSurface = findViewById(R.id.preview_current_surface);
        adjacentSurface = findViewById(R.id.preview_adjacent_surface);
        pages = new CarouselPagePair(currentSurface, frontImage, adjacentSurface, adjacentImage);
        previewStage = findViewById(R.id.preview_stage);
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

    private boolean handleSwipe(MotionEvent event) {
        View image = currentSurface;
        if (event.getAction() == MotionEvent.ACTION_DOWN) {
            image.animate().cancel();
            adjacentSurface.animate().cancel();
            adjacentSurface.setVisibility(View.INVISIBLE);
            dragPreviewPhoto = null;
            touchStartX = event.getX();
            touchStartY = event.getY();
            return true;
        }
        if (event.getAction() == MotionEvent.ACTION_MOVE) {
            float deltaX = event.getX() - touchStartX;
            float deltaY = event.getY() - touchStartY;
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
            adjacentSurface.setVisibility(View.INVISIBLE);
            image.animate().translationY(image.getHeight()).alpha(0.5f).setDuration(160)
                    .withEndAction(this::finish).start();
            return true;
        }
        if (direction == SwipeDirection.NONE) { resetPosition(image); return true; }
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
    }

    private void loadCurrent() {
        int screen = Math.max(getResources().getDisplayMetrics().widthPixels,
                getResources().getDisplayMetrics().heightPixels);
        loader.load(frontImage, photo, Math.min(screen, 1600), bitmap -> {
            if (bitmap != null) frontImage.setVisibility(View.VISIBLE);
        });
        updateRecommendation();
    }

    private void updateRecommendation() {
        TextView recommendation = findViewById(R.id.preview_recommendation);
        boolean recommended = suggestionStore.load().contains(photo.toString());
        recommendation.setText(recommended ? "★  Best shot" : "");
        recommendation.setVisibility(recommended ? View.VISIBLE : View.GONE);
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
        loader.load(adjacentImage, target, Math.min(screen, 1600), bitmap -> {
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
