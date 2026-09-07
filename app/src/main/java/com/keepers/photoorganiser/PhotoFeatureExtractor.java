package com.keepers.photoorganiser;

import android.graphics.Bitmap;
import android.graphics.Color;

public final class PhotoFeatureExtractor {
    private PhotoFeatureExtractor() {}

    public static long hash(Bitmap source) {
        Bitmap scaled = Bitmap.createScaledBitmap(source, 9, 8, true);
        long hash = 0;
        int bit = 0;
        for (int y = 0; y < 8; y++) {
            for (int x = 0; x < 8; x++) {
                if (luminance(scaled.getPixel(x, y)) > luminance(scaled.getPixel(x + 1, y))) {
                    hash |= 1L << bit;
                }
                bit++;
            }
        }
        if (scaled != source) scaled.recycle();
        return hash;
    }

    public static double quality(Bitmap source) {
        return assess(source).detail();
    }

    public static PhotoQualityAssessment assess(Bitmap source) {
        Bitmap scaled = Bitmap.createScaledBitmap(source, 32, 32, true);
        double detail = 0;
        double centreDetail = 0;
        double horizontal = 0;
        double vertical = 0;
        double luminanceTotal = 0;
        int clipped = 0;
        double[] quadrant = new double[4];
        for (int y = 1; y < 32; y++) {
            for (int x = 1; x < 32; x++) {
                int current = luminance(scaled.getPixel(x, y));
                double dx = Math.abs(current - luminance(scaled.getPixel(x - 1, y)));
                double dy = Math.abs(current - luminance(scaled.getPixel(x, y - 1)));
                detail += dx + dy;
                horizontal += dx;
                vertical += dy;
                if (x >= 8 && x < 24 && y >= 8 && y < 24) centreDetail += dx + dy;
                luminanceTotal += current;
                if (current < 10 || current > 245) clipped++;
                quadrant[(y >= 16 ? 2 : 0) + (x >= 16 ? 1 : 0)] += current;
            }
        }
        double normalizedDetail = clamp(detail / (31d * 31d * 2d * 80d));
        double focus = clamp(centreDetail / (16d * 16d * 2d * 55d));
        double mean = luminanceTotal / (31d * 31d);
        double midpoint = 1 - Math.min(1, Math.abs(mean - 128) / 128d);
        double exposure = clamp(midpoint * (1 - clipped / (31d * 31d)));
        double directionBalance = Math.min(horizontal, vertical)
                / Math.max(1, Math.max(horizontal, vertical));
        double motionStability = clamp(normalizedDetail * 0.7 + directionBalance * 0.3);
        double maxQuadrant = Math.max(Math.max(quadrant[0], quadrant[1]),
                Math.max(quadrant[2], quadrant[3]));
        double minQuadrant = Math.min(Math.min(quadrant[0], quadrant[1]),
                Math.min(quadrant[2], quadrant[3]));
        double composition = clamp(0.5 + 0.5 * minQuadrant / Math.max(1, maxQuadrant));
        if (scaled != source) scaled.recycle();
        return new PhotoQualityAssessment(focus, exposure, composition, motionStability,
                normalizedDetail);
    }

    private static double clamp(double value) {
        return Math.max(0, Math.min(1, value));
    }

    private static int luminance(int color) {
        return (Color.red(color) * 299 + Color.green(color) * 587
                + Color.blue(color) * 114) / 1000;
    }
}
