package org.emstrack.models;

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

    public GPSLocation(LatLng latLng) {
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

    public android.location.Location toLocation() {
        android.location.Location location = new android.location.Location("GPS");
        location.setLatitude(this.getLatitude());
        location.setLongitude(this.getLongitude());
        return location;
    }

    @Override
    public String toString() {
        return "{latitude:" + latitude + ",longitude:" + longitude + "}";
    }
}
