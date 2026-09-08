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
    private enum GalleryFilter { ALL, KEEPERS, RECOMMENDED }
    public static final String EXTRA_REVIEW_LIMIT = "review_limit";
    private static final int PHOTO_PERMISSION = 200;
    private KeeperSelectionStore selectionStore;
    private AsyncThumbnailLoader thumbnailLoader;
    private List<Uri> photos = List.of();
    private final List<FrameLayout> tiles = new ArrayList<>();
    private final List<PhotoFeatures> features = new ArrayList<>();
    private Set<String> suggestions = Set.of();
    private Set<String> goodAlternatives = Set.of();
    private Map<String, List<String>> stackMembers = Map.of();
    private int analyzedCount;
    private int analysisGeneration;
    private final ReviewWindow reviewWindow = new ReviewWindow();
    private final InfiniteScrollTrigger infiniteScroll = new InfiniteScrollTrigger(600);
    private boolean hasMorePhotos;
    private FaceAnalyzer faceAnalyzer;
    private GalleryFilter galleryFilter = GalleryFilter.ALL;

    @Override protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_review);
        selectionStore = new KeeperSelectionStore(this);
        thumbnailLoader = AsyncThumbnailLoader.forResolver(getContentResolver());
        faceAnalyzer = createFaceAnalyzer();
        findViewById(R.id.open_settings).setOnClickListener(view ->
                startActivity(new Intent(this, MainActivity.class)));
        findViewById(R.id.open_album_review).setOnClickListener(view ->
                startActivity(new Intent(this, AlbumReviewActivity.class)));
        findViewById(R.id.clear_keepers).setOnClickListener(view -> {
            selectionStore.clear();
            updateSelectionDisplay();
        });
        findViewById(R.id.filter_keepers).setOnClickListener(view ->
                toggleFilter(GalleryFilter.KEEPERS));
        findViewById(R.id.filter_recommended).setOnClickListener(view ->
                toggleFilter(GalleryFilter.RECOMMENDED));
        ScrollView scroll = findViewById(R.id.review_scroll);
        scroll.setOnScrollChangeListener((view, scrollX, scrollY, oldScrollX, oldScrollY) -> {
            View content = scroll.getChildAt(0);
            if (content != null && infiniteScroll.onScroll(scrollY, scroll.getHeight(),
                    content.getHeight(), hasMorePhotos)) loadNextPage();
        });
        loadOrRequestPhotos();
    }

    private static FaceAnalyzer createFaceAnalyzer() {
        try {
            return new MlKitFaceAnalyzer();
        } catch (IllegalStateException unavailable) {
            return (photoId, bitmap, result) -> result.accept(List.of());
        }
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
            goodAlternatives = Set.of();
            stackMembers = Map.of();
            analyzedCount = 0;
            grid.removeAllViews();
            tiles.clear();
            previousCount = 0;
        }
        int tileSize = Math.max(1, getResources().getDisplayMetrics().widthPixels / 3 - 2);
        for (int index = previousCount; index < recentPhotos.size(); index++) {
            FrameLayout tile = createTile(recentPhotos.get(index), tileSize, generation);
            tiles.add(tile);
            grid.addView(tile);
        }
        TextView empty = findViewById(R.id.review_empty);
        empty.setText("No recent local camera photos found.");
        empty.setVisibility(photos.isEmpty() ? View.VISIBLE : View.GONE);
        if (photos.isEmpty()) showSuggestions(Set.of());
        updateSelectionDisplay();
    }

    private FrameLayout createTile(RecentPhoto recentPhoto, int size, int generation) {
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
            if (bitmap != null) {
                PhotoQualityAssessment assessment = PhotoFeatureExtractor.assess(bitmap);
                faceAnalyzer.analyze(photo.toString(), bitmap, faces -> {
                    if (generation != analysisGeneration) return;
                    new FaceObservationStore(this).save(photo.toString(), faces);
                    FaceSignals faceSignals = FaceSignals.from(faces);
                    features.add(new PhotoFeatures(photo.toString(), recentPhoto.takenAtMillis(),
                            PhotoFeatureExtractor.hash(bitmap), assessment.detail(), assessment.focus(),
                            assessment.exposure(), assessment.composition(), assessment.motionStability(),
                            faceSignals.faceCount(), faceSignals.averageSmile(),
                            faceSignals.minimumEyeOpen()));
                    finishPhotoAnalysis(generation);
                });
                return;
            }
            finishPhotoAnalysis(generation);
        });
        tile.addView(image, new FrameLayout.LayoutParams(
                FrameLayout.LayoutParams.MATCH_PARENT, FrameLayout.LayoutParams.MATCH_PARENT));

        ImageView marker = new ImageView(this);
        marker.setTag("marker");
        marker.setImageResource(R.drawable.ic_heart_outline);
        marker.setColorFilter(Color.WHITE);
        marker.setPadding(dp(6), dp(6), dp(6), dp(6));
        FrameLayout.LayoutParams markerParams = new FrameLayout.LayoutParams(dp(30), dp(30),
                Gravity.TOP | Gravity.END);
        markerParams.setMargins(0, dp(7), dp(7), 0);
        tile.addView(marker, markerParams);
        marker.setOnClickListener(view -> {
            selectionStore.toggle(photo);
            new AlbumReviewSelectionStore(this).clear();
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

    private void finishPhotoAnalysis(int generation) {
        if (generation != analysisGeneration) return;
        analyzedCount++;
        if (analyzedCount == photos.size()) {
                Map<String, PhotoStackPosition> stacks = BestShotEngine.stacks(features);
                Map<String, List<String>> members = BestShotEngine.stackMembers(features);
                BestShotResult result = BestShotEngine.classify(features);
                new PhotoStackStore(this).save(members);
                new PhotoInsightStore(this).save(features, stacks, result.recommended(),
                        result.goodAlternatives());
                showStacks(stacks, members);
                showSuggestions(result.recommended(), result.goodAlternatives());
        }
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
        for (FrameLayout tile : tiles) {
            boolean keeper = visibleSelected.contains(tile.getTag().toString());
            boolean suggested = suggestions.contains(tile.getTag().toString());
            boolean alternative = goodAlternatives.contains(tile.getTag().toString());
            tile.setAlpha(1f);
            ImageView heart = (ImageView) tile.getChildAt(1);
            heart.setImageResource(keeper ? R.drawable.ic_heart_filled
                    : R.drawable.ic_heart_outline);
            heart.setColorFilter(keeper ? Color.rgb(234, 67, 53) : Color.WHITE);
            int heartPadding = dp(HeartIconStyle.paddingDp(keeper));
            heart.setPadding(heartPadding, heartPadding, heartPadding, heartPadding);
            heart.setVisibility(View.VISIBLE);
            ImageView star = (ImageView) tile.getChildAt(2);
            star.setImageResource(alternative ? R.drawable.ic_star_outline : R.drawable.ic_star);
            star.setBackground(suggested ? recommendationCircle() : null);
            star.setContentDescription(suggested ? "Recommended best shot" : alternative
                    ? "Good alternative — near-identical photo ranked higher" : null);
            star.setVisibility(suggested || alternative ? View.VISIBLE : View.GONE);
            tile.setContentDescription(keeper ? "Keeper photo. Tap to remove."
                    : "Photo. Tap to mark as keeper.");
        }
        int count = visibleSelected.size();
        ((TextView) findViewById(R.id.keeper_count)).setText(count == 0
                ? "No keepers selected yet" : count + (count == 1 ? " keeper" : " keepers"));
        findViewById(R.id.clear_keepers).setEnabled(count > 0);
        findViewById(R.id.clear_keepers).setAlpha(count > 0 ? 1f : 0.35f);
        applyFilter();
    }

    void showSuggestions(Set<String> recommended) {
        showSuggestions(recommended, Set.of());
    }

    void showSuggestions(Set<String> recommended, Set<String> alternatives) {
        suggestions = Set.copyOf(recommended);
        goodAlternatives = Set.copyOf(alternatives);
        new SuggestionStore(this).save(suggestions);
        new SuggestionStore(this).saveAlternatives(goodAlternatives);
        int count = suggestions.size();
        ((TextView) findViewById(R.id.suggestion_count)).setText(count == 0
                ? "No near-duplicate groups found" : count
                + (count == 1 ? " suggested best shot" : " suggested best shots"));
        updateSelectionDisplay();
    }

    private GradientDrawable recommendationCircle() {
        GradientDrawable circle = new GradientDrawable();
        circle.setShape(GradientDrawable.OVAL);
        circle.setColor(Color.rgb(176, 96, 0));
        return circle;
    }

    void showStacks(Map<String, PhotoStackPosition> stacks) {
        showStacks(stacks, Map.of());
    }

    void showStacks(Map<String, PhotoStackPosition> stacks,
            Map<String, List<String>> members) {
        stackMembers = Map.copyOf(members);
        for (FrameLayout tile : tiles) {
            TextView badge = (TextView) tile.getChildAt(3);
            PhotoStackPosition position = stacks.get(tile.getTag().toString());
            badge.setText(position == null ? "" : position.label());
            badge.setVisibility(position == null ? View.GONE : View.VISIBLE);
        }
        applyFilter();
    }

    private void toggleFilter(GalleryFilter requested) {
        galleryFilter = galleryFilter == requested ? GalleryFilter.ALL : requested;
        applyFilter();
    }

    private void applyFilter() {
        Set<String> keepers = selectionStore.load();
        GridLayout grid = findViewById(R.id.photo_grid);
        grid.removeAllViews();
        List<String> orderedIds = photos.stream().map(Uri::toString).toList();
        Set<String> stackCovers = new HashSet<>(StackPresentation.visibleIds(
                orderedIds, stackMembers, suggestions));
        int visible = 0;
        for (FrameLayout tile : tiles) {
            String id = tile.getTag().toString();
            boolean show = galleryFilter == GalleryFilter.ALL && stackCovers.contains(id)
                    || galleryFilter == GalleryFilter.KEEPERS && keepers.contains(id)
                    || galleryFilter == GalleryFilter.RECOMMENDED && suggestions.contains(id);
            if (show) {
                grid.addView(tile);
                visible++;
            }
        }
        View keeperFilter = findViewById(R.id.filter_keepers);
        View recommendedFilter = findViewById(R.id.filter_recommended);
        keeperFilter.setSelected(galleryFilter == GalleryFilter.KEEPERS);
        recommendedFilter.setSelected(galleryFilter == GalleryFilter.RECOMMENDED);
        TextView empty = findViewById(R.id.review_empty);
        if (!photos.isEmpty() && visible == 0) {
            empty.setText(galleryFilter == GalleryFilter.KEEPERS
                    ? "No Keepers in the loaded photos yet."
                    : "No recommended photos in the loaded photos yet.");
            empty.setVisibility(View.VISIBLE);
        } else if (!photos.isEmpty()) {
            empty.setVisibility(View.GONE);
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
        faceAnalyzer.close();
        thumbnailLoader.close();
        super.onDestroy();
    }
}
