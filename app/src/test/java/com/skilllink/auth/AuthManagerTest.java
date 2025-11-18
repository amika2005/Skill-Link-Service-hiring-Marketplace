package com.skilllink.auth;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.assertFalse;

import android.content.Context;

import androidx.test.core.app.ApplicationProvider;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.annotation.Config;

@RunWith(RobolectricTestRunner.class)
@Config(sdk = android.os.Build.VERSION_CODES.UPSIDE_DOWN_CAKE)
public class AuthManagerTest {

    private static final String ROLE_USER = "user";
    private static final String PREF_NAME = "skilllink_auth_store";

    private Context context;

    @Before
    public void setUp() {
        context = ApplicationProvider.getApplicationContext();
        context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE).edit().clear().apply();
    }

    @Test
    public void registerAndAuthenticateUser() {
        AuthManager manager = new AuthManager(context);

        AuthManager.RegistrationResult registration = manager.register(
                ROLE_USER,
                "test@example.com",
                "password123",
                "Test User",
                "0771234567"
        );

        assertTrue(registration.isSuccess());
        assertNotNull(registration.getProfile());

        AuthManager.AuthResult authResult = manager.authenticate(
                ROLE_USER,
                "test@example.com",
                "password123"
        );

        assertTrue(authResult.isSuccess());
        assertNotNull(authResult.getProfile());
        assertEquals("test@example.com", authResult.getProfile().getEmail());
        assertEquals("Test User", authResult.getProfile().getDisplayName());
    }

    @Test
    public void duplicateRegistrationFails() {
        AuthManager manager = new AuthManager(context);
        assertTrue(manager.register(ROLE_USER, "dup@example.com", "password", null, null).isSuccess());

        AuthManager.RegistrationResult duplicate = manager.register(
                ROLE_USER,
                "dup@example.com",
                "password",
                null,
                null
        );

        assertFalse(duplicate.isSuccess());
        assertEquals(AuthManager.RegistrationError.ALREADY_EXISTS, duplicate.getError());
    }

    @Test
    public void authenticationFailsWithWrongPassword() {
        AuthManager manager = new AuthManager(context);
        assertTrue(manager.register(ROLE_USER, "secure@example.com", "password", null, null).isSuccess());

        AuthManager.AuthResult result = manager.authenticate(
                ROLE_USER,
                "secure@example.com",
                "wrongpass"
        );

        assertFalse(result.isSuccess());
        assertEquals(AuthManager.AuthError.INVALID_CREDENTIALS, result.getError());
    }
}
