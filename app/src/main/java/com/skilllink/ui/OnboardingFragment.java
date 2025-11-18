package com.skilllink.ui;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.DrawableRes;
import androidx.annotation.NonNull;
import androidx.annotation.RawRes;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;

import com.skilllink.R;
import com.airbnb.lottie.LottieComposition;
import com.airbnb.lottie.LottieAnimationView;
import com.airbnb.lottie.LottieCompositionFactory;
import com.airbnb.lottie.LottieDrawable;
import com.airbnb.lottie.LottieTask;
import com.airbnb.lottie.RenderMode;
import com.google.android.material.card.MaterialCardView;

import android.view.animation.AccelerateDecelerateInterpolator;

public class OnboardingFragment extends Fragment {

    private static final String ARG_POSITION = "position";
    private static final String HERO_ANIMATION_URL = "https://lottie.host/f25d5c9e-bf43-4588-a118-f3d179401007/4hFiIOGjcZ.lottie";
    private static final String TRACKING_ANIMATION_URL = "https://lottie.host/b4ea244a-ebbd-4c12-80e0-308a3bf03b01/VBZE9vhGcL.lottie";
    private static final String SECURE_ANIMATION_URL = "https://lottie.host/9efb1e19-4b37-4c39-a85b-b1e23c6fc314/cjGd5SkpbU.lottie";
    private static final int TOTAL_STEPS = 3;

    private int position;
    private LottieTask<LottieComposition> heroAnimationTask;

