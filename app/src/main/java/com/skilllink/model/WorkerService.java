package com.skilllink.model;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.google.firebase.firestore.DocumentSnapshot;

import org.json.JSONException;
import org.json.JSONObject;

import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;

public class WorkerService {

    public static final String PRICE_TYPE_HOURLY = "HOURLY";
    public static final String PRICE_TYPE_CUSTOM = "CUSTOM";

    private final String id;
    private final String category;
    private final String name;
    private final String bio;
    private final String priceType;
    private final String priceValue;
    private final String imageUri;
    private final String ownerId;
    private final String ownerName;
    private final String ownerEmail;
    private final long updatedAt;
    private final String serviceArea;
    private final double latitude;
    private final double longitude;
    private final double coverageRadiusKm;

    public WorkerService(String id,
                         String category,
                         String name,
                         String bio,
                         String priceType,
                         String priceValue,
                         String imageUri,
                         @Nullable String ownerId,
                         @Nullable String ownerName,
                         @Nullable String ownerEmail,
                         long updatedAt) {
        this(id, category, name, bio, priceType, priceValue, imageUri, ownerId, ownerName, ownerEmail, null, Double.NaN, Double.NaN, 20d, updatedAt);
    }

    public WorkerService(String id,
                          String category,
                          String name,
                          String bio,
                          String priceType,
                          String priceValue,
                          String imageUri,
                          @Nullable String ownerId,
                          @Nullable String ownerName,
                          @Nullable String ownerEmail,
                          @Nullable String serviceArea,
                          double latitude,
                          double longitude,
                          double coverageRadiusKm,
                          long updatedAt) {
        this.id = id != null ? id : UUID.randomUUID().toString();
        this.category = category;
        this.name = name;
        this.bio = bio;
        this.priceType = priceType;
        this.priceValue = priceValue;
        this.imageUri = imageUri;
        this.ownerId = ownerId;
        this.ownerName = ownerName;
        this.ownerEmail = ownerEmail;
        this.serviceArea = serviceArea;
        this.latitude = latitude;
        this.longitude = longitude;
        this.coverageRadiusKm = coverageRadiusKm > 0 ? coverageRadiusKm : 20d;
        this.updatedAt = updatedAt > 0 ? updatedAt : System.currentTimeMillis();
    }

    public static WorkerService create(@Nullable String ownerId,
                                       @Nullable String ownerName,
                                       @Nullable String ownerEmail,
                                       String category,
                                       String name,
                                       String bio,
                                       String priceType,
                                       String priceValue,
                                       String imageUri) {
        return new WorkerService(null, category, name, bio, priceType, priceValue, imageUri, ownerId, ownerName, ownerEmail, System.currentTimeMillis());
    }

    public String getId() {
        return id;
    }

    public String getCategory() {
        return category;
    }

    public String getName() {
        return name;
    }

    public String getBio() {
        return bio;
    }

    public String getPriceType() {
        return priceType;
    }

    public boolean isHourlyPricing() {
        return PRICE_TYPE_HOURLY.equalsIgnoreCase(priceType);
    }

    public String getPriceValue() {
        return priceValue;
    }

    public String getImageUri() {
        return imageUri;
    }

    @Nullable
    public String getOwnerId() {
        return ownerId;
    }

    @Nullable
    public String getOwnerName() {
        return ownerName;
    }

    @Nullable
    public String getOwnerEmail() {
        return ownerEmail;
    }

    public long getUpdatedAt() {
        return updatedAt;
    }

    @Nullable
    public String getServiceArea() {
        return serviceArea;
    }

    public double getLatitude() {
        return latitude;
    }

    public double getLongitude() {
        return longitude;
    }

    public double getCoverageRadiusKm() {
        return coverageRadiusKm;
    }

    public JSONObject toJson() throws JSONException {
        JSONObject object = new JSONObject();
        object.put("id", id);
        object.put("category", category);
        object.put("name", name);
        object.put("bio", bio);
        object.put("priceType", priceType);
        object.put("priceValue", priceValue);
        object.put("imageUri", imageUri);
        object.put("ownerId", ownerId);
        object.put("ownerName", ownerName);
        object.put("ownerEmail", ownerEmail);
        object.put("updatedAt", updatedAt);
        object.put("serviceArea", serviceArea);
        object.put("latitude", latitude);
        object.put("longitude", longitude);
        object.put("coverageRadiusKm", coverageRadiusKm);
        return object;
    }

