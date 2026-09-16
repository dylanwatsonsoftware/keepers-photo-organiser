package com.keepers.photoorganiser;

import android.Manifest;
import android.app.Activity;
import android.app.Dialog;
import android.content.pm.PackageManager;
import android.content.Intent;
import android.content.IntentSender;
import android.content.ClipData;
import android.database.Cursor;
import android.graphics.Color;
import android.graphics.Bitmap;
import android.graphics.drawable.GradientDrawable;
import android.graphics.drawable.ColorDrawable;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.os.Process;
import android.provider.MediaStore;
import android.view.Gravity;
import android.view.MotionEvent;
import android.view.ScaleGestureDetector;
import android.view.View;
import android.widget.FrameLayout;
import android.widget.GridLayout;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;
import com.google.android.gms.auth.api.identity.AuthorizationClient;
import com.google.android.gms.auth.api.identity.AuthorizationRequest;
import com.google.android.gms.auth.api.identity.AuthorizationResult;
import com.google.android.gms.auth.api.identity.Identity;
import com.google.android.gms.common.api.Scope;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public final class ReviewActivity extends Activity {
    private static final int IMPORT_PHOTOS = 201;
    private static final int AUTHORIZE_GOOGLE_PHOTOS = 202;
    private static final String PHOTOS_PICKER_SCOPE =
            "https://www.googleapis.com/auth/photospicker.mediaitems.readonly";
    private enum GalleryFilter { ALL, INCLUDE_KEEPERS, KEEPERS, RECOMMENDED, HIDDEN }
    private enum MediaFilter { ALL, PHOTOS, VIDEOS }
    public static final String EXTRA_REVIEW_LIMIT = "review_limit";
    public static final String ACTION_IMPORT_DEVICE_PHOTOS =
            "com.keepers.photoorganiser.action.IMPORT_DEVICE_PHOTOS";
    public static final String ACTION_IMPORT_GOOGLE_PHOTOS =
            "com.keepers.photoorganiser.action.IMPORT_GOOGLE_PHOTOS";
    private static final int PHOTO_PERMISSION = 200;
    private static final long RANGE_SCROLL_FRAME_MILLIS = 16;
    private static final int RANGE_SCROLL_EDGE_DP = 72;
    private static final int RANGE_SCROLL_MAX_STEP_DP = 20;
    private static final long SELECTION_SCALE_ANIMATION_MILLIS = 140;
    private static final long HEART_FEEDBACK_ANIMATION_MILLIS = 160;
    private static final String GALLERY_DISPLAY_PREFERENCES = "gallery_display";
    private static final String GRID_COLUMNS_PREFERENCE = "grid_columns";
    private KeeperSelectionStore selectionStore;
    private AsyncThumbnailLoader thumbnailLoader;
    private List<Uri> photos = List.of();
    private final List<FrameLayout> tiles = new ArrayList<>();
    private final List<PhotoFeatures> features = new ArrayList<>();
    private Set<String> suggestions = Set.of();
    private Set<String> goodAlternatives = Set.of();
    private Map<String, List<String>> stackMembers = Map.of();
    private RecommendationPreferenceProfile recommendationProfile =
            RecommendationPreferenceProfile.learn(List.of());
    private int analyzedCount;
    private int analysisGeneration;
    private final ReviewWindow reviewWindow = new ReviewWindow();
    private final InfiniteScrollTrigger infiniteScroll = new InfiniteScrollTrigger(600);
    private boolean hasMorePhotos;
    private FaceAnalyzer faceAnalyzer;
    private PhotoContextAnalyzer photoContextAnalyzer;
    private GalleryFilter galleryFilter = GalleryFilter.ALL;
    private PhotoOrigin originFilter;
    private Map<String, PhotoOrigin> photoOrigins = Map.of();
    private Map<String, MediaType> mediaTypes = Map.of();
    private Map<String, Long> mediaDurations = Map.of();
    private MediaFilter mediaFilter = MediaFilter.ALL;
    private AuthorizationClient photosAuthorization;
    private String pickerAccessToken;
    private String pickerSessionId;
    private boolean metadataVisible;
    private int gridColumns;
    private ScaleGestureDetector gridScaleDetector;
    private float accumulatedGridScale = 1f;
    private int gridScaleStartColumns;
    private boolean gridScaleInProgress;
    private boolean suppressTouchUntilScaleEnds;
    private String gridScaleFocusMediaId;
    private float gridScaleFocusViewportX;
    private float gridScaleFocusViewportY;
    private float gridScaleFocusFractionX;
    private float gridScaleFocusFractionY;
    private android.view.animation.Animation gridSettleAnimation;
    private final Map<String, Float> mediaAspectRatios = new HashMap<>();
    private boolean selectingMedia;
    private boolean rangeSelectionGestureActive;
    private boolean rangeSelectionRemoving;
    private int rangeSelectionAnchor = -1;
    private float rangeSelectionPointerX;
    private float rangeSelectionPointerY;
    private float rangeSelectionTouchOriginX;
    private float rangeSelectionTouchOriginY;
    private int rangeSelectionScrollStep;
    private boolean rangeSelectionScrollScheduled;
    private boolean viewportLoadPending;
    private final Set<String> mediaSelections = new HashSet<>();
    private final Set<String> renderedMediaSelections = new HashSet<>();
    private Set<String> rangeSelectionBaseline = Set.of();
    private final MediaAnalysisQueue mediaAnalysisQueue = new MediaAnalysisQueue();
    private final Handler mainHandler = new Handler(Looper.getMainLooper());
    private final Runnable deferredRecommendationRefresh = this::refreshRecommendationsIfReady;
    private final Runnable rangeSelectionAutoScroll = new Runnable() {
        @Override public void run() {
            rangeSelectionScrollScheduled = false;
            if (!rangeSelectionGestureActive || rangeSelectionScrollStep == 0) return;
            ScrollView scroll = findViewById(R.id.review_scroll);
            scroll.scrollBy(0, rangeSelectionScrollStep);
            String target = visibleMediaAt(rangeSelectionPointerX, rangeSelectionPointerY);
            if (target != null) updateMediaRangeSelection(target);
            scheduleRangeSelectionAutoScroll();
        }
    };
    private ExecutorService videoAnalysisExecutor;
    private VideoFrameAnalyzer videoFrameAnalyzer;
    private boolean videoAnalysisRunning;
    private int pendingSemanticContexts;
    private RecommendationInputs lastRecommendationInputs;

    private record RecommendationInputs(Set<String> keepers, Set<String> hidden,
            Set<RecommendationFeedback> feedback, Map<String, String> faceAssignments,
            Map<String, String> faceCorrections) {}

    @Override protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_review);
        selectionStore = new KeeperSelectionStore(this);
        KeeperSelectionRecovery.runOnce(this);
        thumbnailLoader = AsyncThumbnailLoader.forResolver(getContentResolver());
        faceAnalyzer = createFaceAnalyzer();
        photoContextAnalyzer = createPhotoContextAnalyzer();
        videoFrameAnalyzer = new VideoFrameAnalyzer(this);
        videoAnalysisExecutor = Executors.newSingleThreadExecutor(runnable ->
                new Thread(() -> {
                    Process.setThreadPriority(Process.THREAD_PRIORITY_BACKGROUND);
                    runnable.run();
                }, "keepers-video-analysis"));
        metadataVisible = getSharedPreferences(GALLERY_DISPLAY_PREFERENCES, MODE_PRIVATE)
                .getBoolean("metadata_visible", false);
        gridColumns = clampGridColumns(getSharedPreferences(GALLERY_DISPLAY_PREFERENCES,
                MODE_PRIVATE).getInt(GRID_COLUMNS_PREFERENCE, 4));
        findViewById(R.id.open_settings).setOnClickListener(view -> navigateWithFeedback(view,
                () -> startActivity(new Intent(this, PeopleActivity.class))));
        findViewById(R.id.albums_destination).setOnClickListener(view ->
                navigateWithFeedback(view,
                        () -> startActivity(new Intent(this, AlbumReviewActivity.class))));
        findViewById(R.id.review_destination).setOnClickListener(view ->
                navigateWithFeedback(view, this::openQuickReview));
        findViewById(R.id.gallery_destination).setSelected(true);
        findViewById(R.id.gallery_destination).setOnClickListener(
                ReviewActivity::playTapFeedback);
        findViewById(R.id.cancel_selection).setOnClickListener(view -> endMediaSelection());
        findViewById(R.id.selection_hide).setOnClickListener(view -> hideSelectedMedia());
        findViewById(R.id.selection_share).setOnClickListener(view -> shareSelectedMedia());
        findViewById(R.id.selection_keeper).setOnClickListener(view -> keepSelectedMedia());
        findViewById(R.id.selection_albums).setOnClickListener(view ->
                addSelectedMediaToAlbums());
        findViewById(R.id.filter_keepers).setOnClickListener(view -> withTapFeedback(view,
                () -> toggleFilter(GalleryFilter.KEEPERS)));
        findViewById(R.id.filter_include_keepers).setOnClickListener(view ->
                withTapFeedback(view, () -> toggleFilter(GalleryFilter.INCLUDE_KEEPERS)));
        findViewById(R.id.filter_recommended).setOnClickListener(view ->
                withTapFeedback(view, () -> toggleFilter(GalleryFilter.RECOMMENDED)));
        findViewById(R.id.filter_hidden).setOnClickListener(view ->
                withTapFeedback(view, () -> toggleFilter(GalleryFilter.HIDDEN)));
        findViewById(R.id.filter_media_all).setOnClickListener(view ->
                withTapFeedback(view, () -> setMediaFilter(MediaFilter.ALL)));
        findViewById(R.id.filter_photos).setOnClickListener(view ->
                withTapFeedback(view, () -> setMediaFilter(MediaFilter.PHOTOS)));
        findViewById(R.id.filter_videos).setOnClickListener(view ->
                withTapFeedback(view, () -> setMediaFilter(MediaFilter.VIDEOS)));
        findViewById(R.id.toggle_metadata).setOnClickListener(view ->
                withTapFeedback(view, this::toggleMetadata));
        findViewById(R.id.filter_origin_all).setOnClickListener(view ->
                withTapFeedback(view, this::clearOriginFilter));
        findViewById(R.id.filter_origin_local).setOnClickListener(view ->
                withTapFeedback(view, () -> setOriginFilter(PhotoOrigin.LOCAL)));
        findViewById(R.id.filter_origin_cloud).setOnClickListener(view ->
                withTapFeedback(view, () -> setOriginFilter(PhotoOrigin.CLOUD)));
        ScrollView scroll = findViewById(R.id.review_scroll);
        GridLayout grid = findViewById(R.id.photo_grid);
        grid.setColumnCount(gridColumns);
        setupGalleryControls();
        setupGridScaleGesture();
        scroll.setOnScrollChangeListener((view, scrollX, scrollY, oldScrollX, oldScrollY) -> {
            View content = scroll.getChildAt(0);
            if (content != null && infiniteScroll.onScroll(scrollY, scroll.getHeight(),
                    content.getHeight(), hasMorePhotos)) loadNextPage();
        });
        grid.addOnLayoutChangeListener((view, left, top, right, bottom,
                oldLeft, oldTop, oldRight, oldBottom) -> {
            if (right - left != oldRight - oldLeft) reflowGrid(false);
            fillFilteredViewport();
        });
        loadOrRequestPhotos();
        handleImportAction(getIntent());
    }

    private void setupGalleryControls() {
        View overlay = findViewById(R.id.gallery_filter_overlay);
        View sheet = findViewById(R.id.gallery_filter_sheet);
        findViewById(R.id.open_gallery_filters).setOnClickListener(view -> {
            playTapFeedback(view);
            overlay.setVisibility(View.VISIBLE);
            sheet.requestFocus();
        });
        findViewById(R.id.close_gallery_filters).setOnClickListener(view ->
                overlay.setVisibility(View.GONE));
        findViewById(R.id.apply_gallery_filters).setOnClickListener(view ->
                overlay.setVisibility(View.GONE));
        overlay.setOnClickListener(view -> overlay.setVisibility(View.GONE));
        sheet.setOnClickListener(view -> {});
        findViewById(R.id.reset_gallery_filters).setOnClickListener(view -> {
            playTapFeedback(view);
            galleryFilter = GalleryFilter.ALL;
            originFilter = null;
            mediaFilter = MediaFilter.ALL;
            if (metadataVisible) toggleMetadata();
            applyFilter();
        });
        int[] densityIds = {R.id.grid_columns_1, R.id.grid_columns_2,
                R.id.grid_columns_3, R.id.grid_columns_4};
        for (int index = 0; index < densityIds.length; index++) {
            int columns = index + 1;
            findViewById(densityIds[index]).setOnClickListener(view ->
                    withTapFeedback(view, () -> setGridColumns(columns)));
        }
        updateGridDensityChoices();
    }

    private void withTapFeedback(View view, Runnable action) {
        playTapFeedback(view);
        action.run();
    }

    private void navigateWithFeedback(View view, Runnable destination) {
        if (!view.isEnabled()) return;
        view.setEnabled(false);
        playTapFeedback(view);
        view.postDelayed(() -> {
            view.setEnabled(true);
            destination.run();
        }, 120);
    }

    private static void playTapFeedback(View view) {
        android.view.animation.AnimationSet feedback =
                new android.view.animation.AnimationSet(false);
        android.view.animation.ScaleAnimation scale =
                new android.view.animation.ScaleAnimation(.94f, 1f, .94f, 1f,
                        android.view.animation.Animation.RELATIVE_TO_SELF, .5f,
                        android.view.animation.Animation.RELATIVE_TO_SELF, .5f);
        scale.setDuration(170);
        scale.setInterpolator(new android.view.animation.OvershootInterpolator(1.4f));
        android.view.animation.AlphaAnimation fade =
                new android.view.animation.AlphaAnimation(.72f, 1f);
        fade.setDuration(130);
        feedback.addAnimation(scale);
        feedback.addAnimation(fade);
        view.startAnimation(feedback);
    }

    private void setupGridScaleGesture() {
        gridScaleDetector = new ScaleGestureDetector(this,
                new ScaleGestureDetector.SimpleOnScaleGestureListener() {
                    @Override public boolean onScaleBegin(ScaleGestureDetector detector) {
                        accumulatedGridScale = 1f;
                        gridScaleStartColumns = gridColumns;
                        gridScaleInProgress = true;
                        captureGridScaleFocus(detector.getFocusX(), detector.getFocusY());
                        GridLayout grid = findViewById(R.id.photo_grid);
                        grid.clearAnimation();
                        grid.setScaleX(1f);
                        grid.setScaleY(1f);
                        boolean cancellingRangeSelection = rangeSelectionGestureActive;
                        if (cancellingRangeSelection) finishMediaRangeSelection();
                        if (cancellingRangeSelection && selectingMedia) endMediaSelection();
                        return true;
                    }

                    @Override public boolean onScale(ScaleGestureDetector detector) {
                        accumulatedGridScale *= detector.getScaleFactor();
                        float minimum = gridScaleStartColumns / 4f;
                        float maximum = gridScaleStartColumns;
                        accumulatedGridScale = Math.max(minimum * .88f,
                                Math.min(maximum * 1.12f, accumulatedGridScale));
                        previewGridScale(accumulatedGridScale,
                                detector.getFocusX(), detector.getFocusY());
                        return true;
                    }

                    @Override public void onScaleEnd(ScaleGestureDetector detector) {
                        gridScaleInProgress = false;
                        finishGridScalePreview();
                    }
                });
    }

    private void previewGridScale(float scale, float focusX, float focusY) {
        GridLayout grid = findViewById(R.id.photo_grid);
        int[] gridLocation = new int[2];
        grid.getLocationInWindow(gridLocation);
        grid.setPivotX(Math.max(0, Math.min(grid.getWidth(), focusX - gridLocation[0])));
        grid.setPivotY(Math.max(0, Math.min(grid.getHeight(), focusY - gridLocation[1])));
        grid.setScaleX(scale);
        grid.setScaleY(scale);
    }

    private void finishGridScalePreview() {
        GridLayout grid = findViewById(R.id.photo_grid);
        int startColumns = Math.max(1, gridScaleStartColumns);
        float previewScale = accumulatedGridScale;
        int targetColumns = clampGridColumns(Math.round(startColumns / previewScale));
        float settleStart = previewScale * targetColumns / startColumns;
        float pivotX = grid.getWidth() == 0 ? .5f : grid.getPivotX() / grid.getWidth();
        float pivotY = grid.getHeight() == 0 ? .5f : grid.getPivotY() / grid.getHeight();
        grid.setScaleX(1f);
        grid.setScaleY(1f);
        setGridColumns(targetColumns, false);
        android.view.animation.ScaleAnimation settle = new android.view.animation.ScaleAnimation(
                settleStart, 1f, settleStart, 1f,
                android.view.animation.Animation.RELATIVE_TO_SELF, pivotX,
                android.view.animation.Animation.RELATIVE_TO_SELF, pivotY);
        settle.setDuration(220);
        settle.setInterpolator(new android.view.animation.DecelerateInterpolator(1.5f));
        gridSettleAnimation = settle;
        grid.startAnimation(settle);
        restoreGridScaleFocus(settleStart);
        accumulatedGridScale = 1f;
    }

    private void captureGridScaleFocus(float windowX, float windowY) {
        ScrollView scroll = findViewById(R.id.review_scroll);
        GridLayout grid = findViewById(R.id.photo_grid);
        int[] scrollLocation = new int[2];
        scroll.getLocationInWindow(scrollLocation);
        float viewportX = windowX - scrollLocation[0];
        float viewportY = windowY - scrollLocation[1];
        String mediaId = visibleMediaAt(viewportX, viewportY);
        View focused = null;
        for (int index = 0; index < grid.getChildCount(); index++) {
            View tile = grid.getChildAt(index);
            if (mediaId != null && mediaId.equals(tile.getTag().toString())) {
                focused = tile;
                break;
            }
        }
        gridScaleFocusMediaId = mediaId;
        gridScaleFocusViewportX = viewportX;
        gridScaleFocusViewportY = viewportY;
        gridScaleFocusFractionX = focused == null || focused.getWidth() == 0 ? .5f
                : Math.max(0f, Math.min(1f, (viewportX + scroll.getScrollX()
                        - focused.getLeft()) / focused.getWidth()));
        gridScaleFocusFractionY = focused == null || focused.getHeight() == 0 ? .5f
                : Math.max(0f, Math.min(1f, (viewportY + scroll.getScrollY()
                        - focused.getTop()) / focused.getHeight()));
    }

    private void restoreGridScaleFocus(float settleStart) {
        String mediaId = gridScaleFocusMediaId;
        float viewportX = gridScaleFocusViewportX;
        float viewportY = gridScaleFocusViewportY;
        float fractionX = gridScaleFocusFractionX;
        float fractionY = gridScaleFocusFractionY;
        gridScaleFocusMediaId = null;
        if (mediaId == null) return;
        ScrollView scroll = findViewById(R.id.review_scroll);
        GridLayout grid = findViewById(R.id.photo_grid);
        grid.post(() -> {
            View focused = null;
            for (int index = 0; index < grid.getChildCount(); index++) {
                View tile = grid.getChildAt(index);
                if (mediaId.equals(tile.getTag().toString())) {
                    focused = tile;
                    break;
                }
            }
            if (focused == null) return;
            int requested = Math.round(focused.getTop() + focused.getHeight() * fractionY
                    - viewportY);
            int maximum = Math.max(0, grid.getHeight() - scroll.getHeight());
            scroll.scrollTo(0, Math.max(0, Math.min(maximum, requested)));
            float anchorX = focused.getLeft() + focused.getWidth() * fractionX;
            float anchorY = focused.getTop() + focused.getHeight() * fractionY;
            float deltaX = viewportX + scroll.getScrollX() - anchorX;
            float deltaY = viewportY + scroll.getScrollY() - anchorY;
            android.view.animation.AnimationSet anchoredSettle =
                    new android.view.animation.AnimationSet(true);
            anchoredSettle.addAnimation(new android.view.animation.ScaleAnimation(
                    settleStart, 1f, settleStart, 1f,
                    android.view.animation.Animation.ABSOLUTE, anchorX,
                    android.view.animation.Animation.ABSOLUTE, anchorY));
            anchoredSettle.addAnimation(new android.view.animation.TranslateAnimation(
                    deltaX, 0f, deltaY, 0f));
            anchoredSettle.setDuration(220);
            anchoredSettle.setInterpolator(
                    new android.view.animation.DecelerateInterpolator(1.5f));
            gridSettleAnimation = anchoredSettle;
            grid.startAnimation(anchoredSettle);
        });
    }

    android.view.animation.Animation gridSettleAnimation() {
        return gridSettleAnimation;
    }

    @Override public boolean dispatchTouchEvent(MotionEvent event) {
        boolean inGallery = touchIsInside(findViewById(R.id.review_scroll), event);
        if (gridScaleDetector != null
                && (inGallery || gridScaleInProgress || suppressTouchUntilScaleEnds))
            gridScaleDetector.onTouchEvent(event);
        if (inGallery && (gridScaleInProgress || event.getPointerCount() > 1)) {
            if (!suppressTouchUntilScaleEnds) {
                MotionEvent cancel = MotionEvent.obtain(event);
                cancel.setAction(MotionEvent.ACTION_CANCEL);
                super.dispatchTouchEvent(cancel);
                cancel.recycle();
            }
            suppressTouchUntilScaleEnds = true;
        }
        if (suppressTouchUntilScaleEnds) {
            if (event.getActionMasked() == MotionEvent.ACTION_UP
                    || event.getActionMasked() == MotionEvent.ACTION_CANCEL)
                suppressTouchUntilScaleEnds = false;
            return true;
        }
        return super.dispatchTouchEvent(event);
    }

    private static boolean touchIsInside(View view, MotionEvent event) {
        if (view == null || view.getVisibility() != View.VISIBLE) return false;
        int[] location = new int[2];
        view.getLocationOnScreen(location);
        float x = event.getRawX();
        float y = event.getRawY();
        return x >= location[0] && x < location[0] + view.getWidth()
                && y >= location[1] && y < location[1] + view.getHeight();
    }

    @Override protected void onNewIntent(Intent intent) {
        super.onNewIntent(intent);
        setIntent(intent);
        handleImportAction(intent);
    }

    @Override public void onBackPressed() {
        View filters = findViewById(R.id.gallery_filter_overlay);
        if (filters != null && filters.getVisibility() == View.VISIBLE) {
            filters.setVisibility(View.GONE);
            return;
        }
        if (selectingMedia) {
            endMediaSelection();
            return;
        }
        super.onBackPressed();
    }

    private void handleImportAction(Intent intent) {
        if (intent == null) return;
        String action = intent.getAction();
        intent.setAction(null);
        if (ACTION_IMPORT_DEVICE_PHOTOS.equals(action)) openPhotoPicker();
        else if (ACTION_IMPORT_GOOGLE_PHOTOS.equals(action)) openGooglePhotosPicker();
    }

    private void clearOriginFilter() {
        originFilter = null;
        applyFilter();
    }

    private static FaceAnalyzer createFaceAnalyzer() {
        try {
            return new MlKitFaceAnalyzer();
        } catch (IllegalStateException unavailable) {
            return (photoId, bitmap, result) -> result.accept(List.of());
        }
    }

    private static PhotoContextAnalyzer createPhotoContextAnalyzer() {
        try {
            return new MlKitPhotoContextAnalyzer();
        } catch (IllegalStateException unavailable) {
            return (bitmap, result) -> result.accept(List.of());
        }
    }

    private void openQuickReview() {
        applyFilter();
        List<String> visibleIds = visibleMediaIds();
        Uri firstVisible = visibleIds.isEmpty() ? null : Uri.parse(visibleIds.get(0));
        if (firstVisible == null) {
            Toast.makeText(this, "No media to review yet", Toast.LENGTH_SHORT).show();
            return;
        }
        Intent review = new Intent(this, PreviewActivity.class).setData(firstVisible)
                .putExtra(EXTRA_REVIEW_LIMIT, reviewWindow.limit())
                .putExtra(PreviewActivity.EXTRA_QUICK_REVIEW, true);
        if (mediaFilter == MediaFilter.PHOTOS)
            review.putExtra(PreviewActivity.EXTRA_MEDIA_TYPE, MediaType.PHOTO.name());
        if (mediaFilter == MediaFilter.VIDEOS)
            review.putExtra(PreviewActivity.EXTRA_MEDIA_TYPE, MediaType.VIDEO.name());
        startActivity(review);
    }

    private void loadOrRequestPhotos() {
        if (hasLocalPhotoAccess()) {
            loadRecentPhotos();
        } else {
            loadImportedPhotos();
        }
        boolean imagesGranted = checkSelfPermission(Build.VERSION.SDK_INT >= 33
                ? Manifest.permission.READ_MEDIA_IMAGES : Manifest.permission.READ_EXTERNAL_STORAGE)
                == PackageManager.PERMISSION_GRANTED;
        boolean videosGranted = Build.VERSION.SDK_INT < 33 || checkSelfPermission(
                Manifest.permission.READ_MEDIA_VIDEO) == PackageManager.PERMISSION_GRANTED;
        boolean selectedGranted = Build.VERSION.SDK_INT >= 34 && checkSelfPermission(
                Manifest.permission.READ_MEDIA_VISUAL_USER_SELECTED)
                == PackageManager.PERMISSION_GRANTED;
        List<String> missing = MediaPermissionRequest.missing(Build.VERSION.SDK_INT,
                imagesGranted, videosGranted, selectedGranted);
        if (!missing.isEmpty()) requestPermissions(missing.toArray(String[]::new), PHOTO_PERMISSION);
    }

    @Override public void onRequestPermissionsResult(int requestCode, String[] permissions,
            int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == PHOTO_PERMISSION && hasLocalPhotoAccess()) {
            loadRecentPhotos();
        } else if (requestCode == PHOTO_PERMISSION) {
            ((TextView) findViewById(R.id.review_empty)).setText(
                    "Photo and video access is needed to review recent Pixel camera media.");
        }
    }

    private boolean hasLocalPhotoAccess() {
        if (Build.VERSION.SDK_INT >= 34 && checkSelfPermission(
                Manifest.permission.READ_MEDIA_VISUAL_USER_SELECTED)
                == PackageManager.PERMISSION_GRANTED) return true;
        if (Build.VERSION.SDK_INT >= 33) return checkSelfPermission(
                Manifest.permission.READ_MEDIA_IMAGES) == PackageManager.PERMISSION_GRANTED
                || checkSelfPermission(Manifest.permission.READ_MEDIA_VIDEO)
                == PackageManager.PERMISSION_GRANTED;
        return checkSelfPermission(Manifest.permission.READ_EXTERNAL_STORAGE)
                == PackageManager.PERMISSION_GRANTED;
    }

    void showPhotos(List<Uri> recentPhotos) {
        HashMap<String, PhotoOrigin> origins = new HashMap<>();
        for (Uri photo : recentPhotos) origins.put(photo.toString(), PhotoOrigin.LOCAL);
        showPhotos(recentPhotos, origins);
    }

    void showMedia(List<RecentPhoto> recentMedia) {
        HashMap<String, PhotoOrigin> origins = new HashMap<>();
        for (RecentPhoto media : recentMedia)
            origins.put(media.uri().toString(), PhotoOrigin.LOCAL);
        photoOrigins = Map.copyOf(origins);
        hasMorePhotos = false;
        showRecentPhotos(recentMedia);
    }

    void showPhotos(List<Uri> recentPhotos, Map<String, PhotoOrigin> origins) {
        ArrayList<RecentPhoto> details = new ArrayList<>();
        long timestamp = 0;
        for (Uri photo : recentPhotos) details.add(new RecentPhoto(photo, timestamp++));
        photoOrigins = Map.copyOf(origins);
        hasMorePhotos = false;
        showRecentPhotos(details);
    }

    private void showRecentPhotos(List<RecentPhoto> recentPhotos) {
        findViewById(R.id.review_loading).setVisibility(View.GONE);
        ArrayList<Uri> uris = new ArrayList<>();
        for (RecentPhoto photo : recentPhotos) uris.add(photo.uri());
        HashMap<String, MediaType> types = new HashMap<>();
        HashMap<String, Long> durations = new HashMap<>();
        for (RecentPhoto media : recentPhotos) {
            types.put(media.uri().toString(), media.mediaType());
            durations.put(media.uri().toString(), media.durationMillis());
        }
        mediaTypes = Map.copyOf(types);
        mediaDurations = Map.copyOf(durations);
        int previousCount = photos.size();
        boolean appending = previousCount > 0 && recentPhotos.size() > previousCount
                && uris.subList(0, previousCount).equals(photos);
        int generation = appending ? analysisGeneration : ++analysisGeneration;
        photos = List.copyOf(uris);
        GridLayout grid = findViewById(R.id.photo_grid);
        if (!appending) {
            features.clear();
            suggestions = Set.of();
            goodAlternatives = Set.of();
            stackMembers = Map.of();
            recommendationProfile = RecommendationPreferenceProfile.learn(List.of());
            analyzedCount = 0;
            mediaAnalysisQueue.clear();
            pendingSemanticContexts = 0;
            grid.removeAllViews();
            tiles.clear();
            renderedMediaSelections.clear();
            previousCount = 0;
        }
        ArrayList<RecentPhoto> pendingAnalysis = new ArrayList<>();
        HashSet<String> pendingAnalysisIds = new HashSet<>();
        PhotoInsightStore photoInsights = new PhotoInsightStore(this);
        VideoInsightStore videoInsights = new VideoInsightStore(this);
        for (RecentPhoto media : recentPhotos.subList(previousCount, recentPhotos.size())) {
            boolean reusable;
            if (media.mediaType() == MediaType.VIDEO) {
                VideoFeatures cached = videoInsights.load(media.uri().toString());
                reusable = cached != null
                        && cached.durationMillis() == media.durationMillis();
            } else {
                PhotoFeatures cached = photoInsights.loadReusableFeatures(
                        media.uri().toString(), media.takenAtMillis());
                reusable = cached != null;
                if (cached != null) features.add(cached);
            }
            if (reusable) analyzedCount++;
            else {
                pendingAnalysis.add(media);
                pendingAnalysisIds.add(media.uri().toString());
            }
        }
        mediaAnalysisQueue.add(pendingAnalysis);
        int tileSize = tileWidth();
        for (int index = previousCount; index < recentPhotos.size(); index++) {
            RecentPhoto media = recentPhotos.get(index);
            FrameLayout tile = createTile(media, tileSize, generation,
                    pendingAnalysisIds.contains(media.uri().toString()));
            tiles.add(tile);
            grid.addView(tile);
        }
        TextView empty = findViewById(R.id.review_empty);
        empty.setText("No recent local camera photos or videos found.");
        empty.setVisibility(photos.isEmpty() ? View.VISIBLE : View.GONE);
        if (photos.isEmpty()) showSuggestions(Set.of());
        else if (!appending) restoreCachedInsights();
        if (!photos.isEmpty() && analyzedCount < photos.size())
            showAnalysisProgress(analyzedCount, photos.size());
        updateSelectionDisplay();
        startNextVideoAnalysis(generation);
        lastRecommendationInputs = currentRecommendationInputs();
    }

    void setGridColumns(int requestedColumns) {
        setGridColumns(requestedColumns, true);
    }

    private void setGridColumns(int requestedColumns, boolean preserveScrollAnchor) {
        int columns = clampGridColumns(requestedColumns);
        if (columns == gridColumns
                && findViewById(R.id.photo_grid) != null
                && ((GridLayout) findViewById(R.id.photo_grid)).getColumnCount() == columns) {
            updateGridDensityChoices();
            return;
        }
        gridColumns = columns;
        getSharedPreferences(GALLERY_DISPLAY_PREFERENCES, MODE_PRIVATE).edit()
                .putInt(GRID_COLUMNS_PREFERENCE, columns).apply();
        GridLayout grid = findViewById(R.id.photo_grid);
        ArrayList<View> visibleTiles = new ArrayList<>();
        for (int index = 0; index < grid.getChildCount(); index++)
            visibleTiles.add(grid.getChildAt(index));
        grid.removeAllViews();
        grid.setColumnCount(columns);
        for (View visibleTile : visibleTiles) {
            resetGridPlacement(visibleTile);
            grid.addView(visibleTile);
        }
        reflowGrid(preserveScrollAnchor);
        updateGridDensityChoices();
        grid.announceForAccessibility(columns == 1 ? "One photo per row"
                : columns + " photos per row");
    }

    int gridColumns() {
        return gridColumns;
    }

    private static int clampGridColumns(int columns) {
        return Math.max(1, Math.min(4, columns));
    }

    private int tileWidth() {
        GridLayout grid = findViewById(R.id.photo_grid);
        int available = grid == null ? 0 : grid.getWidth();
        if (available <= 0) available = getResources().getDisplayMetrics().widthPixels;
        return Math.max(1, available / gridColumns - dp(2));
    }

    private void reflowGrid(boolean preserveScrollAnchor) {
        GridLayout grid = findViewById(R.id.photo_grid);
        ScrollView scroll = findViewById(R.id.review_scroll);
        if (grid == null || scroll == null) return;
        View anchor = null;
        int anchorOffset = 0;
        if (preserveScrollAnchor) {
            int scrollY = scroll.getScrollY();
            for (int index = 0; index < grid.getChildCount(); index++) {
                View child = grid.getChildAt(index);
                if (child.getBottom() > scrollY) {
                    anchor = child;
                    anchorOffset = child.getTop() - scrollY;
                    break;
                }
            }
        }
        int width = tileWidth();
        for (FrameLayout tile : tiles) reflowTile(tile, width);
        if (anchor != null) {
            View anchored = anchor;
            int offset = anchorOffset;
            grid.post(() -> scroll.scrollTo(0, Math.max(0, anchored.getTop() - offset)));
        }
    }

    private void reflowTile(FrameLayout tile, int width) {
        String mediaId = tile.getTag().toString();
        float aspectRatio = mediaAspectRatios.getOrDefault(mediaId, 4f / 3f);
        int height = gridColumns == 1
                ? Math.max(dp(160), Math.round(width / Math.max(.1f, aspectRatio))) : width;
        GridLayout.LayoutParams params = (GridLayout.LayoutParams) tile.getLayoutParams();
        params.width = width;
        params.height = height;
        params.setMargins(dp(1), dp(1), dp(1), dp(1));
        tile.setLayoutParams(params);
        ImageView suggestion = tile.findViewWithTag("suggestion_marker");
        if (suggestion != null) {
            int markerSize = dp(gridColumns >= 4 ? 22 : gridColumns == 3 ? 24 : 28);
            int markerPadding = dp(gridColumns >= 4 ? 4 : 6);
            FrameLayout.LayoutParams suggestionParams =
                    (FrameLayout.LayoutParams) suggestion.getLayoutParams();
            suggestionParams.width = markerSize;
            suggestionParams.height = markerSize;
            int edge = dp(gridColumns >= 4 ? 5 : 7);
            suggestionParams.setMargins(edge, edge, 0, 0);
            suggestion.setLayoutParams(suggestionParams);
            suggestion.setPadding(markerPadding, markerPadding, markerPadding, markerPadding);
        }
    }

    private static void resetGridPlacement(View tile) {
        GridLayout.LayoutParams old = (GridLayout.LayoutParams) tile.getLayoutParams();
        GridLayout.LayoutParams reset = new GridLayout.LayoutParams();
        reset.width = old.width;
        reset.height = old.height;
        reset.setMargins(old.leftMargin, old.topMargin, old.rightMargin, old.bottomMargin);
        tile.setLayoutParams(reset);
    }

    private void updateGridDensityChoices() {
        int[] ids = {R.id.grid_columns_1, R.id.grid_columns_2,
                R.id.grid_columns_3, R.id.grid_columns_4};
        for (int index = 0; index < ids.length; index++) {
            View choice = findViewById(ids[index]);
            if (choice != null) choice.setSelected(gridColumns == index + 1);
        }
    }

    private void restoreCachedInsights() {
        PhotoInsightStore insights = new PhotoInsightStore(this);
        PhotoStackStore stacks = new PhotoStackStore(this);
        HashMap<String, PhotoStackPosition> positions = new HashMap<>();
        HashMap<String, List<String>> members = new HashMap<>();
        HashSet<String> recommended = new HashSet<>();
        HashSet<String> alternatives = new HashSet<>();
        for (Uri photo : photos) {
            String id = photo.toString();
            PhotoInsight insight = insights.load(id);
            if (insight == null) continue;
            if (insight.stack() != null) positions.put(id, insight.stack());
            if (insight.recommended()) recommended.add(id);
            if (insight.goodAlternative()) alternatives.add(id);
            List<String> stack = stacks.load(id);
            if (!stack.isEmpty()) members.put(id, stack);
        }
        showStacks(positions, members);
        showSuggestions(recommended, alternatives);
    }

    private FrameLayout createTile(RecentPhoto recentPhoto, int size, int generation,
            boolean analyze) {
        Uri photo = recentPhoto.uri();
        FrameLayout tile = new FrameLayout(this);
        tile.setTag(photo);
        GridLayout.LayoutParams params = new GridLayout.LayoutParams();
        params.width = size;
        params.height = gridColumns == 1 ? Math.round(size / (4f / 3f)) : size;
        params.setMargins(1, 1, 1, 1);
        tile.setLayoutParams(params);

        ImageView image = new ImageView(this);
        image.setScaleType(ImageView.ScaleType.CENTER_CROP);
        image.setBackgroundColor(getColor(R.color.gallery_surface_elevated));
        thumbnailLoader.load(image, photo, size, bitmap -> {
            if (generation != analysisGeneration) return;
            if (bitmap != null) {
                mediaAspectRatios.put(photo.toString(), bitmap.getWidth()
                        / (float) Math.max(1, bitmap.getHeight()));
                if (gridColumns == 1) reflowTile(tile, tileWidth());
            }
            if (!analyze || recentPhoto.mediaType() == MediaType.VIDEO) return;
            if (bitmap != null) {
                PhotoQualityAssessment assessment = PhotoFeatureExtractor.assess(bitmap);
                PhotoContextStore contextStore = new PhotoContextStore(this);
                PhotoContext cachedContext = contextStore.load(photo.toString());
                PhotoAnalysisJoin enrichment = cachedContext == null
                        ? new PhotoAnalysisJoin(signals -> {
                            if (generation != analysisGeneration) return;
                            PhotoFeatures measured = measuredFeatures(photo, recentPhoto,
                                    bitmap, assessment, FaceSignals.from(signals.faces()));
                            contextStore.save(photo.toString(),
                                    PhotoContextClassifier.infer(measured, signals.labels()));
                            pendingSemanticContexts--;
                            if (pendingSemanticContexts == 0
                                    && mediaAnalysisQueue.photosComplete())
                                finalizePhotoInsights();
                        }) : null;
                if (enrichment != null) {
                    pendingSemanticContexts++;
                    photoContextAnalyzer.analyze(bitmap, enrichment::acceptLabels);
                }
                faceAnalyzer.analyze(photo.toString(), bitmap, faces -> {
                    if (generation != analysisGeneration) return;
                    new FaceObservationStore(this).save(photo.toString(), faces);
                    PhotoFeatures measured = measuredFeatures(photo, recentPhoto, bitmap,
                            assessment, FaceSignals.from(faces));
                    PhotoContext initialContext = cachedContext == null
                            ? PhotoContextClassifier.infer(measured, List.of()) : cachedContext;
                    contextStore.save(photo.toString(), initialContext);
                    features.add(measured);
                    if (enrichment != null) enrichment.acceptFaces(faces);
                    finishStillPhotoAnalysis(generation);
                });
                return;
            }
            finishStillPhotoAnalysis(generation);
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
            if (selectingMedia) {
                toggleMediaSelection(photo.toString());
                return;
            }
            boolean selected = selectionStore.toggle(photo);
            PhotoFeatures measured = features.stream()
                    .filter(item -> item.id().equals(photo.toString())).findFirst().orElse(null);
            if (measured == null) measured = new PhotoInsightStore(this)
                    .loadFeatures(photo.toString());
            if (measured != null) {
                RecommendationFeedbackStore feedbackStore = new RecommendationFeedbackStore(this);
                RecommendationFeedback previous = feedbackStore.load(photo.toString());
                feedbackStore.save(RecommendationFeedback.from(measured, selected
                        ? RecommendationFeedback.LOVED : RecommendationFeedback.NOT_FOR_ME,
                        previous == null ? "" : previous.comment()));
            }
            AlbumApprovalInvalidator.invalidate(this);
            updateSelectionDisplay();
            android.view.animation.ScaleAnimation pop =
                    new android.view.animation.ScaleAnimation(.87f, 1f, .87f, 1f,
                            android.view.animation.Animation.RELATIVE_TO_SELF, .5f,
                            android.view.animation.Animation.RELATIVE_TO_SELF, .5f);
            pop.setDuration(HEART_FEEDBACK_ANIMATION_MILLIS);
            pop.setInterpolator(new android.view.animation.OvershootInterpolator(.8f));
            marker.startAnimation(pop);
            mainHandler.removeCallbacks(deferredRecommendationRefresh);
            mainHandler.postDelayed(deferredRecommendationRefresh,
                    HEART_FEEDBACK_ANIMATION_MILLIS);
        });

        ImageView suggestion = new ImageView(this);
        suggestion.setTag("suggestion_marker");
        suggestion.setImageResource(R.drawable.ic_star);
        suggestion.setColorFilter(Color.WHITE);
        int suggestionPadding = dp(gridColumns >= 4 ? 4 : 6);
        suggestion.setPadding(suggestionPadding, suggestionPadding,
                suggestionPadding, suggestionPadding);
        GradientDrawable suggestionCircle = new GradientDrawable();
        suggestionCircle.setShape(GradientDrawable.OVAL);
        suggestionCircle.setColor(getColor(R.color.gallery_accent_warm));
        suggestion.setBackground(suggestionCircle);
        suggestion.setVisibility(View.GONE);
        int suggestionSize = dp(gridColumns >= 4 ? 22 : gridColumns == 3 ? 24 : 28);
        FrameLayout.LayoutParams suggestionParams = new FrameLayout.LayoutParams(
                suggestionSize, suggestionSize,
                Gravity.TOP | Gravity.START);
        int suggestionEdge = dp(gridColumns >= 4 ? 5 : 7);
        suggestionParams.setMargins(suggestionEdge, suggestionEdge, 0, 0);
        tile.addView(suggestion, suggestionParams);

        TextView stack = new TextView(this);
        stack.setTextColor(Color.WHITE);
        stack.setTextSize(12);
        stack.setGravity(Gravity.CENTER);
        stack.setPadding(dp(7), dp(4), dp(7), dp(4));
        stack.setCompoundDrawablesWithIntrinsicBounds(R.drawable.ic_stack, 0, 0, 0);
        stack.setCompoundDrawablePadding(dp(4));
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

        TextView origin = new TextView(this);
        origin.setText("☁");
        origin.setTextColor(Color.WHITE);
        origin.setTextSize(15);
        origin.setGravity(Gravity.CENTER);
        GradientDrawable originBackground = new GradientDrawable();
        originBackground.setColor(0xCC303134);
        originBackground.setCornerRadius(dp(14));
        origin.setBackground(originBackground);
        origin.setContentDescription("Cloud-only photo");
        origin.setVisibility(photoOrigins.getOrDefault(photo.toString(), PhotoOrigin.LOCAL)
                == PhotoOrigin.CLOUD ? View.VISIBLE : View.GONE);
        FrameLayout.LayoutParams originParams = new FrameLayout.LayoutParams(dp(28), dp(28),
                Gravity.BOTTOM | Gravity.END);
        originParams.setMargins(0, 0, dp(7), dp(7));
        tile.addView(origin, originParams);

        TextView metadata = new TextView(this);
        metadata.setTag("metadata_overlay");
        metadata.setTextColor(Color.WHITE);
        metadata.setTextSize(11);
        metadata.setTypeface(android.graphics.Typeface.DEFAULT,
                android.graphics.Typeface.BOLD);
        metadata.setLineSpacing(0, 1.08f);
        metadata.setPadding(dp(7), dp(5), dp(7), dp(5));
        GradientDrawable metadataBackground = new GradientDrawable();
        metadataBackground.setColor(0xD9202124);
        metadataBackground.setCornerRadius(dp(8));
        metadata.setBackground(metadataBackground);
        metadata.setVisibility(View.GONE);
        FrameLayout.LayoutParams metadataParams = new FrameLayout.LayoutParams(
                FrameLayout.LayoutParams.WRAP_CONTENT, FrameLayout.LayoutParams.WRAP_CONTENT,
                Gravity.BOTTOM | Gravity.CENTER_HORIZONTAL);
        metadataParams.setMargins(dp(7), 0, dp(7), dp(7));
        tile.addView(metadata, metadataParams);
        TextView hideCheck = new TextView(this);
        hideCheck.setTag("hide_selection_check");
        hideCheck.setText("✓");
        hideCheck.setTextColor(Color.WHITE);
        hideCheck.setTextSize(10);
        hideCheck.setGravity(Gravity.CENTER);
        hideCheck.setBackground(selectionCircle());
        hideCheck.setVisibility(View.GONE);
        FrameLayout.LayoutParams checkParams = new FrameLayout.LayoutParams(dp(18), dp(18),
                Gravity.TOP | Gravity.START);
        checkParams.setMargins(dp(7), dp(7), 0, 0);
        tile.addView(hideCheck, checkParams);
        tile.setContentDescription(recentPhoto.mediaType() == MediaType.VIDEO
                ? "Video. Tap to mark as keeper." : "Photo. Tap to mark as keeper.");
        tile.setOnClickListener(view -> {
            if (selectingMedia) { toggleMediaSelection(photo.toString()); return; }
            Intent preview = new Intent(this, PreviewActivity.class)
                    .setData(photo).putExtra(EXTRA_REVIEW_LIMIT, reviewWindow.limit());
            if (recentPhoto.mediaType() == MediaType.VIDEO)
                preview.putExtra(PreviewActivity.EXTRA_AUTOPLAY_VIDEO, true);
            startActivity(preview);
        });
        tile.setOnLongClickListener(view -> {
            beginMediaRangeSelection(photo.toString(), view);
            return true;
        });
        tile.setOnTouchListener((view, event) -> {
            if (!rangeSelectionGestureActive) return false;
            if (event.getActionMasked() == MotionEvent.ACTION_MOVE) {
                ScrollView scroll = findViewById(R.id.review_scroll);
                rangeSelectionPointerX = rangeSelectionTouchOriginX + event.getX()
                        - scroll.getScrollX();
                rangeSelectionPointerY = rangeSelectionTouchOriginY + event.getY()
                        - scroll.getScrollY();
                String target = visibleMediaAt(rangeSelectionPointerX, rangeSelectionPointerY);
                if (target != null) updateMediaRangeSelection(target);
                updateRangeSelectionAutoScroll();
            } else if (event.getActionMasked() == MotionEvent.ACTION_UP
                    || event.getActionMasked() == MotionEvent.ACTION_CANCEL) {
                completeMediaRangeSelection();
            }
            return false;
        });
        TextView videoDuration = new TextView(this);
        videoDuration.setTag("video_duration");
        videoDuration.setText("▶  " + MediaDuration.format(recentPhoto.durationMillis()));
        videoDuration.setTextColor(Color.WHITE);
        videoDuration.setTextSize(12);
        videoDuration.setTypeface(android.graphics.Typeface.DEFAULT,
                android.graphics.Typeface.BOLD);
        videoDuration.setGravity(Gravity.CENTER);
        videoDuration.setPadding(dp(8), dp(4), dp(8), dp(4));
        GradientDrawable videoBackground = new GradientDrawable();
        videoBackground.setColor(0xCC202124);
        videoBackground.setCornerRadius(dp(12));
        videoDuration.setBackground(videoBackground);
        videoDuration.setVisibility(recentPhoto.mediaType() == MediaType.VIDEO
                ? View.VISIBLE : View.GONE);
        FrameLayout.LayoutParams videoParams = new FrameLayout.LayoutParams(
                FrameLayout.LayoutParams.WRAP_CONTENT, FrameLayout.LayoutParams.WRAP_CONTENT,
                Gravity.BOTTOM | Gravity.END);
        videoParams.setMargins(0, 0, dp(7), dp(7));
        tile.addView(videoDuration, videoParams);
        return tile;
    }

    private static PhotoFeatures measuredFeatures(Uri photo, RecentPhoto recentPhoto,
            Bitmap bitmap, PhotoQualityAssessment assessment, FaceSignals faceSignals) {
        return new PhotoFeatures(photo.toString(), recentPhoto.takenAtMillis(),
                PhotoFeatureExtractor.hash(bitmap), assessment.detail(), assessment.focus(),
                assessment.exposure(), assessment.composition(), assessment.motionStability(),
                faceSignals.faceCount(), faceSignals.averageSmile(),
                faceSignals.minimumEyeOpen(), faceSignals.minimumCameraFacing());
    }

    private void finishStillPhotoAnalysis(int generation) {
        if (generation != analysisGeneration) return;
        analyzedCount++;
        showAnalysisProgress(analyzedCount, photos.size());
        if (mediaAnalysisQueue.photoCompleted()) {
            finalizePhotoInsights();
            startNextVideoAnalysis(generation);
        }
    }

    private void finalizePhotoInsights() {
        RecommendationFeedbackStore feedbackStore = new RecommendationFeedbackStore(this);
        Set<String> keepers = selectionStore.load();
        for (PhotoFeatures feature : features) if (keepers.contains(feature.id())) {
            RecommendationFeedback previous = feedbackStore.load(feature.id());
            feedbackStore.save(RecommendationFeedback.from(feature,
                    RecommendationFeedback.LOVED,
                    previous == null ? "" : previous.comment()));
        }
        List<FaceIdentityGroup> faceGroups = FaceClusterer.cluster(
                new FaceObservationStore(this).loadAll(), .30);
        Map<String, Set<String>> namedFaces = NamedFaceResolver.resolve(faceGroups,
                new FaceGroupAssignmentStore(this).load(),
                new FaceCorrectionStore(this).load());
        Map<String, PhotoStackPosition> stacks = BestShotEngine.stacks(features, namedFaces);
        Map<String, List<String>> members = BestShotEngine.stackMembers(features, namedFaces);
        Set<String> hiddenIds = new HiddenPhotoStore(this).load();
        List<PhotoFeatures> visibleFeatures = features.stream()
                .filter(feature -> !hiddenIds.contains(feature.id())).toList();
        List<PhotoFeatures> hiddenFeatures = features.stream()
                .filter(feature -> hiddenIds.contains(feature.id())).toList();
        List<StackPreferenceComparison> comparisons = StackPreferenceComparison.from(
                visibleFeatures,
                BestShotEngine.comparisonMembers(visibleFeatures, namedFaces), keepers);
        RecommendationPreferenceProfile profile = RecommendationPreferenceProfile.learn(
                feedbackStore.load(), comparisons, hiddenFeatures);
        Map<String, PhotoContext> contexts = new HashMap<>();
        PhotoContextStore contextStore = new PhotoContextStore(this);
        for (PhotoFeatures feature : features) {
            PhotoContext context = contextStore.load(feature.id());
            if (context != null) contexts.put(feature.id(), context);
        }
        profile = profile.withContexts(contexts);
        recommendationProfile = profile;
        BestShotResult result = BestShotEngine.classify(features, profile, namedFaces);
        Set<String> analyzedIds = features.stream().map(PhotoFeatures::id)
                .collect(java.util.stream.Collectors.toSet());
        new PhotoStackStore(this).saveForMedia(analyzedIds, members);
        new PhotoInsightStore(this).save(features, stacks, result.recommended(),
                result.goodAlternatives());
        updateMetadataOverlays();
        showStacks(stacks, members);
        showSuggestions(result.recommended(), result.goodAlternatives());
        if (analyzedCount < photos.size()) showAnalysisProgress(analyzedCount, photos.size());
        lastRecommendationInputs = currentRecommendationInputs();
    }

    private void startNextVideoAnalysis(int generation) {
        if (videoAnalysisRunning || !mediaAnalysisQueue.photosComplete()) return;
        RecentPhoto video = mediaAnalysisQueue.pollVideo();
        if (video == null) {
            if (!photos.isEmpty() && analyzedCount >= photos.size())
                showSuggestions(suggestions, goodAlternatives);
            return;
        }
        VideoFeatures cached = new VideoInsightStore(this).load(video.uri().toString());
        if (cached != null) {
            finishVideoAnalysis(generation, cached);
            return;
        }
        videoAnalysisRunning = true;
        videoAnalysisExecutor.execute(() -> {
            VideoFeatures result = videoFrameAnalyzer.analyze(video);
            mainHandler.post(() -> {
                videoAnalysisRunning = false;
                if (generation == analysisGeneration) finishVideoAnalysis(generation, result);
                else startNextVideoAnalysis(analysisGeneration);
            });
        });
    }

    private void finishVideoAnalysis(int generation, VideoFeatures video) {
        if (generation != analysisGeneration) return;
        if (video != null) new VideoInsightStore(this).save(video);
        analyzedCount++;
        updateMetadataOverlays();
        if (analyzedCount < photos.size()) showAnalysisProgress(analyzedCount, photos.size());
        else showSuggestions(suggestions, goodAlternatives);
        startNextVideoAnalysis(generation);
    }

    private void loadRecentPhotos() {
        List<RecentPhoto> local = RecentCameraQuery.loadRecent(
                getContentResolver(), reviewWindow.limit());
        showLocalAndImportedPhotos(local);
    }

    private void loadImportedPhotos() {
        hasMorePhotos = false;
        showLocalAndImportedPhotos(List.of());
    }

    private void showLocalAndImportedPhotos(List<RecentPhoto> local) {
        RecentMediaWindow.Result window = RecentMediaWindow.combine(local,
                new ImportedPhotoStore(this).load(), reviewWindow.limit());
        HashMap<String, PhotoOrigin> origins = new HashMap<>();
        ArrayList<RecentPhoto> ordered = new ArrayList<>();
        for (RecentMediaWindow.Item item : window.items()) {
            ordered.add(item.media());
            origins.put(item.media().uri().toString(), item.origin());
        }
        hasMorePhotos = window.hasMore() || local.size() == reviewWindow.limit();
        photoOrigins = Map.copyOf(origins);
        showRecentPhotos(ordered);
    }

    private void openPhotoPicker() {
        Intent picker = new Intent(MediaStore.ACTION_PICK_IMAGES)
                .setType("*/*")
                .addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION
                        | Intent.FLAG_GRANT_PERSISTABLE_URI_PERMISSION)
                .putExtra(MediaStore.EXTRA_PICK_IMAGES_MAX,
                        Math.min(250, MediaStore.getPickImagesMaxLimit()));
        startActivityForResult(picker, IMPORT_PHOTOS);
    }

    private void openGooglePhotosPicker() {
        photosAuthorization = Identity.getAuthorizationClient(this);
        AuthorizationRequest request = AuthorizationRequest.builder()
                .setRequestedScopes(List.of(new Scope(PHOTOS_PICKER_SCOPE)))
                .build();
        photosAuthorization.authorize(request)
                .addOnSuccessListener(result -> {
                    if (result.hasResolution()) {
                        try {
                            startIntentSenderForResult(result.getPendingIntent().getIntentSender(),
                                    AUTHORIZE_GOOGLE_PHOTOS, null, 0, 0, 0);
                        } catch (IntentSender.SendIntentException error) {
                            showPickerError("Could not open Google authorization");
                        }
                    } else {
                        createGooglePhotosPickerSession(result);
                    }
                })
                .addOnFailureListener(error -> showPickerError(
                        "Google Photos authorization is unavailable"));
    }

    private void createGooglePhotosPickerSession(AuthorizationResult authorization) {
        String accessToken = authorization.getAccessToken();
        if (accessToken == null || accessToken.isBlank()) {
            showPickerError("Google Photos did not return access permission");
            return;
        }
        Toast.makeText(this, "Opening your Google Photos library…", Toast.LENGTH_SHORT).show();
        new Thread(() -> {
            try {
                GooglePhotosPickerApi.PickerSession session =
                        GooglePhotosPickerApi.createSession(accessToken);
                pickerAccessToken = accessToken;
                pickerSessionId = session.id();
                new Handler(Looper.getMainLooper()).post(() -> {
                    startActivity(PickerBrowserIntentFactory.create(
                            Uri.parse(session.pickerUri())));
                });
                pollForPickedOriginal();
            } catch (Exception error) {
                resetGooglePhotosAction("Could not start the Google Photos Picker");
            }
        }, "google-photos-picker-session").start();
    }

    private void pollForPickedOriginal() {
        new Thread(() -> {
            try {
                for (int attempt = 0; attempt < 100; attempt++) {
                    if (GooglePhotosPickerApi.selectionIsComplete(
                            pickerAccessToken, pickerSessionId)) {
                        List<GooglePhotosPickerApi.PickedMedia> media = GooglePhotosPickerApi.allMedia(
                                pickerAccessToken, pickerSessionId);
                        new Handler(Looper.getMainLooper()).post(() ->
                                importCompletedGoogleSelection(() -> showPickedPhotos(media)));
                        return;
                    }
                    Thread.sleep(3_000);
                }
                resetGooglePhotosAction("Google Photos selection timed out");
            } catch (Exception error) {
                resetGooglePhotosAction("Could not read the selected Google Photos item");
            }
        }, "google-photos-picker-poll").start();
    }

    void importCompletedGoogleSelection(Runnable importSelectedPhoto) {
        Toast.makeText(this, "Adding selected photos to review…", Toast.LENGTH_SHORT).show();
        importSelectedPhoto.run();
    }

    private void showPickedPhotos(List<GooglePhotosPickerApi.PickedMedia> selectedMedia) {
        new Thread(() -> {
            CloudPhotoBatchImporter.Result result = new CloudPhotoBatchImporter(this,
                    media -> GooglePhotosPickerApi.downloadDisplayBitmap(
                            pickerAccessToken, media)).importAll(
                            selectedMedia, System.currentTimeMillis());
            if (result.importedCount() == 0) {
                resetGooglePhotosAction("Could not load the selected Google Photos photos");
                return;
            }
            new Handler(Looper.getMainLooper()).post(() -> {
                if (hasLocalPhotoAccess()) loadRecentPhotos(); else loadImportedPhotos();
                ImageView image = new ImageView(this);
                int padding = Math.round(20 * getResources().getDisplayMetrics().density);
                image.setPadding(padding, padding, padding, padding);
                image.setAdjustViewBounds(true);
                image.setImageBitmap(result.preview());
                showPickedPhotoDialog(image, result.importedCount(), result.selectedCount());
            });
        }, "google-photos-picker-image").start();
    }

    private void showPickedPhotoDialog(ImageView image, int importedCount, int selectedCount) {
        Dialog dialog = new Dialog(this);
        LinearLayout content = new LinearLayout(this);
        content.setOrientation(LinearLayout.VERTICAL);
        int spacing = Math.round(20 * getResources().getDisplayMetrics().density);
        content.setPadding(spacing, spacing, spacing, spacing);
        content.setBackgroundColor(getColor(R.color.gallery_surface));

        TextView title = new TextView(this);
        title.setText("Added to review");
        title.setTextColor(getColor(R.color.gallery_text_primary));
        title.setTextSize(21);
        title.setTypeface(null, android.graphics.Typeface.BOLD);
        content.addView(title);

        TextView message = new TextView(this);
        String count = importedCount + (importedCount == 1 ? " photo is" : " photos are");
        message.setText(count + " now in Keepers’ review flow. "
                + "Keepers saved only a private review copy; it did not upload or change "
                + "anything in Google Photos." + (importedCount == selectedCount ? ""
                : " " + (selectedCount - importedCount) + " could not be loaded."));
        message.setTextColor(getColor(R.color.gallery_text_secondary));
        message.setTextSize(14);
        LinearLayout.LayoutParams messageParams = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT);
        messageParams.topMargin = spacing / 2;
        content.addView(message, messageParams);
        content.addView(image, new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, 0, 1));

        TextView done = new TextView(this);
        done.setText("Done");
        done.setGravity(Gravity.CENTER);
        done.setTextColor(getColor(R.color.gallery_on_primary));
        done.setTextSize(15);
        done.setTypeface(null, android.graphics.Typeface.BOLD);
        done.setBackgroundResource(R.drawable.gallery_primary_action);
        done.setOnClickListener(view -> dialog.dismiss());
        LinearLayout.LayoutParams doneParams = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, Math.round(52
                * getResources().getDisplayMetrics().density));
        doneParams.topMargin = spacing / 2;
        content.addView(done, doneParams);

        dialog.setContentView(content);
        dialog.getWindow().setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
        dialog.show();
        dialog.getWindow().setLayout(Math.round(360 * getResources().getDisplayMetrics().density),
                Math.round(620 * getResources().getDisplayMetrics().density));
    }

    private void resetGooglePhotosAction(String error) {
        new Handler(Looper.getMainLooper()).post(() -> showPickerError(error));
    }

    private void showPickerError(String message) {
        Toast.makeText(this, message, Toast.LENGTH_LONG).show();
    }

    @Override protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == AUTHORIZE_GOOGLE_PHOTOS) {
            if (resultCode == RESULT_OK && data != null && photosAuthorization != null) {
                try {
                    createGooglePhotosPickerSession(
                            photosAuthorization.getAuthorizationResultFromIntent(data));
                } catch (Exception error) {
                    showPickerError("Google Photos authorization did not complete");
                }
            }
            return;
        }
        if (requestCode != IMPORT_PHOTOS || resultCode != RESULT_OK || data == null) return;
        ArrayList<Uri> picked = new ArrayList<>();
        ClipData clip = data.getClipData();
        if (clip != null) for (int index = 0; index < clip.getItemCount(); index++)
            picked.add(clip.getItemAt(index).getUri());
        else if (data.getData() != null) picked.add(data.getData());
        ImportedPhotoStore imports = new ImportedPhotoStore(this);
        long fallbackTime = System.currentTimeMillis();
        for (Uri pickerUri : picked) {
            try {
                getContentResolver().takePersistableUriPermission(pickerUri,
                        Intent.FLAG_GRANT_READ_URI_PERMISSION);
            } catch (SecurityException ignored) {}
            ImportedPhoto imported = resolvePickedPhoto(pickerUri, fallbackTime--);
            imports.add(imported);
        }
        loadRecentPhotos();
    }

    private ImportedPhoto resolvePickedPhoto(Uri pickerUri, long fallbackTime) {
        PhotoMetadata metadata = PhotoMetadataReader.read(getContentResolver(), pickerUri,
                fallbackTime);
        MediaType type = MediaType.from(pickerUri, metadata.mimeType());
        if ("media".equals(pickerUri.getAuthority())
                && !pickerUri.toString().contains("/picker/"))
            return new ImportedPhoto(pickerUri, fallbackTime, PhotoOrigin.LOCAL,
                    type, metadata.durationMillis());
        try (Cursor cursor = getContentResolver().query(pickerUri,
                new String[]{android.provider.CloudMediaProviderContract.MediaColumns.MEDIA_STORE_URI,
                        android.provider.CloudMediaProviderContract.MediaColumns.DATE_TAKEN_MILLIS},
                null, null, null)) {
            if (cursor != null && cursor.moveToFirst()) {
                String local = cursor.getString(0);
                long taken = cursor.isNull(1) ? fallbackTime : cursor.getLong(1);
                Uri resolved = local == null ? pickerUri : Uri.parse(local);
                return new ImportedPhoto(resolved, taken,
                        local == null ? PhotoOrigin.CLOUD : PhotoOrigin.LOCAL,
                        MediaType.from(resolved, getContentResolver().getType(resolved)),
                        metadata.durationMillis());
            }
        } catch (RuntimeException ignored) {}
        return new ImportedPhoto(pickerUri, fallbackTime, PhotoOrigin.CLOUD,
                type, metadata.durationMillis());
    }

    void loadNextPage() {
        reviewWindow.expand();
        findViewById(R.id.review_loading).setVisibility(View.VISIBLE);
        if (hasLocalPhotoAccess()) loadRecentPhotos();
    }

    private void fillFilteredViewport() {
        ScrollView scroll = findViewById(R.id.review_scroll);
        GridLayout grid = findViewById(R.id.photo_grid);
        String state = photos.size() + "|" + galleryFilter + "|" + originFilter
                + "|" + mediaFilter
                + "|" + grid.getChildCount();
        if (viewportLoadPending || !infiniteScroll.onContentLayout(scroll.getHeight(),
                grid.getHeight(), hasMorePhotos, state)) return;
        viewportLoadPending = true;
        grid.post(() -> {
            viewportLoadPending = false;
            if (hasMorePhotos) loadNextPage();
        });
    }

    int reviewLimit() { return reviewWindow.limit(); }

    private void updateSelectionDisplay() {
        Set<String> selected = selectionStore.load();
        AlbumCompletionStore completions = new AlbumCompletionStore(this);
        Set<String> visibleSelected = new HashSet<>();
        for (Uri photo : photos) if (selected.contains(photo.toString())) {
            visibleSelected.add(photo.toString());
        }
        for (FrameLayout tile : tiles) {
            boolean keeper = visibleSelected.contains(tile.getTag().toString());
            boolean saved = keeper && completions.hasAny(tile.getTag().toString());
            boolean suggested = suggestions.contains(tile.getTag().toString());
            boolean alternative = goodAlternatives.contains(tile.getTag().toString());
            tile.setAlpha(1f);
            ImageView heart = (ImageView) tile.getChildAt(1);
            heart.setImageResource(keeper ? R.drawable.ic_heart_filled
                    : R.drawable.ic_heart_outline);
            heart.setColorFilter(keeper && !saved
                    ? getColor(R.color.gallery_accent_keeper) : Color.WHITE);
            int heartPadding = dp(HeartIconStyle.paddingDp(keeper));
            heart.setPadding(heartPadding, heartPadding, heartPadding, heartPadding);
            heart.setVisibility(View.VISIBLE);
            ImageView star = (ImageView) tile.getChildAt(2);
            star.setImageResource(alternative ? R.drawable.ic_star_outline : R.drawable.ic_star);
            star.setBackground(suggested ? recommendationCircle() : null);
            star.setContentDescription(suggested ? "Recommended best shot" : alternative
                    ? "Good alternative — near-identical photo ranked higher" : null);
            star.setVisibility(suggested || alternative ? View.VISIBLE : View.GONE);
            String item = mediaTypes.getOrDefault(tile.getTag().toString(), MediaType.PHOTO)
                    == MediaType.VIDEO ? "Video" : "Photo";
            tile.setContentDescription(saved ? "Saved Keeper " + item.toLowerCase()
                    + ". Tap to remove."
                    : keeper ? "New Keeper " + item.toLowerCase() + ". Tap to remove."
                    : item + ". Tap to mark as keeper.");
        }
        ((TextView) findViewById(R.id.filter_keepers)).setText(
                "Keepers · " + selectionStore.load().size());
        applyFilter();
        updateMediaSelectionDisplay();
    }

    void showSuggestions(Set<String> recommended) {
        showSuggestions(recommended, Set.of());
    }

    void showSuggestions(Set<String> recommended, Set<String> alternatives) {
        suggestions = Set.copyOf(recommended);
        goodAlternatives = Set.copyOf(alternatives);
        new SuggestionStore(this).save(suggestions);
        new SuggestionStore(this).saveAlternatives(goodAlternatives);
        findViewById(R.id.analysis_progress_strip).setVisibility(View.INVISIBLE);
        updateSelectionDisplay();
    }

    void showAnalysisProgress(int completed, int total) {
        View strip = findViewById(R.id.analysis_progress_strip);
        android.widget.ProgressBar progress = findViewById(R.id.analysis_progress_bar);
        strip.setVisibility(View.VISIBLE);
        progress.setVisibility(View.VISIBLE);
        progress.setIndeterminate(total <= 0);
        progress.setMax(Math.max(1, total));
        progress.setProgress(Math.max(0, Math.min(completed, total)));
        ((TextView) findViewById(R.id.analysis_progress_label)).setText(
                "Analysing " + completed + " of " + total);
    }

    private GradientDrawable recommendationCircle() {
        GradientDrawable circle = new GradientDrawable();
        circle.setShape(GradientDrawable.OVAL);
        circle.setColor(getColor(R.color.gallery_accent_warm));
        return circle;
    }

    private GradientDrawable selectionCircle() {
        GradientDrawable circle = new GradientDrawable();
        circle.setShape(GradientDrawable.OVAL);
        circle.setColor(getColor(R.color.gallery_selection));
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
            badge.setText(position == null ? "" : Integer.toString(position.size()));
            badge.setContentDescription(position == null ? null
                    : "Stack of " + position.size() + " photos");
            badge.setVisibility(position == null ? View.GONE : View.VISIBLE);
        }
        applyFilter();
    }

    private void toggleFilter(GalleryFilter requested) {
        galleryFilter = galleryFilter == requested ? GalleryFilter.ALL : requested;
        applyFilter();
    }

    private void toggleMetadata() {
        metadataVisible = !metadataVisible;
        getSharedPreferences(GALLERY_DISPLAY_PREFERENCES, MODE_PRIVATE).edit()
                .putBoolean("metadata_visible", metadataVisible).apply();
        updateMetadataOverlays();
        updateFilterBadge();
    }

    private void beginMediaRangeSelection(String mediaId, View touchTarget) {
        List<String> visible = visibleMediaIds();
        rangeSelectionAnchor = visible.indexOf(mediaId);
        if (rangeSelectionAnchor < 0) return;
        rangeSelectionRemoving = selectingMedia && mediaSelections.contains(mediaId);
        rangeSelectionBaseline = new HashSet<>(mediaSelections);
        if (!selectingMedia) {
            selectingMedia = true;
            mediaSelections.clear();
            rangeSelectionBaseline = Set.of();
        }
        rangeSelectionGestureActive = true;
        rangeSelectionTouchOriginX = touchTarget.getLeft();
        rangeSelectionTouchOriginY = touchTarget.getTop();
        rangeSelectionScrollStep = 0;
        ((ScrollView) findViewById(R.id.review_scroll))
                .requestDisallowInterceptTouchEvent(true);
        updateMediaRangeSelection(mediaId);
    }

    private void updateMediaRangeSelection(String mediaId) {
        if (!rangeSelectionGestureActive) return;
        List<String> visible = visibleMediaIds();
        int current = visible.indexOf(mediaId);
        if (current < 0 || rangeSelectionAnchor >= visible.size()) return;
        mediaSelections.clear();
        mediaSelections.addAll(rangeSelectionBaseline);
        int first = Math.min(rangeSelectionAnchor, current);
        int last = Math.max(rangeSelectionAnchor, current);
        List<String> range = visible.subList(first, last + 1);
        if (rangeSelectionRemoving) mediaSelections.removeAll(range);
        else mediaSelections.addAll(range);
        if (mediaSelections.isEmpty()) {
            endMediaSelection();
            return;
        }
        updateMediaSelectionDisplay();
    }

    private List<String> visibleMediaIds() {
        GridLayout grid = findViewById(R.id.photo_grid);
        ArrayList<String> visible = new ArrayList<>();
        for (int index = 0; index < grid.getChildCount(); index++)
            visible.add(grid.getChildAt(index).getTag().toString());
        return visible;
    }

    private String visibleMediaAt(float viewportX, float viewportY) {
        ScrollView scroll = findViewById(R.id.review_scroll);
        GridLayout grid = findViewById(R.id.photo_grid);
        float contentX = viewportX + scroll.getScrollX();
        float contentY = viewportY + scroll.getScrollY();
        View nearest = null;
        double nearestDistance = Double.MAX_VALUE;
        for (int index = 0; index < grid.getChildCount(); index++) {
            View tile = grid.getChildAt(index);
            float left = tile.getLeft();
            float top = tile.getTop();
            if (contentX >= left && contentX < left + tile.getWidth()
                    && contentY >= top && contentY < top + tile.getHeight())
                return tile.getTag().toString();
            float deltaX = contentX - (left + tile.getWidth() / 2f);
            float deltaY = contentY - (top + tile.getHeight() / 2f);
            double distance = deltaX * deltaX + deltaY * deltaY;
            if (distance < nearestDistance) {
                nearest = tile;
                nearestDistance = distance;
            }
        }
        return nearest == null ? null : nearest.getTag().toString();
    }

    private void updateRangeSelectionAutoScroll() {
        ScrollView scroll = findViewById(R.id.review_scroll);
        if (scroll.getHeight() <= 0) {
            stopRangeSelectionAutoScroll();
            return;
        }
        float top = 0;
        float bottom = scroll.getHeight();
        float edge = Math.min(dp(RANGE_SCROLL_EDGE_DP), scroll.getHeight() / 2f);
        float intensity = 0;
        if (rangeSelectionPointerY < top + edge) {
            intensity = -Math.min(1, (top + edge - rangeSelectionPointerY) / edge);
        } else if (rangeSelectionPointerY > bottom - edge) {
            intensity = Math.min(1, (rangeSelectionPointerY - (bottom - edge)) / edge);
        }
        rangeSelectionScrollStep = Math.round(dp(RANGE_SCROLL_MAX_STEP_DP) * intensity);
        if (rangeSelectionScrollStep == 0) stopRangeSelectionAutoScroll();
        else scheduleRangeSelectionAutoScroll();
    }

    private void scheduleRangeSelectionAutoScroll() {
        if (rangeSelectionScrollScheduled) return;
        rangeSelectionScrollScheduled = true;
        mainHandler.postDelayed(rangeSelectionAutoScroll, RANGE_SCROLL_FRAME_MILLIS);
    }

    private void stopRangeSelectionAutoScroll() {
        rangeSelectionScrollStep = 0;
        mainHandler.removeCallbacks(rangeSelectionAutoScroll);
        rangeSelectionScrollScheduled = false;
    }

    private void finishMediaRangeSelection() {
        stopRangeSelectionAutoScroll();
        rangeSelectionGestureActive = false;
        rangeSelectionRemoving = false;
        rangeSelectionAnchor = -1;
        rangeSelectionBaseline = Set.of();
        ((ScrollView) findViewById(R.id.review_scroll))
                .requestDisallowInterceptTouchEvent(false);
    }

    private void completeMediaRangeSelection() {
        finishMediaRangeSelection();
        if (selectingMedia && mediaSelections.isEmpty()) {
            selectingMedia = false;
            updateSelectionDisplay();
        }
    }

    private void endMediaSelection() {
        finishMediaRangeSelection();
        selectingMedia = false;
        mediaSelections.clear();
        updateSelectionDisplay();
    }

    private void toggleMediaSelection(String mediaId) {
        if (!mediaSelections.add(mediaId)) mediaSelections.remove(mediaId);
        if (mediaSelections.isEmpty()) endMediaSelection();
        else updateMediaSelectionDisplay();
    }

    private void updateMediaSelectionDisplay() {
        TextView title = findViewById(R.id.gallery_title);
        TextView count = findViewById(R.id.selection_count);
        View cancel = findViewById(R.id.cancel_selection);
        View settings = findViewById(R.id.open_settings);
        View actions = findViewById(R.id.gallery_selection_actions);
        View navigation = findViewById(R.id.gallery_bottom_navigation);
        View controls = findViewById(R.id.gallery_controls);
        title.setVisibility(selectingMedia ? View.GONE : View.VISIBLE);
        count.setVisibility(selectingMedia ? View.VISIBLE : View.GONE);
        cancel.setVisibility(selectingMedia ? View.VISIBLE : View.GONE);
        settings.setVisibility(selectingMedia ? View.GONE : View.VISIBLE);
        actions.setVisibility(selectingMedia ? View.VISIBLE : View.GONE);
        navigation.setVisibility(selectingMedia ? View.GONE : View.VISIBLE);
        controls.setVisibility(selectingMedia ? View.INVISIBLE : View.VISIBLE);
        count.setText(mediaSelections.size() + " selected");
        for (FrameLayout tile : tiles) {
            String mediaId = tile.getTag().toString();
            boolean selected = selectingMedia
                    && mediaSelections.contains(mediaId);
            ImageView image = (ImageView) tile.getChildAt(0);
            tile.setBackgroundColor(selected ? getColor(R.color.gallery_surface_elevated)
                    : Color.TRANSPARENT);
            boolean selectionChanged = selected != renderedMediaSelections.contains(mediaId);
            if (selectionChanged) {
                if (selected) renderedMediaSelections.add(mediaId);
                else renderedMediaSelections.remove(mediaId);
                float targetScale = selected ? .84f : 1f;
                image.animate().cancel();
                image.animate().scaleX(targetScale).scaleY(targetScale)
                        .setDuration(SELECTION_SCALE_ANIMATION_MILLIS)
                        .setInterpolator(new android.view.animation.DecelerateInterpolator())
                        .start();
            }
            TextView check = tile.findViewWithTag("hide_selection_check");
            check.setVisibility(selected ? View.VISIBLE : View.GONE);
            if (selectingMedia) {
                tile.getChildAt(1).setVisibility(View.GONE);
                tile.getChildAt(2).setVisibility(View.GONE);
            }
        }
    }

    private void hideSelectedMedia() {
        if (mediaSelections.isEmpty()) return;
        new HiddenPhotoStore(this).hide(Set.copyOf(mediaSelections));
        endMediaSelection();
    }

    private void shareSelectedMedia() {
        if (mediaSelections.isEmpty()) return;
        List<Uri> selected = photos.stream().filter(photo ->
                mediaSelections.contains(photo.toString())).toList();
        List<MediaType> types = selected.stream().map(photo ->
                mediaTypes.getOrDefault(photo.toString(), MediaType.PHOTO)).toList();
        Intent share = MediaShareIntentFactory.create(selected, types);
        startActivity(Intent.createChooser(share,
                selected.size() == 1 ? "Share photo" : "Share media"));
        endMediaSelection();
    }

    private void keepSelectedMedia() {
        markSelectedAsKeepers();
        endMediaSelection();
    }

    private void addSelectedMediaToAlbums() {
        if (mediaSelections.isEmpty()) return;
        ArrayList<String> selected = new ArrayList<>();
        for (Uri photo : photos)
            if (mediaSelections.contains(photo.toString())) selected.add(photo.toString());
        markSelectedAsKeepers();
        startActivity(new Intent(this, AlbumReviewActivity.class)
                .putStringArrayListExtra(AlbumReviewActivity.EXTRA_SELECTED_MEDIA, selected));
        endMediaSelection();
    }

    private void markSelectedAsKeepers() {
        if (mediaSelections.isEmpty()) return;
        HashSet<String> keepers = new HashSet<>(selectionStore.load());
        keepers.addAll(mediaSelections);
        selectionStore.replace(keepers);
        RecommendationFeedbackStore feedbackStore = new RecommendationFeedbackStore(this);
        for (String mediaId : mediaSelections) {
            PhotoFeatures measured = features.stream().filter(item ->
                    item.id().equals(mediaId)).findFirst().orElse(null);
            if (measured == null) measured = new PhotoInsightStore(this).loadFeatures(mediaId);
            if (measured == null) continue;
            RecommendationFeedback previous = feedbackStore.load(mediaId);
            feedbackStore.save(RecommendationFeedback.from(measured,
                    RecommendationFeedback.LOVED,
                    previous == null ? "" : previous.comment()));
        }
        AlbumApprovalInvalidator.invalidate(this);
        refreshRecommendationsIfReady();
    }

    private void refreshRecommendationsIfReady() {
        if (features.isEmpty() || !mediaAnalysisQueue.photosComplete()
                || pendingSemanticContexts > 0) return;
        finalizePhotoInsights();
    }

    private RecommendationInputs currentRecommendationInputs() {
        return new RecommendationInputs(Set.copyOf(selectionStore.load()),
                Set.copyOf(new HiddenPhotoStore(this).load()),
                Set.copyOf(new HashSet<>(new RecommendationFeedbackStore(this).load())),
                new FaceGroupAssignmentStore(this).load(),
                new FaceCorrectionStore(this).load());
    }

    private void updateMetadataOverlays() {
        PhotoInsightStore insights = new PhotoInsightStore(this);
        PhotoContextStore contexts = new PhotoContextStore(this);
        HashMap<String, PhotoContext> visibleContexts = new HashMap<>();
        for (FrameLayout tile : tiles) {
            PhotoContext context = contexts.load(tile.getTag().toString());
            if (context != null) visibleContexts.put(tile.getTag().toString(), context);
        }
        RecommendationPreferenceProfile displayProfile =
                recommendationProfile.withContexts(visibleContexts);
        for (FrameLayout tile : tiles) {
            TextView overlay = tile.findViewWithTag("metadata_overlay");
            PhotoFeatures photo = insights.loadFeatures(tile.getTag().toString());
            if (photo == null) photo = features.stream()
                    .filter(item -> item.id().equals(tile.getTag().toString()))
                    .findFirst().orElse(null);
            if (mediaTypes.getOrDefault(tile.getTag().toString(), MediaType.PHOTO)
                    == MediaType.VIDEO) {
                VideoFeatures video = new VideoInsightStore(this)
                        .load(tile.getTag().toString());
                overlay.setText(video == null ? "Video  " + MediaDuration.format(
                        mediaDurations.getOrDefault(tile.getTag().toString(), 0L))
                        : GalleryMetadataOverlay.topSignals(video, 3));
            } else {
                PhotoContext context = contexts.load(tile.getTag().toString());
                overlay.setText(photo == null ? "Analysing…"
                        : GalleryMetadataOverlay.topSignals(photo,
                                context == null ? PhotoContext.general() : context,
                                displayProfile, 3));
            }
            overlay.setVisibility(metadataVisible ? View.VISIBLE : View.GONE);
        }
        View toggle = findViewById(R.id.toggle_metadata);
        toggle.setSelected(metadataVisible);
        toggle.setContentDescription(metadataVisible
                ? "Hide photo rating overlays" : "Show photo rating overlays");
    }

    private void setOriginFilter(PhotoOrigin requested) {
        originFilter = requested;
        applyFilter();
    }

    private void setMediaFilter(MediaFilter requested) {
        mediaFilter = requested;
        applyFilter();
    }

    private void applyFilter() {
        Set<String> keepers = selectionStore.load();
        AlbumCompletionStore completions = new AlbumCompletionStore(this);
        Set<String> kept = new HashSet<>();
        for (String keeper : keepers) if (completions.hasAny(keeper)) kept.add(keeper);
        Set<String> hidden = new HiddenPhotoStore(this).load();
        GridLayout grid = findViewById(R.id.photo_grid);
        grid.removeAllViews();
        boolean showingHidden = galleryFilter == GalleryFilter.HIDDEN;
        List<String> orderedIds = photos.stream().map(Uri::toString)
                .filter(id -> hidden.contains(id) == showingHidden).toList();
        HashMap<String, List<String>> visibleStacks = new HashMap<>();
        stackMembers.forEach((id, members) -> {
            if (hidden.contains(id) == showingHidden) visibleStacks.put(id, members.stream()
                    .filter(member -> hidden.contains(member) == showingHidden).toList());
        });
        List<String> stackCovers = StackPresentation.visibleIds(
                orderedIds, visibleStacks, suggestions, keepers);
        Map<String, FrameLayout> tilesById = new HashMap<>();
        for (FrameLayout tile : tiles) tilesById.put(tile.getTag().toString(), tile);
        int visible = 0;
        List<String> typeFiltered;
        if (showingHidden) {
            typeFiltered = stackCovers;
        } else if (galleryFilter == GalleryFilter.INCLUDE_KEEPERS) {
            typeFiltered = stackCovers;
        } else if (galleryFilter == GalleryFilter.ALL) {
            typeFiltered = stackCovers.stream().filter(id -> {
                List<String> stack = stackMembers.get(id);
                return stack == null ? !kept.contains(id)
                        : stack.stream().noneMatch(kept::contains);
            }).toList();
        } else {
            typeFiltered = orderedIds.stream().filter(id -> galleryFilter == GalleryFilter.KEEPERS
                    ? keepers.contains(id) : suggestions.contains(id)).toList();
        }
        List<String> originFiltered = PhotoOriginFilter.apply(
                typeFiltered, photoOrigins, originFilter);
        List<String> visibleIds = originFiltered.stream().filter(id -> mediaFilter == MediaFilter.ALL
                || mediaFilter == MediaFilter.PHOTOS
                && mediaTypes.getOrDefault(id, MediaType.PHOTO) == MediaType.PHOTO
                || mediaFilter == MediaFilter.VIDEOS
                && mediaTypes.getOrDefault(id, MediaType.PHOTO) == MediaType.VIDEO).toList();
        for (String id : visibleIds) {
            FrameLayout tile = tilesById.get(id);
            if (tile != null) {
                resetGridPlacement(tile);
                grid.addView(tile);
            }
            if (tile != null) visible++;
        }
        View keeperFilter = findViewById(R.id.filter_keepers);
        View recommendedFilter = findViewById(R.id.filter_recommended);
        findViewById(R.id.filter_include_keepers).setSelected(
                galleryFilter == GalleryFilter.INCLUDE_KEEPERS);
        keeperFilter.setSelected(galleryFilter == GalleryFilter.KEEPERS);
        recommendedFilter.setSelected(galleryFilter == GalleryFilter.RECOMMENDED);
        findViewById(R.id.filter_hidden).setSelected(showingHidden);
        findViewById(R.id.filter_origin_all).setSelected(originFilter == null);
        findViewById(R.id.filter_origin_local).setSelected(originFilter == PhotoOrigin.LOCAL);
        findViewById(R.id.filter_origin_cloud).setSelected(originFilter == PhotoOrigin.CLOUD);
        findViewById(R.id.filter_media_all).setSelected(mediaFilter == MediaFilter.ALL);
        findViewById(R.id.filter_photos).setSelected(mediaFilter == MediaFilter.PHOTOS);
        findViewById(R.id.filter_videos).setSelected(mediaFilter == MediaFilter.VIDEOS);
        findViewById(R.id.filter_keepers).setSelected(galleryFilter == GalleryFilter.KEEPERS);
        updateFilterBadge();
        updateMetadataOverlays();
        TextView empty = findViewById(R.id.review_empty);
        if (!photos.isEmpty() && visible == 0) {
            empty.setText(showingHidden ? "No hidden photos."
                    : originFilter == PhotoOrigin.CLOUD ? "No cloud photos imported yet."
                    : originFilter == PhotoOrigin.LOCAL ? "No local photos in this view."
                    : mediaFilter == MediaFilter.VIDEOS ? "No videos in this view."
                    : mediaFilter == MediaFilter.PHOTOS ? "No photos in this view."
                    : galleryFilter == GalleryFilter.ALL ? "No media left to review."
                    : galleryFilter == GalleryFilter.INCLUDE_KEEPERS
                    ? "No visible media."
                    : galleryFilter == GalleryFilter.KEEPERS
                    ? "No Keepers in the loaded photos yet."
                    : "No recommended photos in the loaded photos yet.");
            empty.setVisibility(View.VISIBLE);
        } else if (!photos.isEmpty()) {
            empty.setVisibility(View.GONE);
        }
    }

    private void updateFilterBadge() {
        int active = 0;
        if (galleryFilter == GalleryFilter.RECOMMENDED
                || galleryFilter == GalleryFilter.HIDDEN
                || galleryFilter == GalleryFilter.INCLUDE_KEEPERS) active++;
        if (originFilter != null) active++;
        if (mediaFilter != MediaFilter.ALL) active++;
        if (metadataVisible) active++;
        TextView badge = findViewById(R.id.gallery_filter_badge);
        badge.setText(String.valueOf(active));
        badge.setVisibility(active == 0 ? View.GONE : View.VISIBLE);
        findViewById(R.id.open_gallery_filters).setContentDescription(active == 0
                ? "Open gallery filters" : "Open gallery filters, " + active + " active");
    }

    @Override protected void onResume() {
        super.onResume();
        if (!photos.isEmpty()) {
            updateSelectionDisplay();
            RecommendationInputs currentInputs = currentRecommendationInputs();
            if (lastRecommendationInputs != null
                    && !lastRecommendationInputs.equals(currentInputs))
                refreshRecommendationsIfReady();
            lastRecommendationInputs = currentRecommendationInputs();
        }
    }

    private int dp(int value) {
        return Math.round(value * getResources().getDisplayMetrics().density);
    }

    @Override protected void onDestroy() {
        stopRangeSelectionAutoScroll();
        mainHandler.removeCallbacks(deferredRecommendationRefresh);
        analysisGeneration++;
        mediaAnalysisQueue.clear();
        if (videoAnalysisExecutor != null) videoAnalysisExecutor.shutdownNow();
        photoContextAnalyzer.close();
        faceAnalyzer.close();
        thumbnailLoader.close();
        super.onDestroy();
    }
}
