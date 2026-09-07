package com.keepers.photoorganiser;

import android.net.Uri;
import android.provider.MediaStore;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Function;

public final class MediaUriResolver {
    private final Function<Uri, Uri> documentConverter;

    public MediaUriResolver(Function<Uri, Uri> documentConverter) {
        this.documentConverter = documentConverter;
    }

    public List<Uri> resolveAll(List<Uri> selectedUris) {
        ArrayList<Uri> resolved = new ArrayList<>();
        for (Uri selected : selectedUris) {
            if (MediaStore.AUTHORITY.equals(selected.getAuthority())) {
                resolved.add(selected);
                continue;
            }

            Uri converted = documentConverter.apply(selected);
            if (converted == null || !MediaStore.AUTHORITY.equals(converted.getAuthority())) {
                throw new IllegalArgumentException("Photo has no local MediaStore equivalent");
            }
            resolved.add(converted);
        }
        return resolved;
    }
}
