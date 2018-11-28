package org.emstrack.models;

import java.util.Comparator;

public class Waypoint {

    static class SortByOrder implements Comparator<Waypoint>
    {
        public int compare(Waypoint a, Waypoint b) {
            return a.order - b.order;
        }
    }

    private int order;
    private String status;
    private Location location;

    public Waypoint(int order, String status, Location location) {
        this.order = order;
        this.status = status;
        this.location = location;
    }

    public int getOrder() {
        return order;
    }

    public void setOrder(int order) {
        this.order = order;
    }

    public boolean isActive() {
        return !status.equals("I");
    }

    public boolean isVisited() {
        return status.equals("D");
    }

    public boolean isVisiting() {
        return status.equals("V");
    }

    public void setActive() {
        this.status = "A";
    }

    public void setVisited() {
        this.status = "D";
    }

    public void setVisiting() {
        this.status = "V";
    }

    public void setInactive() {
        this.status = "I";
    }

    public Location getLocation() {
        return location;
    }

    public void setLocation(Location location) {
        this.location = location;
    }

}
