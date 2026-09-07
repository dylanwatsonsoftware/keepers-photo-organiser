package com.keepers.photoorganiser;

import static org.junit.Assert.assertEquals;

import android.content.Intent;
import android.net.Uri;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;

@RunWith(RobolectricTestRunner.class)
public class PreviewPageRequestTest {
    @Test public void adjacentPhotoRequestPreservesReviewWindow() {
        Intent current = new Intent().setData(Uri.parse("content://media/photo/1"))
                .putExtra(ReviewActivity.EXTRA_REVIEW_LIMIT, 120);

        Intent adjacent = PreviewPageRequest.forPhoto(current,
                Uri.parse("content://media/photo/2"));

        assertEquals(Uri.parse("content://media/photo/2"), adjacent.getData());
        assertEquals(120, adjacent.getIntExtra(ReviewActivity.EXTRA_REVIEW_LIMIT, 0));
    }
}
