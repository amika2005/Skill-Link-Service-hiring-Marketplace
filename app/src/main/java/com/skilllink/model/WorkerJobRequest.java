package com.skilllink.model;

import com.google.firebase.firestore.DocumentSnapshot;

import org.json.JSONException;
import org.json.JSONObject;

import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

/**
 * Represents a job request that gets surfaced to a worker whenever a customer completes a booking.
 */
public class WorkerJobRequest {

    private static final String DEFAULT_STATUS_PENDING = "Pending";

    private final String id;
    private final String bookingId;
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
    private final String customerName;
    private final String customerPhone;
    private final String serviceOwnerId;
    private final String serviceOwnerName;
    private final String serviceOwnerEmail;
    private final String status;
    private final String cancellationReason;
    private final long createdAt;
    private final long cancelledAt;

    private WorkerJobRequest(String id,
                              String bookingId,
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
                              String customerName,
                              String customerPhone,
                              String serviceOwnerId,
                              String serviceOwnerName,
                              String serviceOwnerEmail,
                              String status,
                              String cancellationReason,
                              long createdAt,
                              long cancelledAt) {
        this.id = id;
        this.bookingId = bookingId;
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
        this.customerName = customerName;
        this.customerPhone = customerPhone;
        this.serviceOwnerId = serviceOwnerId;
        this.serviceOwnerName = serviceOwnerName;
        this.serviceOwnerEmail = serviceOwnerEmail;
        this.status = status;
        this.cancellationReason = cancellationReason;
        this.createdAt = createdAt;
        this.cancelledAt = cancelledAt;
    }

    public static WorkerJobRequest createFromBooking(UserBooking booking,
                                                     String customerName,
                                                     String customerPhone) {
        if (booking == null) {
            throw new IllegalArgumentException("booking must not be null");
        }

        String resolvedCustomerName = TextUtilsCompat.isEmpty(customerName)
                ? "Customer"
                : customerName;

        String resolvedCustomerPhone = TextUtilsCompat.isEmpty(customerPhone)
                ? ""
                : customerPhone;

        return new WorkerJobRequest(
                booking.getId(),
                booking.getId(),
                booking.getServiceId(),
                booking.getServiceName(),
                booking.getServiceCategory(),
                booking.getScheduledDate(),
                booking.getScheduledTime(),
                booking.getLocation(),
                booking.getNotes(),
                booking.getPaymentMethod(),
                booking.getPriceDisplay(),
                booking.getImageUri(),
                resolvedCustomerName,
                resolvedCustomerPhone,
                null, // serviceOwnerId
                null, // serviceOwnerName
                null, // serviceOwnerEmail
                DEFAULT_STATUS_PENDING,
                null, // cancellationReason
                booking.getCreatedAt(),
                0L // cancelledAt
        );
    }

    public static WorkerJobRequest createFromBooking(UserBooking booking,
                                                     String customerName,
                                                     String customerPhone,
                                                     String serviceOwnerId,
                                                     String serviceOwnerName,
                                                     String serviceOwnerEmail) {
        if (booking == null) {
            throw new IllegalArgumentException("booking must not be null");
        }

        String resolvedCustomerName = TextUtilsCompat.isEmpty(customerName)
                ? "Customer"
                : customerName;

        String resolvedCustomerPhone = TextUtilsCompat.isEmpty(customerPhone)
                ? ""
                : customerPhone;

        String resolvedServiceOwnerId = TextUtilsCompat.isEmpty(serviceOwnerId)
                ? null
                : serviceOwnerId;

        String resolvedServiceOwnerName = TextUtilsCompat.isEmpty(serviceOwnerName)
                ? null
                : serviceOwnerName;

        String resolvedServiceOwnerEmail = TextUtilsCompat.isEmpty(serviceOwnerEmail)
                ? null
                : serviceOwnerEmail;

        return new WorkerJobRequest(
                booking.getId(),
                booking.getId(),
                booking.getServiceId(),
                booking.getServiceName(),
                booking.getServiceCategory(),
                booking.getScheduledDate(),
                booking.getScheduledTime(),
                booking.getLocation(),
                booking.getNotes(),
                booking.getPaymentMethod(),
                booking.getPriceDisplay(),
                booking.getImageUri(),
                resolvedCustomerName,
                resolvedCustomerPhone,
                resolvedServiceOwnerId,
                resolvedServiceOwnerName,
                resolvedServiceOwnerEmail,
                DEFAULT_STATUS_PENDING,
                null, // cancellationReason
                booking.getCreatedAt(),
                0L // cancelledAt
        );
    }

