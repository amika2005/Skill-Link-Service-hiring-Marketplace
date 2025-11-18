package com.skilllink.ui.user.bookings;

import android.content.Intent;
import android.content.res.ColorStateList;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;

import com.skilllink.R;
import com.skilllink.databinding.FragmentUserBookingsBinding;
import com.skilllink.model.UserBooking;
import com.skilllink.model.RecommendedWorker;
import com.skilllink.model.WorkerService;
import com.google.android.material.snackbar.Snackbar;
import com.skilllink.ui.user.chat.WorkerChatActivity;
import com.skilllink.util.SessionManager;
import com.skilllink.util.ImageLoader;

import java.util.ArrayList;
import java.util.List;

public class BookingsFragment extends Fragment {

    private FragmentUserBookingsBinding binding;
    private SessionManager sessionManager;
    private UserBookingAdapter adapter;
    private final List<UserBooking> cachedBookings = new ArrayList<>();
    private UserBooking highlightedBooking;

    public View onCreateView(@NonNull LayoutInflater inflater,
                             ViewGroup container, Bundle savedInstanceState) {
        binding = FragmentUserBookingsBinding.inflate(inflater, container, false);
        View root = binding.getRoot();

        sessionManager = new SessionManager(requireContext());

        initViews();
        setupRecyclerView();
        setupResultListeners();
        loadBookings();

        return root;
    }

    private void initViews() {
        binding.btnBookService.setOnClickListener(v -> openBookingFlow());
        binding.buttonEmptyBook.setOnClickListener(v -> openBookingFlow());
        binding.buttonFilter.setOnClickListener(v ->
                Toast.makeText(getContext(), R.string.feature_coming_soon, Toast.LENGTH_SHORT).show()
        );
    }

    private void setupRecyclerView() {
        binding.recyclerViewBookings.setLayoutManager(new LinearLayoutManager(getContext()));
        adapter = new UserBookingAdapter(requireContext(), cachedBookings);
        adapter.setBookingActionListener(new UserBookingAdapter.BookingActionListener() {
            @Override
            public void onMessage(UserBooking booking) {
                openChatForBooking(booking);
            }

            @Override
            public void onDetails(UserBooking booking) {
                showBookingDetails(booking);
            }

            @Override
            public void onCancel(UserBooking booking) {
                promptCancelBooking(booking);
            }
        });
        binding.recyclerViewBookings.setAdapter(adapter);
    }

    private void setupResultListeners() {
        getChildFragmentManager().setFragmentResultListener(
                CancelBookingBottomSheet.REQUEST_KEY,
                getViewLifecycleOwner(),
                (requestKey, bundle) -> {
                    String bookingId = bundle.getString(CancelBookingBottomSheet.RESULT_BOOKING_ID);
                    String reason = bundle.getString(CancelBookingBottomSheet.RESULT_REASON);
                    if (!TextUtils.isEmpty(bookingId)) {
                        sessionManager.updateBookingStatus(bookingId, getString(R.string.bookings_status_cancelled), reason);
                        loadBookings();
                        View root = binding != null ? binding.getRoot() : getView();
                        if (root != null) {
                            Snackbar.make(root, R.string.bookings_cancel_success, Snackbar.LENGTH_LONG).show();
                        }
                    }
                }
        );
    }

    private void loadBookings() {
        List<UserBooking> allBookings = sessionManager.getUserBookings();
        List<UserBooking> activeBookings = new ArrayList<>();
        String cancelledLabel = getString(R.string.bookings_status_cancelled);
        for (UserBooking booking : allBookings) {
            if (booking == null) {
                continue;
            }
            String status = booking.getStatus();
            if (status == null || !status.equalsIgnoreCase(cancelledLabel)) {
                activeBookings.add(booking);
            }
        }

        if (activeBookings.isEmpty()) {
            cachedBookings.clear();
            adapter.notifyDataSetChanged();
            showEmptyState();
            return;
        }

        highlightedBooking = activeBookings.get(0);

        List<UserBooking> upcoming = activeBookings.size() > 1
                ? new ArrayList<>(activeBookings.subList(1, activeBookings.size()))
                : new ArrayList<>();

        cachedBookings.clear();
        cachedBookings.addAll(upcoming);
        adapter.notifyDataSetChanged();

        showContentState();
        bindNextBooking(highlightedBooking);
        updateUpcomingSection(upcoming);
    }

    private void showEmptyState() {
        binding.emptyState.setVisibility(View.VISIBLE);
        binding.cardNextBooking.setVisibility(View.GONE);
        binding.cardBookingsList.setVisibility(View.GONE);
        binding.textUpcomingHint.setVisibility(View.GONE);
        binding.chipGroupFilters.setVisibility(View.GONE);
        binding.textBookingsCount.setText("0");
    }

    private void showContentState() {
        binding.emptyState.setVisibility(View.GONE);
        binding.cardNextBooking.setVisibility(View.VISIBLE);
        binding.cardBookingsList.setVisibility(View.VISIBLE);
        binding.textUpcomingHint.setVisibility(View.VISIBLE);
        binding.chipGroupFilters.setVisibility(View.VISIBLE);
    }

