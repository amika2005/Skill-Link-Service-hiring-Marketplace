package com.skilllink.ui.worker.jobs;

import android.content.Context;
import android.content.res.ColorStateList;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import androidx.core.content.ContextCompat;

import com.skilllink.R;
import com.skilllink.databinding.ItemWorkerJobRequestBinding;
import com.skilllink.model.WorkerJobRequest;
import com.skilllink.util.ImageLoader;

import java.util.List;

class WorkerJobRequestAdapter extends RecyclerView.Adapter<WorkerJobRequestAdapter.ViewHolder> {

    interface OnRequestActionListener {
        void onRequestDetails(@NonNull WorkerJobRequest request);
        void onRequestCancel(@NonNull WorkerJobRequest request);
    }

    private static final String STATUS_PENDING = "Pending";
    private static final String STATUS_ACCEPTED = "Accepted";
    private static final String STATUS_COMPLETED = "Completed";
    private static final String STATUS_CANCELLED = "Cancelled";

    private final Context context;
    private final List<WorkerJobRequest> requests;
    private final LayoutInflater inflater;
    private final OnRequestActionListener listener;

    WorkerJobRequestAdapter(@NonNull Context context,
                             @NonNull List<WorkerJobRequest> requests,
                             @NonNull OnRequestActionListener listener) {
        this.context = context;
        this.requests = requests;
        this.listener = listener;
        this.inflater = LayoutInflater.from(context);
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        ItemWorkerJobRequestBinding binding = ItemWorkerJobRequestBinding.inflate(inflater, parent, false);
        return new ViewHolder(binding);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        holder.bind(requests.get(position));
    }

    @Override
    public int getItemCount() {
        return requests.size();
    }

    class ViewHolder extends RecyclerView.ViewHolder {

        private final ItemWorkerJobRequestBinding binding;

        ViewHolder(ItemWorkerJobRequestBinding binding) {
            super(binding.getRoot());
            this.binding = binding;
        }

        void bind(WorkerJobRequest request) {
            ImageLoader.loadUriInto(
                    context,
                    binding.imageIcon,
                    request.getImageUri(),
                    R.drawable.ic_service_manage,
                    R.color.worker_home_cta
            );

            binding.textTitle.setText(request.getServiceName());

            String metaText = joinNonEmpty(" • ",
                    request.getCustomerName(),
                    request.getScheduleDisplay(),
                    request.getLocation());
            if (TextUtils.isEmpty(metaText)) {
                metaText = context.getString(R.string.worker_home_job_request_placeholder_meta);
            }
            binding.textMeta.setText(metaText);

            String priceDisplay = request.getPriceDisplay();
            if (TextUtils.isEmpty(priceDisplay)) {
                priceDisplay = request.getPaymentMethod();
            }
            if (TextUtils.isEmpty(priceDisplay)) {
                binding.textBudget.setVisibility(View.GONE);
            } else {
                binding.textBudget.setVisibility(View.VISIBLE);
                binding.textBudget.setText(context.getString(R.string.worker_home_job_request_budget_format, priceDisplay));
            }

            renderStatus(request);
            setupActions(request);
            animateCard(binding.getRoot());
        }

        private void setupActions(WorkerJobRequest request) {
            binding.buttonDetails.setOnClickListener(v -> listener.onRequestDetails(request));
            binding.buttonCancel.setOnClickListener(v -> listener.onRequestCancel(request));

            // Show cancel button only for pending jobs
            boolean isPending = STATUS_PENDING.equalsIgnoreCase(normalizeStatus(request.getStatus()));
            binding.buttonCancel.setVisibility(isPending ? View.VISIBLE : View.GONE);

            binding.getRoot().setOnClickListener(v -> listener.onRequestDetails(request));
        }

        private void renderStatus(WorkerJobRequest request) {
            String status = normalizeStatus(request.getStatus());
            binding.chipStatus.setText(resolveStatusLabel(status));

            int backgroundRes;
            int textRes;
            int iconRes;
            if (STATUS_COMPLETED.equalsIgnoreCase(status)) {
                backgroundRes = R.color.worker_home_status_chip_completed_bg;
                textRes = android.R.color.black;
                iconRes = R.drawable.ic_check_circle;
            } else if (STATUS_CANCELLED.equalsIgnoreCase(status)) {
                backgroundRes = R.color.worker_home_status_chip_offline_bg;
                textRes = R.color.worker_home_accent_warm;
                iconRes = R.drawable.ic_close;
            } else if (STATUS_ACCEPTED.equalsIgnoreCase(status)) {
                backgroundRes = R.color.worker_home_status_chip_online_bg;
                textRes = android.R.color.white;
                iconRes = R.drawable.ic_check_circle;
            } else {
                backgroundRes = R.color.worker_home_status_chip_upcoming_bg;
                textRes = R.color.worker_home_cta;
                iconRes = R.drawable.ic_service_manage;
            }

            int backgroundColor = ContextCompat.getColor(context, backgroundRes);
            int foregroundColor = ContextCompat.getColor(context, textRes);
            binding.chipStatus.setChipBackgroundColor(ColorStateList.valueOf(backgroundColor));
            binding.chipStatus.setTextColor(foregroundColor);
            binding.chipStatus.setChipIconResource(iconRes);
            binding.chipStatus.setChipIconTint(ColorStateList.valueOf(foregroundColor));
            binding.chipStatus.setVisibility(View.VISIBLE);
        }

        private void animateCard(View view) {
            view.setAlpha(0f);
            view.setTranslationY(24f);
            view.animate()
                    .alpha(1f)
                    .translationY(0f)
                    .setDuration(320L)
                    .start();
        }

        private String normalizeStatus(String status) {
            return TextUtils.isEmpty(status) ? STATUS_PENDING : status.trim();
        }

        private String resolveStatusLabel(String status) {
            if (STATUS_COMPLETED.equalsIgnoreCase(status)) {
                return context.getString(R.string.worker_jobs_status_completed);
            }
            if (STATUS_CANCELLED.equalsIgnoreCase(status)) {
                return context.getString(R.string.worker_jobs_status_cancelled);
            }
            if (STATUS_ACCEPTED.equalsIgnoreCase(status)) {
                return context.getString(R.string.worker_jobs_status_accepted);
            }
            return context.getString(R.string.worker_jobs_status_active);
        }
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
}
