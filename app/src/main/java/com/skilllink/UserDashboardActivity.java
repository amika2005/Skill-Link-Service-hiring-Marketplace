package com.skilllink;

import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
import androidx.navigation.NavController;
import androidx.navigation.Navigation;
import androidx.navigation.ui.NavigationUI;
import com.google.android.material.bottomnavigation.BottomNavigationView;

public class UserDashboardActivity extends AppCompatActivity {

    private static final String TAG = "UserDashboardActivity";
    
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Log.d(TAG, "onCreate called");

        setContentView(R.layout.activity_user_dashboard);
        Log.d(TAG, "ContentView set");

        // Show welcome message with username
        showWelcomeMessage();

        // Wait for the view to be fully loaded
        findViewById(R.id.bottom_navigation).post(new Runnable() {
            @Override
            public void run() {
                setupBottomNavigation();
            }
        });
    }
    
    private void setupBottomNavigation() {
        try {
            Log.d(TAG, "Setting up bottom navigation");
            
            // Find the bottom navigation view
            BottomNavigationView bottomNavigationView = findViewById(R.id.bottom_navigation);
            if (bottomNavigationView == null) {
                Log.e(TAG, "BottomNavigationView is NULL");
                Toast.makeText(this, "ERROR: BottomNavigationView is NULL", Toast.LENGTH_LONG).show();
                return;
            }
            
            Log.d(TAG, "BottomNavigationView found: " + bottomNavigationView.toString());
            Toast.makeText(this, "SUCCESS: BottomNavigationView found", Toast.LENGTH_SHORT).show();
            
            // Check if the view is visible
            if (bottomNavigationView.getVisibility() == View.VISIBLE) {
                Log.d(TAG, "BottomNavigationView is VISIBLE");
            } else {
                Log.d(TAG, "BottomNavigationView is NOT VISIBLE");
                bottomNavigationView.setVisibility(View.VISIBLE);
            }

            bottomNavigationView.setItemIconTintList(null);
            bottomNavigationView.setItemTextColor(null);
            
            // Get the NavController
            NavController navController = Navigation.findNavController(this, R.id.nav_host_fragment_content_user_dashboard);
            Log.d(TAG, "NavController found");
            
            // Setup the navigation
            NavigationUI.setupWithNavController(bottomNavigationView, navController);
            Log.d(TAG, "Bottom navigation setup completed");
            
            // Add destination change listener
            navController.addOnDestinationChangedListener((controller, destination, arguments) -> {
                Log.d(TAG, "Navigated to: " + destination.getLabel());
            });
            
        } catch (Exception e) {
            Log.e(TAG, "Error setting up bottom navigation", e);
            Toast.makeText(this, "ERROR: " + e.getMessage(), Toast.LENGTH_LONG).show();
        }
    }
    
    private void showWelcomeMessage() {
        // Get user data from intent extras
        String userName = getIntent().getStringExtra("user_name");
        String userEmail = getIntent().getStringExtra("user_email");
        String userRole = getIntent().getStringExtra("user_role");
        
        // Fallback to session manager if intent extras are null
        if (userName == null) {
            com.skilllink.util.FirebaseSessionManager sessionManager = new com.skilllink.util.FirebaseSessionManager(this);
            com.skilllink.auth.FirebaseAuthManager.UserProfile currentUser = sessionManager.getCurrentUserProfile();
            if (currentUser != null) {
                userName = currentUser.getDisplayName();
                userEmail = currentUser.getEmail();
                userRole = currentUser.getRole();
            }
        }
        
        // Create welcome message
        String welcomeMessage;
        if (userName != null && !userName.trim().isEmpty()) {
            welcomeMessage = "Welcome back, " + userName + "!";
        } else {
            welcomeMessage = "Welcome back!";
        }
        
        // Add role information if available
        if (userRole != null) {
            String roleText = userRole.equals("user") ? "Customer" : "Professional";
            welcomeMessage += "\nLogged in as " + roleText;
        }
        
        // Show welcome toast
        Toast.makeText(this, welcomeMessage, Toast.LENGTH_LONG).show();
        Log.d(TAG, "Welcome message shown: " + welcomeMessage);
    }
    
    @Override
    protected void onResume() {
        super.onResume();
        Log.d(TAG, "onResume called");
    }
}
