package com.keepers.photoorganiser;

import static org.junit.Assert.assertTrue;

import android.Manifest;
import android.content.Context;
import android.content.pm.PackageManager;
import java.util.Arrays;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.RuntimeEnvironment;

@RunWith(RobolectricTestRunner.class)
public class FeedbackSyncManifestTest {
    @Test public void networkConstrainedWorkDeclaresConnectivityPermission() throws Exception {
        Context context = RuntimeEnvironment.getApplication();

        String[] permissions = context.getPackageManager().getPackageInfo(
                context.getPackageName(), PackageManager.GET_PERMISSIONS).requestedPermissions;
        assertTrue(Arrays.asList(permissions).contains(Manifest.permission.ACCESS_NETWORK_STATE));
    }
}