    public static OnboardingFragment newInstance(int position) {
        OnboardingFragment fragment = new OnboardingFragment();
        Bundle args = new Bundle();
        args.putInt(ARG_POSITION, position);
        fragment.setArguments(args);
        return fragment;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (getArguments() != null) {
            position = getArguments().getInt(ARG_POSITION);
        }
    }

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_onboarding, container, false);

        LottieAnimationView animationView = view.findViewById(R.id.onboarding_animation);
        TextView badgeText = view.findViewById(R.id.badge_text);
        TextView stepText = view.findViewById(R.id.step_text);
        TextView titleText = view.findViewById(R.id.title_text);
        TextView descriptionText = view.findViewById(R.id.description_text);
        TextView highlightPrimary = view.findViewById(R.id.highlight_primary);
        TextView highlightSecondary = view.findViewById(R.id.highlight_secondary);
        View highlightsContainer = view.findViewById(R.id.highlights_container);
        MaterialCardView highlightPrimaryCard = view.findViewById(R.id.highlight_primary_card);
        MaterialCardView highlightSecondaryCard = view.findViewById(R.id.highlight_secondary_card);

        animationView.setRenderMode(RenderMode.AUTOMATIC);
        applyContentForPosition(animationView, badgeText, titleText, descriptionText, highlightPrimary, highlightSecondary, stepText, highlightPrimaryCard, highlightSecondaryCard);
        playIntroAnimation(animationView, badgeText, titleText, descriptionText, highlightsContainer);

        return view;
    }

    private void applyContentForPosition(LottieAnimationView animationView,
                                         TextView badgeText,
                                         TextView titleText,
                                         TextView descriptionText,
                                         TextView highlightPrimary,
                                         TextView highlightSecondary,
                                         TextView stepText,
                                         MaterialCardView highlightPrimaryCard,
                                         MaterialCardView highlightSecondaryCard) {
        animationView.cancelAnimation();
        animationView.setImageDrawable(null);
        animationView.setRepeatCount(LottieDrawable.INFINITE);
        animationView.setRepeatMode(LottieDrawable.RESTART);
        stepText.setText(getString(R.string.onboarding_step_format, position + 1, TOTAL_STEPS));

        int primarySurface = ContextCompat.getColor(requireContext(), R.color.onboarding_highlight_primary);
        int accentBlue = ContextCompat.getColor(requireContext(), R.color.onboarding_highlight_blue);
        int accentGreen = ContextCompat.getColor(requireContext(), R.color.onboarding_highlight_green);
        int strokeColor = ContextCompat.getColor(requireContext(), R.color.onboarding_highlight_stroke);
        highlightPrimaryCard.setStrokeColor(strokeColor);
        highlightSecondaryCard.setStrokeColor(strokeColor);

        switch (position) {
            case 0:
                badgeText.setText(R.string.onboarding_badge_discover);
                titleText.setText(R.string.onboarding_title_1);
                descriptionText.setText(R.string.onboarding_description_1);
                highlightPrimary.setText(R.string.onboarding_highlight_1_primary);
                highlightSecondary.setText(R.string.onboarding_highlight_1_secondary);
                highlightPrimaryCard.setCardBackgroundColor(primarySurface);
                highlightSecondaryCard.setCardBackgroundColor(primarySurface);
                loadDotLottie(animationView, HERO_ANIMATION_URL, 1.4f, R.raw.user_lottie, 0.85f);
                break;
            case 1:
                badgeText.setText(R.string.onboarding_badge_track);
                titleText.setText(R.string.onboarding_title_2);
                descriptionText.setText(R.string.onboarding_description_2);
                highlightPrimary.setText(R.string.onboarding_highlight_2_primary);
                highlightSecondary.setText(R.string.onboarding_highlight_2_secondary);
                highlightPrimaryCard.setCardBackgroundColor(accentBlue);
                highlightSecondaryCard.setCardBackgroundColor(accentBlue);
                loadDotLottie(animationView, TRACKING_ANIMATION_URL, 1.4f, R.raw.worker_lottie, 0.85f);
                break;
            case 2:
            default:
                badgeText.setText(R.string.onboarding_badge_secure);
                titleText.setText(R.string.onboarding_title_3);
                descriptionText.setText(R.string.onboarding_description_3);
                highlightPrimary.setText(R.string.onboarding_highlight_3_primary);
                highlightSecondary.setText(R.string.onboarding_highlight_3_secondary);
                highlightPrimaryCard.setCardBackgroundColor(accentGreen);
                highlightSecondaryCard.setCardBackgroundColor(accentGreen);
                loadDotLottieWithImageFallback(animationView, SECURE_ANIMATION_URL, 1.4f, R.drawable.ic_payment);
                break;
        }
    }

    private void loadDotLottie(@NonNull LottieAnimationView animationView,
                               @NonNull String url,
                               float successSpeed,
                               @RawRes int fallbackRes,
                               float fallbackSpeed) {
        cancelHeroAnimationTask();
        heroAnimationTask = LottieCompositionFactory.fromUrl(requireContext(), url);
        heroAnimationTask.addListener(composition -> {
            animationView.setComposition(composition);
            animationView.setRepeatCount(LottieDrawable.INFINITE);
            animationView.setRepeatMode(LottieDrawable.RESTART);
            animationView.setSpeed(successSpeed);
            animationView.playAnimation();
        });
        heroAnimationTask.addFailureListener(throwable -> {
            animationView.setAnimation(fallbackRes);
            animationView.setRepeatCount(LottieDrawable.INFINITE);
            animationView.setRepeatMode(LottieDrawable.RESTART);
            animationView.setSpeed(fallbackSpeed);
            animationView.playAnimation();
        });
    }

    private void loadDotLottieWithImageFallback(@NonNull LottieAnimationView animationView,
                                                @NonNull String url,
                                                float successSpeed,
                                                @DrawableRes int fallbackDrawable) {
        cancelHeroAnimationTask();
        heroAnimationTask = LottieCompositionFactory.fromUrl(requireContext(), url);
        heroAnimationTask.addListener(composition -> {
            animationView.setComposition(composition);
            animationView.setRepeatCount(LottieDrawable.INFINITE);
            animationView.setRepeatMode(LottieDrawable.RESTART);
            animationView.setSpeed(successSpeed);
            animationView.playAnimation();
        });
        heroAnimationTask.addFailureListener(throwable -> {
            animationView.setRepeatCount(0);
            animationView.setImageResource(fallbackDrawable);
        });
    }

    private void cancelHeroAnimationTask() {
        heroAnimationTask = null;
    }

    private void playIntroAnimation(View... views) {
        long delay = 0L;
        AccelerateDecelerateInterpolator interpolator = new AccelerateDecelerateInterpolator();
        for (View target : views) {
            if (target == null) {
                continue;
            }
            target.setAlpha(0f);
            target.setTranslationY(32f);
            target.animate()
                    .alpha(1f)
                    .translationY(0f)
                    .setDuration(520L)
                    .setStartDelay(delay)
                    .setInterpolator(interpolator)
                    .start();
            delay += 90L;
        }
    }

    @Override
    public void onDestroyView() {
        cancelHeroAnimationTask();
        super.onDestroyView();
    }
}