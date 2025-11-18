package com.skilllink.ui.worker.earnings;

import static org.junit.Assert.assertNotNull;

import android.os.Build;

import android.view.LayoutInflater;
import android.view.View;

import androidx.fragment.app.testing.FragmentScenario;
import androidx.test.core.app.ApplicationProvider;

import com.skilllink.R;
import com.google.android.material.chip.ChipGroup;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.annotation.Config;
import org.robolectric.shadows.ShadowLooper;

@RunWith(RobolectricTestRunner.class)
@Config(sdk = Build.VERSION_CODES.UPSIDE_DOWN_CAKE)
public class EarningsFragmentInflationTest {

    @Test
    public void fragmentInflatesWithoutCrash() {
        FragmentScenario<EarningsFragment> scenario = FragmentScenario.launchInContainer(
                EarningsFragment.class,
                null,
                R.style.Theme_SkillLink,
                null
        );

        scenario.onFragment(fragment -> assertNotNull(fragment.getView()));
        ShadowLooper.runUiThreadTasksIncludingDelayedTasks();
    }

    @Test
    public void layoutInflatesSuccessfully() {
        LayoutInflater inflater = LayoutInflater.from(ApplicationProvider.getApplicationContext());
        assertNotNull(inflater.inflate(R.layout.fragment_worker_earnings, null, false));
    }

    @Test
    public void selectingAllRangesDoesNotCrash() {
        FragmentScenario<EarningsFragment> scenario = FragmentScenario.launchInContainer(
                EarningsFragment.class,
                null,
                R.style.Theme_SkillLink,
                null
        );

        scenario.onFragment(fragment -> {
            View root = fragment.getView();
            assertNotNull(root);
            ChipGroup chipGroup = root.findViewById(R.id.chipRange);
            assertNotNull(chipGroup);
            chipGroup.check(R.id.chipRangeWeek);
            chipGroup.check(R.id.chipRangeMonth);
            chipGroup.check(R.id.chipRangeQuarter);
        });
        ShadowLooper.runUiThreadTasksIncludingDelayedTasks();
    }
}
