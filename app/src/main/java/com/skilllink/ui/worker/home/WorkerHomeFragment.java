package com.skilllink.ui.worker.home;

import android.animation.ValueAnimator;
import android.content.Intent;
import android.content.res.ColorStateList;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.AccelerateDecelerateInterpolator;
import android.widget.TextView;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.TransitionDrawable;

import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;
import androidx.core.widget.NestedScrollView;
import androidx.fragment.app.Fragment;

import com.google.android.material.chip.Chip;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.android.material.snackbar.Snackbar;
import com.github.mikephil.charting.charts.LineChart;
import com.github.mikephil.charting.components.XAxis;
import com.github.mikephil.charting.components.YAxis;
import com.github.mikephil.charting.data.Entry;
import com.github.mikephil.charting.data.LineData;
import com.github.mikephil.charting.data.LineDataSet;
import com.github.mikephil.charting.formatter.IndexAxisValueFormatter;
import com.github.mikephil.charting.formatter.ValueFormatter;
import com.skilllink.R;
import com.skilllink.databinding.FragmentWorkerHomeBinding;
import com.skilllink.databinding.DialogWorkerJobRequestDetailsBinding;
import com.skilllink.databinding.ItemWorkerJobRequestBinding;
import com.skilllink.ui.common.ModernNotificationsActivity;
import com.skilllink.ui.worker.account.ManageServicesActivity;
import com.skilllink.model.WorkerJobRequest;
import com.skilllink.util.ImageLoader;
import com.skilllink.util.NameFormatter;
import com.skilllink.util.SessionManager;

import java.text.NumberFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Locale;

public class WorkerHomeFragment extends Fragment {

    private static final int TARGET_EARNINGS_TODAY = 0;
    private static final int TARGET_JOBS_COMPLETED = 0;
    private static final float TARGET_RATING = 0f;
    private static final int PERFORMANCE_SCORE = 0;
    private static final int[] WEEKLY_REVENUE_SERIES = {0, 0, 0, 0, 0, 0, 0};
    private static final int PREVIOUS_WEEK_TOTAL = 0;
    private static final float ANALYTICS_ACCEPTANCE_RATE = 0f;
    private static final String[] WEEKLY_REVENUE_LABELS = {"Mon", "Tue", "Wed", "Thu", "Fri", "Sat", "Sun"};

    private static final String STATUS_PENDING = "Pending";
    private static final String STATUS_ACCEPTED = "Accepted";

    private FragmentWorkerHomeBinding binding;
    private SessionManager sessionManager;
    private final List<ValueAnimator> runningAnimators = new ArrayList<>();

    public View onCreateView(@NonNull LayoutInflater inflater,
                             ViewGroup container, Bundle savedInstanceState) {
        binding = FragmentWorkerHomeBinding.inflate(inflater, container, false);
        View root = binding.getRoot();

        sessionManager = new SessionManager(requireContext());
        initViews();

        return root;
    }

    private void initViews() {
        setupGreeting();
        setupStatusSwitch();
        setupPrimaryActions();
        setupAnalyticsCard();
        populateMetrics();
        populateJobRequests();
        animateHeroState(false, false);
        runEntryAnimations();
    }

    private void setupGreeting() {
        String greetingName = getString(R.string.worker_home_greeting_default);
        NameFormatter.Parts parts = NameFormatter.resolve(sessionManager.getUserName(), sessionManager.getUserEmail());
        if (parts != null && !TextUtils.isEmpty(parts.getFirstName())) {
            greetingName = parts.getFirstName();
        }
        binding.textGreeting.setText(getString(R.string.worker_home_greeting_format, greetingName));
    }

    private void setupStatusSwitch() {
        binding.switchOnlineStatus.setChecked(false);
        binding.switchOnlineStatus.setOnCheckedChangeListener((buttonView, isChecked) -> animateHeroState(isChecked, true));
        binding.chipStatus.setOnClickListener(v -> binding.switchOnlineStatus.toggle());
    }

