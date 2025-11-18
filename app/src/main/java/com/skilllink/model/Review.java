package com.skilllink.model;

public class Review {
    private long reviewId;
    private long bookingId;
    private int rating; // 1-5 stars
    private String comment;
    private String photos; // JSON array of photo URLs
    private String response; // Worker's response to the review

    // Constructors
    public Review() {
    }

    public Review(long bookingId, int rating, String comment, String photos, String response) {
        this.bookingId = bookingId;
        this.rating = rating;
        this.comment = comment;
        this.photos = photos;
        this.response = response;
    }

    // Getters and Setters
    public long getReviewId() {
        return reviewId;
    }

    public void setReviewId(long reviewId) {
        this.reviewId = reviewId;
    }

    public long getBookingId() {
        return bookingId;
    }

    public void setBookingId(long bookingId) {
        this.bookingId = bookingId;
    }

    public int getRating() {
        return rating;
    }

    public void setRating(int rating) {
        this.rating = rating;
    }

    public String getComment() {
        return comment;
    }

    public void setComment(String comment) {
        this.comment = comment;
    }

    public String getPhotos() {
        return photos;
    }

    public void setPhotos(String photos) {
        this.photos = photos;
    }

    public String getResponse() {
        return response;
    }

    public void setResponse(String response) {
        this.response = response;
    }
}