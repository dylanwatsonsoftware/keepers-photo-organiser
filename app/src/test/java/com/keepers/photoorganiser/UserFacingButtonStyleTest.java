package com.keepers.photoorganiser;

import static org.junit.Assert.assertFalse;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.RuntimeEnvironment;

@RunWith(RobolectricTestRunner.class)
public class UserFacingButtonStyleTest {
    @Test public void userFacingScreensNeverFallBackToStockAndroidButtons() {
        int[] layouts = {
                R.layout.activity_review,
                R.layout.activity_preview,
                R.layout.activity_people,
                R.layout.activity_person_detail,
                R.layout.activity_face_group_review,
                R.layout.activity_album_review
        };
        LayoutInflater inflater = LayoutInflater.from(RuntimeEnvironment.getApplication());
        for (int layout : layouts)
            assertFalse("Layout contains a stock Android Button: " + layout,
                    containsButton(inflater.inflate(layout, null)));
    }

    private static boolean containsButton(View view) {
        if (view instanceof Button) return true;
        if (view instanceof ViewGroup) for (int index = 0;
                index < ((ViewGroup) view).getChildCount(); index++)
            if (containsButton(((ViewGroup) view).getChildAt(index))) return true;
        return false;
    }
}
