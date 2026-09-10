package com.keepers.photoorganiser;

import android.content.Context;
import android.graphics.Bitmap;
import android.net.Uri;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

final class CloudPhotoCache {
    static final String AUTHORITY = "com.keepers.photoorganiser.cloud";
    private final File directory;

    CloudPhotoCache(Context context) {
        directory = new File(context.getFilesDir(), "cloud-review");
    }

    Uri save(String mediaId, Bitmap bitmap) throws IOException {
        if (!directory.exists() && !directory.mkdirs())
            throw new IOException("Could not create the cloud review cache");
        String name = digest(mediaId) + ".jpg";
        File destination = new File(directory, name);
        File temporary = new File(directory, name + ".tmp");
        try (FileOutputStream output = new FileOutputStream(temporary)) {
            if (!bitmap.compress(Bitmap.CompressFormat.JPEG, 95, output))
                throw new IOException("Could not encode the selected cloud photo");
        }
        if (destination.exists() && !destination.delete())
            throw new IOException("Could not replace the selected cloud photo");
        if (!temporary.renameTo(destination))
            throw new IOException("Could not finish the selected cloud photo");
        return new Uri.Builder().scheme("content").authority(AUTHORITY)
                .appendPath(name).build();
    }

    static File resolve(Context context, Uri uri) {
        if (!AUTHORITY.equals(uri.getAuthority()) || uri.getPathSegments().size() != 1)
            throw new IllegalArgumentException("Unknown cloud photo URI");
        String name = uri.getLastPathSegment();
        if (name == null || !name.matches("[0-9a-f]{64}\\.jpg"))
            throw new IllegalArgumentException("Unknown cloud photo URI");
        return new File(new File(context.getFilesDir(), "cloud-review"), name);
    }

    private static String digest(String value) {
        try {
            byte[] bytes = MessageDigest.getInstance("SHA-256")
                    .digest(value.getBytes(StandardCharsets.UTF_8));
            StringBuilder result = new StringBuilder();
            for (byte item : bytes) result.append(String.format("%02x", item));
            return result.toString();
        } catch (NoSuchAlgorithmException impossible) {
            throw new IllegalStateException(impossible);
        }
    }
}
