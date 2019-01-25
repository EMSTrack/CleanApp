package org.emstrack.models;

import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * A class representing an ambulance call.
 */
public class AmbulanceCall {

    public static final String STATUS_REQUESTED = "R";
    public static final String STATUS_ACCEPTED = "A";
    public static final String STATUS_DECLINED = "D";
    public static final String STATUS_SUSPENDED = "S";
    public static final String STATUS_COMPLETED = "C";

    public static final Map<String, String> statusLabel;
    static {

        Map<String, String> map = new HashMap<>();

        map.put(STATUS_REQUESTED, "Requested");
        map.put(STATUS_ACCEPTED, "Accepted");
        map.put(STATUS_DECLINED, "Declined");
        map.put(STATUS_SUSPENDED, "Suspended");
        map.put(STATUS_COMPLETED, "Completed");

        statusLabel = Collections.unmodifiableMap(map);
    }

    private int id;
    private int ambulanceId;
    private String status;
    private List<Waypoint> waypointSet;
    private boolean sorted;
    private String comment;
    private int updatedBy;
    private Date updatedOn;

    public AmbulanceCall(int id, int ambulanceId, String status,
                         String comment, int updatedBy, Date updatedOn,
                         List<Waypoint> waypointSet) {
        this.id = id;
        this.ambulanceId = ambulanceId;
        this.status = status;
        this.waypointSet = waypointSet;
        this.sorted = false;
        this.comment = comment;
        this.updatedBy = updatedBy;
        this.updatedOn = updatedOn;
    }

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public int getAmbulanceId() {
        return ambulanceId;
    }

    public void setAmbulanceId(int ambulanceId) {
        this.ambulanceId = ambulanceId;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public boolean isSorted() {
        return sorted;
    }

    public void setSorted(boolean sorted) {
        this.sorted = sorted;
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

    public void setUpdatedBy(int updatedBy) {
        this.updatedBy = updatedBy;
    }

    public Date getUpdatedOn() {
        return updatedOn;
    }

    public void setUpdatedOn(Date updatedOn) {
        this.updatedOn = updatedOn;
    }

    public List<Waypoint> getWaypointSet() {
        return waypointSet;
    }

    public void setWaypointSet(List<Waypoint> waypointSet) {
        this.waypointSet = waypointSet;
    }

    public void sortWaypoints() {
        sortWaypoints(false);
    }

    public void sortWaypoints(boolean force) {
        if (force || !this.sorted) {
            Collections.sort(waypointSet, new Waypoint.SortByAscendingOrder());
            this.sorted = true;
        }
    }

    public Waypoint getNextIncidentWaypoint() {
        return getNextWaypoint("i");
    }

    public Waypoint getNextWaypoint() {
        return getNextWaypoint(null);
    }

    public Waypoint getNextWaypoint(String type) {
        // Sort first
        sortWaypoints();

        // Find first non-visited waypoint
        for (Waypoint waypoint : this.waypointSet) {
            if ((type == null || waypoint.getLocation().getType().equals(type))
                    && !(waypoint.isSkipped() || waypoint.isVisited()))
                return waypoint;
        }

        // Otherwise return null
        return null;
    }

    public Waypoint getWaypoint(int id) {
        // Find waypoint
        for (Waypoint waypoint : this.waypointSet)
            if (waypoint.getId() == id)
                return waypoint;
        return null;
    }

    public boolean containsWaypoint(Waypoint waypoint) {
        return this.waypointSet.contains(waypoint);
    }

    public int getMaximumWaypointOrder() {
        // Sort first?
        sortWaypoints();

        // Find maximum order
        int maximumOrder = -1;
        for (Waypoint waypoint : this.waypointSet)
            maximumOrder = Math.max(maximumOrder, waypoint.getOrder());

        // Return maximum
        return maximumOrder;
    }

    public int getNextNewWaypointOrder() {
        return getMaximumWaypointOrder() + 1;
    }

}

