package org.emstrack.ambulance.models;

import org.emstrack.ambulance.util.LatLon;
import org.emstrack.models.GPSLocation;
import org.emstrack.models.NamedAddress;

import java.util.Comparator;

public class NamedAddressWithDistance {

    static public class SortAscending implements Comparator<NamedAddressWithDistance> {
        public int compare(NamedAddressWithDistance a, NamedAddressWithDistance b) {
            return Double.compare(a.getDistance(), b.getDistance());
        }
    }

    private final NamedAddress namedAddress;
    private final double distance;

    public NamedAddressWithDistance(NamedAddress namedAddress, GPSLocation target) {
        this.namedAddress = namedAddress;
        this.distance = LatLon.calculateDistanceHaversine(namedAddress.getLocation().toLocation(), target.toLocation()) / 1000;
    }

    public NamedAddress getNamedAddress() {
        return namedAddress;
    }

    public double getDistance() {
        return distance;
    }

}
