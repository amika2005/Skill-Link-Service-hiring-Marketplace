package com.skilllink.ui.user.bookings;

import android.content.Intent;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.ActivityResultCallback;
import androidx.activity.result.ActivityResult;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;

import com.google.android.material.appbar.MaterialToolbar;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.card.MaterialCardView;
import com.google.android.material.datepicker.MaterialDatePicker;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.android.material.textfield.TextInputEditText;
import com.google.android.material.textfield.TextInputLayout;
import com.google.android.material.timepicker.MaterialTimePicker;
import com.google.android.material.timepicker.TimeFormat;
import com.skilllink.R;
import com.skilllink.model.UserBooking;
import com.skilllink.model.WorkerJobRequest;
import com.skilllink.model.WorkerService;
import com.skilllink.ui.payments.AddCardDialog;
import com.skilllink.ui.payments.FakeXmlPaymentActivity;
import com.skilllink.ui.payments.PaymentGatewayManager;
import com.skilllink.util.ImageLoader;
import com.skilllink.util.ServiceCategoryRegistry;
import com.skilllink.util.SessionManager;

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class ServiceBookingActivity extends AppCompatActivity {

    public static final String EXTRA_SERVICE_ID = "extra_service_id";
    private final SimpleDateFormat dateFormatter = new SimpleDateFormat("dd MMMM yyyy", Locale.getDefault());
    private final SimpleDateFormat timeFormatter = new SimpleDateFormat("h:mm a", Locale.getDefault());

    private SessionManager sessionManager;
    private WorkerService service;

    private ImageView imageHero;
    private ImageView imageSummary;
    private TextView textSummaryName;
    private TextView textSummaryCategory;
    private TextView textSummaryPrice;
    private TextView textHighlights;
    private TextView textHeroRating;
    private TextView textReviewPrice;
    private TextView textBottomPrice;
    private TextInputLayout inputLayoutDate;
    private TextInputLayout inputLayoutTime;
    private TextInputEditText inputDate;
    private TextInputEditText inputTime;
    private TextInputEditText inputLocation;
    private TextInputEditText inputNotes;
    private MaterialCardView cardPaymentCash;
    private MaterialCardView cardPaymentCard;
    private MaterialCardView cardPaymentOnline;
    private ImageView imagePaymentCashCheck;
    private ImageView imagePaymentCardCheck;
    private ImageView imagePaymentOnlineCheck;
    private MaterialButton buttonCostBreakdown;
    private View buttonManageCards;
    private MaterialButton buttonConfirm;

    private PaymentSelection selectedPayment = PaymentSelection.ONLINE;

    private Long selectedDateMillis;
    private int selectedHour = -1;
    private int selectedMinute = -1;
    private boolean isProcessingPayment;
    private boolean supportsOnlinePayment;
    private UserBooking pendingBooking;
    private ActivityResultLauncher<Intent> paymentLauncher;

    private enum PaymentSelection {
        CASH,
        CARD,
        ONLINE
    }

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_service_booking);

        sessionManager = new SessionManager(this);
        paymentLauncher = registerForActivityResult(
                new ActivityResultContracts.StartActivityForResult(),
                this::handlePaymentResult
        );

        MaterialToolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setTitle(R.string.service_booking_title);
        }
        toolbar.setNavigationOnClickListener(v -> finish());

        bindViews();

        String serviceId = getIntent().getStringExtra(EXTRA_SERVICE_ID);
        service = sessionManager.findWorkerServiceById(serviceId);

        if (service == null) {
            handleMissingService();
            return;
        }

        populateSummary();
        setupInteraction();
    }

    private void bindViews() {
        imageHero = findViewById(R.id.imageHero);
        imageSummary = findViewById(R.id.imageSummary);
        textSummaryName = findViewById(R.id.textSummaryName);
        textSummaryCategory = findViewById(R.id.textSummaryCategory);
        textSummaryPrice = findViewById(R.id.textSummaryPrice);
        textHighlights = findViewById(R.id.textHighlights);
        textHeroRating = findViewById(R.id.textHeroRating);
        textReviewPrice = findViewById(R.id.textReviewPrice);
        textBottomPrice = findViewById(R.id.textBottomPrice);
        inputLayoutDate = findViewById(R.id.inputLayoutDate);
        inputLayoutTime = findViewById(R.id.inputLayoutTime);
        inputDate = findViewById(R.id.inputDate);
        inputTime = findViewById(R.id.inputTime);
        inputLocation = findViewById(R.id.inputLocation);
        inputNotes = findViewById(R.id.inputNotes);
        cardPaymentCash = findViewById(R.id.cardPaymentCash);
        cardPaymentCard = findViewById(R.id.cardPaymentCard);
        cardPaymentOnline = findViewById(R.id.cardPaymentOnline);
        imagePaymentCashCheck = findViewById(R.id.imagePaymentCashCheck);
        imagePaymentCardCheck = findViewById(R.id.imagePaymentCardCheck);
        imagePaymentOnlineCheck = findViewById(R.id.imagePaymentOnlineCheck);
        buttonCostBreakdown = findViewById(R.id.buttonCostBreakdown);
        buttonManageCards = findViewById(R.id.buttonManageCards);
        buttonConfirm = findViewById(R.id.buttonConfirm);
    }

    private void populateSummary() {
        textSummaryName.setText(service.getName());
        ServiceCategoryRegistry.Category category = ServiceCategoryRegistry.resolve(service.getCategory());
        String displayCategory = ServiceCategoryRegistry.getDisplayNameOrDefault(service.getCategory());
        textSummaryCategory.setText(displayCategory);

        String priceDisplay;
        if (TextUtils.isEmpty(service.getPriceValue())) {
            priceDisplay = getString(R.string.worker_service_detail_custom_format, getString(R.string.home_service_price_custom_label));
        } else if (service.isHourlyPricing()) {
            priceDisplay = getString(R.string.worker_service_detail_hourly_format, service.getPriceValue());
        } else {
            priceDisplay = getString(R.string.worker_service_detail_custom_format, service.getPriceValue());
        }

        textSummaryPrice.setText(priceDisplay);
        textReviewPrice.setText(priceDisplay);
        if (textBottomPrice != null) {
            textBottomPrice.setText(priceDisplay);
        }
        if (textHeroRating != null) {
            int jobsCount = category != null ? Math.max(category.getDefaultWorkersCount(), 10) : 24;
            String heroMeta = getString(R.string.home_service_meta_format, 4.9f, jobsCount);
            textHeroRating.setText(heroMeta);
        }

        ImageLoader.loadUriInto(this, imageSummary, service.getImageUri(), R.drawable.ic_service_manage, R.color.primary_color);
        if (imageHero != null) {
            ImageLoader.loadUriInto(this, imageHero, service.getImageUri(), R.drawable.ic_service_manage, R.color.primary_color);
        }

        if (textHighlights != null) {
            if (!TextUtils.isEmpty(service.getBio())) {
                textHighlights.setText(service.getBio());
            } else {
                textHighlights.setText(R.string.worker_service_detail_no_bio);
            }
        }

        String savedLocation = sessionManager.getUserLocation();
        if (!TextUtils.isEmpty(savedLocation)) {
            inputLocation.setText(savedLocation);
        }

        supportsOnlinePayment = true;
        applyOnlinePaymentAvailabilityVisuals();

        if (cardPaymentOnline != null) {
            selectPaymentCard(cardPaymentOnline, PaymentSelection.ONLINE);
        } else if (cardPaymentCard != null) {
            selectPaymentCard(cardPaymentCard, PaymentSelection.CARD);
        } else if (sessionManager.isPaymentCashEnabled() && cardPaymentCash != null) {
            selectPaymentCard(cardPaymentCash, PaymentSelection.CASH);
        } else {
            selectedPayment = PaymentSelection.ONLINE;
        }
    }

    private void setupInteraction() {
        View.OnClickListener dateClickListener = v -> showDatePicker();
        inputDate.setOnClickListener(dateClickListener);
        inputLayoutDate.setEndIconOnClickListener(dateClickListener);

        View.OnClickListener timeClickListener = v -> showTimePicker();
        inputTime.setOnClickListener(timeClickListener);
        inputLayoutTime.setEndIconOnClickListener(timeClickListener);

        buttonConfirm.setOnClickListener(v -> confirmBooking());
        if (cardPaymentCash != null) {
            cardPaymentCash.setOnClickListener(v -> selectPaymentCard(cardPaymentCash, PaymentSelection.CASH));
        }
        if (cardPaymentCard != null) {
            cardPaymentCard.setOnClickListener(v -> selectPaymentCard(cardPaymentCard, PaymentSelection.CARD));
        }
        if (cardPaymentOnline != null) {
            cardPaymentOnline.setOnClickListener(v -> selectPaymentCard(cardPaymentOnline, PaymentSelection.ONLINE));
        }
        if (buttonCostBreakdown != null) {
            buttonCostBreakdown.setOnClickListener(v -> showCostBreakdownDialog(textSummaryPrice.getText()));
        }
        if (buttonManageCards != null) {
            buttonManageCards.setOnClickListener(v -> showManageCardDialog());
        }
    }

    private void showDatePicker() {
        MaterialDatePicker<Long> picker = MaterialDatePicker.Builder.datePicker()
                .setTitleText(R.string.service_booking_date_label)
                .setSelection(selectedDateMillis != null ? selectedDateMillis : MaterialDatePicker.todayInUtcMilliseconds())
                .build();

        picker.addOnPositiveButtonClickListener(selection -> {
            selectedDateMillis = selection;
            Date date = new Date(selection);
            inputDate.setText(dateFormatter.format(date));
            inputLayoutDate.setError(null);
        });

        picker.show(getSupportFragmentManager(), "booking_date");
    }

    private void showTimePicker() {
        int hour = selectedHour >= 0 ? selectedHour : 9;
        int minute = selectedMinute >= 0 ? selectedMinute : 0;

        MaterialTimePicker picker = new MaterialTimePicker.Builder()
                .setTimeFormat(TimeFormat.CLOCK_12H)
                .setHour(hour)
                .setMinute(minute)
                .setTitleText(R.string.service_booking_time_label)
                .build();

        picker.addOnPositiveButtonClickListener(v -> {
            selectedHour = picker.getHour();
            selectedMinute = picker.getMinute();

            Calendar calendar = Calendar.getInstance();
            calendar.set(Calendar.HOUR_OF_DAY, selectedHour);
            calendar.set(Calendar.MINUTE, selectedMinute);

            inputTime.setText(timeFormatter.format(calendar.getTime()));
            inputLayoutTime.setError(null);
        });

        picker.show(getSupportFragmentManager(), "booking_time");
    }

    private void confirmBooking() {
        CharSequence dateText = inputDate.getText();
        CharSequence timeText = inputTime.getText();

        if (TextUtils.isEmpty(dateText)) {
            inputLayoutDate.setError(getString(R.string.service_booking_missing_date));
            return;
        }

        if (TextUtils.isEmpty(timeText)) {
            inputLayoutTime.setError(getString(R.string.service_booking_missing_time));
            return;
        }

        String location = safeText(inputLocation);
        String notes = safeText(inputNotes);
        PaymentSelection paymentSelection = selectedPayment;
        String paymentMethod = resolvePaymentMethod(paymentSelection);

        // Fake gateway supports all bookings; no availability check needed

        UserBooking booking = UserBooking.create(
                service.getId(),
                service.getName(),
                service.getCategory(),
                dateText.toString(),
                timeText.toString(),
                location,
                notes,
                paymentMethod,
                textSummaryPrice.getText().toString(),
                service.getImageUri()
        );
        if (paymentSelection == PaymentSelection.CASH) {
            finalizeBooking(booking);
            return;
        }

        pendingBooking = booking;
        beginFakeXmlPayment(booking);
    }

    private void beginFakeXmlPayment(UserBooking booking) {
        if (isProcessingPayment) {
            return;
        }


        CharSequence amountDisplay = textSummaryPrice.getText();
        pendingBooking = booking;
        setProcessingPayment(true);

        Intent intent = FakeXmlPaymentActivity.createIntent(
                this,
                amountDisplay,
                service != null ? service.getName() : null);
        paymentLauncher.launch(intent);
    }

    private void handlePaymentResult(ActivityResult result) {
        if (result == null) {
            setProcessingPayment(false);
            return;
        }
        boolean success = result.getResultCode() == RESULT_OK
                && result.getData() != null
                && result.getData().getBooleanExtra(FakeXmlPaymentActivity.EXTRA_SUCCESS, false);
        if (success) {
            if (pendingBooking != null) {
                finalizeBooking(pendingBooking);
                pendingBooking = null;
            }
        } else {
            Toast.makeText(this, R.string.service_booking_payment_cancelled, Toast.LENGTH_SHORT).show();
        }
        setProcessingPayment(false);
    }

    private void finalizeBooking(UserBooking booking) {
        sessionManager.addUserBooking(booking);
        
        // Create job request targeted to the specific service owner
        WorkerJobRequest jobRequest = WorkerJobRequest.createFromBooking(
                booking,
                sessionManager.getUserName(),
                sessionManager.getUserPhone(),
                service.getOwnerId(),      // Target the specific service owner
                service.getOwnerName(),    // Include service owner's name
                service.getOwnerEmail()    // Include service owner's email
        );
        sessionManager.addWorkerJobRequest(jobRequest);
        showBookingSuccessDialog();
    }

    private void showCostBreakdownDialog(CharSequence priceDisplay) {
        String priceText = priceDisplay != null ? priceDisplay.toString() : getString(R.string.home_service_price_custom_label);
        new MaterialAlertDialogBuilder(this)
                .setTitle(R.string.service_booking_breakdown)
                .setMessage(getString(R.string.service_booking_breakdown_body, priceText))
                .setPositiveButton(android.R.string.ok, null)
                .show();
    }

    private void showManageCardDialog() {
        new MaterialAlertDialogBuilder(this)
                .setTitle(R.string.service_booking_payment_card)
                .setItems(new CharSequence[]{
                        getString(R.string.service_booking_card_option_default),
                        getString(R.string.service_booking_card_option_new)
                }, (dialog, which) -> {
                    if (which == 1) {
                        showAddCardDialog();
                    }
                })
                .setNegativeButton(android.R.string.cancel, null)
                .show();
    }

    private void showAddCardDialog() {
        AddCardDialog addCardDialog = AddCardDialog.newInstance();
        addCardDialog.setAddCardListener(new AddCardDialog.AddCardListener() {
            @Override
            public void onCardAdded(AddCardDialog.CardDetails cardDetails) {
                // Update the UI to show the new card
                updatePaymentCardDisplay();
                Toast.makeText(ServiceBookingActivity.this, 
                    "Card added: " + cardDetails.getMaskedNumber(), 
                    Toast.LENGTH_SHORT).show();
            }

            @Override
            public void onCardAddingCancelled() {
                // User cancelled, no action needed
            }
        });
        addCardDialog.show(getSupportFragmentManager(), "add_card_dialog");
    }

    private void updatePaymentCardDisplay() {
        SessionManager.SavedCard primaryCard = sessionManager.getPrimarySavedCard();
        if (primaryCard != null && cardPaymentCard != null) {
            // Update the card display to show the new card
            TextView cardSubtitle = cardPaymentCard.findViewById(R.id.textSavedCardLabel);
            if (cardSubtitle != null) {
                cardSubtitle.setText("•••• " + primaryCard.last4);
            }
        }
    }

    private void applyOnlinePaymentAvailabilityVisuals() {
        if (cardPaymentOnline == null) {
            return;
        }
        cardPaymentOnline.setAlpha(1f);
    }

    private boolean isOnlinePayment(CharSequence method) {
        if (method == null) {
            return false;
        }
        return getString(R.string.service_booking_payment_online).contentEquals(method);
    }

    private void showOnlinePaymentUnavailableDialog() {
        new MaterialAlertDialogBuilder(this)
                .setTitle(R.string.service_booking_payment_unavailable_title)
                .setMessage(R.string.service_booking_payment_unavailable_body)
                .setPositiveButton(android.R.string.ok, null)
                .show();
    }

    private void selectPaymentCard(@Nullable MaterialCardView card, PaymentSelection selection) {
        MaterialCardView[] cards = new MaterialCardView[]{cardPaymentCash, cardPaymentCard, cardPaymentOnline};
        ImageView[] icons = new ImageView[]{imagePaymentCashCheck, imagePaymentCardCheck, imagePaymentOnlineCheck};
        for (int i = 0; i < cards.length; i++) {
            MaterialCardView item = cards[i];
            if (item == null) continue;
            boolean isSelected = item == card;
            ImageView icon = i < icons.length ? icons[i] : null;
            updatePaymentCardVisuals(item, icon, isSelected);
        }
        selectedPayment = selection;
    }


    private void updatePaymentCardVisuals(@NonNull MaterialCardView card, @Nullable ImageView checkIcon, boolean selected) {
        int strokeColor = ContextCompat.getColor(this, selected ? R.color.primary_color : R.color.primary_soft_stroke);
        int backgroundColor = ContextCompat.getColor(this, selected ? R.color.primary_chip_surface : android.R.color.white);
        card.setStrokeColor(strokeColor);
        card.setStrokeWidth(selected ? dpToPx(2) : dpToPx(1));
        card.setCardBackgroundColor(backgroundColor);
        if (checkIcon != null) {
            checkIcon.setVisibility(selected ? View.VISIBLE : View.GONE);
        }
    }

    private void showBookingSuccessDialog() {
        new MaterialAlertDialogBuilder(this)
                .setTitle(R.string.service_booking_success_title)
                .setMessage(R.string.service_booking_success_message)
                .setPositiveButton(R.string.service_booking_go_to_bookings, (dialog, which) -> {
                    dialog.dismiss();
                    finish();
                })
                .setNegativeButton(android.R.string.ok, (dialog, which) -> dialog.dismiss())
                .show();
    }

    private String resolvePaymentMethod(PaymentSelection selection) {
        switch (selection) {
            case CASH:
                return getString(R.string.service_booking_payment_cash);
            case CARD:
                return getString(R.string.service_booking_payment_card);
            case ONLINE:
            default:
                return getString(R.string.service_booking_payment_online);
        }
    }

    private void setProcessingPayment(boolean processing) {
        isProcessingPayment = processing;
        buttonConfirm.setEnabled(!processing);
        buttonConfirm.setAlpha(processing ? 0.6f : 1f);
        MaterialCardView[] cards = new MaterialCardView[]{cardPaymentCash, cardPaymentCard, cardPaymentOnline};
        for (MaterialCardView card : cards) {
            if (card == null) continue;
            card.setEnabled(!processing);
            card.setAlpha(processing ? 0.7f : 1f);
        }
    }

    private String safeText(TextInputEditText editText) {
        return editText.getText() != null ? editText.getText().toString().trim() : "";
    }

    private void handleMissingService() {
        List<WorkerService> services = sessionManager.getWorkerServices();
        if (services.isEmpty()) {
            Toast.makeText(this, R.string.worker_service_detail_unavailable, Toast.LENGTH_LONG).show();
            finish();
        } else {
            service = services.get(0);
            populateSummary();
            setupInteraction();
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
    }

    private int dpToPx(int dp) {
        return Math.round(getResources().getDisplayMetrics().density * dp);
    }

}
