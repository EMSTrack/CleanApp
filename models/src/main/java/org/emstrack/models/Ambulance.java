package org.emstrack.models;

import com.google.gson.annotations.Expose;

import java.util.Date;

public class Ambulance {

    private int id;
    private String identifier;
    private String capability;
    @Expose
    private String status;
    @Expose
    private double orientation;
    @Expose
    private Location location;
    @Expose
    private Date timestamp;
    private String locationClientId;
    private String comment;
    private int updatedBy;
    private Date updatedOn;

    public Ambulance(int id, String identifier, String capability, String status,
                     double orientation, Location location, Date timestamp, String comment,
                     int updatedBy, Date updatedOn) {
        this.id = id;
        this.identifier = identifier;
        this.capability = capability;
        this.status = status;
        this.orientation = orientation;
        this.location = location;
        this.timestamp = timestamp;
        this.comment = comment;
        this.locationClientId = null;
        this.updatedBy = updatedBy;
        this.updatedOn = updatedOn;
    }

    public Ambulance(int id, String identifier, String capability) {
        this.id = id;
        this.identifier = identifier;
        this.capability = capability;
        this.status = "UK";
        this.orientation = 0.0;
        this.location = null;
        this.timestamp = null;
        this.locationClientId = null;
        this.comment = "";
        this.updatedBy = -1;
        this.updatedOn = null;
    }

    public void updateLocation(android.location.Location lastLocation) {

        // Update ambulance
        location = new Location(lastLocation.getLatitude(),lastLocation.getLongitude());
        orientation = lastLocation.getBearing();
        timestamp = new Date(lastLocation.getTime());

    }

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public String getIdentifier() {
        return identifier;
    }

    public void setIdentifier(String identifier) {
        this.identifier = identifier;
    }

    public String getCapability() {
        return capability;
    }

    public void setCapability(String capability) {
        this.capability = capability;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public double getOrientation() {
        return orientation;
    }

    public void setOrientation(double orientation) {
        this.orientation = orientation;
    }

    public Location getLocation() {
        return location;
    }

    public void setLocation(Location location) {
        this.location = location;
    }

    public Date getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(Date timestamp) {
        this.timestamp = timestamp;
    }

    public String getLocationClientId() { return locationClientId; }

    public void setLocationClientId(String locationClientId) { this.locationClientId = locationClientId; }

    public String getComment() {
        return comment;
    }

    public void setComment(String comment) {
        this.comment = comment;
    }

    public int getUpdatedBy() {
        return updatedBy;
    }

    public void setUpdatedBy(int updatedBy) {
        this.updatedBy = updatedBy;
    }

    public Date getUpdatedOn() {
        return updatedOn;
    }

    public void setUpdatedOn(Date updatedOn) {
        this.updatedOn = updatedOn;
    }

}