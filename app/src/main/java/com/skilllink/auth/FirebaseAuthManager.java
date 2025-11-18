package com.skilllink.auth;

import android.util.Log;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.android.gms.tasks.Tasks;
import com.google.firebase.auth.AuthResult;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.auth.UserProfileChangeRequest;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.SetOptions;

import java.util.HashMap;
import java.util.Map;

/**
 * Complete Firebase-based authentication manager that handles all auth operations
 * and user profile management using Firebase Authentication and Firestore.
 */
public class FirebaseAuthManager {

    private static final String TAG = "FirebaseAuthManager";
    private static final String COLLECTION_USERS = "users";
    
    public static final String ROLE_USER = "USER";
    public static final String ROLE_WORKER = "WORKER";

    public enum AuthError {
        INVALID_EMAIL("auth/invalid-email"),
        WEAK_PASSWORD("auth/weak-password"),
        EMAIL_ALREADY_IN_USE("auth/email-already-in-use"),
        USER_NOT_FOUND("auth/user-not-found"),
        WRONG_PASSWORD("auth/wrong-password"),
        INVALID_CREDENTIALS("auth/invalid-credential"),
        USER_DISABLED("auth/user-disabled"),
        TOO_MANY_REQUESTS("auth/too-many-requests"),
        NETWORK_ERROR("network-error"),
        UNKNOWN_ERROR("unknown-error");

        private final String firebaseCode;

        AuthError(String firebaseCode) {
            this.firebaseCode = firebaseCode;
        }

        public String getFirebaseCode() {
            return firebaseCode;
        }

        public static AuthError fromFirebaseCode(String firebaseCode) {
            for (AuthError error : values()) {
                if (error.firebaseCode.equals(firebaseCode)) {
                    return error;
                }
            }
            return UNKNOWN_ERROR;
        }
    }

    public static class UserProfile {
        private final String uid;
        private final String email;
        private final String displayName;
        private final String phone;
        private final String role;
        private final boolean emailVerified;
        private final long createdAt;
        private final String avatarUri;
        private final String location;
        private final String bio;

        public UserProfile(String uid, String email, String displayName, String phone, 
                          String role, boolean emailVerified, long createdAt) {
            this(uid, email, displayName, phone, role, emailVerified, createdAt, null, null, null);
        }

        public UserProfile(String uid, String email, String displayName, String phone, 
                          String role, boolean emailVerified, long createdAt, 
                          String avatarUri, String location, String bio) {
            this.uid = uid;
            this.email = email;
            this.displayName = displayName;
            this.phone = phone;
            this.role = role;
            this.emailVerified = emailVerified;
            this.createdAt = createdAt;
            this.avatarUri = avatarUri;
            this.location = location;
            this.bio = bio;
        }

        public String getUid() { return uid; }
        public String getEmail() { return email; }
        public String getDisplayName() { return displayName; }
        public String getPhone() { return phone; }
        public String getRole() { return role; }
        public boolean isEmailVerified() { return emailVerified; }
        public long getCreatedAt() { return createdAt; }
        public String getAvatarUri() { return avatarUri; }
        public String getLocation() { return location; }
        public String getBio() { return bio; }
        public boolean isUser() { return ROLE_USER.equals(role); }
        public boolean isWorker() { return ROLE_WORKER.equals(role); }

        public Map<String, Object> toMap() {
            Map<String, Object> map = new HashMap<>();
            map.put("uid", uid);
            map.put("email", email);
            map.put("displayName", displayName);
            map.put("phone", phone);
            map.put("role", role);
            map.put("emailVerified", emailVerified);
            map.put("createdAt", createdAt);
            if (avatarUri != null) map.put("avatarUri", avatarUri);
            if (location != null) map.put("location", location);
            if (bio != null) map.put("bio", bio);
            return map;
        }

        public static UserProfile fromDocument(DocumentSnapshot doc) {
            if (doc == null || !doc.exists()) return null;
            
            return new UserProfile(
                doc.getString("uid"),
                doc.getString("email"),
                doc.getString("displayName"),
                doc.getString("phone"),
                doc.getString("role"),
                doc.getBoolean("emailVerified") != null ? doc.getBoolean("emailVerified") : false,
                doc.getLong("createdAt") != null ? doc.getLong("createdAt") : System.currentTimeMillis(),
                doc.getString("avatarUri"),
                doc.getString("location"),
                doc.getString("bio")
            );
        }
    }

