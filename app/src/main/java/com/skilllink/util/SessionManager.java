package com.skilllink.util;

import android.content.Context;
import android.content.SharedPreferences;
import android.text.TextUtils;

import androidx.annotation.Nullable;

import com.skilllink.R;
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
import java.util.Iterator;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;

public class SessionManager {

    private static final String PREF_NAME = "skilllink_session";
    private static final String KEY_LOGGED_IN = "logged_in";
    private static final String KEY_USER_ROLE = "user_role";
    private static final String KEY_USER_EMAIL = "user_email";
    private static final String KEY_USER_NAME = "user_name";
    private static final String KEY_USER_PHONE = "user_phone";
    private static final String KEY_USER_LOCATION = "user_location";
    private static final String KEY_USER_BIO = "user_bio";
    private static final String KEY_USER_AVATAR_URI = "user_avatar_uri";
    private static final String KEY_USER_DOCUMENT_ID = "user_document_id";
    private static final String KEY_WORKER_DOCUMENT_ID = "worker_document_id";
    private static final String KEY_PAYMENT_CASH_ENABLED = "payment_cash_enabled";
    private static final String KEY_PAYMENT_CARD_COLLECTION = "payment_cards";
    private static final String KEY_PAYHERE_MERCHANT_ID = "payhere_merchant_id";
    private static final String KEY_PAYHERE_MERCHANT_SECRET = "payhere_merchant_secret";
    private static final String KEY_WORKER_SERVICES = "worker_services";
    private static final String KEY_USER_BOOKINGS = "user_bookings";
    private static final String KEY_WORKER_JOB_REQUESTS = "worker_job_requests";
    private static final String KEY_RECOMMENDED_WORKERS = "recommended_workers";
    private static final String KEY_USER_CONVERSATIONS = "user_conversations";
    private static final String KEY_CHAT_MESSAGES_PREFIX = "user_chat_messages_";
    private static final String KEY_RECENT_SERVICE_AREAS = "recent_service_areas";

    private final Context appContext;
    private final SharedPreferences preferences;

