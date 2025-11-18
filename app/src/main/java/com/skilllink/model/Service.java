package com.skilllink.model;

public class Service {
    private long serviceId;
    private String category;
    private String subCategory;
    private String serviceName;
    private String description;
    private double basePrice;
    private String priceType; // fixed/hourly
    private int averageDuration; // in minutes
    private String iconUrl;

    // Constructors
    public Service() {
    }

    public Service(String category, String subCategory, String serviceName, String description,
                   double basePrice, String priceType, int averageDuration, String iconUrl) {
        this.category = category;
        this.subCategory = subCategory;
        this.serviceName = serviceName;
        this.description = description;
        this.basePrice = basePrice;
        this.priceType = priceType;
        this.averageDuration = averageDuration;
        this.iconUrl = iconUrl;
    }

    // Getters and Setters
    public long getServiceId() {
        return serviceId;
    }

    public void setServiceId(long serviceId) {
        this.serviceId = serviceId;
    }

    public String getCategory() {
        return category;
    }

    public void setCategory(String category) {
        this.category = category;
    }

    public String getSubCategory() {
        return subCategory;
    }

    public void setSubCategory(String subCategory) {
        this.subCategory = subCategory;
    }

    public String getServiceName() {
        return serviceName;
    }

    public void setServiceName(String serviceName) {
        this.serviceName = serviceName;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public double getBasePrice() {
        return basePrice;
    }

    public void setBasePrice(double basePrice) {
        this.basePrice = basePrice;
    }

    public String getPriceType() {
        return priceType;
    }

    public void setPriceType(String priceType) {
        this.priceType = priceType;
    }

    public int getAverageDuration() {
        return averageDuration;
    }

    public void setAverageDuration(int averageDuration) {
        this.averageDuration = averageDuration;
    }

    public String getIconUrl() {
        return iconUrl;
    }

    public void setIconUrl(String iconUrl) {
        this.iconUrl = iconUrl;
    }
}