package com.keepers.photoorganiser;

import static org.junit.Assert.assertTrue;

import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.view.MotionEvent;
import android.widget.TextView;
import java.lang.reflect.Method;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.Robolectric;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.RuntimeEnvironment;

@RunWith(RobolectricTestRunner.class)
public class PreviewQuickReviewTest {
    @Test public void rightSwipeMarksCurrentPhotoAsKeeper() throws Exception {
        Context context = RuntimeEnvironment.getApplication();
        Uri photo = Uri.parse("content://photo/quick-keep");
        PreviewActivity activity = Robolectric.buildActivity(PreviewActivity.class,
                new Intent(context, PreviewActivity.class).setData(photo)
                        .putExtra(PreviewActivity.EXTRA_QUICK_REVIEW, true)).setup().get();
        Method swipe = PreviewActivity.class.getDeclaredMethod("handleSwipe", MotionEvent.class);
        swipe.setAccessible(true);

        swipe.invoke(activity, event(MotionEvent.ACTION_DOWN, 100, 100));
        swipe.invoke(activity, event(MotionEvent.ACTION_UP, 190, 100));

        assertTrue(new KeeperSelectionStore(activity).load().contains(photo.toString()));
        assertTrue(((TextView) activity.findViewById(R.id.preview_hint)).getText().toString()
                .contains("right to keep"));
    }

    private static MotionEvent event(int action, float x, float y) {
        return MotionEvent.obtain(0, 10, action, x, y, 0);
    }
}
