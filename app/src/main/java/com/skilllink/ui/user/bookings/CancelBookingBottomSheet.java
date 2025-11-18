package com.skilllink.ui.user.bookings;

import android.os.Bundle;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.RadioGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.google.android.material.bottomsheet.BottomSheetDialogFragment;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.radiobutton.MaterialRadioButton;
import com.google.android.material.textfield.TextInputEditText;
import com.google.android.material.textfield.TextInputLayout;
import com.skilllink.R;
import com.skilllink.model.UserBooking;

import org.json.JSONException;
import org.json.JSONObject;

public class CancelBookingBottomSheet extends BottomSheetDialogFragment {

    public static final String REQUEST_KEY = "cancel_booking_request";
    public static final String RESULT_BOOKING_ID = "result_booking_id";
    public static final String RESULT_REASON = "result_reason";
    private static final String ARG_BOOKING = "arg_booking";
    private static final String TAG_OTHER = "other_reason";

    private UserBooking booking;

    public static CancelBookingBottomSheet newInstance(UserBooking booking) {
        CancelBookingBottomSheet sheet = new CancelBookingBottomSheet();
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
        return inflater.inflate(R.layout.bottom_sheet_cancel_booking, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        if (booking == null) {
            dismissAllowingStateLoss();
            return;
        }

        TextView subtitle = view.findViewById(R.id.text_subtitle);
        subtitle.setText(getString(R.string.booking_cancel_subtitle));

        RadioGroup reasonGroup = view.findViewById(R.id.group_reasons);
        TextInputLayout otherLayout = view.findViewById(R.id.input_other_layout);
        TextInputEditText otherInput = view.findViewById(R.id.input_other);
        MaterialButton confirmButton = view.findViewById(R.id.button_confirm);
        MaterialButton dismissButton = view.findViewById(R.id.button_dismiss);

        String[] reasons = getResources().getStringArray(R.array.booking_cancel_reasons);
        int firstId = View.NO_ID;
        for (int i = 0; i < reasons.length; i++) {
            MaterialRadioButton option = new MaterialRadioButton(requireContext());
            option.setText(reasons[i]);
            option.setId(View.generateViewId());
            option.setTextAppearance(com.google.android.material.R.style.TextAppearance_MaterialComponents_Body1);
            option.setPadding(0, option.getPaddingTop(), 0, option.getPaddingBottom());
            if (i == reasons.length - 1) {
                option.setTag(TAG_OTHER);
            }
            reasonGroup.addView(option);
            if (i == 0) {
                firstId = option.getId();
            }
        }

        if (firstId != View.NO_ID) {
            reasonGroup.check(firstId);
        }
        otherLayout.setEnabled(false);
        otherInput.setEnabled(false);

        reasonGroup.setOnCheckedChangeListener((group, checkedId) -> {
            View selected = group.findViewById(checkedId);
            boolean isOther = selected != null && TAG_OTHER.equals(selected.getTag());
            otherLayout.setEnabled(isOther);
            otherInput.setEnabled(isOther);
            otherLayout.setError(null);
            if (!isOther) {
                otherInput.setText(null);
            }
        });

        confirmButton.setOnClickListener(v -> {
            int checkedId = reasonGroup.getCheckedRadioButtonId();
            if (checkedId == View.NO_ID) {
                otherLayout.setError(getString(R.string.booking_cancel_error_reason));
                return;
            }

            View selected = reasonGroup.findViewById(checkedId);
            boolean isOther = selected != null && TAG_OTHER.equals(selected.getTag());
            String reason;

            if (isOther) {
                CharSequence input = otherInput.getText();
                if (input == null || TextUtils.isEmpty(input.toString().trim())) {
                    otherLayout.setError(getString(R.string.booking_cancel_error_reason));
                    return;
                }
                otherLayout.setError(null);
                reason = input.toString().trim();
            } else if (selected instanceof MaterialRadioButton) {
                otherLayout.setError(null);
                reason = ((MaterialRadioButton) selected).getText().toString();
            } else {
                otherLayout.setError(getString(R.string.booking_cancel_error_reason));
                return;
            }

            Bundle result = new Bundle();
            result.putString(RESULT_BOOKING_ID, booking.getId());
            result.putString(RESULT_REASON, reason);
            getParentFragmentManager().setFragmentResult(REQUEST_KEY, result);
            dismissAllowingStateLoss();
        });

        dismissButton.setOnClickListener(v -> dismissAllowingStateLoss());
    }
}
