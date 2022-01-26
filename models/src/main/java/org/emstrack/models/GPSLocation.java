package org.emstrack.models;

import android.location.Location;

import androidx.annotation.NonNull;

import com.google.android.gms.maps.model.LatLng;
import com.google.gson.annotations.Expose;

/**
 * A class representing a GPS location.
 */
public class GPSLocation {

    @Expose
    private double latitude;
    @Expose
    private double longitude;

    public GPSLocation(double latitude, double longitude) {
        this.latitude = latitude;
        this.longitude = longitude;
    }

    public GPSLocation(@NonNull LatLng latLng) {
        this.latitude = latLng.latitude;
        this.longitude = latLng.longitude;
    }

    public double getLatitude() {
        return latitude;
    }

    public void setLatitude(double latitude) {
        this.latitude = latitude;
    }

    public double getLongitude() {
        return longitude;
    }

    public void setLongitude(double longitude) {
        this.longitude = longitude;
    }

    @NonNull
    public android.location.Location toLocation() {
        android.location.Location location = new android.location.Location("GPS");
        location.setLatitude(this.getLatitude());
        location.setLongitude(this.getLongitude());
        return location;
    }

    @NonNull
    public LatLng toLatLng() {
        return new LatLng(getLatitude(), getLongitude());
    }

    @NonNull
    public GPSLocation add(@NonNull GPSLocation location) {
        latitude += location.latitude;
        longitude += location.longitude;
        return this;
    }

    @NonNull
    public GPSLocation add(@NonNull LatLng location) {
        latitude += location.latitude;
        longitude += location.longitude;
        return this;
    }

    @NonNull
    public GPSLocation add(@NonNull Location location) {
        latitude += location.getLatitude();
        longitude += location.getLongitude();
        return this;
    }

    @NonNull
    public GPSLocation add(double lat, double lng) {
        latitude += lat;
        longitude += lng;
        return this;
    }

    @NonNull
    @Override
    public String toString() {
        return "{latitude:" + latitude + ",longitude:" + longitude + "}";
    }
}
