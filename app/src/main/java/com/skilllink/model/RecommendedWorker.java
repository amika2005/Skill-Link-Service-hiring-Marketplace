package com.skilllink.model;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class RecommendedWorker {

    private final String id;
    private final String name;
    private final String occupation;
    private final double rating;
    private final int reviewCount;
    private final String availability;
    private final String priceDisplay;
    private final String distanceDisplay;
    private final String imageUri;
    private final String serviceId;
    private final String category;
    private final List<String> specialties;
    private final String serviceArea;
    private final double latitude;
    private final double longitude;
    private final double serviceRadiusKm;

    public RecommendedWorker(@NonNull String id,
                             @NonNull String name,
                             @NonNull String occupation,
                             double rating,
                             int reviewCount,
                             @NonNull String availability,
                             @NonNull String priceDisplay,
                             @NonNull String distanceDisplay,
                             @Nullable String imageUri,
                             @Nullable String serviceId,
                             @Nullable String category,
                             @Nullable List<String> specialties,
                             @Nullable String serviceArea,
                             double latitude,
                             double longitude,
                             double serviceRadiusKm) {
        this.id = id;
        this.name = name;
        this.occupation = occupation;
        this.rating = rating;
        this.reviewCount = reviewCount;
        this.availability = availability;
        this.priceDisplay = priceDisplay;
        this.distanceDisplay = distanceDisplay;
        this.imageUri = imageUri;
        this.serviceId = serviceId;
        this.category = category;
        this.serviceArea = serviceArea;
        this.latitude = latitude;
        this.longitude = longitude;
        this.serviceRadiusKm = serviceRadiusKm;
        if (specialties == null || specialties.isEmpty()) {
            this.specialties = Collections.emptyList();
        } else {
            this.specialties = Collections.unmodifiableList(new ArrayList<>(specialties));
        }
    }

    @NonNull
    public String getId() {
        return id;
    }

    @NonNull
    public String getName() {
        return name;
    }

    @NonNull
    public String getOccupation() {
        return occupation;
    }

    public double getRating() {
        return rating;
    }

    public int getReviewCount() {
        return reviewCount;
    }

    @NonNull
    public String getAvailability() {
        return availability;
    }

    @NonNull
    public String getPriceDisplay() {
        return priceDisplay;
    }

    @NonNull
    public String getDistanceDisplay() {
        return distanceDisplay;
    }

    @Nullable
    public String getImageUri() {
        return imageUri;
    }

    @Nullable
    public String getServiceId() {
        return serviceId;
    }

    @Nullable
    public String getCategory() {
        return category;
    }

    @NonNull
    public List<String> getSpecialties() {
        return specialties;
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

    public double getServiceRadiusKm() {
        return serviceRadiusKm;
    }

    @NonNull
    public JSONObject toJson() throws JSONException {
        JSONObject object = new JSONObject();
        object.put("id", id);
        object.put("name", name);
        object.put("occupation", occupation);
        object.put("rating", rating);
        object.put("reviewCount", reviewCount);
        object.put("availability", availability);
        object.put("priceDisplay", priceDisplay);
        object.put("distanceDisplay", distanceDisplay);
        object.put("imageUri", imageUri);
        object.put("serviceId", serviceId);
        object.put("category", category);
        object.put("serviceArea", serviceArea);
        object.put("latitude", latitude);
        object.put("longitude", longitude);
        object.put("serviceRadiusKm", serviceRadiusKm);

        JSONArray specialtiesArray = new JSONArray();
        for (String specialty : specialties) {
            specialtiesArray.put(specialty);
        }
        object.put("specialties", specialtiesArray);
        return object;
    }

    @Nullable
    public static RecommendedWorker fromJson(@Nullable JSONObject object) {
        if (object == null) {
            return null;
        }

        String id = object.optString("id", null);
        String name = object.optString("name", null);
        String occupation = object.optString("occupation", null);
        String availability = object.optString("availability", "");
        String priceDisplay = object.optString("priceDisplay", "");
        String distanceDisplay = object.optString("distanceDisplay", "");

        if (isNullOrEmpty(id) || isNullOrEmpty(name) || isNullOrEmpty(occupation)) {
            return null;
        }

        double rating = object.optDouble("rating", 0d);
        int reviewCount = object.optInt("reviewCount", 0);
        String imageUri = optNullableString(object, "imageUri");
        String serviceId = optNullableString(object, "serviceId");
        String category = optNullableString(object, "category");
        String serviceArea = optNullableString(object, "serviceArea");
        double latitude = object.optDouble("latitude", Double.NaN);
        double longitude = object.optDouble("longitude", Double.NaN);
        double serviceRadiusKm = object.optDouble("serviceRadiusKm", 15d);

        List<String> specialties = new ArrayList<>();
        JSONArray array = object.optJSONArray("specialties");
        if (array != null) {
            for (int i = 0; i < array.length(); i++) {
                String value = array.optString(i, null);
                if (!isNullOrEmpty(value)) {
                    specialties.add(value);
                }
            }
        }

        return new RecommendedWorker(
                id,
                name,
                occupation,
                rating,
                reviewCount,
                availability.isEmpty() ? "" : availability,
                priceDisplay.isEmpty() ? "" : priceDisplay,
                distanceDisplay.isEmpty() ? "" : distanceDisplay,
                imageUri,
                serviceId,
                category,
                specialties,
                serviceArea,
                Double.isNaN(latitude) ? 6.927079 : latitude,
                Double.isNaN(longitude) ? 79.861244 : longitude,
                serviceRadiusKm);
    }

    private static boolean isNullOrEmpty(@Nullable String value) {
        return value == null || value.trim().isEmpty();
    }

    @Nullable
    private static String optNullableString(@NonNull JSONObject object, @NonNull String key) {
        String value = object.optString(key, null);
        return isNullOrEmpty(value) ? null : value;
    }
}