    private void setupPrimaryActions() {
        binding.buttonManageServices.setOnClickListener(v ->
                startActivity(new Intent(requireContext(), ManageServicesActivity.class)));

        binding.buttonNotifications.setOnClickListener(v ->
                startActivity(new Intent(requireContext(), ModernNotificationsActivity.class)));

        NestedScrollView scrollView = (NestedScrollView) binding.getRoot();
        binding.buttonViewRequests.setOnClickListener(v ->
                scrollView.post(() -> scrollView.smoothScrollTo(0, binding.cardJobRequests.getTop())));

        binding.buttonViewAllRequests.setOnClickListener(v -> showComingSoon());
    }

    private void setupAnalyticsCard() {
        configureAnalyticsChart();
        populateAnalyticsSummary();
    }

    private void configureAnalyticsChart() {
        LineChart chart = binding.chartPerformanceTrend;
        chart.setNoDataText(getString(R.string.worker_home_analytics_no_data));
        chart.setNoDataTextColor(ContextCompat.getColor(requireContext(), R.color.worker_home_text_secondary));
        chart.setTouchEnabled(false);
        chart.setDragEnabled(false);
        chart.setScaleEnabled(false);
        chart.setPinchZoom(false);
        chart.setDoubleTapToZoomEnabled(false);
        chart.setDrawGridBackground(false);
        chart.getDescription().setEnabled(false);
        chart.getLegend().setEnabled(false);
        chart.setViewPortOffsets(56f, 36f, 24f, 60f);

        XAxis xAxis = chart.getXAxis();
        xAxis.setPosition(XAxis.XAxisPosition.BOTTOM);
        xAxis.setDrawGridLines(false);
        xAxis.setDrawAxisLine(false);
        xAxis.setTextColor(ContextCompat.getColor(requireContext(), R.color.worker_home_analytics_axis));
        xAxis.setTextSize(11f);
        xAxis.setGranularity(1f);
        xAxis.setAxisMinimum(-0.2f);
        xAxis.setAxisMaximum(WEEKLY_REVENUE_SERIES.length - 1 + 0.2f);
        xAxis.setValueFormatter(new IndexAxisValueFormatter(WEEKLY_REVENUE_LABELS));

        YAxis leftAxis = chart.getAxisLeft();
        leftAxis.setTextColor(ContextCompat.getColor(requireContext(), R.color.worker_home_analytics_axis));
        leftAxis.setTextSize(11f);
        leftAxis.setAxisMinimum(0f);
        leftAxis.setDrawAxisLine(false);
        leftAxis.setGridColor(ContextCompat.getColor(requireContext(), R.color.worker_home_metric_stroke));
        leftAxis.setLabelCount(4, true);
        leftAxis.setSpaceTop(12f);
        leftAxis.setGranularity(5000f);
        final NumberFormat thousandsFormat = NumberFormat.getIntegerInstance(Locale.getDefault());
        leftAxis.setValueFormatter(new ValueFormatter() {
            @Override
            public String getFormattedValue(float value) {
                int thousands = (int) (value / 1000f);
                return thousandsFormat.format(thousands) + "k";
            }
        });

        chart.getAxisRight().setEnabled(false);

        List<Entry> entries = new ArrayList<>();
        for (int i = 0; i < WEEKLY_REVENUE_SERIES.length; i++) {
            entries.add(new Entry(i, WEEKLY_REVENUE_SERIES[i]));
        }

        LineDataSet dataSet = new LineDataSet(entries, getString(R.string.worker_home_analytics_title));
        dataSet.setMode(LineDataSet.Mode.CUBIC_BEZIER);
        dataSet.setCubicIntensity(0.25f);
        dataSet.setColor(ContextCompat.getColor(requireContext(), R.color.worker_home_analytics_line));
        dataSet.setLineWidth(2.8f);
        dataSet.setDrawCircles(true);
        dataSet.setCircleRadius(4.2f);
        dataSet.setCircleColor(ContextCompat.getColor(requireContext(), R.color.worker_home_analytics_line));
        dataSet.setCircleHoleColor(ContextCompat.getColor(requireContext(), R.color.white));
        dataSet.setCircleHoleRadius(2f);
        dataSet.setDrawValues(false);
        dataSet.setDrawFilled(true);
        Drawable fillDrawable = ContextCompat.getDrawable(requireContext(), R.drawable.bg_worker_analytics_fill);
        if (fillDrawable != null) {
            dataSet.setFillDrawable(fillDrawable);
        } else {
            dataSet.setFillColor(ContextCompat.getColor(requireContext(), R.color.worker_home_analytics_line));
            dataSet.setFillAlpha(60);
        }
        dataSet.setDrawHorizontalHighlightIndicator(false);
        dataSet.setDrawVerticalHighlightIndicator(false);

        LineData lineData = new LineData(dataSet);
        chart.setData(lineData);
        chart.animateY(900);
        chart.invalidate();
    }

