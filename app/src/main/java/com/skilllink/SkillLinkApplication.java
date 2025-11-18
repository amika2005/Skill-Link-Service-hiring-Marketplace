package com.skilllink;

import android.app.Application;
import android.util.Log;
import com.google.firebase.FirebaseApp;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FirebaseFirestore;
import com.skilllink.util.FirebaseDatabaseInitializer;

/**
 * Application class for SkillLink app
 * Initializes Firebase and provides global application context
 */
public class SkillLinkApplication extends Application {
    
    private static final String TAG = "SkillLinkApp";
    private static SkillLinkApplication instance;
    
    @Override
    public void onCreate() {
        super.onCreate();
        instance = this;
        
        // Initialize Firebase
        initializeFirebase();
    }
    
    private void initializeFirebase() {
        try {
            // Check if Firebase is already initialized
            if (FirebaseApp.getApps(this).isEmpty()) {
                Log.d(TAG, "Initializing Firebase...");
                FirebaseApp.initializeApp(this);
                Log.d(TAG, "Firebase initialized successfully");
            } else {
                Log.d(TAG, "Firebase already initialized");
            }
            
            // Test Firebase connectivity
            testFirebaseConnection();
            
        } catch (Exception e) {
            Log.e(TAG, "Failed to initialize Firebase", e);
        }
    }
    
    private void testFirebaseConnection() {
        try {
            // Test FirebaseAuth
            FirebaseAuth auth = FirebaseAuth.getInstance();
            Log.d(TAG, "FirebaseAuth instance: " + (auth != null ? "Available" : "Not available"));
            
            // Test FirebaseFirestore
            FirebaseFirestore firestore = FirebaseFirestore.getInstance();
            Log.d(TAG, "FirebaseFirestore instance: " + (firestore != null ? "Available" : "Not available"));
            
            // Get Firebase project info
            FirebaseApp firebaseApp = FirebaseApp.getInstance();
            Log.d(TAG, "Firebase project ID: " + firebaseApp.getOptions().getProjectId());
            Log.d(TAG, "Firebase app ID: " + firebaseApp.getOptions().getApplicationId());
            
            // Initialize Firestore database and collections
            FirebaseDatabaseInitializer.initializeDatabase(new FirebaseDatabaseInitializer.DatabaseInitializationCallback() {
                @Override
                public void onSuccess() {
                    Log.d(TAG, "✅ Firebase Firestore database initialization completed successfully!");
                }
                
                @Override
                public void onError(String error) {
                    Log.e(TAG, "❌ Firebase Firestore database initialization failed: " + error);
                }
            });
            
        } catch (Exception e) {
            Log.e(TAG, "Firebase connection test failed", e);
        }
    }
    
    public static SkillLinkApplication getInstance() {
        return instance;
    }
    
    public static boolean isFirebaseInitialized() {
        try {
            return !FirebaseApp.getApps(getInstance()).isEmpty();
        } catch (Exception e) {
            Log.e(TAG, "Error checking Firebase initialization", e);
            return false;
        }
    }
}
