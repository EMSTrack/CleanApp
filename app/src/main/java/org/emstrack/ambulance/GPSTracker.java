package org.emstrack.ambulance;

import android.Manifest;
import android.app.Activity;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.os.Bundle;
import android.os.IBinder;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.util.Log;
import org.emstrack.ambulance.R;

import org.emstrack.ambulance.fragments.GPSFragment;
import org.emstrack.ambulance.fragments.GPSLocation;


/**
 * Created by Yitzchak on 2/19/2018.
 */

public class GPSTracker extends Service implements LocationListener {

    public static final int REQUEST_FINE_LOCATION = 998;
    private GPSLocation gpsLocation;

    private long MIN_UPDATE_DISTANCE = 0;
    private long MIN_UPDATE_TIME = 0;

    /**
     * Constructor for GPSTracker
     * @param context
     * @param minTime the minimum time that must pass between getting location update from GPS
     * @param minDistance the minimum distance the ambulance must travel to get location update from GPS
     */
    public GPSTracker(GPSFragment gpsFragment, Context context, long minTime, long minDistance) {
        Log.e("GPSTracker", "Creating new GPSTracker");
        gpsLocation = new GPSLocation();
        gpsLocation.addObserver(gpsFragment);

        // If have permission to access location, request location updates, otherwise request permission
        if (ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION) ==
                PackageManager.PERMISSION_GRANTED) {

            String provider = LocationManager.GPS_PROVIDER;
            LocationManager m_locationManager = (LocationManager) context.getSystemService(Context.LOCATION_SERVICE);

            Log.e("GPSTracker", "Requesting location update");
            m_locationManager.requestLocationUpdates(LocationManager.GPS_PROVIDER, minTime, minDistance, this);
            m_locationManager.requestLocationUpdates(LocationManager.NETWORK_PROVIDER, minTime, minDistance, this);
        } else {
            ActivityCompat.requestPermissions(
                    (Activity) context,
                    new String[]{Manifest.permission.ACCESS_FINE_LOCATION},
                    REQUEST_FINE_LOCATION);
            // TODO: Add callback in Activity connected to this fragment
        }
    }

    @Override
    public void onLocationChanged(Location location) {
        gpsLocation.setLocation(location);
        gpsLocation.notifyObservers();
        Log.e("ONLOCATIONCHANGED", "Updated GPS Location Received");
        Log.e("New Location", location.toString());
        //TODO: Push to mqtt
//        final MqttProfileClient profileClient = ((AmbulanceApp) getApplication()).getProfileClient();

    }

    // Required by the LocationListener interface
    @Override
    public void onProviderDisabled(String provider) {}

    // Required by the LocationListener interface
    @Override
    public void onProviderEnabled(String provider) {}

    // Required by the LocationListener interface
    @Override
    public void onStatusChanged(String provider, int status, Bundle extras) {}

    // Required by the LocationListener interface
    @Override
    public IBinder onBind(Intent arg0) {
        return null;
    }

    public GPSLocation getGPSLocation() {
        return gpsLocation;
    }
}