    private void populateAnalyticsSummary() {
        NumberFormat numberFormat = NumberFormat.getNumberInstance(Locale.getDefault());
        int total = 0;
        for (int revenue : WEEKLY_REVENUE_SERIES) {
            total += revenue;
        }

        binding.textAnalyticsRevenueValue.setText(
                getString(R.string.worker_home_analytics_revenue_value_format, numberFormat.format(total)));

        float growthValue = PREVIOUS_WEEK_TOTAL == 0
                ? 0f
                : (total - PREVIOUS_WEEK_TOTAL) * 100f / PREVIOUS_WEEK_TOTAL;
        String growthMagnitude = getString(R.string.worker_home_analytics_growth_value_format, Math.abs(growthValue));
        String growthText = (growthValue >= 0f ? "+" : "-") + growthMagnitude;
        binding.textAnalyticsGrowthValue.setText(growthText);
        binding.textAnalyticsGrowthValue.setTextColor(ContextCompat.getColor(requireContext(),
                growthValue >= 0f ? R.color.worker_home_cta : R.color.error_color));

        binding.textAnalyticsTrendLabel.setText(growthValue >= 0f
                ? R.string.worker_home_analytics_trend_positive
                : R.string.worker_home_analytics_trend_negative);
        binding.textAnalyticsTrendLabel.setTextColor(ContextCompat.getColor(requireContext(),
                growthValue >= 0f ? R.color.worker_home_cta : R.color.error_color));
        Drawable trendBackground = binding.textAnalyticsTrendLabel.getBackground();
        if (trendBackground != null) {
            Drawable mutated = trendBackground.mutate();
            mutated.setTint(ContextCompat.getColor(requireContext(),
                    growthValue >= 0f ? R.color.worker_home_status_chip_upcoming_bg
                            : R.color.worker_home_status_chip_offline_bg));
            binding.textAnalyticsTrendLabel.setBackground(mutated);
        }

        binding.textAnalyticsConversionValue.setText(
                getString(R.string.worker_home_analytics_conversion_value_format, ANALYTICS_ACCEPTANCE_RATE));
    }

