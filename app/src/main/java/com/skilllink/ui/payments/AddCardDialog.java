package com.skilllink.ui.payments;

import android.app.Dialog;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextUtils;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.DialogFragment;

import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.android.material.textfield.TextInputEditText;
import com.google.android.material.textfield.TextInputLayout;
import com.skilllink.R;
import com.skilllink.util.SessionManager;

import java.util.regex.Pattern;

/**
 * Dialog for adding a new payment card
 */
public class AddCardDialog extends DialogFragment {

    public interface AddCardListener {
        void onCardAdded(CardDetails cardDetails);
        void onCardAddingCancelled();
    }

    public static class CardDetails {
        final String holderName;
        final String cardNumber;
        final String expiry;
        final String cvv;
        final String last4;

        CardDetails(String holderName, String cardNumber, String expiry, String cvv) {
            this.holderName = holderName;
            this.cardNumber = cardNumber.replaceAll("\\s", ""); // Remove spaces
            this.expiry = expiry;
            this.cvv = cvv;
            this.last4 = this.cardNumber.length() >= 4 ? 
                this.cardNumber.substring(this.cardNumber.length() - 4) : "****";
        }

        public String getMaskedNumber() {
            return "•••• " + last4;
        }
    }

    private AddCardListener listener;
    private SessionManager sessionManager;
    
    private TextInputLayout inputLayoutHolderName;
    private TextInputLayout inputLayoutCardNumber;
    private TextInputLayout inputLayoutExpiry;
    private TextInputLayout inputLayoutCvv;
    private TextInputEditText inputHolderName;
    private TextInputEditText inputCardNumber;
    private TextInputEditText inputExpiry;
    private TextInputEditText inputCvv;
    private Button buttonSaveCard;
    private Button buttonCancel;

    // Patterns for validation
    private static final Pattern CARD_NUMBER_PATTERN = Pattern.compile("^[0-9]{12,19}$");
    private static final Pattern EXPIRY_PATTERN = Pattern.compile("^(0[1-9]|1[0-2])/\\d{2}$");
    private static final Pattern CVV_PATTERN = Pattern.compile("^[0-9]{3,4}$");
    private static final Pattern HOLDER_NAME_PATTERN = Pattern.compile("^[a-zA-Z\\s]{2,50}$");

    public static AddCardDialog newInstance() {
        return new AddCardDialog();
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        sessionManager = new SessionManager(requireContext());
    }

    @NonNull
    @Override
    public Dialog onCreateDialog(@Nullable Bundle savedInstanceState) {
        LayoutInflater inflater = LayoutInflater.from(requireContext());
        View view = inflater.inflate(R.layout.dialog_add_card, null);
        
        bindViews(view);
        setupValidation();
        setupClickListeners();
        
        return new MaterialAlertDialogBuilder(requireContext())
                .setTitle(getString(R.string.service_booking_add_card_title))
                .setView(view)
                .create();
    }

    private void bindViews(View view) {
        inputLayoutHolderName = view.findViewById(R.id.inputLayoutHolderName);
        inputLayoutCardNumber = view.findViewById(R.id.inputLayoutCardNumber);
        inputLayoutExpiry = view.findViewById(R.id.inputLayoutExpiry);
        inputLayoutCvv = view.findViewById(R.id.inputLayoutCvv);
        
        inputHolderName = view.findViewById(R.id.inputHolderName);
        inputCardNumber = view.findViewById(R.id.inputCardNumber);
        inputExpiry = view.findViewById(R.id.inputExpiry);
        inputCvv = view.findViewById(R.id.inputCvv);
        
        buttonSaveCard = view.findViewById(R.id.buttonSaveCard);
        buttonCancel = view.findViewById(R.id.buttonCancel);
        
        // Set hints
        inputLayoutHolderName.setHint(getString(R.string.service_booking_add_card_holder));
        inputLayoutCardNumber.setHint(getString(R.string.service_booking_add_card_number));
        inputLayoutExpiry.setHint(getString(R.string.service_booking_add_card_expiry));
        inputLayoutCvv.setHint(getString(R.string.service_booking_add_card_cvv));
    }

