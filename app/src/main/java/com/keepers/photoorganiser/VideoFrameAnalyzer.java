package com.keepers.photoorganiser;

import android.content.Context;
import android.graphics.Bitmap;
import android.media.MediaMetadataRetriever;
import android.net.Uri;
import java.util.ArrayList;
import java.util.List;

public final class VideoFrameAnalyzer {
    private static final int MAX_FRAME_EDGE = 384;
    private final Context context;

    public VideoFrameAnalyzer(Context context) {
        this.context = context.getApplicationContext();
    }

    public VideoFeatures analyze(RecentPhoto video) {
        MediaMetadataRetriever retriever = new MediaMetadataRetriever();
        ArrayList<Bitmap> frames = new ArrayList<>();
        try {
            retriever.setDataSource(context, video.uri());
            long duration = video.durationMillis() > 0 ? video.durationMillis()
                    : longMetadata(retriever, MediaMetadataRetriever.METADATA_KEY_DURATION);
            int width = (int) longMetadata(retriever,
                    MediaMetadataRetriever.METADATA_KEY_VIDEO_WIDTH);
            int height = (int) longMetadata(retriever,
                    MediaMetadataRetriever.METADATA_KEY_VIDEO_HEIGHT);
            int[] size = sampleSize(width, height);
            for (long time : sampleTimesMicros(duration)) {
                if (Thread.currentThread().isInterrupted()) return null;
                Bitmap frame = retriever.getScaledFrameAtTime(time,
                        MediaMetadataRetriever.OPTION_CLOSEST_SYNC, size[0], size[1]);
                if (frame != null) frames.add(frame);
            }
            if (frames.isEmpty()) return null;
            return VideoFeatureExtractor.fromFrames(video.uri().toString(), duration, frames);
        } catch (RuntimeException unavailable) {
            return null;
        } finally {
            for (Bitmap frame : frames) if (!frame.isRecycled()) frame.recycle();
            try { retriever.release(); } catch (Exception ignored) {}
        }
    }

    static List<Long> sampleTimesMicros(long durationMillis) {
        long durationMicros = Math.max(1, durationMillis) * 1_000L;
        return List.of(Math.round(durationMicros * .15), Math.round(durationMicros * .50),
                Math.round(durationMicros * .85));
    }

    private static int[] sampleSize(int width, int height) {
        if (width <= 0 || height <= 0) return new int[]{MAX_FRAME_EDGE, MAX_FRAME_EDGE};
        double scale = Math.min(1, MAX_FRAME_EDGE / (double) Math.max(width, height));
        return new int[]{Math.max(1, (int) Math.round(width * scale)),
                Math.max(1, (int) Math.round(height * scale))};
    }

    private static long longMetadata(MediaMetadataRetriever retriever, int key) {
        try {
            String value = retriever.extractMetadata(key);
            return value == null ? 0 : Long.parseLong(value);
        } catch (NumberFormatException invalid) {
            return 0;
        }
    }
}
