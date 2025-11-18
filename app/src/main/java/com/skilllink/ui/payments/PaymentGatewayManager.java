package com.skilllink.ui.payments;

import android.content.Context;
import android.content.Intent;

import com.skilllink.model.Payment;
import com.skilllink.model.UserBooking;
import com.skilllink.util.SessionManager;

import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;

/**
 * Payment Gateway Manager
 * Handles payment processing for different payment methods including the mock XML Pay gateway
 */
public class PaymentGatewayManager {

    public static final String PAYMENT_METHOD_CASH = "cash";
    public static final String PAYMENT_METHOD_CARD = "card";
    public static final String PAYMENT_METHOD_ONLINE = "online";
    public static final String PAYMENT_METHOD_XML_PAY = "xml_pay";

    private static final String GATEWAY_NAME = "SkillLink XML Pay";
    private static final String GATEWAY_VERSION = "1.0.0";

    private final Context context;
    private final SessionManager sessionManager;

    public interface PaymentCallback {
        void onPaymentSuccess(PaymentResult result);
        void onPaymentFailure(String errorMessage, PaymentError errorType);
        void onPaymentCancelled();
    }

    public enum PaymentError {
        NETWORK_ERROR,
        INVALID_CARD,
        INSUFFICIENT_FUNDS,
        GATEWAY_ERROR,
        USER_CANCELLED,
        TIMEOUT
    }

    public static class PaymentResult {
        private final boolean success;
        private final String transactionId;
        private final String reference;
        private final String gateway;
        private final String maskedCard;
        private final double amount;
        private final long timestamp;
        private final String status;

        public PaymentResult(boolean success, String transactionId, String reference, 
                           String gateway, String maskedCard, double amount, String status) {
            this.success = success;
            this.transactionId = transactionId;
            this.reference = reference;
            this.gateway = gateway;
            this.maskedCard = maskedCard;
            this.amount = amount;
            this.timestamp = System.currentTimeMillis();
            this.status = status;
        }

        public boolean isSuccess() { return success; }
        public String getTransactionId() { return transactionId; }
        public String getReference() { return reference; }
        public String getGateway() { return gateway; }
        public String getMaskedCard() { return maskedCard; }
        public double getAmount() { return amount; }
        public long getTimestamp() { return timestamp; }
        public String getStatus() { return status; }

        public Payment toPaymentModel(long bookingId) {
            Payment payment = new Payment();
            payment.setBookingId(bookingId);
            payment.setAmount(amount);
            payment.setMethod(gateway);
            payment.setTransactionId(transactionId);
            payment.setStatus(status);
            payment.setMetadata(String.format("{\"reference\":\"%s\",\"maskedCard\":\"%s\",\"gateway\":\"%s\"}", 
                reference, maskedCard, gateway));
            return payment;
        }
    }

    public PaymentGatewayManager(Context context) {
        this.context = context.getApplicationContext();
        this.sessionManager = new SessionManager(this.context);
    }

    /**
     * Process payment using the specified method
     */
    public void processPayment(UserBooking booking, String paymentMethod, 
                              PaymentCallback callback) {
        switch (paymentMethod.toLowerCase()) {
            case PAYMENT_METHOD_CASH:
                processCashPayment(booking, callback);
                break;
            case PAYMENT_METHOD_CARD:
                processCardPayment(booking, callback);
                break;
            case PAYMENT_METHOD_ONLINE:
            case PAYMENT_METHOD_XML_PAY:
                processXmlPayPayment(booking, callback);
                break;
            default:
                if (callback != null) {
                    callback.onPaymentFailure("Unsupported payment method", PaymentError.GATEWAY_ERROR);
                }
        }
    }

    /**
     * Process cash payment (immediate success)
     */
    private void processCashPayment(UserBooking booking, PaymentCallback callback) {
        // Cash payments are always successful in this demo
        PaymentResult result = new PaymentResult(
            true,
            generateTransactionId(),
            generateReference(),
            "Cash",
            "Cash on Delivery",
            parseAmount(booking.getPriceDisplay()),
            "completed"
        );

        if (callback != null) {
            callback.onPaymentSuccess(result);
        }
    }

    /**
     * Process saved card payment (simulated)
     */
    private void processCardPayment(UserBooking booking, PaymentCallback callback) {
        // Simulate card processing
        simulateAsyncPayment(booking, "Saved Card", "•••• " + getLast4SavedCard(), callback);
    }

    /**
     * Process XML Pay payment (launch payment activity)
     */
    private void processXmlPayPayment(UserBooking booking, PaymentCallback callback) {
        // This will be handled by the FakeXmlPaymentActivity
        // The callback will be called from the activity result
        Intent intent = FakeXmlPaymentActivity.createIntent(
            context,
            booking.getPriceDisplay(),
            booking.getServiceName()
        );
        
        // Note: The actual launching should be done from the activity
        // This method just prepares the intent
        if (callback != null) {
            // Store callback for later use by the activity
            PaymentCallbackRegistry.setCallback(callback);
        }
    }