    private void setupValidation() {
        // Card number formatting (add spaces every 4 digits)
        inputCardNumber.addTextChangedListener(new TextWatcher() {
            private boolean isFormatting = false;
            
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
            
            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {}
            
            @Override
            public void afterTextChanged(Editable s) {
                if (isFormatting) return;
                
                isFormatting = true;
                String input = s.toString().replaceAll("\\s", "");
                StringBuilder formatted = new StringBuilder();
                
                for (int i = 0; i < input.length(); i++) {
                    if (i > 0 && i % 4 == 0) {
                        formatted.append(" ");
                    }
                    formatted.append(input.charAt(i));
                }
                
                inputCardNumber.setText(formatted.toString());
                inputCardNumber.setSelection(formatted.length());
                isFormatting = false;
                
                validateCardNumber();
            }
        });
        
        // Expiry formatting (MM/YY)
        inputExpiry.addTextChangedListener(new TextWatcher() {
            private boolean isFormatting = false;
            
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
            
            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {}
            
            @Override
            public void afterTextChanged(Editable s) {
                if (isFormatting) return;
                
                isFormatting = true;
                String input = s.toString().replaceAll("/", "");
                StringBuilder formatted = new StringBuilder();
                
                if (input.length() >= 2) {
                    formatted.append(input.substring(0, 2));
                    formatted.append("/");
                    if (input.length() > 2) {
                        formatted.append(input.substring(2, Math.min(input.length(), 4)));
                    }
                } else {
                    formatted.append(input);
                }
                
                inputExpiry.setText(formatted.toString());
                inputExpiry.setSelection(formatted.length());
                isFormatting = false;
                
                validateExpiry();
            }
        });
        
        // Holder name validation
        inputHolderName.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
            
            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {}
            
            @Override
            public void afterTextChanged(Editable s) {
                validateHolderName();
            }
        });
        
        // CVV validation
        inputCvv.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
            
            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {}
            
            @Override
            public void afterTextChanged(Editable s) {
                validateCvv();
            }
        });
    }

    private void setupClickListeners() {
        buttonSaveCard.setOnClickListener(v -> validateAndSaveCard());
        buttonCancel.setOnClickListener(v -> {
            if (listener != null) {
                listener.onCardAddingCancelled();
            }
            dismiss();
        });
    }

    private void validateHolderName() {
        String holderName = inputHolderName.getText().toString().trim();
        if (TextUtils.isEmpty(holderName)) {
            inputLayoutHolderName.setError(getString(R.string.service_booking_add_card_error_holder));
        } else if (!HOLDER_NAME_PATTERN.matcher(holderName).matches()) {
            inputLayoutHolderName.setError("Enter a valid name (letters only)");
        } else {
            inputLayoutHolderName.setError(null);
        }
    }

    private void validateCardNumber() {
        String cardNumber = inputCardNumber.getText().toString().replaceAll("\\s", "");
        if (TextUtils.isEmpty(cardNumber)) {
            inputLayoutCardNumber.setError(getString(R.string.service_booking_add_card_error_number));
        } else if (!CARD_NUMBER_PATTERN.matcher(cardNumber).matches()) {
            inputLayoutCardNumber.setError("Enter a valid card number (12-19 digits)");
        } else if (!isValidLuhn(cardNumber)) {
            inputLayoutCardNumber.setError("Invalid card number");
        } else {
            inputLayoutCardNumber.setError(null);
        }
    }

    private void validateExpiry() {
        String expiry = inputExpiry.getText().toString();
        if (TextUtils.isEmpty(expiry)) {
            inputLayoutExpiry.setError(getString(R.string.service_booking_add_card_error_expiry));
        } else if (!EXPIRY_PATTERN.matcher(expiry).matches()) {
            inputLayoutExpiry.setError("Use MM/YY format");
        } else if (!isFutureExpiry(expiry)) {
            inputLayoutExpiry.setError("Card has expired");
        } else {
            inputLayoutExpiry.setError(null);
        }
    }

    private void validateCvv() {
        String cvv = inputCvv.getText().toString();
        if (TextUtils.isEmpty(cvv)) {
            inputLayoutCvv.setError(getString(R.string.service_booking_add_card_error_cvv));
        } else if (!CVV_PATTERN.matcher(cvv).matches()) {
            inputLayoutCvv.setError("Enter a valid CVV (3-4 digits)");
        } else {
            inputLayoutCvv.setError(null);
        }
    }

    private boolean isFormValid() {
        validateHolderName();
        validateCardNumber();
        validateExpiry();
        validateCvv();
        
        return inputLayoutHolderName.getError() == null &&
               inputLayoutCardNumber.getError() == null &&
               inputLayoutExpiry.getError() == null &&
               inputLayoutCvv.getError() == null;
    }

    private void validateAndSaveCard() {
        if (!isFormValid()) {
            return;
        }
        
        String holderName = inputHolderName.getText().toString().trim();
        String cardNumber = inputCardNumber.getText().toString();
        String expiry = inputExpiry.getText().toString();
        String cvv = inputCvv.getText().toString();
        
        CardDetails cardDetails = new CardDetails(holderName, cardNumber, expiry, cvv);
        
        // Save card to session (in real app, this would be encrypted storage)
        sessionManager.savePaymentCard(cardDetails.last4, holderName, expiry);
        
        if (listener != null) {
            listener.onCardAdded(cardDetails);
        }
        
        Toast.makeText(requireContext(), "Card added successfully", Toast.LENGTH_SHORT).show();
        dismiss();
    }

    // Luhn algorithm for card number validation
    private boolean isValidLuhn(String cardNumber) {
        int sum = 0;
        boolean alternate = false;
        
        for (int i = cardNumber.length() - 1; i >= 0; i--) {
            int digit = Character.getNumericValue(cardNumber.charAt(i));
            
            if (alternate) {
                digit *= 2;
                if (digit > 9) {
                    digit = (digit % 10) + 1;
                }
            }
            
            sum += digit;
            alternate = !alternate;
        }
        
        return sum % 10 == 0;
    }

    private boolean isFutureExpiry(String expiry) {
        try {
            String[] parts = expiry.split("/");
            int month = Integer.parseInt(parts[0]);
            int year = 2000 + Integer.parseInt(parts[1]); // Convert YY to YYYY
            
            java.util.Calendar cal = java.util.Calendar.getInstance();
            int currentMonth = cal.get(java.util.Calendar.MONTH) + 1; // Calendar months are 0-based
            int currentYear = cal.get(java.util.Calendar.YEAR);
            
            if (year < currentYear) {
                return false;
            } else if (year == currentYear && month < currentMonth) {
                return false;
            }
            
            return true;
        } catch (Exception e) {
            return false;
        }
    }

    public void setAddCardListener(@Nullable AddCardListener listener) {
        this.listener = listener;
    }
}