    public static WorkerJobRequest fromJson(JSONObject object) throws JSONException {
        return new WorkerJobRequest(
                object.optString("id"),
                object.optString("bookingId"),
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
                object.optString("customerName"),
                object.optString("customerPhone"),
                object.optString("serviceOwnerId"),
                object.optString("serviceOwnerName"),
                object.optString("serviceOwnerEmail"),
                object.optString("status", DEFAULT_STATUS_PENDING),
                object.optString("cancellationReason", null),
                object.optLong("createdAt", System.currentTimeMillis()),
                object.optLong("cancelledAt", 0L)
        );
    }

    public JSONObject toJson() throws JSONException {
        JSONObject object = new JSONObject();
        object.put("id", id);
        object.put("bookingId", bookingId);
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
        object.put("customerName", customerName);
        object.put("customerPhone", customerPhone);
        object.put("status", status);
        if (cancellationReason != null) {
            object.put("cancellationReason", cancellationReason);
        }
        object.put("createdAt", createdAt);
        if (cancelledAt > 0) {
            object.put("cancelledAt", cancelledAt);
        }
        return object;
    }

    public String getId() {
        return id;
    }

    public String getBookingId() {
        return bookingId;
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

    public String getCustomerName() {
        return customerName;
    }

    public String getCustomerPhone() {
        return customerPhone;
    }

    public String getStatus() {
        return status;
    }

    public long getCreatedAt() {
        return createdAt;
    }

    public String getScheduleDisplay() {
        boolean hasDate = !TextUtilsCompat.isEmpty(scheduledDate);
        boolean hasTime = !TextUtilsCompat.isEmpty(scheduledTime);

        if (!hasDate && !hasTime) {
            return "";
        }
        if (!hasDate) {
            return scheduledTime;
        }
        if (!hasTime) {
            return scheduledDate;
        }
        return String.format(Locale.getDefault(), "%1$s · %2$s", scheduledDate, scheduledTime);
    }

    public WorkerJobRequest withStatus(String newStatus) {
        return new WorkerJobRequest(
                id,
                bookingId,
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
                customerName,
                customerPhone,
                serviceOwnerId,
                serviceOwnerName,
                serviceOwnerEmail,
                TextUtilsCompat.isEmpty(newStatus) ? status : newStatus,
                cancellationReason,
                createdAt,
                cancelledAt
        );
    }

    public WorkerJobRequest withCancellation(String reason) {
        return new WorkerJobRequest(
                id,
                bookingId,
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
                customerName,
                customerPhone,
                serviceOwnerId,
                serviceOwnerName,
                serviceOwnerEmail,
                "Cancelled",
                reason,
                createdAt,
                System.currentTimeMillis()
        );
    }

    public String getCancellationReason() {
        return cancellationReason;
    }

    public long getCancelledAt() {
        return cancelledAt;
    }

    public String getServiceOwnerId() {
        return serviceOwnerId;
    }

    public String getServiceOwnerName() {
        return serviceOwnerName;
    }

    public String getServiceOwnerEmail() {
        return serviceOwnerEmail;
    }

    public Map<String, Object> toMap() {
        Map<String, Object> map = new HashMap<>();
        map.put("id", id);
        map.put("bookingId", bookingId);
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
        map.put("customerName", customerName);
        map.put("customerPhone", customerPhone);
        map.put("serviceOwnerId", serviceOwnerId);
        map.put("serviceOwnerName", serviceOwnerName);
        map.put("serviceOwnerEmail", serviceOwnerEmail);
        map.put("status", status);
        map.put("cancellationReason", cancellationReason);
        map.put("createdAt", createdAt);
        map.put("cancelledAt", cancelledAt);
        return map;
    }

    public static WorkerJobRequest fromDocument(DocumentSnapshot doc) {
        if (doc == null || !doc.exists()) return null;
        return new WorkerJobRequest(
                doc.getString("id"),
                doc.getString("bookingId"),
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
                doc.getString("customerName"),
                doc.getString("customerPhone"),
                doc.getString("serviceOwnerId"),
                doc.getString("serviceOwnerName"),
                doc.getString("serviceOwnerEmail"),
                doc.getString("status"),
                doc.getString("cancellationReason"),
                doc.getLong("createdAt") != null ? doc.getLong("createdAt") : System.currentTimeMillis(),
                doc.getLong("cancelledAt") != null ? doc.getLong("cancelledAt") : 0L
        );
    }

    private static final class TextUtilsCompat {
        static boolean isEmpty(CharSequence sequence) {
            return sequence == null || sequence.length() == 0;
        }
    }
}
