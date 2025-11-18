package com.skilllink.util;

import android.util.Log;

import com.google.firebase.firestore.CollectionReference;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.Query;
import com.google.firebase.firestore.SetOptions;
import com.skilllink.BuildConfig;
import com.skilllink.model.WorkerService;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Utility class to recover deleted worker document fields in Firebase
 */
public class FirebaseWorkerRecoveryUtility {
    private static final String TAG = "FirebaseWorkerRecovery";
    
    // Collections
    private static final String COLLECTION_USERS = "users";
    private static final String COLLECTION_WORKERS = "workers";
    private static final String COLLECTION_SERVICES = "services";
    private static final String SUBCOLLECTION_SERVICES = "services";
    
    // Required worker document fields
    private static final String[] REQUIRED_WORKER_FIELDS = {
        "uid", "email", "displayName", "role", "createdAt", "phone",
        "emailVerified", "avatarUri", "location", "bio"
    };
    
    // Required service document fields
    private static final String[] REQUIRED_SERVICE_FIELDS = {
        "id", "category", "name", "bio", "priceType", "priceValue",
        "imageUri", "ownerId", "ownerName", "ownerEmail", "updatedAt",
        "serviceArea", "latitude", "longitude", "coverageRadiusKm"
    };
    
    private final FirebaseFirestore firestore;
    private final boolean enabled;
    
    public FirebaseWorkerRecoveryUtility() {
        enabled = BuildConfig.FIRESTORE_ENABLED;
        firestore = enabled ? FirebaseFirestore.getInstance() : null;
    }
    
    /**
     * Recovers all missing fields for worker documents
     */
    public void recoverAllWorkerFields(RecoveryCallback callback) {
        if (!enabled) {
            callback.onError(new IllegalStateException("Firestore integration disabled"));
            return;
        }
        
        Log.d(TAG, "Starting worker field recovery process");
        
        // Get all worker users
        firestore.collection(COLLECTION_USERS)
                .whereEqualTo("role", "WORKER")
                .get()
                .addOnSuccessListener(workerSnapshot -> {
                    Log.d(TAG, "Found " + workerSnapshot.size() + " workers to process");
                    int[] processedCount = {0};
                    int totalCount = workerSnapshot.size();
                    
                    if (totalCount == 0) {
                        callback.onSuccess("No workers found to recover");
                        return;
                    }
                    
                    for (DocumentSnapshot workerDoc : workerSnapshot.getDocuments()) {
                        String workerId = workerDoc.getId();
                        Log.d(TAG, "Processing worker: " + workerId);
                        
                        recoverSingleWorker(workerId, workerDoc, new RecoveryCallback() {
                            @Override
                            public void onSuccess(String message) {
                                synchronized (this) {
                                    processedCount[0]++;
                                    Log.d(TAG, "Worker " + workerId + " recovered. Progress: " + processedCount[0] + "/" + totalCount);
                                    
                                    if (processedCount[0] == totalCount) {
                                        callback.onSuccess("Recovered " + totalCount + " workers successfully");
                                    }
                                }
                            }
                            
                            @Override
                            public void onError(Exception exception) {
                                synchronized (this) {
                                    processedCount[0]++;
                                    Log.e(TAG, "Failed to recover worker " + workerId, exception);
                                    
                                    if (processedCount[0] == totalCount) {
                                        callback.onSuccess("Recovered " + (processedCount[0] - 1) + " workers with some errors");
                                    }
                                }
                            }
                        });
                    }
                })
                .addOnFailureListener(callback::onError);
    }
    
    /**
     * Recovers a single worker and their services
     */
    private void recoverSingleWorker(String workerId, DocumentSnapshot workerDoc, RecoveryCallback callback) {
        // Recover main user document fields
        recoverUserDocumentFields(workerId, workerDoc, new RecoveryCallback() {
            @Override
            public void onSuccess(String message) {
                // Recover worker services
                recoverWorkerServices(workerId, callback);
            }
            
            @Override
            public void onError(Exception exception) {
                callback.onError(exception);
            }
        });
    }
    
