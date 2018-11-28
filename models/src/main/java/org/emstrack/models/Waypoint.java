package org.emstrack.models;

import java.util.Comparator;

public class Waypoint {

    static class SortByOrder implements Comparator<Waypoint>
    {
        public int compare(Waypoint a, Waypoint b) {
            return a.order - b.order;
        }
    }

    public static final String STATUS_NOT_VISITED = "N";
    public static final String STATUS_VISITING = "V";
    public static final String STATUS_VISITED = "D";

    private int order;
    private String status;
    private Location location;
    private boolean active;

    public Waypoint(int order, String status, Location location, boolean active) {
        this.order = order;
        this.status = status;
        this.location = location;
        this.active = active;
    }

    public Waypoint(int order, String status, Location location) {
        this(order, status, location, true);
    }

    public int getOrder() {
        return order;
    }

    public void setOrder(int order) {
        this.order = order;
    }

    public boolean isActive() {
        return active;
    }

    public void setActive(boolean active) {
        this.active = active;
    }

    public boolean isNotVisited() {
        return status.equals(STATUS_NOT_VISITED);
    }

    public boolean isVisited() {
        return status.equals(STATUS_VISITED);
    }

    public boolean isVisiting() {
        return status.equals(STATUS_VISITING);
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

}
