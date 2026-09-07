package com.keepers.photoorganiser;

import android.content.ContentResolver;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.provider.MediaStore;
import java.util.ArrayList;
import java.util.List;

public final class RecentCameraQuery {
    public static final int LIMIT = 60;
    public static final String PATH_PATTERN = "DCIM/Camera/%";

    private RecentCameraQuery() {}

    public static Uri itemUri(long id) {
        return MediaStore.Images.Media.getContentUri(MediaStore.VOLUME_EXTERNAL, id);
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

        ArrayList<RecentPhoto> photos = new ArrayList<>();
        try (Cursor cursor = resolver.query(
                MediaStore.Images.Media.EXTERNAL_CONTENT_URI,
                new String[]{MediaStore.Images.Media._ID, MediaStore.Images.Media.DATE_TAKEN}, args, null)) {
            if (cursor == null) return photos;
            int idColumn = cursor.getColumnIndexOrThrow(MediaStore.Images.Media._ID);
            int dateColumn = cursor.getColumnIndexOrThrow(MediaStore.Images.Media.DATE_TAKEN);
            while (cursor.moveToNext()) photos.add(new RecentPhoto(
                    itemUri(cursor.getLong(idColumn)), cursor.getLong(dateColumn)));
        }
        return photos;
    }
}
