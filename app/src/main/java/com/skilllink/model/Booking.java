package com.skilllink.model;

public class Booking {
    private long bookingId;
    private long customerId;
    private long workerId;
    private long serviceId;
    private String status; // pending, confirmed, in_progress, completed, cancelled
    private String scheduledTime;
    private double locationLat;
    private double locationLng;
    private double totalAmount;
    private String paymentStatus; // pending, completed, failed
    private String trackingData;

    // Constructors
    public Booking() {
    }

    public Booking(long customerId, long workerId, long serviceId, String status,
                   String scheduledTime, double locationLat, double locationLng,
                   double totalAmount, String paymentStatus, String trackingData) {
        this.customerId = customerId;
        this.workerId = workerId;
        this.serviceId = serviceId;
        this.status = status;
        this.scheduledTime = scheduledTime;
        this.locationLat = locationLat;
        this.locationLng = locationLng;
        this.totalAmount = totalAmount;
        this.paymentStatus = paymentStatus;
        this.trackingData = trackingData;
    }

    // Getters and Setters
    public long getBookingId() {
        return bookingId;
    }

    public void setBookingId(long bookingId) {
        this.bookingId = bookingId;
    }

    public long getCustomerId() {
        return customerId;
    }

    public void setCustomerId(long customerId) {
        this.customerId = customerId;
    }

    public long getWorkerId() {
        return workerId;
    }

    public void setWorkerId(long workerId) {
        this.workerId = workerId;
    }

    public long getServiceId() {
        return serviceId;
    }

    public void setServiceId(long serviceId) {
        this.serviceId = serviceId;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public String getScheduledTime() {
        return scheduledTime;
    }

    public void setScheduledTime(String scheduledTime) {
        this.scheduledTime = scheduledTime;
    }

    public double getLocationLat() {
        return locationLat;
    }

    public void setLocationLat(double locationLat) {
        this.locationLat = locationLat;
    }

    public double getLocationLng() {
        return locationLng;
    }

    public void setLocationLng(double locationLng) {
        this.locationLng = locationLng;
    }

    public double getTotalAmount() {
        return totalAmount;
    }

    public void setTotalAmount(double totalAmount) {
        this.totalAmount = totalAmount;
    }

    public String getPaymentStatus() {
        return paymentStatus;
    }

    public void setPaymentStatus(String paymentStatus) {
        this.paymentStatus = paymentStatus;
    }

    public String getTrackingData() {
        return trackingData;
    }

    public void setTrackingData(String trackingData) {
        this.trackingData = trackingData;
    }
}