package com.skilllink.ui.worker.jobs;

import android.os.Bundle;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.AccelerateDecelerateInterpolator;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;

import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.android.material.snackbar.Snackbar;
import com.google.android.material.tabs.TabLayout;
import com.skilllink.R;
import com.skilllink.databinding.DialogWorkerJobRequestDetailsBinding;
import com.skilllink.databinding.FragmentWorkerJobsBinding;
import com.skilllink.model.WorkerJobRequest;
import com.skilllink.util.SessionManager;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class JobsFragment extends Fragment {

    private static final String STATUS_PENDING = "Pending";
    private static final String STATUS_ACCEPTED = "Accepted";
    private static final String STATUS_COMPLETED = "Completed";
    private static final String STATUS_CANCELLED = "Cancelled";

    private FragmentWorkerJobsBinding binding;
    private SessionManager sessionManager;
    private WorkerJobRequestAdapter adapter;
    private final List<WorkerJobRequest> allRequests = new ArrayList<>();
    private final List<WorkerJobRequest> visibleRequests = new ArrayList<>();

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
                             ViewGroup container, Bundle savedInstanceState) {
        binding = FragmentWorkerJobsBinding.inflate(inflater, container, false);
        View root = binding.getRoot();

        sessionManager = new SessionManager(requireContext());

        setupRecyclerView();
        setupTabs();
        loadJobs();

        return root;
    }

    private void setupRecyclerView() {
        binding.recyclerViewJobs.setLayoutManager(new LinearLayoutManager(getContext()));
        adapter = new WorkerJobRequestAdapter(requireContext(), visibleRequests, new WorkerJobRequestAdapter.OnRequestActionListener() {
            @Override
            public void onRequestDetails(@NonNull WorkerJobRequest request) {
                JobsFragment.this.showRequestDetails(request);
            }

            @Override
            public void onRequestCancel(@NonNull WorkerJobRequest request) {
                JobsFragment.this.showCancellationDialog(request);
            }
        });
        binding.recyclerViewJobs.setAdapter(adapter);
        binding.recyclerViewJobs.setLayoutAnimation(AnimationUtils.loadLayoutAnimation(requireContext(), R.anim.layout_animation_fall_down));
        binding.buttonScrollToActive.setOnClickListener(v -> binding.recyclerViewJobs.post(() -> binding.recyclerViewJobs.smoothScrollToPosition(0)));
        binding.buttonEmptyAction.setOnClickListener(v -> binding.recyclerViewJobs.postDelayed(this::loadJobs, 150));
    }

    private void setupTabs() {
        TabLayout tabLayout = binding.tabs;
        tabLayout.addOnTabSelectedListener(new TabLayout.OnTabSelectedListener() {
            @Override
            public void onTabSelected(TabLayout.Tab tab) {
                applyFilter(tab.getPosition());
            }

            @Override
            public void onTabUnselected(TabLayout.Tab tab) {
                // no-op
            }

            @Override
            public void onTabReselected(TabLayout.Tab tab) {
                applyFilter(tab.getPosition());
            }
        });
    }

    private void loadJobs() {
        allRequests.clear();
        List<WorkerJobRequest> allWorkerRequests = sessionManager.getWorkerJobRequests();
        
        // Filter requests for current worker only
        String currentWorkerId = sessionManager.getUserId();
        if (!TextUtils.isEmpty(currentWorkerId)) {
            for (WorkerJobRequest request : allWorkerRequests) {
                // Check if this request is for the current worker
                if (isRequestForCurrentWorker(request, currentWorkerId)) {
                    allRequests.add(request);
                }
            }
        }
        
        Collections.sort(allRequests, (first, second) -> Long.compare(second.getCreatedAt(), first.getCreatedAt()));

        animateHeroCard();
        startHeroAnimations();

        int selectedPosition = binding.tabs.getSelectedTabPosition();
        if (selectedPosition == TabLayout.Tab.INVALID_POSITION) {
            selectedPosition = 0;
        }
        applyFilter(selectedPosition);
    }

    private void applyFilter(int tabPosition) {
        visibleRequests.clear();
        for (WorkerJobRequest request : allRequests) {
            if (shouldInclude(request, tabPosition)) {
                visibleRequests.add(request);
            }
        }
        adapter.notifyDataSetChanged();
        binding.recyclerViewJobs.scheduleLayoutAnimation();
        updateEmptyState();
    }

    private boolean shouldInclude(WorkerJobRequest request, int tabPosition) {
        String status = request.getStatus();
        if (tabPosition == 0) {
            return status == null || (!STATUS_COMPLETED.equalsIgnoreCase(status) && !STATUS_CANCELLED.equalsIgnoreCase(status));
        } else if (tabPosition == 1) {
            return STATUS_COMPLETED.equalsIgnoreCase(status);
        } else if (tabPosition == 2) {
            return STATUS_CANCELLED.equalsIgnoreCase(status);
        }
        return true;
    }

    private void updateEmptyState() {
        boolean showEmpty = visibleRequests.isEmpty();
        binding.emptyState.setVisibility(showEmpty ? View.VISIBLE : View.GONE);
        binding.recyclerViewJobs.setVisibility(showEmpty ? View.GONE : View.VISIBLE);
        if (showEmpty) {
            binding.emptyState.setAlpha(0f);
            binding.emptyState.setTranslationY(24f);
            binding.emptyState.animate()
                    .alpha(1f)
                    .translationY(0f)
                    .setDuration(320L)
                    .setInterpolator(new AccelerateDecelerateInterpolator())
                    .start();
        }
    }

    private void showRequestDetails(WorkerJobRequest request) {
        if (request == null) {
            return;
        }

        DialogWorkerJobRequestDetailsBinding dialogBinding = DialogWorkerJobRequestDetailsBinding.inflate(LayoutInflater.from(requireContext()));
        dialogBinding.textServiceValue.setText(resolveOrPlaceholder(request.getServiceName()));
        bindDetail(dialogBinding.textCustomerValue, request.getCustomerName());
        bindDetail(dialogBinding.textContactValue, request.getCustomerPhone());
        bindDetail(dialogBinding.textScheduleValue, request.getScheduleDisplay());
        bindDetail(dialogBinding.textLocationValue, request.getLocation());
        bindDetail(dialogBinding.textNotesValue, request.getNotes());
        String paymentSummary = TextUtils.isEmpty(request.getPriceDisplay())
                ? request.getPaymentMethod()
                : request.getPriceDisplay();
        bindDetail(dialogBinding.textPaymentValue, paymentSummary);

        String normalized = normalizeStatus(request.getStatus());
        boolean isPending = STATUS_PENDING.equalsIgnoreCase(normalized);
        boolean isCompleted = STATUS_COMPLETED.equalsIgnoreCase(normalized);

        MaterialAlertDialogBuilder builder = new MaterialAlertDialogBuilder(requireContext())
                .setTitle(R.string.worker_home_job_details_title)
                .setView(dialogBinding.getRoot())
                .setNegativeButton(R.string.worker_home_job_details_close_button, null);

        if (isPending) {
            builder.setPositiveButton(R.string.worker_home_job_details_accept_button, (dialogInterface, which) -> acceptJobRequest(request));
        }

        final androidx.appcompat.app.AlertDialog dialog = builder.create();

        dialogBinding.layoutCompletionActions.setVisibility(View.VISIBLE);
        if (isCompleted) {
            dialogBinding.buttonTaskCompleted.setEnabled(false);
            dialogBinding.buttonTaskCompleted.setText(R.string.worker_jobs_task_already_completed);
        } else {
            dialogBinding.buttonTaskCompleted.setEnabled(true);
            dialogBinding.buttonTaskCompleted.setText(R.string.worker_jobs_mark_completed);
            dialogBinding.buttonTaskCompleted.setOnClickListener(v -> {
                completeJobRequest(request);
                dialog.dismiss();
            });
        }

        dialog.show();
    }

    private void bindDetail(@NonNull TextView view, String value) {
        view.setText(resolveOrPlaceholder(value));
    }

    private String resolveOrPlaceholder(String value) {
        return TextUtils.isEmpty(value)
                ? getString(R.string.worker_home_job_details_placeholder)
                : value;
    }

    private void acceptJobRequest(WorkerJobRequest request) {
        if (request == null) {
            return;
        }

        String currentStatus = normalizeStatus(request.getStatus());
        if (!STATUS_PENDING.equalsIgnoreCase(currentStatus)) {
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
        loadJobs();
    }

    private void completeJobRequest(WorkerJobRequest request) {
        if (request == null) {
            return;
        }

        List<WorkerJobRequest> requests = sessionManager.getWorkerJobRequests();
        for (int i = 0; i < requests.size(); i++) {
            WorkerJobRequest existing = requests.get(i);
            if (existing != null && request.getId().equals(existing.getId())) {
                requests.set(i, existing.withStatus(STATUS_COMPLETED));
                break;
            }
        }

        sessionManager.saveWorkerJobRequests(requests);
        Snackbar.make(binding.getRoot(), R.string.worker_jobs_task_completed_success, Snackbar.LENGTH_SHORT).show();
        loadJobs();
    }

    private void showCancellationDialog(WorkerJobRequest request) {
        if (request == null) {
            return;
        }

        // Check if job can be cancelled (only pending jobs)
        String currentStatus = normalizeStatus(request.getStatus());
        if (!STATUS_PENDING.equalsIgnoreCase(currentStatus)) {
            Snackbar.make(binding.getRoot(), "This job cannot be cancelled", Snackbar.LENGTH_SHORT).show();
            return;
        }

        // Inflate cancellation dialog
        View dialogView = LayoutInflater.from(requireContext()).inflate(R.layout.dialog_job_cancellation, null);
        
        // Get dialog views
        com.google.android.material.textfield.TextInputEditText inputReason = dialogView.findViewById(R.id.inputReason);
        android.widget.RadioGroup radioGroupReasons = dialogView.findViewById(R.id.radioGroupReasons);
        com.google.android.material.button.MaterialButton buttonCancel = dialogView.findViewById(R.id.buttonCancel);
        com.google.android.material.button.MaterialButton buttonConfirm = dialogView.findViewById(R.id.buttonConfirm);

        // Create dialog
        androidx.appcompat.app.AlertDialog dialog = new androidx.appcompat.app.AlertDialog.Builder(requireContext())
                .setView(dialogView)
                .create();

        // Set up button listeners
        buttonCancel.setOnClickListener(v -> dialog.dismiss());
        
        buttonConfirm.setOnClickListener(v -> {
            String selectedReason = null;
            int selectedId = radioGroupReasons.getCheckedRadioButtonId();
            
            if (selectedId == R.id.radioScheduleConflict) {
                selectedReason = getString(R.string.job_cancellation_reason_schedule_conflict);
            } else if (selectedId == R.id.radioEmergency) {
                selectedReason = getString(R.string.job_cancellation_reason_emergency);
            } else if (selectedId == R.id.radioCustomerRequest) {
                selectedReason = getString(R.string.job_cancellation_reason_customer_request);
            } else if (selectedId == R.id.radioTechnicalIssue) {
                selectedReason = getString(R.string.job_cancellation_reason_technical_issue);
            } else if (selectedId == R.id.radioOther) {
                selectedReason = getString(R.string.job_cancellation_reason_other);
            }

            if (selectedReason == null) {
                Snackbar.make(binding.getRoot(), R.string.job_cancellation_reason_required, Snackbar.LENGTH_SHORT).show();
                return;
            }

            // Add additional details if provided
            String additionalDetails = inputReason.getText() != null ? inputReason.getText().toString().trim() : "";
            if (!additionalDetails.isEmpty()) {
                selectedReason += ": " + additionalDetails;
            }

            cancelJobRequest(request, selectedReason);
            dialog.dismiss();
        });

        dialog.show();
    }

    private void cancelJobRequest(WorkerJobRequest request, String reason) {
        if (request == null) {
            return;
        }

        List<WorkerJobRequest> requests = sessionManager.getWorkerJobRequests();
        for (int i = 0; i < requests.size(); i++) {
            WorkerJobRequest existing = requests.get(i);
            if (existing != null && request.getId().equals(existing.getId())) {
                requests.set(i, existing.withCancellation(reason));
                break;
            }
        }

        sessionManager.saveWorkerJobRequests(requests);
        Snackbar.make(binding.getRoot(), R.string.job_cancellation_success, Snackbar.LENGTH_SHORT).show();
        loadJobs();
    }

    private String normalizeStatus(String status) {
        return TextUtils.isEmpty(status) ? STATUS_PENDING : status.trim();
    }

    private void animateHeroCard() {
        if (binding.cardJobsHero.getAlpha() == 1f) {
            return;
        }
        binding.cardJobsHero.setAlpha(0f);
        binding.cardJobsHero.setTranslationY(28f);
        binding.cardJobsHero.animate()
                .alpha(1f)
                .translationY(0f)
                .setDuration(420L)
                .setInterpolator(new AccelerateDecelerateInterpolator())
                .start();
    }

    private void startHeroAnimations() {
        Animation imageAnimation = AnimationUtils.loadAnimation(requireContext(), R.anim.worker_jobs_hero_pulse);
        Animation buttonAnimation = AnimationUtils.loadAnimation(requireContext(), R.anim.worker_jobs_hero_fade);

        if (binding.imageHero.getAnimation() == null) {
            binding.imageHero.startAnimation(imageAnimation);
        }

        if (binding.buttonScrollToActive.getAnimation() == null) {
            binding.buttonScrollToActive.startAnimation(buttonAnimation);
        }
    }

    @Override
    public void onResume() {
        super.onResume();
        if (binding != null) {
            loadJobs();
        }
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }

    /**
     * Checks if a job request is for the current logged-in worker
     * A request is for the current worker if:
     * 1. The request's serviceOwnerId matches the current worker's ID, OR
     * 2. The request was created for the current worker (backward compatibility)
     */
    private boolean isRequestForCurrentWorker(@NonNull WorkerJobRequest request, @NonNull String currentWorkerId) {
        // Check if this request is specifically for the current worker (service owner)
        if (!TextUtils.isEmpty(request.getServiceOwnerId())) {
            return currentWorkerId.equals(request.getServiceOwnerId());
        }
        
        // For backward compatibility, also check if the service belongs to current worker
        // This handles cases where service owner info might not be available
        String serviceId = request.getServiceId();
        if (!TextUtils.isEmpty(serviceId)) {
            // Check if current worker owns this service
            List<com.skilllink.model.WorkerService> workerServices = sessionManager.getWorkerServices();
            for (com.skilllink.model.WorkerService service : workerServices) {
                if (serviceId.equals(service.getId()) && currentWorkerId.equals(service.getOwnerId())) {
                    return true;
                }
            }
        }
        
        return false;
    }
}
