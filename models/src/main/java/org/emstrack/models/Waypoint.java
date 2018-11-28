package org.emstrack.models;

public class Waypoint {

    private int order;
    private boolean visited;
    private Location location;

    public Waypoint(int order, boolean visited, Location location) {
        this.order = order;
        this.visited = visited;
        this.location = location;
    }

    public int getOrder() {
        return order;
    }

    public void setOrder(int order) {
        this.order = order;
    }

    public boolean isVisited() {
        return visited;
    }

    public void setVisited(boolean visited) {
        this.visited = visited;
    }

    public Location getLocation() {
        return location;
    }

    public void setLocation(Location location) {
        this.location = location;
    }

}
