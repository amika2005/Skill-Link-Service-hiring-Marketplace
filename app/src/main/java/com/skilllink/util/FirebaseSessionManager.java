package com.skilllink.util;

import android.content.Context;
import android.text.TextUtils;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.android.gms.tasks.Tasks;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.EventListener;
import com.google.firebase.firestore.FieldValue;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.FirebaseFirestoreException;
import com.google.firebase.firestore.ListenerRegistration;
import com.google.firebase.firestore.Query;
import com.google.firebase.firestore.QuerySnapshot;
import com.google.firebase.firestore.SetOptions;
import com.google.firebase.firestore.Transaction;
import com.skilllink.auth.FirebaseAuthManager;
import com.skilllink.model.RecommendedWorker;
import com.skilllink.model.ServiceArea;
import com.skilllink.model.UserBooking;
import com.skilllink.model.UserChatMessage;
import com.skilllink.model.UserConversation;
import com.skilllink.model.WorkerJobRequest;
import com.skilllink.model.WorkerService;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Firebase-based session manager that replaces SharedPreferences with Firestore.
 * Handles all data persistence using Firebase Firestore for real-time synchronization.
 */
public class FirebaseSessionManager {

    private static final String TAG = "FirebaseSessionManager";
    
    // Firestore Collections
    private static final String COLLECTION_USERS = "users";
    private static final String COLLECTION_SERVICES = "services";
    private static final String COLLECTION_BOOKINGS = "bookings";
    private static final String COLLECTION_JOB_REQUESTS = "jobRequests";
    private static final String COLLECTION_CONVERSATIONS = "conversations";
    private static final String COLLECTION_RECOMMENDED_WORKERS = "recommendedWorkers";
    private static final String SUBCOLLECTION_MESSAGES = "messages";
    
    // User Document Fields
    private static final String FIELD_USER_PROFILE = "profile";
    private static final String FIELD_PAYMENT_CARDS = "paymentCards";
    private static final String FIELD_RECENT_AREAS = "recentAreas";
    private static final String FIELD_PAYHERE_MERCHANT_ID = "payHereMerchantId";
    private static final String FIELD_PAYHERE_MERCHANT_SECRET = "payHereMerchantSecret";
    
    private final Context appContext;
    private final FirebaseFirestore firestore;
    private final FirebaseAuth auth;
    private final FirebaseAuthManager authManager;
    
    // Real-time listeners
    private ListenerRegistration userProfileListener;
    private ListenerRegistration servicesListener;
    private ListenerRegistration bookingsListener;
    private ListenerRegistration jobRequestsListener;
    private ListenerRegistration conversationsListener;
    
    // Cached data
    private FirebaseAuthManager.UserProfile cachedUserProfile;
    private List<WorkerService> cachedServices;
    private List<UserBooking> cachedBookings;
    private List<WorkerJobRequest> cachedJobRequests;
    private List<UserConversation> cachedConversations;
    private Map<String, List<UserChatMessage>> cachedMessages;
    
    // Change listeners
    private UserProfileChangeListener userProfileChangeListener;
    private ServicesChangeListener servicesChangeListener;
    private BookingsChangeListener bookingsChangeListener;
    private JobRequestsChangeListener jobRequestsChangeListener;
    private ConversationsChangeListener conversationsChangeListener;

    public FirebaseSessionManager(Context context) {
        this.appContext = context.getApplicationContext();
        this.firestore = FirebaseFirestore.getInstance();
        this.auth = FirebaseAuth.getInstance();
        this.authManager = FirebaseAuthManager.getInstance();
        this.cachedMessages = new HashMap<>();
        
        // Initialize real-time listeners when user is authenticated
        authManager.addAuthStateListener(new FirebaseAuthManager.AuthStateListener() {
            @Override
            public void onAuthStateChanged(FirebaseAuthManager.UserProfile user) {
                if (user != null) {
                    initializeRealtimeListeners(user.getUid());
                } else {
                    clearRealtimeListeners();
                    clearCache();
                }
            }
        });
    }

    // Interface definitions for change listeners
    public interface UserProfileChangeListener {
        void onProfileChanged(FirebaseAuthManager.UserProfile profile);
    }

    public interface ServicesChangeListener {
        void onServicesChanged(List<WorkerService> services);
    }

    public interface BookingsChangeListener {
        void onBookingsChanged(List<UserBooking> bookings);
    }

    public interface JobRequestsChangeListener {
        void onJobRequestsChanged(List<WorkerJobRequest> requests);
    }

    public interface ConversationsChangeListener {
        void onConversationsChanged(List<UserConversation> conversations);
    }

    public interface MessagesChangeListener {
        void onMessagesChanged(String conversationId, List<UserChatMessage> messages);
    }

    public interface DataOperationCallback {
        void onSuccess();
        void onError(String error);
    }

    // Setters for change listeners
    public void setUserProfileChangeListener(UserProfileChangeListener listener) {
        this.userProfileChangeListener = listener;
    }

    public void setServicesChangeListener(ServicesChangeListener listener) {
        this.servicesChangeListener = listener;
    }

    public void setBookingsChangeListener(BookingsChangeListener listener) {
        this.bookingsChangeListener = listener;
    }

    public void setJobRequestsChangeListener(JobRequestsChangeListener listener) {
        this.jobRequestsChangeListener = listener;
    }

