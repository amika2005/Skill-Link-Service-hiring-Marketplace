package com.skilllink.model;

import com.google.firebase.firestore.DocumentSnapshot;

import org.json.JSONException;
import org.json.JSONObject;

import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;

public class UserBooking {

    private final String id;
    private final String serviceId;
    private final String serviceName;
    private final String serviceCategory;
    private final String scheduledDate;
    private final String scheduledTime;
    private final String location;
    private final String notes;
    private final String paymentMethod;
    private final String priceDisplay;
    private final String imageUri;
    private final String status;
    private final String cancellationReason;
    private final long createdAt;

    private UserBooking(String id,
                        String serviceId,
                        String serviceName,
                        String serviceCategory,
                        String scheduledDate,
                        String scheduledTime,
                        String location,
                        String notes,
                        String paymentMethod,
                        String priceDisplay,
                        String imageUri,
                        String status,
                        String cancellationReason,
                        long createdAt) {
        this.id = id;
        this.serviceId = serviceId;
        this.serviceName = serviceName;
        this.serviceCategory = serviceCategory;
        this.scheduledDate = scheduledDate;
        this.scheduledTime = scheduledTime;
        this.location = location;
        this.notes = notes;
        this.paymentMethod = paymentMethod;
        this.priceDisplay = priceDisplay;
        this.imageUri = imageUri;
        this.status = status;
        this.cancellationReason = cancellationReason;
        this.createdAt = createdAt;
    }

    public static UserBooking create(String serviceId,
                                     String serviceName,
                                     String serviceCategory,
                                     String scheduledDate,
                                     String scheduledTime,
                                     String location,
                                     String notes,
                                     String paymentMethod,
                                     String priceDisplay,
                                     String imageUri) {
        long timestamp = System.currentTimeMillis();
        String identifier = UUID.randomUUID().toString();
        return new UserBooking(
                identifier,
                serviceId,
                serviceName,
                serviceCategory,
                scheduledDate,
                scheduledTime,
                location,
                notes,
                paymentMethod,
                priceDisplay,
                imageUri,
                "Scheduled",
                null,
                timestamp
        );
    }

    public static UserBooking fromJson(JSONObject object) throws JSONException {
        return new UserBooking(
                object.optString("id"),
                object.optString("serviceId"),
                object.optString("serviceName"),
                object.optString("serviceCategory"),
                object.optString("scheduledDate"),
                object.optString("scheduledTime"),
                object.optString("location"),
                object.optString("notes"),
                object.optString("paymentMethod"),
                object.optString("priceDisplay"),
                object.optString("imageUri"),
                object.optString("status", "Scheduled"),
                object.optString("cancellationReason", null),
                object.optLong("createdAt", System.currentTimeMillis())
        );
    }

    public JSONObject toJson() throws JSONException {
        JSONObject object = new JSONObject();
        object.put("id", id);
        object.put("serviceId", serviceId);
        object.put("serviceName", serviceName);
        object.put("serviceCategory", serviceCategory);
        object.put("scheduledDate", scheduledDate);
        object.put("scheduledTime", scheduledTime);
        object.put("location", location);
        object.put("notes", notes);
        object.put("paymentMethod", paymentMethod);
        object.put("priceDisplay", priceDisplay);
        object.put("imageUri", imageUri);
        object.put("status", status);
        if (cancellationReason != null) {
            object.put("cancellationReason", cancellationReason);
        }
        object.put("createdAt", createdAt);
        return object;
    }

    public String getId() {
        return id;
    }

    public String getServiceId() {
        return serviceId;
    }

    public String getServiceName() {
        return serviceName;
    }

    public String getServiceCategory() {
        return serviceCategory;
    }

    public String getScheduledDate() {
        return scheduledDate;
    }

    public String getScheduledTime() {
        return scheduledTime;
    }

    public String getScheduleDisplay() {
        if (TextUtilsCompat.isEmpty(scheduledDate) && TextUtilsCompat.isEmpty(scheduledTime)) {
            return "";
        }
        if (TextUtilsCompat.isEmpty(scheduledDate)) {
            return scheduledTime;
        }
        if (TextUtilsCompat.isEmpty(scheduledTime)) {
            return scheduledDate;
        }
        return String.format(Locale.getDefault(), "%1$s · %2$s", scheduledDate, scheduledTime);
    }

    public String getLocation() {
        return location;
    }

    public String getNotes() {
        return notes;
    }

    public String getPaymentMethod() {
        return paymentMethod;
    }

    public String getPriceDisplay() {
        return priceDisplay;
    }

    public String getImageUri() {
        return imageUri;
    }

    public String getStatus() {
        return status;
    }

    public String getCancellationReason() {
        return cancellationReason;
    }

    public long getCreatedAt() {
        return createdAt;
    }

    public UserBooking withStatus(String status, String cancellationReason) {
        return new UserBooking(
                id,
                serviceId,
                serviceName,
                serviceCategory,
                scheduledDate,
                scheduledTime,
                location,
                notes,
                paymentMethod,
                priceDisplay,
                imageUri,
                status,
                cancellationReason,
                createdAt
        );
    }

    public Map<String, Object> toMap() {
        Map<String, Object> map = new HashMap<>();
        map.put("id", id);
        map.put("serviceId", serviceId);
        map.put("serviceName", serviceName);
        map.put("serviceCategory", serviceCategory);
        map.put("scheduledDate", scheduledDate);
        map.put("scheduledTime", scheduledTime);
        map.put("location", location);
        map.put("notes", notes);
        map.put("paymentMethod", paymentMethod);
        map.put("priceDisplay", priceDisplay);
        map.put("imageUri", imageUri);
        map.put("status", status);
        map.put("cancellationReason", cancellationReason);
        map.put("createdAt", createdAt);
        return map;
    }

    public static UserBooking fromDocument(DocumentSnapshot doc) {
        if (doc == null || !doc.exists()) return null;
        return new UserBooking(
                doc.getString("id"),
                doc.getString("serviceId"),
                doc.getString("serviceName"),
                doc.getString("serviceCategory"),
                doc.getString("scheduledDate"),
                doc.getString("scheduledTime"),
                doc.getString("location"),
                doc.getString("notes"),
                doc.getString("paymentMethod"),
                doc.getString("priceDisplay"),
                doc.getString("imageUri"),
                doc.getString("status"),
                doc.getString("cancellationReason"),
                doc.getLong("createdAt") != null ? doc.getLong("createdAt") : System.currentTimeMillis()
        );
    }

    private static final class TextUtilsCompat {
        static boolean isEmpty(CharSequence sequence) {
            return sequence == null || sequence.length() == 0;
        }
    }
}
