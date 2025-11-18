package com.skilllink.ui.payments;

import android.app.Dialog;
import android.content.DialogInterface;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.DialogFragment;

import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.skilllink.R;
import com.skilllink.databinding.FragmentFakePaymentConfirmationBinding;

import java.util.UUID;

public class FakeXmlPaymentDialog extends DialogFragment {

    public interface OnPaymentConfirmedListener {
        void onPaymentConfirmed();
    }

    private static final String ARG_AMOUNT = "arg_amount";
    private OnPaymentConfirmedListener listener;
    private Runnable dismissListener;

    public static FakeXmlPaymentDialog newInstance(CharSequence amount) {
        Bundle args = new Bundle();
        args.putCharSequence(ARG_AMOUNT, amount);
        FakeXmlPaymentDialog fragment = new FakeXmlPaymentDialog();
        fragment.setArguments(args);
        return fragment;
    }

    public void setOnPaymentConfirmedListener(OnPaymentConfirmedListener listener) {
        this.listener = listener;
    }

    public void setOnDismissListener(Runnable listener) {
        this.dismissListener = listener;
    }

    @NonNull
    @Override
    public Dialog onCreateDialog(@Nullable Bundle savedInstanceState) {
        FragmentFakePaymentConfirmationBinding binding = FragmentFakePaymentConfirmationBinding.inflate(LayoutInflater.from(requireContext()));

        Bundle arguments = getArguments();
        CharSequence amount = arguments != null ? arguments.getCharSequence(ARG_AMOUNT) : null;
        if (amount != null) {
            binding.textSubtitle.setText(getString(R.string.fake_payment_success_body_amount, amount));
        }

        binding.textReference.setVisibility(View.VISIBLE);
        binding.textReference.setText(getString(R.string.fake_payment_success_reference_format,
                UUID.randomUUID().toString().substring(0, 8).toUpperCase()));

        binding.buttonClose.setOnClickListener(v -> {
            if (listener != null) {
                listener.onPaymentConfirmed();
            }
            dismiss();
        });

        MaterialAlertDialogBuilder builder = new MaterialAlertDialogBuilder(requireContext());
        builder.setView(binding.getRoot());
        builder.setCancelable(false);
        Dialog dialog = builder.create();
        dialog.setCanceledOnTouchOutside(false);
        return dialog;
    }

    @Override
    public void onDismiss(@NonNull DialogInterface dialog) {
        super.onDismiss(dialog);
        if (dismissListener != null) {
            dismissListener.run();
        }
    }
}