    /**
     * Recovers missing fields for a specific worker user document
     */
    private void recoverUserDocumentFields(String workerId, DocumentSnapshot currentDoc, RecoveryCallback callback) {
        Map<String, Object> missingFields = new HashMap<>();
        
        // Check each required field
        for (String field : REQUIRED_WORKER_FIELDS) {
            if (!currentDoc.contains(field) || currentDoc.get(field) == null) {
                Object defaultValue = getDefaultValueForField(field, currentDoc);
                if (defaultValue != null) {
                    missingFields.put(field, defaultValue);
                    Log.d(TAG, "Added missing field '" + field + "' for worker " + workerId);
                }
            }
        }
        
        // Update document if there are missing fields
        if (!missingFields.isEmpty()) {
            DocumentReference workerRef = firestore.collection(COLLECTION_USERS).document(workerId);
            workerRef.update(missingFields)
                    .addOnSuccessListener(aVoid -> callback.onSuccess("User fields recovered"))
                    .addOnFailureListener(callback::onError);
        } else {
            callback.onSuccess("No missing user fields");
        }
    }
    
    /**
     * Recovers worker services and their missing fields
     */
    private void recoverWorkerServices(String workerId, RecoveryCallback callback) {
        // Get worker's services from global collection
        firestore.collection(COLLECTION_SERVICES)
                .whereEqualTo("ownerId", workerId)
                .get()
                .addOnSuccessListener(servicesSnapshot -> {
                    if (servicesSnapshot.isEmpty()) {
                        callback.onSuccess("No services found for worker");
                        return;
                    }
                    
                    int[] processedCount = {0};
                    int totalCount = servicesSnapshot.size();
                    
                    for (DocumentSnapshot serviceDoc : servicesSnapshot.getDocuments()) {
                        recoverServiceDocumentFields(serviceDoc, workerId, new RecoveryCallback() {
                            @Override
                            public void onSuccess(String message) {
                                // Ensure service exists in worker's subcollection
                                ensureServiceInWorkerSubcollection(serviceDoc, workerId, new RecoveryCallback() {
                                    @Override
                                    public void onSuccess(String message) {
                                        synchronized (this) {
                                            processedCount[0]++;
                                            if (processedCount[0] == totalCount) {
                                                callback.onSuccess("All services recovered for worker");
                                            }
                                        }
                                    }
                                    
                                    @Override
                                    public void onError(Exception exception) {
                                        synchronized (this) {
                                            processedCount[0]++;
                                            if (processedCount[0] == totalCount) {
                                                callback.onSuccess("Services recovered with some errors");
                                            }
                                        }
                                    }
                                });
                            }
                            
                            @Override
                            public void onError(Exception exception) {
                                synchronized (this) {
                                    processedCount[0]++;
                                    if (processedCount[0] == totalCount) {
                                        callback.onSuccess("Services recovered with some errors");
                                    }
                                }
                            }
                        });
                    }
                })
                .addOnFailureListener(callback::onError);
    }
    
    /**
     * Recovers missing fields for a service document
     */
    private void recoverServiceDocumentFields(DocumentSnapshot serviceDoc, String workerId, RecoveryCallback callback) {
        Map<String, Object> missingFields = new HashMap<>();
        
        // Check each required field
        for (String field : REQUIRED_SERVICE_FIELDS) {
            if (!serviceDoc.contains(field) || serviceDoc.get(field) == null) {
                Object defaultValue = getDefaultValueForServiceField(field, serviceDoc, workerId);
                if (defaultValue != null) {
                    missingFields.put(field, defaultValue);
                    Log.d(TAG, "Added missing service field '" + field + "' for service " + serviceDoc.getId());
                }
            }
        }
        
        // Update document if there are missing fields
        if (!missingFields.isEmpty()) {
            DocumentReference serviceRef = firestore.collection(COLLECTION_SERVICES).document(serviceDoc.getId());
            serviceRef.update(missingFields)
                    .addOnSuccessListener(aVoid -> callback.onSuccess("Service fields recovered"))
                    .addOnFailureListener(callback::onError);
        } else {
            callback.onSuccess("No missing service fields");
        }
    }
    
