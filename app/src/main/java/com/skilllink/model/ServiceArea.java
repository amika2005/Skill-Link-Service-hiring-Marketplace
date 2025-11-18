package com.skilllink.model;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import org.json.JSONException;
import org.json.JSONObject;

public class ServiceArea {

    private final String name;
    private final String district;
    private final boolean popular;
    private final double latitude;
    private final double longitude;

    public ServiceArea(@NonNull String name,
                       @Nullable String district,
                       boolean popular,
                       double latitude,
                       double longitude) {
        this.name = name;
        this.district = district;
        this.popular = popular;
        this.latitude = latitude;
        this.longitude = longitude;
    }

    @NonNull
    public String getName() {
        return name;
    }

    @Nullable
    public String getDistrict() {
        return district;
    }

    public boolean isPopular() {
        return popular;
    }

    public double getLatitude() {
        return latitude;
    }

    public double getLongitude() {
        return longitude;
    }

    @NonNull
    public JSONObject toJson() throws JSONException {
        JSONObject object = new JSONObject();
        object.put("name", name);
        object.put("district", district);
        object.put("popular", popular);
        object.put("latitude", latitude);
        object.put("longitude", longitude);
        return object;
    }

    @Nullable
    public static ServiceArea fromJson(@Nullable JSONObject object) {
        if (object == null) {
            return null;
        }
        String name = object.optString("name", null);
        if (name == null || name.trim().isEmpty()) {
            return null;
        }
        String district = object.optString("district", null);
        boolean popular = object.optBoolean("popular", false);
        double latitude = object.optDouble("latitude", Double.NaN);
        double longitude = object.optDouble("longitude", Double.NaN);
        return new ServiceArea(name, district, popular, latitude, longitude);
    }
}
