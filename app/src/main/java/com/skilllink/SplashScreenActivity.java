package com.skilllink;

import android.animation.ObjectAnimator;
import android.animation.ValueAnimator;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.View;
import android.view.animation.AccelerateDecelerateInterpolator;
import android.view.animation.OvershootInterpolator;
import android.widget.ImageView;
import android.widget.TextView;
import androidx.appcompat.app.AppCompatActivity;
import com.skilllink.util.SessionManager;

public class SplashScreenActivity extends AppCompatActivity {

    private static final int SPLASH_DELAY = 2800;
    private final Handler splashHandler = new Handler(Looper.getMainLooper());
    private final Runnable navigationRunnable = this::navigateToOnboarding;
    private ObjectAnimator glowPulseAnimator;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_splash);

        // Hide the action bar for splash screen
        if (getSupportActionBar() != null) {
            getSupportActionBar().hide();
        }

        SessionManager sessionManager = new SessionManager(this);
        if (sessionManager.isLoggedIn()) {
            launchDashboard(sessionManager.getUserRole());
            return;
        }

        startIntroAnimation();
        splashHandler.postDelayed(navigationRunnable, SPLASH_DELAY);
    }

    private void launchDashboard(String role) {
        String resolvedRole = role != null ? role : "user";
        Intent intent;
        if ("worker".equals(resolvedRole)) {
            intent = new Intent(this, WorkerDashboardActivity.class);
        } else {
            intent = new Intent(this, UserDashboardActivity.class);
        }
        startActivity(intent);
        finish();
    }

    private void navigateToOnboarding() {
        Intent intent = new Intent(SplashScreenActivity.this, OnboardingActivity.class);
        startActivity(intent);
        finish();
    }

    private void startIntroAnimation() {
        ImageView logo = findViewById(R.id.splash_logo);
        View glow = findViewById(R.id.splash_glow);
        TextView title = findViewById(R.id.splash_app_name);
        TextView badge = findViewById(R.id.splash_badge);
        TextView tagline = findViewById(R.id.splash_tagline);
        View footerLine = findViewById(R.id.splash_footer_line);
        TextView footerText = findViewById(R.id.splash_footer_text);

        AccelerateDecelerateInterpolator smooth = new AccelerateDecelerateInterpolator();
        OvershootInterpolator overshoot = new OvershootInterpolator(1.4f);

        if (glow != null) {
            glow.setScaleX(0.2f);
            glow.setScaleY(0.2f);
            glow.setAlpha(0f);
            glow.animate()
                    .alpha(0.45f)
                    .scaleX(1f)
                    .scaleY(1f)
                    .setStartDelay(80)
                    .setDuration(720)
                    .setInterpolator(smooth)
                    .withEndAction(() -> {
                        glowPulseAnimator = ObjectAnimator.ofFloat(glow, View.ALPHA, 0.35f, 0.55f);
                        glowPulseAnimator.setRepeatMode(ValueAnimator.REVERSE);
                        glowPulseAnimator.setRepeatCount(ValueAnimator.INFINITE);
                        glowPulseAnimator.setDuration(1600);
                        glowPulseAnimator.setInterpolator(smooth);
                        glowPulseAnimator.start();
                    })
                    .start();
        }

        if (logo != null) {
            logo.setScaleX(0.6f);
            logo.setScaleY(0.6f);
            logo.setAlpha(0f);
            logo.animate()
                    .alpha(1f)
                    .scaleX(1f)
                    .scaleY(1f)
                    .setStartDelay(160)
                    .setDuration(680)
                    .setInterpolator(overshoot)
                    .start();
        }

        if (title != null) {
            title.setAlpha(0f);
            title.setTranslationY(36f);
            title.animate()
                    .alpha(1f)
                    .translationY(0f)
                    .setStartDelay(320)
                    .setDuration(480)
                    .setInterpolator(smooth)
                    .start();

            ValueAnimator letterAnimator = ValueAnimator.ofFloat(0.35f, 0.08f);
            letterAnimator.setStartDelay(320);
            letterAnimator.setDuration(700);
            letterAnimator.addUpdateListener(animation -> {
                Float value = (Float) animation.getAnimatedValue();
                title.setLetterSpacing(value);
            });
            letterAnimator.start();
        }

        if (badge != null) {
            badge.setAlpha(0f);
            badge.setTranslationY(24f);
            badge.animate()
                    .alpha(1f)
                    .translationY(0f)
                    .setStartDelay(420)
                    .setDuration(420)
                    .setInterpolator(smooth)
                    .start();
        }

        if (tagline != null) {
            tagline.setAlpha(0f);
            tagline.setTranslationY(28f);
            tagline.animate()
                    .alpha(1f)
                    .translationY(0f)
                    .setStartDelay(520)
                    .setDuration(420)
                    .setInterpolator(smooth)
                    .start();
        }

        if (footerLine != null) {
            footerLine.setAlpha(0f);
            footerLine.setScaleX(0f);
            footerLine.animate()
                    .alpha(1f)
                    .scaleX(1f)
                    .setStartDelay(640)
                    .setDuration(420)
                    .setInterpolator(smooth)
                    .start();
        }

        if (footerText != null) {
            footerText.setAlpha(0f);
            footerText.setTranslationY(20f);
            footerText.animate()
                    .alpha(1f)
                    .translationY(0f)
                    .setStartDelay(720)
                    .setDuration(420)
                    .setInterpolator(smooth)
                    .start();
        }
    }

    @Override
    protected void onDestroy() {
        splashHandler.removeCallbacks(navigationRunnable);
        if (glowPulseAnimator != null) {
            glowPulseAnimator.cancel();
            glowPulseAnimator = null;
        }
        super.onDestroy();
    }
}
