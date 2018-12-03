package org.emstrack.ambulance.util;

import org.emstrack.models.GPSLocation;
import org.emstrack.models.Location;
import org.emstrack.models.Waypoint;

import java.util.HashMap;
import java.util.Map;

import static com.google.android.gms.location.Geofence.GEOFENCE_TRANSITION_ENTER;
import static com.google.android.gms.location.Geofence.GEOFENCE_TRANSITION_EXIT;
import static com.google.android.gms.location.Geofence.NEVER_EXPIRE;

/**
 * Created by mauricio on 4/26/18.
 */

public class Geofence {

    float radius;
    Waypoint waypoint;
    Map<String, Integer> ids;

    public Geofence(Waypoint waypoint, float radius) {
        this.radius = radius;
        this.waypoint = waypoint;
        this.ids = new HashMap<>();
    }

    public Geofence(GPSLocation location, float radius, String type) {
        this(new Waypoint(-1, Waypoint.STATUS_CREATED,
                new Location(null, type, location)), radius);
    }

    public Waypoint getWaypoint() {
        return waypoint;
    }

    public GPSLocation getLocation() { return waypoint.getLocation().getLocation(); }

    public float getRadius() { return radius; }

    public boolean isHospital() { return waypoint.getLocation().getType().equals("h"); }

    public void removeId(String id) {
        ids.remove(id);
    }

    public com.google.android.gms.location.Geofence build(String id) {
        return build(id, GEOFENCE_TRANSITION_ENTER | GEOFENCE_TRANSITION_EXIT);
    }

    private com.google.android.gms.location.Geofence build(String id, int transitionTypes) {

        // Add id to map
        ids.put(id, transitionTypes);

        // Create geofence object
        GPSLocation location = waypoint.getLocation().getLocation();
        com.google.android.gms.location.Geofence.Builder builder = new com.google.android.gms.location.Geofence.Builder();
        builder.setRequestId(id);
        builder.setCircularRegion((float) location.getLatitude(), (float) location.getLongitude(), radius);
        builder.setExpirationDuration(NEVER_EXPIRE);
        builder.setTransitionTypes(transitionTypes);
        return builder.build();

    }

}
