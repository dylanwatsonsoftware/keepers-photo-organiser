package com.keepers.photoorganiser;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import android.net.Uri;
import android.widget.Button;
import android.widget.TextView;
import java.util.Arrays;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.Robolectric;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.Shadows;

@RunWith(RobolectricTestRunner.class)
public class MainActivityTest {
    @Test
    public void actionsStayDisabledUntilPhotosAreSelected() {
        MainActivity activity = Robolectric.buildActivity(MainActivity.class).setup().get();

        assertEquals("No photos selected", text(activity, R.id.selection_status));
        assertFalse(button(activity, R.id.open_existing).isEnabled());
        assertFalse(button(activity, R.id.request_favourite).isEnabled());
        assertFalse(button(activity, R.id.share_experiment).isEnabled());
    }

    @Test
    public void selectedPhotosEnableSafeTestsAndGuardBatchShare() {
        MainActivity activity = Robolectric.buildActivity(MainActivity.class).setup().get();

        activity.showSelection(Arrays.asList(
                Uri.parse("content://media/photo/1"),
                Uri.parse("content://media/photo/2")));

        assertEquals("2 photos selected", text(activity, R.id.selection_status));
        assertTrue(button(activity, R.id.open_existing).isEnabled());
        assertTrue(button(activity, R.id.request_favourite).isEnabled());
        assertTrue(button(activity, R.id.share_experiment).isEnabled());
        assertTrue(button(activity, R.id.share_experiment).getText().toString()
                .contains("DUPLICATE RISK"));
    }

    @Test public void favoriteDiagnosticExplainsTheObservedMediaStoreState() {
        MainActivity activity = Robolectric.buildActivity(MainActivity.class).setup().get();

        activity.showFavoriteDiagnostic(new FavoriteDiagnostic(12,
                Arrays.asList(Uri.parse("content://media/photo/1"))));

        assertEquals("MediaStore reports 1 favourite among 12 recent local camera photos.",
                text(activity, R.id.favorite_diagnostic_status));
    }

    @Test public void peopleSetupOpensGuidedProfileScreen() {
        MainActivity activity = Robolectric.buildActivity(MainActivity.class).setup().get();

        activity.findViewById(R.id.open_people).performClick();

        assertEquals(PeopleActivity.class.getName(), Shadows.shadowOf(activity)
                .getNextStartedActivity().getComponent().getClassName());
    }

    private static Button button(MainActivity activity, int id) {
        return activity.findViewById(id);
    }

    private static String text(MainActivity activity, int id) {
        return ((TextView) activity.findViewById(id)).getText().toString();
    }
}
