package com.skilllink.ui.user.bookings;

import android.content.res.ColorStateList;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;

import com.google.android.material.bottomsheet.BottomSheetDialogFragment;
import com.google.android.material.chip.Chip;
import com.google.android.material.imageview.ShapeableImageView;
import com.skilllink.R;
import com.skilllink.model.UserBooking;
import com.skilllink.util.ImageLoader;

import org.json.JSONException;
import org.json.JSONObject;

public class BookingDetailsBottomSheet extends BottomSheetDialogFragment {

    private static final String ARG_BOOKING = "arg_booking";
    private UserBooking booking;

    public static BookingDetailsBottomSheet newInstance(UserBooking booking) {
        BookingDetailsBottomSheet sheet = new BookingDetailsBottomSheet();
        try {
            JSONObject payload = booking.toJson();
            Bundle args = new Bundle();
            args.putString(ARG_BOOKING, payload.toString());
            sheet.setArguments(args);
        } catch (JSONException ignored) {
            // Ignore malformed payloads
        }
        return sheet;
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Bundle args = getArguments();
        if (args != null) {
            String raw = args.getString(ARG_BOOKING);
            if (!TextUtils.isEmpty(raw)) {
                try {
                    booking = UserBooking.fromJson(new JSONObject(raw));
                } catch (JSONException ignored) {
                    // Ignore malformed payloads
                }
            }
        }
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.bottom_sheet_booking_details, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        if (booking == null) {
            dismissAllowingStateLoss();
            return;
        }
        bindViews(view);
    }

    private void bindViews(View root) {
        ShapeableImageView imageService = root.findViewById(R.id.image_service);
        TextView textServiceName = root.findViewById(R.id.text_service_name);
        TextView textServiceCategory = root.findViewById(R.id.text_service_category);
        Chip chipStatus = root.findViewById(R.id.chip_status);
        TextView textSchedule = root.findViewById(R.id.text_schedule);
        TextView textLocation = root.findViewById(R.id.text_location);
        TextView textPrice = root.findViewById(R.id.text_price);
        TextView textPayment = root.findViewById(R.id.text_payment);
        View notesContainer = root.findViewById(R.id.container_notes);
        TextView textNotes = root.findViewById(R.id.text_notes);
        View cancellationContainer = root.findViewById(R.id.container_cancellation);
        TextView textCancellationReason = root.findViewById(R.id.text_cancellation_reason);

        ImageLoader.loadUriInto(requireContext(), imageService, booking.getImageUri(), R.drawable.ic_service_manage, R.color.primary_color);
        textServiceName.setText(booking.getServiceName());
        textServiceCategory.setText(booking.getServiceCategory());

        String status = booking.getStatus();
        chipStatus.setText(status);
        if (status != null && status.equalsIgnoreCase(getString(R.string.bookings_status_cancelled))) {
            int cancelledColor = ContextCompat.getColor(requireContext(), R.color.error_color);
            chipStatus.setChipBackgroundColor(ColorStateList.valueOf(0xFFFFEBEE));
            chipStatus.setChipIconResource(R.drawable.ic_close);
            chipStatus.setChipIconTint(ColorStateList.valueOf(cancelledColor));
            chipStatus.setTextColor(cancelledColor);
        } else {
            int activeColor = ContextCompat.getColor(requireContext(), R.color.primary_color);
            chipStatus.setChipBackgroundColor(ColorStateList.valueOf(0xFFE9F2FF));
            chipStatus.setChipIconResource(R.drawable.ic_check_circle);
            chipStatus.setChipIconTint(ColorStateList.valueOf(activeColor));
            chipStatus.setTextColor(0xFF1A202C);
        }

        CharSequence schedule = booking.getScheduleDisplay();
        if (TextUtils.isEmpty(schedule)) {
            textSchedule.setText(R.string.bookings_schedule_pending);
        } else {
            textSchedule.setText(schedule);
        }

        if (TextUtils.isEmpty(booking.getLocation())) {
            textLocation.setText(R.string.bookings_location_pending);
        } else {
            textLocation.setText(booking.getLocation());
        }

        if (TextUtils.isEmpty(booking.getPriceDisplay())) {
            textPrice.setText(R.string.bookings_price_pending);
        } else {
            textPrice.setText(booking.getPriceDisplay());
        }

        if (TextUtils.isEmpty(booking.getPaymentMethod())) {
            textPayment.setText(R.string.bookings_payment_pending);
        } else {
            textPayment.setText(booking.getPaymentMethod());
        }

        if (TextUtils.isEmpty(booking.getNotes())) {
            notesContainer.setVisibility(View.GONE);
        } else {
            notesContainer.setVisibility(View.VISIBLE);
            textNotes.setText(booking.getNotes());
        }

        if (TextUtils.isEmpty(booking.getCancellationReason())) {
            cancellationContainer.setVisibility(View.GONE);
        } else {
            cancellationContainer.setVisibility(View.VISIBLE);
            textCancellationReason.setText(booking.getCancellationReason());
        }
    }
}