    public void setConversationsChangeListener(ConversationsChangeListener listener) {
        this.conversationsChangeListener = listener;
    }

    // Authentication methods
    public boolean isLoggedIn() {
        return authManager.isAuthenticated();
    }

    public String getUserRole() {
        FirebaseAuthManager.UserProfile profile = getCurrentUserProfile();
        return profile != null ? profile.getRole() : null;
    }

    public String getUserEmail() {
        FirebaseAuthManager.UserProfile profile = getCurrentUserProfile();
        return profile != null ? profile.getEmail() : null;
    }

    public String getUserName() {
        FirebaseAuthManager.UserProfile profile = getCurrentUserProfile();
        return profile != null ? profile.getDisplayName() : null;
    }

    public String getUserPhone() {
        FirebaseAuthManager.UserProfile profile = getCurrentUserProfile();
        return profile != null ? profile.getPhone() : null;
    }

    public String getUserLocation() {
        FirebaseAuthManager.UserProfile profile = getCurrentUserProfile();
        return profile != null ? profile.getLocation() : null;
    }

    public String getUserBio() {
        FirebaseAuthManager.UserProfile profile = getCurrentUserProfile();
        return profile != null ? profile.getBio() : null;
    }

    public String getUserAvatarUri() {
        FirebaseAuthManager.UserProfile profile = getCurrentUserProfile();
        return profile != null ? profile.getAvatarUri() : null;
    }

    @Nullable
    public FirebaseAuthManager.UserProfile getCurrentUserProfile() {
        if (cachedUserProfile != null) {
            return cachedUserProfile;
        }
        
        if (!isLoggedIn()) {
            return null;
        }
        
        // Load from Firestore synchronously if not cached
        String userId = authManager.getCurrentUserId();
        if (userId == null) return null;
        
        // For now, return the basic profile from auth manager
        // In a real implementation, you might want to load from cache
        authManager.getCurrentUserProfile(new FirebaseAuthManager.ProfileCallback() {
            @Override
            public void onProfileLoaded(FirebaseAuthManager.UserProfile profile) {
                cachedUserProfile = profile;
                if (userProfileChangeListener != null) {
                    userProfileChangeListener.onProfileChanged(profile);
                }
            }
            
            @Override
            public void onError(String error) {
                Log.e(TAG, "Error loading user profile: " + error);
            }
        });
        
        return null; // Return null for now, will be updated asynchronously
    }

    public void updateUserProfile(String name, String email, String phone, String location, String bio, DataOperationCallback callback) {
        if (!isLoggedIn()) {
            callback.onError("User not logged in");
            return;
        }

        String userId = authManager.getCurrentUserId();
        if (userId == null) {
            callback.onError("User ID not found");
            return;
        }

        // Update in Firebase Auth
        authManager.updateProfile(name, phone, new FirebaseAuthManager.ProfileUpdateCallback() {
            @Override
            public void onSuccess() {
                // Update additional fields in Firestore
                Map<String, Object> updates = new HashMap<>();
                updates.put("location", location);
                updates.put("bio", bio);
                updates.put("updatedAt", System.currentTimeMillis());

                firestore.collection(COLLECTION_USERS).document(userId)
                    .set(updates, SetOptions.merge())
                    .addOnSuccessListener(aVoid -> callback.onSuccess())
                    .addOnFailureListener(e -> callback.onError(e.getMessage()));
            }

            @Override
            public void onError(FirebaseAuthManager.AuthError error, String message) {
                callback.onError(message);
            }
        });
    }

    public void setUserAvatarUri(String avatarUri, DataOperationCallback callback) {
        if (!isLoggedIn()) {
            callback.onError("User not logged in");
            return;
        }

        String userId = authManager.getCurrentUserId();
        if (userId == null) {
            callback.onError("User ID not found");
            return;
        }

        Map<String, Object> updates = new HashMap<>();
        updates.put("avatarUri", avatarUri);
        updates.put("updatedAt", System.currentTimeMillis());

        firestore.collection(COLLECTION_USERS).document(userId)
            .set(updates, SetOptions.merge())
            .addOnSuccessListener(aVoid -> callback.onSuccess())
            .addOnFailureListener(e -> callback.onError(e.getMessage()));
    }

    public void clearSession() {
        authManager.signOut();
        clearCache();
    }

    // Payment methods
    public void setPaymentCashEnabled(boolean enabled, DataOperationCallback callback) {
        if (!isLoggedIn()) {
            callback.onError("User not logged in");
            return;
        }

        String userId = authManager.getCurrentUserId();
        Map<String, Object> updates = new HashMap<>();
        updates.put("paymentCashEnabled", enabled);
        updates.put("updatedAt", System.currentTimeMillis());

        firestore.collection(COLLECTION_USERS).document(userId)
            .collection("preferences").document("payment")
            .set(updates, SetOptions.merge())
            .addOnSuccessListener(aVoid -> callback.onSuccess())
            .addOnFailureListener(e -> callback.onError(e.getMessage()));
    }

    public boolean isPaymentCashEnabled() {
        // This would need to be loaded from Firestore
        // For now, return false as default
        return false;
    }

