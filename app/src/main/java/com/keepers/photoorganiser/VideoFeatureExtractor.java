package com.keepers.photoorganiser;

import android.graphics.Bitmap;
import android.graphics.Color;
import java.util.List;

public final class VideoFeatureExtractor {
    private VideoFeatureExtractor() {}

    public static VideoFeatures fromFrames(String id, long durationMillis, List<Bitmap> frames) {
        if (frames.isEmpty()) return new VideoFeatures(VideoFeatures.SCHEMA_VERSION, id,
                durationMillis, 0, 0, 0, 0, 0, 0, 0, 0);
        double detail = 0;
        double focus = 0;
        double exposure = 0;
        double composition = 0;
        double stability = 0;
        int blackFrames = 0;
        int frozenTransitions = 0;
        Long previousHash = null;
        for (Bitmap frame : frames) {
            PhotoQualityAssessment assessment = PhotoFeatureExtractor.assess(frame);
            detail += assessment.detail();
            focus += assessment.focus();
            exposure += assessment.exposure();
            composition += assessment.composition();
            stability += assessment.motionStability();
            if (averageLuminance(frame) <= 12) blackFrames++;
            long hash = PhotoFeatureExtractor.hash(frame);
            if (previousHash != null && Long.bitCount(previousHash ^ hash) <= 2)
                frozenTransitions++;
            previousHash = hash;
        }
        double count = frames.size();
        return new VideoFeatures(VideoFeatures.SCHEMA_VERSION, id, durationMillis, frames.size(),
                detail / count, focus / count, exposure / count, composition / count,
                stability / count, blackFrames / count,
                frames.size() < 2 ? 0 : frozenTransitions / (count - 1));
    }

    private static double averageLuminance(Bitmap source) {
        Bitmap sample = Bitmap.createScaledBitmap(source, 16, 16, true);
        long total = 0;
        for (int y = 0; y < sample.getHeight(); y++) {
            for (int x = 0; x < sample.getWidth(); x++) {
                int color = sample.getPixel(x, y);
                total += (Color.red(color) * 299L + Color.green(color) * 587L
                        + Color.blue(color) * 114L) / 1000L;
            }
        }
        if (sample != source) sample.recycle();
        return total / (double) (16 * 16);
    }
}
