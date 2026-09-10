package com.keepers.photoorganiser;

import android.content.Context;
import android.graphics.Bitmap;
import android.net.Uri;
import java.util.List;

final class CloudPhotoBatchImporter {
    interface Downloader {
        Bitmap download(GooglePhotosPickerApi.PickedMedia media) throws Exception;
    }

    record Result(int importedCount, int selectedCount, Bitmap preview) {}

    private final ImportedPhotoStore imports;
    private final CloudPhotoCache cache;
    private final Downloader downloader;

    CloudPhotoBatchImporter(Context context, Downloader downloader) {
        imports = new ImportedPhotoStore(context);
        cache = new CloudPhotoCache(context);
        this.downloader = downloader;
    }

    Result importAll(List<GooglePhotosPickerApi.PickedMedia> selected, long fallbackTime) {
        Bitmap preview = null;
        int imported = 0;
        for (GooglePhotosPickerApi.PickedMedia media : selected) {
            try {
                Bitmap bitmap = downloader.download(media);
                Uri reviewUri = cache.save(media.id(), bitmap);
                imports.add(new ImportedPhoto(reviewUri, fallbackTime--, PhotoOrigin.CLOUD));
                if (preview == null) preview = bitmap;
                imported++;
            } catch (Exception ignored) {}
        }
        return new Result(imported, selected.size(), preview);
    }
}
