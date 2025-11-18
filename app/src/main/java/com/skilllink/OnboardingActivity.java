package com.skilllink;

import android.content.Intent;
import android.os.Bundle;
import android.transition.AutoTransition;
import android.transition.TransitionManager;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.AccelerateDecelerateInterpolator;

import androidx.appcompat.app.AppCompatActivity;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;
import androidx.lifecycle.Lifecycle;
import androidx.viewpager2.adapter.FragmentStateAdapter;
import androidx.viewpager2.widget.ViewPager2;

import com.google.android.material.button.MaterialButton;
import com.skilllink.ui.OnboardingFragment;

import java.util.ArrayList;
import java.util.List;

public class OnboardingActivity extends AppCompatActivity {

    private ViewPager2 viewPager;
    private View indicator1, indicator2, indicator3;
    private View topBubble, bottomBubble;
    private MaterialButton actionButton;
    private MaterialButton skipButton;
    private final List<View> indicators = new ArrayList<>();
    private final AutoTransition indicatorTransition = new AutoTransition();
    private final float[][] topBubblePositionsDp = new float[][]{
            {-80f, -100f},
            {48f, -64f},
            {-12f, -24f}
    };
    private final float[][] bottomBubblePositionsDp = new float[][]{
            {72f, 110f},
            {-36f, 128f},
            {88f, 72f}
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_onboarding);

