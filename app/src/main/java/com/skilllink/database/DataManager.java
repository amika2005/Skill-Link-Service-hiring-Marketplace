package com.skilllink.database;

import android.content.Context;
import android.database.sqlite.SQLiteDatabase;

public class DataManager {
    private DatabaseHelper dbHelper;
    private SQLiteDatabase database;

    public DataManager(Context context) {
        dbHelper = new DatabaseHelper(context);
    }

    public void open() {
        database = dbHelper.getWritableDatabase();
    }

    public void close() {
        dbHelper.close();
    }

    // User operations
    public long insertUser(String phoneNumber, String email, String fullName, String avatarUrl, 
                          String userType, String verificationStatus, String metadata) {
        android.content.ContentValues values = new android.content.ContentValues();
        values.put(DatabaseHelper.COLUMN_PHONE_NUMBER, phoneNumber);
        values.put(DatabaseHelper.COLUMN_EMAIL, email);
        values.put(DatabaseHelper.COLUMN_FULL_NAME, fullName);
        values.put(DatabaseHelper.COLUMN_AVATAR_URL, avatarUrl);
        values.put(DatabaseHelper.COLUMN_USER_TYPE, userType);
        values.put(DatabaseHelper.COLUMN_VERIFICATION_STATUS, verificationStatus);
        values.put(DatabaseHelper.COLUMN_METADATA, metadata);
        
        return database.insert(DatabaseHelper.TABLE_USERS, null, values);
    }

    // Worker operations
    public long insertWorker(long userId, int experienceYears, double ratingAverage, int totalJobs,
                            String verificationDocuments, String availabilityStatus, 
                            double locationLat, double locationLng, double serviceAreaRadius) {
        android.content.ContentValues values = new android.content.ContentValues();
        values.put(DatabaseHelper.COLUMN_USER_ID, userId);
        values.put(DatabaseHelper.COLUMN_EXPERIENCE_YEARS, experienceYears);
        values.put(DatabaseHelper.COLUMN_RATING_AVERAGE, ratingAverage);
        values.put(DatabaseHelper.COLUMN_TOTAL_JOBS, totalJobs);
        values.put(DatabaseHelper.COLUMN_VERIFICATION_DOCUMENTS, verificationDocuments);
        values.put(DatabaseHelper.COLUMN_AVAILABILITY_STATUS, availabilityStatus);
        values.put(DatabaseHelper.COLUMN_LOCATION_LAT, locationLat);
        values.put(DatabaseHelper.COLUMN_LOCATION_LNG, locationLng);
        values.put(DatabaseHelper.COLUMN_SERVICE_AREA_RADIUS, serviceAreaRadius);
        
        return database.insert(DatabaseHelper.TABLE_WORKERS, null, values);
    }

    // Service operations
    public long insertService(String category, String subCategory, String serviceName, 
                             String description, double basePrice, String priceType, 
                             int averageDuration, String iconUrl) {
        android.content.ContentValues values = new android.content.ContentValues();
        values.put(DatabaseHelper.COLUMN_CATEGORY, category);
        values.put(DatabaseHelper.COLUMN_SUB_CATEGORY, subCategory);
        values.put(DatabaseHelper.COLUMN_SERVICE_NAME, serviceName);
        values.put(DatabaseHelper.COLUMN_DESCRIPTION, description);
        values.put(DatabaseHelper.COLUMN_BASE_PRICE, basePrice);
        values.put(DatabaseHelper.COLUMN_PRICE_TYPE, priceType);
        values.put(DatabaseHelper.COLUMN_AVERAGE_DURATION, averageDuration);
        values.put(DatabaseHelper.COLUMN_ICON_URL, iconUrl);
        
        return database.insert(DatabaseHelper.TABLE_SERVICES, null, values);
    }

    // Booking operations
    public long insertBooking(long customerId, long workerId, long serviceId, String status,
                             String scheduledTime, double locationLat, double locationLng,
                             double totalAmount, String paymentStatus, String trackingData) {
        android.content.ContentValues values = new android.content.ContentValues();
        values.put(DatabaseHelper.COLUMN_CUSTOMER_ID, customerId);
        values.put(DatabaseHelper.COLUMN_WORKER_ID, workerId);
        values.put(DatabaseHelper.COLUMN_SERVICE_ID, serviceId);
        values.put(DatabaseHelper.COLUMN_STATUS, status);
        values.put(DatabaseHelper.COLUMN_SCHEDULED_TIME, scheduledTime);
        values.put(DatabaseHelper.COLUMN_LOCATION_LAT_BOOKING, locationLat);
        values.put(DatabaseHelper.COLUMN_LOCATION_LNG_BOOKING, locationLng);
        values.put(DatabaseHelper.COLUMN_TOTAL_AMOUNT, totalAmount);
        values.put(DatabaseHelper.COLUMN_PAYMENT_STATUS, paymentStatus);
        values.put(DatabaseHelper.COLUMN_TRACKING_DATA, trackingData);
        
        return database.insert(DatabaseHelper.TABLE_BOOKINGS, null, values);
    }
}