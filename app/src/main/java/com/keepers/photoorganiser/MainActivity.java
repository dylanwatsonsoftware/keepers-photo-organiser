package com.keepers.photoorganiser;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.PendingIntent;
import android.Manifest;
import android.content.Intent;
import android.content.IntentSender;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.provider.MediaStore;
import android.provider.Settings;
import android.widget.Toast;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import java.util.ArrayList;
import java.util.List;

public final class MainActivity extends Activity {
    private static final int PHOTO_PERMISSION = 100;
    private static final int FAVOURITE_REQUEST = 101;
    private final ArrayList<Uri> selectedPhotos = new ArrayList<>();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        findViewById(R.id.choose_photos).setOnClickListener(view -> choosePhotos());
        findViewById(R.id.open_review).setOnClickListener(view ->
                startActivity(new Intent(this, ReviewActivity.class)));
        findViewById(R.id.open_existing).setOnClickListener(view -> openFirstPhoto());
        findViewById(R.id.request_favourite).setOnClickListener(view -> requestFavourite());
        findViewById(R.id.share_experiment).setOnClickListener(view -> confirmShareExperiment());
        findViewById(R.id.open_accessibility_settings).setOnClickListener(view ->
                startActivity(new Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS)));
        findViewById(R.id.arm_accessibility_favourite).setOnClickListener(view -> armFavourite());
        findViewById(R.id.arm_accessibility_album).setOnClickListener(view -> armAlbum());
        showSelection(List.of());
    }

    private void armAlbum() {
        String album = ((EditText) findViewById(R.id.album_name)).getText().toString().trim();
        if (album.isEmpty()) {
            explain("Album name required", "Enter the exact existing Google Photos album name.");
            return;
        }
        getSharedPreferences(KeepersAccessibilityService.PREFS, MODE_PRIVATE).edit()
                .putLong(KeepersAccessibilityService.ALBUM_ARMED_UNTIL,
                        System.currentTimeMillis() + 120_000)
                .putString(KeepersAccessibilityService.ALBUM_NAME, album)
                .putInt(KeepersAccessibilityService.ALBUM_PHASE, 0).apply();
        Toast.makeText(this, "Armed for album “" + album + "”", Toast.LENGTH_LONG).show();
        Intent launch = getPackageManager().getLaunchIntentForPackage(
                GooglePhotosIntentFactory.GOOGLE_PHOTOS_PACKAGE);
        if (launch != null) startActivity(launch);
    }

    private void armFavourite() {
        long deadline = System.currentTimeMillis() + 120_000;
        getSharedPreferences(KeepersAccessibilityService.PREFS, MODE_PRIVATE).edit()
                .putLong(KeepersAccessibilityService.ARMED_UNTIL, deadline).apply();
        Toast.makeText(this, "Armed for two minutes. Open one test photo.", Toast.LENGTH_LONG).show();
        Intent launch = getPackageManager().getLaunchIntentForPackage(
                GooglePhotosIntentFactory.GOOGLE_PHOTOS_PACKAGE);
        if (launch != null) startActivity(launch);
    }

    private void choosePhotos() {
        if (hasLocalPhotoAccess()) {
            loadRecentCameraPhotos();
            return;
        }
        if (Build.VERSION.SDK_INT >= 34) {
            requestPermissions(new String[]{
                    Manifest.permission.READ_MEDIA_IMAGES,
                    Manifest.permission.READ_MEDIA_VISUAL_USER_SELECTED}, PHOTO_PERMISSION);
        } else if (Build.VERSION.SDK_INT >= 33) {
            requestPermissions(new String[]{Manifest.permission.READ_MEDIA_IMAGES}, PHOTO_PERMISSION);
        } else {
            requestPermissions(new String[]{Manifest.permission.READ_EXTERNAL_STORAGE}, PHOTO_PERMISSION);
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] results) {
        super.onRequestPermissionsResult(requestCode, permissions, results);
        if (requestCode == PHOTO_PERMISSION && hasLocalPhotoAccess()) loadRecentCameraPhotos();
    }

    private boolean hasLocalPhotoAccess() {
        if (Build.VERSION.SDK_INT >= 34
                && checkSelfPermission(Manifest.permission.READ_MEDIA_VISUAL_USER_SELECTED)
                == PackageManager.PERMISSION_GRANTED) return true;
        String permission = Build.VERSION.SDK_INT >= 33
                ? Manifest.permission.READ_MEDIA_IMAGES
                : Manifest.permission.READ_EXTERNAL_STORAGE;
        return checkSelfPermission(permission) == PackageManager.PERMISSION_GRANTED;
    }

    private void loadRecentCameraPhotos() {
        List<Uri> photos = RecentCameraQuery.load(getContentResolver());
        showSelection(photos);
        if (photos.isEmpty()) explain("No local camera photos found",
                "Grant access to recent Pixel camera photos that still exist on this phone.");
    }

    void showSelection(List<Uri> photos) {
        selectedPhotos.clear();
        selectedPhotos.addAll(photos);

        String status = photos.isEmpty()
                ? "No photos selected"
                : photos.size() + (photos.size() == 1 ? " photo selected" : " photos selected");
        ((TextView) findViewById(R.id.selection_status)).setText(status);

        boolean hasPhotos = !photos.isEmpty();
        button(R.id.open_existing).setEnabled(hasPhotos);
        button(R.id.request_favourite).setEnabled(hasPhotos);
        button(R.id.share_experiment).setEnabled(photos.size() > 1);
    }

    private void openFirstPhoto() {
        launchOrExplain(GooglePhotosIntentFactory.openExisting(selectedPhotos.get(0)));
    }

    private void requestFavourite() {
        try {
            PendingIntent request = MediaStore.createFavoriteRequest(
                    getContentResolver(), selectedPhotos, true);
            startIntentSenderForResult(
                    request.getIntentSender(), FAVOURITE_REQUEST, null, 0, 0, 0);
        } catch (IntentSender.SendIntentException | IllegalArgumentException exception) {
            explain("Favourite request unavailable",
                    "These selected URIs cannot be changed through Android MediaStore. "
                            + "This is a failed integration result worth recording.");
        }
    }

    private void confirmShareExperiment() {
        new AlertDialog.Builder(this)
                .setTitle("Possible duplicate upload")
                .setMessage("Google Photos may treat this as a new upload instead of existing items. "
                        + "Use only expendable test photos and check the cloud library afterwards.")
                .setNegativeButton("Cancel", null)
                .setPositiveButton("Run experiment", (dialog, which) ->
                        launchOrExplain(GooglePhotosIntentFactory.shareExperiment(selectedPhotos)))
                .show();
    }

    private void launchOrExplain(Intent intent) {
        if (intent.resolveActivity(getPackageManager()) == null) {
            explain("Google Photos unavailable", "The Google Photos app could not handle this action.");
            return;
        }
        startActivity(intent);
    }

    private void explain(String title, String message) {
        new AlertDialog.Builder(this)
                .setTitle(title)
                .setMessage(message)
                .setPositiveButton("OK", null)
                .show();
    }

    private Button button(int id) {
        return findViewById(id);
    }
}
