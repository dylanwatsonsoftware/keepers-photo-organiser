package com.keepers.photoorganiser;

import android.Manifest;
import android.app.Activity;
import android.content.pm.PackageManager;
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
import android.widget.TextView;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public final class ReviewActivity extends Activity {
    private static final int PHOTO_PERMISSION = 200;
    private static final float FADED_ALPHA = 0.38f;
    private KeeperSelectionStore selectionStore;
    private AsyncThumbnailLoader thumbnailLoader;
    private List<Uri> photos = List.of();

    @Override protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_review);
        selectionStore = new KeeperSelectionStore(this);
        thumbnailLoader = AsyncThumbnailLoader.forResolver(getContentResolver());
        findViewById(R.id.clear_keepers).setOnClickListener(view -> {
            selectionStore.clear();
            updateSelectionDisplay();
        });
        loadOrRequestPhotos();
    }

    private void loadOrRequestPhotos() {
        if (hasLocalPhotoAccess()) {
            showPhotos(RecentCameraQuery.load(getContentResolver()));
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
            showPhotos(RecentCameraQuery.load(getContentResolver()));
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
        photos = List.copyOf(recentPhotos);
        GridLayout grid = findViewById(R.id.photo_grid);
        grid.removeAllViews();
        int tileSize = Math.max(1, getResources().getDisplayMetrics().widthPixels / 3 - 2);
        for (Uri photo : photos) grid.addView(createTile(photo, tileSize));
        TextView empty = findViewById(R.id.review_empty);
        empty.setText("No recent local camera photos found.");
        empty.setVisibility(photos.isEmpty() ? View.VISIBLE : View.GONE);
        updateSelectionDisplay();
    }

    private View createTile(Uri photo, int size) {
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
        thumbnailLoader.load(image, photo, size);
        tile.addView(image, new FrameLayout.LayoutParams(
                FrameLayout.LayoutParams.MATCH_PARENT, FrameLayout.LayoutParams.MATCH_PARENT));

        TextView marker = new TextView(this);
        marker.setTag("marker");
        marker.setText("♥");
        marker.setTextColor(Color.WHITE);
        marker.setTextSize(19);
        marker.setGravity(Gravity.CENTER);
        GradientDrawable circle = new GradientDrawable();
        circle.setShape(GradientDrawable.OVAL);
        circle.setColor(Color.rgb(11, 87, 208));
        marker.setBackground(circle);
        FrameLayout.LayoutParams markerParams = new FrameLayout.LayoutParams(dp(34), dp(34),
                Gravity.TOP | Gravity.END);
        markerParams.setMargins(0, dp(7), dp(7), 0);
        tile.addView(marker, markerParams);
        tile.setContentDescription("Photo. Tap to mark as keeper.");
        tile.setOnClickListener(view -> {
            selectionStore.toggle((Uri) view.getTag());
            updateSelectionDisplay();
        });
        return tile;
    }

    private void updateSelectionDisplay() {
        Set<String> selected = selectionStore.load();
        Set<String> visibleSelected = new HashSet<>();
        for (Uri photo : photos) if (selected.contains(photo.toString())) {
            visibleSelected.add(photo.toString());
        }
        boolean hasSelection = !visibleSelected.isEmpty();
        GridLayout grid = findViewById(R.id.photo_grid);
        for (int index = 0; index < grid.getChildCount(); index++) {
            FrameLayout tile = (FrameLayout) grid.getChildAt(index);
            boolean keeper = visibleSelected.contains(tile.getTag().toString());
            tile.setAlpha(!hasSelection || keeper ? 1f : FADED_ALPHA);
            tile.getChildAt(1).setVisibility(keeper ? View.VISIBLE : View.GONE);
            tile.setContentDescription(keeper ? "Keeper photo. Tap to remove."
                    : "Photo. Tap to mark as keeper.");
        }
        int count = visibleSelected.size();
        ((TextView) findViewById(R.id.keeper_count)).setText(count == 0
                ? "No keepers selected yet" : count + (count == 1 ? " keeper" : " keepers"));
        findViewById(R.id.clear_keepers).setEnabled(count > 0);
    }

    private int dp(int value) {
        return Math.round(value * getResources().getDisplayMetrics().density);
    }

    @Override protected void onDestroy() {
        thumbnailLoader.close();
        super.onDestroy();
    }
}
