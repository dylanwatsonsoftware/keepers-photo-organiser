package com.keepers.photoorganiser;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import android.content.Intent;
import android.net.Uri;
import androidx.browser.customtabs.CustomTabsIntent;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;

@RunWith(RobolectricTestRunner.class)
public class PickerBrowserIntentFactoryTest {
    @Test public void pickerOpensInAnAppOwnedCustomTab() {
        Uri picker = Uri.parse("https://photos.google.com/picker/session/autoclose");

        Intent intent = PickerBrowserIntentFactory.create(picker);

        assertEquals(Intent.ACTION_VIEW, intent.getAction());
        assertEquals(picker, intent.getData());
        assertTrue(intent.hasExtra(CustomTabsIntent.EXTRA_SESSION));
    }
}
