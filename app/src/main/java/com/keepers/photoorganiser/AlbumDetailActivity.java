package com.keepers.photoorganiser;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.widget.EditText;
import android.widget.ImageView;
import java.util.ArrayList;

public final class AlbumDetailActivity extends Activity {
    public static final String EXTRA_ALBUM_ID = "album_id";
    private static final int CHOOSE_FEATURE = 41;
    private RegisteredAlbum album;
    private AsyncThumbnailLoader loader;

    @Override protected void onCreate(Bundle state) {
        super.onCreate(state);
        setContentView(R.layout.activity_album_detail);
        loader = AsyncThumbnailLoader.forResolver(getContentResolver());
        String id = getIntent().getStringExtra(EXTRA_ALBUM_ID);
        album = new RegisteredAlbumStore(this).load().stream()
                .filter(candidate -> candidate.id().equals(id)).findFirst().orElse(null);
        if (album == null) { finish(); return; }
        findViewById(R.id.album_detail_back).setOnClickListener(view -> finish());
        EditText name = findViewById(R.id.album_detail_name);
        name.setText(album.albumName());
        name.addTextChangedListener(new TextWatcher() {
            @Override public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
            @Override public void onTextChanged(CharSequence s, int start, int before, int count) {}
            @Override public void afterTextChanged(Editable value) {
                album = new RegisteredAlbum(album.id(), value.toString().trim(),
                        album.featurePhotoId());
                persist();
            }
        });
        findViewById(R.id.choose_album_feature).setOnClickListener(view -> {
            Intent pick = new Intent(Intent.ACTION_OPEN_DOCUMENT)
                    .setType("image/*").addCategory(Intent.CATEGORY_OPENABLE)
                    .addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION
                            | Intent.FLAG_GRANT_PERSISTABLE_URI_PERMISSION);
            startActivityForResult(pick, CHOOSE_FEATURE);
        });
        findViewById(R.id.delete_album_destination).setOnClickListener(view ->
                new AlertDialog.Builder(this).setTitle("Remove this album destination?")
                        .setMessage("This removes it from Keepers only. The Google Photos album will not be changed.")
                        .setNegativeButton("Cancel", null)
                        .setPositiveButton("Remove", (dialog, which) -> remove()).show());
        showFeature();
    }

    @Override protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode != CHOOSE_FEATURE || resultCode != RESULT_OK
                || data == null || data.getData() == null) return;
        Uri selected = data.getData();
        try { getContentResolver().takePersistableUriPermission(selected,
                Intent.FLAG_GRANT_READ_URI_PERMISSION); }
        catch (SecurityException ignored) {}
        album = new RegisteredAlbum(album.id(), album.albumName(), selected.toString());
        persist();
        showFeature();
    }

    private void showFeature() {
        ImageView feature = findViewById(R.id.album_detail_feature);
        if (album.featurePhotoId().isBlank()) {
            feature.setImageResource(R.drawable.ic_review_albums);
            feature.setPadding(dp(42), dp(42), dp(42), dp(42));
            feature.setColorFilter(0xFF5F6368);
            return;
        }
        feature.setPadding(0, 0, 0, 0);
        feature.clearColorFilter();
        loader.loadProgressive(feature, Uri.parse(album.featurePhotoId()), 640, 1600, bitmap -> {});
    }

    private void persist() {
        ArrayList<RegisteredAlbum> albums = new ArrayList<>(new RegisteredAlbumStore(this).load());
        for (int index = 0; index < albums.size(); index++)
            if (albums.get(index).id().equals(album.id())) albums.set(index, album);
        new RegisteredAlbumStore(this).save(albums);
        AlbumApprovalInvalidator.invalidate(this);
    }

    private void remove() {
        ArrayList<RegisteredAlbum> albums = new ArrayList<>(new RegisteredAlbumStore(this).load());
        albums.removeIf(candidate -> candidate.id().equals(album.id()));
        new RegisteredAlbumStore(this).save(albums);
        AlbumApprovalInvalidator.invalidate(this);
        finish();
    }

    private int dp(int value) {
        return Math.round(value * getResources().getDisplayMetrics().density);
    }

    @Override protected void onDestroy() {
        if (loader != null) loader.close();
        super.onDestroy();
    }
}
