package com.keepers.photoorganiser;

import android.content.ContentResolver;
import android.database.Cursor;
import android.media.ExifInterface;
import android.net.Uri;
import android.provider.MediaStore;
import android.provider.OpenableColumns;
import java.io.IOException;
import java.io.InputStream;

public final class PhotoMetadataReader {
    private PhotoMetadataReader() {}

    public static PhotoMetadata read(ContentResolver resolver, Uri uri, long fallbackTakenAt) {
        long takenAt = fallbackTakenAt;
        String caption = "";
        int width = 0;
        int height = 0;
        String mimeType = resolver.getType(uri);
        long size = 0;
        long duration = 0;
        String filename = "";
        String[] projection = {MediaStore.Images.Media.DATE_TAKEN,
                MediaStore.Images.Media.DESCRIPTION, MediaStore.Images.Media.WIDTH,
                MediaStore.Images.Media.HEIGHT, MediaStore.Images.Media.MIME_TYPE,
                MediaStore.Images.Media.SIZE, MediaStore.Video.Media.DURATION,
                OpenableColumns.DISPLAY_NAME};
        try (Cursor cursor = resolver.query(uri, projection, null, null, null)) {
            if (cursor != null && cursor.moveToFirst()) {
                takenAt = longValue(cursor, MediaStore.Images.Media.DATE_TAKEN, takenAt);
                caption = stringValue(cursor, MediaStore.Images.Media.DESCRIPTION, caption);
                width = (int) longValue(cursor, MediaStore.Images.Media.WIDTH, width);
                height = (int) longValue(cursor, MediaStore.Images.Media.HEIGHT, height);
                mimeType = stringValue(cursor, MediaStore.Images.Media.MIME_TYPE, mimeType);
                size = longValue(cursor, MediaStore.Images.Media.SIZE, size);
                duration = longValue(cursor, MediaStore.Video.Media.DURATION, duration);
                filename = stringValue(cursor, OpenableColumns.DISPLAY_NAME, filename);
            }
        } catch (RuntimeException ignored) {}

        filename = value(filename);
        if (filename.isBlank()) {
            String candidate = value(uri.getLastPathSegment());
            if (candidate.contains(".")) filename = candidate;
        }

        String location = "";
        try (InputStream stream = resolver.openInputStream(uri)) {
            if (stream != null) {
                ExifInterface exif = new ExifInterface(stream);
                if (caption == null || caption.isBlank())
                    caption = value(exif.getAttribute(ExifInterface.TAG_IMAGE_DESCRIPTION));
                float[] coordinates = new float[2];
                if (exif.getLatLong(coordinates)) location =
                        PhotoMetadata.locationFromCoordinates(coordinates[0], coordinates[1]);
            }
        } catch (IOException | RuntimeException ignored) {}
        return new PhotoMetadata(takenAt, value(caption), location, filename, width, height,
                value(mimeType), size, duration);
    }

    private static long longValue(Cursor cursor, String column, long fallback) {
        int index = cursor.getColumnIndex(column);
        return index < 0 || cursor.isNull(index) ? fallback : cursor.getLong(index);
    }

    private static String stringValue(Cursor cursor, String column, String fallback) {
        int index = cursor.getColumnIndex(column);
        return index < 0 || cursor.isNull(index) ? fallback : cursor.getString(index);
    }

    private static String value(String value) { return value == null ? "" : value.trim(); }
}
