package com.keepers.photoorganiser;

import android.content.ContentResolver;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.provider.MediaStore;
import java.util.ArrayList;

public final class MediaStoreFavoriteDiagnostic {
    private static final int LIMIT = 500;

    private MediaStoreFavoriteDiagnostic() {}

    public static FavoriteDiagnostic scan(ContentResolver resolver) {
        Bundle args = new Bundle();
        args.putString(ContentResolver.QUERY_ARG_SQL_SELECTION,
                MediaStore.Images.Media.RELATIVE_PATH + " LIKE ?");
        args.putStringArray(ContentResolver.QUERY_ARG_SQL_SELECTION_ARGS,
                new String[]{RecentCameraQuery.PATH_PATTERN});
        args.putStringArray(ContentResolver.QUERY_ARG_SORT_COLUMNS,
                new String[]{MediaStore.Images.Media.DATE_TAKEN});
        args.putInt(ContentResolver.QUERY_ARG_SORT_DIRECTION,
                ContentResolver.QUERY_SORT_DIRECTION_DESCENDING);
        args.putInt(ContentResolver.QUERY_ARG_LIMIT, LIMIT);

        try (Cursor cursor = resolver.query(
                MediaStore.Images.Media.EXTERNAL_CONTENT_URI,
                new String[]{MediaStore.Images.Media._ID, MediaStore.MediaColumns.IS_FAVORITE},
                args,
                null)) {
            return read(cursor);
        }
    }

    static FavoriteDiagnostic read(Cursor cursor) {
        if (cursor == null) return new FavoriteDiagnostic(0, new ArrayList<>());
        int idColumn = cursor.getColumnIndexOrThrow(MediaStore.Images.Media._ID);
        int favoriteColumn = cursor.getColumnIndexOrThrow(MediaStore.MediaColumns.IS_FAVORITE);
        ArrayList<Uri> favorites = new ArrayList<>();
        int scanned = 0;
        while (cursor.moveToNext()) {
            scanned++;
            if (cursor.getInt(favoriteColumn) != 0) {
                favorites.add(RecentCameraQuery.itemUri(cursor.getLong(idColumn)));
            }
        }
        return new FavoriteDiagnostic(scanned, favorites);
    }
}
