package com.skilllink.network;

/**
 * Legacy Stripe client retained for backward compatibility references. The active
 * payment flow now uses PayHere and this class should no longer be instantiated.
 */
public final class StripeApiClient {

    private StripeApiClient() {
        throw new UnsupportedOperationException("Stripe integration has been removed in favour of PayHere.");
    }
}
