package com.skilllink.database;

import android.content.Context;
import android.util.Log;

public class DatabaseTest {
    private static final String TAG = "DatabaseTest";
    
    public static void runDatabaseTest(Context context) {
        try {
            DataManager dataManager = new DataManager(context);
            dataManager.open();
            
            // Test inserting a user
            long userId = dataManager.insertUser(
                "+94771234567", 
                "test@example.com", 
                "Test User", 
                "", 
                "customer", 
                "verified", 
                "{}"
            );
            
            Log.d(TAG, "Inserted user with ID: " + userId);
            
            // Test inserting a worker
            long workerId = dataManager.insertWorker(
                userId, 
                5, 
                4.8, 
                120, 
                "{}", 
                "available", 
                6.9271,  // Latitude for Colombo
                79.8612, // Longitude for Colombo
                10.0
            );
            
            Log.d(TAG, "Inserted worker with ID: " + workerId);
            
            // Test inserting a service
            long serviceId = dataManager.insertService(
                "Plumbers", 
                "Emergency Plumbing", 
                "Emergency Pipe Repair", 
                "24/7 emergency pipe repair service", 
                2500.0, 
                "fixed", 
                60, 
                ""
            );
            
            Log.d(TAG, "Inserted service with ID: " + serviceId);
            
            // Test inserting a booking
            long bookingId = dataManager.insertBooking(
                userId, 
                workerId, 
                serviceId, 
                "pending", 
                "2025-10-05 10:00:00", 
                6.9271, 
                79.8612, 
                3000.0, 
                "pending", 
                "{}"
            );
            
            Log.d(TAG, "Inserted booking with ID: " + bookingId);
            
            dataManager.close();
            
            Log.d(TAG, "Database test completed successfully!");
            
        } catch (Exception e) {
            Log.e(TAG, "Database test failed", e);
        }
    }
}