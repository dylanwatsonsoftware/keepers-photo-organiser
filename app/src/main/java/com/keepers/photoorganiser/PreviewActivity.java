package com.keepers.photoorganiser;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Intent;
import android.graphics.Color;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.GradientDrawable;
import android.media.MediaPlayer;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.widget.FrameLayout;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.HorizontalScrollView;
import android.widget.LinearLayout;
import android.widget.SeekBar;
import android.widget.TextView;
import android.widget.Toast;
import android.widget.GridLayout;
import android.widget.VideoView;
import android.graphics.Bitmap;
import android.view.MotionEvent;
import android.view.Gravity;
import android.view.ScaleGestureDetector;
import android.view.View;
import android.view.ViewGroup;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Comparator;
import java.util.Set;
import java.time.ZoneId;
import java.util.Locale;

public final class PreviewActivity extends Activity {
    public static final String EXTRA_QUICK_REVIEW = "quick_review";
    public static final String EXTRA_MEDIA_TYPE = "media_type";
    public static final String EXTRA_AUTOPLAY_VIDEO = "autoplay_video";
    private static final int QUICK_REVIEW_THRESHOLD_DP = 96;
    private static final long VIDEO_AUTOPLAY_DELAY_MS = 300;
    private static final long VIDEO_PROGRESS_UPDATE_MS = 250;
    private static final String ADD_NEW_PERSON = "__add_new_person__";
    private AsyncThumbnailLoader loader;
    private VideoFirstFrameLoader videoFirstFrameLoader;
    private KeeperSelectionStore store;
    private Uri photo;
    private PhotoNavigator navigator;
    private GestureCoordinates photoGesture;
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
    private GestureCoordinates analysisGesture;
    private boolean analysisPulling;
    private boolean analysisSwiping;
    private final PhotoZoomState zoomState = new PhotoZoomState();
    private ScaleGestureDetector scaleGestureDetector;
    private boolean zoomGestureInProgress;
    private float lastPanRawX;
    private float lastPanRawY;
    private boolean quickReview;
    private View previewQuickReview;
    private Map<String, RecentPhoto> mediaDetails = Map.of();
    private boolean photoChromeVisible = true;
    private GestureCoordinates stackCarouselGesture;
    private List<Uri> stackCarouselMembers = List.of();
    private final Handler playbackHandler = new Handler(Looper.getMainLooper());
    private Runnable pendingVideoAutoplay;
    private Runnable videoProgressUpdate;