    /**
     * Simulate async payment processing with realistic delays
     */
    private void simulateAsyncPayment(UserBooking booking, String method, 
                                    String maskedCard, PaymentCallback callback) {
        CompletableFuture
            .supplyAsync(() -> {
                // Simulate network delay
                try {
                    TimeUnit.SECONDS.sleep(2 + (int)(Math.random() * 3));
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                }
                
                // Simulate 95% success rate
                if (Math.random() < 0.95) {
                    return new PaymentResult(
                        true,
                        generateTransactionId(),
                        generateReference(),
                        method,
                        maskedCard,
                        parseAmount(booking.getPriceDisplay()),
                        "completed"
                    );
                } else {
                    // Simulate random failure
                    PaymentError[] errors = {
                        PaymentError.NETWORK_ERROR,
                        PaymentError.GATEWAY_ERROR,
                        PaymentError.INSUFFICIENT_FUNDS
                    };
                    PaymentError error = errors[(int)(Math.random() * errors.length)];
                    throw new PaymentProcessingException("Payment failed", error);
                }
            })
            .thenAccept(result -> {
                if (callback != null) {
                    callback.onPaymentSuccess(result);
                }
            })
            .exceptionally(throwable -> {
                if (callback != null) {
                    PaymentProcessingException ex = (PaymentProcessingException) throwable.getCause();
                    if (ex != null) {
                        callback.onPaymentFailure(ex.getMessage(), ex.getErrorType());
                    } else {
                        callback.onPaymentFailure("Unknown error occurred", PaymentError.GATEWAY_ERROR);
                    }
                }
                return null;
            });
    }

    /**
     * Generate unique transaction ID
     */
    private String generateTransactionId() {
        return "TXN" + System.currentTimeMillis() + UUID.randomUUID().toString().substring(0, 8).toUpperCase();
    }

    /**
     * Generate unique reference number
     */
    private String generateReference() {
        return "XML" + UUID.randomUUID().toString().replace("-", "").substring(0, 8).toUpperCase();
    }

    /**
     * Parse amount from string
     */
    private double parseAmount(String amountString) {
        if (amountString == null || amountString.trim().isEmpty()) {
            return 0.0;
        }
        
        try {
            // Remove non-numeric characters except decimal point
            String cleanAmount = amountString.replaceAll("[^0-9.]", "");
            return Double.parseDouble(cleanAmount);
        } catch (NumberFormatException e) {
            return 0.0;
        }
    }

    /**
     * Get last 4 digits of saved card
     */
    private String getLast4SavedCard() {
        // In a real app, this would come from secure storage
        return "4242";
    }

    /**
     * Check if payment method is available
     */
    public boolean isPaymentMethodAvailable(String paymentMethod) {
        switch (paymentMethod.toLowerCase()) {
            case PAYMENT_METHOD_CASH:
                return sessionManager.isPaymentCashEnabled();
            case PAYMENT_METHOD_CARD:
                return true; // Always available in demo
            case PAYMENT_METHOD_ONLINE:
            case PAYMENT_METHOD_XML_PAY:
                return true; // XML Pay is always available
            default:
                return false;
        }
    }

    /**
     * Get display name for payment method
     */
    public String getPaymentMethodDisplayName(String paymentMethod) {
        switch (paymentMethod.toLowerCase()) {
            case PAYMENT_METHOD_CASH:
                return "Cash on Delivery";
            case PAYMENT_METHOD_CARD:
                return "Saved Card";
            case PAYMENT_METHOD_ONLINE:
            case PAYMENT_METHOD_XML_PAY:
                return GATEWAY_NAME;
            default:
                return "Unknown";
        }
    }

    /**
     * Validate payment method for service
     */
    public boolean validatePaymentMethod(UserBooking booking, String paymentMethod) {
        // All payment methods are valid for all services in this demo
        return isPaymentMethodAvailable(paymentMethod);
    }

    /**
     * Registry for storing payment callbacks (simplified for demo)
     */
    public static class PaymentCallbackRegistry {
        private static PaymentCallback currentCallback;

        public static void setCallback(PaymentCallback callback) {
            currentCallback = callback;
        }

        public static PaymentCallback getCallback() {
            PaymentCallback callback = currentCallback;
            currentCallback = null; // Clear after retrieval
            return callback;
        }

        public static void clearCallback() {
            currentCallback = null;
        }
    }

    /**
     * Custom exception for payment processing errors
     */
    private static class PaymentProcessingException extends RuntimeException {
        private final PaymentError errorType;

        public PaymentProcessingException(String message, PaymentError errorType) {
            super(message);
            this.errorType = errorType;
        }

        public PaymentError getErrorType() {
            return errorType;
        }
    }

    /**
     * Get gateway information
     */
    public String getGatewayName() {
        return GATEWAY_NAME;
    }

    public String getGatewayVersion() {
        return GATEWAY_VERSION;
    }

    /**
     * Check if gateway is available
     */
    public boolean isGatewayAvailable() {
        return true; // Always available in demo
    }
}
