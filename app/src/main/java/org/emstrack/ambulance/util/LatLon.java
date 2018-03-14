package org.emstrack.ambulance.util;

import android.location.Location;

/**
 * Created by mauricio on 3/14/2018.
 */

public class LatLon {

    public final static double earthRadius = 6371e3; // in meters
    public static double stationaryRadius = 10.; // in meters

    public static double CalculateDistanceHaversine(Location location1, Location location2) {

        // convert latitude and longitude to radians first
        double lat1 = Math.PI * location1.getLatitude() / 180;
        double lat2 = Math.PI * location2.getLatitude() / 180;
        double d_phi = lat2 - lat1;
        double d_lambda = Math.PI * (location2.getLongitude() - location1.getLongitude()) / 180;

        double a = Math.sin(d_phi / 2) * Math.sin(d_phi / 2) + Math.cos(lat1) * Math.cos(lat2) * Math.sin(d_lambda / 2) * Math.sin(d_lambda / 2);
        double c = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a));

        return LatLon.earthRadius * c;

    }

}