    private void bindNextBooking(UserBooking booking) {
        binding.textNextService.setText(booking.getServiceName());
        binding.textNextProvider.setText(booking.getServiceCategory());
        binding.chipStatus.setText(booking.getStatus());
        boolean isCancelled = booking.getStatus() != null
                && booking.getStatus().equalsIgnoreCase(getString(R.string.bookings_status_cancelled));
        if (isCancelled) {
            binding.chipStatus.setChipBackgroundColor(ColorStateList.valueOf(0xFFFFEBEE));
            int cancelledColor = ContextCompat.getColor(requireContext(), R.color.error_color);
            binding.chipStatus.setChipIconResource(R.drawable.ic_close);
            binding.chipStatus.setChipIconTint(ColorStateList.valueOf(cancelledColor));
            binding.chipStatus.setTextColor(cancelledColor);
        } else {
            binding.chipStatus.setChipBackgroundColor(ColorStateList.valueOf(0xFFE9F2FF));
            int activeColor = ContextCompat.getColor(requireContext(), R.color.primary_color);
            binding.chipStatus.setChipIconResource(R.drawable.ic_check_circle);
            binding.chipStatus.setChipIconTint(ColorStateList.valueOf(activeColor));
            binding.chipStatus.setTextColor(0xFF1A202C);
        }

        ImageLoader.loadUriInto(requireContext(), binding.imageNextService, booking.getImageUri(), R.drawable.ic_booking, R.color.primary_color);

        CharSequence schedule = booking.getScheduleDisplay();
        if (TextUtils.isEmpty(schedule)) {
            binding.textNextSchedule.setVisibility(View.GONE);
        } else {
            binding.textNextSchedule.setVisibility(View.VISIBLE);
            binding.textNextSchedule.setText(schedule);
        }

        String price = booking.getPriceDisplay();
        if (TextUtils.isEmpty(price)) {
            binding.textNextPrice.setVisibility(View.VISIBLE);
            binding.textNextPrice.setText(R.string.bookings_price_pending);
        } else {
            binding.textNextPrice.setVisibility(View.VISIBLE);
            binding.textNextPrice.setText(price);
        }

        if (TextUtils.isEmpty(booking.getLocation())) {
            binding.textNextLocation.setVisibility(View.GONE);
        } else {
            binding.textNextLocation.setVisibility(View.VISIBLE);
            binding.textNextLocation.setText(booking.getLocation());
        }

        String payment = booking.getPaymentMethod();
        if (TextUtils.isEmpty(payment)) {
            binding.textNextPayment.setVisibility(View.VISIBLE);
            binding.textNextPayment.setText(R.string.bookings_payment_pending);
        } else {
            binding.textNextPayment.setVisibility(View.VISIBLE);
            binding.textNextPayment.setText(payment);
        }

        binding.buttonNextDetails.setOnClickListener(v -> showBookingDetails(booking));
        binding.buttonNextChat.setOnClickListener(v -> openChatForBooking(booking));
        binding.buttonNextCancel.setText(isCancelled
                ? getString(R.string.bookings_status_cancelled)
                : getString(R.string.bookings_action_cancel));
        binding.buttonNextCancel.setEnabled(!isCancelled);
        binding.buttonNextCancel.setAlpha(isCancelled ? 0.5f : 1f);
        if (isCancelled) {
            binding.buttonNextCancel.setOnClickListener(null);
        } else {
            binding.buttonNextCancel.setOnClickListener(v -> promptCancelBooking(booking));
        }
    }

    private void updateUpcomingSection(List<UserBooking> upcoming) {
        int count = upcoming.size();
        binding.textBookingsCount.setText(String.valueOf(count));

        if (count == 0) {
            binding.textBookingsEmpty.setVisibility(View.VISIBLE);
        } else {
            binding.textBookingsEmpty.setVisibility(View.GONE);
        }
    }

    private void openBookingFlow() {
        List<WorkerService> services = sessionManager.getWorkerServices();
        if (services.isEmpty()) {
            Toast.makeText(getContext(), R.string.worker_service_detail_unavailable, Toast.LENGTH_SHORT).show();
            return;
        }

        WorkerService service = services.get(0);
        Intent intent = new Intent(requireContext(), ServiceBookingActivity.class);
        intent.putExtra(ServiceBookingActivity.EXTRA_SERVICE_ID, service.getId());
        startActivity(intent);
    }

    private void openChatForBooking(UserBooking booking) {
        Intent intent = new Intent(requireContext(), WorkerChatActivity.class);
        intent.putExtra(WorkerChatActivity.EXTRA_SERVICE_ID, booking.getServiceId());
        intent.putExtra(WorkerChatActivity.EXTRA_SERVICE_NAME, booking.getServiceName());
        intent.putExtra(WorkerChatActivity.EXTRA_SERVICE_CATEGORY, booking.getServiceCategory());

        RecommendedWorker worker = sessionManager.findRecommendedWorkerByServiceId(booking.getServiceId());
        if (worker != null) {
            intent.putExtra(WorkerChatActivity.EXTRA_WORKER_ID, worker.getId());
            intent.putExtra(WorkerChatActivity.EXTRA_WORKER_NAME, worker.getName());
            intent.putExtra(WorkerChatActivity.EXTRA_WORKER_OCCUPATION, worker.getOccupation());
            if (!TextUtils.isEmpty(worker.getImageUri())) {
                intent.putExtra(WorkerChatActivity.EXTRA_WORKER_IMAGE_URI, worker.getImageUri());
            }
        }

        startActivity(intent);
    }

    private void showBookingDetails(UserBooking booking) {
        BookingDetailsBottomSheet sheet = BookingDetailsBottomSheet.newInstance(booking);
        sheet.show(getChildFragmentManager(), "booking_details");
    }

    private void promptCancelBooking(UserBooking booking) {
        CancelBookingBottomSheet sheet = CancelBookingBottomSheet.newInstance(booking);
        sheet.show(getChildFragmentManager(), "booking_cancel");
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }

    @Override
    public void onResume() {
        super.onResume();
        if (binding != null) {
            loadBookings();
        }
    }
}