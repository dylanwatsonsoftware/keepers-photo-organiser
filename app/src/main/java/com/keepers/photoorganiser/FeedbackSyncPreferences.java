package com.keepers.photoorganiser;

import android.content.Context;
import android.content.SharedPreferences;

public final class FeedbackSyncPreferences {
    private static final String ENABLED = "enabled";
    private static final String LAST_SEQUENCE = "last_sequence";
    private static final String LAST_SUCCESS = "last_success";
    private static final String LAST_ERROR = "last_error";
    private final SharedPreferences preferences;

    public FeedbackSyncPreferences(Context context) {
        preferences = context.getSharedPreferences("feedback_sync", Context.MODE_PRIVATE);
    }

    public boolean isEnabled() { return preferences.getBoolean(ENABLED, false); }

    public void setEnabled(boolean enabled) {
        preferences.edit().putBoolean(ENABLED, enabled).apply();
    }

    public long lastSnapshotSequence() { return preferences.getLong(LAST_SEQUENCE, 0); }

    public long lastSuccessAtMillis() { return preferences.getLong(LAST_SUCCESS, 0); }

    public String lastError() { return preferences.getString(LAST_ERROR, ""); }

    public void recordSuccess(long sequence, long atMillis) {
        preferences.edit().putLong(LAST_SEQUENCE, sequence).putLong(LAST_SUCCESS, atMillis)
                .remove(LAST_ERROR).apply();
    }

    public void recordFailure(String error) {
        preferences.edit().putString(LAST_ERROR, error == null ? "Unknown error" : error).apply();
    }

    void clear() { preferences.edit().clear().commit(); }
}
