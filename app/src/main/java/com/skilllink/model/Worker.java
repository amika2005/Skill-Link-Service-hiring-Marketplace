package com.skilllink.model;

public class Worker {
    private long workerId;
    private long userId;
    private int experienceYears;
    private double ratingAverage;
    private int totalJobs;
    private String verificationDocuments;
    private String availabilityStatus;
    private double locationLat;
    private double locationLng;
    private double serviceAreaRadius;

    // Constructors
    public Worker() {
    }

    public Worker(long userId, int experienceYears, double ratingAverage, int totalJobs,
                  String verificationDocuments, String availabilityStatus, 
                  double locationLat, double locationLng, double serviceAreaRadius) {
        this.userId = userId;
        this.experienceYears = experienceYears;
        this.ratingAverage = ratingAverage;
        this.totalJobs = totalJobs;
        this.verificationDocuments = verificationDocuments;
        this.availabilityStatus = availabilityStatus;
        this.locationLat = locationLat;
        this.locationLng = locationLng;
        this.serviceAreaRadius = serviceAreaRadius;
    }

    // Getters and Setters
    public long getWorkerId() {
        return workerId;
    }

    public void setWorkerId(long workerId) {
        this.workerId = workerId;
    }

    public long getUserId() {
        return userId;
    }

    public void setUserId(long userId) {
        this.userId = userId;
    }

    public int getExperienceYears() {
        return experienceYears;
    }

    public void setExperienceYears(int experienceYears) {
        this.experienceYears = experienceYears;
    }

    public double getRatingAverage() {
        return ratingAverage;
    }

    public void setRatingAverage(double ratingAverage) {
        this.ratingAverage = ratingAverage;
    }

    public int getTotalJobs() {
        return totalJobs;
    }

    public void setTotalJobs(int totalJobs) {
        this.totalJobs = totalJobs;
    }

    public String getVerificationDocuments() {
        return verificationDocuments;
    }

    public void setVerificationDocuments(String verificationDocuments) {
        this.verificationDocuments = verificationDocuments;
    }

    public String getAvailabilityStatus() {
        return availabilityStatus;
    }

    public void setAvailabilityStatus(String availabilityStatus) {
        this.availabilityStatus = availabilityStatus;
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

    public double getServiceAreaRadius() {
        return serviceAreaRadius;
    }

    public void setServiceAreaRadius(double serviceAreaRadius) {
        this.serviceAreaRadius = serviceAreaRadius;
    }
}