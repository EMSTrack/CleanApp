package org.emstrack.ambulance.interfaces;

import android.Manifest;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.location.LocationListener;
import android.location.LocationManager;
import android.os.Bundle;
import android.os.IBinder;
import android.location.Location;

import org.emstrack.ambulance.AmbulanceApp;
import org.emstrack.mqtt.MqttProfileClient;
import android.support.v4.content.ContextCompat;
import android.util.Log;
import android.widget.TextView;

import org.eclipse.paho.client.mqttv3.MqttClient;


/**
 * Created by Yitzchak on 2/19/2018.
 */

public class GPSTracker extends Service implements LocationListener {

    private static LocationManager m_locationManager;
    private Context mContext;
    private static String provider;
    private static final int REQUEST_FINE_LOCATION = 998;
    TextView LatLongTextView;

    Location location;
    public double latitude;
    public double longitude;

    // These define the mimimun time  and distance in which the gps should get
    // the updated location of the ambulance.
    private long MIN_UPDATE_DISTANCE = 0;
    private long MIN_UPDATE_TIME = 0;


    /**
     * Constructor for GPSTracker
     * @param context
     * @param minTime the minimum time that must pass between getting location update from GPS
     * @param minDistance the minimum distance the ambulance must travel to get location update from GPS
     */
    public GPSTracker(Context context, long minTime, long minDistance) {
        this.mContext = context;

        // This is required to get access to the provider
        if (ContextCompat.checkSelfPermission(mContext, Manifest.permission.ACCESS_FINE_LOCATION) ==
                PackageManager.PERMISSION_GRANTED) {

            provider = LocationManager.GPS_PROVIDER;
            m_locationManager = (LocationManager) mContext.getSystemService(Context.LOCATION_SERVICE);

            m_locationManager.requestLocationUpdates(LocationManager.GPS_PROVIDER, minTime, minDistance, this);
            m_locationManager.requestLocationUpdates(LocationManager.NETWORK_PROVIDER, minTime, minDistance, this);


        }



    }

    @Override
    public void onLocationChanged(Location location) {
        Log.e("ONLOCATIONCHANGED", "Updated GPS Location Received");
        Log.e("New Location", location.toString());

        if (location == null) {
            return;
        }
        Log.e("New Location: :","New Location: "+ location.toString());
        final MqttProfileClient profileClient = ((AmbulanceApp) getApplication()).getProfileClient();


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

}
