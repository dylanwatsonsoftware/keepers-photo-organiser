package com.keepers.photoorganiser;

import android.content.ClipData;
import android.content.Intent;
import android.net.Uri;
import java.util.ArrayList;
import java.util.List;

public final class MediaShareIntentFactory {
    private MediaShareIntentFactory() {}

    public static Intent create(List<Uri> media, List<MediaType> types) {
        boolean multiple = media.size() > 1;
        Intent share = new Intent(multiple ? Intent.ACTION_SEND_MULTIPLE : Intent.ACTION_SEND)
                .setType(mimeType(types))
                .addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
        if (multiple) share.putParcelableArrayListExtra(
                Intent.EXTRA_STREAM, new ArrayList<>(media));
        else share.putExtra(Intent.EXTRA_STREAM, media.get(0));
        ClipData clip = ClipData.newRawUri("Shared media", media.get(0));
        for (int index = 1; index < media.size(); index++)
            clip.addItem(new ClipData.Item(media.get(index)));
        share.setClipData(clip);
        return share;
    }

    private static String mimeType(List<MediaType> types) {
        boolean photos = types.stream().anyMatch(type -> type == MediaType.PHOTO);
        boolean videos = types.stream().anyMatch(type -> type == MediaType.VIDEO);
        if (photos && videos) return "*/*";
        return videos ? "video/*" : "image/*";
    }
}
