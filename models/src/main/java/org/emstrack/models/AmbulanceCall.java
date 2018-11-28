package org.emstrack.models;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

public class AmbulanceCall {

    private int id;
    private int ambulanceId;
    private String status;
    private Date createdAt;
    private List<Waypoint> waypointSet = new ArrayList<>();

    public AmbulanceCall(int id, int ambulanceId, String status, Date createdAt, List<Waypoint> waypointSet) {
        this.id = id;
        this.ambulanceId = ambulanceId;
        this.status = status;
        this.createdAt = createdAt;
        this.waypointSet = waypointSet;
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

    public Date getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(Date createdAt) {
        this.createdAt = createdAt;
    }

    public List<Waypoint> getWaypointSet() {
        return waypointSet;
    }

    public void setWaypointSet(List<Waypoint> waypointSet) {
        this.waypointSet = waypointSet;
    }

}

