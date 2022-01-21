package org.emstrack.models;

import com.google.gson.ExclusionStrategy;
import com.google.gson.FieldAttributes;

import org.emstrack.models.gson.Exclude;

import java.util.Calendar;
import java.util.Comparator;

/**
 * A class representing a waypoint.
 */
public class Waypoint {

    static class SortByAscendingOrder implements Comparator<Waypoint>
    {
        public int compare(Waypoint a, Waypoint b) {
            return a.order - b.order;
        }
    }

    public enum WaypointEvent {
        ENTER,
        EXIT
    };

    public static final String DETECTION_MARK = "mark";
    public static final String DETECTION_NOTIFY = "notify";
    public static final String DETECTION_DISABLED = "notify";

    public static final String STATUS_CREATED = "C";
    public static final String STATUS_VISITING = "V";
    public static final String STATUS_VISITED = "D";
    public static final String STATUS_SKIPPED = "S";

    private int id;
    private int ambulanceCallId;
    private int order;
    private String status;
    private Location location;
    private String comment;
    @Exclude
    private int updatedBy;
    @Exclude
    private Calendar updatedOn;

    public static class WaypointCreationExclusionStrategy implements ExclusionStrategy {

        @Override
        public boolean shouldSkipField(FieldAttributes f) {
            return (
                    (f.getDeclaringClass() == Waypoint.class && (f.getName().equals("id") || f.getName().equals("ambulanceCallId")))
                            ||
                            (f.getDeclaringClass() == Location.class && (f.getName().equals("id")))
            );
        }

        public boolean shouldSkipClass(Class<?> arg0) {
            return false;
        }

    }

    public Waypoint(int id, int ambulanceCallId,
                    int order, String status, Location location,
                    String comment, int updatedBy, Calendar updatedOn) {
        this.id = id;
        this.ambulanceCallId = ambulanceCallId;
        this.order = order;
        this.status = status;
        this.location = location;
        this.comment = comment;
        this.updatedBy = updatedBy;
        this.updatedOn = updatedOn;
    }

    public Waypoint(int order, String status, Location location) {
        this(-1, -1, order, status, location,
                "", -1, Calendar.getInstance());
    }

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public int getAmbulanceCallId() {
        return ambulanceCallId;
    }

    public void setAmbulanceCallId(int ambulanceCallId) {
        this.ambulanceCallId = ambulanceCallId;
    }

    public int getOrder() {
        return order;
    }

    public void setOrder(int order) {
        this.order = order;
    }

    public boolean isCreated() {
        return status.equals(STATUS_CREATED);
    }

    public boolean isVisited() {
        return status.equals(STATUS_VISITED);
    }

    public boolean isVisiting() {
        return status.equals(STATUS_VISITING);
    }

    public boolean isSkipped() {
        return status.equals(STATUS_SKIPPED);
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public String getStatus() {
        return status;
    }

    public Location getLocation() {
        return location;
    }

    public void setLocation(Location location) {
        this.location = location;
    }

    public String getComment() {
        return comment;
    }

    public void setComment(String comment) {
        this.comment = comment;
    }

    public int getUpdatedBy() {
        return updatedBy;
    }

    public void setUpdatedBy(Integer updatedBy) {
        this.updatedBy = updatedBy;
    }

    public Calendar getUpdatedOn() {
        return updatedOn;
    }

    public void setUpdatedOn(Calendar updatedOn) {
        this.updatedOn = updatedOn;
    }

    public float calculateDistance(android.location.Location lastLocation) {
        // Calculate distance to patient in kilometers
        if (lastLocation != null) {
            return lastLocation.distanceTo(location.getLocation().toLocation()) / 1000;
        } else {
            return -1;
        }
    }

}
