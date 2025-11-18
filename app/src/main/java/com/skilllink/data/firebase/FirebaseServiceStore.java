package com.skilllink.data.firebase;

import android.text.TextUtils;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.ListenerRegistration;
import com.google.firebase.firestore.Query;
import com.google.firebase.firestore.SetOptions;
import com.google.firebase.firestore.WriteBatch;
import com.skilllink.BuildConfig;
import com.skilllink.model.WorkerService;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class FirebaseServiceStore {

    private static final String COLLECTION_WORKERS = "workers";
    private static final String COLLECTION_SERVICES = "services";
    private static final String SUBCOLLECTION_SERVICES = "services";
    private static final String FIELD_UPDATED_AT = "updatedAt";

    private final FirebaseFirestore firestore;
    private final boolean enabled;

    private FirebaseServiceStore() {
        enabled = BuildConfig.FIRESTORE_ENABLED;
        firestore = enabled ? FirebaseFirestore.getInstance() : null;
    }

    private static class Holder {
        private static final FirebaseServiceStore INSTANCE = new FirebaseServiceStore();
    }

    public static FirebaseServiceStore getInstance() {
        return Holder.INSTANCE;
    }

    public boolean isEnabled() {
        return enabled;
    }

    public void upsertService(@NonNull String workerId,
                              @Nullable String workerName,
                              @Nullable String workerEmail,
                              @NonNull WorkerService service,
                              @Nullable CompletionListener listener) {
        if (!enabled) {
            if (listener != null) {
                listener.onSuccess();
            }
            return;
        }

        Map<String, Object> payload = createPayload(workerId, workerName, workerEmail, service);

        WriteBatch batch = firestore.batch();
        DocumentReference workerDoc = firestore.collection(COLLECTION_WORKERS)
                .document(workerId)
                .collection(SUBCOLLECTION_SERVICES)
                .document(service.getId());
        DocumentReference globalDoc = firestore.collection(COLLECTION_SERVICES)
                .document(service.getId());

        batch.set(workerDoc, payload, SetOptions.merge());
        batch.set(globalDoc, payload, SetOptions.merge());

        batch.commit()
                .addOnSuccessListener(unused -> {
                    if (listener != null) {
                        listener.onSuccess();
                    }
                })
                .addOnFailureListener(error -> {
                    if (listener != null) {
                        listener.onError(error);
                    }
                });
    }

    public void deleteService(@NonNull String workerId,
                              @NonNull String serviceId,
                              @Nullable CompletionListener listener) {
        if (!enabled) {
            if (listener != null) {
                listener.onSuccess();
            }
            return;
        }

        WriteBatch batch = firestore.batch();
        DocumentReference workerDoc = firestore.collection(COLLECTION_WORKERS)
                .document(workerId)
                .collection(SUBCOLLECTION_SERVICES)
                .document(serviceId);
        DocumentReference globalDoc = firestore.collection(COLLECTION_SERVICES)
                .document(serviceId);

        batch.delete(workerDoc);
        batch.delete(globalDoc);

        batch.commit()
                .addOnSuccessListener(unused -> {
                    if (listener != null) {
                        listener.onSuccess();
                    }
                })
                .addOnFailureListener(error -> {
                    if (listener != null) {
                        listener.onError(error);
                    }
                });
    }

    public void refreshAllServices(@NonNull ServiceListCallback callback) {
        if (!enabled) {
            callback.onError(new IllegalStateException("Firestore integration disabled"));
            return;
        }

        firestore.collection(COLLECTION_SERVICES)
                .orderBy(FIELD_UPDATED_AT, Query.Direction.DESCENDING)
                .get()
                .addOnSuccessListener(snapshot -> {
                    List<WorkerService> services = new ArrayList<>();
                    if (snapshot != null) {
                        for (DocumentSnapshot document : snapshot.getDocuments()) {
                            WorkerService service = parseService(document);
                            if (service != null) {
                                services.add(service);
                            }
                        }
                    }
                    callback.onSuccess(services);
                })
                .addOnFailureListener(callback::onError);
    }

    public ListenerRegistration listenToAllServices(@NonNull ServiceListener listener) {
        if (!enabled) {
            listener.onError(new IllegalStateException("Firestore integration disabled"));
            return new NoopRegistration();
        }

        return firestore.collection(COLLECTION_SERVICES)
                .orderBy(FIELD_UPDATED_AT, Query.Direction.DESCENDING)
                .addSnapshotListener((snapshots, error) -> {
                    if (error != null) {
                        listener.onError(error);
                        return;
                    }

                    if (snapshots == null) {
                        listener.onServicesChanged(Collections.emptyList());
                        return;
                    }

                    List<WorkerService> services = new ArrayList<>();
                    for (DocumentSnapshot document : snapshots.getDocuments()) {
                        WorkerService service = parseService(document);
                        if (service != null) {
                            services.add(service);
                        }
                    }
                    listener.onServicesChanged(services);
                });
    }

    private Map<String, Object> createPayload(String workerId,
                                              @Nullable String workerName,
                                              @Nullable String workerEmail,
                                              WorkerService service) {
        Map<String, Object> data = new HashMap<>();
        data.put("id", service.getId());
        data.put("category", service.getCategory());
        data.put("name", service.getName());
        data.put("bio", service.getBio());
        data.put("priceType", service.getPriceType());
        data.put("priceValue", service.getPriceValue());
        data.put("imageUri", service.getImageUri());

        String resolvedOwnerId = !TextUtils.isEmpty(service.getOwnerId()) ? service.getOwnerId() : workerId;
        String resolvedOwnerName = !TextUtils.isEmpty(service.getOwnerName()) ? service.getOwnerName() : workerName;
        String resolvedOwnerEmail = !TextUtils.isEmpty(service.getOwnerEmail()) ? service.getOwnerEmail() : workerEmail;

        data.put("ownerId", resolvedOwnerId);
        data.put("ownerName", resolvedOwnerName);
        data.put("ownerEmail", resolvedOwnerEmail);

        // Include location data
        data.put("serviceArea", service.getServiceArea());
        data.put("latitude", service.getLatitude());
        data.put("longitude", service.getLongitude());
        data.put("coverageRadiusKm", service.getCoverageRadiusKm());

        long updatedAt = service.getUpdatedAt() > 0 ? service.getUpdatedAt() : System.currentTimeMillis();
        data.put(FIELD_UPDATED_AT, updatedAt);

        return data;
    }

    @Nullable
    private WorkerService parseService(@Nullable DocumentSnapshot document) {
        if (document == null || !document.exists()) {
            return null;
        }

        String id = document.getString("id");
        if (TextUtils.isEmpty(id)) {
            id = document.getId();
        }

        String category = document.getString("category");
        String name = document.getString("name");
        String bio = document.getString("bio");
        String priceType = document.getString("priceType");
        String priceValue = document.getString("priceValue");
        String imageUri = document.getString("imageUri");
        String ownerId = document.getString("ownerId");
        String ownerName = document.getString("ownerName");
        String ownerEmail = document.getString("ownerEmail");
        String serviceArea = document.getString("serviceArea");
        Double latitude = document.getDouble("latitude");
        Double longitude = document.getDouble("longitude");
        Double coverageRadiusKm = document.getDouble("coverageRadiusKm");

        Long updated = document.getLong(FIELD_UPDATED_AT);
        long updatedAt = updated != null ? updated : 0;

        return new WorkerService(id, category, name, bio, priceType, priceValue, imageUri, ownerId, ownerName, ownerEmail, serviceArea, latitude != null ? latitude : Double.NaN, longitude != null ? longitude : Double.NaN, coverageRadiusKm != null ? coverageRadiusKm : 20d, updatedAt);
    }

    public interface CompletionListener {
        void onSuccess();

        void onError(Exception exception);
    }

    public interface ServiceListCallback {
        void onSuccess(List<WorkerService> services);

        void onError(Exception exception);
    }

    public interface ServiceListener {
        void onServicesChanged(List<WorkerService> services);

        void onError(Exception exception);
    }

    private static final class NoopRegistration implements ListenerRegistration {
        @Override
        public void remove() {
            // no-op
        }
    }
}
