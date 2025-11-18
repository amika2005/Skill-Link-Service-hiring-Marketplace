package com.skilllink.model;

public class Payment {
    private long paymentId;
    private long bookingId;
    private double amount;
    private String method; // cash, card, digital_wallet, etc.
    private String transactionId;
    private String status; // pending, completed, failed, refunded
    private String metadata; // JSON string for additional payment information

    // Constructors
    public Payment() {
    }

    public Payment(long bookingId, double amount, String method, String transactionId, 
                   String status, String metadata) {
        this.bookingId = bookingId;
        this.amount = amount;
        this.method = method;
        this.transactionId = transactionId;
        this.status = status;
        this.metadata = metadata;
    }

    // Getters and Setters
    public long getPaymentId() {
        return paymentId;
    }

    public void setPaymentId(long paymentId) {
        this.paymentId = paymentId;
    }

    public long getBookingId() {
        return bookingId;
    }

    public void setBookingId(long bookingId) {
        this.bookingId = bookingId;
    }

    public double getAmount() {
        return amount;
    }

    public void setAmount(double amount) {
        this.amount = amount;
    }

    public String getMethod() {
        return method;
    }

    public void setMethod(String method) {
        this.method = method;
    }

    public String getTransactionId() {
        return transactionId;
    }

    public void setTransactionId(String transactionId) {
        this.transactionId = transactionId;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public String getMetadata() {
        return metadata;
    }

    public void setMetadata(String metadata) {
        this.metadata = metadata;
    }
}