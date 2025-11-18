package com.skilllink.ui.user.account;

import android.os.Bundle;
import android.text.TextUtils;
import android.widget.Toast;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import com.google.android.material.textfield.TextInputEditText;
import com.skilllink.BuildConfig;
import com.skilllink.R;
import com.skilllink.databinding.ActivityManagePaymentsBinding;
import com.skilllink.util.SessionManager;

public class ManagePaymentsActivity extends AppCompatActivity {

    private ActivityManagePaymentsBinding binding;
    private SessionManager sessionManager;
    private String existingCardLast4;
    private String defaultMerchantId;
    private String defaultMerchantSecret;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityManagePaymentsBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        sessionManager = new SessionManager(this);
        defaultMerchantId = BuildConfig.PAYHERE_MERCHANT_ID;
        defaultMerchantSecret = BuildConfig.PAYHERE_MERCHANT_SECRET;

        binding.toolbar.setTitle(R.string.manage_payments_title);
        binding.toolbar.setNavigationOnClickListener(v -> getOnBackPressedDispatcher().onBackPressed());

        binding.switchCash.setOnCheckedChangeListener((buttonView, isChecked) -> updateSwitchSummary(isChecked));

        populateFields();

        binding.buttonSave.setOnClickListener(v -> savePaymentMethods());
    }

    private void populateFields() {
        boolean cashEnabled = sessionManager.isPaymentCashEnabled();
        binding.switchCash.setChecked(cashEnabled);
        updateSwitchSummary(cashEnabled);

        SessionManager.SavedCard savedCard = sessionManager.getPrimarySavedCard();
        String holderName = savedCard != null ? savedCard.holderName : null;
        existingCardLast4 = savedCard != null ? savedCard.last4 : null;

        binding.inputCardHolder.setText(!TextUtils.isEmpty(holderName) ? holderName : "");
        binding.inputCardNumber.setText("");
        binding.inputCardNumber.setError(null);

        String merchantId = sessionManager.getPayHereMerchantId();
        if (TextUtils.isEmpty(merchantId)) {
            merchantId = defaultMerchantId;
        }
        binding.inputMerchantId.setText(!TextUtils.isEmpty(merchantId) ? merchantId : "");
        binding.inputMerchantId.setError(null);

        String merchantSecret = sessionManager.getPayHereMerchantSecret();
        if (TextUtils.isEmpty(merchantSecret)) {
            merchantSecret = defaultMerchantSecret;
        }
        binding.inputMerchantSecret.setText(!TextUtils.isEmpty(merchantSecret) ? merchantSecret : "");
        binding.inputMerchantSecret.setError(null);

        if (!TextUtils.isEmpty(existingCardLast4)) {
            binding.textCurrentCard.setText(getString(R.string.manage_payments_current_card_format, existingCardLast4));
            binding.textCurrentCard.setVisibility(android.view.View.VISIBLE);
        } else {
            binding.textCurrentCard.setVisibility(android.view.View.GONE);
        }
    }

    private void updateSwitchSummary(boolean enabled) {
        binding.switchCashSummary.setText(enabled
                ? R.string.manage_payments_cash_enabled_summary
                : R.string.manage_payments_cash_disabled_summary);
    }

    private void savePaymentMethods() {
        boolean cashEnabled = binding.switchCash.isChecked();
        sessionManager.setPaymentCashEnabled(cashEnabled);

        String cardHolder = readText(binding.inputCardHolder);
        String sanitizedCardNumber = sanitizeCardNumber(readText(binding.inputCardNumber));

        if (!TextUtils.isEmpty(sanitizedCardNumber)) {
            if (sanitizedCardNumber.length() < 4) {
                binding.inputCardNumber.setError(getString(R.string.manage_payments_invalid_card));
                return;
            }
            String last4 = sanitizedCardNumber.substring(sanitizedCardNumber.length() - 4);
            sessionManager.addSavedCard(cardHolder, sanitizedCardNumber, null);
            existingCardLast4 = last4;
        } else {
            binding.inputCardNumber.setError(null);

            if (TextUtils.isEmpty(cardHolder)) {
                sessionManager.clearPaymentCardDetails();
                existingCardLast4 = null;
            } else if (!TextUtils.isEmpty(existingCardLast4)) {
                sessionManager.addSavedCard(cardHolder, existingCardLast4, null);
            } else {
                sessionManager.clearPaymentCardDetails();
                existingCardLast4 = null;
            }
        }

        String merchantIdInput = readText(binding.inputMerchantId);
        if (TextUtils.isEmpty(merchantIdInput)) {
            binding.inputMerchantId.setError(null);
            binding.inputMerchantId.setText(defaultMerchantId);
            sessionManager.savePayHereMerchantId(null);
        } else {
            binding.inputMerchantId.setError(null);
            sessionManager.savePayHereMerchantId(merchantIdInput);
        }

        String merchantSecretInput = readText(binding.inputMerchantSecret);
        if (TextUtils.isEmpty(merchantSecretInput)) {
            binding.inputMerchantSecret.setError(null);
            binding.inputMerchantSecret.setText(defaultMerchantSecret);
            sessionManager.savePayHereMerchantSecret(null);
        } else {
            binding.inputMerchantSecret.setError(null);
            sessionManager.savePayHereMerchantSecret(merchantSecretInput);
        }

        Toast.makeText(this, R.string.manage_payments_saved_toast, Toast.LENGTH_SHORT).show();
        finish();
    }

    private String sanitizeCardNumber(String value) {
        if (TextUtils.isEmpty(value)) {
            return "";
        }
        return value.replaceAll("[^0-9]", "");
    }

    private String readText(TextInputEditText editText) {
        return editText.getText() != null ? editText.getText().toString().trim() : "";
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        binding = null;
    }
}