    public interface AuthStateListener {
        void onAuthStateChanged(UserProfile user);
    }

    public interface RegistrationCallback {
        void onSuccess(UserProfile profile);
        void onError(AuthError error, String message);
    }

    public interface LoginCallback {
        void onSuccess(UserProfile profile);
        void onError(AuthError error, String message);
    }

    public interface ProfileUpdateCallback {
        void onSuccess();
        void onError(AuthError error, String message);
    }

    public interface PasswordResetCallback {
        void onSuccess();
        void onError(AuthError error, String message);
    }

    public interface EmailVerificationCallback {
        void onSuccess();
        void onError(AuthError error, String message);
    }

    public interface ProfileCallback {
        void onProfileLoaded(UserProfile profile);
        void onError(String error);
    }

    private static FirebaseAuthManager instance;
    private AuthStateListener authStateListener;
    private final FirebaseAuth firebaseAuth;
    private final FirebaseFirestore firestore;

    private FirebaseAuthManager() {
        firebaseAuth = FirebaseAuth.getInstance();
        firestore = FirebaseFirestore.getInstance();
        
        // Set up auth state listener
        firebaseAuth.addAuthStateListener(new FirebaseAuth.AuthStateListener() {
            @Override
            public void onAuthStateChanged(@NonNull FirebaseAuth auth) {
                FirebaseUser firebaseUser = auth.getCurrentUser();
                UserProfile userProfile = null;
                
                if (firebaseUser != null) {
                    // Load user profile from Firestore
                    loadUserProfileFromFirestore(firebaseUser.getUid(), new ProfileCallback() {
                        @Override
                        public void onProfileLoaded(UserProfile profile) {
                            if (authStateListener != null) {
                                authStateListener.onAuthStateChanged(profile);
                            }
                        }
                        
                        @Override
                        public void onError(String error) {
                            Log.e(TAG, "Error loading user profile: " + error);
                            if (authStateListener != null) {
                                authStateListener.onAuthStateChanged(null);
                            }
                        }
                    });
                } else {
                    if (authStateListener != null) {
                        authStateListener.onAuthStateChanged(null);
                    }
                }
            }
        });
    }

    public static synchronized FirebaseAuthManager getInstance() {
        if (instance == null) {
            instance = new FirebaseAuthManager();
        }
        return instance;
    }

    public void addAuthStateListener(AuthStateListener listener) {
        this.authStateListener = listener;
    }

    public void removeAuthStateListener() {
        this.authStateListener = null;
    }

    public boolean isAuthenticated() {
        return firebaseAuth.getCurrentUser() != null;
    }

    @Nullable
    public String getCurrentUserId() {
        FirebaseUser user = firebaseAuth.getCurrentUser();
        return user != null ? user.getUid() : null;
    }

