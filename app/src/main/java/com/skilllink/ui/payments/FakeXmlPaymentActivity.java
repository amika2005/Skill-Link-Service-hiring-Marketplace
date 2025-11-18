package com.skilllink.ui.payments;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.text.Editable;
import android.text.TextUtils;
import android.text.TextWatcher;
import android.view.View;
import android.widget.TextView;
import android.widget.ViewAnimator;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import com.google.android.material.appbar.MaterialToolbar;
import com.google.android.material.textfield.TextInputEditText;
import com.google.android.material.textfield.TextInputLayout;
import com.skilllink.R;
import com.skilllink.ui.payments.PaymentGatewayManager;

import java.util.Calendar;
import java.util.Locale;
import java.util.Random;
import java.util.UUID;

public class FakeXmlPaymentActivity extends AppCompatActivity {

    public static final String EXTRA_AMOUNT = "extra_amount";
    public static final String EXTRA_SERVICE = "extra_service";
    public static final String EXTRA_SUCCESS = "extra_success";
    public static final String EXTRA_REFERENCE = "extra_reference";
    public static final String EXTRA_CARD_LAST4 = "extra_card_last4";

    private final Handler handler = new Handler(Looper.getMainLooper());
    private TextInputLayout inputLayoutName;
    private TextInputLayout inputLayoutCard;
    private TextInputLayout inputLayoutExpiry;
    private TextInputLayout inputLayoutCvv;
    private TextInputEditText inputCard;
    private TextInputEditText inputName;
    private TextInputEditText inputExpiry;
    private TextInputEditText inputCvv;
    private View buttonPay;
    private View buttonSuccess;
    private View processingView;
    private View successView;
    private View formView;
    private ViewAnimator viewAnimator;

    private TextView textAmountValue;
    private TextView textServiceSummary;
    private TextView textSuccessBody;
    private TextView textSuccessCard;
    private TextView textSuccessReference;

    private String amount;
    private String service;
    private boolean processing;
    private String reference;
    private String last4;