    public static WorkerService fromJson(JSONObject object) throws JSONException {
        return new WorkerService(
                object.optString("id", null),
                object.optString("category", null),
                object.optString("name", null),
                object.optString("bio", null),
                object.optString("priceType", PRICE_TYPE_CUSTOM),
                object.optString("priceValue", null),
                object.optString("imageUri", null),
                object.optString("ownerId", null),
                object.optString("ownerName", null),
                object.optString("ownerEmail", null),
                object.optString("serviceArea", null),
                object.optDouble("latitude", Double.NaN),
                object.optDouble("longitude", Double.NaN),
                object.optDouble("coverageRadiusKm", 20d),
                object.optLong("updatedAt", 0)
        );
    }

    public WorkerService withUpdatedImage(String newImageUri) {
        return new WorkerService(id, category, name, bio, priceType, priceValue, newImageUri, ownerId, ownerName, ownerEmail, serviceArea, latitude, longitude, coverageRadiusKm, System.currentTimeMillis());
    }

    public WorkerService withUpdatedDetails(String newCategory,
                                            String newName,
                                            String newBio,
                                            String newPriceType,
                                            String newPriceValue,
                                            String newImageUri) {
        return new WorkerService(id, newCategory, newName, newBio, newPriceType, newPriceValue, newImageUri, ownerId, ownerName, ownerEmail, serviceArea, latitude, longitude, coverageRadiusKm, System.currentTimeMillis());
    }

    public WorkerService withOwner(@Nullable String newOwnerId,
                                   @Nullable String newOwnerName,
                                   @Nullable String newOwnerEmail) {
        return new WorkerService(id, category, name, bio, priceType, priceValue, imageUri, newOwnerId, newOwnerName, newOwnerEmail, serviceArea, latitude, longitude, coverageRadiusKm, System.currentTimeMillis());
    }

    public WorkerService withLocation(@Nullable String newServiceArea,
                                      double newLatitude,
                                      double newLongitude,
                                      double newCoverageRadiusKm) {
        return new WorkerService(id, category, name, bio, priceType, priceValue, imageUri, ownerId, ownerName, ownerEmail, newServiceArea, newLatitude, newLongitude, newCoverageRadiusKm, System.currentTimeMillis());
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof WorkerService)) return false;
        WorkerService that = (WorkerService) o;
        return Objects.equals(id, that.id);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id);
    }

    public Map<String, Object> toMap() {
        Map<String, Object> map = new HashMap<>();
        map.put("id", id);
        map.put("category", category);
        map.put("name", name);
        map.put("bio", bio);
        map.put("priceType", priceType);
        map.put("priceValue", priceValue);
        map.put("imageUri", imageUri);
        map.put("ownerId", ownerId);
        map.put("ownerName", ownerName);
        map.put("ownerEmail", ownerEmail);
        map.put("updatedAt", updatedAt);
        map.put("serviceArea", serviceArea);
        map.put("latitude", latitude);
        map.put("longitude", longitude);
        map.put("coverageRadiusKm", coverageRadiusKm);
        return map;
    }

    public static WorkerService fromDocument(DocumentSnapshot doc) {
        if (doc == null || !doc.exists()) return null;
        return new WorkerService(
                doc.getString("id"),
                doc.getString("category"),
                doc.getString("name"),
                doc.getString("bio"),
                doc.getString("priceType"),
                doc.getString("priceValue"),
                doc.getString("imageUri"),
                doc.getString("ownerId"),
                doc.getString("ownerName"),
                doc.getString("ownerEmail"),
                doc.getString("serviceArea"),
                doc.getDouble("latitude") != null ? doc.getDouble("latitude") : Double.NaN,
                doc.getDouble("longitude") != null ? doc.getDouble("longitude") : Double.NaN,
                doc.getDouble("coverageRadiusKm") != null ? doc.getDouble("coverageRadiusKm") : 20d,
                doc.getLong("updatedAt") != null ? doc.getLong("updatedAt") : System.currentTimeMillis()
        );
    }

    @NonNull
    @Override
    public String toString() {
        return "WorkerService{" +
                "id='" + id + '\'' +
                ", category='" + category + '\'' +
                ", name='" + name + '\'' +
                ", priceType='" + priceType + '\'' +
                ", priceValue='" + priceValue + '\'' +
                ", ownerId='" + ownerId + '\'' +
                '}';
    }
}