    /**
     * Ensures service exists in worker's subcollection
     */
    private void ensureServiceInWorkerSubcollection(DocumentSnapshot serviceDoc, String workerId, RecoveryCallback callback) {
        DocumentReference workerServiceRef = firestore.collection(COLLECTION_WORKERS)
                .document(workerId)
                .collection(SUBCOLLECTION_SERVICES)
                .document(serviceDoc.getId());
        
        // Check if service exists in worker subcollection
        workerServiceRef.get()
                .addOnSuccessListener(workerServiceDoc -> {
                    if (!workerServiceDoc.exists()) {
                        // Copy service to worker subcollection
                        Map<String, Object> serviceData = serviceDoc.getData();
                        if (serviceData != null) {
                            workerServiceRef.set(serviceData)
                                    .addOnSuccessListener(aVoid -> callback.onSuccess("Service added to worker subcollection"))
                                    .addOnFailureListener(callback::onError);
                        } else {
                            callback.onSuccess("Service data was null");
                        }
                    } else {
                        // Check and fix missing fields in subcollection
                        recoverServiceDocumentFields(workerServiceDoc, workerId, callback);
                    }
                })
                .addOnFailureListener(callback::onError);
    }
    
    /**
     * Gets default value for a missing user field
     */
    private Object getDefaultValueForField(String field, DocumentSnapshot currentDoc) {
        switch (field) {
            case "uid":
                return currentDoc.getId();
            case "email":
                return currentDoc.getString("email");
            case "displayName":
                return currentDoc.getString("displayName");
            case "role":
                return "WORKER";
            case "createdAt":
                return currentDoc.getLong("createdAt") != null ? 
                    currentDoc.getLong("createdAt") : System.currentTimeMillis();
            case "phone":
                return currentDoc.getString("phone");
            case "emailVerified":
                return currentDoc.getBoolean("emailVerified") != null ? 
                    currentDoc.getBoolean("emailVerified") : false;
            case "avatarUri":
                return currentDoc.getString("avatarUri");
            case "location":
                return currentDoc.getString("location");
            case "bio":
                return currentDoc.getString("bio");
            default:
                return null;
        }
    }
    
    /**
     * Gets default value for a missing service field
     */
    private Object getDefaultValueForServiceField(String field, DocumentSnapshot currentDoc, String workerId) {
        switch (field) {
            case "id":
                return currentDoc.getId();
            case "category":
                return currentDoc.getString("category") != null ? 
                    currentDoc.getString("category") : "general";
            case "name":
                return currentDoc.getString("name") != null ? 
                    currentDoc.getString("name") : "Service";
            case "bio":
                return currentDoc.getString("bio") != null ? 
                    currentDoc.getString("bio") : "Professional service";
            case "priceType":
                return currentDoc.getString("priceType") != null ? 
                    currentDoc.getString("priceType") : "CUSTOM";
            case "priceValue":
                return currentDoc.getString("priceValue") != null ? 
                    currentDoc.getString("priceValue") : "0";
            case "imageUri":
                return currentDoc.getString("imageUri");
            case "ownerId":
                return workerId;
            case "ownerName":
                return currentDoc.getString("ownerName");
            case "ownerEmail":
                return currentDoc.getString("ownerEmail");
            case "updatedAt":
                return currentDoc.getLong("updatedAt") != null ? 
                    currentDoc.getLong("updatedAt") : System.currentTimeMillis();
            case "serviceArea":
                return currentDoc.getString("serviceArea") != null ? 
                    currentDoc.getString("serviceArea") : "Colombo";
            case "latitude":
                return currentDoc.getDouble("latitude") != null ? 
                    currentDoc.getDouble("latitude") : 6.927079;
            case "longitude":
                return currentDoc.getDouble("longitude") != null ? 
                    currentDoc.getDouble("longitude") : 79.861244;
            case "coverageRadiusKm":
                return currentDoc.getDouble("coverageRadiusKm") != null ? 
                    currentDoc.getDouble("coverageRadiusKm") : 20.0;
            default:
                return null;
        }
    }
    
    /**
     * Recovers a specific worker by ID
     */
    public void recoverSpecificWorker(String workerId, RecoveryCallback callback) {
        if (!enabled) {
            callback.onError(new IllegalStateException("Firestore integration disabled"));
            return;
        }
        
        firestore.collection(COLLECTION_USERS).document(workerId).get()
                .addOnSuccessListener(workerDoc -> {
                    if (workerDoc.exists()) {
                        recoverSingleWorker(workerId, workerDoc, callback);
                    } else {
                        callback.onError(new IllegalArgumentException("Worker not found: " + workerId));
                    }
                })
                .addOnFailureListener(callback::onError);
    }
    
