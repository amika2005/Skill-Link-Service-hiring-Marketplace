package com.skilllink;

import android.content.Context;
import android.content.Intent;
import android.content.res.ColorStateList;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.Log;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;

import com.google.android.material.button.MaterialButton;
import com.google.android.material.checkbox.MaterialCheckBox;
import com.google.android.material.textfield.TextInputEditText;
import com.google.firebase.firestore.FirebaseFirestore;
import com.skilllink.auth.FirebaseAuthManager;

import java.util.HashMap;
import java.util.Map;

public class UserSignupActivity extends AppCompatActivity {

    private static final String ROLE_USER = "user";
    private static final String ROLE_WORKER = "worker";

    private LinearLayout userFieldsContainer;
    private LinearLayout workerFieldsContainer;
    private TextView roleContextText;
    private MaterialButton userTypeUserButton;
    private MaterialButton userTypeWorkerButton;
    private TextInputEditText nameInput;
    private TextInputEditText phoneInput;
    private TextInputEditText firstNameInput;
    private TextInputEditText lastNameInput;
    private TextInputEditText workerPhoneInput;
    private TextInputEditText skillsInput;
    private TextInputEditText experienceInput;
    private TextInputEditText locationInput;
    private TextInputEditText emailInput;
    private TextInputEditText passwordInput;
    private TextInputEditText confirmPasswordInput;
    private MaterialButton signupButton;
    private TextView loginPrompt;
    private MaterialCheckBox termsCheckbox;
    private String selectedRole = ROLE_USER;
    private FirebaseAuthManager authManager;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_user_signup);

        authManager = FirebaseAuthManager.getInstance();

        String initialRole = getIntent().getStringExtra("user_role");
        if (ROLE_WORKER.equals(initialRole)) {
            selectedRole = ROLE_WORKER;
        }

        initializeViews();
        setupClickListeners();
        updateRoleState(selectedRole);
    }

    private void initializeViews() {
        userFieldsContainer = findViewById(R.id.user_fields_container);
        workerFieldsContainer = findViewById(R.id.worker_fields_container);
        roleContextText = findViewById(R.id.text_role_context);
        userTypeUserButton = findViewById(R.id.user_type_user);
        userTypeWorkerButton = findViewById(R.id.user_type_worker);
        nameInput = findViewById(R.id.name_input);
        phoneInput = findViewById(R.id.phone_input);
        firstNameInput = findViewById(R.id.first_name_input);
        lastNameInput = findViewById(R.id.last_name_input);
        workerPhoneInput = findViewById(R.id.worker_phone_input);
        skillsInput = findViewById(R.id.skills_input);
        experienceInput = findViewById(R.id.experience_input);
        locationInput = findViewById(R.id.location_input);
        emailInput = findViewById(R.id.email_input);
        passwordInput = findViewById(R.id.password_input);
        confirmPasswordInput = findViewById(R.id.confirm_password_input);
        signupButton = findViewById(R.id.signup_button);
        loginPrompt = findViewById(R.id.login_prompt);
        termsCheckbox = findViewById(R.id.terms_checkbox);
    }

    private void setupClickListeners() {
        userTypeUserButton.setOnClickListener(v -> updateRoleState(ROLE_USER));
        userTypeWorkerButton.setOnClickListener(v -> updateRoleState(ROLE_WORKER));

        signupButton.setOnClickListener(v -> attemptSignup());

        loginPrompt.setOnClickListener(v -> {
            Intent intent = new Intent(UserSignupActivity.this, LoginActivity.class);
            intent.putExtra("user_role", selectedRole);
            startActivity(intent);
            finish();
        });
    }

    private void attemptSignup() {
        if (!isNetworkAvailable()) {
            Toast.makeText(this, "No internet connection. Please check your network and try again.", Toast.LENGTH_LONG).show();
            return;
        }

        if (!termsCheckbox.isChecked()) {
            Toast.makeText(this, "Please agree to the Terms of Service and Privacy Policy", Toast.LENGTH_SHORT).show();
            return;
        }

        String email = emailInput.getText().toString().trim();
        String password = passwordInput.getText().toString().trim();
        String confirmPassword = confirmPasswordInput.getText().toString().trim();

        if (TextUtils.isEmpty(email)) {
            emailInput.setError("Email is required");
            return;
        }

        if (!android.util.Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
            emailInput.setError("Please enter a valid email address");
            return;
        }

        if (TextUtils.isEmpty(password)) {
            passwordInput.setError("Password is required");
            return;
        }

        if (password.length() < 6) {
            passwordInput.setError("Password must be at least 6 characters");
            return;
        }

        if (!password.equals(confirmPassword)) {
            confirmPasswordInput.setError("Passwords do not match");
            return;
        }

        if (ROLE_USER.equals(selectedRole)) {
            String name = readValue(nameInput);
            String phone = readValue(phoneInput);

            if (TextUtils.isEmpty(name)) {
                nameInput.setError("Full name is required");
                return;
            }

            if (TextUtils.isEmpty(phone)) {
                phoneInput.setError("Phone number is required");
                return;
            }

            setLoadingState(true);
            new android.os.Handler().postDelayed(() -> {
                performSignupUser(name, email, phone, password);
            }, 1500);
        } else {
            String firstName = readValue(firstNameInput);
            String lastName = readValue(lastNameInput);
            String workerPhone = readValue(workerPhoneInput);
            String skills = readValue(skillsInput);
            String experience = readValue(experienceInput);
            String location = readValue(locationInput);

            if (TextUtils.isEmpty(firstName)) {
                firstNameInput.setError("First name is required");
                return;
            }

            if (TextUtils.isEmpty(lastName)) {
                lastNameInput.setError("Last name is required");
                return;
            }

            if (TextUtils.isEmpty(workerPhone)) {
                workerPhoneInput.setError("Contact number is required");
                return;
            }

            if (TextUtils.isEmpty(skills)) {
                skillsInput.setError("Please list your primary skills");
                return;
            }

            if (TextUtils.isEmpty(experience)) {
                experienceInput.setError("Experience is required");
                return;
            }

            if (TextUtils.isEmpty(location)) {
                locationInput.setError("Service locations are required");
                return;
            }

            setLoadingState(true);
            new android.os.Handler().postDelayed(() -> {
                performSignupWorker(firstName, lastName, email, workerPhone, skills, experience, location, password);
            }, 1500);
        }
    }

    private void performSignupUser(String name, String email, String phone, String password) {
        authManager.registerUser(email, password, FirebaseAuthManager.ROLE_USER, name, phone, 
            new FirebaseAuthManager.RegistrationCallback() {
                @Override
                public void onSuccess(FirebaseAuthManager.UserProfile profile) {
                    runOnUiThread(() -> {
                        Toast.makeText(UserSignupActivity.this, "Account created successfully!", Toast.LENGTH_SHORT).show();
                        navigateToLogin(email);
                    });
                }

                @Override
                public void onError(FirebaseAuthManager.AuthError error, String message) {
                    runOnUiThread(() -> {
                        handleFirebaseRegistrationError(error);
                        setLoadingState(false);
                    });
                }
            });
    }

    private void performSignupWorker(String firstName, String lastName, String email, String phone,
                                     String skills, String experience, String location, String password) {
        String displayName = (firstName + " " + lastName).trim();

        Log.d("UserSignupActivity", "Starting worker registration for: " + email);

        authManager.registerUser(email, password, FirebaseAuthManager.ROLE_WORKER, displayName, phone, 
            new FirebaseAuthManager.RegistrationCallback() {
                @Override
                public void onSuccess(FirebaseAuthManager.UserProfile profile) {
                    runOnUiThread(() -> {
                        Log.d("UserSignupActivity", "Worker registration successful for: " + email);
                        
                        // Update worker-specific fields in the profile
                        updateWorkerProfile(profile.getUid(), skills, experience, location, 
                            new FirebaseAuthManager.ProfileUpdateCallback() {
                                @Override
                                public void onSuccess() {
                                    Log.d("UserSignupActivity", "Worker profile updated successfully");
                                    Toast.makeText(UserSignupActivity.this, "Worker account created successfully!", Toast.LENGTH_SHORT).show();
                                    navigateToLogin(email);
                                }

                                @Override
                                public void onError(FirebaseAuthManager.AuthError error, String message) {
                                    Log.w("UserSignupActivity", "Failed to update worker profile, but registration succeeded: " + message);
                                    Toast.makeText(UserSignupActivity.this, "Account created successfully!", Toast.LENGTH_SHORT).show();
                                    navigateToLogin(email);
                                }
                            });
                    });
                }

                @Override
                public void onError(FirebaseAuthManager.AuthError error, String message) {
                    runOnUiThread(() -> {
                        Log.e("UserSignupActivity", "Worker registration failed: " + message);
                        handleFirebaseRegistrationError(error);
                        setLoadingState(false);
                    });
                }
            });
    }

    private void updateWorkerProfile(String uid, String skills, String experience, String location, 
                                   FirebaseAuthManager.ProfileUpdateCallback callback) {
        // Update worker-specific fields in Firestore
        FirebaseFirestore firestore = FirebaseFirestore.getInstance();
        Map<String, Object> workerData = new HashMap<>();
        workerData.put("skills", skills);
        workerData.put("experience", experience);
        workerData.put("location", location);
        workerData.put("profileCompleted", true);

        firestore.collection("users").document(uid)
            .update(workerData)
            .addOnSuccessListener(aVoid -> callback.onSuccess())
            .addOnFailureListener(e -> {
                Log.e("UserSignupActivity", "Failed to update worker profile", e);
                callback.onError(FirebaseAuthManager.AuthError.UNKNOWN_ERROR, e.getMessage());
            });
    }

    private void handleFirebaseRegistrationError(FirebaseAuthManager.AuthError error) {
        switch (error) {
            case EMAIL_ALREADY_IN_USE:
                emailInput.setError("Email already registered");
                emailInput.requestFocus();
                break;
            case INVALID_EMAIL:
                emailInput.setError("Invalid email address");
                emailInput.requestFocus();
                break;
            case WEAK_PASSWORD:
                passwordInput.setError("Password is too weak");
                passwordInput.requestFocus();
                break;
            case NETWORK_ERROR:
                Toast.makeText(this, "Network error. Please try again.", Toast.LENGTH_SHORT).show();
                break;
            default:
                Toast.makeText(this, "Registration failed. Please try again.", Toast.LENGTH_SHORT).show();
                break;
        }
    }

    private void navigateToLogin(String email) {
        Intent intent = new Intent(this, LoginActivity.class);
        intent.putExtra("user_role", selectedRole);
        intent.putExtra("registered_email", email);
        startActivity(intent);
        finish();
    }

    private void setLoadingState(boolean loading) {
        if (loading) {
            signupButton.setText("Creating account...");
            signupButton.setEnabled(false);
        } else {
            updateSignupButtonText();
            signupButton.setEnabled(true);
        }
    }

    private void updateRoleState(String role) {
        selectedRole = role;

        boolean isUser = ROLE_USER.equals(role);

        userFieldsContainer.setVisibility(isUser ? View.VISIBLE : View.GONE);
        workerFieldsContainer.setVisibility(isUser ? View.GONE : View.VISIBLE);

        if (isUser) {
            roleContextText.setText("Tell us about yourself");
        } else {
            roleContextText.setText("Help clients understand your expertise");
        }

        updateRoleButtonsUI(isUser);
        updateSignupButtonText();
    }

    private void updateRoleButtonsUI(boolean userSelected) {
        ColorStateList primaryTint = ContextCompat.getColorStateList(this, R.color.primary_color);
        int white = ContextCompat.getColor(this, android.R.color.white);
        int darkText = ContextCompat.getColor(this, R.color.black);
        int outlineColor = ContextCompat.getColor(this, R.color.bottom_nav_divider);

        int strokeWidth = dpToPx(1.5f);

        if (userSelected) {
            userTypeUserButton.setBackgroundTintList(primaryTint);
            userTypeUserButton.setStrokeWidth(0);
            userTypeUserButton.setTextColor(white);

            userTypeWorkerButton.setBackgroundTintList(ColorStateList.valueOf(0x00000000));
            userTypeWorkerButton.setStrokeColor(ColorStateList.valueOf(outlineColor));
            userTypeWorkerButton.setStrokeWidth(strokeWidth);
            userTypeWorkerButton.setTextColor(darkText);
        } else {
            userTypeWorkerButton.setBackgroundTintList(primaryTint);
            userTypeWorkerButton.setStrokeWidth(0);
            userTypeWorkerButton.setTextColor(white);

            userTypeUserButton.setBackgroundTintList(ColorStateList.valueOf(0x00000000));
            userTypeUserButton.setStrokeColor(ColorStateList.valueOf(outlineColor));
            userTypeUserButton.setStrokeWidth(strokeWidth);
            userTypeUserButton.setTextColor(darkText);
        }
    }

    private void updateSignupButtonText() {
        if (ROLE_USER.equals(selectedRole)) {
            signupButton.setText("Create customer account");
        } else {
            signupButton.setText("Join as professional");
        }
    }

    private String readValue(TextInputEditText input) {
        return input != null && input.getText() != null ? input.getText().toString().trim() : "";
    }

    private int dpToPx(float dp) {
        return (int) (dp * getResources().getDisplayMetrics().density + 0.5f);
    }

    private boolean isNetworkAvailable() {
        ConnectivityManager connectivityManager = 
            (ConnectivityManager) getSystemService(Context.CONNECTIVITY_SERVICE);
        if (connectivityManager != null) {
            NetworkInfo activeNetworkInfo = connectivityManager.getActiveNetworkInfo();
            return activeNetworkInfo != null && activeNetworkInfo.isConnected();
        }
        return false;
    }
}
