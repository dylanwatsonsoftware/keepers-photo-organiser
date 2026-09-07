package com.keepers.photoorganiser;

import android.Manifest;
import android.app.Activity;
import android.content.pm.PackageManager;
import android.content.Intent;
import android.graphics.Color;
import android.graphics.drawable.GradientDrawable;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.view.Gravity;
import android.view.View;
import android.widget.FrameLayout;
import android.widget.GridLayout;
import android.widget.ImageView;
import android.widget.ScrollView;
import android.widget.TextView;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.ArrayList;

public final class ReviewActivity extends Activity {
    public static final String EXTRA_REVIEW_LIMIT = "review_limit";
    private static final int PHOTO_PERMISSION = 200;
    private KeeperSelectionStore selectionStore;
    private AsyncThumbnailLoader thumbnailLoader;
    private List<Uri> photos = List.of();
    private final List<PhotoFeatures> features = new ArrayList<>();
    private Set<String> suggestions = Set.of();
    private int analyzedCount;
    private int analysisGeneration;
    private final ReviewWindow reviewWindow = new ReviewWindow();
    private final InfiniteScrollTrigger infiniteScroll = new InfiniteScrollTrigger(600);
    private boolean hasMorePhotos;

    @Override protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_review);
        selectionStore = new KeeperSelectionStore(this);
        thumbnailLoader = AsyncThumbnailLoader.forResolver(getContentResolver());
        findViewById(R.id.open_settings).setOnClickListener(view ->
                startActivity(new Intent(this, MainActivity.class)));
        findViewById(R.id.clear_keepers).setOnClickListener(view -> {
            selectionStore.clear();
            updateSelectionDisplay();
        });
        ScrollView scroll = findViewById(R.id.review_scroll);
        scroll.setOnScrollChangeListener((view, scrollX, scrollY, oldScrollX, oldScrollY) -> {
            View content = scroll.getChildAt(0);
            if (content != null && infiniteScroll.onScroll(scrollY, scroll.getHeight(),
                    content.getHeight(), hasMorePhotos)) loadNextPage();
        });
        loadOrRequestPhotos();
    }

    private void loadOrRequestPhotos() {
        if (hasLocalPhotoAccess()) {
            loadRecentPhotos();
            return;
        }
        if (Build.VERSION.SDK_INT >= 34) {
            requestPermissions(new String[]{Manifest.permission.READ_MEDIA_IMAGES,
                    Manifest.permission.READ_MEDIA_VISUAL_USER_SELECTED}, PHOTO_PERMISSION);
        } else if (Build.VERSION.SDK_INT >= 33) {
            requestPermissions(new String[]{Manifest.permission.READ_MEDIA_IMAGES}, PHOTO_PERMISSION);
        } else {
            requestPermissions(new String[]{Manifest.permission.READ_EXTERNAL_STORAGE}, PHOTO_PERMISSION);
        }
    }

    @Override public void onRequestPermissionsResult(int requestCode, String[] permissions,
            int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == PHOTO_PERMISSION && hasLocalPhotoAccess()) {
            loadRecentPhotos();
        } else if (requestCode == PHOTO_PERMISSION) {
            ((TextView) findViewById(R.id.review_empty)).setText(
                    "Photo access is needed to review recent Pixel camera images.");
        }
    }

    private boolean hasLocalPhotoAccess() {
        if (Build.VERSION.SDK_INT >= 34 && checkSelfPermission(
                Manifest.permission.READ_MEDIA_VISUAL_USER_SELECTED)
                == PackageManager.PERMISSION_GRANTED) return true;
        String permission = Build.VERSION.SDK_INT >= 33
                ? Manifest.permission.READ_MEDIA_IMAGES : Manifest.permission.READ_EXTERNAL_STORAGE;
        return checkSelfPermission(permission) == PackageManager.PERMISSION_GRANTED;
    }

    void showPhotos(List<Uri> recentPhotos) {
        ArrayList<RecentPhoto> details = new ArrayList<>();
        long timestamp = 0;
        for (Uri photo : recentPhotos) details.add(new RecentPhoto(photo, timestamp++));
        showRecentPhotos(details);
    }

    private void showRecentPhotos(List<RecentPhoto> recentPhotos) {
        hasMorePhotos = recentPhotos.size() == reviewWindow.limit();
        findViewById(R.id.review_loading).setVisibility(View.GONE);
        ArrayList<Uri> uris = new ArrayList<>();
        for (RecentPhoto photo : recentPhotos) uris.add(photo.uri());
        int previousCount = photos.size();
        boolean appending = recentPhotos.size() > previousCount
                && uris.subList(0, previousCount).equals(photos);
        int generation = appending ? analysisGeneration : ++analysisGeneration;
        photos = List.copyOf(uris);
        GridLayout grid = findViewById(R.id.photo_grid);
        if (!appending) {
            features.clear();
            suggestions = Set.of();
            analyzedCount = 0;
            grid.removeAllViews();
            previousCount = 0;
        }
        int tileSize = Math.max(1, getResources().getDisplayMetrics().widthPixels / 3 - 2);
        for (int index = previousCount; index < recentPhotos.size(); index++) {
            grid.addView(createTile(recentPhotos.get(index), tileSize, generation));
        }
        TextView empty = findViewById(R.id.review_empty);
        empty.setText("No recent local camera photos found.");
        empty.setVisibility(photos.isEmpty() ? View.VISIBLE : View.GONE);
        if (photos.isEmpty()) showSuggestions(Set.of());
        updateSelectionDisplay();
    }

    private View createTile(RecentPhoto recentPhoto, int size, int generation) {
        Uri photo = recentPhoto.uri();
        FrameLayout tile = new FrameLayout(this);
        tile.setTag(photo);
        GridLayout.LayoutParams params = new GridLayout.LayoutParams();
        params.width = size;
        params.height = size;
        params.setMargins(1, 1, 1, 1);
        tile.setLayoutParams(params);

        ImageView image = new ImageView(this);
        image.setScaleType(ImageView.ScaleType.CENTER_CROP);
        image.setBackgroundColor(Color.rgb(232, 234, 237));
        thumbnailLoader.load(image, photo, size, bitmap -> {
            if (generation != analysisGeneration) return;
            analyzedCount++;
            if (bitmap != null) features.add(new PhotoFeatures(photo.toString(),
                    recentPhoto.takenAtMillis(), PhotoFeatureExtractor.hash(bitmap),
                    PhotoFeatureExtractor.quality(bitmap)));
            if (analyzedCount == photos.size()) {
                showStacks(BestShotEngine.stacks(features));
                showSuggestions(BestShotEngine.recommend(features));
            }
        });
        tile.addView(image, new FrameLayout.LayoutParams(
                FrameLayout.LayoutParams.MATCH_PARENT, FrameLayout.LayoutParams.MATCH_PARENT));

        ImageView marker = new ImageView(this);
        marker.setTag("marker");
        marker.setImageResource(R.drawable.ic_heart_outline);
        marker.setColorFilter(Color.WHITE);
        marker.setPadding(dp(6), dp(6), dp(6), dp(6));
        GradientDrawable circle = new GradientDrawable();
        circle.setShape(GradientDrawable.OVAL);
        circle.setColor(Color.rgb(11, 87, 208));
        marker.setBackground(circle);
        FrameLayout.LayoutParams markerParams = new FrameLayout.LayoutParams(dp(30), dp(30),
                Gravity.TOP | Gravity.END);
        markerParams.setMargins(0, dp(7), dp(7), 0);
        tile.addView(marker, markerParams);
        marker.setOnClickListener(view -> {
            selectionStore.toggle(photo);
            updateSelectionDisplay();
        });

        ImageView suggestion = new ImageView(this);
        suggestion.setImageResource(R.drawable.ic_star);
        suggestion.setColorFilter(Color.WHITE);
        suggestion.setPadding(dp(6), dp(6), dp(6), dp(6));
        GradientDrawable suggestionCircle = new GradientDrawable();
        suggestionCircle.setShape(GradientDrawable.OVAL);
        suggestionCircle.setColor(Color.rgb(176, 96, 0));
        suggestion.setBackground(suggestionCircle);
        suggestion.setVisibility(View.GONE);
        FrameLayout.LayoutParams suggestionParams = new FrameLayout.LayoutParams(dp(28), dp(28),
                Gravity.TOP | Gravity.START);
        suggestionParams.setMargins(dp(7), dp(7), 0, 0);
        tile.addView(suggestion, suggestionParams);

        TextView stack = new TextView(this);
        stack.setTextColor(Color.WHITE);
        stack.setTextSize(12);
        stack.setGravity(Gravity.CENTER);
        stack.setPadding(dp(7), dp(4), dp(7), dp(4));
        GradientDrawable stackBackground = new GradientDrawable();
        stackBackground.setColor(0xCC303134);
        stackBackground.setCornerRadius(dp(12));
        stack.setBackground(stackBackground);
        stack.setVisibility(View.GONE);
        FrameLayout.LayoutParams stackParams = new FrameLayout.LayoutParams(
                FrameLayout.LayoutParams.WRAP_CONTENT, FrameLayout.LayoutParams.WRAP_CONTENT,
                Gravity.BOTTOM | Gravity.START);
        stackParams.setMargins(dp(7), 0, 0, dp(7));
        tile.addView(stack, stackParams);
        tile.setContentDescription("Photo. Tap to mark as keeper.");
        tile.setOnClickListener(view -> startActivity(new Intent(this, PreviewActivity.class)
                .setData(photo).putExtra(EXTRA_REVIEW_LIMIT, reviewWindow.limit())));
        return tile;
    }

    private void loadRecentPhotos() {
        showRecentPhotos(RecentCameraQuery.loadRecent(getContentResolver(), reviewWindow.limit()));
    }

    void loadNextPage() {
        reviewWindow.expand();
        findViewById(R.id.review_loading).setVisibility(View.VISIBLE);
        if (hasLocalPhotoAccess()) loadRecentPhotos();
    }

    int reviewLimit() { return reviewWindow.limit(); }

    private void updateSelectionDisplay() {
        Set<String> selected = selectionStore.load();
        Set<String> visibleSelected = new HashSet<>();
        for (Uri photo : photos) if (selected.contains(photo.toString())) {
            visibleSelected.add(photo.toString());
        }
        GridLayout grid = findViewById(R.id.photo_grid);
        for (int index = 0; index < grid.getChildCount(); index++) {
            FrameLayout tile = (FrameLayout) grid.getChildAt(index);
            boolean keeper = visibleSelected.contains(tile.getTag().toString());
            boolean suggested = suggestions.contains(tile.getTag().toString());
            tile.setAlpha(1f);
            ImageView heart = (ImageView) tile.getChildAt(1);
            heart.setImageResource(keeper ? R.drawable.ic_heart_filled
                    : R.drawable.ic_heart_outline);
            heart.setColorFilter(keeper ? Color.rgb(234, 67, 53) : Color.WHITE);
            heart.setVisibility(View.VISIBLE);
            tile.getChildAt(2).setVisibility(suggested ? View.VISIBLE : View.GONE);
            tile.setContentDescription(keeper ? "Keeper photo. Tap to remove."
                    : "Photo. Tap to mark as keeper.");
        }
        int count = visibleSelected.size();
        ((TextView) findViewById(R.id.keeper_count)).setText(count == 0
                ? "No keepers selected yet" : count + (count == 1 ? " keeper" : " keepers"));
        findViewById(R.id.clear_keepers).setEnabled(count > 0);
        findViewById(R.id.clear_keepers).setAlpha(count > 0 ? 1f : 0.35f);
    }

    void showSuggestions(Set<String> recommended) {
        suggestions = Set.copyOf(recommended);
        new SuggestionStore(this).save(suggestions);
        int count = suggestions.size();
        ((TextView) findViewById(R.id.suggestion_count)).setText(count == 0
                ? "No near-duplicate groups found" : count
                + (count == 1 ? " suggested best shot" : " suggested best shots"));
        updateSelectionDisplay();
    }

    void showStacks(Map<String, PhotoStackPosition> stacks) {
        GridLayout grid = findViewById(R.id.photo_grid);
        for (int index = 0; index < grid.getChildCount(); index++) {
            FrameLayout tile = (FrameLayout) grid.getChildAt(index);
            TextView badge = (TextView) tile.getChildAt(3);
            PhotoStackPosition position = stacks.get(tile.getTag().toString());
            badge.setText(position == null ? "" : position.label());
            badge.setVisibility(position == null ? View.GONE : View.VISIBLE);
        }
    }

    @Override protected void onResume() {
        super.onResume();
        if (!photos.isEmpty()) updateSelectionDisplay();
    }

    private int dp(int value) {
        return Math.round(value * getResources().getDisplayMetrics().density);
    }

    @Override protected void onDestroy() {
        thumbnailLoader.close();
        super.onDestroy();
    }
}
