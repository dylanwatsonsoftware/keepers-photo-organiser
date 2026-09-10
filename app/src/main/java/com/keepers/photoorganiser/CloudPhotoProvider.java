package com.keepers.photoorganiser;

import android.content.ContentProvider;
import android.content.ContentValues;
import android.database.Cursor;
import android.database.MatrixCursor;
import android.net.Uri;
import android.os.ParcelFileDescriptor;
import android.provider.OpenableColumns;
import java.io.File;
import java.io.FileNotFoundException;

public final class CloudPhotoProvider extends ContentProvider {
    @Override public boolean onCreate() { return true; }
    @Override public String getType(Uri uri) { return "image/jpeg"; }

    @Override public ParcelFileDescriptor openFile(Uri uri, String mode)
            throws FileNotFoundException {
        if (!"r".equals(mode)) throw new FileNotFoundException("Cloud photos are read-only");
        File photo;
        try { photo = CloudPhotoCache.resolve(getContext(), uri); }
        catch (IllegalArgumentException invalid) { throw new FileNotFoundException(invalid.getMessage()); }
        return ParcelFileDescriptor.open(photo, ParcelFileDescriptor.MODE_READ_ONLY);
    }

    @Override public Cursor query(Uri uri, String[] projection, String selection,
            String[] selectionArgs, String sortOrder) {
        File photo = CloudPhotoCache.resolve(getContext(), uri);
        MatrixCursor result = new MatrixCursor(new String[]{OpenableColumns.DISPLAY_NAME,
                OpenableColumns.SIZE});
        result.addRow(new Object[]{photo.getName(), photo.length()});
        return result;
    }

    @Override public Uri insert(Uri uri, ContentValues values) { throw readOnly(); }
    @Override public int delete(Uri uri, String selection, String[] args) { throw readOnly(); }
    @Override public int update(Uri uri, ContentValues values, String selection, String[] args) {
        throw readOnly();
    }
    private UnsupportedOperationException readOnly() {
        return new UnsupportedOperationException("Cloud review photos are read-only");
    }
}
