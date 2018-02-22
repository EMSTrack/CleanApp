package org.emstrack.ambulance;

import android.Manifest;
import android.app.Activity;
import android.app.Service;
import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.nfc.Tag;
import android.os.Build;
import android.os.Bundle;
import android.os.IBinder;
import android.provider.Settings;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.util.Log;
import android.widget.TextView;

import com.google.android.gms.location.LocationRequest;

import org.emstrack.mqtt.MqttProfileClient;

import java.util.TimerTask;

/**
 *
 * Created by Hans Yuan on 10/19/2016.
 *
 * Java Class Only:
 * Purpose is to get the GPSActivity information from Android.
 * Use by creating a new instance of GPSTracker. This is done by the GPSActivity class.
 */
public class GPSTracker extends Service implements LocationListener {
    private static LocationManager m_locationManager;
    private Context mContext;
    private static String provider;
    private static final int REQUEST_FINE_LOCATION = 998;
    TextView LatLongTextView;

    //Used in LocationListener to check whether to add a new locationPoint
    public LocationPoint lastKnownLocation;

    Location location; // location
    public double latitude; // latitude
    public double longitude; // longitude

    // The minimum distance to change Updates in meters
    private long MIN_DISTANCE_CHANGE_FOR_UPDATES = -1; // 1 meters
    // The minimum time between updates in milliseconds
    private long MIN_TIME_BW_UPDATES = 500 * 1 * 1; // 10 sec minute


    /** Constructor. Sets the location manager, sets listeners for location (both location and time
     *
     * @param context
     */
    public GPSTracker(Context context, long minTime, long minDist) {
        if (minTime != -1) {
            MIN_TIME_BW_UPDATES = minTime;
        }
        if (minDist != -1) {
            MIN_DISTANCE_CHANGE_FOR_UPDATES = minDist;
        }
        this.mContext = context;
        if (ContextCompat.checkSelfPermission(mContext, Manifest.permission.ACCESS_FINE_LOCATION) ==
                PackageManager.PERMISSION_GRANTED) {

            provider = LocationManager.GPS_PROVIDER;
            m_locationManager = (LocationManager) mContext.getSystemService(Context.LOCATION_SERVICE);


            LocationRequest locationrequest = LocationRequest.create();
            locationrequest.setInterval(5000);   // 5 seconds


            m_locationManager.requestLocationUpdates(LocationManager.GPS_PROVIDER, minTime, minDist, this);
            m_locationManager.requestLocationUpdates(LocationManager.NETWORK_PROVIDER, minTime, minDist, this);


        }

        getLocation();
        Log.e("Location", "Getlocation called");
    }

    public void setLatLongTextView(TextView t) {
        LatLongTextView = t;
    }

    public void turnOff() {
        System.out.println("\n Turning off listener");
        if (m_locationManager != null)
            m_locationManager.removeUpdates(this);
    }


