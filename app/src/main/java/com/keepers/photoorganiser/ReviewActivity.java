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
import android.provider.MediaStore;
import android.view.Gravity;
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

public final class ReviewActivity extends Activity {
    private static final int IMPORT_PHOTOS = 201;
    private static final int AUTHORIZE_GOOGLE_PHOTOS = 202;
    private static final String PHOTOS_PICKER_SCOPE =
            "https://www.googleapis.com/auth/photospicker.mediaitems.readonly";
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
    private PhotoOrigin originFilter;
    private Map<String, PhotoOrigin> photoOrigins = Map.of();
    private AuthorizationClient photosAuthorization;
    private String pickerAccessToken;
    private String pickerSessionId;

    @Override protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_review);
        selectionStore = new KeeperSelectionStore(this);
        thumbnailLoader = AsyncThumbnailLoader.forResolver(getContentResolver());
        faceAnalyzer = createFaceAnalyzer();
        findViewById(R.id.open_settings).setOnClickListener(view ->
                startActivity(new Intent(this, PeopleActivity.class)));
        findViewById(R.id.open_album_review).setOnClickListener(view ->
                startActivity(new Intent(this, AlbumReviewActivity.class)));
        findViewById(R.id.clear_keepers).setOnClickListener(view -> {
            selectionStore.clear();
            AlbumApprovalInvalidator.invalidate(this);
            updateSelectionDisplay();
        });
        findViewById(R.id.filter_keepers).setOnClickListener(view ->
                toggleFilter(GalleryFilter.KEEPERS));
        findViewById(R.id.filter_recommended).setOnClickListener(view ->
                toggleFilter(GalleryFilter.RECOMMENDED));
        findViewById(R.id.import_photos).setOnClickListener(view -> openPhotoPicker());
        findViewById(R.id.import_google_photos).setOnClickListener(view ->
                openGooglePhotosPicker());
        findViewById(R.id.filter_origin_all).setOnClickListener(view -> setOriginFilter(null));
        findViewById(R.id.filter_origin_local).setOnClickListener(view ->
                setOriginFilter(PhotoOrigin.LOCAL));
        findViewById(R.id.filter_origin_cloud).setOnClickListener(view ->
                setOriginFilter(PhotoOrigin.CLOUD));
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
        loadImportedPhotos();
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
        HashMap<String, PhotoOrigin> origins = new HashMap<>();
        for (Uri photo : recentPhotos) origins.put(photo.toString(), PhotoOrigin.LOCAL);
        showPhotos(recentPhotos, origins);
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
        else if (!appending) restoreCachedInsights();
        updateSelectionDisplay();
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
        tile.setContentDescription("Photo. Tap to mark as keeper.");
        tile.setOnClickListener(view -> startActivity(new Intent(this, PreviewActivity.class)
                .setData(photo).putExtra(EXTRA_REVIEW_LIMIT, reviewWindow.limit())));
        return tile;
    }

    private void finishPhotoAnalysis(int generation) {
        if (generation != analysisGeneration) return;
        analyzedCount++;
        if (analyzedCount == photos.size()) {
                RecommendationFeedbackStore feedbackStore = new RecommendationFeedbackStore(this);
                Set<String> keepers = selectionStore.load();
                for (PhotoFeatures feature : features) if (keepers.contains(feature.id())) {
                    RecommendationFeedback previous = feedbackStore.load(feature.id());
                    feedbackStore.save(RecommendationFeedback.from(feature,
                            RecommendationFeedback.LOVED,
                            previous == null ? "" : previous.comment()));
                }
                Map<String, PhotoStackPosition> stacks = BestShotEngine.stacks(features);
                Map<String, List<String>> members = BestShotEngine.stackMembers(features);
                RecommendationPreferenceProfile profile = RecommendationPreferenceProfile.learn(
                        feedbackStore.load());
                BestShotResult result = BestShotEngine.classify(features, profile);
                new PhotoStackStore(this).save(members);
                new PhotoInsightStore(this).save(features, stacks, result.recommended(),
                        result.goodAlternatives());
                showStacks(stacks, members);
                showSuggestions(result.recommended(), result.goodAlternatives());
        }
    }

    private void loadRecentPhotos() {
        List<RecentPhoto> local = RecentCameraQuery.loadRecent(
                getContentResolver(), reviewWindow.limit());
        hasMorePhotos = local.size() == reviewWindow.limit();
        showLocalAndImportedPhotos(local);
    }

    private void loadImportedPhotos() {
        hasMorePhotos = false;
        showLocalAndImportedPhotos(List.of());
    }

    private void showLocalAndImportedPhotos(List<RecentPhoto> local) {
        LinkedHashMap<String, RecentPhoto> combined = new LinkedHashMap<>();
        HashMap<String, PhotoOrigin> origins = new HashMap<>();
        for (RecentPhoto photo : local) {
            combined.put(photo.uri().toString(), photo);
            origins.put(photo.uri().toString(), PhotoOrigin.LOCAL);
        }
        for (ImportedPhoto imported : new ImportedPhotoStore(this).load()) {
            combined.put(imported.uri().toString(),
                    new RecentPhoto(imported.uri(), imported.takenAtMillis()));
            origins.put(imported.uri().toString(), imported.origin());
        }
        ArrayList<RecentPhoto> ordered = new ArrayList<>(combined.values());
        ordered.sort(Comparator.comparingLong(RecentPhoto::takenAtMillis).reversed());
        photoOrigins = Map.copyOf(origins);
        showRecentPhotos(ordered);
    }

    private void openPhotoPicker() {
        Intent picker = new Intent(MediaStore.ACTION_PICK_IMAGES)
                .setType("image/*")
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
        findViewById(R.id.import_google_photos).setEnabled(false);
        Toast.makeText(this, "Opening your Google Photos library…", Toast.LENGTH_SHORT).show();
        new Thread(() -> {
            try {
                GooglePhotosPickerApi.PickerSession session =
                        GooglePhotosPickerApi.createSession(accessToken);
                pickerAccessToken = accessToken;
                pickerSessionId = session.id();
                new Handler(Looper.getMainLooper()).post(() -> {
                    TextView action = findViewById(R.id.import_google_photos);
                    action.setText("Waiting for photo…");
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
                        GooglePhotosPickerApi.PickedMedia media = GooglePhotosPickerApi.firstMedia(
                                pickerAccessToken, pickerSessionId);
                        new Handler(Looper.getMainLooper()).post(() ->
                                importCompletedGoogleSelection(() -> showPickedPhoto(media)));
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
        TextView action = findViewById(R.id.import_google_photos);
        action.setEnabled(false);
        action.setText("Adding to review…");
        action.setContentDescription("Adding the selected Google Photos photo to review");
        importSelectedPhoto.run();
    }

    private void showPickedPhoto(GooglePhotosPickerApi.PickedMedia media) {
        TextView action = findViewById(R.id.import_google_photos);
        new Thread(() -> {
            try {
                Bitmap bitmap = GooglePhotosPickerApi.downloadDisplayBitmap(
                        pickerAccessToken, media);
                Uri reviewUri = new CloudPhotoCache(this).save(media.id(), bitmap);
                new ImportedPhotoStore(this).add(new ImportedPhoto(reviewUri,
                        System.currentTimeMillis(), PhotoOrigin.CLOUD));
                new Handler(Looper.getMainLooper()).post(() -> {
                    if (hasLocalPhotoAccess()) loadRecentPhotos(); else loadImportedPhotos();
                    ImageView image = new ImageView(this);
                    int padding = Math.round(20 * getResources().getDisplayMetrics().density);
                    image.setPadding(padding, padding, padding, padding);
                    image.setAdjustViewBounds(true);
                    image.setImageBitmap(bitmap);
                    showPickedPhotoDialog(image);
                    action.setEnabled(true);
                    action.setText("Google Photos");
                    action.setContentDescription("Select one photo from Google Photos");
                    action.setOnClickListener(view -> openGooglePhotosPicker());
                });
            } catch (Exception error) {
                resetGooglePhotosAction("Could not load the selected Google Photos image");
            }
        }, "google-photos-picker-image").start();
    }

    private void showPickedPhotoDialog(ImageView image) {
        Dialog dialog = new Dialog(this);
        LinearLayout content = new LinearLayout(this);
        content.setOrientation(LinearLayout.VERTICAL);
        int spacing = Math.round(20 * getResources().getDisplayMetrics().density);
        content.setPadding(spacing, spacing, spacing, spacing);
        content.setBackgroundColor(Color.WHITE);

        TextView title = new TextView(this);
        title.setText("Added to review");
        title.setTextColor(Color.rgb(32, 33, 36));
        title.setTextSize(21);
        title.setTypeface(null, android.graphics.Typeface.BOLD);
        content.addView(title);

        TextView message = new TextView(this);
        message.setText("This selected Google Photos image is now in Keepers’ review flow. "
                + "Keepers saved only a private review copy; it did not upload or change "
                + "anything in Google Photos.");
        message.setTextColor(Color.rgb(95, 99, 104));
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
        done.setTextColor(Color.WHITE);
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
        new Handler(Looper.getMainLooper()).post(() -> {
            TextView action = findViewById(R.id.import_google_photos);
            action.setEnabled(true);
            action.setText("Google Photos");
            action.setContentDescription("Select one photo from Google Photos");
            action.setOnClickListener(view -> openGooglePhotosPicker());
            showPickerError(error);
        });
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
        if ("media".equals(pickerUri.getAuthority())
                && !pickerUri.toString().contains("/picker/"))
            return new ImportedPhoto(pickerUri, fallbackTime, PhotoOrigin.LOCAL);
        try (Cursor cursor = getContentResolver().query(pickerUri,
                new String[]{android.provider.CloudMediaProviderContract.MediaColumns.MEDIA_STORE_URI,
                        android.provider.CloudMediaProviderContract.MediaColumns.DATE_TAKEN_MILLIS},
                null, null, null)) {
            if (cursor != null && cursor.moveToFirst()) {
                String local = cursor.getString(0);
                long taken = cursor.isNull(1) ? fallbackTime : cursor.getLong(1);
                return new ImportedPhoto(local == null ? pickerUri : Uri.parse(local), taken,
                        local == null ? PhotoOrigin.CLOUD : PhotoOrigin.LOCAL);
            }
        } catch (RuntimeException ignored) {}
        return new ImportedPhoto(pickerUri, fallbackTime, PhotoOrigin.CLOUD);
    }

    void loadNextPage() {
        reviewWindow.expand();
        findViewById(R.id.review_loading).setVisibility(View.VISIBLE);
        if (hasLocalPhotoAccess()) loadRecentPhotos();
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
            heart.setColorFilter(keeper ? KeeperStatusStyle.heartColor(saved) : Color.WHITE);
            int heartPadding = dp(HeartIconStyle.paddingDp(keeper));
            heart.setPadding(heartPadding, heartPadding, heartPadding, heartPadding);
            heart.setVisibility(View.VISIBLE);
            ImageView star = (ImageView) tile.getChildAt(2);
            star.setImageResource(alternative ? R.drawable.ic_star_outline : R.drawable.ic_star);
            star.setBackground(suggested ? recommendationCircle() : null);
            star.setContentDescription(suggested ? "Recommended best shot" : alternative
                    ? "Good alternative — near-identical photo ranked higher" : null);
            star.setVisibility(suggested || alternative ? View.VISIBLE : View.GONE);
            tile.setContentDescription(saved ? "Saved Keeper photo. Tap to remove."
                    : keeper ? "New Keeper photo. Tap to remove."
                    : "Photo. Tap to mark as keeper.");
        }
        int count = 0;
        for (String keeper : visibleSelected) if (!completions.hasAny(keeper)) count++;
        ((TextView) findViewById(R.id.keeper_count)).setText(count == 0
                ? "No new keepers" : count + (count == 1 ? " new keeper" : " new keepers"));
        findViewById(R.id.clear_keepers).setEnabled(!visibleSelected.isEmpty());
        findViewById(R.id.clear_keepers).setAlpha(!visibleSelected.isEmpty() ? 1f : 0.35f);
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

    private void setOriginFilter(PhotoOrigin requested) {
        originFilter = requested;
        applyFilter();
    }

    private void applyFilter() {
        Set<String> keepers = selectionStore.load();
        GridLayout grid = findViewById(R.id.photo_grid);
        grid.removeAllViews();
        List<String> orderedIds = photos.stream().map(Uri::toString).toList();
        List<String> stackCovers = StackPresentation.visibleIds(
                orderedIds, stackMembers, suggestions, keepers);
        Map<String, FrameLayout> tilesById = new HashMap<>();
        for (FrameLayout tile : tiles) tilesById.put(tile.getTag().toString(), tile);
        int visible = 0;
        List<String> typeFiltered = galleryFilter == GalleryFilter.ALL ? stackCovers
                : orderedIds.stream().filter(id -> galleryFilter == GalleryFilter.KEEPERS
                        ? keepers.contains(id) : suggestions.contains(id)).toList();
        List<String> visibleIds = PhotoOriginFilter.apply(typeFiltered, photoOrigins, originFilter);
        for (String id : visibleIds) {
            FrameLayout tile = tilesById.get(id);
            if (tile != null) grid.addView(tile);
            if (tile != null) visible++;
        }
        View keeperFilter = findViewById(R.id.filter_keepers);
        View recommendedFilter = findViewById(R.id.filter_recommended);
        keeperFilter.setSelected(galleryFilter == GalleryFilter.KEEPERS);
        recommendedFilter.setSelected(galleryFilter == GalleryFilter.RECOMMENDED);
        findViewById(R.id.filter_origin_all).setSelected(originFilter == null);
        findViewById(R.id.filter_origin_local).setSelected(originFilter == PhotoOrigin.LOCAL);
        findViewById(R.id.filter_origin_cloud).setSelected(originFilter == PhotoOrigin.CLOUD);
        TextView empty = findViewById(R.id.review_empty);
        if (!photos.isEmpty() && visible == 0) {
            empty.setText(originFilter == PhotoOrigin.CLOUD ? "No cloud photos imported yet."
                    : originFilter == PhotoOrigin.LOCAL ? "No local photos in this view."
                    : galleryFilter == GalleryFilter.KEEPERS
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
