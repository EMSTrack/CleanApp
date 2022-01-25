package org.emstrack.ambulance.models;

import androidx.annotation.NonNull;

import org.emstrack.ambulance.util.LatLon;
import org.emstrack.models.GPSLocation;
import org.emstrack.models.NamedAddress;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

public class NamedAddressWithDistance {

    static public class SortAscending implements Comparator<NamedAddressWithDistance> {
        public int compare(@NonNull NamedAddressWithDistance a, @NonNull NamedAddressWithDistance b) {
            return Double.compare(a.getDistance(), b.getDistance());
        }
    }

    private final NamedAddress namedAddress;
    private final double distance;

    public NamedAddressWithDistance(@NonNull NamedAddress namedAddress) {
        this.namedAddress = namedAddress;
        this.distance = 0f;
    }

    public NamedAddressWithDistance(@NonNull NamedAddress namedAddress, @NonNull GPSLocation target) {
        this.namedAddress = namedAddress;
        this.distance = LatLon.calculateDistanceHaversine(namedAddress.getLocation().toLocation(), target.toLocation()) / 1000;
    }

    @NonNull
    public NamedAddress getNamedAddress() {
        return namedAddress;
    }

    public double getDistance() {
        return distance;
    }

    @NonNull
    public static List<NamedAddressWithDistance> fromNamedAddresses(@NonNull List<? extends NamedAddress> namedAddresses, @NonNull GPSLocation target) {
        // create sorted list of locations
        ArrayList<NamedAddressWithDistance> list = new ArrayList<>();
        for (NamedAddress location: namedAddresses) {
            list.add(new NamedAddressWithDistance(location, target));
        }
        Collections.sort(list, new NamedAddressWithDistance.SortAscending());
        return list;
    }


}
