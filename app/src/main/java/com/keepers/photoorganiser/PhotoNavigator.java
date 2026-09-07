package com.keepers.photoorganiser;

import android.net.Uri;
import java.util.List;

public final class PhotoNavigator {
    private final List<Uri> photos;
    private int index;

    public PhotoNavigator(List<Uri> photos, Uri requested) {
        if (photos.isEmpty()) throw new IllegalArgumentException("photos must not be empty");
        this.photos = List.copyOf(photos);
        index = this.photos.indexOf(requested);
        if (index < 0) index = 0;
    }

    public Uri current() { return photos.get(index); }
    public Uri peekNext() { return photos.get(Math.min(index + 1, photos.size() - 1)); }
    public Uri peekPrevious() { return photos.get(Math.max(index - 1, 0)); }
    public Uri next() { if (index < photos.size() - 1) index++; return current(); }
    public Uri previous() { if (index > 0) index--; return current(); }
}