    /** checks if the GPSActivity is enabled. If it is not, returns false
     *
     * @return
     */
    public boolean isGPSEnabled() {
        //TODO toast if it is not enabled
        return m_locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER);
    }

    public boolean isNetworkEnabled() {
        //TODO toast if it is not enabled
        return m_locationManager.isProviderEnabled(LocationManager.NETWORK_PROVIDER);
    }

    /** Name: getLastKnowLocation
     *  Returns LocationPoint lastKnownLocation
     */
    public LocationPoint getLastKnownLocation() {
        return lastKnownLocation;
    }

    public long getMinDistanceChangeForUpdates() {
        return MIN_DISTANCE_CHANGE_FOR_UPDATES;
    }

    public long getMinTimeBWUpdates() {
        return MIN_TIME_BW_UPDATES;
    }

    public Location getLocation() {
        try {

            /** Test */
            if (getLastKnownLocationIfAllowed() == null) {

                ActivityCompat.requestPermissions(
                        (Activity) mContext,
                        new String[]{Manifest.permission.ACCESS_FINE_LOCATION},
                        REQUEST_FINE_LOCATION);
            }


            if (isGPSEnabled() && isNetworkEnabled()) {

                // First get location from Network Provider
                try {
                    if (isNetworkEnabled()) {
                        m_locationManager.requestLocationUpdates(
                                LocationManager.NETWORK_PROVIDER,
                                MIN_TIME_BW_UPDATES,
                                MIN_DISTANCE_CHANGE_FOR_UPDATES, this);
                        Log.d("Network", "Network");
                        if (m_locationManager != null) {
                            location = m_locationManager
                                    .getLastKnownLocation(LocationManager.NETWORK_PROVIDER);
                            if (location != null) {
                                latitude = location.getLatitude();
                                longitude = location.getLongitude();
                            }
                        }
                    }
                    /** Adding code to check for location permission*/
                    int currentapiVersion = android.os.Build.VERSION.SDK_INT;
                    if (currentapiVersion >= Build.VERSION_CODES.M) {
                        //Request Permission at Runtime!
                    } else {
                        // do something for phones running an SDK before lollipop
                    }

                    // TODO This is the original isGPSEnabled if-statement.
                    if (isGPSEnabled()) {
                        if (location == null) {
                            m_locationManager.requestLocationUpdates(
                                    LocationManager.GPS_PROVIDER,
                                    MIN_TIME_BW_UPDATES,
                                    MIN_DISTANCE_CHANGE_FOR_UPDATES, this);
                            Log.d("GPSActivity Enabled", "GPSActivity Enabled");
                            if (m_locationManager != null) {
                                location = m_locationManager
                                        .getLastKnownLocation(LocationManager.GPS_PROVIDER);
                                if (location != null) {
                                    latitude = location.getLatitude();
                                    longitude = location.getLongitude();
                                }
                            }
                        }
                    }

                } catch (SecurityException e) {
                    //error
                }
            }

        } catch (Exception e) {
            e.printStackTrace();
        }

        Log.e("LOCATION", "Location is updated");
        return location;
    }

    /**
     * Function to get latitude
     * */
    public double getLatitude() {
        if (location != null) {
            latitude = location.getLatitude();
        }

        // return latitude
        return latitude;
    }

    /**
     * Function to get longitude
     * */
    public double getLongitude() {
        if (location != null) {
            longitude = location.getLongitude();
        }

        return longitude;
    }

    /**
     * http://stackoverflow.com/questions/33562951/android-6-0-location-permissions
     *
     * This method resolves operating system version differences when requesting
     * permission for locations.
     */
    public LocationPoint getLastKnownLocationIfAllowed() {

        if (ContextCompat.checkSelfPermission(mContext, Manifest.permission.ACCESS_FINE_LOCATION) ==
                PackageManager.PERMISSION_GRANTED) {

            provider = LocationManager.GPS_PROVIDER;
            Location location = m_locationManager.getLastKnownLocation(provider);
            System.out.println("\nGET LAST KNOWN LOCATION");
            if (location == null) {
                return null;
            }
            lastKnownLocation = new LocationPoint(location);

            return lastKnownLocation;
        } else {
            ActivityCompat.requestPermissions(
                    (Activity) mContext,
                    new String[]{Manifest.permission.ACCESS_FINE_LOCATION},
                    REQUEST_FINE_LOCATION);
        }

        return null;
    }

    public void display(LocationPoint point) {
        if (point == null || LatLongTextView == null)
            return;
        LatLongTextView.setText(point.toString());
    }


    /**
     * Function to show settings alert dialog
     * On pressing Settings button will lauch Settings Options
     * */
    public void showSettingsAlert() {
        AlertDialog.Builder alertDialog = new AlertDialog.Builder(mContext);

        // Setting Dialog Title
        alertDialog.setTitle("GPSActivity is settings");

        // Setting Dialog Message
        alertDialog.setMessage("GPSActivity is not enabled. Do you want to go to settings menu?");

        // On pressing Settings button
        alertDialog.setPositiveButton("Settings", new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int which) {
                Intent intent = new Intent(Settings.ACTION_LOCATION_SOURCE_SETTINGS);
                mContext.startActivity(intent);
            }
        });

        // on pressing cancel button
        alertDialog.setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int which) {
                dialog.cancel();
            }
        });

        // Showing Alert Message
        alertDialog.show();
    }

    @Override
    public void onLocationChanged(Location location) {
        Log.e("ONLOCATIONCHANGED", "ONLOCATIONCHANGED");

        if (location == null) {
            //((AmbulanceApp) mContext.getApplicationContext()).toasting("onLocationChanged, location is null");
            return;
        }
        //if we don't have an original location
        LocationPoint newLocation = new LocationPoint(location);

        final MqttProfileClient profileClient = ((AmbulanceApp) getApplication()).getProfileClient();
        // TODO: FIX newLocation.setStatus(profileClient.getSettings().getAmbulanceStatus()[]);

        if (lastKnownLocation == null) {
            System.out.println("\nPrevious location was null\n");
            lastKnownLocation = newLocation;
            // TODO: FIX ((AmbulanceApp) mContext.getApplicationContext()).writeLocationsToFile(newLocation);
            display(lastKnownLocation);

            // TODO: FIX ((AmbulanceApp) mContext.getApplicationContext()).updateLastKnownLocation(newLocation);

        }

        lastKnownLocation = newLocation;
        System.out.println("\n LOCATION IS BEING WRITTEN: " + newLocation.toString());
        //((AmbulanceApp) mContext.getApplicationContext()).toasting("LOCATION IS BEING WRITTEN");
        // TODO: FIX ((AmbulanceApp) mContext.getApplicationContext()).writeLocationsToFile(newLocation);
        display(lastKnownLocation);

        // TODO: FIX ((AmbulanceApp) mContext.getApplicationContext()).updateLastKnownLocation(newLocation);
        Log.e("ISLOCATIONCHANGED?", "onLocationChanged is called");
        // TODO: FIX ((AmbulanceApp) mContext.getApplicationContext()).mqttMaster();
    }

    @Override
    public void onProviderDisabled(String provider) {}

    @Override
    public void onProviderEnabled(String provider) {}

    @Override
    public void onStatusChanged(String provider, int status, Bundle extras) {}

    @Override
    public IBinder onBind(Intent arg0) {
        return null;
    }

    private static final LocationRequest REQUEST = LocationRequest.create()
            .setInterval(5000) // 5 seconds
            .setFastestInterval(16) // 16ms = 60fps
            .setPriority(LocationRequest.PRIORITY_HIGH_ACCURACY);
}