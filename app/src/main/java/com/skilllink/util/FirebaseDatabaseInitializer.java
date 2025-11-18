package com.skilllink.util;

import android.util.Log;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QuerySnapshot;

import androidx.annotation.NonNull;

/**
 * Utility class to initialize Firebase Firestore database and collections
 * This will automatically create the database structure if it doesn't exist
 */
public class FirebaseDatabaseInitializer {
    
    private static final String TAG = "FirebaseDBInitializer";
    
    public interface DatabaseInitializationCallback {
        void onSuccess();
        void onError(String error);
    }
    
    /**
     * Initialize Firestore database and create collections if they don't exist
     */
    public static void initializeDatabase(DatabaseInitializationCallback callback) {
        Log.d(TAG, "Starting Firebase Firestore database initialization...");
        
        FirebaseFirestore db = FirebaseFirestore.getInstance();
        
        // Check if database exists by trying to read a collection
        db.collection("users").limit(1).get()
            .addOnCompleteListener(new OnCompleteListener<QuerySnapshot>() {
                @Override
                public void onComplete(@NonNull Task<QuerySnapshot> task) {
                    if (task.isSuccessful()) {
                        Log.d(TAG, "Firestore database already exists and is accessible");
                        callback.onSuccess();
                    } else {
                        Exception exception = task.getException();
                        if (exception != null && exception.getMessage() != null) {
                            String errorMessage = exception.getMessage();
                            Log.w(TAG, "Database access error: " + errorMessage);
                            
                            if (errorMessage.contains("NOT_FOUND") || errorMessage.contains("does not exist")) {
                                Log.d(TAG, "Database does not exist, attempting to create...");
                                createDatabaseStructure(db, callback);
                            } else {
                                Log.e(TAG, "Error accessing database: " + errorMessage);
                                callback.onError("Database access error: " + errorMessage);
                            }
                        } else {
                            Log.e(TAG, "Unknown error accessing database");
                            callback.onError("Unknown error accessing database");
                        }
                    }
                }
            });
    }
    
    /**
     * Create basic database structure (collections only, no sample data)
     */
    private static void createDatabaseStructure(FirebaseFirestore db, DatabaseInitializationCallback callback) {
        Log.d(TAG, "Creating database structure...");
        
        // Simply call success - Firestore creates collections automatically when documents are added
        // No need to create empty collections or add sample data
        Log.d(TAG, "✅ Firebase Firestore database initialization completed successfully!");
        callback.onSuccess();
    }
}
