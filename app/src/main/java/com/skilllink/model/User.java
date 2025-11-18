package com.skilllink.model;

public class User {
    private long userId;
    private String phoneNumber;
    private String email;
    private String fullName;
    private String avatarUrl;
    private String userType; // customer/worker
    private String verificationStatus;
    private String metadata;

    // Constructors
    public User() {
    }

    public User(String phoneNumber, String email, String fullName, String avatarUrl, 
                String userType, String verificationStatus, String metadata) {
        this.phoneNumber = phoneNumber;
        this.email = email;
        this.fullName = fullName;
        this.avatarUrl = avatarUrl;
        this.userType = userType;
        this.verificationStatus = verificationStatus;
        this.metadata = metadata;
    }

    // Getters and Setters
    public long getUserId() {
        return userId;
    }

    public void setUserId(long userId) {
        this.userId = userId;
    }

    public String getPhoneNumber() {
        return phoneNumber;
    }

    public void setPhoneNumber(String phoneNumber) {
        this.phoneNumber = phoneNumber;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public String getFullName() {
        return fullName;
    }

    public void setFullName(String fullName) {
        this.fullName = fullName;
    }

    public String getAvatarUrl() {
        return avatarUrl;
    }

    public void setAvatarUrl(String avatarUrl) {
        this.avatarUrl = avatarUrl;
    }

    public String getUserType() {
        return userType;
    }

    public void setUserType(String userType) {
        this.userType = userType;
    }

    public String getVerificationStatus() {
        return verificationStatus;
    }

    public void setVerificationStatus(String verificationStatus) {
        this.verificationStatus = verificationStatus;
    }

    public String getMetadata() {
        return metadata;
    }

    public void setMetadata(String metadata) {
        this.metadata = metadata;
    }
}