    @Override protected void onCreate(Bundle state) {
        super.onCreate(state);
        setContentView(R.layout.activity_preview);
        photo = getIntent().getData();
        quickReview = getIntent().getBooleanExtra(EXTRA_QUICK_REVIEW, false);
        store = new KeeperSelectionStore(this);
        suggestionStore = new SuggestionStore(this);
        loader = AsyncThumbnailLoader.forResolver(getContentResolver());
        videoFirstFrameLoader = new VideoFirstFrameLoader(this);
        int limit = getIntent().getIntExtra(ReviewActivity.EXTRA_REVIEW_LIMIT,
                ReviewWindow.PAGE_SIZE);
        ArrayList<RecentPhoto> galleryPhotos = new ArrayList<>(
                RecentCameraQuery.loadRecent(getContentResolver(), limit));
        for (ImportedPhoto imported : new ImportedPhotoStore(this).load())
            galleryPhotos.add(new RecentPhoto(imported.uri(), imported.takenAtMillis(),
                    imported.mediaType(), imported.durationMillis()));
        String requestedType = getIntent().getStringExtra(EXTRA_MEDIA_TYPE);
        if (requestedType != null) {
            try {
                MediaType type = MediaType.valueOf(requestedType);
                galleryPhotos.removeIf(item -> item.mediaType() != type);
            } catch (IllegalArgumentException ignored) {}
        }
        galleryPhotos.sort(Comparator.comparingLong(RecentPhoto::takenAtMillis).reversed());
        ArrayList<Uri> photos = new ArrayList<>();
        HashSet<String> seenPhotos = new HashSet<>();
        HashMap<String, RecentPhoto> details = new HashMap<>();
        for (RecentPhoto recent : galleryPhotos) if (seenPhotos.add(recent.uri().toString())) {
            photos.add(recent.uri());
            details.put(recent.uri().toString(), recent);
        }
        mediaDetails = Map.copyOf(details);
        Set<String> hiddenPhotos = new HiddenPhotoStore(this).load();
        photos.removeIf(candidate -> hiddenPhotos.contains(candidate.toString())
                && (quickReview || !candidate.equals(photo)));
        if (photos.isEmpty() && (!quickReview || !hiddenPhotos.contains(photo.toString())))
            photos.add(photo);
        if (photos.isEmpty()) {
            Toast.makeText(this, "No visible photos to review", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }
        allPhotos = List.copyOf(photos);
        navigator = stackNavigator(photo, false);
        photo = navigator.current();
        setIntent(PreviewPageRequest.forPhoto(getIntent(), photo));
        frontImage = findViewById(R.id.preview_image);
        adjacentImage = findViewById(R.id.preview_adjacent_image);
        currentSurface = findViewById(R.id.preview_current_surface);
        adjacentSurface = findViewById(R.id.preview_adjacent_surface);
        pages = new CarouselPagePair(currentSurface, frontImage, adjacentSurface, adjacentImage);
        if (quickReview) configureQuickReviewCards();
        analysisSheet = findViewById(R.id.preview_analysis_sheet);
        analysisSheet.setOnTouchListener((view, event) -> handleAnalysisScroll(event));
        previewStage = findViewById(R.id.preview_stage);
        previewControls = findViewById(R.id.preview_controls);
        previewClose = findViewById(R.id.preview_close);
        findViewById(R.id.preview_stack_carousel).setOnTouchListener(
                (view, event) -> handleStackCarouselTouch(event));
        scaleGestureDetector = new ScaleGestureDetector(this,
                new ScaleGestureDetector.SimpleOnScaleGestureListener() {
                    @Override public boolean onScaleBegin(ScaleGestureDetector detector) {
                        zoomGestureInProgress = true;
                        currentSurface.animate().cancel();
                        adjacentSurface.animate().cancel();
                        adjacentSurface.setVisibility(View.INVISIBLE);
                        return true;
                    }

                    @Override public boolean onScale(ScaleGestureDetector detector) {
                        zoomState.scaleBy(detector.getScaleFactor(),
                                detector.getFocusX(), detector.getFocusY(),
                                frontImage.getWidth(), frontImage.getHeight());
                        applyZoom();
                        return true;
                    }
                });
        previewStage.setOnTouchListener((view, event) -> handleSwipe(event));
        findViewById(R.id.preview_close).setOnClickListener(view -> finish());
        previewQuickReview = findViewById(R.id.preview_start_quick_review);
        previewQuickReview.setVisibility(quickReview ? View.GONE : View.VISIBLE);
        previewQuickReview.setOnClickListener(view -> startActivity(
                PreviewPageRequest.forPhoto(getIntent(), photo)
                        .putExtra(EXTRA_QUICK_REVIEW, true)));
        loadCurrent();
        findViewById(R.id.preview_keeper).setOnClickListener(view -> {
            boolean selected = store.toggle(photo);
            recordHeartFeedback(selected);
            updateButton();
            showStackCarousel();
        });
        View hide = findViewById(R.id.preview_hide);
        hide.setOnClickListener(view -> hideCurrentPhoto());
        findViewById(R.id.preview_hide_icon).setVisibility(quickReview ? View.VISIBLE : View.GONE);
        View skip = findViewById(R.id.preview_skip);
        skip.setVisibility(quickReview ? View.VISIBLE : View.GONE);
        skip.setOnClickListener(view -> skipCurrentPhoto());
        findViewById(R.id.preview_feedback).setOnClickListener(view -> showFeedbackDialog());
        if (quickReview) ((TextView) findViewById(R.id.preview_hint)).setText(
                "Swipe right to keep  ·  Swipe left to pass  ·  Skip leaves unchanged");
        if (quickReview && !navigator.peekNext().equals(photo))
            showDragPreview(navigator.peekNext());
        updateButton();
    }

    private void updateButton() {
        ((TextView) findViewById(R.id.preview_keeper)).setText(store.load().contains(photo.toString())
                ? "♥ Keeper" : "♡ Keeper");
    }

    private void hideCurrentPhoto() {
        String hiddenId = photo.toString();
        HiddenPhotoStore hiddenStore = new HiddenPhotoStore(this);
        hiddenStore.hide(Set.of(hiddenId));
        Set<String> hidden = hiddenStore.load();
        ArrayList<Uri> remaining = new ArrayList<>(allPhotos);
        int hiddenIndex = remaining.indexOf(photo);
        remaining.removeIf(candidate -> hidden.contains(candidate.toString()));
        if (remaining.isEmpty()) {
            finish();
            return;
        }
        allPhotos = List.copyOf(remaining);
        Uri requested = remaining.get(Math.min(Math.max(0, hiddenIndex), remaining.size() - 1));
        navigator = stackNavigator(requested, false);
        photo = navigator.current();
        setIntent(PreviewPageRequest.forPhoto(getIntent(), photo));
        loadCurrent();
        updateButton();
        if (analysisSheet.getVisibility() == View.VISIBLE) showAnalysis();
    }

    private void skipCurrentPhoto() {
        Uri next = navigator.peekNext();
        if (next.equals(photo)) {
            Toast.makeText(this, "Quick review complete", Toast.LENGTH_SHORT).show();
            return;
        }
        photo = navigator.next();
        setIntent(PreviewPageRequest.forPhoto(getIntent(), photo));
        loadCurrent();
        updateButton();
        configureQuickReviewCards();
        Uri following = navigator.peekNext();
        if (!following.equals(photo)) showDragPreview(following);
    }

    private void recordHeartFeedback(boolean selected) {
        PhotoFeatures features = new PhotoInsightStore(this).loadFeatures(photo.toString());
        if (features == null) return;
        RecommendationFeedbackStore feedback = new RecommendationFeedbackStore(this);
        RecommendationFeedback previous = feedback.load(photo.toString());
        String comment = previous == null ? "" : previous.comment();
        feedback.save(RecommendationFeedback.from(features, selected
                ? RecommendationFeedback.LOVED : RecommendationFeedback.NOT_FOR_ME, comment));
    }

    private void showFeedbackDialog() {
        PhotoFeatures features = new PhotoInsightStore(this).loadFeatures(photo.toString());
        if (features == null) return;
        RecommendationFeedbackStore feedbackStore = new RecommendationFeedbackStore(this);
        RecommendationFeedback existing = feedbackStore.load(photo.toString());
        LinearLayout content = new LinearLayout(this);
        content.setOrientation(LinearLayout.VERTICAL);
        content.setPadding(dp(22), dp(8), dp(22), dp(10));
        TextView prompt = new TextView(this);
        prompt.setText("Was this a good recommendation?");
        prompt.setTextSize(17);
        prompt.setTextColor(0xFF202124);
        prompt.setPadding(0, dp(4), 0, dp(10));
        content.addView(prompt);
        LinearLayout choices = new LinearLayout(this);
        choices.setOrientation(LinearLayout.HORIZONTAL);
        TextView loved = feedbackChoice("♥ Love this", RecommendationFeedback.LOVED,
                existing);
        TextView rejected = feedbackChoice("Not for me", RecommendationFeedback.NOT_FOR_ME,
                existing);
        choices.addView(loved, new LinearLayout.LayoutParams(0, dp(44), 1));
        LinearLayout.LayoutParams rejectedParams = new LinearLayout.LayoutParams(0, dp(44), 1);
        rejectedParams.setMarginStart(dp(8));
        choices.addView(rejected, rejectedParams);
        content.addView(choices);
        EditText comment = new EditText(this);
        comment.setHint("Optional: tell us why");
        comment.setSingleLine(false);
        comment.setMinLines(2);
        comment.setText(existing == null ? "" : existing.comment());
        comment.setTag("recommendation_feedback_comment");
        content.addView(comment, new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT));
        TextView save = new TextView(this);
        save.setText("Save feedback");
        save.setTextColor(0xFFFFFFFF);
        save.setTypeface(android.graphics.Typeface.DEFAULT, android.graphics.Typeface.BOLD);
        save.setGravity(android.view.Gravity.CENTER);
        save.setBackgroundResource(R.drawable.gallery_primary_action);
        save.setTag("save_recommendation_feedback");
        LinearLayout.LayoutParams saveParams = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, dp(48));
        saveParams.topMargin = dp(12);
        content.addView(save, saveParams);
        AlertDialog dialog = new AlertDialog.Builder(this)
                .setTitle("Improve recommendations").setView(content).create();
        final int[] rating = { existing == null ? RecommendationFeedback.LOVED : existing.rating() };
        loved.setOnClickListener(view -> { rating[0] = RecommendationFeedback.LOVED;
            loved.setSelected(true); rejected.setSelected(false); });
        rejected.setOnClickListener(view -> { rating[0] = RecommendationFeedback.NOT_FOR_ME;
            loved.setSelected(false); rejected.setSelected(true); });
        save.setOnClickListener(view -> {
            feedbackStore.save(RecommendationFeedback.from(features, rating[0],
                    comment.getText().toString()));
            dialog.dismiss();
        });
        dialog.show();
    }

    private TextView feedbackChoice(String label, int rating, RecommendationFeedback existing) {
        TextView choice = new TextView(this);
        choice.setText(label);
        choice.setGravity(android.view.Gravity.CENTER);
        choice.setTextColor(getColorStateList(R.color.gallery_filter_text));
        choice.setBackgroundResource(R.drawable.gallery_filter_chip);
        choice.setClickable(true);
        choice.setFocusable(true);
        choice.setSelected(existing != null && existing.rating() == rating);
        return choice;
    }

    private boolean handleAnalysisScroll(MotionEvent event) {
        if (event.getAction() == MotionEvent.ACTION_DOWN) {
            analysisGesture = new GestureCoordinates(event.getRawX(), event.getRawY());
            analysisPulling = false;
            analysisSwiping = false;
            return false;
        }
        float deltaX = analysisGesture == null ? 0 : analysisGesture.deltaX(event.getRawX());
        float pull = analysisGesture == null ? 0 : analysisGesture.deltaY(event.getRawY());
        if (event.getAction() == MotionEvent.ACTION_MOVE) {
            if (analysisSwiping || AnalysisGestureRouting.isHorizontalPageSwipe(
                    deltaX, pull, dp(8))) {
                analysisSwiping = true;
                analysisSheet.animate().cancel();
                analysisSheet.setTranslationX(deltaX);
                analysisSheet.setAlpha(Math.max(.72f, 1f - Math.abs(deltaX)
                        / Math.max(1f, analysisSheet.getWidth()) * .28f));
                return true;
            }
            if (analysisSheet.getScrollY() > 0 || pull <= dp(4)) return false;
            analysisPulling = true;
            AnalysisSheetTransform transform = AnalysisSheetTransform.fromOpenPull(pull,
                    previewStage.getHeight(), dp(72));
            setPhotoChromeTranslation(transform.photoTranslationY());
            analysisSheet.setTranslationY(transform.sheetTranslationY());
            return true;
        }
        if (analysisSwiping) {
            if (event.getAction() == MotionEvent.ACTION_UP
                    && AnalysisGestureRouting.isHorizontalPageSwipe(deltaX, pull, dp(64))) {
                Uri target = deltaX < 0 ? navigator.peekNext() : navigator.peekPrevious();
                if (!target.equals(photo)) animateAnalysisPageChange(target, deltaX);
                else resetAnalysisHorizontalPosition();
            } else if (event.getAction() == MotionEvent.ACTION_UP)
                resetAnalysisHorizontalPosition();
            if (event.getAction() == MotionEvent.ACTION_CANCEL)
                resetAnalysisHorizontalPosition();
            if (event.getAction() == MotionEvent.ACTION_UP
                    || event.getAction() == MotionEvent.ACTION_CANCEL) analysisSwiping = false;
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
        scaleGestureDetector.onTouchEvent(event);
        int action = event.getActionMasked();
        if (event.getPointerCount() > 1
                || action == MotionEvent.ACTION_POINTER_DOWN
                || action == MotionEvent.ACTION_POINTER_UP
                || zoomGestureInProgress) {
            if (action == MotionEvent.ACTION_UP || action == MotionEvent.ACTION_CANCEL) {
                zoomGestureInProgress = false;
            }
            return true;
        }
        if (!zoomState.allowsPageGesture()) {
            if (action == MotionEvent.ACTION_DOWN) {
                frontImage.animate().cancel();
                lastPanRawX = event.getRawX();
                lastPanRawY = event.getRawY();
            } else if (action == MotionEvent.ACTION_MOVE) {
                float rawX = event.getRawX();
                float rawY = event.getRawY();
                zoomState.panBy(rawX - lastPanRawX, rawY - lastPanRawY,
                        frontImage.getWidth(), frontImage.getHeight());
                lastPanRawX = rawX;
                lastPanRawY = rawY;
                applyZoom();
            }
            return true;
        }
        if (event.getAction() == MotionEvent.ACTION_DOWN) {
            image.animate().cancel();
            adjacentSurface.animate().cancel();
            adjacentSurface.setVisibility(View.INVISIBLE);
            dragPreviewPhoto = null;
            analysisDragStarted = false;
            analysisWasOpen = analysisSheet.getVisibility() == View.VISIBLE;
            photoGesture = new GestureCoordinates(event.getRawX(), event.getRawY());
            if (quickReview) hideQuickReviewIndicators();
            // Once open, let the analysis ScrollView handle its long factor breakdown.
            return AnalysisGestureRouting.handleAsPhotoGesture(analysisWasOpen);
        }
        if (!AnalysisGestureRouting.handleAsPhotoGesture(analysisWasOpen)) return false;
        if (event.getAction() == MotionEvent.ACTION_MOVE) {
            float deltaX = photoGesture.deltaX(event.getRawX());
            float deltaY = photoGesture.deltaY(event.getRawY());
            if (!quickReview && !analysisWasOpen && deltaY < 0
                    && Math.abs(deltaY) > Math.abs(deltaX)
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
                showDragPreview(quickReview ? navigator.peekNext()
                        : deltaX < 0 ? navigator.peekNext() : navigator.peekPrevious());
                if (quickReview) {
                    QuickReviewCardTransform card = QuickReviewCardTransform.from(deltaX,
                            previewStage.getWidth());
                    image.setTranslationX(card.translationX());
                    image.setTranslationY(card.translationY());
                    image.setRotation(card.rotation());
                    image.setAlpha(1);
                    adjacentSurface.setTranslationX(0);
                    adjacentSurface.setTranslationY(dp(Math.round(card.nextTranslationY())));
                    adjacentSurface.setScaleX(card.nextScale());
                    adjacentSurface.setScaleY(card.nextScale());
                    adjacentSurface.setAlpha(card.nextAlpha());
                    showQuickReviewIndicator(deltaX);
                    return true;
                }
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
        float releaseDeltaX = photoGesture.deltaX(event.getRawX());
        float releaseDeltaY = photoGesture.deltaY(event.getRawY());
        if (analysisDragStarted) {
            resetPosition(image);
            if (AnalysisSheetTransform.shouldOpen(releaseDeltaY, dp(24))) openAnalysis();
            else hideAnalysis();
            return true;
        }
        float releaseThreshold = dp(quickReview ? QUICK_REVIEW_THRESHOLD_DP : 64);
        SwipeDirection direction = SwipeDirection.classify(
                releaseDeltaX, releaseDeltaY, releaseThreshold);
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
            if (!quickReview) {
                showAnalysis();
                openAnalysis();
            }
            return true;
        }
        if (direction == SwipeDirection.NONE) {
            resetPosition(image);
            if (!quickReview && analysisSheet.getVisibility() != View.VISIBLE
                    && Math.abs(releaseDeltaX) <= dp(8)
                    && Math.abs(releaseDeltaY) <= dp(8)) togglePhotoChrome();
            return true;
        }
        if (quickReview && (direction == SwipeDirection.NEXT
                || direction == SwipeDirection.PREVIOUS)) {
            applyQuickReviewDecision(QuickReviewDecision.fromSwipe(
                    releaseDeltaX, dp(QUICK_REVIEW_THRESHOLD_DP)));
            Uri target = navigator.peekNext();
            if (target.equals(photo)) {
                resetPosition(image);
                Toast.makeText(this, "Quick review complete", Toast.LENGTH_SHORT).show();
            } else {
                float exit = releaseDeltaX > 0
                        ? previewStage.getWidth() + dp(80) : -previewStage.getWidth() - dp(80);
                photo = navigator.next();
                setIntent(PreviewPageRequest.forPhoto(getIntent(), photo));
                adjacentSurface.animate().scaleX(1).scaleY(1).translationY(0).alpha(1)
                        .setDuration(180).start();
                image.animate().translationX(exit)
                        .translationY(Math.abs(exit) * .025f)
                        .rotation(releaseDeltaX > 0 ? 14 : -14).alpha(.35f)
                        .setDuration(180).withEndAction(() -> {
                            hideQuickReviewIndicators();
                            promoteAdjacentPage();
                            configureQuickReviewCards();
                            Uri next = navigator.peekNext();
                            if (!next.equals(photo)) showDragPreview(next);
                        }).start();
            }
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

    private void applyQuickReviewDecision(QuickReviewDecision decision) {
        boolean selected = store.load().contains(photo.toString());
        boolean shouldSelect = decision == QuickReviewDecision.KEEP;
        if (decision == QuickReviewDecision.NONE) return;
        if (selected != shouldSelect) store.toggle(photo);
        recordHeartFeedback(shouldSelect);
        updateButton();
    }

    private void showQuickReviewIndicator(float deltaX) {
        findViewById(R.id.quick_review_mode_badge).setVisibility(View.GONE);
        TextView keep = findViewById(R.id.quick_review_keep_indicator);
        TextView pass = findViewById(R.id.quick_review_pass_indicator);
        TextView active = deltaX > 0 ? keep : pass;
        TextView inactive = deltaX > 0 ? pass : keep;
        inactive.setVisibility(View.GONE);
        float progress = Math.min(1, Math.abs(deltaX) / dp(QUICK_REVIEW_THRESHOLD_DP));
        active.setAlpha(.38f + .62f * progress);
        active.setScaleX(.88f + .12f * progress);
        active.setScaleY(.88f + .12f * progress);
        active.setVisibility(View.VISIBLE);
    }

    private void hideQuickReviewIndicators() {
        findViewById(R.id.quick_review_keep_indicator).setVisibility(View.GONE);
        findViewById(R.id.quick_review_pass_indicator).setVisibility(View.GONE);
        findViewById(R.id.quick_review_mode_badge).setVisibility(
                quickReview ? View.VISIBLE : View.GONE);
    }

    private void configureQuickReviewCards() {
        configureQuickReviewCard(currentSurface, dp(12), 0xFF202124, 0xCCFFD38A);
        configureQuickReviewCard(adjacentSurface, dp(6), 0xFF3C4043, 0x996C727A);
        findViewById(R.id.quick_review_mode_badge).setVisibility(View.VISIBLE);
        currentSurface.setScaleX(1);
        currentSurface.setScaleY(1);
        currentSurface.setRotation(0);
        currentSurface.setAlpha(1);
        adjacentSurface.setScaleX(.96f);
        adjacentSurface.setScaleY(.96f);
        adjacentSurface.setTranslationX(0);
        adjacentSurface.setTranslationY(dp(24));
        adjacentSurface.setRotation(0);
        adjacentSurface.setAlpha(.72f);
    }

    private void configureQuickReviewCard(FrameLayout card, float elevation, int color,
            int outlineColor) {
        FrameLayout.LayoutParams params = (FrameLayout.LayoutParams) card.getLayoutParams();
        params.setMargins(dp(26), dp(82), dp(26), dp(190));
        card.setLayoutParams(params);
        GradientDrawable background = new GradientDrawable();
        background.setColor(color);
        background.setCornerRadius(dp(24));
        card.setBackground(background);
        GradientDrawable outline = new GradientDrawable();
        outline.setColor(Color.TRANSPARENT);
        outline.setCornerRadius(dp(24));
        outline.setStroke(dp(2), outlineColor);
        card.setForegroundGravity(Gravity.FILL);
        card.setForeground(outline);
        card.setClipToOutline(true);
        card.setElevation(elevation);
    }

    private void promoteAdjacentPage() {
        pages.promoteAdjacent();
        currentSurface = pages.currentSurface();
        frontImage = pages.currentImage();
        adjacentSurface = pages.adjacentSurface();
        adjacentImage = pages.adjacentImage();
        VideoView previousVideo = adjacentSurface.findViewWithTag("video_surface");
        previousVideo.stopPlayback();
        resetZoom();
        dragPreviewPhoto = null;
        loadCurrent();
        updateButton();
        if (analysisSheet.getVisibility() == View.VISIBLE) showAnalysis();
    }

    private void loadCurrent() {
        resetZoom();
        cancelPendingVideoAutoplay();
        cancelVideoProgressUpdates();
        boolean autoplayVideo = getIntent().getBooleanExtra(EXTRA_AUTOPLAY_VIDEO, false);
        getIntent().removeExtra(EXTRA_AUTOPLAY_VIDEO);
        VideoView video = currentSurface.findViewWithTag("video_surface");
        View play = currentSurface.findViewWithTag("video_play");
        int screen = Math.max(getResources().getDisplayMetrics().widthPixels,
                getResources().getDisplayMetrics().heightPixels);
        findViewById(R.id.preview_hide).setContentDescription(
                mediaTypeOf(photo) == MediaType.VIDEO ? "Hide video" : "Hide photo");
        if (mediaTypeOf(photo) == MediaType.VIDEO) {
            ImageView videoThumbnail = frontImage;
            if (!photo.equals(videoThumbnail.getTag())) videoThumbnail.setImageDrawable(null);
            videoThumbnail.setVisibility(View.VISIBLE);
            videoThumbnail.setContentDescription("First frame of video");
            videoFirstFrameLoader.load(videoThumbnail, photo, screen, firstFrame -> {
                if (firstFrame == null) loader.load(videoThumbnail, photo, screen);
            });
            video.setVideoURI(photo);
            video.setVisibility(View.VISIBLE);
            RecentPhoto videoDetails = mediaDetails.get(photo.toString());
            configureVideoTimeline(video, videoDetails == null
                    ? 0 : videoDetails.durationMillis());
            video.setOnInfoListener((player, what, extra) -> {
                if (what == MediaPlayer.MEDIA_INFO_VIDEO_RENDERING_START)
                    videoThumbnail.setVisibility(View.GONE);
                return false;
            });
            if (play != null) {
                play.setVisibility(photoChromeVisible ? View.VISIBLE : View.INVISIBLE);
                setVideoControlState(play, false);
                play.setOnClickListener(view -> {
                    if (video.isPlaying()) {
                        video.pause();
                        cancelVideoProgressUpdates();
                        setVideoControlState(play, false);
                    } else {
                        cancelPendingVideoAutoplay();
                        startVideoPlayback(video, videoThumbnail, play);
                    }
                });
            }
            video.setOnPreparedListener(player -> {
                player.setLooping(false);
                if (player.getDuration() > 0) setVideoTimelineDuration(player.getDuration());
            });
            video.setOnCompletionListener(player -> {
                cancelVideoProgressUpdates();
                setVideoTimelinePosition(((SeekBar) findViewById(
                        R.id.preview_video_seek)).getMax());
                videoThumbnail.setVisibility(View.VISIBLE);
                if (play != null) {
                    setVideoControlState(play, false);
                    play.setVisibility(photoChromeVisible ? View.VISIBLE : View.INVISIBLE);
                }
            });
            if (autoplayVideo) {
                Uri autoplayTarget = photo;
                pendingVideoAutoplay = () -> {
                    pendingVideoAutoplay = null;
                    if (autoplayTarget.equals(photo) && currentSurface
                            .findViewWithTag("video_surface") == video)
                        startVideoPlayback(video, videoThumbnail, play);
                };
                playbackHandler.postDelayed(pendingVideoAutoplay, VIDEO_AUTOPLAY_DELAY_MS);
            }
            updateRecommendation();
            showStackCarousel();
            return;
        }
        video.stopPlayback();
        video.setVisibility(View.GONE);
        if (play != null) play.setVisibility(View.GONE);
        findViewById(R.id.preview_video_timeline).setVisibility(View.GONE);
        frontImage.setVisibility(View.VISIBLE);
        PreviewImageSizes sizes = PreviewImageSizes.forScreen(screen);
        loader.loadProgressive(frontImage, photo, sizes.previewPixels(), sizes.fullPixels(), bitmap -> {
            if (bitmap != null) frontImage.setVisibility(View.VISIBLE);
        });
        updateRecommendation();
        showStackCarousel();
    }

    private void startVideoPlayback(VideoView video, ImageView thumbnail, View play) {
        thumbnail.setVisibility(View.GONE);
        video.start();
        startVideoProgressUpdates(video);
        if (play != null) setVideoControlState(play, true);
    }

    private void configureVideoTimeline(VideoView video, long durationMillis) {
        View timeline = findViewById(R.id.preview_video_timeline);
        SeekBar seek = findViewById(R.id.preview_video_seek);
        timeline.setVisibility(View.VISIBLE);
        setVideoTimelineDuration(durationMillis);
        setVideoTimelinePosition(0);
        seek.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override public void onProgressChanged(SeekBar bar, int progress,
                    boolean fromUser) {
                if (!fromUser) return;
                video.seekTo(progress);
                setVideoTimelinePosition(progress);
            }

            @Override public void onStartTrackingTouch(SeekBar bar) {
                cancelVideoProgressUpdates();
            }

            @Override public void onStopTrackingTouch(SeekBar bar) {
                if (video.isPlaying()) startVideoProgressUpdates(video);
            }
        });
    }

    private void setVideoTimelineDuration(long durationMillis) {
        int duration = (int) Math.min(Integer.MAX_VALUE, Math.max(1, durationMillis));
        ((SeekBar) findViewById(R.id.preview_video_seek)).setMax(duration);
        ((TextView) findViewById(R.id.preview_video_duration)).setText(
                MediaDuration.format(durationMillis));
    }

    private void setVideoTimelinePosition(int positionMillis) {
        ((SeekBar) findViewById(R.id.preview_video_seek)).setProgress(positionMillis);
        ((TextView) findViewById(R.id.preview_video_elapsed)).setText(
                MediaDuration.format(positionMillis));
    }

    private void startVideoProgressUpdates(VideoView video) {
        cancelVideoProgressUpdates();
        videoProgressUpdate = new Runnable() {
            @Override public void run() {
                if (currentSurface.findViewWithTag("video_surface") != video) return;
                setVideoTimelinePosition(video.getCurrentPosition());
                if (video.isPlaying()) playbackHandler.postDelayed(
                        this, VIDEO_PROGRESS_UPDATE_MS);
                else videoProgressUpdate = null;
            }
        };
        playbackHandler.post(videoProgressUpdate);
    }

    private void cancelVideoProgressUpdates() {
        if (videoProgressUpdate == null) return;
        playbackHandler.removeCallbacks(videoProgressUpdate);
        videoProgressUpdate = null;
    }

    private void cancelPendingVideoAutoplay() {
        if (pendingVideoAutoplay == null) return;
        playbackHandler.removeCallbacks(pendingVideoAutoplay);
        pendingVideoAutoplay = null;
    }

    private static void setVideoControlState(View control, boolean playing) {
        ((ImageButton) control).setImageResource(
                playing ? R.drawable.ic_pause : R.drawable.ic_play);
        control.setContentDescription(playing ? "Pause video" : "Play video");
    }

    private void updateRecommendation() {
        TextView recommendation = findViewById(R.id.preview_recommendation);
        boolean photoMedia = mediaTypeOf(photo) == MediaType.PHOTO;
        boolean recommended = photoMedia && suggestionStore.load().contains(photo.toString());
        boolean alternative = photoMedia
                && suggestionStore.loadAlternatives().contains(photo.toString());
        recommendation.setText(recommended ? "★  Best shot"
                : alternative ? "☆  Good alternative" : "");
        recommendation.setVisibility(recommended || alternative ? View.VISIBLE : View.INVISIBLE);
    }

    private void showStackCarousel() {
        List<String> members = new PhotoStackStore(this).load(photo.toString());
        Set<String> hidden = new HiddenPhotoStore(this).load();
        members = members.stream().filter(member -> !hidden.contains(member)
                || (!quickReview && member.equals(photo.toString()))).toList();
        HorizontalScrollView carousel = findViewById(R.id.preview_stack_carousel);
        LinearLayout thumbnails = findViewById(R.id.preview_stack_thumbnails);
        thumbnails.removeAllViews();
        stackCarouselMembers = members.stream().map(Uri::parse).toList();
        if (members.size() < 2) {
            carousel.setVisibility(View.GONE);
            return;
        }
        Set<String> keepers = store.load();
        AlbumCompletionStore completions = new AlbumCompletionStore(this);
        Set<String> recommendations = suggestionStore.load();
        Set<String> alternatives = suggestionStore.loadAlternatives();
        int selectedIndex = 0;
        for (int index = 0; index < members.size(); index++) {
            String member = members.get(index);
            Uri memberUri = Uri.parse(member);
            boolean selected = memberUri.equals(photo);
            if (selected) selectedIndex = index;
            FrameLayout thumbnailFrame = StackThumbnailView.create(this, selected,
                    keepers.contains(member), completions.hasAny(member), recommendations.contains(member),
                    alternatives.contains(member));
            ImageView thumbnail = StackThumbnailView.image(thumbnailFrame);
            thumbnailFrame.setContentDescription(selected
                    ? "Current photo in stack" : "Show photo from stack");
            LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(dp(72), dp(72));
            thumbnails.addView(thumbnailFrame, params);
            loader.load(thumbnail, memberUri, 160);
            thumbnailFrame.setOnClickListener(view -> selectStackPhoto(memberUri));
        }
        carousel.setVisibility(View.VISIBLE);
        int targetIndex = selectedIndex;
        carousel.post(() -> carousel.smoothScrollTo(CarouselScrollTarget.centered(targetIndex,
                dp(72), carousel.getWidth(), thumbnails.getWidth()), 0));
    }

    private boolean handleStackCarouselTouch(MotionEvent event) {
        int action = event.getActionMasked();
        if (action == MotionEvent.ACTION_DOWN) {
            stackCarouselGesture = new GestureCoordinates(event.getRawX(), event.getRawY());
            return false;
        }
        if (action == MotionEvent.ACTION_CANCEL) {
            stackCarouselGesture = null;
            return false;
        }
        if (action != MotionEvent.ACTION_UP || stackCarouselGesture == null) return false;
        float deltaX = stackCarouselGesture.deltaX(event.getRawX());
        stackCarouselGesture = null;
        int currentIndex = stackCarouselMembers.indexOf(photo);
        if (currentIndex < 0) return false;
        int targetIndex = StackCarouselSwipe.targetIndex(currentIndex,
                stackCarouselMembers.size(), deltaX, dp(24), dp(72));
        if (targetIndex != currentIndex) {
            Uri target = stackCarouselMembers.get(targetIndex);
            findViewById(R.id.preview_stack_carousel).post(() -> selectStackPhoto(target));
        }
        return false;
    }

    private void selectStackPhoto(Uri selected) {
        if (selected.equals(photo)) return;
        resetZoom();
        photo = selected;
        navigator = stackNavigator(photo, true);
        setIntent(PreviewPageRequest.forPhoto(getIntent(), photo));
        loadCurrent();
        updateButton();
        if (analysisSheet.getVisibility() == View.VISIBLE) showAnalysis();
    }

    private PhotoNavigator stackNavigator(Uri current, boolean preserveCurrentMember) {
        List<String> orderedIds = allPhotos.stream().map(Uri::toString).toList();
        Set<String> orderedSet = Set.copyOf(orderedIds);
        HashMap<String, List<String>> stacks = new HashMap<>();
        PhotoStackStore stackStore = new PhotoStackStore(this);
        for (String id : orderedIds) {
            List<String> members = stackStore.load(id).stream()
                    .filter(orderedSet::contains).toList();
            if (!members.isEmpty()) stacks.put(id, members);
        }
        Set<String> recommendations = suggestionStore.load();
        Set<String> keepers = store.load();
        List<String> navigationIds = preserveCurrentMember
                ? StackPresentation.navigationIds(orderedIds, stacks, recommendations,
                        keepers, current.toString())
                : StackPresentation.visibleIds(orderedIds, stacks, recommendations, keepers);
        String requestedId = current.toString();
        List<String> currentStack = stacks.get(requestedId);
        if (!preserveCurrentMember && currentStack != null) requestedId = navigationIds.stream()
                .filter(currentStack::contains).findFirst().orElse(requestedId);
        List<Uri> navigation = navigationIds.stream().map(Uri::parse).toList();
        return new PhotoNavigator(navigation, Uri.parse(requestedId));
    }

    private void showAnalysis() {
        boolean opening = analysisSheet.getVisibility() != View.VISIBLE;
        previewQuickReview.setVisibility(View.GONE);
        showAnalysisFaces();
        showMetadata();
        showSavedAlbums();
        PhotoInsight insight = new PhotoInsightStore(this).load(photo.toString());
        TextView title = findViewById(R.id.preview_analysis_title);
        TextView body = findViewById(R.id.preview_analysis_body);
        if (mediaTypeOf(photo) == MediaType.VIDEO) {
            VideoFeatures video = new VideoInsightStore(this).load(photo.toString());
            if (video == null) {
                title.setText("Video analysis pending");
                body.setText("Videos are analysed after photos to keep the gallery responsive.");
            } else {
                VideoAssessment assessment = VideoAssessment.from(video);
                title.setText("Video assessment · " + assessment.score() + "/100");
                body.setText(assessment.explanation());
            }
            applyAssessmentIcon(title, 0);
        } else if (insight == null) {
            title.setText("Analysis pending");
            applyAssessmentIcon(title, 0);
            body.setText("This photo has not finished being analysed yet.");
        } else {
            title.setText(insight.recommended() ? "Recommended best shot"
                    : insight.goodAlternative() ? "Good alternative" : "Not recommended");
            applyAssessmentIcon(title, AssessmentStatusStyle.iconRes(
                    insight.recommended(), insight.goodAlternative()));
            String stack = insight.stack() == null ? "Distinct photo"
                    : "Photo " + insight.stack().position() + " of " + insight.stack().size()
                    + " in this detected stack";
            body.setText("Assessment " + insight.assessment().score() + "/100\n" + stack
                    + "\n" + insight.reason() + "\n\n" + insight.assessment().explanation());
        }
        View feedback = findViewById(R.id.preview_feedback);
        feedback.setEnabled(insight != null && mediaTypeOf(photo) == MediaType.PHOTO);
        feedback.setAlpha(feedback.isEnabled() ? 1f : .45f);
        if (opening) analysisSheet.setTranslationY(analysisRevealDistance());
        analysisSheet.setVisibility(View.VISIBLE);
    }

    private void showMetadata() {
        long fallbackTakenAt = 0;
        for (ImportedPhoto imported : new ImportedPhotoStore(this).load())
            if (imported.uri().equals(photo)) fallbackTakenAt = imported.takenAtMillis();
        PhotoMetadata metadata = PhotoMetadataReader.read(
                getContentResolver(), photo, fallbackTakenAt);
        showMetadataValue(R.id.preview_metadata_filename,
                metadata.filename().isBlank() ? "" : "File  ·  " + metadata.filename());
        showMetadataValue(R.id.preview_metadata_date,
                metadata.formattedDate(ZoneId.systemDefault(), Locale.getDefault()));
        showMetadataValue(R.id.preview_metadata_caption, metadata.caption());
        showMetadataValue(R.id.preview_metadata_location,
                metadata.location().isBlank() ? "" : "Location  ·  " + metadata.location());
        showMetadataValue(R.id.preview_metadata_technical,
                metadata.technicalSummary(Locale.getDefault()));
    }

    private void showMetadataValue(int viewId, String value) {
        TextView view = findViewById(viewId);
        view.setText(value);
        view.setVisibility(value == null || value.isBlank() ? View.GONE : View.VISIBLE);
    }

    private void showSavedAlbums() {
        List<String> albumNames = new AlbumCompletionStore(this).albumNames(photo.toString());
        View section = findViewById(R.id.preview_saved_albums_section);
        section.setVisibility(albumNames.isEmpty() ? View.GONE : View.VISIBLE);
        ((TextView) findViewById(R.id.preview_saved_albums)).setText(
                android.text.TextUtils.join("\n", albumNames));
    }

    private void showAnalysisFaces() {
        GridLayout grid = findViewById(R.id.preview_analysis_faces);
        TextView heading = findViewById(R.id.preview_analysis_faces_title);
        grid.removeAllViews();
        List<FaceDisplay> faces = resolveFaces(photo.toString());
        boolean hasFaces = !faces.isEmpty();
        grid.setVisibility(hasFaces ? View.VISIBLE : View.GONE);
        heading.setVisibility(hasFaces ? View.VISIBLE : View.GONE);
        for (FaceDisplay display : faces) grid.addView(faceCard(display));
    }

    private LinearLayout faceCard(FaceDisplay display) {
        LinearLayout card = new LinearLayout(this);
        card.setOrientation(LinearLayout.VERTICAL);
        card.setGravity(android.view.Gravity.CENTER_HORIZONTAL);
        GridLayout.LayoutParams params = new GridLayout.LayoutParams();
        params.width = dp(112);
        params.height = ViewGroup.LayoutParams.WRAP_CONTENT;
        params.setMargins(0, 0, dp(10), dp(12));
        card.setLayoutParams(params);
        card.setClickable(true);
        card.setFocusable(true);
        boolean knownPerson = display.personId() != null;
        card.setContentDescription(display.suggested() ? "Suggested " + display.name()
                + ". Confirm or change identity."
                : knownPerson ? "Open " + display.name() + " associated faces"
                : "Identify " + display.name());
        card.setOnClickListener(view -> {
            if (knownPerson) startActivity(new Intent(this, PersonDetailActivity.class)
                    .putExtra(PersonDetailActivity.EXTRA_PERSON_ID, display.personId()));
            else showFaceIdentityChooser(display.face());
        });
        ImageView crop = new ImageView(this);
        crop.setScaleType(ImageView.ScaleType.CENTER_CROP);
        crop.setBackgroundResource(R.drawable.preview_face_crop);
        crop.setClipToOutline(true);
        crop.setContentDescription("Expanded face crop for " + display.name());
        card.addView(crop, new LinearLayout.LayoutParams(dp(78), dp(78)));
        FaceObservation portrait = featurePortrait(display);
        if (portrait != display.face()) {
            loader.loadProgressive(crop, Uri.parse(portrait.photoId()),
                    FeaturePortrait.PREVIEW_PIXELS, FeaturePortrait.FULL_PIXELS,
                    bitmap -> crop.setImageBitmap(FeaturePortrait.crop(bitmap, portrait)));
        } else {
            loader.load(crop, Uri.parse(display.face().photoId()), 480,
                    bitmap -> showExpandedFaceCrop(crop, bitmap, display.face()));
        }
        TextView label = new TextView(this);
        label.setText(display.suggested() ? display.name() + "\nSuggested" : display.name());
        label.setTextColor(display.suggested() ? 0xFFB06000 : 0xFF3C4043);
        label.setTextSize(13);
        label.setGravity(android.view.Gravity.CENTER);
        label.setMaxLines(2);
        LinearLayout.LayoutParams labelParams = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        labelParams.setMargins(0, dp(6), 0, 0);
        card.addView(label, labelParams);
        if (display.suggested()) card.addView(faceSuggestionActions(display));
        return card;
    }

    private LinearLayout faceSuggestionActions(FaceDisplay display) {
        LinearLayout actions = new LinearLayout(this);
        actions.setOrientation(LinearLayout.HORIZONTAL);
        LinearLayout.LayoutParams actionParams = new LinearLayout.LayoutParams(
                0, dp(34), 1);
        actionParams.setMargins(0, dp(6), 0, 0);
        TextView confirm = faceSuggestionAction("Yes", "confirm_face_identity", true);
        confirm.setOnClickListener(view -> confirmFaceIdentity(display.face(), display.personId()));
        actions.addView(confirm, actionParams);
        TextView reject = faceSuggestionAction("No", "reject_face_identity", false);
        LinearLayout.LayoutParams rejectParams = new LinearLayout.LayoutParams(0, dp(34), 1);
        rejectParams.setMargins(dp(4), dp(6), 0, 0);
        reject.setOnClickListener(view -> rejectFaceIdentity(display.face(), display.personId()));
        actions.addView(reject, rejectParams);
        TextView change = faceSuggestionAction("Change", "change_face_identity", false);
        LinearLayout.LayoutParams changeParams = new LinearLayout.LayoutParams(0, dp(34), 1);
        changeParams.setMargins(dp(4), dp(6), 0, 0);
        change.setOnClickListener(view -> showFaceIdentityChooser(display.face()));
        actions.addView(change, changeParams);
        return actions;
    }

    private TextView faceSuggestionAction(String label, String tag, boolean primary) {
        TextView action = new TextView(this);
        action.setText(label);
        action.setTag(tag);
        action.setTextSize(12);
        action.setTypeface(android.graphics.Typeface.DEFAULT, android.graphics.Typeface.BOLD);
        action.setGravity(android.view.Gravity.CENTER);
        action.setTextColor(primary ? 0xFFFFFFFF : 0xFF3C4043);
        action.setBackgroundResource(primary ? R.drawable.gallery_primary_action
                : R.drawable.gallery_filter_chip);
        action.setClickable(true);
        action.setFocusable(true);
        return action;
    }

    private void confirmFaceIdentity(FaceObservation face, String personId) {
        FaceCorrectionStore store = new FaceCorrectionStore(this);
        HashMap<String, String> corrections = new HashMap<>(store.load());
        corrections.put(FaceCorrectionStore.key(face), personId);
        store.save(corrections);
        new FaceSuggestionRejectionStore(this).allow(FaceCorrectionStore.key(face), personId);
        AlbumApprovalInvalidator.invalidate(this);
        showAnalysisFaces();
    }

    private void rejectFaceIdentity(FaceObservation face, String personId) {
        new FaceSuggestionRejectionStore(this).reject(FaceCorrectionStore.key(face), personId);
        AlbumApprovalInvalidator.invalidate(this);
        showAnalysisFaces();
    }

    private FaceObservation featurePortrait(FaceDisplay display) {
        if (display.personId() == null) return display.face();
        String key = new PersonFeatureFaceStore(this).load(display.personId());
        FaceObservation feature = FeaturePortrait.resolve(display.personId(),
                new FaceObservationStore(this).loadAll(), Map.of(display.personId(), key));
        return feature == null ? display.face() : feature;
    }

    private void showFaceIdentityChooser(FaceObservation face) {
        List<TrackedPerson> people = new TrackedPersonStore(this).load();
        ArrayList<String> labels = new ArrayList<>();
        ArrayList<String> ids = new ArrayList<>();
        for (TrackedPerson person : people) {
            labels.add(displayName(person.name()));
            ids.add(person.id());
        }
        labels.add("Add a new person…");
        ids.add(ADD_NEW_PERSON);
        labels.add("Not someone I track");
        ids.add(FaceCorrectionStore.IGNORE);
        labels.add("Leave unconfirmed");
        ids.add("");
        new AlertDialog.Builder(this)
                .setTitle("Who is this?")
                .setSingleChoiceItems(labels.toArray(new String[0]), -1, (dialog, which) -> {
                    FaceCorrectionStore store = new FaceCorrectionStore(this);
                    HashMap<String, String> changed = new HashMap<>(store.load());
                    String key = FaceCorrectionStore.key(face);
                    String personId = ids.get(which);
                    if (ADD_NEW_PERSON.equals(personId)) {
                        dialog.dismiss();
                        showCreatePersonDialog(face);
                        return;
                    }
                    if (personId.isBlank()) changed.remove(key); else changed.put(key, personId);
                    store.save(changed);
                    AlbumApprovalInvalidator.invalidate(this);
                    dialog.dismiss();
                    showAnalysisFaces();
                })
                .setNegativeButton("Cancel", null)
                .show();
    }

    private void showCreatePersonDialog(FaceObservation face) {
        LinearLayout fields = new LinearLayout(this);
        fields.setOrientation(LinearLayout.VERTICAL);
        fields.setPadding(dp(24), 0, dp(24), 0);
        EditText name = new EditText(this);
        name.setTag("new_person_name");
        name.setHint("Name");
        name.setSingleLine(true);
        fields.addView(name, new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));
        EditText album = new EditText(this);
        album.setTag("new_person_album");
        album.setHint("Exact Google Photos album name (optional)");
        album.setSingleLine(true);
        fields.addView(album, new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));

        AlertDialog dialog = new AlertDialog.Builder(this)
                .setTitle("Add this person")
                .setMessage("This face will teach Keepers who to suggest in future photos.")
                .setView(fields)
                .setNegativeButton("Cancel", null)
                .create();
        TextView add = new TextView(this);
        add.setTag("add_new_person");
        add.setText("Add person");
        add.setTextColor(0xFFFFFFFF);
        add.setTextSize(15);
        add.setTypeface(null, android.graphics.Typeface.BOLD);
        add.setGravity(android.view.Gravity.CENTER);
        add.setBackgroundResource(R.drawable.gallery_primary_action);
        add.setClickable(true);
        add.setFocusable(true);
        LinearLayout.LayoutParams addParams = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, dp(48));
        addParams.setMargins(0, dp(16), 0, dp(8));
        fields.addView(add, addParams);
        add.setOnClickListener(view -> {
            String enteredName = name.getText().toString().trim();
            if (enteredName.isEmpty()) {
                name.setError("Enter a name");
                return;
            }
            createPersonForFace(face, enteredName, album.getText().toString().trim());
            dialog.dismiss();
        });
        dialog.show();
    }

    private void createPersonForFace(FaceObservation face, String name, String album) {
        TrackedPersonStore peopleStore = new TrackedPersonStore(this);
        ArrayList<TrackedPerson> people = new ArrayList<>(peopleStore.load());
        String personId = nextPersonId(people);
        people.add(new TrackedPerson(personId, name, album, true));
        peopleStore.save(people);

        String faceKey = FaceCorrectionStore.key(face);
        FaceCorrectionStore correctionStore = new FaceCorrectionStore(this);
        HashMap<String, String> corrections = new HashMap<>(correctionStore.load());
        corrections.put(faceKey, personId);
        correctionStore.save(corrections);
        new PersonFeatureFaceStore(this).save(personId, faceKey);
        AlbumApprovalInvalidator.invalidate(this);
        showAnalysisFaces();
    }

    private static String nextPersonId(List<TrackedPerson> people) {
        Set<String> ids = people.stream().map(TrackedPerson::id)
                .collect(java.util.stream.Collectors.toSet());
        int number = 1;
        while (ids.contains("person-" + number)) number++;
        return "person-" + number;
    }

    private List<FaceDisplay> resolveFaces(String photoId) {
        FaceObservationStore observations = new FaceObservationStore(this);
        List<FaceObservation> allFaces = observations.loadAll();
        List<FaceIdentityGroup> groups = FaceClusterer.cluster(allFaces, .30);
        Map<String, String> assignments = new FaceGroupAssignmentStore(this).load();
        Map<String, String> corrections = new FaceCorrectionStore(this).load();
        Map<String, String> learned = FaceIdentityLearner.predict(allFaces, groups, assignments,
                corrections, .15);
        FaceSuggestionRejectionStore rejections = new FaceSuggestionRejectionStore(this);
        Map<String, String> names = new TrackedPersonStore(this).load().stream().collect(
                java.util.stream.Collectors.toMap(TrackedPerson::id, TrackedPerson::name,
                        (first, ignored) -> first));
        return observations.load(photoId).stream()
                .sorted(Comparator.comparingDouble(FaceObservation::top)
                        .thenComparingDouble(FaceObservation::left))
                .map(face -> displayFor(face, groups, assignments, corrections, learned, names,
                        rejections))
                .toList();
    }

    private static FaceDisplay displayFor(FaceObservation face, List<FaceIdentityGroup> groups,
            Map<String, String> assignments, Map<String, String> corrections,
            Map<String, String> learned, Map<String, String> names,
            FaceSuggestionRejectionStore rejections) {
        String key = FaceCorrectionStore.key(face);
        String corrected = corrections.get(key);
        if (FaceCorrectionStore.IGNORE.equals(corrected))
            return new FaceDisplay(face, "Unknown", null, false);
        if (corrected != null && names.containsKey(corrected))
            return new FaceDisplay(face, displayName(names.get(corrected)), corrected, false);
        for (FaceIdentityGroup group : groups) if (group.members().stream()
                .anyMatch(member -> FaceCorrectionStore.key(member).equals(key))) {
            String assigned = FaceGroupAssignmentResolver.personFor(group, assignments);
            if (!assigned.isBlank() && names.containsKey(assigned))
                return new FaceDisplay(face, displayName(names.get(assigned)), assigned, false);
            break;
        }
        String predicted = learned.get(key);
        if (predicted != null && names.containsKey(predicted)
                && !rejections.isRejected(key, predicted))
            return new FaceDisplay(face, displayName(names.get(predicted)), predicted, true);
        return new FaceDisplay(face, "Unknown", null, false);
    }

    private static String displayName(String name) {
        return name == null || name.isBlank() ? "Unnamed person" : name;
    }

    private static void showExpandedFaceCrop(ImageView view, Bitmap bitmap, FaceObservation face) {
        if (bitmap == null) return;
        double width = face.right() - face.left();
        double height = face.bottom() - face.top();
        double horizontalMargin = width * .38;
        double verticalMargin = height * .38;
        int left = Math.max(0, (int) ((face.left() - horizontalMargin) * bitmap.getWidth()));
        int top = Math.max(0, (int) ((face.top() - verticalMargin) * bitmap.getHeight()));
        int right = Math.min(bitmap.getWidth(), Math.max(left + 1,
                (int) ((face.right() + horizontalMargin) * bitmap.getWidth())));
        int bottom = Math.min(bitmap.getHeight(), Math.max(top + 1,
                (int) ((face.bottom() + verticalMargin) * bitmap.getHeight())));
        view.setImageBitmap(Bitmap.createBitmap(bitmap, left, top, right - left, bottom - top));
    }

    private record FaceDisplay(FaceObservation face, String name, String personId,
            boolean suggested) {}

    private void applyAssessmentIcon(TextView title, int iconRes) {
        if (iconRes == 0) {
            title.setCompoundDrawablesRelative(null, null, null, null);
            return;
        }
        Drawable icon = getDrawable(iconRes).mutate();
        icon.setTint(0xFFB06000);
        int size = dp(23);
        icon.setBounds(0, 0, size, size);
        title.setCompoundDrawablePadding(dp(8));
        title.setCompoundDrawablesRelative(icon, null, null, null);
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
                    analysisSheet.setTranslationX(0);
                    analysisSheet.setAlpha(1);
                    previewQuickReview.setVisibility(quickReview ? View.GONE
                            : photoChromeVisible ? View.VISIBLE : View.INVISIBLE);
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
        VideoView adjacentVideo = adjacentSurface.findViewWithTag("video_surface");
        adjacentVideo.stopPlayback();
        adjacentVideo.setVisibility(View.GONE);
        View adjacentPlay = adjacentSurface.findViewWithTag("video_play");
        if (adjacentPlay != null) adjacentPlay.setVisibility(View.GONE);
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
        if (quickReview) {
            hideQuickReviewIndicators();
            adjacentSurface.animate().translationX(0).translationY(dp(12))
                    .scaleX(.94f).scaleY(.94f).alpha(.72f).setDuration(160).start();
            image.animate().translationX(0).translationY(0).rotation(0).alpha(1)
                    .setDuration(160).start();
            return;
        }
        float pageDistance = previewStage.getWidth() + dp(8);
        float adjacentRest = image.getTranslationX() < 0 ? pageDistance : -pageDistance;
        adjacentSurface.animate().translationX(adjacentRest).setDuration(140).start();
        image.animate().translationX(0).translationY(0).alpha(1).setDuration(140)
                .withEndAction(() -> adjacentSurface.setVisibility(View.INVISIBLE)).start();
    }

    private void animateAnalysisPageChange(Uri target, float deltaX) {
        float distance = Math.max(1, analysisSheet.getWidth());
        float exit = deltaX < 0 ? -distance : distance;
        analysisSheet.animate().translationX(exit).alpha(.72f).setDuration(120)
                .withEndAction(() -> {
                    selectStackPhoto(target);
                    analysisSheet.setTranslationX(-exit);
                    analysisSheet.animate().translationX(0).alpha(1).setDuration(140).start();
                }).start();
    }

    private void resetAnalysisHorizontalPosition() {
        analysisSheet.animate().translationX(0).alpha(1).setDuration(140).start();
    }

    private void togglePhotoChrome() {
        photoChromeVisible = !photoChromeVisible;
        int visibility = photoChromeVisible ? View.VISIBLE : View.INVISIBLE;
        previewClose.setVisibility(visibility);
        previewControls.setVisibility(visibility);
        previewQuickReview.setVisibility(visibility);
        View play = currentSurface.findViewWithTag("video_play");
        if (play != null && mediaTypeOf(photo) == MediaType.VIDEO)
            play.setVisibility(visibility);
    }

    private MediaType mediaTypeOf(Uri media) {
        RecentPhoto details = mediaDetails.get(media.toString());
        if (details != null) return details.mediaType();
        return MediaType.from(media, getContentResolver().getType(media));
    }

    private void applyZoom() {
        frontImage.setScaleX(zoomState.scale());
        frontImage.setScaleY(zoomState.scale());
        frontImage.setTranslationX(zoomState.translationX());
        frontImage.setTranslationY(zoomState.translationY());
    }

    private void resetZoom() {
        zoomState.reset();
        if (frontImage != null) applyZoom();
        zoomGestureInProgress = false;
    }

    private int dp(int value) {
        return Math.round(value * getResources().getDisplayMetrics().density);
    }

    @Override protected void onDestroy() {
        cancelPendingVideoAutoplay();
        cancelVideoProgressUpdates();
        VideoView currentVideo = currentSurface == null ? null
                : currentSurface.findViewWithTag("video_surface");
        VideoView adjacentVideo = adjacentSurface == null ? null
                : adjacentSurface.findViewWithTag("video_surface");
        if (currentVideo != null) currentVideo.stopPlayback();
        if (adjacentVideo != null) adjacentVideo.stopPlayback();
        loader.close();
        videoFirstFrameLoader.close();
        super.onDestroy();
    }

    @Override protected void onPause() {
        super.onPause();
        cancelPendingVideoAutoplay();
        cancelVideoProgressUpdates();
        VideoView video = currentSurface == null ? null
                : currentSurface.findViewWithTag("video_surface");
        if (video != null && video.isPlaying()) {
            video.pause();
            View play = currentSurface.findViewWithTag("video_play");
            if (play != null) setVideoControlState(play, false);
        }
    }
}