        initializeViews();
        setupViewPager();
        setupIndicators();
        setupClickListeners();
    }

    private void initializeViews() {
        viewPager = findViewById(R.id.onboarding_pager);
        indicator1 = findViewById(R.id.indicator_1);
        indicator2 = findViewById(R.id.indicator_2);
        indicator3 = findViewById(R.id.indicator_3);
        topBubble = findViewById(R.id.top_bubble);
        bottomBubble = findViewById(R.id.bottom_bubble);
        actionButton = findViewById(R.id.action_button);
        skipButton = findViewById(R.id.skip_button);
        indicators.clear();
        indicators.add(indicator1);
        indicators.add(indicator2);
        indicators.add(indicator3);
        indicatorTransition.setDuration(200);
        skipButton.setAlpha(1f);
        skipButton.setVisibility(View.VISIBLE);
        skipButton.setEnabled(true);
        updateBackgroundBubbles(0, false);
    }

    private void setupViewPager() {
        OnboardingPagerAdapter adapter = new OnboardingPagerAdapter(getSupportFragmentManager(), getLifecycle());
        viewPager.setAdapter(adapter);
        viewPager.setClipToPadding(false);
        viewPager.setClipChildren(false);
        viewPager.setOffscreenPageLimit(adapter.getItemCount());
        viewPager.setPageTransformer(createPageTransformer());
        View recyclerView = viewPager.getChildAt(0);
        if (recyclerView instanceof androidx.recyclerview.widget.RecyclerView) {
            recyclerView.setOverScrollMode(View.OVER_SCROLL_NEVER);
        }

        viewPager.registerOnPageChangeCallback(new ViewPager2.OnPageChangeCallback() {
            @Override
            public void onPageSelected(int position) {
                super.onPageSelected(position);
                updateIndicators(position);
                updateActionButtonText(position);
                updateSkipButtonVisibility(position);
                updateBackgroundBubbles(position, true);
            }
        });
    }

    private void updateBackgroundBubbles(int position, boolean animate) {
        if (topBubble == null || bottomBubble == null) {
            return;
        }
        if (position < 0 || position >= topBubblePositionsDp.length) {
            return;
        }
        float[] topTarget = topBubblePositionsDp[position];
        float[] bottomTarget = bottomBubblePositionsDp[position];
        float topX = dpToPxF(topTarget[0]);
        float topY = dpToPxF(topTarget[1]);
        float bottomX = dpToPxF(bottomTarget[0]);
        float bottomY = dpToPxF(bottomTarget[1]);
        if (animate) {
            topBubble.animate()
                    .translationX(topX)
                    .translationY(topY)
                    .setDuration(500)
                    .setInterpolator(new AccelerateDecelerateInterpolator())
                    .start();
            bottomBubble.animate()
                    .translationX(bottomX)
                    .translationY(bottomY)
                    .setDuration(500)
                    .setInterpolator(new AccelerateDecelerateInterpolator())
                    .start();
        } else {
            topBubble.setTranslationX(topX);
            topBubble.setTranslationY(topY);
            bottomBubble.setTranslationX(bottomX);
            bottomBubble.setTranslationY(bottomY);
        }
    }

    private void setupIndicators() {
        updateIndicators(0); // First page is selected by default
    }

    private void updateIndicators(int position) {
        ViewGroup parent = (ViewGroup) indicator1.getParent();
        TransitionManager.beginDelayedTransition(parent, indicatorTransition);

        for (int i = 0; i < indicators.size(); i++) {
            View indicator = indicators.get(i);
            ViewGroup.LayoutParams params = indicator.getLayoutParams();
            params.width = i == position ? dpToPx(28) : dpToPx(12);
            params.height = dpToPx(10);
            indicator.setLayoutParams(params);
            indicator.setBackgroundResource(i == position ? R.drawable.bg_onboarding_indicator_active : R.drawable.bg_onboarding_indicator_inactive);
            indicator.setAlpha(i == position ? 1f : 0.6f);
        }
    }

    private void updateActionButtonText(int position) {
        actionButton.setText(position < 2 ? R.string.onboarding_next : R.string.onboarding_get_started);
        actionButton.setScaleX(0.96f);
        actionButton.setScaleY(0.96f);
        actionButton.animate()
                .scaleX(1f)
                .scaleY(1f)
                .setDuration(200)
                .setInterpolator(new AccelerateDecelerateInterpolator())
                .start();
    }

    private void updateSkipButtonVisibility(int position) {
        boolean shouldShow = position < 2;
        skipButton.animate()
                .alpha(shouldShow ? 1f : 0f)
                .setDuration(200)
                .withStartAction(() -> {
                    if (shouldShow) {
                        skipButton.setVisibility(View.VISIBLE);
                        skipButton.setEnabled(true);
                    }
                })
                .withEndAction(() -> {
                    if (!shouldShow) {
                        skipButton.setVisibility(View.INVISIBLE);
                        skipButton.setEnabled(false);
                    }
                })
                .start();
    }

    private void setupClickListeners() {
        actionButton.setOnClickListener(v -> {
            int currentPosition = viewPager.getCurrentItem();
            if (currentPosition < 2) {
                viewPager.setCurrentItem(currentPosition + 1);
            } else {
                navigateToRoleSelection();
            }
        });

        skipButton.setOnClickListener(v -> navigateToRoleSelection());
    }

    private void navigateToRoleSelection() {
        Intent intent = new Intent(OnboardingActivity.this, RoleSelectionActivity.class);
        startActivity(intent);
        finish();
    }

    private ViewPager2.PageTransformer createPageTransformer() {
        return (page, position) -> {
            float scale = 1 - (0.12f * Math.abs(position));
            float alpha = 1 - (0.3f * Math.abs(position));
            page.setScaleY(scale);
            page.setScaleX(scale);
            page.setAlpha(alpha);
        };
    }

    private int dpToPx(int dp) {
        float density = getResources().getDisplayMetrics().density;
        return Math.round(dp * density);
    }

    private float dpToPxF(float dp) {
        return dp * getResources().getDisplayMetrics().density;
    }

    private class OnboardingPagerAdapter extends FragmentStateAdapter {
        public OnboardingPagerAdapter(FragmentManager fm, Lifecycle lifecycle) {
            super(fm, lifecycle);
        }

        @Override
        public Fragment createFragment(int position) {
            return OnboardingFragment.newInstance(position);
        }

        @Override
        public int getItemCount() {
            return 3; // Three onboarding screens
        }
    }
}