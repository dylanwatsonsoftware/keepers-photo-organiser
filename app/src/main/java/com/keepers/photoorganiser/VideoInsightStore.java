package com.keepers.photoorganiser;

import android.content.Context;
import android.content.SharedPreferences;

public final class VideoInsightStore {
    private final SharedPreferences preferences;

    public VideoInsightStore(Context context) {
        preferences = context.getSharedPreferences("video_insights", Context.MODE_PRIVATE);
    }

    public void save(VideoFeatures video) {
        preferences.edit().putString(video.id(), encode(video)).apply();
    }

    public VideoFeatures load(String id) {
        String encoded = preferences.getString(id, null);
        if (encoded == null) return null;
        String[] parts = encoded.split("\\|");
        if (parts.length != 10) return null;
        try {
            int version = Integer.parseInt(parts[0]);
            if (version != VideoFeatures.SCHEMA_VERSION) return null;
            return new VideoFeatures(version, id, Long.parseLong(parts[1]),
                    Integer.parseInt(parts[2]), Double.parseDouble(parts[3]),
                    Double.parseDouble(parts[4]), Double.parseDouble(parts[5]),
                    Double.parseDouble(parts[6]), Double.parseDouble(parts[7]),
                    Double.parseDouble(parts[8]), Double.parseDouble(parts[9]));
        } catch (NumberFormatException invalid) {
            return null;
        }
    }

    public void clear() { preferences.edit().clear().apply(); }

    private static String encode(VideoFeatures video) {
        return video.schemaVersion() + "|" + video.durationMillis() + "|"
                + video.sampledFrames() + "|" + video.detail() + "|" + video.focus() + "|"
                + video.exposure() + "|" + video.composition() + "|" + video.frameStability()
                + "|" + video.blackFrameRate() + "|" + video.frozenFrameRate();
    }
}