    private void populateMetrics() {
        NumberFormat numberFormat = NumberFormat.getNumberInstance(Locale.getDefault());

        binding.textMetricEarningsValue.setText(
                getString(R.string.worker_home_metric_earnings_value_format, numberFormat.format(0)));
        binding.textMetricJobsValue.setText(numberFormat.format(0));
        binding.textMetricRatingValue.setText(getString(R.string.worker_home_metric_rating_value_format, 0f));
        binding.textPerformanceValue.setText(getString(R.string.worker_home_performance_value_format, 0));

        ValueAnimator earningsAnimator = ValueAnimator.ofInt(0, TARGET_EARNINGS_TODAY);
        earningsAnimator.setDuration(1200);
        earningsAnimator.addUpdateListener(animator -> {
            int value = (int) animator.getAnimatedValue();
            binding.textMetricEarningsValue.setText(
                    getString(R.string.worker_home_metric_earnings_value_format, numberFormat.format(value)));
        });
        startAnimator(earningsAnimator);

        ValueAnimator jobsAnimator = ValueAnimator.ofInt(0, TARGET_JOBS_COMPLETED);
        jobsAnimator.setDuration(900);
        jobsAnimator.addUpdateListener(animator ->
                binding.textMetricJobsValue.setText(numberFormat.format((int) animator.getAnimatedValue())));
        startAnimator(jobsAnimator);

        ValueAnimator ratingAnimator = ValueAnimator.ofFloat(0f, TARGET_RATING);
        ratingAnimator.setDuration(900);
        ratingAnimator.addUpdateListener(animator -> {
            float value = (float) animator.getAnimatedValue();
            binding.textMetricRatingValue.setText(
                    getString(R.string.worker_home_metric_rating_value_format, value));
        });
        startAnimator(ratingAnimator);

        binding.progressPerformance.setProgressCompat(0, false);
        ValueAnimator performanceAnimator = ValueAnimator.ofInt(0, PERFORMANCE_SCORE);
        performanceAnimator.setDuration(1100);
        performanceAnimator.addUpdateListener(animator -> {
            int value = (int) animator.getAnimatedValue();
            binding.progressPerformance.setProgressCompat(value, true);
            binding.textPerformanceValue.setText(getString(R.string.worker_home_performance_value_format, value));
        });
        startAnimator(performanceAnimator);
    }

    private void populateJobRequests() {
        LayoutInflater inflater = LayoutInflater.from(requireContext());
        binding.containerJobRequests.removeAllViews();

        List<WorkerJobRequest> jobRequests = sessionManager.getWorkerJobRequests();
        List<WorkerJobRequest> pendingRequests = new ArrayList<>();
        for (WorkerJobRequest request : jobRequests) {
            if (isPendingRequest(request)) {
                pendingRequests.add(request);
            }
        }

        if (pendingRequests.isEmpty()) {
            binding.textJobRequestsEmpty.setVisibility(View.VISIBLE);
            return;
        }

        binding.textJobRequestsEmpty.setVisibility(View.GONE);
        Collections.sort(pendingRequests, (first, second) -> Long.compare(second.getCreatedAt(), first.getCreatedAt()));

        long delay = 0L;
        for (WorkerJobRequest request : pendingRequests) {
            ItemWorkerJobRequestBinding itemBinding = ItemWorkerJobRequestBinding.inflate(inflater, binding.containerJobRequests, false);
            ImageLoader.loadUriInto(
                    requireContext(),
                    itemBinding.imageIcon,
                    request.getImageUri(),
                    R.drawable.ic_category,
                    R.color.worker_home_cta
            );

            itemBinding.textTitle.setText(request.getServiceName());

            String metaText = joinNonEmpty(" • ",
                    request.getCustomerName(),
                    request.getScheduleDisplay(),
                    request.getLocation());
            if (TextUtils.isEmpty(metaText)) {
                metaText = getString(R.string.worker_home_job_request_placeholder_meta);
            }
            itemBinding.textMeta.setText(metaText);

            String priceDisplay = request.getPriceDisplay();
            if (TextUtils.isEmpty(priceDisplay)) {
                priceDisplay = request.getPaymentMethod();
            }
            if (TextUtils.isEmpty(priceDisplay)) {
                itemBinding.textBudget.setVisibility(View.GONE);
            } else {
                itemBinding.textBudget.setVisibility(View.VISIBLE);
                itemBinding.textBudget.setText(getString(R.string.worker_home_job_request_budget_format, priceDisplay));
            }

            itemBinding.buttonDetails.setOnClickListener(v -> showRequestDetails(request));

            binding.containerJobRequests.addView(itemBinding.getRoot());
            animateListItem(itemBinding.getRoot(), delay);
            delay += 80L;
        }
    }

