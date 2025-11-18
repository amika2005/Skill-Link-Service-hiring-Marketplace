package com.skilllink.model;

public class ServiceCategory {
    private long categoryId;
    private String categoryName;
    private String categoryIcon;
    private boolean isActive;

    // Constructors
    public ServiceCategory() {
    }

    public ServiceCategory(String categoryName, String categoryIcon, boolean isActive) {
        this.categoryName = categoryName;
        this.categoryIcon = categoryIcon;
        this.isActive = isActive;
    }

    // Getters and Setters
    public long getCategoryId() {
        return categoryId;
    }

    public void setCategoryId(long categoryId) {
        this.categoryId = categoryId;
    }

    public String getCategoryName() {
        return categoryName;
    }

    public void setCategoryName(String categoryName) {
        this.categoryName = categoryName;
    }

    public String getCategoryIcon() {
        return categoryIcon;
    }

    public void setCategoryIcon(String categoryIcon) {
        this.categoryIcon = categoryIcon;
    }

    public boolean isActive() {
        return isActive;
    }

    public void setActive(boolean active) {
        isActive = active;
    }
}