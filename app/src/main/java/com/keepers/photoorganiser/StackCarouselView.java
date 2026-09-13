package com.keepers.photoorganiser;

import android.content.Context;
import android.util.AttributeSet;
import android.view.MotionEvent;
import android.widget.HorizontalScrollView;

public final class StackCarouselView extends HorizontalScrollView {
    interface GestureListener {
        void onGestureEvent(MotionEvent event);
    }

    private GestureListener gestureListener;

    public StackCarouselView(Context context, AttributeSet attributes) {
        super(context, attributes);
    }

    void setGestureListener(GestureListener listener) {
        gestureListener = listener;
    }

    @Override public boolean dispatchTouchEvent(MotionEvent event) {
        if (gestureListener != null) gestureListener.onGestureEvent(event);
        return super.dispatchTouchEvent(event);
    }
}
