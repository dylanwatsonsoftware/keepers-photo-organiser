package com.keepers.photoorganiser;

import android.Manifest;
import java.util.ArrayList;
import java.util.List;

public final class MediaPermissionRequest {
    private MediaPermissionRequest() {}

    public static List<String> missing(int apiLevel, boolean imagesGranted,
            boolean videosGranted, boolean selectedMediaGranted) {
        if (apiLevel >= 34 && selectedMediaGranted && !imagesGranted && !videosGranted)
            return List.of();
        ArrayList<String> permissions = new ArrayList<>();
        if (apiLevel >= 33) {
            if (!imagesGranted) permissions.add(Manifest.permission.READ_MEDIA_IMAGES);
            if (!videosGranted) permissions.add(Manifest.permission.READ_MEDIA_VIDEO);
        } else if (!imagesGranted) {
            permissions.add(Manifest.permission.READ_EXTERNAL_STORAGE);
        }
        return List.copyOf(permissions);
    }
}
