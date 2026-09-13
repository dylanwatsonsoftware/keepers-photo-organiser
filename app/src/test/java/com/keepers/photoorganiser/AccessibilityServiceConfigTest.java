package com.keepers.photoorganiser;

import static org.junit.Assert.assertEquals;

import android.content.res.XmlResourceParser;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.RuntimeEnvironment;
import org.xmlpull.v1.XmlPullParser;

@RunWith(RobolectricTestRunner.class)
public class AccessibilityServiceConfigTest {
    @Test public void albumAutomationCanPerformTheTapThatRevealsVideoControls()
            throws Exception {
        XmlResourceParser parser = RuntimeEnvironment.getApplication().getResources()
                .getXml(R.xml.accessibility_service_config);
        while (parser.next() != XmlPullParser.START_TAG) {}

        assertEquals("true", parser.getAttributeValue(
                "http://schemas.android.com/apk/res/android", "canPerformGestures"));
        parser.close();
    }
}
