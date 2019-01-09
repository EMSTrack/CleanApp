package org.emstrack.ambulance.util;

import android.location.Location;
import android.text.TextUtils;

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.Date;
import java.util.List;
import java.util.TimeZone;

/**
 * Created by mauricio on 3/22/2018.
 */

public class AmbulanceUpdate {

    static class SortByAscendingOrder implements Comparator<AmbulanceUpdate>
    {
        public int compare(AmbulanceUpdate a, AmbulanceUpdate b) {
            return a.timestamp.compareTo(b.timestamp);
        }
    }


    private Location location;
    private float bearing;
    private float velocity;
    private Date timestamp;
    private String status;

    public AmbulanceUpdate() {
        this.location = null;
        this.bearing = (float) 0.0;
        this.velocity = (float) 0.0;
        this.timestamp = new Date();
        this.status = null;
    }

    public AmbulanceUpdate(Location location) {
        this.location = new Location(location);
        this.bearing = location.getBearing();
        this.velocity = location.getSpeed();
        this.timestamp = new Date(location.getTime());
        this.status = null;
    }

    public AmbulanceUpdate(AmbulanceUpdate update) {
        this.location = new Location(update.location);
        this.bearing = update.bearing;
        this.velocity = update.velocity;
        this.timestamp = new Date(update.timestamp.getTime());
        this.status = null;
    }

    public AmbulanceUpdate(String status) {
        this(status, new Date());
    }

    public AmbulanceUpdate(String status, Date timestamp) {
        this.location = null;
        this.bearing = 0;
        this.velocity = 0;
        this.timestamp = timestamp == null ? new Date() : timestamp;
        this.status = status;
    }

    public void setLocation(Location location) {
        this.location = location;
    }

    public Location getLocation() {
        return this.location;
    }

    public boolean hasLocation() {
        return this.location != null;
    }

    public void setBearing(float bearing) {
        this.bearing = bearing;
    }

    public void setBearing(double bearing) {
        this.bearing = (float) bearing;
    }

    public float getBearing() {
        return bearing;
    }

    public void setVelocity(float velocity) {
        this.velocity = velocity;
    }

    public void setVelocity(double velocity) {
        this.velocity = (float) velocity;
    }

    public float getVelocity() {
        return velocity;
    }

    public void setTimestamp(Date timestamp) {
        this.timestamp = timestamp;
    }

    public Date getTimestamp() {
        return timestamp;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public String getStatus() {
        return status;
    }

    public String toUpdateString() {

        List<String> updates=  new ArrayList<>();

        // add location update
        if (this.location != null) {

            double latitude = this.location.getLatitude();
            double longitude = this.location.getLongitude();
            double orientation = this.bearing;

            updates.add("\"orientation\":" + orientation);
            updates.add("\"location\":{" + "\"latitude\":"+ latitude + ",\"longitude\":" + longitude + "}");

        }

        // add status update
        if (this.status != null) {
            updates.add("\"status\":\"" + status + "\"");
        }

        // format timestamp
        DateFormat df = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'");
        df.setTimeZone(TimeZone.getTimeZone("UTC"));
        String timestamp = df.format(this.getTimestamp());

        // add timestamp
        updates.add("\"timestamp\":\"" + timestamp + "\"");

        return "{" + TextUtils.join(",", updates)+ "}";
    }


};


