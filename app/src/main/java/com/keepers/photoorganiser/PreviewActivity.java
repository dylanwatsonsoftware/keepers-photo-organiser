package com.keepers.photoorganiser;

import android.app.Activity;
import android.net.Uri;
import android.os.Bundle;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;
import android.view.MotionEvent;
import java.util.ArrayList;

public final class PreviewActivity extends Activity {
    private AsyncThumbnailLoader loader;
    private KeeperSelectionStore store;
    private Uri photo;
    private PhotoNavigator navigator;
    private float touchStartX;
    private float touchStartY;
    private SuggestionStore suggestionStore;

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
        ImageView image = findViewById(R.id.preview_image);
        image.setOnTouchListener((view, event) -> handleSwipe(event));
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
        ImageView image = findViewById(R.id.preview_image);
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
        float exit = direction == SwipeDirection.NEXT ? -image.getWidth() : image.getWidth();
        float enter = -exit;
        image.animate().translationX(exit).alpha(0.7f).setDuration(140).withEndAction(() -> {
            image.setImageDrawable(null);
            image.setTranslationX(enter);
            image.setTranslationY(0);
            image.setAlpha(1);
            loadCurrent(() -> image.animate().translationX(0).setDuration(170).start());
        }).start();
        updateButton();
        return true;
    }

    private void loadCurrent() {
        loadCurrent(() -> {});
    }

    private void loadCurrent(Runnable loaded) {
        int screen = Math.max(getResources().getDisplayMetrics().widthPixels,
                getResources().getDisplayMetrics().heightPixels);
        loader.load((ImageView) findViewById(R.id.preview_image), photo, Math.min(screen, 1600),
                bitmap -> loaded.run());
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
