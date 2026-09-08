package com.keepers.photoorganiser;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Intent;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.os.Bundle;
import android.widget.FrameLayout;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.HorizontalScrollView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.GridLayout;
import android.graphics.Bitmap;
import android.view.MotionEvent;
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

public final class PreviewActivity extends Activity {
    private static final String ADD_NEW_PERSON = "__add_new_person__";
    private AsyncThumbnailLoader loader;
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
    private final PhotoZoomState zoomState = new PhotoZoomState();
    private ScaleGestureDetector scaleGestureDetector;
    private boolean zoomGestureInProgress;
    private float lastPanRawX;
    private float lastPanRawY;

    @Override protected void onCreate(Bundle state) {
        super.onCreate(state);
        setContentView(R.layout.activity_preview);
        photo = getIntent().getData();
        store = new KeeperSelectionStore(this);
        suggestionStore = new SuggestionStore(this);
        loader = AsyncThumbnailLoader.forResolver(getContentResolver());
        int limit = getIntent().getIntExtra(ReviewActivity.EXTRA_REVIEW_LIMIT,
                ReviewWindow.PAGE_SIZE);
        ArrayList<RecentPhoto> galleryPhotos = new ArrayList<>(
                RecentCameraQuery.loadRecent(getContentResolver(), limit));
        for (ImportedPhoto imported : new ImportedPhotoStore(this).load())
            galleryPhotos.add(new RecentPhoto(imported.uri(), imported.takenAtMillis()));
        galleryPhotos.sort(Comparator.comparingLong(RecentPhoto::takenAtMillis).reversed());
        ArrayList<Uri> photos = new ArrayList<>();
        HashSet<String> seenPhotos = new HashSet<>();
        for (RecentPhoto recent : galleryPhotos) if (seenPhotos.add(recent.uri().toString()))
            photos.add(recent.uri());
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
        loadCurrent();
        findViewById(R.id.preview_keeper).setOnClickListener(view -> {
            store.toggle(photo);
            updateButton();
            showStackCarousel();
        });
        updateButton();
    }

    private void updateButton() {
        ((TextView) findViewById(R.id.preview_keeper)).setText(store.load().contains(photo.toString())
                ? "♥ Keeper — tap to remove" : "♡ Mark as keeper");
    }

    private boolean handleAnalysisScroll(MotionEvent event) {
        if (event.getAction() == MotionEvent.ACTION_DOWN) {
            analysisGesture = new GestureCoordinates(event.getRawX(), event.getRawY());
            analysisPulling = false;
            return false;
        }
        float pull = analysisGesture == null ? 0 : analysisGesture.deltaY(event.getRawY());
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
            // Once open, let the analysis ScrollView handle its long factor breakdown.
            return AnalysisGestureRouting.handleAsPhotoGesture(analysisWasOpen);
        }
        if (!AnalysisGestureRouting.handleAsPhotoGesture(analysisWasOpen)) return false;
        if (event.getAction() == MotionEvent.ACTION_MOVE) {
            float deltaX = photoGesture.deltaX(event.getRawX());
            float deltaY = photoGesture.deltaY(event.getRawY());
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
        float releaseDeltaX = photoGesture.deltaX(event.getRawX());
        float releaseDeltaY = photoGesture.deltaY(event.getRawY());
        if (analysisDragStarted) {
            resetPosition(image);
            if (AnalysisSheetTransform.shouldOpen(releaseDeltaY, dp(24))) openAnalysis();
            else hideAnalysis();
            return true;
        }
        SwipeDirection direction = SwipeDirection.classify(
                releaseDeltaX, releaseDeltaY, dp(64));
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
        resetZoom();
        dragPreviewPhoto = null;
        updateRecommendation();
        updateButton();
        showStackCarousel();
        if (analysisSheet.getVisibility() == View.VISIBLE) showAnalysis();
    }

    private void loadCurrent() {
        resetZoom();
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

    private void selectStackPhoto(Uri selected) {
        if (selected.equals(photo)) return;
        resetZoom();
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
        showSavedAlbums();
        PhotoInsight insight = new PhotoInsightStore(this).load(photo.toString());
        TextView title = findViewById(R.id.preview_analysis_title);
        TextView body = findViewById(R.id.preview_analysis_body);
        if (insight == null) {
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
        if (opening) analysisSheet.setTranslationY(analysisRevealDistance());
        analysisSheet.setVisibility(View.VISIBLE);
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
        params.width = dp(92);
        params.height = ViewGroup.LayoutParams.WRAP_CONTENT;
        params.setMargins(0, 0, dp(10), dp(12));
        card.setLayoutParams(params);
        card.setClickable(true);
        card.setFocusable(true);
        boolean knownPerson = display.personId() != null;
        card.setContentDescription(knownPerson ? "Open " + display.name() + " associated faces"
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
        return card;
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
        if (predicted != null && names.containsKey(predicted))
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
        loader.close();
        super.onDestroy();
    }
}
