package com.skilllink;

import android.os.Bundle;
import android.util.Log;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
import androidx.navigation.NavController;
import androidx.navigation.Navigation;
import androidx.navigation.ui.NavigationUI;
import com.skilllink.databinding.ActivityWorkerDashboardBinding; 

public class WorkerDashboardActivity extends AppCompatActivity {

    private static final String TAG = "WorkerDashboardActivity";
    private ActivityWorkerDashboardBinding binding;
    
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Log.d(TAG, "onCreate called");

        binding = ActivityWorkerDashboardBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());
        Log.d(TAG, "ContentView set");

        // Show welcome message with username
        showWelcomeMessage();

        NavController navController = Navigation.findNavController(this, R.id.nav_host_fragment_content_worker_dashboard);
        NavigationUI.setupWithNavController(binding.bottomNavigation, navController);
        binding.bottomNavigation.setItemIconTintList(null);
        Log.d(TAG, "Worker dashboard setup completed");
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
}
