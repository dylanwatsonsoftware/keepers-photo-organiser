package com.keepers.photoorganiser;

import android.app.Activity;
import android.app.AlertDialog;
import android.net.Uri;
import android.os.Bundle;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.HorizontalScrollView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.GridLayout;
import android.graphics.Bitmap;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Comparator;

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
        recommendation.setVisibility(recommended || alternative ? View.VISIBLE : View.INVISIBLE);
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
        int selectedIndex = 0;
        for (int index = 0; index < members.size(); index++) {
            String member = members.get(index);
            Uri memberUri = Uri.parse(member);
            boolean selected = memberUri.equals(photo);
            if (selected) selectedIndex = index;
            FrameLayout thumbnailFrame = StackThumbnailView.create(this, selected);
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
        showAnalysisFaces();
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
        params.width = dp(92);
        params.height = ViewGroup.LayoutParams.WRAP_CONTENT;
        params.setMargins(0, 0, dp(10), dp(12));
        card.setLayoutParams(params);
        card.setClickable(true);
        card.setFocusable(true);
        card.setContentDescription("Identify " + display.name());
        card.setOnClickListener(view -> showFaceIdentityChooser(display.face()));
        ImageView crop = new ImageView(this);
        crop.setScaleType(ImageView.ScaleType.CENTER_CROP);
        crop.setBackgroundResource(R.drawable.preview_face_crop);
        crop.setClipToOutline(true);
        crop.setContentDescription("Expanded face crop for " + display.name());
        card.addView(crop, new LinearLayout.LayoutParams(dp(78), dp(78)));
        loader.load(crop, Uri.parse(display.face().photoId()), 480,
                bitmap -> showExpandedFaceCrop(crop, bitmap, display.face()));
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
        return card;
    }

    private void showFaceIdentityChooser(FaceObservation face) {
        List<TrackedPerson> people = new TrackedPersonStore(this).load();
        ArrayList<String> labels = new ArrayList<>();
        ArrayList<String> ids = new ArrayList<>();
        for (TrackedPerson person : people) {
            labels.add(displayName(person.name()));
            ids.add(person.id());
        }
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
                    if (personId.isBlank()) changed.remove(key); else changed.put(key, personId);
                    store.save(changed);
                    AlbumApprovalInvalidator.invalidate(this);
                    dialog.dismiss();
                    showAnalysisFaces();
                })
                .setNegativeButton("Cancel", null)
                .show();
    }

    private List<FaceDisplay> resolveFaces(String photoId) {
        FaceObservationStore observations = new FaceObservationStore(this);
        List<FaceObservation> allFaces = observations.loadAll();
        List<FaceIdentityGroup> groups = FaceClusterer.cluster(allFaces, .30);
        Map<String, String> assignments = new FaceGroupAssignmentStore(this).load();
        Map<String, String> corrections = new FaceCorrectionStore(this).load();
        Map<String, String> learned = FaceIdentityLearner.predict(allFaces, groups, assignments,
                corrections, .15);
        Map<String, String> names = new TrackedPersonStore(this).load().stream().collect(
                java.util.stream.Collectors.toMap(TrackedPerson::id, TrackedPerson::name,
                        (first, ignored) -> first));
        return observations.load(photoId).stream()
                .sorted(Comparator.comparingDouble(FaceObservation::top)
                        .thenComparingDouble(FaceObservation::left))
                .map(face -> displayFor(face, groups, assignments, corrections, learned, names))
                .toList();
    }

    private static FaceDisplay displayFor(FaceObservation face, List<FaceIdentityGroup> groups,
            Map<String, String> assignments, Map<String, String> corrections,
            Map<String, String> learned, Map<String, String> names) {
        String key = FaceCorrectionStore.key(face);
        String corrected = corrections.get(key);
        if (FaceCorrectionStore.IGNORE.equals(corrected)) return new FaceDisplay(face, "Unknown", false);
        if (corrected != null && names.containsKey(corrected))
            return new FaceDisplay(face, displayName(names.get(corrected)), false);
        for (FaceIdentityGroup group : groups) if (group.members().stream()
                .anyMatch(member -> FaceCorrectionStore.key(member).equals(key))) {
            String assigned = assignments.get(group.id());
            if (assigned != null && names.containsKey(assigned))
                return new FaceDisplay(face, displayName(names.get(assigned)), false);
            break;
        }
        String predicted = learned.get(key);
        if (predicted != null && names.containsKey(predicted))
            return new FaceDisplay(face, displayName(names.get(predicted)), true);
        return new FaceDisplay(face, "Unknown", false);
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

    private record FaceDisplay(FaceObservation face, String name, boolean suggested) {}

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
