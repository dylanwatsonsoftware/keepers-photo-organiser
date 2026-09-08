package com.keepers.photoorganiser;

import android.graphics.Bitmap;
import android.graphics.Color;
import java.util.Locale;

public final class FaceDescriptor {
    private static final int SIDE = 8;
    private FaceDescriptor() {}

    public static double[] extract(Bitmap bitmap, FaceObservation face) {
        double[] values = new double[SIDE * SIDE];
        int left = pixel(face.left(), bitmap.getWidth());
        int top = pixel(face.top(), bitmap.getHeight());
        int right = Math.max(left + 1, pixel(face.right(), bitmap.getWidth()));
        int bottom = Math.max(top + 1, pixel(face.bottom(), bitmap.getHeight()));
        double mean = 0;
        for (int y = 0; y < SIDE; y++) for (int x = 0; x < SIDE; x++) {
            int sourceX = Math.min(bitmap.getWidth() - 1, left + (right - left) * x / SIDE);
            int sourceY = Math.min(bitmap.getHeight() - 1, top + (bottom - top) * y / SIDE);
            int color = bitmap.getPixel(sourceX, sourceY);
            double luminance = (Color.red(color) * .299 + Color.green(color) * .587
                    + Color.blue(color) * .114) / 255.0;
            values[y * SIDE + x] = luminance;
            mean += luminance;
        }
        mean /= values.length;
        double magnitude = 0;
        for (int i = 0; i < values.length; i++) {
            values[i] -= mean;
            magnitude += values[i] * values[i];
        }
        magnitude = Math.sqrt(magnitude);
        if (magnitude > 0) for (int i = 0; i < values.length; i++) values[i] /= magnitude;
        return values;
    }

    public static String encode(double[] descriptor) {
        StringBuilder result = new StringBuilder();
        for (double value : descriptor) {
            if (!result.isEmpty()) result.append(',');
            result.append(String.format(Locale.ROOT, "%.6f", value));
        }
        return result.toString();
    }

    static double[] decode(String encoded) {
        if (encoded == null || encoded.isEmpty()) return new double[0];
        String[] parts = encoded.split(",");
        double[] result = new double[parts.length];
        try {
            for (int i = 0; i < parts.length; i++) result[i] = Double.parseDouble(parts[i]);
        } catch (NumberFormatException invalid) { return new double[0]; }
        return result;
    }

    private static int pixel(double normalized, int size) {
        return Math.max(0, Math.min(size - 1, (int) Math.floor(normalized * size)));
    }
}
