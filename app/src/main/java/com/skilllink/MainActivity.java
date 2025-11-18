package com.skilllink;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;

import androidx.appcompat.app.AppCompatActivity;

import com.skilllink.auth.FirebaseAuthManager;
import com.skilllink.util.FirebaseSessionManager;

public class MainActivity extends AppCompatActivity {
    private static final String TAG = "SkillLink";
    
    private FirebaseAuthManager authManager;
    private FirebaseSessionManager sessionManager;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        // Initialize Firebase managers
        authManager = FirebaseAuthManager.getInstance();
        sessionManager = new FirebaseSessionManager(this);

        // Check authentication state
        checkAuthenticationState();
    }

    private void checkAuthenticationState() {
        if (authManager.isAuthenticated()) {
            // User is logged in, get user profile and navigate to appropriate screen
            FirebaseAuthManager.UserProfile profile = sessionManager.getCurrentUserProfile();
            if (profile != null) {
                String role = profile.getRole();
                Log.d(TAG, "User is logged in with role: " + role);
                
                // Navigate to role-specific home screen
                navigateToHomeScreen(role);
            } else {
                // Profile not loaded yet, wait for it
                authManager.getCurrentUserProfile(new FirebaseAuthManager.ProfileCallback() {
                    @Override
                    public void onProfileLoaded(FirebaseAuthManager.UserProfile profile) {
                        if (profile != null) {
                            String role = profile.getRole();
                            Log.d(TAG, "User profile loaded with role: " + role);
                            navigateToHomeScreen(role);
                        } else {
                            // No profile found, go to role selection
                            Log.d(TAG, "No user profile found, going to role selection");
                            navigateToRoleSelection();
                        }
                    }
                    
                    @Override
                    public void onError(String error) {
                        Log.e(TAG, "Error loading user profile: " + error);
                        navigateToRoleSelection();
                    }
                });
            }
        } else {
            // User is not logged in, navigate to role selection
            Log.d(TAG, "User is not logged in, going to role selection");
            navigateToRoleSelection();
        }
    }

    private void navigateToHomeScreen(String role) {
        Intent intent = new Intent(this, com.skilllink.RoleSelectionActivity.class);
        intent.putExtra("USER_ROLE", role);
        intent.putExtra("SKIP_SELECTION", true);
        startActivity(intent);
        finish();
    }

    private void navigateToRoleSelection() {
        Intent intent = new Intent(this, com.skilllink.RoleSelectionActivity.class);
        startActivity(intent);
        finish();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        // Clean up Firebase listeners if needed
        if (sessionManager != null) {
            // sessionManager.clearRealtimeListeners(); // Uncomment if needed
        }
    }
}
