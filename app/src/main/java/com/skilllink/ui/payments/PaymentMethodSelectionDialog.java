package com.skilllink.ui.payments;

import android.app.Dialog;
import android.content.Context;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.DialogFragment;

import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.skilllink.R;
import com.skilllink.util.SessionManager;

import java.util.ArrayList;
import java.util.List;

/**
 * Dialog for selecting payment method during checkout
 */
public class PaymentMethodSelectionDialog extends DialogFragment {

    public interface PaymentMethodListener {
        void onPaymentMethodSelected(String paymentMethod);
        void onPaymentMethodSelectionCancelled();
    }

    private static final String ARG_SELECTED_METHOD = "selected_method";
    private static final String ARG_SERVICE_PRICE = "service_price";

    private PaymentMethodListener listener;
    private String selectedMethod;
    private String servicePrice;
    private SessionManager sessionManager;
    private PaymentGatewayManager paymentGateway;

    private static class PaymentOption {
        final String method;
        final String title;
        final String subtitle;
        final int iconRes;

        PaymentOption(String method, String title, String subtitle, int iconRes) {
            this.method = method;
            this.title = title;
            this.subtitle = subtitle;
            this.iconRes = iconRes;
        }
    }

    public static PaymentMethodSelectionDialog newInstance(@Nullable String selectedMethod, @Nullable String servicePrice) {
        PaymentMethodSelectionDialog dialog = new PaymentMethodSelectionDialog();
        Bundle args = new Bundle();
        args.putString(ARG_SELECTED_METHOD, selectedMethod);
        args.putString(ARG_SERVICE_PRICE, servicePrice);
        dialog.setArguments(args);
        return dialog;
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Bundle args = getArguments();
        if (args != null) {
            selectedMethod = args.getString(ARG_SELECTED_METHOD);
            servicePrice = args.getString(ARG_SERVICE_PRICE);
        }
        
        sessionManager = new SessionManager(requireContext());
        paymentGateway = new PaymentGatewayManager(requireContext());
    }

    @NonNull
    @Override
    public Dialog onCreateDialog(@Nullable Bundle savedInstanceState) {
        Context context = requireContext();
        LayoutInflater inflater = LayoutInflater.from(context);
        
        // Create custom view
        View customView = inflater.inflate(R.layout.dialog_payment_method_selection, null);
        
        // Find views
        TextView textTitle = customView.findViewById(R.id.textPaymentTitle);
        TextView textSubtitle = customView.findViewById(R.id.textPaymentSubtitle);
        ViewGroup optionsContainer = customView.findViewById(R.id.paymentOptionsContainer);
        
        // Set title and subtitle
        textTitle.setText("Select Payment Method");
        if (servicePrice != null && !servicePrice.trim().isEmpty()) {
            textSubtitle.setText("Choose how to pay " + servicePrice);
        } else {
            textSubtitle.setText("Choose your preferred payment option");
        }
        
        // Get available payment options
        List<PaymentOption> options = getAvailablePaymentOptions();
        
        // Create payment option views
        for (PaymentOption option : options) {
            View optionView = createPaymentOptionView(context, inflater, option);
            optionsContainer.addView(optionView);
        }
        
        // Build dialog
        return new MaterialAlertDialogBuilder(context)
                .setView(customView)
                .setNegativeButton("Cancel", (dialog, which) -> {
                    if (listener != null) {
                        listener.onPaymentMethodSelectionCancelled();
                    }
                })
                .create();
    }

    private List<PaymentOption> getAvailablePaymentOptions() {
        List<PaymentOption> options = new ArrayList<>();
        
        // Online payment (XML Pay) - always available
        if (paymentGateway.isPaymentMethodAvailable(PaymentGatewayManager.PAYMENT_METHOD_XML_PAY)) {
            options.add(new PaymentOption(
                PaymentGatewayManager.PAYMENT_METHOD_XML_PAY,
                paymentGateway.getGatewayName(),
                "Pay instantly with credit/debit card",
                R.drawable.ic_payment
            ));
        }
        
        // Saved card - always available in demo
        if (paymentGateway.isPaymentMethodAvailable(PaymentGatewayManager.PAYMENT_METHOD_CARD)) {
            options.add(new PaymentOption(
                PaymentGatewayManager.PAYMENT_METHOD_CARD,
                "Saved Card",
                "•••• 4242",
                R.drawable.ic_payment
            ));
        }
        
        // Cash - if enabled
        if (paymentGateway.isPaymentMethodAvailable(PaymentGatewayManager.PAYMENT_METHOD_CASH)) {
            options.add(new PaymentOption(
                PaymentGatewayManager.PAYMENT_METHOD_CASH,
                "Cash on Delivery",
                "Pay when service is completed",
                R.drawable.ic_cash
            ));
        }
        
        return options;
    }

    private View createPaymentOptionView(Context context, LayoutInflater inflater, PaymentOption option) {
        View view = inflater.inflate(R.layout.item_payment_method_option, null);
        
        TextView textTitle = view.findViewById(R.id.textPaymentOptionTitle);
        TextView textSubtitle = view.findViewById(R.id.textPaymentOptionSubtitle);
        View iconContainer = view.findViewById(R.id.paymentOptionIcon);
        
        textTitle.setText(option.title);
        textSubtitle.setText(option.subtitle);
        
        // Set selected state
        boolean isSelected = option.method.equals(selectedMethod);
        view.setSelected(isSelected);
        
        // Set click listener
        view.setOnClickListener(v -> {
            selectedMethod = option.method;
            if (listener != null) {
                listener.onPaymentMethodSelected(selectedMethod);
            }
            dismiss();
        });
        
        return view;
    }

    public void setPaymentMethodListener(@Nullable PaymentMethodListener listener) {
        this.listener = listener;
    }

    @Nullable
    public String getSelectedMethod() {
        return selectedMethod;
    }
}
