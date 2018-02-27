package org.emstrack.ambulance.fragments;

import android.location.Location;

import java.util.Observable;

/**
 * Created by tina on 2/27/18.
 */

public class GPSLocation extends Observable {
    private Location location;
    public GPSLocation() {}
    public GPSLocation(Location location) {
        this.location = location;
    }

    public Location getLocation() {
        return location;
    }

    public void setLocation(Location location) {
        this.location =location;
        setChanged();
    }

    public String getLatitude() {
        return String.valueOf(location.getLatitude());
    }

    public String getLongitude() {
        return String.valueOf(location.getLongitude());
    }

    public String getTime() {
        return String.valueOf(location.getTime());
    }
}
