package com.keepers.photoorganiser;

import android.net.Uri;
import java.util.List;

public record FavoriteDiagnostic(int scannedCount, List<Uri> favoriteUris) {
    public FavoriteDiagnostic {
        favoriteUris = List.copyOf(favoriteUris);
    }
}
