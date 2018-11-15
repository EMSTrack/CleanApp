package org.emstrack.models;

import com.google.gson.annotations.Expose;

public class Location {

    @Expose
    private double latitude;
    @Expose
    private double longitude;

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

    public Location(double latitude, double longitude) {
        this.latitude = latitude;
        this.longitude = longitude;
    }
}