    public void addSavedCard(String holderName, String cardNumberOrLast4, String expiry, DataOperationCallback callback) {
        if (!isLoggedIn()) {
            callback.onError("User not logged in");
            return;
        }

        String userId = authManager.getCurrentUserId();
        String digits = extractDigits(cardNumberOrLast4);
        if (TextUtils.isEmpty(digits)) {
            callback.onError("Invalid card number");
            return;
        }

        String last4 = digits.length() >= 4 ? digits.substring(digits.length() - 4) : digits;
        String brand = detectCardBrand(digits);
        String normalizedExpiry = normalizeExpiry(expiry);
        String cardId = generateCardId(holderName, last4, normalizedExpiry);

        SavedCard card = new SavedCard(cardId, holderName, brand, last4, normalizedExpiry, System.currentTimeMillis());

        firestore.collection(COLLECTION_USERS).document(userId)
            .collection(FIELD_PAYMENT_CARDS).document(cardId)
            .set(card.toMap())
            .addOnSuccessListener(aVoid -> callback.onSuccess())
            .addOnFailureListener(e -> callback.onError(e.getMessage()));
    }

    public void removeSavedCard(String cardId, DataOperationCallback callback) {
        if (!isLoggedIn()) {
            callback.onError("User not logged in");
            return;
        }

        String userId = authManager.getCurrentUserId();
        firestore.collection(COLLECTION_USERS).document(userId)
            .collection(FIELD_PAYMENT_CARDS).document(cardId)
            .delete()
            .addOnSuccessListener(aVoid -> callback.onSuccess())
            .addOnFailureListener(e -> callback.onError(e.getMessage()));
    }

    public List<SavedCard> getSavedCards() {
        // This would need to be loaded from Firestore
        // For now, return empty list
        return new ArrayList<>();
    }

    public boolean hasSavedCards() {
        return !getSavedCards().isEmpty();
    }

    public SavedCard getPrimarySavedCard() {
        List<SavedCard> cards = getSavedCards();
        return cards.isEmpty() ? null : cards.get(0);
    }

    public SavedCard getSavedCardById(String cardId) {
        if (TextUtils.isEmpty(cardId)) {
            return null;
        }
        List<SavedCard> cards = getSavedCards();
        for (SavedCard card : cards) {
            if (cardId.equals(card.id)) {
                return card;
            }
        }
        return null;
    }

    public String getPaymentCardLast4() {
        SavedCard primaryCard = getPrimarySavedCard();
        return primaryCard != null ? primaryCard.last4 : null;
    }

    // PayHere merchant methods
    public void savePayHereMerchantId(String merchantId, DataOperationCallback callback) {
        if (!isLoggedIn()) {
            callback.onError("User not logged in");
            return;
        }

        String userId = authManager.getCurrentUserId();
        Map<String, Object> updates = new HashMap<>();
        updates.put(FIELD_PAYHERE_MERCHANT_ID, merchantId);
        updates.put("updatedAt", System.currentTimeMillis());

        firestore.collection(COLLECTION_USERS).document(userId)
            .collection("preferences").document("payhere")
            .set(updates, SetOptions.merge())
            .addOnSuccessListener(aVoid -> callback.onSuccess())
            .addOnFailureListener(e -> callback.onError(e.getMessage()));
    }

    public String getPayHereMerchantId() {
        // This would need to be loaded from Firestore
        return null;
    }

    public void savePayHereMerchantSecret(String merchantSecret, DataOperationCallback callback) {
        if (!isLoggedIn()) {
            callback.onError("User not logged in");
            return;
        }

        String userId = authManager.getCurrentUserId();
        Map<String, Object> updates = new HashMap<>();
        updates.put(FIELD_PAYHERE_MERCHANT_SECRET, merchantSecret);
        updates.put("updatedAt", System.currentTimeMillis());

        firestore.collection(COLLECTION_USERS).document(userId)
            .collection("preferences").document("payhere")
            .set(updates, SetOptions.merge())
            .addOnSuccessListener(aVoid -> callback.onSuccess())
            .addOnFailureListener(e -> callback.onError(e.getMessage()));
    }

    public String getPayHereMerchantSecret() {
        // This would need to be loaded from Firestore
        return null;
    }

    // Services methods
    public List<WorkerService> getWorkerServices() {
        if (cachedServices == null) {
            cachedServices = new ArrayList<>();
        }
        return cachedServices;
    }

    public void saveWorkerServices(List<WorkerService> services, DataOperationCallback callback) {
        if (!isLoggedIn()) {
            callback.onError("User not logged in");
            return;
        }

        String userId = authManager.getCurrentUserId();
        if (userId == null) {
            callback.onError("User ID not found");
            return;
        }

        // Use batch operation for multiple services
        List<Task<Void>> tasks = new ArrayList<>();
        
        for (WorkerService service : services) {
            if (service == null) continue;
            
            Map<String, Object> serviceData = service.toMap();
            serviceData.put("workerId", userId);
            serviceData.put("updatedAt", System.currentTimeMillis());
            
            Task<Void> task = firestore.collection(COLLECTION_SERVICES)
                .document(service.getId())
                .set(serviceData, SetOptions.merge());
            tasks.add(task);
        }

        // Wait for all operations to complete
        Task<Void> batchTask = com.google.android.gms.tasks.Tasks.whenAll(tasks);
        batchTask.addOnSuccessListener(aVoid -> callback.onSuccess())
                   .addOnFailureListener(e -> callback.onError(e.getMessage()));
    }