    public void registerUser(String email, String password, String role, 
                           String displayName, String phone, RegistrationCallback callback) {
        
        Log.d(TAG, "Starting registration for email: " + email + ", role: " + role);
        
        firebaseAuth.createUserWithEmailAndPassword(email, password)
            .addOnSuccessListener(authResult -> {
                FirebaseUser firebaseUser = authResult.getUser();
                if (firebaseUser == null) {
                    Log.e(TAG, "Firebase user is null after successful auth");
                    callback.onError(AuthError.UNKNOWN_ERROR, "Failed to create user");
                    return;
                }

                Log.d(TAG, "User created successfully with UID: " + firebaseUser.getUid());

                // Create user profile
                UserProfile userProfile = new UserProfile(
                    firebaseUser.getUid(),
                    email,
                    displayName,
                    phone,
                    role,
                    firebaseUser.isEmailVerified(),
                    System.currentTimeMillis()
                );

                Log.d(TAG, "Saving user profile to Firestore...");

                // Save to Firestore
                saveUserProfileToFirestore(userProfile, new OnCompleteListener<Void>() {
                    @Override
                    public void onComplete(@NonNull Task<Void> task) {
                        if (task.isSuccessful()) {
                            Log.d(TAG, "User profile saved successfully to Firestore");
                            
                            // Update display name in Firebase Auth
                            UserProfileChangeRequest profileUpdates = new UserProfileChangeRequest.Builder()
                                .setDisplayName(displayName)
                                .build();
                            
                            firebaseUser.updateProfile(profileUpdates)
                                .addOnSuccessListener(aVoid -> {
                                    Log.d(TAG, "Display name updated successfully");
                                    // Send email verification
                                    sendEmailVerificationInternal(firebaseUser);
                                    callback.onSuccess(userProfile);
                                })
                                .addOnFailureListener(e -> {
                                    Log.w(TAG, "Failed to update display name", e);
                                    callback.onSuccess(userProfile); // Still success, profile saved
                                });
                        } else {
                            Log.e(TAG, "Failed to save user profile to Firestore", task.getException());
                            String errorMessage = task.getException() != null ? task.getException().getMessage() : "Unknown error";
                            callback.onError(AuthError.UNKNOWN_ERROR, "Failed to save user profile: " + errorMessage);
                        }
                    }
                });
            })
            .addOnFailureListener(e -> {
                Log.e(TAG, "Registration failed", e);
                AuthError error = mapFirebaseAuthError(e);
                callback.onError(error, e.getMessage());
            });
    }

    public void signIn(String email, String password, LoginCallback callback) {
        firebaseAuth.signInWithEmailAndPassword(email, password)
            .addOnSuccessListener(authResult -> {
                FirebaseUser firebaseUser = authResult.getUser();
                if (firebaseUser == null) {
                    callback.onError(AuthError.UNKNOWN_ERROR, "Sign in failed");
                    return;
                }

                loadUserProfileFromFirestore(firebaseUser.getUid(), new ProfileCallback() {
                    @Override
                    public void onProfileLoaded(UserProfile profile) {
                        callback.onSuccess(profile);
                    }
                    
                    @Override
                    public void onError(String error) {
                        Log.e(TAG, "Failed to load user profile after sign in: " + error);
                        // Create basic profile from Firebase Auth user
                        UserProfile basicProfile = new UserProfile(
                            firebaseUser.getUid(),
                            firebaseUser.getEmail(),
                            firebaseUser.getDisplayName(),
                            null, // phone
                            ROLE_USER, // default role
                            firebaseUser.isEmailVerified(),
                            System.currentTimeMillis()
                        );
                        callback.onSuccess(basicProfile);
                    }
                });
            })
            .addOnFailureListener(e -> {
                Log.w(TAG, "Sign in failed", e);
                AuthError error = mapFirebaseAuthError(e);
                callback.onError(error, e.getMessage());
            });
    }

    public void signOut() {
        firebaseAuth.signOut();
    }

    public void updateProfile(String displayName, String phone, ProfileUpdateCallback callback) {
        FirebaseUser firebaseUser = firebaseAuth.getCurrentUser();
        if (firebaseUser == null) {
            callback.onError(AuthError.USER_NOT_FOUND, "No user logged in");
            return;
        }

        // Update Firebase Auth profile
        UserProfileChangeRequest profileUpdates = new UserProfileChangeRequest.Builder()
            .setDisplayName(displayName)
            .build();

        firebaseUser.updateProfile(profileUpdates)
            .addOnSuccessListener(aVoid -> {
                // Update Firestore profile
                loadUserProfileFromFirestore(firebaseUser.getUid(), new ProfileCallback() {
                    @Override
                    public void onProfileLoaded(UserProfile profile) {
                        if (profile != null) {
                            UserProfile updatedProfile = new UserProfile(
                                profile.getUid(),
                                profile.getEmail(),
                                displayName,
                                phone,
                                profile.getRole(),
                                profile.isEmailVerified(),
                                profile.getCreatedAt(),
                                profile.getAvatarUri(),
                                profile.getLocation(),
                                profile.getBio()
                            );

                            saveUserProfileToFirestore(updatedProfile, new OnCompleteListener<Void>() {
                                @Override
                                public void onComplete(@NonNull Task<Void> task) {
                                    if (task.isSuccessful()) {
                                        callback.onSuccess();
                                    } else {
                                        callback.onError(AuthError.UNKNOWN_ERROR, "Failed to update profile");
                                    }
                                }
                            });
                        } else {
                            callback.onError(AuthError.USER_NOT_FOUND, "Profile not found");
                        }
                    }
                    
                    @Override
                    public void onError(String error) {
                        callback.onError(AuthError.UNKNOWN_ERROR, error);
                    }
                });
            })
            .addOnFailureListener(e -> {
                Log.w(TAG, "Failed to update profile", e);
                callback.onError(AuthError.UNKNOWN_ERROR, e.getMessage());
            });
    }

