package com.keepers.photoorganiser;

import android.content.Context;
import android.content.res.ColorStateList;
import android.graphics.PixelFormat;
import android.graphics.drawable.GradientDrawable;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.TextView;

final class AlbumAutomationOverlay {
    private final Context context;
    private final WindowManager windows;
    private View view;

    AlbumAutomationOverlay(Context context) {
        this.context = context;
        windows = (WindowManager) context.getSystemService(Context.WINDOW_SERVICE);
    }

    void show(AlbumAutomationProgress progress) {
        performSafely(() -> showUnsafe(progress));
    }

    private void showUnsafe(AlbumAutomationProgress progress) {
        if (view != null) {
            updateView(view, progress);
            return;
        }
        View candidate = createView(context, progress);
        WindowManager.LayoutParams params = layoutParams();
        params.y = dp(context, 52);
        windows.addView(candidate, params);
        view = candidate;
    }

    void hide() {
        View attached = view;
        view = null;
        if (attached != null) performSafely(() -> windows.removeView(attached));
    }

    static WindowManager.LayoutParams layoutParams() {
        WindowManager.LayoutParams params = new WindowManager.LayoutParams(
                WindowManager.LayoutParams.WRAP_CONTENT,
                WindowManager.LayoutParams.WRAP_CONTENT,
                WindowManager.LayoutParams.TYPE_ACCESSIBILITY_OVERLAY,
                WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE
                        | WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE
                        | WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN,
                PixelFormat.TRANSLUCENT);
        params.gravity = Gravity.TOP | Gravity.CENTER_HORIZONTAL;
        return params;
    }

    static View createView(Context context, AlbumAutomationProgress progress) {
        LinearLayout card = new LinearLayout(context);
        card.setOrientation(LinearLayout.VERTICAL);
        card.setPadding(dp(context, 16), dp(context, 9), dp(context, 16), dp(context, 10));
        card.setMinimumWidth(dp(context, 280));
        card.setElevation(dp(context, 8));
        GradientDrawable background = new GradientDrawable();
        background.setColor(0xEE202124);
        background.setCornerRadius(dp(context, 18));
        background.setStroke(dp(context, 1), 0xCCFFD38A);
        card.setBackground(background);

        TextView title = new TextView(context);
        title.setTag("album_overlay_title");
        title.setTextColor(0xFFFFFFFF);
        title.setTextSize(14);
        title.setTypeface(android.graphics.Typeface.DEFAULT,
                android.graphics.Typeface.BOLD);
        card.addView(title, new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));

        TextView detail = new TextView(context);
        detail.setTag("album_overlay_detail");
        detail.setTextColor(0xFFFFD38A);
        detail.setTextSize(12);
        detail.setSingleLine(true);
        detail.setEllipsize(android.text.TextUtils.TruncateAt.END);
        LinearLayout.LayoutParams detailParams = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        detailParams.setMargins(0, dp(context, 2), 0, dp(context, 6));
        card.addView(detail, detailParams);

        ProgressBar bar = new ProgressBar(context, null, android.R.attr.progressBarStyleHorizontal);
        bar.setTag("album_overlay_progress");
        bar.setProgressTintList(ColorStateList.valueOf(0xFFFFB74D));
        bar.setProgressBackgroundTintList(ColorStateList.valueOf(0xFF5F6368));
        card.addView(bar, new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, dp(context, 4)));
        updateView(card, progress);
        return card;
    }

    private static void updateView(View root, AlbumAutomationProgress progress) {
        ((TextView) root.findViewWithTag("album_overlay_title")).setText(progress.title());
        ((TextView) root.findViewWithTag("album_overlay_detail")).setText(progress.detail());
        ProgressBar bar = root.findViewWithTag("album_overlay_progress");
        bar.setMax(progress.total());
        bar.setProgress(progress.position());
    }

    static boolean performSafely(Runnable operation) {
        try {
            operation.run();
            return true;
        } catch (RuntimeException unavailable) {
            return false;
        }
    }

    private static int dp(Context context, int value) {
        return Math.round(value * context.getResources().getDisplayMetrics().density);
    }
}