    public void upsertWorkerService(WorkerService service, DataOperationCallback callback) {
        if (!isLoggedIn()) {
            callback.onError("User not logged in");
            return;
        }

        String userId = authManager.getCurrentUserId();
        if (userId == null) {
            callback.onError("User ID not found");
            return;
        }

        Map<String, Object> serviceData = service.toMap();
        serviceData.put("workerId", userId);
        serviceData.put("updatedAt", System.currentTimeMillis());

        firestore.collection(COLLECTION_SERVICES).document(service.getId())
            .set(serviceData, SetOptions.merge())
            .addOnSuccessListener(aVoid -> callback.onSuccess())
            .addOnFailureListener(e -> callback.onError(e.getMessage()));
    }

    public void deleteWorkerService(String serviceId, DataOperationCallback callback) {
        if (!isLoggedIn()) {
            callback.onError("User not logged in");
            return;
        }

        firestore.collection(COLLECTION_SERVICES).document(serviceId)
            .delete()
            .addOnSuccessListener(aVoid -> callback.onSuccess())
            .addOnFailureListener(e -> callback.onError(e.getMessage()));
    }

    public WorkerService findWorkerServiceById(String serviceId) {
        if (serviceId == null) {
            return null;
        }

        List<WorkerService> services = getWorkerServices();
        for (WorkerService service : services) {
            if (serviceId.equals(service.getId())) {
                return service;
            }
        }
        return null;
    }

    // Recommended workers methods
    public List<RecommendedWorker> getRecommendedWorkers() {
        // Load from Firestore - no defaults for clean start
        return new ArrayList<>();
    }

    public void saveRecommendedWorkers(List<RecommendedWorker> workers, DataOperationCallback callback) {
        // This would save to a global collection or use defaults
        callback.onSuccess();
    }

    public RecommendedWorker findRecommendedWorkerByServiceId(String serviceId) {
        if (serviceId == null || serviceId.trim().isEmpty()) {
            return null;
        }
        
        List<RecommendedWorker> workers = getRecommendedWorkers();
        for (RecommendedWorker worker : workers) {
            if (worker != null && serviceId.equals(worker.getServiceId())) {
                return worker;
            }
        }
        return null;
    }

    // Bookings methods
    public List<UserBooking> getUserBookings() {
        if (cachedBookings == null) {
            cachedBookings = new ArrayList<>();
        }
        return cachedBookings;
    }

    public void saveUserBookings(List<UserBooking> bookings, DataOperationCallback callback) {
        if (!isLoggedIn()) {
            callback.onError("User not logged in");
            return;
        }

        String userId = authManager.getCurrentUserId();
        List<Task<Void>> tasks = new ArrayList<>();
        
        for (UserBooking booking : bookings) {
            if (booking == null) continue;
            
            Map<String, Object> bookingData = booking.toMap();
            bookingData.put("userId", userId);
            bookingData.put("updatedAt", System.currentTimeMillis());
            
            Task<Void> task = firestore.collection(COLLECTION_BOOKINGS)
                .document(booking.getId())
                .set(bookingData, SetOptions.merge());
            tasks.add(task);
        }

        Task<Void> batchTask = com.google.android.gms.tasks.Tasks.whenAll(tasks);
        batchTask.addOnSuccessListener(aVoid -> callback.onSuccess())
                   .addOnFailureListener(e -> callback.onError(e.getMessage()));
    }

    public void addUserBooking(UserBooking booking, DataOperationCallback callback) {
        if (!isLoggedIn()) {
            callback.onError("User not logged in");
            return;
        }

        String userId = authManager.getCurrentUserId();
        Map<String, Object> bookingData = booking.toMap();
        bookingData.put("userId", userId);
        bookingData.put("createdAt", System.currentTimeMillis());
        bookingData.put("updatedAt", System.currentTimeMillis());

        firestore.collection(COLLECTION_BOOKINGS).document(booking.getId())
            .set(bookingData, SetOptions.merge())
            .addOnSuccessListener(aVoid -> callback.onSuccess())
            .addOnFailureListener(e -> callback.onError(e.getMessage()));
    }

    public void updateBookingStatus(String bookingId, String status, String cancellationReason, DataOperationCallback callback) {
        if (TextUtils.isEmpty(bookingId) || TextUtils.isEmpty(status)) {
            callback.onError("Invalid booking ID or status");
            return;
        }

        Map<String, Object> updates = new HashMap<>();
        updates.put("status", status);
        updates.put("updatedAt", System.currentTimeMillis());
        if (cancellationReason != null) {
            updates.put("cancellationReason", cancellationReason);
        }

        firestore.collection(COLLECTION_BOOKINGS).document(bookingId)
            .set(updates, SetOptions.merge())
            .addOnSuccessListener(aVoid -> callback.onSuccess())
            .addOnFailureListener(e -> callback.onError(e.getMessage()));
    }

    // Job requests methods
    public List<WorkerJobRequest> getWorkerJobRequests() {
        if (cachedJobRequests == null) {
            cachedJobRequests = new ArrayList<>();
        }
        return cachedJobRequests;
    }