    private void animateHeroState(boolean online, boolean animateBackground) {
        Drawable targetBackground = ContextCompat.getDrawable(requireContext(),
                online ? R.drawable.bg_worker_hero_online : R.drawable.bg_worker_hero_offline);
        if (targetBackground != null) {
            if (animateBackground) {
                Drawable current = binding.heroContainer.getBackground();
                if (current != null) {
                    Drawable start = current.getConstantState() != null
                            ? current.getConstantState().newDrawable().mutate()
                            : current.mutate();
                    Drawable end = targetBackground.getConstantState() != null
                            ? targetBackground.getConstantState().newDrawable().mutate()
                            : targetBackground.mutate();
                    TransitionDrawable transition = new TransitionDrawable(new Drawable[]{start, end});
                    binding.heroContainer.setBackground(transition);
                    transition.startTransition(350);
                } else {
                    binding.heroContainer.setBackground(targetBackground);
                }
            } else {
                binding.heroContainer.setBackground(targetBackground);
            }
        }

        binding.textAvailability.setText(
                online ? R.string.worker_home_status_online : R.string.worker_home_status_offline);
        binding.textAvailability.setTextColor(ContextCompat.getColor(requireContext(),
                online ? R.color.worker_home_status_text_online : R.color.worker_home_status_text_offline));

        binding.textSubtitle.setText(online
                ? R.string.worker_home_hero_secondary_online
                : R.string.worker_home_hero_secondary_offline);

        Chip statusChip = binding.chipStatus;
        statusChip.setText(online
                ? R.string.worker_home_status_chip_online
                : R.string.worker_home_status_chip_offline);
        int chipBackground = ContextCompat.getColor(requireContext(),
                online ? R.color.worker_home_status_chip_online_bg : R.color.worker_home_status_chip_offline_bg);
        int chipTextColor = ContextCompat.getColor(requireContext(),
                online ? android.R.color.white : R.color.worker_home_accent_warm);
        statusChip.setChipBackgroundColor(ColorStateList.valueOf(chipBackground));
        statusChip.setTextColor(chipTextColor);
        statusChip.setChipIconTint(ColorStateList.valueOf(chipTextColor));
        statusChip.setChipIconResource(online ? R.drawable.ic_check_circle : R.drawable.ic_availability);

        int trackColor = ContextCompat.getColor(requireContext(),
                online ? R.color.worker_home_cta : R.color.worker_home_button_outline);
        binding.switchOnlineStatus.setTrackTintList(ColorStateList.valueOf(trackColor));
        binding.switchOnlineStatus.setThumbTintList(ColorStateList.valueOf(
                ContextCompat.getColor(requireContext(), android.R.color.white)));

        binding.imageHero.setImageResource(R.drawable.worker);
    }

    private void animateListItem(View view, long delay) {
        view.setAlpha(0f);
        view.setTranslationY(24f);
        view.animate()
                .alpha(1f)
                .translationY(0f)
                .setStartDelay(delay)
                .setDuration(350L)
                .setInterpolator(new AccelerateDecelerateInterpolator())
                .start();
    }

    private void runEntryAnimations() {
        animateCard(binding.cardHero, 0L);
        animateCard(binding.cardMetrics, 70L);
        animateCard(binding.cardAnalytics, 120L);
        animateCard(binding.cardJobRequests, 170L);
        animateCard(binding.cardPerformance, 210L);
    }

    private void animateCard(View view, long startDelay) {
        view.setAlpha(0f);
        view.setTranslationY(32f);
        view.animate()
                .alpha(1f)
                .translationY(0f)
                .setStartDelay(startDelay)
                .setDuration(420L)
                .setInterpolator(new AccelerateDecelerateInterpolator())
                .start();
    }

