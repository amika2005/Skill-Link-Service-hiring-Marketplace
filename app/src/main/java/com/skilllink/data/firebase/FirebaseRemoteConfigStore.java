package com.skilllink.data.firebase;

import android.text.TextUtils;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.Source;

public class FirebaseRemoteConfigStore {

    private static final String COLLECTION_CONFIG = "config";
    private static final String DOCUMENT_PAYMENTS = "payments";
    private static final String FIELD_STRIPE_BACKEND_URL = "stripeBackendUrl";

    private final FirebaseFirestore firestore;

    private FirebaseRemoteConfigStore() {
        firestore = FirebaseFirestore.getInstance();
    }

    private static class Holder {
        private static final FirebaseRemoteConfigStore INSTANCE = new FirebaseRemoteConfigStore();
    }

    public static FirebaseRemoteConfigStore getInstance() {
        return Holder.INSTANCE;
    }

    public void fetchStripeBackendUrl(@NonNull StripeConfigCallback callback) {
        firestore.collection(COLLECTION_CONFIG)
                .document(DOCUMENT_PAYMENTS)
                .get(Source.SERVER)
                .addOnSuccessListener(snapshot -> {
                    if (snapshot == null || !snapshot.exists()) {
                        callback.onSuccess(null);
                        return;
                    }
                    String value = snapshot.getString(FIELD_STRIPE_BACKEND_URL);
                    callback.onSuccess(!TextUtils.isEmpty(value) ? value : null);
                })
                .addOnFailureListener(callback::onError);
    }

    public interface StripeConfigCallback {
        void onSuccess(@Nullable String backendUrl);

        void onError(@NonNull Exception exception);
    }
}