    public void saveWorkerJobRequests(List<WorkerJobRequest> requests, DataOperationCallback callback) {
        if (!isLoggedIn()) {
            callback.onError("User not logged in");
            return;
        }

        String userId = authManager.getCurrentUserId();
        List<Task<Void>> tasks = new ArrayList<>();
        
        for (WorkerJobRequest request : requests) {
            if (request == null) continue;
            
            Map<String, Object> requestData = request.toMap();
            requestData.put("workerId", userId);
            requestData.put("updatedAt", System.currentTimeMillis());
            
            Task<Void> task = firestore.collection(COLLECTION_JOB_REQUESTS)
                .document(request.getId())
                .set(requestData, SetOptions.merge());
            tasks.add(task);
        }

        Task<Void> batchTask = com.google.android.gms.tasks.Tasks.whenAll(tasks);
        batchTask.addOnSuccessListener(aVoid -> callback.onSuccess())
                   .addOnFailureListener(e -> callback.onError(e.getMessage()));
    }

    public void addWorkerJobRequest(WorkerJobRequest request, DataOperationCallback callback) {
        if (!isLoggedIn()) {
            callback.onError("User not logged in");
            return;
        }

        String userId = authManager.getCurrentUserId();
        Map<String, Object> requestData = request.toMap();
        requestData.put("workerId", userId);
        requestData.put("createdAt", System.currentTimeMillis());
        requestData.put("updatedAt", System.currentTimeMillis());

        firestore.collection(COLLECTION_JOB_REQUESTS).document(request.getId())
            .set(requestData, SetOptions.merge())
            .addOnSuccessListener(aVoid -> callback.onSuccess())
            .addOnFailureListener(e -> callback.onError(e.getMessage()));
    }

    // Conversations methods
    public List<UserConversation> getUserConversations() {
        if (cachedConversations == null) {
            cachedConversations = new ArrayList<>();
        }
        return cachedConversations;
    }

    public void saveUserConversations(List<UserConversation> conversations, DataOperationCallback callback) {
        if (!isLoggedIn()) {
            callback.onError("User not logged in");
            return;
        }

        String userId = authManager.getCurrentUserId();
        List<Task<Void>> tasks = new ArrayList<>();
        
        for (UserConversation conversation : conversations) {
            if (conversation == null) continue;
            
            Map<String, Object> conversationData = conversation.toMap();
            conversationData.put("userId", userId);
            conversationData.put("updatedAt", System.currentTimeMillis());
            
            Task<Void> task = firestore.collection(COLLECTION_CONVERSATIONS)
                .document(conversation.getId())
                .set(conversationData, SetOptions.merge());
            tasks.add(task);
        }

        Task<Void> batchTask = com.google.android.gms.tasks.Tasks.whenAll(tasks);
        batchTask.addOnSuccessListener(aVoid -> callback.onSuccess())
                   .addOnFailureListener(e -> callback.onError(e.getMessage()));
    }

    public void upsertUserConversation(UserConversation conversation, DataOperationCallback callback) {
        if (!isLoggedIn()) {
            callback.onError("User not logged in");
            return;
        }

        String userId = authManager.getCurrentUserId();
        Map<String, Object> conversationData = conversation.toMap();
        conversationData.put("userId", userId);
        conversationData.put("updatedAt", System.currentTimeMillis());

        firestore.collection(COLLECTION_CONVERSATIONS).document(conversation.getId())
            .set(conversationData, SetOptions.merge())
            .addOnSuccessListener(aVoid -> callback.onSuccess())
            .addOnFailureListener(e -> callback.onError(e.getMessage()));
    }

    public void markConversationAsRead(String conversationId, DataOperationCallback callback) {
        if (conversationId == null) {
            callback.onError("Invalid conversation ID");
            return;
        }

        Map<String, Object> updates = new HashMap<>();
        updates.put("userUnread", false);
        updates.put("updatedAt", System.currentTimeMillis());

        firestore.collection(COLLECTION_CONVERSATIONS).document(conversationId)
            .set(updates, SetOptions.merge())
            .addOnSuccessListener(aVoid -> callback.onSuccess())
            .addOnFailureListener(e -> callback.onError(e.getMessage()));
    }

    // Messages methods
    public List<UserChatMessage> getChatMessages(String conversationId) {
        if (conversationId == null) {
            return new ArrayList<>();
        }
        
        List<UserChatMessage> messages = cachedMessages.get(conversationId);
        if (messages == null) {
            messages = new ArrayList<>();
            cachedMessages.put(conversationId, messages);
        }
        return messages;
    }

    public void saveChatMessages(String conversationId, List<UserChatMessage> messages, DataOperationCallback callback) {
        if (conversationId == null) {
            callback.onError("Invalid conversation ID");
            return;
        }

        List<Task<Void>> tasks = new ArrayList<>();
        
        for (UserChatMessage message : messages) {
            if (message == null) continue;
            
            Map<String, Object> messageData = message.toMap();
            messageData.put("conversationId", conversationId);
            messageData.put("createdAt", System.currentTimeMillis());
            
            Task<Void> task = firestore.collection(COLLECTION_CONVERSATIONS)
                .document(conversationId)
                .collection(SUBCOLLECTION_MESSAGES)
                .document(message.getId())
                .set(messageData, SetOptions.merge());
            tasks.add(task);
        }

        Task<Void> batchTask = com.google.android.gms.tasks.Tasks.whenAll(tasks);
        batchTask.addOnSuccessListener(aVoid -> callback.onSuccess())
                   .addOnFailureListener(e -> callback.onError(e.getMessage()));
    }

