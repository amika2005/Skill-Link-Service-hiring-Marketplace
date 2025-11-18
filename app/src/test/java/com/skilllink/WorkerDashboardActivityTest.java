package com.skilllink;

import static org.junit.Assert.assertNotNull;

import android.os.Build;

import androidx.test.core.app.ActivityScenario;

import com.google.android.material.bottomnavigation.BottomNavigationView;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.annotation.Config;
import org.robolectric.shadows.ShadowLooper;

@RunWith(RobolectricTestRunner.class)
@Config(sdk = Build.VERSION_CODES.UPSIDE_DOWN_CAKE)
public class WorkerDashboardActivityTest {

    @Test
    public void navigatingToEarningsDoesNotCrash() {
        try (ActivityScenario<WorkerDashboardActivity> scenario = ActivityScenario.launch(WorkerDashboardActivity.class)) {
            scenario.onActivity(activity -> {
                BottomNavigationView navView = activity.findViewById(R.id.bottom_navigation);
                assertNotNull(navView);
                navView.setSelectedItemId(R.id.nav_worker_earnings);
            });
            ShadowLooper.runUiThreadTasksIncludingDelayedTasks();
        }
    }
}