    /**
     * Validates all worker documents and reports missing fields
     */
    public void validateWorkerDocuments(ValidationCallback callback) {
        if (!enabled) {
            callback.onError(new IllegalStateException("Firestore integration disabled"));
            return;
        }
        
        firestore.collection(COLLECTION_USERS)
                .whereEqualTo("role", "WORKER")
                .get()
                .addOnSuccessListener(workerSnapshot -> {
                    List<ValidationReport> reports = new ArrayList<>();
                    
                    for (DocumentSnapshot workerDoc : workerSnapshot.getDocuments()) {
                        ValidationReport report = validateWorkerDocument(workerDoc);
                        reports.add(report);
                    }
                    
                    callback.onSuccess(reports);
                })
                .addOnFailureListener(callback::onError);
    }
    
    /**
     * Validates a single worker document
     */
    private ValidationReport validateWorkerDocument(DocumentSnapshot workerDoc) {
        ValidationReport report = new ValidationReport(workerDoc.getId());
        
        // Check user document fields
        for (String field : REQUIRED_WORKER_FIELDS) {
            if (!workerDoc.contains(field) || workerDoc.get(field) == null) {
                report.addMissingUserField(field);
            }
        }
        
        // Check services
        String workerId = workerDoc.getId();
        firestore.collection(COLLECTION_SERVICES)
                .whereEqualTo("ownerId", workerId)
                .get()
                .addOnSuccessListener(servicesSnapshot -> {
                    for (DocumentSnapshot serviceDoc : servicesSnapshot.getDocuments()) {
                        for (String field : REQUIRED_SERVICE_FIELDS) {
                            if (!serviceDoc.contains(field) || serviceDoc.get(field) == null) {
                                report.addMissingServiceField(serviceDoc.getId(), field);
                            }
                        }
                    }
                });
        
        return report;
    }
    
    /**
     * Callback for recovery operations
     */
    public interface RecoveryCallback {
        void onSuccess(String message);
        void onError(Exception exception);
    }
    
    /**
     * Callback for validation operations
     */
    public interface ValidationCallback {
        void onSuccess(List<ValidationReport> reports);
        void onError(Exception exception);
    }
    
    /**
     * Validation report for a worker
     */
    public static class ValidationReport {
        private final String workerId;
        private final List<String> missingUserFields;
        private final Map<String, List<String>> missingServiceFields;
        
        public ValidationReport(String workerId) {
            this.workerId = workerId;
            this.missingUserFields = new ArrayList<>();
            this.missingServiceFields = new HashMap<>();
        }
        
        public void addMissingUserField(String field) {
            missingUserFields.add(field);
        }
        
        public void addMissingServiceField(String serviceId, String field) {
            if (!missingServiceFields.containsKey(serviceId)) {
                missingServiceFields.put(serviceId, new ArrayList<>());
            }
            missingServiceFields.get(serviceId).add(field);
        }
        
        public String getWorkerId() {
            return workerId;
        }
        
        public List<String> getMissingUserFields() {
            return missingUserFields;
        }
        
        public Map<String, List<String>> getMissingServiceFields() {
            return missingServiceFields;
        }
        
        public boolean hasIssues() {
            return !missingUserFields.isEmpty() || !missingServiceFields.isEmpty();
        }
        
        @Override
        public String toString() {
            StringBuilder sb = new StringBuilder();
            sb.append("Worker ").append(workerId);
            
            if (!missingUserFields.isEmpty()) {
                sb.append("\n  Missing user fields: ").append(missingUserFields);
            }
            
            if (!missingServiceFields.isEmpty()) {
                sb.append("\n  Missing service fields:");
                for (Map.Entry<String, List<String>> entry : missingServiceFields.entrySet()) {
                    sb.append("\n    Service ").append(entry.getKey()).append(": ").append(entry.getValue());
                }
            }
            
            return sb.toString();
        }
    }
}
