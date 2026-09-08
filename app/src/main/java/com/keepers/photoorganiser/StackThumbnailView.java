package com.keepers.photoorganiser;

import android.content.Context;
import android.graphics.Color;
import android.graphics.drawable.GradientDrawable;
import android.view.Gravity;
import android.view.View;
import android.widget.FrameLayout;
import android.widget.ImageView;

public final class StackThumbnailView {
    private StackThumbnailView() {}

    public static FrameLayout create(Context context, boolean selected) {
        int densityInset = Math.max(1, Math.round(3 * context.getResources()
                .getDisplayMetrics().density));
        FrameLayout frame = new FrameLayout(context);
        frame.setClipChildren(true);
        ImageView image = new ImageView(context);
        image.setScaleType(ImageView.ScaleType.CENTER_CROP);
        image.setBackgroundResource(R.drawable.stack_thumbnail_mask);
        image.setClipToOutline(true);
        image.setAlpha(selected ? 1f : .72f);
        FrameLayout.LayoutParams imageParams = new FrameLayout.LayoutParams(
                FrameLayout.LayoutParams.MATCH_PARENT, FrameLayout.LayoutParams.MATCH_PARENT);
        imageParams.setMargins(densityInset, densityInset, densityInset, densityInset);
        frame.addView(image, imageParams);
        View outline = new View(context);
        outline.setBackgroundResource(R.drawable.stack_thumbnail_selected);
        outline.setVisibility(selected ? View.VISIBLE : View.INVISIBLE);
        int borderInset = Math.max(1, Math.round(context.getResources()
                .getDisplayMetrics().density));
        FrameLayout.LayoutParams outlineParams = new FrameLayout.LayoutParams(
                FrameLayout.LayoutParams.MATCH_PARENT, FrameLayout.LayoutParams.MATCH_PARENT);
        outlineParams.setMargins(borderInset, borderInset, borderInset, borderInset);
        frame.addView(outline, outlineParams);
        return frame;
    }

    public static FrameLayout create(Context context, boolean selected, boolean keeper,
            boolean recommended, boolean alternative) {
        FrameLayout frame = create(context, selected);
        int iconSize = dp(context, 24);
        int iconMargin = dp(context, 3);

        ImageView heart = new ImageView(context);
        heart.setTag("stack_heart");
        heart.setImageResource(keeper ? R.drawable.ic_heart_filled : R.drawable.ic_heart_outline);
        heart.setColorFilter(keeper ? Color.rgb(234, 67, 53) : Color.WHITE);
        heart.setPadding(dp(context, keeper ? 4 : 5), dp(context, keeper ? 4 : 5),
                dp(context, keeper ? 4 : 5), dp(context, keeper ? 4 : 5));
        heart.setContentDescription(keeper ? "Keeper" : "Not a Keeper");
        FrameLayout.LayoutParams heartParams = new FrameLayout.LayoutParams(iconSize, iconSize,
                Gravity.TOP | Gravity.END);
        heartParams.setMargins(0, iconMargin, iconMargin, 0);
        frame.addView(heart, heartParams);

        ImageView star = new ImageView(context);
        star.setTag("stack_star");
        star.setImageResource(alternative ? R.drawable.ic_star_outline : R.drawable.ic_star);
        star.setColorFilter(Color.WHITE);
        star.setPadding(dp(context, 4), dp(context, 4), dp(context, 4), dp(context, 4));
        if (recommended) {
            GradientDrawable circle = new GradientDrawable();
            circle.setShape(GradientDrawable.OVAL);
            circle.setColor(Color.rgb(176, 96, 0));
            star.setBackground(circle);
        }
        star.setContentDescription(recommended ? "Recommended best shot"
                : alternative ? "Good alternative" : null);
        star.setVisibility(recommended || alternative ? View.VISIBLE : View.GONE);
        FrameLayout.LayoutParams starParams = new FrameLayout.LayoutParams(iconSize, iconSize,
                Gravity.TOP | Gravity.START);
        starParams.setMargins(iconMargin, iconMargin, 0, 0);
        frame.addView(star, starParams);
        return frame;
    }

    public static ImageView image(FrameLayout thumbnail) {
        return (ImageView) thumbnail.getChildAt(0);
    }

    public static ImageView heart(FrameLayout thumbnail) {
        return thumbnail.findViewWithTag("stack_heart");
    }

    public static ImageView star(FrameLayout thumbnail) {
        return thumbnail.findViewWithTag("stack_star");
    }

    private static int dp(Context context, int value) {
        return Math.round(value * context.getResources().getDisplayMetrics().density);
    }
}
