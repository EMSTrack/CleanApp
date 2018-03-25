package org.emstrack.ambulance.util;

import android.location.Location;

import java.util.Date;

/**
 * Created by mauricio on 3/22/2018.
 */

public class LocationUpdate {

    private Location location;
    private float bearing;
    private float velocity;
    private Date timestamp;

    public LocationUpdate() {
        this.location = null;
        this.bearing = (float) 0.0;
        this.velocity = (float) 0.0;
        this.timestamp = new Date();
    }

    public LocationUpdate(Location location) {
        this.location = new Location(location);
        this.bearing = location.getBearing();
        this.velocity = location.getSpeed();
        this.timestamp = new Date(location.getTime());
    }

    public LocationUpdate(LocationUpdate update) {
        this.location = new Location(update.location);
        this.bearing = update.bearing;
        this.velocity = update.velocity;
        this.timestamp = new Date(update.timestamp.getTime());
    }

    public void setLocation(Location location) {
        this.location = location;
    }

    public void setBearing(float bearing) {
        this.bearing = bearing;
    }

    public void setVelocity(float velocity) {
        this.velocity = velocity;
    }

    public void setBearing(double bearing) {
        this.bearing = (float) bearing;
    }

    public void setVelocity(double velocity) {
        this.velocity = (float) velocity;
    }

    public void setTimestamp(Date timestamp) {
        this.timestamp = timestamp;
    }

    public Location getLocation() {
        return location;
    }

    public float getBearing() {
        return bearing;
    }

    public float getVelocity() {
        return velocity;
    }

    public Date getTimestamp() {
        return timestamp;
    }
};


