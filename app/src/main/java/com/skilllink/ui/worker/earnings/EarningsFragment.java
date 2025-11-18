package com.skilllink.ui.worker.earnings;

import android.animation.ValueAnimator;
import android.content.Context;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.DecelerateInterpolator;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;

import com.github.mikephil.charting.animation.Easing;
import com.github.mikephil.charting.charts.LineChart;
import com.github.mikephil.charting.components.Legend;
import com.github.mikephil.charting.components.XAxis;
import com.github.mikephil.charting.components.YAxis;
import com.github.mikephil.charting.data.Entry;
import com.github.mikephil.charting.data.LineData;
import com.github.mikephil.charting.data.LineDataSet;
import com.github.mikephil.charting.formatter.IndexAxisValueFormatter;
import com.skilllink.R;
import com.skilllink.databinding.FragmentWorkerEarningsBinding;

import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.util.ArrayList;
import java.util.EnumMap;
import java.util.List;
import java.util.Locale;

public class EarningsFragment extends Fragment {

    private enum Range {
        WEEK,
        MONTH,
        QUARTER
    }

    private static final int ANIMATION_DURATION = 900;
    private static final Locale LOCALE_LK = new Locale("en", "LK");

    private FragmentWorkerEarningsBinding binding;
    private final DecimalFormat currencyFormat = new DecimalFormat("#,##0",
            DecimalFormatSymbols.getInstance(LOCALE_LK));
    private final EnumMap<Range, ChartPayload> chartPayloads = new EnumMap<>(Range.class);
    private float rangeTotal = 0f;
    private float rangePeakValue = 0f;
    private String rangePeakLabel = "";

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
                             @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        binding = FragmentWorkerEarningsBinding.inflate(inflater, container, false);
        setupDataModels();
        initViews();
        return binding.getRoot();
    }

    private void setupDataModels() {
        chartPayloads.put(Range.WEEK, new ChartPayload(
                new float[0],
                new String[]{"Mon", "Tue", "Wed", "Thu", "Fri", "Sat", "Sun"}
        ));
        chartPayloads.put(Range.MONTH, new ChartPayload(
                new float[0],
                new String[]{"Week 1", "Week 2", "Week 3", "Week 4"}
        ));
        chartPayloads.put(Range.QUARTER, new ChartPayload(
                new float[0],
                new String[]{"Jul", "Aug", "Sep"}
        ));
    }

    private void initViews() {
        if (binding == null) {
            return;
        }

        final float todayAmount = 0f;
        final int todayJobs = 0;
        final float weekAmount = 0f;
        final float weekChangePercent = 0f;
        final float monthAmount = 0f;
        final float monthTarget = sanitizeNonNegative(65000f);
        final float totalBalance = 0f;

        currencyFormat.setMaximumFractionDigits(0);

        binding.textHeroLabel.setText(R.string.worker_earnings_today_badge);
        animateCurrency(binding.textTotalEarnings, totalBalance, ANIMATION_DURATION);
        binding.textTotalChange.setText(getString(
                weekChangePercent >= 0
                        ? R.string.worker_earnings_total_change_format
                        : R.string.worker_earnings_total_change_negative_format,
                Math.abs(weekChangePercent)));

        animateCurrency(binding.textSummaryToday, todayAmount, ANIMATION_DURATION);
        binding.textSummaryTodayJobs.setText(
                getString(R.string.worker_earnings_jobs_format, todayJobs));

        animateCurrency(binding.textSummaryWeek, weekAmount, ANIMATION_DURATION + 150);
        float safeWeekChangePercent = sanitizeFinite(weekChangePercent);
        int weekChangeStringRes = safeWeekChangePercent >= 0
                ? R.string.worker_earnings_week_change_format
                : R.string.worker_earnings_week_change_negative_format;
        binding.textSummaryWeekChange.setText(
                getString(weekChangeStringRes, Math.abs(safeWeekChangePercent)));

        animateCurrency(binding.textSummaryMonth, monthAmount, ANIMATION_DURATION + 300);

        float monthProgressPercent = 0f;
        if (monthTarget > 0f) {
            monthProgressPercent = (monthAmount / monthTarget) * 100f;
        }
        monthProgressPercent = Math.max(0f, Math.min(100f, sanitizeFinite(monthProgressPercent)));
        animateProgress(binding.progressMonthTarget, Math.round(monthProgressPercent));
        binding.textMonthTarget.setText(
                getString(R.string.worker_earnings_month_target_format,
                        monthProgressPercent,
                        currencyFormat.format(monthTarget)));

        setupChart(binding.chartWeekly);
        binding.chipRange.setOnCheckedChangeListener((group, checkedId) -> {
            Range range = mapRange(checkedId);
            if (range != null) {
                applyChartRange(range, true);
            }
        });
        applyChartRange(Range.WEEK, false);

        binding.textRecentPrimary.setText(R.string.worker_earnings_recent_empty_title);
        binding.textRecentSecondary.setText(R.string.worker_earnings_recent_empty_body);
        binding.textRecentAmount.setVisibility(View.GONE);

        binding.itemRecentTwo.setVisibility(View.GONE);
        binding.dividerRecent.setVisibility(View.GONE);
        binding.textRecentAmountTwo.setVisibility(View.GONE);

        binding.buttonQuickWithdraw.setOnClickListener(v -> handleWithdraw());
        binding.btnWithdraw.setOnClickListener(v -> handleWithdraw());
    }

    private void setupChart(@NonNull LineChart chart) {
        Context context = getContext();
        if (context == null) {
            return;
        }

        chart.setViewPortOffsets(60f, 40f, 30f, 60f);
        chart.setBackgroundColor(ContextCompat.getColor(context, android.R.color.transparent));
        chart.setGridBackgroundColor(ContextCompat.getColor(context, android.R.color.transparent));
        chart.setDrawGridBackground(false);
        chart.setTouchEnabled(true);
        chart.setPinchZoom(false);
        chart.setScaleEnabled(false);
        chart.setDoubleTapToZoomEnabled(false);
        chart.getDescription().setEnabled(false);

        Legend legend = chart.getLegend();
        legend.setEnabled(false);

        XAxis xAxis = chart.getXAxis();
        xAxis.setPosition(XAxis.XAxisPosition.BOTTOM);
        xAxis.setDrawAxisLine(false);
        xAxis.setTextColor(ContextCompat.getColor(context, R.color.worker_home_text_muted));
        xAxis.setTextSize(11f);
        xAxis.setGranularity(1f);
        xAxis.setYOffset(12f);
        xAxis.setGridColor(ContextCompat.getColor(context, R.color.worker_home_metric_stroke));
        xAxis.setGridLineWidth(0.6f);

        YAxis yLeft = chart.getAxisLeft();
        yLeft.setDrawAxisLine(false);
        yLeft.setDrawGridLines(true);
        yLeft.setGridColor(ContextCompat.getColor(context, R.color.worker_home_metric_stroke));
        yLeft.setGridLineWidth(0.6f);
        yLeft.setTextColor(ContextCompat.getColor(context, R.color.worker_home_text_muted));
        yLeft.setTextSize(11f);
        yLeft.setXOffset(16f);
        yLeft.setAxisMinimum(0f);

        YAxis yRight = chart.getAxisRight();
        yRight.setEnabled(false);

        chart.setExtraOffsets(0f, 10f, 0f, 10f);
        chart.animateY(900, Easing.EaseInOutQuad);
    }

    private void applyChartRange(@NonNull Range range, boolean animated) {
        ChartPayload payload = chartPayloads.get(range);
        if (payload == null || binding == null) {
            return;
        }

        Context context = getContext();
        if (context == null) {
            return;
        }

        float[] values = payload.values != null ? payload.values : new float[0];
        String[] labels = payload.labels != null ? payload.labels : new String[0];

        List<Entry> entries = new ArrayList<>(values.length);
        rangeTotal = 0f;
        rangePeakValue = 0f;
        rangePeakLabel = labels.length > 0 ? labels[0] : resolveRangeLabel(range);

        for (int i = 0; i < values.length; i++) {
            float sanitizedValue = sanitizeNonNegative(values[i]);
            entries.add(new Entry(i, sanitizedValue));
            rangeTotal += sanitizedValue;
            if (sanitizedValue > rangePeakValue) {
                rangePeakValue = sanitizedValue;
                if (i < labels.length) {
                    rangePeakLabel = labels[i];
                }
            }
        }

        if (entries.isEmpty()) {
            binding.chartWeekly.clear();
            binding.chartWeekly.invalidate();
            rangeTotal = sanitizeNonNegative(rangeTotal);
            rangePeakValue = sanitizeNonNegative(rangePeakValue);
            binding.textWeekTotal.setText(formatCurrency(rangeTotal));
            binding.textWeekInsight.setText(getString(R.string.worker_earnings_no_data));
            return;
        }

        LineDataSet dataSet = new LineDataSet(entries, null);
        int primaryColor = ContextCompat.getColor(context, R.color.worker_home_cta);
        dataSet.setColor(primaryColor);
        dataSet.setLineWidth(2.8f);
        dataSet.setMode(LineDataSet.Mode.CUBIC_BEZIER);
        dataSet.setCubicIntensity(0.18f);
        dataSet.setCircleRadius(5f);
        dataSet.setCircleColor(primaryColor);
        dataSet.setCircleHoleColor(ContextCompat.getColor(context, android.R.color.white));
        dataSet.setHighLightColor(ContextCompat.getColor(context, R.color.worker_home_accent_warm));
        dataSet.setDrawValues(false);
        dataSet.setDrawCircles(true);
        dataSet.setDrawFilled(true);

        Drawable fillDrawable = ContextCompat.getDrawable(context, R.drawable.bg_worker_analytics_fill);
        if (fillDrawable != null) {
            dataSet.setFillDrawable(fillDrawable);
        } else {
            dataSet.setFillColor(primaryColor);
        }

        LineData lineData = new LineData(dataSet);
        binding.chartWeekly.setData(lineData);
        binding.chartWeekly.getXAxis().setValueFormatter(new IndexAxisValueFormatter(labels));
        binding.chartWeekly.invalidate();

        if (animated) {
            binding.chartWeekly.animateY(800, Easing.EaseInOutQuad);
        }

        float average = entries.isEmpty() ? 0f : rangeTotal / entries.size();
        average = sanitizeFinite(average);
        binding.textWeekTotal.setText(formatCurrency(rangeTotal));
        binding.textWeekInsight.setText(
                getString(R.string.worker_earnings_peak_day_with_avg_format,
                        rangePeakLabel,
                        formatCurrency(rangePeakValue),
                        formatCurrency(average)));
    }

    private void animateCurrency(@NonNull TextView view, float target, int duration) {
        float safeTarget = sanitizeFinite(target);
        ValueAnimator animator = ValueAnimator.ofFloat(0f, safeTarget);
        animator.setInterpolator(new DecelerateInterpolator());
        animator.setDuration(duration);
        animator.addUpdateListener(animation -> view.setText(formatCurrency((Float) animation.getAnimatedValue())));
        animator.start();
    }

    private void animateProgress(@NonNull com.google.android.material.progressindicator.LinearProgressIndicator indicator,
                                 int target) {
        int max = indicator.getMax();
        int clampedTarget = Math.max(0, Math.min(target, max));
        indicator.setProgressCompat(0, false);
        if (clampedTarget == 0) {
            indicator.setProgressCompat(0, false);
            return;
        }
        ValueAnimator animator = ValueAnimator.ofInt(0, clampedTarget);
        animator.setDuration(ANIMATION_DURATION);
        animator.setInterpolator(new DecelerateInterpolator());
        animator.addUpdateListener(animation ->
                indicator.setProgressCompat((Integer) animation.getAnimatedValue(), true));
        animator.start();
    }

    private void handleWithdraw() {
        // TODO Integrate with withdrawal flow.
    }

    @NonNull
    private String resolveRangeLabel(@NonNull Range range) {
        int labelRes;
        if (range == Range.MONTH) {
            labelRes = R.string.worker_earnings_range_month;
        } else if (range == Range.QUARTER) {
            labelRes = R.string.worker_earnings_range_quarter;
        } else {
            labelRes = R.string.worker_earnings_range_week;
        }
        return getString(labelRes);
    }

    private float sanitizeFinite(float value) {
        if (Float.isNaN(value) || Float.isInfinite(value)) {
            return 0f;
        }
        return value;
    }

    private float sanitizeNonNegative(float value) {
        return Math.max(0f, sanitizeFinite(value));
    }

    @NonNull
    private String formatCurrency(float value) {
        float safeValue = sanitizeFinite(value);
        return "LKR " + currencyFormat.format(safeValue);
    }

    @Nullable
    private Range mapRange(int chipId) {
        if (chipId == R.id.chipRangeWeek) {
            return Range.WEEK;
        } else if (chipId == R.id.chipRangeMonth) {
            return Range.MONTH;
        } else if (chipId == R.id.chipRangeQuarter) {
            return Range.QUARTER;
        }
        return null;
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }

    private static final class ChartPayload {
        final float[] values;
        final String[] labels;

        ChartPayload(float[] values, String[] labels) {
            this.values = values != null ? values.clone() : new float[0];
            this.labels = labels != null ? labels.clone() : new String[0];
        }
    }
}