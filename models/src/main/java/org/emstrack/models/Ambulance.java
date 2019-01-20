package org.emstrack.models;

import com.google.gson.annotations.Expose;

import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

/**
 * A class representing an ambulance.
 */
public class Ambulance {

    public static final String STATUS_UNKNOWN = "UK";
    public static final String STATUS_AVAILABLE= "AV";
    public static final String STATUS_OUT_OF_SERVICE= "OS";
    public static final String STATUS_PATIENT_BOUND = "PB";
    public static final String STATUS_AT_PATIENT = "AP";
    public static final String STATUS_HOSPITAL_BOUND = "HB";
    public static final String STATUS_AT_HOSPITAL = "AH";
    public static final String STATUS_BASE_BOUND = "BB";
    public static final String STATUS_AT_BASE = "AB";
    public static final String STATUS_WAYPOINT_BOUND = "WB";
    public static final String STATUS_AT_WAYPOINT = "AW";

    public static final Map<String, Integer> statusBackgroundColorMap;
    public static final Map<String, Integer> statusTextColorMap;
    static {

        Map<String, Integer> backgroundColorMap = new HashMap<>();
        Map<String, Integer> textColorMap = new HashMap<>();

        backgroundColorMap.put(STATUS_UNKNOWN, R.color.bootstrapDark);
        textColorMap.put(STATUS_UNKNOWN, R.color.bootstrapLight);

        backgroundColorMap.put(STATUS_AVAILABLE, R.color.bootstrapSuccess);
        textColorMap.put(STATUS_AVAILABLE, R.color.bootstrapLight);

        backgroundColorMap.put(STATUS_OUT_OF_SERVICE, R.color.bootstrapDark);
        textColorMap.put(STATUS_OUT_OF_SERVICE, R.color.bootstrapLight);

        backgroundColorMap.put(STATUS_AT_BASE, R.color.bootstrapWarning);
        textColorMap.put(STATUS_AT_BASE, R.color.bootstrapDark);

        backgroundColorMap.put(STATUS_AT_PATIENT, R.color.bootstrapWarning);
        textColorMap.put(STATUS_AT_PATIENT, R.color.bootstrapDark);

        backgroundColorMap.put(STATUS_AT_HOSPITAL, R.color.bootstrapWarning);
        textColorMap.put(STATUS_AT_HOSPITAL, R.color.bootstrapDark);

        backgroundColorMap.put(STATUS_AT_WAYPOINT, R.color.bootstrapWarning);
        textColorMap.put(STATUS_AT_WAYPOINT, R.color.bootstrapDark);

        backgroundColorMap.put(STATUS_PATIENT_BOUND, R.color.bootstrapInfo);
        textColorMap.put(STATUS_PATIENT_BOUND, R.color.bootstrapLight);

        backgroundColorMap.put(STATUS_HOSPITAL_BOUND, R.color.bootstrapInfo);
        textColorMap.put(STATUS_HOSPITAL_BOUND, R.color.bootstrapLight);

        backgroundColorMap.put(STATUS_BASE_BOUND, R.color.bootstrapInfo);
        textColorMap.put(STATUS_BASE_BOUND, R.color.bootstrapLight);

        backgroundColorMap.put(STATUS_WAYPOINT_BOUND, R.color.bootstrapInfo);
        textColorMap.put(STATUS_WAYPOINT_BOUND, R.color.bootstrapLight);

        statusBackgroundColorMap = Collections.unmodifiableMap(backgroundColorMap);
        statusTextColorMap = Collections.unmodifiableMap(textColorMap);

    }

    private int id;
    private String identifier;
    @Expose
    private String capability;
    @Expose
    private String status;
    @Expose
    private double orientation;
    @Expose
    private GPSLocation location;
    @Expose
    private Date timestamp;
    private String locationClientId;
    private String comment;
    private int updatedBy;
    private Date updatedOn;

    public Ambulance(int id, String identifier, String capability, String status,
                     double orientation, GPSLocation location, Date timestamp,
                     String comment, int updatedBy, Date updatedOn) {
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
        location = new GPSLocation(lastLocation.getLatitude(),lastLocation.getLongitude());
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

    public GPSLocation getLocation() {
        return location;
    }

    public void setLocation(GPSLocation location) {
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