    public SessionManager(Context context) {
        this.appContext = context.getApplicationContext();
        this.preferences = appContext.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE);
    }

    private String trimToNull(String value) {
        if (value == null) {
            return null;
        }
        String trimmed = value.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }

    private String normalizeEmail(String email) {
        String candidate = trimToNull(email);
        return candidate != null ? candidate.toLowerCase(Locale.US) : null;
    }

    private String normalizePhone(String phone) {
        String candidate = trimToNull(phone);
        if (candidate == null) {
            return null;
        }
        StringBuilder digits = new StringBuilder(candidate.length());
        for (int i = 0; i < candidate.length(); i++) {
            char ch = candidate.charAt(i);
            if (Character.isDigit(ch)) {
                digits.append(ch);
            }
        }
        return digits.length() > 0 ? digits.toString() : null;
    }

    private String buildDocumentId(String raw) {
        if (raw == null) {
            return UUID.randomUUID().toString();
        }
        String normalized = raw.trim().toLowerCase(Locale.US);
        if (normalized.isEmpty()) {
            return UUID.randomUUID().toString();
        }

        StringBuilder replaced = new StringBuilder(normalized.length());
        for (int i = 0; i < normalized.length(); i++) {
            char ch = normalized.charAt(i);
            if ((ch >= 'a' && ch <= 'z') || (ch >= '0' && ch <= '9')) {
                replaced.append(ch);
            } else {
                replaced.append('_');
            }
        }

        StringBuilder collapsed = new StringBuilder(replaced.length());
        boolean lastUnderscore = false;
        for (int i = 0; i < replaced.length(); i++) {
            char ch = replaced.charAt(i);
            if (ch == '_') {
                if (!lastUnderscore) {
                    collapsed.append(ch);
                    lastUnderscore = true;
                }
            } else {
                collapsed.append(ch);
                lastUnderscore = false;
            }
        }

        String collapsedString = collapsed.toString();
        int start = 0;
        int end = collapsedString.length();
        while (start < end && collapsedString.charAt(start) == '_') {
            start++;
        }
        while (end > start && collapsedString.charAt(end - 1) == '_') {
            end--;
        }

        String trimmed = collapsedString.substring(start, end);
        if (trimmed.isEmpty()) {
            return UUID.randomUUID().toString();
        }
        return trimmed;
    }

    public void saveSession(String email, String role) {
        String trimmedEmail = trimToNull(email);
        SharedPreferences.Editor editor = preferences.edit()
                .putBoolean(KEY_LOGGED_IN, true)
                .putString(KEY_USER_ROLE, role)
                .putString(KEY_USER_EMAIL, trimmedEmail);

        if (!preferences.contains(KEY_USER_NAME)) {
            NameFormatter.Parts parts = NameFormatter.resolve(null, trimmedEmail);
            if (parts != null) {
                editor.putString(KEY_USER_NAME, parts.getFullName());
            }
        }

        editor.apply();
    }

    public void updateUserProfile(String name, String email, String phone, String location, String bio) {
        preferences.edit()
                .putString(KEY_USER_NAME, trimToNull(name))
                .putString(KEY_USER_EMAIL, trimToNull(email))
                .putString(KEY_USER_PHONE, trimToNull(phone))
                .putString(KEY_USER_LOCATION, trimToNull(location))
                .putString(KEY_USER_BIO, trimToNull(bio))
                .apply();
    }

    public void setUserName(String name) {
        preferences.edit().putString(KEY_USER_NAME, name).apply();
    }

    public String getUserName() {
        return preferences.getString(KEY_USER_NAME, null);
    }

    public void setUserPhone(String phone) {
        preferences.edit().putString(KEY_USER_PHONE, trimToNull(phone)).apply();
    }

    public String getUserPhone() {
        return preferences.getString(KEY_USER_PHONE, null);
    }

    public void setUserLocation(String location) {
        preferences.edit().putString(KEY_USER_LOCATION, location).apply();
    }

    public String getUserLocation() {
        return preferences.getString(KEY_USER_LOCATION, null);
    }

    public void setUserBio(String bio) {
        preferences.edit().putString(KEY_USER_BIO, bio).apply();
    }

    public String getUserBio() {
        return preferences.getString(KEY_USER_BIO, null);
    }

    public String getOrCreateWorkerDocumentId() {
        String existing = preferences.getString(KEY_WORKER_DOCUMENT_ID, null);
        String email = normalizeEmail(getUserEmail());
        if (!TextUtils.isEmpty(email)) {
            String candidate = buildDocumentId(email);
            if (!TextUtils.isEmpty(existing) && existing.equals(candidate)) {
                return existing;
            }
            preferences.edit().putString(KEY_WORKER_DOCUMENT_ID, candidate).apply();
            return candidate;
        }

        String phone = normalizePhone(getUserPhone());
        if (!TextUtils.isEmpty(phone)) {
            String candidate = buildDocumentId(phone);
            if (!TextUtils.isEmpty(existing) && existing.equals(candidate)) {
                return existing;
            }
            preferences.edit().putString(KEY_WORKER_DOCUMENT_ID, candidate).apply();
            return candidate;
        }

        if (!TextUtils.isEmpty(existing)) {
            return existing;
        }

        String fallback = UUID.randomUUID().toString();
        preferences.edit().putString(KEY_WORKER_DOCUMENT_ID, fallback).apply();
        return fallback;
    }

    public String getOrCreateUserDocumentId() {
        String existing = preferences.getString(KEY_USER_DOCUMENT_ID, null);
        String email = normalizeEmail(getUserEmail());
        if (!TextUtils.isEmpty(email)) {
            String candidate = buildDocumentId(email);
            if (!TextUtils.isEmpty(existing) && existing.equals(candidate)) {
                return existing;
            }
            preferences.edit().putString(KEY_USER_DOCUMENT_ID, candidate).apply();
            return candidate;
        }

        String phone = normalizePhone(getUserPhone());
        if (!TextUtils.isEmpty(phone)) {
            String candidate = buildDocumentId(phone);
            if (!TextUtils.isEmpty(existing) && existing.equals(candidate)) {
                return existing;
            }
            preferences.edit().putString(KEY_USER_DOCUMENT_ID, candidate).apply();
            return candidate;
        }

        if (!TextUtils.isEmpty(existing)) {
            return existing;
        }

        String fallback = UUID.randomUUID().toString();
        preferences.edit().putString(KEY_USER_DOCUMENT_ID, fallback).apply();
        return fallback;
    }

    private String sanitizeDocumentId(String raw) {
        return buildDocumentId(raw);
    }

    public void setUserAvatarUri(String avatarUri) {
        preferences.edit().putString(KEY_USER_AVATAR_URI, avatarUri).apply();
    }

    public String getUserAvatarUri() {
        return preferences.getString(KEY_USER_AVATAR_URI, null);
    }

    public void setPaymentCashEnabled(boolean enabled) {
        preferences.edit().putBoolean(KEY_PAYMENT_CASH_ENABLED, enabled).apply();
    }

    public boolean isPaymentCashEnabled() {
        return preferences.getBoolean(KEY_PAYMENT_CASH_ENABLED, false);
    }

    public SavedCard addSavedCard(String holderName, String cardNumberOrLast4, @Nullable String expiry) {
        String digits = extractDigits(cardNumberOrLast4);
        if (TextUtils.isEmpty(digits)) {
            return null;
        }
        String last4 = digits.length() >= 4 ? digits.substring(digits.length() - 4) : digits;
        String resolvedBrand = detectCardBrand(digits);
        String normalizedExpiry = normalizeExpiry(expiry);
        String cardId = generateCardId(holderName, last4, normalizedExpiry);
        List<SavedCard> cards = getSavedCards();
        SavedCard existing = null;
        for (SavedCard card : cards) {
            if (card != null && cardId.equals(card.id)) {
                existing = card;
                break;
            }
        }
        SavedCard updated = new SavedCard(cardId, holderName, resolvedBrand, last4, normalizedExpiry, System.currentTimeMillis());
        if (existing != null) {
            cards.remove(existing);
        }
        cards.add(0, updated);
        persistSavedCards(cards);
        return updated;
    }

    /**
     * Save a payment card (convenience method for AddCardDialog)
     */
    public void savePaymentCard(String last4, String holderName, String expiry) {
        addSavedCard(holderName, last4, expiry);
    }

    public void removeSavedCard(String cardId) {
        if (TextUtils.isEmpty(cardId)) {
            return;
        }
        List<SavedCard> cards = getSavedCards();
        Iterator<SavedCard> iterator = cards.iterator();
        boolean changed = false;
        while (iterator.hasNext()) {
            SavedCard card = iterator.next();
            if (card != null && cardId.equals(card.id)) {
                iterator.remove();
                changed = true;
                break;
            }
        }
        if (changed) {
            persistSavedCards(cards);
        }
    }

    public List<SavedCard> getSavedCards() {
        String rawJson = preferences.getString(KEY_PAYMENT_CARD_COLLECTION, null);
        List<SavedCard> cards = new ArrayList<>();
        if (rawJson == null || rawJson.trim().isEmpty()) {
            return cards;
        }
        try {
            JSONArray array = new JSONArray(rawJson);
            for (int i = 0; i < array.length(); i++) {
                JSONObject object = array.optJSONObject(i);
                SavedCard card = SavedCard.fromJson(object);
                if (card != null) {
                    cards.add(card);
                }
            }
        } catch (JSONException ignored) {
        }
        Collections.sort(cards, new Comparator<SavedCard>() {
            @Override
            public int compare(SavedCard o1, SavedCard o2) {
                return Long.compare(o2.savedAt, o1.savedAt);
            }
        });
        return cards;
    }

    private void persistSavedCards(List<SavedCard> cards) {
        JSONArray array = new JSONArray();
        if (cards != null) {
            for (SavedCard card : cards) {
                if (card == null) {
                    continue;
                }
                array.put(card.toJson());
            }
        }
        preferences.edit().putString(KEY_PAYMENT_CARD_COLLECTION, array.toString()).apply();
    }

    public void clearPaymentCardDetails() {
        preferences.edit().remove(KEY_PAYMENT_CARD_COLLECTION).apply();
    }

    public boolean hasSavedCards() {
        return !getSavedCards().isEmpty();
    }

    private String generateCardId(String holderName, String maskedLast4, String expiry) {
        String base = (holderName == null ? "" : holderName.trim().toLowerCase(Locale.US))
                + "|" + (maskedLast4 == null ? "" : maskedLast4)
                + "|" + (expiry == null ? "" : expiry);
        return UUID.nameUUIDFromBytes(base.getBytes()).toString();
    }

    private String extractDigits(@Nullable String raw) {
        if (raw == null) {
            return "";
        }
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
        if (TextUtils.isEmpty(digits)) {
            return "Card";
        }
        if (digits.startsWith("34") || digits.startsWith("37")) {
            return "American Express";
        }
        if (digits.startsWith("4")) {
            return "Visa";
        }
        if (digits.startsWith("5")) {
            return "Mastercard";
        }
        if (digits.startsWith("6")) {
            return "Discover";
        }
        return "Card";
    }

    private String normalizeExpiry(@Nullable String expiry) {
        if (expiry == null) {
            return null;
        }
        String trimmed = expiry.trim();
        if (trimmed.length() == 5 && trimmed.charAt(2) == '/') {
            return trimmed;
        }
        return null;
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
            if (card != null && cardId.equals(card.id)) {
                return card;
            }
        }
        return null;
    }

    public String getPaymentCardLast4() {
        SavedCard primaryCard = getPrimarySavedCard();
        return primaryCard != null ? primaryCard.last4 : null;
    }


    public void savePayHereMerchantId(String merchantId) {
        preferences.edit().putString(KEY_PAYHERE_MERCHANT_ID, trimToNull(merchantId)).apply();
    }

    public String getPayHereMerchantId() {
        return preferences.getString(KEY_PAYHERE_MERCHANT_ID, null);
    }

    public void savePayHereMerchantSecret(String merchantSecret) {
        preferences.edit().putString(KEY_PAYHERE_MERCHANT_SECRET, trimToNull(merchantSecret)).apply();
    }

    public String getPayHereMerchantSecret() {
        return preferences.getString(KEY_PAYHERE_MERCHANT_SECRET, null);
    }

    public void setUserEmail(String email) {
        preferences.edit().putString(KEY_USER_EMAIL, trimToNull(email)).apply();
    }

    public boolean isLoggedIn() {
        return preferences.getBoolean(KEY_LOGGED_IN, false);
    }

    public String getUserRole() {
        return preferences.getString(KEY_USER_ROLE, null);
    }

    public String getUserEmail() {
        return preferences.getString(KEY_USER_EMAIL, null);
    }

    public String getUserId() {
        return getOrCreateUserDocumentId();
    }

    public void clearSession() {
        String avatarUri = preferences.getString(KEY_USER_AVATAR_URI, null);
        String userDocumentId = preferences.getString(KEY_USER_DOCUMENT_ID, null);
        String workerServicesJson = preferences.getString(KEY_WORKER_SERVICES, null);
        String userBookingsJson = preferences.getString(KEY_USER_BOOKINGS, null);
        String workerJobRequestsJson = preferences.getString(KEY_WORKER_JOB_REQUESTS, null);
        String storedMerchantId = preferences.getString(KEY_PAYHERE_MERCHANT_ID, null);
        String storedMerchantSecret = preferences.getString(KEY_PAYHERE_MERCHANT_SECRET, null);
        String recommendedWorkersJson = preferences.getString(KEY_RECOMMENDED_WORKERS, null);
        String userConversationsJson = preferences.getString(KEY_USER_CONVERSATIONS, null);
        Map<String, ?> allEntries = preferences.getAll();
        List<String> chatMessageKeys = new ArrayList<>();
        for (String key : allEntries.keySet()) {
            if (key != null && key.startsWith(KEY_CHAT_MESSAGES_PREFIX)) {
                chatMessageKeys.add(key);
            }
        }

        SharedPreferences.Editor editor = preferences.edit();
        editor.clear();

        if (avatarUri != null && !avatarUri.trim().isEmpty()) {
            editor.putString(KEY_USER_AVATAR_URI, avatarUri);
        }

        if (userDocumentId != null && !userDocumentId.trim().isEmpty()) {
            editor.putString(KEY_USER_DOCUMENT_ID, userDocumentId);
        }

        if (workerServicesJson != null && !workerServicesJson.trim().isEmpty()) {
            editor.putString(KEY_WORKER_SERVICES, workerServicesJson);
        }

        if (userBookingsJson != null && !userBookingsJson.trim().isEmpty()) {
            editor.putString(KEY_USER_BOOKINGS, userBookingsJson);
        }

        if (workerJobRequestsJson != null && !workerJobRequestsJson.trim().isEmpty()) {
            editor.putString(KEY_WORKER_JOB_REQUESTS, workerJobRequestsJson);
        }

        if (recommendedWorkersJson != null && !recommendedWorkersJson.trim().isEmpty()) {
            editor.putString(KEY_RECOMMENDED_WORKERS, recommendedWorkersJson);
        }

        if (userConversationsJson != null && !userConversationsJson.trim().isEmpty()) {
            editor.putString(KEY_USER_CONVERSATIONS, userConversationsJson);
        }

        for (String key : chatMessageKeys) {
            Object value = allEntries.get(key);
            if (value instanceof String) {
                editor.putString(key, (String) value);
            }
        }

        if (storedMerchantId != null && !storedMerchantId.trim().isEmpty()) {
            editor.putString(KEY_PAYHERE_MERCHANT_ID, storedMerchantId);
        }

        if (storedMerchantSecret != null && !storedMerchantSecret.trim().isEmpty()) {
            editor.putString(KEY_PAYHERE_MERCHANT_SECRET, storedMerchantSecret);
        }

        editor.apply();
    }

    public List<WorkerService> getWorkerServices() {
        String rawJson = preferences.getString(KEY_WORKER_SERVICES, null);
        List<WorkerService> services = new ArrayList<>();
        if (rawJson == null || rawJson.trim().isEmpty()) {
            return services;
        }

        try {
            JSONArray array = new JSONArray(rawJson);
            for (int i = 0; i < array.length(); i++) {
                JSONObject object = array.optJSONObject(i);
                if (object == null) {
                    continue;
                }
                services.add(WorkerService.fromJson(object));
            }
        } catch (JSONException ignored) {
            // Corrupted payload; ignore and return empty collection.
        }
        return services;
    }

    public void saveWorkerServices(List<WorkerService> services) {
        JSONArray array = new JSONArray();
        if (services != null) {
            for (WorkerService service : services) {
                if (service == null) {
                    continue;
                }
                try {
                    array.put(service.toJson());
                } catch (JSONException ignored) {
                    // Skip malformed entries.
                }
            }
        }
        preferences.edit().putString(KEY_WORKER_SERVICES, array.toString()).apply();
    }

    public List<RecommendedWorker> getRecommendedWorkers() {
        String rawJson = preferences.getString(KEY_RECOMMENDED_WORKERS, null);
        List<RecommendedWorker> workers = new ArrayList<>();

        if (rawJson != null && !rawJson.trim().isEmpty()) {
            try {
                JSONArray array = new JSONArray(rawJson);
                for (int i = 0; i < array.length(); i++) {
                    JSONObject object = array.optJSONObject(i);
                    RecommendedWorker worker = RecommendedWorker.fromJson(object);
                    if (worker != null) {
                        workers.add(worker);
                    }
                }
            } catch (JSONException ignored) {
                // Ignore malformed payloads
            }
        }

        return workers;
    }

    public void saveRecommendedWorkers(List<RecommendedWorker> workers) {
        JSONArray array = new JSONArray();
        if (workers != null) {
            for (RecommendedWorker worker : workers) {
                if (worker == null) {
                    continue;
                }
                try {
                    array.put(worker.toJson());
                } catch (JSONException ignored) {
                    // Skip invalid entries
                }
            }
        }
        preferences.edit().putString(KEY_RECOMMENDED_WORKERS, array.toString()).apply();
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

    public List<UserBooking> getUserBookings() {
        String rawJson = preferences.getString(KEY_USER_BOOKINGS, null);
        List<UserBooking> bookings = new ArrayList<>();
        if (rawJson == null || rawJson.trim().isEmpty()) {
            return bookings;
        }

        try {
            JSONArray array = new JSONArray(rawJson);
            for (int i = 0; i < array.length(); i++) {
                JSONObject object = array.optJSONObject(i);
                if (object == null) {
                    continue;
                }
                bookings.add(UserBooking.fromJson(object));
            }
        } catch (JSONException ignored) {
            // Ignore malformed payloads
        }
        return bookings;
    }

    public void saveUserBookings(List<UserBooking> bookings) {
        JSONArray array = new JSONArray();
        if (bookings != null) {
            for (UserBooking booking : bookings) {
                if (booking == null) {
                    continue;
                }
                try {
                    array.put(booking.toJson());
                } catch (JSONException ignored) {
                    // Skip malformed entries
                }
            }
        }
        preferences.edit().putString(KEY_USER_BOOKINGS, array.toString()).apply();
    }

    public void addUserBooking(UserBooking booking) {
        if (booking == null) {
            return;
        }
        List<UserBooking> bookings = getUserBookings();
        bookings.add(0, booking);
        saveUserBookings(bookings);
    }

    public void updateBookingStatus(String bookingId, String status, String cancellationReason) {
        if (TextUtils.isEmpty(bookingId) || TextUtils.isEmpty(status)) {
            return;
        }

        List<UserBooking> bookings = getUserBookings();
        boolean changed = false;
        for (int i = 0; i < bookings.size(); i++) {
            UserBooking booking = bookings.get(i);
            if (booking != null && bookingId.equals(booking.getId())) {
                bookings.set(i, booking.withStatus(status, cancellationReason));
                changed = true;
                break;
            }
        }

        if (changed) {
            saveUserBookings(bookings);
        }
    }

    public List<WorkerJobRequest> getWorkerJobRequests() {
        String rawJson = preferences.getString(KEY_WORKER_JOB_REQUESTS, null);
        List<WorkerJobRequest> requests = new ArrayList<>();
        if (rawJson == null || rawJson.trim().isEmpty()) {
            return requests;
        }

        try {
            JSONArray array = new JSONArray(rawJson);
            for (int i = 0; i < array.length(); i++) {
                JSONObject object = array.optJSONObject(i);
                if (object == null) {
                    continue;
                }
                requests.add(WorkerJobRequest.fromJson(object));
            }
        } catch (JSONException ignored) {
            // Ignore malformed payloads
        }
        return requests;
    }

    public void saveWorkerJobRequests(List<WorkerJobRequest> requests) {
        JSONArray array = new JSONArray();
        if (requests != null) {
            for (WorkerJobRequest request : requests) {
                if (request == null) {
                    continue;
                }
                try {
                    array.put(request.toJson());
                } catch (JSONException ignored) {
                    // Skip malformed entries
                }
            }
        }
        preferences.edit().putString(KEY_WORKER_JOB_REQUESTS, array.toString()).apply();
    }

    public void addWorkerJobRequest(WorkerJobRequest request) {
        if (request == null) {
            return;
        }

        List<WorkerJobRequest> requests = getWorkerJobRequests();
        boolean replaced = false;
        for (int i = 0; i < requests.size(); i++) {
            WorkerJobRequest existing = requests.get(i);
            if (existing != null && request.getId().equals(existing.getId())) {
                requests.set(i, request);
                replaced = true;
                break;
            }
        }

        if (!replaced) {
            requests.add(0, request);
        }

        saveWorkerJobRequests(requests);
    }

    public void upsertWorkerService(WorkerService candidate) {
        if (candidate == null) {
            return;
        }

        // Ensure the service has proper owner metadata
        String currentWorkerId = getOrCreateWorkerDocumentId();
        if (TextUtils.isEmpty(candidate.getOwnerId())) {
            candidate = candidate.withOwner(currentWorkerId, getUserName(), getUserEmail());
        }

        // Get all existing services (global list)
        List<WorkerService> services = getWorkerServices();
        boolean updated = false;
        for (int i = 0; i < services.size(); i++) {
            WorkerService existing = services.get(i);
            if (candidate.getId().equals(existing.getId())) {
                services.set(i, candidate);
                updated = true;
                break;
            }
        }

        if (!updated) {
            services.add(candidate);
        }

        // Save the updated global services list
        saveWorkerServices(services);
    }

    public void deleteWorkerService(String serviceId) {
        if (serviceId == null) {
            return;
        }

        List<WorkerService> services = getWorkerServices();
        Iterator<WorkerService> iterator = services.iterator();
        boolean changed = false;
        while (iterator.hasNext()) {
            WorkerService service = iterator.next();
            if (serviceId.equals(service.getId())) {
                iterator.remove();
                changed = true;
                break;
            }
        }

        if (changed) {
            saveWorkerServices(services);
        }
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

    public List<UserConversation> getUserConversations() {
        String rawJson = preferences.getString(KEY_USER_CONVERSATIONS, null);
        List<UserConversation> conversations = new ArrayList<>();
        if (rawJson == null || rawJson.trim().isEmpty()) {
            return conversations;
        }

        try {
            JSONArray array = new JSONArray(rawJson);
            for (int i = 0; i < array.length(); i++) {
                JSONObject object = array.optJSONObject(i);
                if (object == null) {
                    continue;
                }
                try {
                    conversations.add(UserConversation.fromJson(object));
                } catch (JSONException ignored) {
                    // Skip malformed entries
                }
            }
        } catch (JSONException ignored) {
            // Ignore malformed payload and return accumulated list
        }

        Collections.sort(conversations, new Comparator<UserConversation>() {
            @Override
            public int compare(UserConversation o1, UserConversation o2) {
                return Long.compare(o2.getLastMessageTimestamp(), o1.getLastMessageTimestamp());
            }
        });
        return conversations;
    }

    public void saveUserConversations(List<UserConversation> conversations) {
        JSONArray array = new JSONArray();
        if (conversations != null) {
            for (UserConversation conversation : conversations) {
                if (conversation == null) {
                    continue;
                }
                try {
                    array.put(conversation.toJson());
                } catch (JSONException ignored) {
                    // Skip malformed entries
                }
            }
        }
        preferences.edit().putString(KEY_USER_CONVERSATIONS, array.toString()).apply();
    }

    public void upsertUserConversation(UserConversation incoming) {
        if (incoming == null || incoming.getId() == null) {
            return;
        }

        List<UserConversation> conversations = getUserConversations();
        boolean replaced = false;
        for (int i = 0; i < conversations.size(); i++) {
            UserConversation existing = conversations.get(i);
            if (existing != null && incoming.getId().equals(existing.getId())) {
                conversations.set(i, mergeConversations(existing, incoming));
                replaced = true;
                break;
            }
        }

        if (!replaced) {
            conversations.add(incoming);
        }

        Collections.sort(conversations, new Comparator<UserConversation>() {
            @Override
            public int compare(UserConversation o1, UserConversation o2) {
                return Long.compare(o2.getLastMessageTimestamp(), o1.getLastMessageTimestamp());
            }
        });

        saveUserConversations(conversations);
    }

    public void markConversationAsRead(String conversationId) {
        if (conversationId == null) {
            return;
        }

        List<UserConversation> conversations = getUserConversations();
        boolean changed = false;
        for (int i = 0; i < conversations.size(); i++) {
            UserConversation conversation = conversations.get(i);
            if (conversation != null && conversationId.equals(conversation.getId()) && conversation.hasUnread()) {
                conversations.set(i, conversation.withLastMessage(conversation.getLastMessage(), conversation.getLastMessageTimestamp(), false));
                changed = true;
                break;
            }
        }

        if (changed) {
            saveUserConversations(conversations);
        }
    }

    public List<UserChatMessage> getChatMessages(String conversationId) {
        List<UserChatMessage> messages = new ArrayList<>();
        if (conversationId == null) {
            return messages;
        }

        String rawJson = preferences.getString(KEY_CHAT_MESSAGES_PREFIX + conversationId, null);
        if (rawJson == null || rawJson.trim().isEmpty()) {
            return messages;
        }

        try {
            JSONArray array = new JSONArray(rawJson);
            for (int i = 0; i < array.length(); i++) {
                JSONObject object = array.optJSONObject(i);
                if (object == null) {
                    continue;
                }
                try {
                    messages.add(UserChatMessage.fromJson(object));
                } catch (JSONException ignored) {
                    // Skip malformed entries
                }
            }
        } catch (JSONException ignored) {
            // Ignore malformed payloads
        }

        Collections.sort(messages, new Comparator<UserChatMessage>() {
            @Override
            public int compare(UserChatMessage o1, UserChatMessage o2) {
                return Long.compare(o1.getTimestamp(), o2.getTimestamp());
            }
        });

        return messages;
    }

    public void saveChatMessages(String conversationId, List<UserChatMessage> messages) {
        if (conversationId == null) {
            return;
        }

        JSONArray array = new JSONArray();
        if (messages != null) {
            for (UserChatMessage message : messages) {
                if (message == null) {
                    continue;
                }
                try {
                    array.put(message.toJson());
                } catch (JSONException ignored) {
                    // Skip malformed entries
                }
            }
        }
        preferences.edit().putString(KEY_CHAT_MESSAGES_PREFIX + conversationId, array.toString()).apply();
    }

    private UserConversation mergeConversations(UserConversation existing, UserConversation incoming) {
        if (existing == null) {
            return incoming;
        }
        if (incoming == null) {
            return existing;
        }

        String id = incoming.getId() != null ? incoming.getId() : existing.getId();
        String workerId = firstNonEmpty(incoming.getWorkerId(), existing.getWorkerId());
        String title = firstNonEmpty(incoming.getTitle(), existing.getTitle());
        String subtitle = firstNonEmpty(incoming.getSubtitle(), existing.getSubtitle());
        String serviceId = firstNonEmpty(incoming.getServiceId(), existing.getServiceId());
        String serviceName = firstNonEmpty(incoming.getServiceName(), existing.getServiceName());
        String serviceCategory = firstNonEmpty(incoming.getServiceCategory(), existing.getServiceCategory());
        String workerImageUri = firstNonEmpty(incoming.getWorkerImageUri(), existing.getWorkerImageUri());
        String lastMessage = incoming.getLastMessage() != null ? incoming.getLastMessage() : existing.getLastMessage();
        long lastTimestamp = incoming.getLastMessageTimestamp() > 0 ? incoming.getLastMessageTimestamp() : existing.getLastMessageTimestamp();
        boolean hasUnread = incoming.hasUnread();

        return new UserConversation(
                id,
                workerId,
                title,
                subtitle,
                serviceId,
                serviceName,
                serviceCategory,
                workerImageUri,
                lastMessage,
                lastTimestamp,
                hasUnread
        );
    }

    private String firstNonEmpty(String primary, String fallback) {
        if (primary != null && !primary.trim().isEmpty()) {
            return primary;
        }
        return fallback;
    }

    private static final List<ServiceArea> DEFAULT_SERVICE_AREAS;

    static {
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
        DEFAULT_SERVICE_AREAS = Collections.unmodifiableList(areas);
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
                String.format(Locale.getDefault(), "LKR %,d / hr", 4800),
                String.format(Locale.getDefault(), "%.1f km away", 2.1),
                buildResourceUri(R.drawable.ic_worker_expert_amal),
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
                String.format(Locale.getDefault(), "LKR %,d / visit", 5500),
                String.format(Locale.getDefault(), "%.1f km away", 3.4),
                buildResourceUri(R.drawable.ic_worker_expert_nadeesha),
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
                String.format(Locale.getDefault(), "LKR %,d / project", 12500),
                String.format(Locale.getDefault(), "%.1f km away", 5.6),
                buildResourceUri(R.drawable.ic_worker_expert_isuru),
                null,
                "Gardening",
                Arrays.asList("Garden design", "Irrigation setup", "Seasonal maintenance"),
                "Galle",
                6.053519,
                80.220978,
                25d));

        return defaults;
    }

    private List<WorkerService> createDefaultWorkerServices() {
        List<WorkerService> defaults = new ArrayList<>();

        String electricianKey = ServiceCategoryRegistry.resolveKey("Electrician");
        if (TextUtils.isEmpty(electricianKey)) {
            electricianKey = "electrician";
        }
        WorkerService colomboElectric = WorkerService.create(
                "worker_amal_perera",
                "Amal Perera",
                "amal.perera@skilllink.lk",
                electricianKey,
                "Emergency Electrical Repairs",
                "Certified electrician for urgent fixes, inspections, and safety upgrades across Colombo.",
                WorkerService.PRICE_TYPE_HOURLY,
                "4800",
                buildResourceUri(R.drawable.ic_worker_expert_amal)
        ).withLocation("Colombo", 6.927079, 79.861244, 18d);
        defaults.add(colomboElectric);

        String plumberKey = ServiceCategoryRegistry.resolveKey("Plumber");
        if (TextUtils.isEmpty(plumberKey)) {
            plumberKey = "plumber";
        }
        WorkerService kandyPlumber = WorkerService.create(
                "worker_nadeesha_silva",
                "Nadeesha Silva",
                "nadeesha.silva@skilllink.lk",
                plumberKey,
                "Heritage Property Plumbing",
                "Specialised maintenance for classic homes, leak repairs, and bathroom renovations in Kandy.",
                WorkerService.PRICE_TYPE_CUSTOM,
                "17500",
                buildResourceUri(R.drawable.ic_worker_expert_nadeesha)
        ).withLocation("Kandy", 7.290572, 80.633728, 22d);
        defaults.add(kandyPlumber);

        String gardenerKey = ServiceCategoryRegistry.resolveKey("Gardener");
        if (TextUtils.isEmpty(gardenerKey)) {
            gardenerKey = "gardener";
        }
        WorkerService galleGardener = WorkerService.create(
                "worker_isuru_jayasinghe",
                "Isuru Jayasinghe",
                "isuru.jayasinghe@skilllink.lk",
                gardenerKey,
                "Coastal Landscape Care",
                "Resilient plant selections, irrigation setup, and routine upkeep for seaside villas in Galle.",
                WorkerService.PRICE_TYPE_CUSTOM,
                "24000",
                buildResourceUri(R.drawable.ic_worker_expert_isuru)
        ).withLocation("Galle", 6.053519, 80.220978, 25d);
        defaults.add(galleGardener);

        String mechanicKey = ServiceCategoryRegistry.resolveKey("Mechanic");
        if (TextUtils.isEmpty(mechanicKey)) {
            mechanicKey = "mechanic";
        }
        WorkerService negomboMechanic = WorkerService.create(
                "worker_shehan_fernando",
                "Shehan Fernando",
                "shehan.fernando@skilllink.lk",
                mechanicKey,
                "Mobile Vehicle Diagnostics",
                "On-site engine diagnostics, roadside support, and preventive servicing covering Negombo and surrounds.",
                WorkerService.PRICE_TYPE_HOURLY,
                "6500",
                buildResourceUri(R.drawable.ic_worker_expert_amal)
        ).withLocation("Negombo", 7.20084, 79.87366, 28d);
        defaults.add(negomboMechanic);

        return defaults;
    }

    private String buildResourceUri(int drawableRes) {
        return "android.resource://" + appContext.getPackageName() + "/" + drawableRes;
    }

    public List<ServiceArea> getServiceAreas() {
        return DEFAULT_SERVICE_AREAS;
    }

    @Nullable
    public ServiceArea findServiceAreaByName(@Nullable String areaName) {
        if (areaName == null) {
            return null;
        }
        for (ServiceArea area : DEFAULT_SERVICE_AREAS) {
            if (area != null && areaName.equalsIgnoreCase(area.getName())) {
                return area;
            }
        }
        return null;
    }

    public List<String> getRecentServiceAreas() {
        String raw = preferences.getString(KEY_RECENT_SERVICE_AREAS, null);
        List<String> recent = new ArrayList<>();
        if (raw == null || raw.trim().isEmpty()) {
            return recent;
        }
        try {
            JSONArray array = new JSONArray(raw);
            for (int i = 0; i < array.length(); i++) {
                String value = array.optString(i, null);
                if (value != null && !value.trim().isEmpty()) {
                    recent.add(value.trim());
                }
            }
        } catch (JSONException ignored) {
        }
        return recent;
    }

    public void addRecentServiceArea(String areaName) {
        if (areaName == null || areaName.trim().isEmpty()) {
            return;
        }
        String trimmed = areaName.trim();
        List<String> current = new ArrayList<>(getRecentServiceAreas());
        current.removeIf(value -> value.equalsIgnoreCase(trimmed));
        current.add(0, trimmed);
        while (current.size() > 6) {
            current.remove(current.size() - 1);
        }

        JSONArray array = new JSONArray();
        for (String value : current) {
            array.put(value);
        }
        preferences.edit().putString(KEY_RECENT_SERVICE_AREAS, array.toString()).apply();
    }

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

        JSONObject toJson() {
            JSONObject object = new JSONObject();
            try {
                object.put("id", id);
                object.put("holder", holderName);
                object.put("brand", brand);
                object.put("last4", last4);
                object.put("expiry", expiry);
                object.put("savedAt", savedAt);
            } catch (JSONException ignored) {
            }
            return object;
        }

        static SavedCard fromJson(@Nullable JSONObject object) {
            if (object == null) {
                return null;
            }
            String id = object.optString("id", null);
            if (TextUtils.isEmpty(id)) {
                return null;
            }
            return new SavedCard(
                    id,
                    object.optString("holder", null),
                    object.optString("brand", "Card"),
                    object.optString("last4", null),
                    object.optString("expiry", null),
                    object.optLong("savedAt", System.currentTimeMillis())
            );
        }
    }
}
