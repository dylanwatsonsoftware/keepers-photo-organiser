package com.keepers.photoorganiser;

import android.content.Context;
import android.content.SharedPreferences;
import java.util.UUID;

public final class RecommendationExportIdentityStore {
    private static final String SOURCE_ID = "source_id";
    private static final String SNAPSHOT_SEQUENCE = "snapshot_sequence";
    private final SharedPreferences preferences;

    public RecommendationExportIdentityStore(Context context) {
        preferences = context.getSharedPreferences(
                "recommendation_export_identity", Context.MODE_PRIVATE);
    }

    public RecommendationExportMetadata nextSnapshot() {
        String sourceId = preferences.getString(SOURCE_ID, null);
        if (sourceId == null) sourceId = UUID.randomUUID().toString();
        long sequence = preferences.getLong(SNAPSHOT_SEQUENCE, 0) + 1;
        preferences.edit().putString(SOURCE_ID, sourceId)
                .putLong(SNAPSHOT_SEQUENCE, sequence).commit();
        return new RecommendationExportMetadata(sourceId, sequence);
    }

    void clear() { preferences.edit().clear().commit(); }
}
