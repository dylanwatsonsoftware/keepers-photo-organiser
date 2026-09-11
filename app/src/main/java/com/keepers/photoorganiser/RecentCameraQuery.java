package com.keepers.photoorganiser;

import android.content.ContentResolver;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.provider.MediaStore;
import java.util.ArrayList;
import java.util.List;
import java.util.Comparator;

public final class RecentCameraQuery {
    public static final int LIMIT = 60;
    public static final String PATH_PATTERN = "DCIM/Camera/%";

    private RecentCameraQuery() {}

    public static Uri itemUri(long id) {
        return MediaStore.Images.Media.getContentUri(MediaStore.VOLUME_EXTERNAL, id);
    }

    public static Uri videoItemUri(long id) {
        return MediaStore.Video.Media.getContentUri(MediaStore.VOLUME_EXTERNAL, id);
    }

    public static List<Uri> load(ContentResolver resolver) {
        ArrayList<Uri> uris = new ArrayList<>();
        for (RecentPhoto photo : loadRecent(resolver)) uris.add(photo.uri());
        return uris;
    }

    public static List<RecentPhoto> loadRecent(ContentResolver resolver) {
        return loadRecent(resolver, LIMIT);
    }

    public static List<RecentPhoto> loadRecent(ContentResolver resolver, int limit) {
        Bundle args = new Bundle();
        args.putString(ContentResolver.QUERY_ARG_SQL_SELECTION,
                MediaStore.Images.Media.RELATIVE_PATH + " LIKE ?");
        args.putStringArray(ContentResolver.QUERY_ARG_SQL_SELECTION_ARGS,
                new String[]{PATH_PATTERN});
        args.putStringArray(ContentResolver.QUERY_ARG_SORT_COLUMNS,
                new String[]{MediaStore.Images.Media.DATE_TAKEN});
        args.putInt(ContentResolver.QUERY_ARG_SORT_DIRECTION,
                ContentResolver.QUERY_SORT_DIRECTION_DESCENDING);
        args.putInt(ContentResolver.QUERY_ARG_LIMIT, limit);

        ArrayList<RecentPhoto> media = new ArrayList<>();
        loadType(resolver, MediaStore.Images.Media.EXTERNAL_CONTENT_URI, args,
                MediaType.PHOTO, media);
        loadType(resolver, MediaStore.Video.Media.EXTERNAL_CONTENT_URI, args,
                MediaType.VIDEO, media);
        media.sort(Comparator.comparingLong(RecentPhoto::takenAtMillis).reversed());
        if (media.size() > limit) return List.copyOf(media.subList(0, limit));
        return List.copyOf(media);
    }

    private static void loadType(ContentResolver resolver, Uri collection, Bundle args,
            MediaType type, List<RecentPhoto> destination) {
        String[] projection = type == MediaType.VIDEO
                ? new String[]{MediaStore.Video.Media._ID, MediaStore.Video.Media.DATE_TAKEN,
                        MediaStore.Video.Media.DURATION}
                : new String[]{MediaStore.Images.Media._ID, MediaStore.Images.Media.DATE_TAKEN};
        try (Cursor cursor = resolver.query(collection, projection, args, null)) {
            if (cursor == null) return;
            int idColumn = cursor.getColumnIndexOrThrow(MediaStore.MediaColumns._ID);
            int dateColumn = cursor.getColumnIndexOrThrow(MediaStore.MediaColumns.DATE_TAKEN);
            int durationColumn = cursor.getColumnIndex(MediaStore.Video.Media.DURATION);
            while (cursor.moveToNext()) destination.add(new RecentPhoto(
                    type == MediaType.VIDEO ? videoItemUri(cursor.getLong(idColumn))
                            : itemUri(cursor.getLong(idColumn)),
                    cursor.getLong(dateColumn), type,
                    durationColumn < 0 || cursor.isNull(durationColumn)
                            ? 0 : cursor.getLong(durationColumn)));
        } catch (SecurityException ignored) {
            // A user can grant access to photos but not videos, or vice versa.
        }
    }
}