    public void addChatMessage(String conversationId, UserChatMessage message, DataOperationCallback callback) {
        if (conversationId == null) {
            callback.onError("Invalid conversation ID");
            return;
        }

        Map<String, Object> messageData = message.toMap();
        messageData.put("conversationId", conversationId);
        messageData.put("createdAt", System.currentTimeMillis());

        firestore.collection(COLLECTION_CONVERSATIONS).document(conversationId)
            .collection(SUBCOLLECTION_MESSAGES).document(message.getId())
            .set(messageData, SetOptions.merge())
            .addOnSuccessListener(aVoid -> callback.onSuccess())
            .addOnFailureListener(e -> callback.onError(e.getMessage()));
    }

    // Service areas methods
    public List<ServiceArea> getServiceAreas() {
        return createDefaultServiceAreas();
    }

    @Nullable
    public ServiceArea findServiceAreaByName(@Nullable String areaName) {
        if (areaName == null) {
            return null;
        }
        
        List<ServiceArea> areas = getServiceAreas();
        for (ServiceArea area : areas) {
            if (area != null && areaName.equalsIgnoreCase(area.getName())) {
                return area;
            }
        }
        return null;
    }

    public List<String> getRecentServiceAreas() {
        // This would be loaded from Firestore user preferences
        return new ArrayList<>();
    }

    public void addRecentServiceArea(String areaName, DataOperationCallback callback) {
        if (areaName == null || areaName.trim().isEmpty()) {
            callback.onError("Invalid area name");
            return;
        }

        if (!isLoggedIn()) {
            callback.onError("User not logged in");
            return;
        }

        String userId = authManager.getCurrentUserId();
        String trimmed = areaName.trim();

        firestore.collection(COLLECTION_USERS).document(userId)
            .collection("preferences").document("locations")
            .update("recentAreas", FieldValue.arrayUnion(trimmed))
            .addOnSuccessListener(aVoid -> callback.onSuccess())
            .addOnFailureListener(e -> {
                // If document doesn't exist, create it
                Map<String, Object> data = new HashMap<>();
                data.put("recentAreas", Arrays.asList(trimmed));
                data.put("updatedAt", System.currentTimeMillis());
                
                firestore.collection(COLLECTION_USERS).document(userId)
                    .collection("preferences").document("locations")
                    .set(data)
                    .addOnSuccessListener(aVoid2 -> callback.onSuccess())
                    .addOnFailureListener(e2 -> callback.onError(e2.getMessage()));
            });
    }

    // Private helper methods
    private void initializeRealtimeListeners(String userId) {
        // User profile listener
        userProfileListener = firestore.collection(COLLECTION_USERS).document(userId)
            .addSnapshotListener(new EventListener<DocumentSnapshot>() {
                @Override
                public void onEvent(@Nullable DocumentSnapshot snapshot, @Nullable FirebaseFirestoreException e) {
                    if (e != null) {
                        Log.e(TAG, "User profile listener error", e);
                        return;
                    }
                    
                    if (snapshot != null && snapshot.exists()) {
                        cachedUserProfile = FirebaseAuthManager.UserProfile.fromDocument(snapshot);
                        if (userProfileChangeListener != null) {
                            userProfileChangeListener.onProfileChanged(cachedUserProfile);
                        }
                    }
                }
            });

        // Services listener
        servicesListener = firestore.collection(COLLECTION_SERVICES)
            .whereEqualTo("workerId", userId)
            .addSnapshotListener(new EventListener<QuerySnapshot>() {
                @Override
                public void onEvent(@Nullable QuerySnapshot snapshots, @Nullable FirebaseFirestoreException e) {
                    if (e != null) {
                        Log.e(TAG, "Services listener error", e);
                        return;
                    }
                    
                    List<WorkerService> services = new ArrayList<>();
                    if (snapshots != null) {
                        for (DocumentSnapshot doc : snapshots.getDocuments()) {
                            WorkerService service = WorkerService.fromDocument(doc);
                            if (service != null) {
                                services.add(service);
                            }
                        }
                    }
                    
                    cachedServices = services;
                    if (servicesChangeListener != null) {
                        servicesChangeListener.onServicesChanged(services);
                    }
                }
            });

        // Bookings listener
        bookingsListener = firestore.collection(COLLECTION_BOOKINGS)
            .whereEqualTo("userId", userId)
            .orderBy("createdAt", com.google.firebase.firestore.Query.Direction.DESCENDING)
            .addSnapshotListener(new EventListener<QuerySnapshot>() {
                @Override
                public void onEvent(@Nullable QuerySnapshot snapshots, @Nullable FirebaseFirestoreException e) {
                    if (e != null) {
                        Log.e(TAG, "Bookings listener error", e);
                        return;
                    }
                    
                    List<UserBooking> bookings = new ArrayList<>();
                    if (snapshots != null) {
                        for (DocumentSnapshot doc : snapshots.getDocuments()) {
                            UserBooking booking = UserBooking.fromDocument(doc);
                            if (booking != null) {
                                bookings.add(booking);
                            }
                        }
                    }
                    
                    cachedBookings = bookings;
                    if (bookingsChangeListener != null) {
                        bookingsChangeListener.onBookingsChanged(bookings);
                    }
                }
            });

        // Job requests listener
        jobRequestsListener = firestore.collection(COLLECTION_JOB_REQUESTS)
            .whereEqualTo("workerId", userId)
            .orderBy("createdAt", com.google.firebase.firestore.Query.Direction.DESCENDING)
            .addSnapshotListener(new EventListener<QuerySnapshot>() {
                @Override
                public void onEvent(@Nullable QuerySnapshot snapshots, @Nullable FirebaseFirestoreException e) {
                    if (e != null) {
                        Log.e(TAG, "Job requests listener error", e);
                        return;
                    }
                    
                    List<WorkerJobRequest> requests = new ArrayList<>();
                    if (snapshots != null) {
                        for (DocumentSnapshot doc : snapshots.getDocuments()) {
                            WorkerJobRequest request = WorkerJobRequest.fromDocument(doc);
                            if (request != null) {
                                requests.add(request);
                            }
                        }
                    }
                    
                    cachedJobRequests = requests;
                    if (jobRequestsChangeListener != null) {
                        jobRequestsChangeListener.onJobRequestsChanged(requests);
                    }
                }
            });

        // Conversations listener
        conversationsListener = firestore.collection(COLLECTION_CONVERSATIONS)
            .whereEqualTo("userId", userId)
            .orderBy("lastMessageTimestamp", com.google.firebase.firestore.Query.Direction.DESCENDING)
            .addSnapshotListener(new EventListener<QuerySnapshot>() {
                @Override
                public void onEvent(@Nullable QuerySnapshot snapshots, @Nullable FirebaseFirestoreException e) {
                    if (e != null) {
                        Log.e(TAG, "Conversations listener error", e);
                        return;
                    }
                    
                    List<UserConversation> conversations = new ArrayList<>();
                    if (snapshots != null) {
                        for (DocumentSnapshot doc : snapshots.getDocuments()) {
                            UserConversation conversation = UserConversation.fromDocument(doc);
                            if (conversation != null) {
                                conversations.add(conversation);
                            }
                        }
                    }
                    
                    cachedConversations = conversations;
                    if (conversationsChangeListener != null) {
                        conversationsChangeListener.onConversationsChanged(conversations);
                    }
                }
            });
    }

