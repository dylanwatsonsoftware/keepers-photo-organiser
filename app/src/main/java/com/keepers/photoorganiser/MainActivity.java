package com.keepers.photoorganiser;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.PendingIntent;
import android.content.ClipData;
import android.content.Intent;
import android.content.IntentSender;
import android.net.Uri;
import android.os.Bundle;
import android.provider.MediaStore;
import android.widget.Button;
import android.widget.TextView;
import java.util.ArrayList;
import java.util.List;

public final class MainActivity extends Activity {
    private static final int PICK_PHOTOS = 100;
    private static final int FAVOURITE_REQUEST = 101;
    private final ArrayList<Uri> selectedPhotos = new ArrayList<>();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        findViewById(R.id.choose_photos).setOnClickListener(view -> choosePhotos());
        findViewById(R.id.open_existing).setOnClickListener(view -> openFirstPhoto());
        findViewById(R.id.request_favourite).setOnClickListener(view -> requestFavourite());
        findViewById(R.id.share_experiment).setOnClickListener(view -> confirmShareExperiment());
        showSelection(List.of());
    }

    private void choosePhotos() {
        Intent intent = new Intent(MediaStore.ACTION_PICK_IMAGES)
                .setType("image/*")
                .putExtra(MediaStore.EXTRA_PICK_IMAGES_MAX, 5)
                .addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
        startActivityForResult(intent, PICK_PHOTOS);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode != PICK_PHOTOS || resultCode != RESULT_OK || data == null) {
            return;
        }

        ArrayList<Uri> photos = new ArrayList<>();
        ClipData clipData = data.getClipData();
        if (clipData != null) {
            for (int index = 0; index < clipData.getItemCount(); index++) {
                photos.add(clipData.getItemAt(index).getUri());
            }
        } else if (data.getData() != null) {
            photos.add(data.getData());
        }
        try {
            MediaUriResolver resolver = new MediaUriResolver(
                    uri -> MediaStore.getMediaUri(this, uri));
            showSelection(resolver.resolveAll(photos));
        } catch (IllegalArgumentException exception) {
            showSelection(List.of());
            explain("Local photo required",
                    "At least one selection exists only through a document or cloud provider. "
                            + "Choose recent Pixel camera photos that are still stored on this phone.");
        }
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
