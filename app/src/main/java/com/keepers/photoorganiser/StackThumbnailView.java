package com.keepers.photoorganiser;

import android.content.Context;
import android.view.View;
import android.widget.FrameLayout;
import android.widget.ImageView;

public final class StackThumbnailView {
    private StackThumbnailView() {}

    public static FrameLayout create(Context context, boolean selected) {
        int densityInset = Math.max(1, Math.round(3 * context.getResources()
                .getDisplayMetrics().density));
        FrameLayout frame = new FrameLayout(context);
        frame.setClipChildren(false);
        ImageView image = new ImageView(context);
        image.setScaleType(ImageView.ScaleType.CENTER_CROP);
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

    public static ImageView image(FrameLayout thumbnail) {
        return (ImageView) thumbnail.getChildAt(0);
    }
}