    public void sendPasswordResetEmail(String email, PasswordResetCallback callback) {
        firebaseAuth.sendPasswordResetEmail(email)
            .addOnSuccessListener(aVoid -> callback.onSuccess())
            .addOnFailureListener(e -> {
                Log.w(TAG, "Failed to send password reset email", e);
                AuthError error = mapFirebaseAuthError(e);
                callback.onError(error, e.getMessage());
            });
    }

    public void sendEmailVerification(EmailVerificationCallback callback) {
        FirebaseUser firebaseUser = firebaseAuth.getCurrentUser();
        if (firebaseUser == null) {
            callback.onError(AuthError.USER_NOT_FOUND, "No user logged in");
            return;
        }

        sendEmailVerificationInternal(firebaseUser);
        callback.onSuccess();
    }

    private void sendEmailVerificationInternal(FirebaseUser firebaseUser) {
        firebaseUser.sendEmailVerification()
            .addOnSuccessListener(aVoid -> Log.d(TAG, "Verification email sent"))
            .addOnFailureListener(e -> Log.w(TAG, "Failed to send verification email", e));
    }

    public void getCurrentUserProfile(ProfileCallback callback) {
        FirebaseUser firebaseUser = firebaseAuth.getCurrentUser();
        if (firebaseUser == null) {
            callback.onProfileLoaded(null);
            return;
        }

        loadUserProfileFromFirestore(firebaseUser.getUid(), callback);
    }

    private void loadUserProfileFromFirestore(String uid, ProfileCallback callback) {
        firestore.collection(COLLECTION_USERS).document(uid).get()
            .addOnSuccessListener(documentSnapshot -> {
                UserProfile profile = UserProfile.fromDocument(documentSnapshot);
                callback.onProfileLoaded(profile);
            })
            .addOnFailureListener(e -> {
                Log.e(TAG, "Failed to load user profile", e);
                callback.onError(e.getMessage());
            });
    }

    private void saveUserProfileToFirestore(UserProfile profile, OnCompleteListener<Void> callback) {
        // Check if Firestore is available
        try {
            firestore.collection(COLLECTION_USERS).document(profile.getUid())
                .set(profile.toMap(), SetOptions.merge())
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful()) {
                        Log.d(TAG, "User profile saved successfully to Firestore");
                        callback.onComplete(task);
                    } else {
                        Log.w(TAG, "Failed to save user profile to Firestore, but continuing with registration", task.getException());
                        // Allow registration to continue even if Firestore fails
                        callback.onComplete(com.google.android.gms.tasks.Tasks.forResult(null));
                    }
                });
        } catch (Exception e) {
            Log.w(TAG, "Firestore not available, but continuing with registration", e);
            // Allow registration to continue even if Firestore is not available
            callback.onComplete(com.google.android.gms.tasks.Tasks.forResult(null));
        }
    }

    private AuthError mapFirebaseAuthError(Exception e) {
        String message = e.getMessage();
        if (message == null) return AuthError.UNKNOWN_ERROR;

        Log.e(TAG, "Firebase Auth Error: " + message);

        for (AuthError error : AuthError.values()) {
            if (message.contains(error.getFirebaseCode())) {
                return error;
            }
        }
        
        if (message.contains("network") || message.contains("connection") || 
            message.contains("offline") || message.contains("timeout")) {
            return AuthError.NETWORK_ERROR;
        }
        
        if (message.contains("permission-denied") || message.contains("permission denied")) {
            return AuthError.UNKNOWN_ERROR;
        }
        
        return AuthError.UNKNOWN_ERROR;
    }
}
