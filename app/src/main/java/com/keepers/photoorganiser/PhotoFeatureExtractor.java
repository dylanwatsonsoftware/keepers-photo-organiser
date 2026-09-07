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
        Bitmap scaled = Bitmap.createScaledBitmap(source, 32, 32, true);
        double detail = 0;
        for (int y = 1; y < 32; y++) {
            for (int x = 1; x < 32; x++) {
                int current = luminance(scaled.getPixel(x, y));
                detail += Math.abs(current - luminance(scaled.getPixel(x - 1, y)));
                detail += Math.abs(current - luminance(scaled.getPixel(x, y - 1)));
            }
        }
        if (scaled != source) scaled.recycle();
        return detail / (31d * 31d * 2d * 255d);
    }

    private static int luminance(int color) {
        return (Color.red(color) * 299 + Color.green(color) * 587
                + Color.blue(color) * 114) / 1000;
    }
}