    private static String joinNonEmpty(String separator, String... parts) {
        if (parts == null || parts.length == 0) {
            return "";
        }

        StringBuilder builder = new StringBuilder();
        for (String part : parts) {
            if (TextUtils.isEmpty(part)) {
                continue;
            }
            if (builder.length() > 0) {
                builder.append(separator);
            }
            builder.append(part);
        }
        return builder.toString();
    }

    private void showRequestDetails(WorkerJobRequest request) {
        if (request == null) {
            return;
        }

        LayoutInflater inflater = LayoutInflater.from(requireContext());
        DialogWorkerJobRequestDetailsBinding dialogBinding = DialogWorkerJobRequestDetailsBinding.inflate(inflater);

        dialogBinding.textServiceValue.setText(TextUtils.isEmpty(request.getServiceName())
                ? getString(R.string.worker_home_job_details_placeholder)
                : request.getServiceName());
        bindDetailValue(dialogBinding.textCustomerValue, request.getCustomerName());
        bindDetailValue(dialogBinding.textContactValue, request.getCustomerPhone());
        bindDetailValue(dialogBinding.textScheduleValue, request.getScheduleDisplay());
        bindDetailValue(dialogBinding.textLocationValue, request.getLocation());
        bindDetailValue(dialogBinding.textNotesValue, request.getNotes());

        String paymentSummary = TextUtils.isEmpty(request.getPriceDisplay())
                ? request.getPaymentMethod()
                : request.getPriceDisplay();
        bindDetailValue(dialogBinding.textPaymentValue, paymentSummary);

        new MaterialAlertDialogBuilder(requireContext())
                .setTitle(R.string.worker_home_job_details_title)
                .setView(dialogBinding.getRoot())
                .setPositiveButton(R.string.worker_home_job_details_accept_button,
                        (dialog, which) -> acceptJobRequest(request))
                .setNegativeButton(R.string.worker_home_job_details_close_button, null)
                .show();
    }

    private void acceptJobRequest(WorkerJobRequest request) {
        if (request == null || binding == null) {
            return;
        }
        if (!isPendingRequest(request)) {
            Snackbar.make(binding.getRoot(), R.string.worker_home_job_details_accept_already, Snackbar.LENGTH_SHORT).show();
            return;
        }

        List<WorkerJobRequest> requests = sessionManager.getWorkerJobRequests();
        boolean updated = false;
        for (int i = 0; i < requests.size(); i++) {
            WorkerJobRequest existing = requests.get(i);
            if (existing != null && request.getId().equals(existing.getId())) {
                requests.set(i, existing.withStatus(STATUS_ACCEPTED));
                updated = true;
                break;
            }
        }

        if (!updated) {
            requests.add(request.withStatus(STATUS_ACCEPTED));
        }

        sessionManager.saveWorkerJobRequests(requests);
        Snackbar.make(binding.getRoot(), R.string.worker_home_job_details_accept_success, Snackbar.LENGTH_SHORT).show();
        populateJobRequests();
    }

    private void bindDetailValue(TextView view, String value) {
        if (view == null) {
            return;
        }
        view.setText(TextUtils.isEmpty(value)
                ? getString(R.string.worker_home_job_details_placeholder)
                : value);
    }

    private boolean isPendingRequest(WorkerJobRequest request) {
        if (request == null) {
            return false;
        }
        String status = request.getStatus();
        return TextUtils.isEmpty(status) || STATUS_PENDING.equalsIgnoreCase(status.trim());
    }

    private void showComingSoon() {
        Snackbar.make(binding.getRoot(), R.string.feature_coming_soon, Snackbar.LENGTH_SHORT).show();
    }

    private void startAnimator(ValueAnimator animator) {
        animator.setInterpolator(new AccelerateDecelerateInterpolator());
        runningAnimators.add(animator);
        animator.start();
    }

    @Override
    public void onResume() {
        super.onResume();
        if (binding != null) {
            populateJobRequests();
        }
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        for (ValueAnimator animator : runningAnimators) {
            animator.cancel();
        }
        runningAnimators.clear();
        binding = null;
    }

}