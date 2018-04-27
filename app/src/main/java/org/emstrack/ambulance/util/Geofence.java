package org.emstrack.ambulance.util;

import org.emstrack.models.Location;

import static com.google.android.gms.location.Geofence.GEOFENCE_TRANSITION_ENTER;
import static com.google.android.gms.location.Geofence.GEOFENCE_TRANSITION_EXIT;
import static com.google.android.gms.location.Geofence.NEVER_EXPIRE;

/**
 * Created by mauricio on 4/26/18.
 */

public class Geofence {

    Location location;
    float radius;

    public Geofence(Location location, float radius) {
        this.location = location;
        this.radius = radius;
    }

    public Location getLocation() { return location; }

    public float getRadius() { return radius; }

    public com.google.android.gms.location.Geofence build(String id) {
        return build(id, GEOFENCE_TRANSITION_ENTER | GEOFENCE_TRANSITION_EXIT);
    }

    private com.google.android.gms.location.Geofence build(String id, int transitionTypes) {

        // Create geofence object
        com.google.android.gms.location.Geofence.Builder builder = new com.google.android.gms.location.Geofence.Builder();
        builder.setRequestId(id);
        builder.setCircularRegion((float) location.getLatitude(), (float) location.getLongitude(), radius);
        builder.setExpirationDuration(NEVER_EXPIRE);
        builder.setTransitionTypes(transitionTypes);
        return builder.build();

    }

}
