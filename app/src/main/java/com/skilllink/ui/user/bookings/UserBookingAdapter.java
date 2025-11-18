package com.skilllink.ui.user.bookings;

import android.content.Context;
import android.content.res.ColorStateList;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.button.MaterialButton;
import com.google.android.material.chip.Chip;
import com.google.android.material.imageview.ShapeableImageView;
import com.skilllink.R;
import com.skilllink.model.UserBooking;
import com.skilllink.util.ImageLoader;

import java.util.List;

class UserBookingAdapter extends RecyclerView.Adapter<UserBookingAdapter.BookingViewHolder> {

    interface BookingActionListener {
        void onMessage(UserBooking booking);
        void onDetails(UserBooking booking);
        void onCancel(UserBooking booking);
    }

    private final List<UserBooking> items;
    private final Context context;
    private BookingActionListener listener;

    UserBookingAdapter(Context context, List<UserBooking> items) {
        this.context = context;
        this.items = items;
    }

    void setBookingActionListener(BookingActionListener listener) {
        this.listener = listener;
    }

    @NonNull
    @Override
    public BookingViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_booking, parent, false);
        return new BookingViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull BookingViewHolder holder, int position) {
        UserBooking booking = items.get(position);
        holder.bind(context, booking, listener);
    }

    @Override
    public int getItemCount() {
        return items.size();
    }

    static class BookingViewHolder extends RecyclerView.ViewHolder {

        private final ShapeableImageView imageService;
        private final TextView textServiceName;
        private final TextView textServiceCategory;
        private final TextView textNotes;
        private final TextView textSchedule;
        private final TextView textPrice;
        private final TextView textLocation;
        private final TextView textPayment;
        private final Chip chipStatus;
        private final MaterialButton buttonViewDetails;
        private final MaterialButton buttonMessage;
        private final MaterialButton buttonCancel;

        BookingViewHolder(@NonNull View itemView) {
            super(itemView);
            imageService = itemView.findViewById(R.id.image_service);
            textServiceName = itemView.findViewById(R.id.text_service_name);
            textServiceCategory = itemView.findViewById(R.id.text_service_category);
            textNotes = itemView.findViewById(R.id.text_notes);
            textSchedule = itemView.findViewById(R.id.text_schedule);
            textPrice = itemView.findViewById(R.id.text_price);
            textLocation = itemView.findViewById(R.id.text_location);
            textPayment = itemView.findViewById(R.id.text_payment);
            chipStatus = itemView.findViewById(R.id.chip_status);
            buttonViewDetails = itemView.findViewById(R.id.button_view_details);
            buttonMessage = itemView.findViewById(R.id.button_message);
            buttonCancel = itemView.findViewById(R.id.button_cancel);
        }

        void bind(Context context, UserBooking booking, BookingActionListener listener) {
            ImageLoader.loadUriInto(context, imageService, booking.getImageUri(), R.drawable.ic_service_manage, R.color.primary_color);
            textServiceName.setText(booking.getServiceName());
            textServiceCategory.setText(booking.getServiceCategory());

            if (booking.getNotes() == null || booking.getNotes().trim().isEmpty()) {
                textNotes.setVisibility(View.GONE);
            } else {
                textNotes.setVisibility(View.VISIBLE);
                textNotes.setText(booking.getNotes());
            }

            CharSequence schedule = booking.getScheduleDisplay();
            if (schedule == null || schedule.length() == 0) {
                textSchedule.setText(R.string.bookings_schedule_pending);
            } else {
                textSchedule.setText(schedule);
            }

            String price = booking.getPriceDisplay();
            if (price == null || price.trim().isEmpty()) {
                textPrice.setText(R.string.bookings_price_pending);
            } else {
                textPrice.setText(price);
            }

            if (booking.getLocation() == null || booking.getLocation().trim().isEmpty()) {
                textLocation.setText(R.string.bookings_location_pending);
            } else {
                textLocation.setText(booking.getLocation());
            }

            if (booking.getPaymentMethod() == null || booking.getPaymentMethod().trim().isEmpty()) {
                textPayment.setText(R.string.bookings_payment_pending);
            } else {
                textPayment.setText(booking.getPaymentMethod());
            }

            String status = booking.getStatus();
            chipStatus.setText(status);
            boolean isCancelled = status != null && status.equalsIgnoreCase(context.getString(R.string.bookings_status_cancelled));
            if (isCancelled) {
                chipStatus.setChipBackgroundColor(ColorStateList.valueOf(0xFFFFEBEE));
                chipStatus.setChipIconResource(R.drawable.ic_close);
                int cancelledColor = ContextCompat.getColor(context, R.color.error_color);
                chipStatus.setChipIconTint(ColorStateList.valueOf(cancelledColor));
                chipStatus.setTextColor(cancelledColor);
            } else {
                chipStatus.setChipBackgroundColor(ColorStateList.valueOf(0xFFE9F2FF));
                chipStatus.setChipIconResource(R.drawable.ic_check_circle);
                int activeColor = ContextCompat.getColor(context, R.color.primary_color);
                chipStatus.setChipIconTint(ColorStateList.valueOf(activeColor));
                chipStatus.setTextColor(0xFF1A202C);
            }

            buttonCancel.setText(isCancelled
                    ? context.getString(R.string.bookings_status_cancelled)
                    : context.getString(R.string.bookings_action_cancel));
            buttonCancel.setEnabled(!isCancelled);
            buttonCancel.setAlpha(isCancelled ? 0.5f : 1f);

            buttonMessage.setOnClickListener(v -> {
                if (listener != null) {
                    listener.onMessage(booking);
                }
            });

            buttonViewDetails.setOnClickListener(v -> {
                if (listener != null) {
                    listener.onDetails(booking);
                }
            });

            if (isCancelled) {
                buttonCancel.setOnClickListener(null);
            } else {
                buttonCancel.setOnClickListener(v -> {
                    if (listener != null) {
                        listener.onCancel(booking);
                    }
                });
            }
        }
    }
}
