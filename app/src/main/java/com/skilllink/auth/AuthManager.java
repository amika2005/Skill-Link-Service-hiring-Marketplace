package com.skilllink.auth;

import android.content.Context;
import android.content.SharedPreferences;
import android.text.TextUtils;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import org.json.JSONException;
import org.json.JSONObject;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.util.Locale;

public final class AuthManager {

    private static final String PREF_NAME = "skilllink_auth_store";
    private static final String KEY_PREFIX = "account_";
    private static final int SALT_LENGTH = 32;

    private final SharedPreferences preferences;
    private final SecureRandom secureRandom = new SecureRandom();

    public AuthManager(@NonNull Context context) {
        this.preferences = context.getApplicationContext()
                .getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE);
    }

    @NonNull
    public RegistrationResult register(@NonNull String role,
                                       @NonNull String email,
                                       @NonNull String password,
                                       @Nullable String displayName,
                                       @Nullable String phone) {
        String normalizedEmail = normalizeEmail(email);
        if (normalizedEmail == null || TextUtils.isEmpty(password)) {
            return RegistrationResult.error(RegistrationError.INVALID_INPUT);
        }

        String key = buildAccountKey(role, normalizedEmail);
        synchronized (this) {
            if (preferences.contains(key)) {
                return RegistrationResult.error(RegistrationError.ALREADY_EXISTS);
            }

            String salt = generateSalt();
            String hash = hashPassword(password, salt);
            if (hash == null) {
                return RegistrationResult.error(RegistrationError.UNKNOWN_FAILURE);
            }

            JSONObject payload = new JSONObject();
            try {
                payload.put("role", role);
                payload.put("email", normalizedEmail);
                payload.put("displayName", displayName);
                payload.put("phone", phone);
                payload.put("salt", salt);
                payload.put("hash", hash);
            } catch (JSONException ignored) {
                return RegistrationResult.error(RegistrationError.UNKNOWN_FAILURE);
            }

            preferences.edit().putString(key, payload.toString()).apply();
            AccountProfile profile = new AccountProfile(role, normalizedEmail, displayName, phone);
            return RegistrationResult.success(profile);
        }
    }

    @NonNull
    public AuthResult authenticate(@NonNull String role,
                                   @NonNull String email,
                                   @NonNull String password) {
        String normalizedEmail = normalizeEmail(email);
        if (normalizedEmail == null) {
            return AuthResult.error(AuthError.NOT_FOUND);
        }

        String key = buildAccountKey(role, normalizedEmail);
        String raw = preferences.getString(key, null);
        if (raw == null) {
            return AuthResult.error(AuthError.NOT_FOUND);
        }

        try {
            JSONObject payload = new JSONObject(raw);
            String salt = payload.optString("salt", null);
            String expectedHash = payload.optString("hash", null);
            if (TextUtils.isEmpty(salt) || TextUtils.isEmpty(expectedHash)) {
                preferences.edit().remove(key).apply();
                return AuthResult.error(AuthError.NOT_FOUND);
            }

            String actualHash = hashPassword(password, salt);
            if (actualHash == null || !constantTimeEquals(expectedHash, actualHash)) {
                return AuthResult.error(AuthError.INVALID_CREDENTIALS);
            }

            AccountProfile profile = new AccountProfile(
                    role,
                    normalizedEmail,
                    payload.optString("displayName", null),
                    payload.optString("phone", null)
            );
            return AuthResult.success(profile);
        } catch (JSONException ignored) {
            preferences.edit().remove(key).apply();
            return AuthResult.error(AuthError.NOT_FOUND);
        }
    }

    private String buildAccountKey(@NonNull String role, @NonNull String normalizedEmail) {
        return KEY_PREFIX + role.toLowerCase(Locale.US) + "_" + normalizedEmail;
    }

    @Nullable
    private String normalizeEmail(@Nullable String email) {
        if (email == null) {
            return null;
        }
        String trimmed = email.trim().toLowerCase(Locale.US);
        return trimmed.isEmpty() ? null : trimmed;
    }

    @Nullable
    private String hashPassword(@NonNull String password, @NonNull String salt) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            digest.update(salt.getBytes(StandardCharsets.UTF_8));
            byte[] hashed = digest.digest(password.getBytes(StandardCharsets.UTF_8));
            return bytesToHex(hashed);
        } catch (NoSuchAlgorithmException ignored) {
            return null;
        }
    }

    @NonNull
    private String generateSalt() {
        byte[] buffer = new byte[SALT_LENGTH];
        secureRandom.nextBytes(buffer);
        return bytesToHex(buffer);
    }

    private boolean constantTimeEquals(@NonNull String expected, @NonNull String actual) {
        if (expected.length() != actual.length()) {
            return false;
        }
        int result = 0;
        for (int i = 0; i < expected.length(); i++) {
            result |= expected.charAt(i) ^ actual.charAt(i);
        }
        return result == 0;
    }

    @NonNull
    private String bytesToHex(@NonNull byte[] bytes) {
        StringBuilder builder = new StringBuilder(bytes.length * 2);
        for (byte b : bytes) {
            builder.append(String.format(Locale.US, "%02x", b));
        }
        return builder.toString();
    }

    public static final class AccountProfile {
        private final String role;
        private final String email;
        private final String displayName;
        private final String phone;

        AccountProfile(String role, String email, String displayName, String phone) {
            this.role = role;
            this.email = email;
            this.displayName = displayName;
            this.phone = phone;
        }

        @NonNull
        public String getRole() {
            return role;
        }

        @NonNull
        public String getEmail() {
            return email;
        }

        @Nullable
        public String getDisplayName() {
            return displayName;
        }

        @Nullable
        public String getPhone() {
            return phone;
        }
    }

    public enum RegistrationError {
        ALREADY_EXISTS,
        INVALID_INPUT,
        UNKNOWN_FAILURE
    }

    public static final class RegistrationResult {
        private final boolean success;
        private final RegistrationError error;
        private final AccountProfile profile;

        private RegistrationResult(boolean success, RegistrationError error, AccountProfile profile) {
            this.success = success;
            this.error = error;
            this.profile = profile;
        }

        public static RegistrationResult success(AccountProfile profile) {
            return new RegistrationResult(true, null, profile);
        }

        public static RegistrationResult error(RegistrationError error) {
            return new RegistrationResult(false, error, null);
        }

        public boolean isSuccess() {
            return success;
        }

        @Nullable
        public RegistrationError getError() {
            return error;
        }

        @Nullable
        public AccountProfile getProfile() {
            return profile;
        }
    }

    public enum AuthError {
        NOT_FOUND,
        INVALID_CREDENTIALS
    }

    public static final class AuthResult {
        private final boolean success;
        private final AuthError error;
        private final AccountProfile profile;

        private AuthResult(boolean success, AuthError error, AccountProfile profile) {
            this.success = success;
            this.error = error;
            this.profile = profile;
        }

        public static AuthResult success(AccountProfile profile) {
            return new AuthResult(true, null, profile);
        }

        public static AuthResult error(AuthError error) {
            return new AuthResult(false, error, null);
        }

        public boolean isSuccess() {
            return success;
        }

        @Nullable
        public AuthError getError() {
            return error;
        }

        @Nullable
        public AccountProfile getProfile() {
            return profile;
        }
    }
}
