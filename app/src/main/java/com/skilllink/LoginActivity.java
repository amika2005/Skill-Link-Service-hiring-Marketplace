package com.skilllink;

import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.text.TextUtils;
import android.util.Log;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
import com.google.android.material.textfield.TextInputEditText;
import com.google.android.material.textfield.TextInputLayout;
import com.skilllink.auth.FirebaseAuthManager;
import com.skilllink.util.SessionManager;

public class LoginActivity extends AppCompatActivity {

    private TextInputEditText emailInput;
    private TextInputEditText passwordInput;
    private Button loginButton;
    private TextView forgotPassword;
    private TextView signupPrompt;
    private TextView roleIndicator;
    private String userRole;
    private SessionManager sessionManager;
    private TextInputLayout emailLayout;
    private TextInputLayout passwordLayout;
    private String loginButtonDefaultText;
    private FirebaseAuthManager authManager;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_login);

        sessionManager = new SessionManager(this);
        authManager = FirebaseAuthManager.getInstance();

        if (sessionManager.isLoggedIn()) {
            userRole = sessionManager.getUserRole();
            if (userRole == null) {
                // If role is not available, try to get it from the intent first
                userRole = getIntent().getStringExtra("user_role");
                if (userRole == null) {
                    userRole = "user"; // Default to user role
                }
            }
            navigateToDashboard(userRole);
            return;
        }

        // Get user role from intent
        userRole = getIntent().getStringExtra("user_role");
        if (userRole == null) {
            userRole = "user"; // Default to user role
        }

        initializeViews();
        updateRoleIndicator();
        setupClickListeners();
        loadAnimations();
    }

    private void initializeViews() {
        emailInput = findViewById(R.id.email_input);
        passwordInput = findViewById(R.id.password_input);
        loginButton = findViewById(R.id.login_button);
        forgotPassword = findViewById(R.id.forgot_password);
        signupPrompt = findViewById(R.id.signup_prompt);
        roleIndicator = findViewById(R.id.role_indicator);
        emailLayout = findViewById(R.id.email_input_layout);
        passwordLayout = findViewById(R.id.password_input_layout);
        loginButtonDefaultText = loginButton.getText() != null ? loginButton.getText().toString() : "Sign in";
    }

    private void updateRoleIndicator() {
        String roleText = userRole.equals("user") ? "Customer" : "Professional";
        roleIndicator.setText("Logging in as: " + roleText);
    }

    private void loadAnimations() {
        // Add a fade-in animation to the main content
        Animation fadeIn = AnimationUtils.loadAnimation(this, android.R.anim.fade_in);
        findViewById(R.id.email_input_layout).startAnimation(fadeIn);
        findViewById(R.id.password_input_layout).startAnimation(fadeIn);
    }

    private void setupClickListeners() {
        loginButton.setOnClickListener(v -> attemptLogin());

        forgotPassword.setOnClickListener(v -> {
            Toast.makeText(LoginActivity.this, "Forgot Password clicked", Toast.LENGTH_SHORT).show();
            // TODO: Implement forgot password functionality
        });

        signupPrompt.setOnClickListener(v -> navigateToSignup());
    }

    private void attemptLogin() {
        String email = emailInput.getText().toString().trim();
        String password = passwordInput.getText().toString().trim();

        clearFieldErrors();

        // Validate inputs
        if (TextUtils.isEmpty(email)) {
            emailInput.setError("Email is required");
            if (emailLayout != null) {
                emailLayout.setError("Email is required");
            }
            emailInput.requestFocus();
            return;
        }

        if (TextUtils.isEmpty(password)) {
            passwordInput.setError("Password is required");
            passwordLayout.setError("Password is required");
            passwordInput.requestFocus();
            return;
        }

        if (password.length() < 6) {
            passwordInput.setError("Password must be at least 6 characters");
            if (passwordLayout != null) {
                passwordLayout.setError("Password must be at least 6 characters");
            }
            passwordInput.requestFocus();
            return;
        }

        // Show loading state with animation
        loginButton.setText("Signing In...");
        loginButton.setEnabled(false);

        // Animate the login button
        loginButton.animate()
                .scaleX(0.95f)
                .scaleY(0.95f)
                .setDuration(100)
                .withEndAction(() -> {
                    loginButton.animate()
                            .scaleX(1.0f)
                            .scaleY(1.0f)
                            .setDuration(100)
                            .start();
                })
                .start();

        new android.os.Handler().postDelayed(() -> {
            performLogin(email, password);
        }, 1500);
    }

    private void performLogin(String email, String password) {
        authManager.signIn(email, password, new FirebaseAuthManager.LoginCallback() {
            @Override
            public void onSuccess(FirebaseAuthManager.UserProfile profile) {
                runOnUiThread(() -> {
                    // Verify role matches before proceeding (case-insensitive)
                    String profileRole = profile.getRole();
                    String expectedRole = userRole;
                    
                    // Normalize roles for comparison
                    if (profileRole != null) {
                        profileRole = profileRole.toUpperCase().trim();
                    }
                    if (expectedRole != null) {
                        expectedRole = expectedRole.toUpperCase().trim();
                    }
                    
                    if (profileRole != null && profileRole.equals(expectedRole)) {
                        // Save session using SessionManager
                        sessionManager.saveSession(email, userRole);
                        sessionManager.updateUserProfile(profile.getDisplayName(), email, profile.getPhone(), 
                                                       profile.getLocation(), profile.getBio());
                        
                        Toast.makeText(LoginActivity.this, "Login successful!", Toast.LENGTH_SHORT).show();
                        navigateToDashboard(userRole);
                    } else {
                        String message = "Account exists but with different role. ";
                        if (profileRole != null) {
                            message += "Account role: " + profileRole + ", Expected: " + expectedRole;
                        } else {
                            message += "Account role not found. Please contact support.";
                        }
                        Toast.makeText(LoginActivity.this, message, Toast.LENGTH_LONG).show();
                        resetLoginButton();
                    }
                });
            }

            @Override
            public void onError(FirebaseAuthManager.AuthError error, String message) {
                runOnUiThread(() -> {
                    handleFirebaseLoginError(error);
                    resetLoginButton();
                });
            }
        });
    }

    private void navigateToDashboard(String role) {
        Intent intent;
        if ("user".equals(role)) {
            intent = new Intent(this, UserDashboardActivity.class);
        } else {
            intent = new Intent(this, WorkerDashboardActivity.class);
        }
        
        // Pass user profile data to dashboard for welcome message
        String userName = sessionManager.getUserName();
        String userEmail = sessionManager.getUserEmail();
        String userRole = sessionManager.getUserRole();
        
        if (userName != null) {
            intent.putExtra("user_name", userName);
        }
        if (userEmail != null) {
            intent.putExtra("user_email", userEmail);
        }
        if (userRole != null) {
            intent.putExtra("user_role", userRole);
        }
        
        Toast.makeText(this, "Welcome back!", Toast.LENGTH_SHORT).show();
        android.util.Log.d("LoginActivity", "Starting dashboard with welcome message");
        startActivity(intent);
        finish();
    }

    private void navigateToSignup() {
        Intent intent = new Intent(this, UserSignupActivity.class);
        intent.putExtra("user_role", userRole);
        startActivity(intent);
    }

    private void resetLoginButton() {
        loginButton.setText(loginButtonDefaultText);
        loginButton.setEnabled(true);
    }

    private void handleFirebaseLoginError(FirebaseAuthManager.AuthError error) {
        String message = "Login failed";
        switch (error) {
            case INVALID_EMAIL:
                message = "Invalid email address";
                emailInput.setError(message);
                emailInput.requestFocus();
                break;
            case WRONG_PASSWORD:
                message = "Incorrect password";
                passwordInput.setError(message);
                passwordInput.requestFocus();
                break;
            case USER_NOT_FOUND:
                message = "No account found with this email";
                emailInput.setError(message);
                emailInput.requestFocus();
                break;
            case NETWORK_ERROR:
                message = "Network error. Please check your connection";
                break;
            default:
                message = "Login failed. Please try again";
                break;
        }
        Toast.makeText(this, message, Toast.LENGTH_SHORT).show();
    }

    private void clearFieldErrors() {
        if (emailLayout != null) {
            emailLayout.setError(null);
        }
        if (passwordLayout != null) {
            passwordLayout.setError(null);
        }
        emailInput.setError(null);
        passwordInput.setError(null);
    }
}