    private void clearRealtimeListeners() {
        if (userProfileListener != null) {
            userProfileListener.remove();
            userProfileListener = null;
        }
        if (servicesListener != null) {
            servicesListener.remove();
            servicesListener = null;
        }
        if (bookingsListener != null) {
            bookingsListener.remove();
            bookingsListener = null;
        }
        if (jobRequestsListener != null) {
            jobRequestsListener.remove();
            jobRequestsListener = null;
        }
        if (conversationsListener != null) {
            conversationsListener.remove();
            conversationsListener = null;
        }
    }

    private void clearCache() {
        cachedUserProfile = null;
        cachedServices = null;
        cachedBookings = null;
        cachedJobRequests = null;
        cachedConversations = null;
        cachedMessages.clear();
    }

    // Default data creation methods
    private List<WorkerService> createDefaultWorkerServices() {
        List<WorkerService> defaults = new ArrayList<>();
        
        // Create default services similar to the original SessionManager
        defaults.add(WorkerService.create(
            "worker_amal_perera",
            "Amal Perera",
            "amal.perera@skilllink.lk",
            "electrician",
            "Emergency Electrical Repairs",
            "Certified electrician for urgent fixes, inspections, and safety upgrades across Colombo.",
            WorkerService.PRICE_TYPE_HOURLY,
            "4800",
            "android.resource://" + appContext.getPackageName() + "/" + com.skilllink.R.drawable.ic_worker_expert_amal
        ).withLocation("Colombo", 6.927079, 79.861244, 18d));

        defaults.add(WorkerService.create(
            "worker_nadeesha_silva",
            "Nadeesha Silva",
            "nadeesha.silva@skilllink.lk",
            "plumber",
            "Heritage Property Plumbing",
            "Specialised maintenance for classic homes, leak repairs, and bathroom renovations in Kandy.",
            WorkerService.PRICE_TYPE_CUSTOM,
            "17500",
            "android.resource://" + appContext.getPackageName() + "/" + com.skilllink.R.drawable.ic_worker_expert_nadeesha
        ).withLocation("Kandy", 7.290572, 80.633728, 22d));

        defaults.add(WorkerService.create(
            "worker_isuru_jayasinghe",
            "Isuru Jayasinghe",
            "isuru.jayasinghe@skilllink.lk",
            "gardener",
            "Coastal Landscape Care",
            "Resilient plant selections, irrigation setup, and routine upkeep for seaside villas in Galle.",
            WorkerService.PRICE_TYPE_CUSTOM,
            "24000",
            "android.resource://" + appContext.getPackageName() + "/" + com.skilllink.R.drawable.ic_worker_expert_isuru
        ).withLocation("Galle", 6.053519, 80.220978, 25d));

        return defaults;
    }

