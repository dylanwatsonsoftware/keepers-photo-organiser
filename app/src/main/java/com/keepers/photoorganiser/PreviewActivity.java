package com.keepers.photoorganiser;

import android.app.Activity;
import android.net.Uri;
import android.os.Bundle;
import android.util.Log;
import android.widget.Button;
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
    private ImageView backImage;

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
        backImage = findViewById(R.id.preview_incoming_image);
        FrameLayout stage = findViewById(R.id.preview_stage);
        stage.setOnTouchListener((view, event) -> handleSwipe(event));
        loadCurrent();
        findViewById(R.id.preview_keeper).setOnClickListener(view -> {
            store.toggle(photo);
            updateButton();
        });
        updateButton();
    }

    private void updateButton() {
        ((Button) findViewById(R.id.preview_keeper)).setText(store.load().contains(photo.toString())
                ? "♥ Keeper — tap to remove" : "♡ Mark as keeper");
    }

    private boolean handleSwipe(MotionEvent event) {
        ImageView image = frontImage;
        if (event.getAction() == MotionEvent.ACTION_DOWN) {
            image.animate().cancel();
            touchStartX = event.getX();
            touchStartY = event.getY();
            return true;
        }
        if (event.getAction() == MotionEvent.ACTION_MOVE) {
            DragTransform drag = DragTransform.from(event.getX() - touchStartX,
                    event.getY() - touchStartY, image.getHeight());
            image.setTranslationX(drag.x());
            image.setTranslationY(drag.y());
            image.setAlpha(drag.alpha());
            return true;
        }
        if (event.getAction() != MotionEvent.ACTION_UP) return true;
        SwipeDirection direction = SwipeDirection.classify(event.getX() - touchStartX,
                event.getY() - touchStartY, dp(64));
        if (direction == SwipeDirection.BACK) {
            image.animate().translationY(image.getHeight()).alpha(0.5f).setDuration(160)
                    .withEndAction(this::finish).start();
            return true;
        }
        if (direction == SwipeDirection.NONE) { resetPosition(image); return true; }
        Uri before = photo;
        photo = direction == SwipeDirection.NEXT ? navigator.next() : navigator.previous();
        if (photo.equals(before)) { resetPosition(image); return true; }
        image.animate().cancel();
        image.setTranslationX(0);
        image.setTranslationY(0);
        image.setAlpha(1);
        loadCurrentKeepingVisiblePhoto();
        updateButton();
        return true;
    }

    private void loadCurrent() {
        int screen = Math.max(getResources().getDisplayMetrics().widthPixels,
                getResources().getDisplayMetrics().heightPixels);
        loader.load(frontImage, photo, Math.min(screen, 1600), bitmap -> {
            if (bitmap != null) frontImage.setVisibility(View.VISIBLE);
        });
        updateRecommendation();
    }

    private void loadCurrentKeepingVisiblePhoto() {
        Uri requested = photo;
        ImageView outgoing = frontImage;
        ImageView incoming = backImage;
        incoming.setVisibility(View.INVISIBLE);
        int screen = Math.max(getResources().getDisplayMetrics().widthPixels,
                getResources().getDisplayMetrics().heightPixels);
        loader.load(incoming, requested, Math.min(screen, 1600), bitmap -> {
            Log.d("KeepersPreview", "adjacent uri=" + requested + " bitmap="
                    + (bitmap == null ? "null" : bitmap.getWidth() + "x" + bitmap.getHeight())
                    + " current=" + requested.equals(photo) + " incomingVisibility="
                    + incoming.getVisibility() + " incomingAlpha=" + incoming.getAlpha()
                    + " incomingX=" + incoming.getTranslationX());
            if (bitmap == null || !requested.equals(photo)) return;
            incoming.setTranslationX(0);
            incoming.setTranslationY(0);
            incoming.setAlpha(1);
            incoming.setVisibility(View.VISIBLE);
            incoming.bringToFront();
            outgoing.setVisibility(View.INVISIBLE);
            frontImage = incoming;
            backImage = outgoing;
            Log.d("KeepersPreview", "swapped drawable=" + (frontImage.getDrawable() != null)
                    + " visibility=" + frontImage.getVisibility() + " alpha="
                    + frontImage.getAlpha() + " x=" + frontImage.getTranslationX());
        });
        updateRecommendation();
    }

    private void updateRecommendation() {
        ((TextView) findViewById(R.id.preview_recommendation)).setText(
                suggestionStore.load().contains(photo.toString()) ? "★ Recommended best shot" : "");
    }

    private void resetPosition(ImageView image) {
        image.animate().translationX(0).translationY(0).alpha(1).setDuration(140).start();
    }

    private int dp(int value) {
        return Math.round(value * getResources().getDisplayMetrics().density);
    }

    @Override protected void onDestroy() {
        loader.close();
        super.onDestroy();
    }
}