    public static Intent createIntent(@NonNull Context context, @Nullable CharSequence amount, @Nullable CharSequence service) {
        Intent intent = new Intent(context, FakeXmlPaymentActivity.class);
        if (!TextUtils.isEmpty(amount)) {
            intent.putExtra(EXTRA_AMOUNT, amount.toString());
        }
        if (!TextUtils.isEmpty(service)) {
            intent.putExtra(EXTRA_SERVICE, service.toString());
        }
        return intent;
    }

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_fake_xml_payment);
        MaterialToolbar toolbar = findViewById(R.id.toolbar);
        toolbar.setNavigationOnClickListener(v -> cancelAndFinish());

        viewAnimator = findViewById(R.id.viewSwitcher);
        formView = viewAnimator.getChildAt(0);
        processingView = viewAnimator.getChildAt(1);
        successView = viewAnimator.getChildAt(2);
        viewAnimator.setDisplayedChild(viewAnimator.indexOfChild(formView));

        inputLayoutName = findViewById(R.id.inputLayoutName);
        inputLayoutCard = findViewById(R.id.inputLayoutCard);
        inputLayoutExpiry = findViewById(R.id.inputLayoutExpiry);
        inputLayoutCvv = findViewById(R.id.inputLayoutCvv);
        inputCard = findViewById(R.id.inputCard);
        inputName = findViewById(R.id.inputName);
        inputExpiry = findViewById(R.id.inputExpiry);
        inputCvv = findViewById(R.id.inputCvv);
        buttonPay = findViewById(R.id.buttonPay);
        buttonSuccess = findViewById(R.id.buttonSuccessDone);

        textAmountValue = findViewById(R.id.textAmountValue);
        textServiceSummary = findViewById(R.id.textServiceSummary);
        textSuccessBody = findViewById(R.id.textSuccessBody);
        textSuccessCard = findViewById(R.id.textSuccessCard);
        textSuccessReference = findViewById(R.id.textSuccessReference);

        amount = getIntent().getStringExtra(EXTRA_AMOUNT);
        service = getIntent().getStringExtra(EXTRA_SERVICE);

        textAmountValue.setText(!TextUtils.isEmpty(amount)
                ? amount
                : getString(R.string.fake_payment_amount_placeholder));
        if (!TextUtils.isEmpty(service)) {
            textServiceSummary.setText(service);
        } else {
            textServiceSummary.setVisibility(View.GONE);
        }

        buttonPay.setOnClickListener(v -> {
            if (processing) {
                return;
            }
            hideErrors();
            if (validateForm()) {
                simulatePayment();
            }
        });

        buttonSuccess.setOnClickListener(v -> {
            Intent data = new Intent();
            data.putExtra(EXTRA_SUCCESS, true);
            data.putExtra(EXTRA_AMOUNT, amount);
            data.putExtra(EXTRA_REFERENCE, reference);
            data.putExtra(EXTRA_CARD_LAST4, last4);
            setResult(RESULT_OK, data);
            finish();
        });

        inputCard.addTextChangedListener(new CardFormattingWatcher(inputCard));
    }

    @Override
    public void onBackPressed() {
        if (processing) {
            return;
        }
        cancelAndFinish();
    }

    private void cancelAndFinish() {
        Intent data = new Intent();
        data.putExtra(EXTRA_SUCCESS, false);
        setResult(RESULT_CANCELED, data);
        finish();
    }

    private void simulatePayment() {
        processing = true;
        viewAnimator.setDisplayedChild(viewAnimator.indexOfChild(processingView));
        buttonPay.setEnabled(false);
        handler.postDelayed(() -> {
            processing = false;
            buttonPay.setEnabled(true);
            reference = generateReference();
            last4 = getLast4(inputCard.getText());
            showSuccess();
        }, 1600L + new Random().nextInt(900));
    }

    private void showSuccess() {
        CharSequence amountDisplay = !TextUtils.isEmpty(amount)
                ? amount
                : getString(R.string.fake_payment_amount_placeholder);
        textSuccessBody.setText(getString(R.string.fake_payment_success_body_amount, amountDisplay));
        textSuccessReference.setText(getString(R.string.fake_payment_success_reference_format, reference));
        textSuccessCard.setText(getString(R.string.fake_payment_success_card_format, maskCard(inputCard.getText())));
        viewAnimator.setDisplayedChild(viewAnimator.indexOfChild(successView));
    }

    private void hideErrors() {
        inputLayoutName.setError(null);
        inputLayoutCard.setError(null);
        inputLayoutExpiry.setError(null);
        inputLayoutCvv.setError(null);
    }

    private boolean validateForm() {
        boolean valid = true;
        if (TextUtils.isEmpty(getText(inputName))) {
            inputLayoutName.setError(getString(R.string.fake_payment_error_name));
            valid = false;
        }
        CharSequence cardNumber = getText(inputCard);
        if (TextUtils.isEmpty(cardNumber) || removeSpaces(cardNumber).length() < 12) {
            inputLayoutCard.setError(getString(R.string.fake_payment_error_card));
            valid = false;
        }
        if (!isValidExpiry(getText(inputExpiry))) {
            inputLayoutExpiry.setError(getString(R.string.fake_payment_error_expiry));
            valid = false;
        }
        CharSequence cvvValue = getText(inputCvv);
        if (TextUtils.isEmpty(cvvValue) || cvvValue.length() < 3) {
            inputLayoutCvv.setError(getString(R.string.fake_payment_error_cvv));
            valid = false;
        }
        return valid;
    }

    private boolean isValidExpiry(@Nullable CharSequence expiry) {
        if (TextUtils.isEmpty(expiry) || expiry.length() != 5 || expiry.charAt(2) != '/') {
            return false;
        }
        String[] parts = expiry.toString().split("/");
        if (parts.length != 2) {
            return false;
        }
        try {
            int month = Integer.parseInt(parts[0]);
            int year = Integer.parseInt(parts[1]) + 2000;
            if (month < 1 || month > 12) {
                return false;
            }
            Calendar calendar = Calendar.getInstance();
            calendar.set(Calendar.DAY_OF_MONTH, 1);
            calendar.set(Calendar.MONTH, month - 1);
            calendar.set(Calendar.YEAR, year);
            calendar.set(Calendar.HOUR_OF_DAY, 23);
            calendar.set(Calendar.MINUTE, 59);
            calendar.set(Calendar.SECOND, 59);
            return calendar.getTimeInMillis() >= System.currentTimeMillis();
        } catch (Exception e) {
            return false;
        }
    }

    private CharSequence getText(@Nullable TextInputEditText editText) {
        return editText != null ? editText.getText() : null;
    }

    private String maskCard(@Nullable CharSequence card) {
        String digits = removeSpaces(card);
        if (digits.length() < 4) {
            return "••••";
        }
        return "•••• " + digits.substring(digits.length() - 4);
    }

    private String getLast4(@Nullable CharSequence card) {
        String digits = removeSpaces(card);
        if (digits.length() < 4) {
            return digits;
        }
        return digits.substring(digits.length() - 4);
    }

    private String removeSpaces(@Nullable CharSequence input) {
        return input != null ? input.toString().replace(" ", "") : "";
    }

    private String generateReference() {
        return "XML" + UUID.randomUUID().toString().replace("-", "").substring(0, 8).toUpperCase(Locale.US);
    }

    private static class CardFormattingWatcher implements TextWatcher {

        private final TextInputEditText editText;
        CardFormattingWatcher(TextInputEditText editText) {
            this.editText = editText;
        }

        @Override
        public void beforeTextChanged(CharSequence s, int start, int count, int after) {
        }

        @Override
        public void onTextChanged(CharSequence s, int start, int before, int count) {
        }

        @Override
        public void afterTextChanged(Editable s) {
            editText.removeTextChangedListener(this);
            String digits = s.toString().replace(" ", "");
            StringBuilder builder = new StringBuilder();
            for (int i = 0; i < digits.length(); i++) {
                if (i > 0 && i % 4 == 0) {
                    builder.append(' ');
                }
                builder.append(digits.charAt(i));
            }
            editText.setText(builder.toString());
            editText.setSelection(editText.getText().length());
            editText.addTextChangedListener(this);
        }
    }

    @Override
    protected void onDestroy() {
        handler.removeCallbacksAndMessages(null);
        super.onDestroy();
    }
}