    private List<RecommendedWorker> createDefaultRecommendedWorkers() {
        List<RecommendedWorker> defaults = new ArrayList<>();

        defaults.add(new RecommendedWorker(
            "worker_amal_perera",
            "Amal Perera",
            "Master Electrician",
            4.9,
            182,
            "Available today",
            String.format("LKR %,d / hr", 4800),
            String.format("%.1f km away", 2.1),
            "android.resource://" + appContext.getPackageName() + "/" + com.skilllink.R.drawable.ic_worker_expert_amal,
            null,
            "Electrical",
            Arrays.asList("Panel upgrades", "Safety inspections", "Emergency repairs"),
            "Colombo",
            6.927079,
            79.861244,
            18d));

        defaults.add(new RecommendedWorker(
            "worker_nadeesha_silva",
            "Nadeesha Silva",
            "Premium Home Cleaning",
            4.8,
            210,
            "Accepting new bookings",
            String.format("LKR %,d / visit", 5500),
            String.format("%.1f km away", 3.4),
            "android.resource://" + appContext.getPackageName() + "/" + com.skilllink.R.drawable.ic_worker_expert_nadeesha,
            null,
            "Home Cleaning",
            Arrays.asList("Deep cleaning", "Move-out prep", "Eco-friendly supplies"),
            "Kandy",
            7.290572,
            80.633728,
            20d));

        defaults.add(new RecommendedWorker(
            "worker_isuru_jayasinghe",
            "Isuru Jayasinghe",
            "Landscape Specialist",
            4.7,
            144,
            "Available this week",
            String.format("LKR %,d / project", 12500),
            String.format("%.1f km away", 5.6),
            "android.resource://" + appContext.getPackageName() + "/" + com.skilllink.R.drawable.ic_worker_expert_isuru,
            null,
            "Gardening",
            Arrays.asList("Garden design", "Irrigation setup", "Seasonal maintenance"),
            "Galle",
            6.053519,
            80.220978,
            25d));

        return defaults;
    }

    private List<ServiceArea> createDefaultServiceAreas() {
        List<ServiceArea> areas = new ArrayList<>();
        areas.add(new ServiceArea("Colombo", "Western Province", true, 6.927079, 79.861244));
        areas.add(new ServiceArea("Kandy", "Central Province", true, 7.290572, 80.633728));
        areas.add(new ServiceArea("Galle", "Southern Province", true, 6.053519, 80.220978));
        areas.add(new ServiceArea("Negombo", "Western Province", false, 7.20084, 79.87366));
        areas.add(new ServiceArea("Matara", "Southern Province", false, 5.949278, 80.546875));
        areas.add(new ServiceArea("Kurunegala", "North Western Province", false, 7.486302, 80.362304));
        areas.add(new ServiceArea("Jaffna", "Northern Province", false, 9.661498, 80.025482));
        areas.add(new ServiceArea("Anuradhapura", "North Central Province", false, 8.311362, 80.403656));
        areas.add(new ServiceArea("Batticaloa", "Eastern Province", false, 7.717722, 81.674362));
        areas.add(new ServiceArea("Badulla", "Uva Province", false, 6.989524, 81.056045));
        return areas;
    }

    // Card utility methods
    private String extractDigits(String raw) {
        if (raw == null) return "";
        StringBuilder digits = new StringBuilder(raw.length());
        for (int i = 0; i < raw.length(); i++) {
            char ch = raw.charAt(i);
            if (Character.isDigit(ch)) {
                digits.append(ch);
            }
        }
        return digits.toString();
    }

    private String detectCardBrand(String digits) {
        if (TextUtils.isEmpty(digits)) return "Card";
        if (digits.startsWith("34") || digits.startsWith("37")) return "American Express";
        if (digits.startsWith("4")) return "Visa";
        if (digits.startsWith("5")) return "Mastercard";
        if (digits.startsWith("6")) return "Discover";
        return "Card";
    }

    private String normalizeExpiry(String expiry) {
        if (expiry == null) return null;
        String trimmed = expiry.trim();
        return trimmed.length() == 5 && trimmed.charAt(2) == '/' ? trimmed : null;
    }

    private String generateCardId(String holderName, String maskedLast4, String expiry) {
        String base = (holderName == null ? "" : holderName.trim().toLowerCase())
                + "|" + (maskedLast4 == null ? "" : maskedLast4)
                + "|" + (expiry == null ? "" : expiry);
        return UUID.nameUUIDFromBytes(base.getBytes()).toString();
    }

    // SavedCard class
    public static class SavedCard {
        public final String id;
        public final String holderName;
        public final String brand;
        public final String last4;
        public final String expiry;
        public final long savedAt;

        SavedCard(String id, String holderName, String brand, String last4, String expiry, long savedAt) {
            this.id = id;
            this.holderName = holderName;
            this.brand = brand;
            this.last4 = last4;
            this.expiry = expiry;
            this.savedAt = savedAt;
        }

        Map<String, Object> toMap() {
            Map<String, Object> map = new HashMap<>();
            map.put("id", id);
            map.put("holderName", holderName);
            map.put("brand", brand);
            map.put("last4", last4);
            map.put("expiry", expiry);
            map.put("savedAt", savedAt);
            return map;
        }

        static SavedCard fromDocument(DocumentSnapshot doc) {
            if (doc == null || !doc.exists()) return null;
            return new SavedCard(
                doc.getString("id"),
                doc.getString("holderName"),
                doc.getString("brand"),
                doc.getString("last4"),
                doc.getString("expiry"),
                doc.getLong("savedAt") != null ? doc.getLong("savedAt") : System.currentTimeMillis()
            );
        }
    }
}
