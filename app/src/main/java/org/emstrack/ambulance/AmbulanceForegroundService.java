package org.emstrack.ambulance;

import android.annotation.SuppressLint;
import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.os.Build;
import android.os.IBinder;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.app.NotificationCompat;
import android.support.v4.content.LocalBroadcastManager;
import android.text.TextUtils;
import android.util.Log;
import android.widget.Toast;

import com.google.android.gms.common.api.ApiException;
import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationResult;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.location.LocationSettingsRequest;
import com.google.android.gms.location.LocationSettingsResponse;
import com.google.android.gms.location.LocationSettingsStatusCodes;
import com.google.android.gms.location.SettingsClient;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.gson.FieldNamingPolicy;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

import org.eclipse.paho.android.service.MqttAndroidClient;
import org.eclipse.paho.client.mqttv3.MqttException;
import org.eclipse.paho.client.mqttv3.MqttMessage;
import org.emstrack.ambulance.util.LatLon;
import org.emstrack.models.Ambulance;
import org.emstrack.models.Hospital;
import org.emstrack.models.HospitalPermission;
import org.emstrack.models.Location;
import org.emstrack.mqtt.MqttProfileCallback;
import org.emstrack.mqtt.MqttProfileClient;
import org.emstrack.mqtt.MqttProfileMessageCallback;

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.TimeZone;
import java.util.UUID;

/**
 * Created by mauricio on 3/18/2018.
 */

public class AmbulanceForegroundService extends Service {

    class LocationUpdate {
        public android.location.Location location;
        public float bearing;
        public Date timestamp;
    };

    final static String TAG = AmbulanceForegroundService.class.getSimpleName();

    // Notification channel
    private static final String PRIMARY_CHANNEL = "default";
    private static final String PRIMARY_CHANNEL_LABEL = "Default channel";

    // SharedPreferences
    public static final String PREFERENCES_NAME = "org.emstrack.ambulance";
    public static final String PREFERENCES_USERNAME = "USERNAME";
    public static final String PREFERENCES_PASSWORD = "PASSWORD";

    private NotificationManager notificationManager;

    private static final String serverUri = "ssl://cruzroja.ucsd.edu:8883";
    private static final String clientId = "AmbulanceAppClient_" + UUID.randomUUID().toString();

    private static MqttProfileClient client;
    private static Ambulance _ambulance;
    private static List<Hospital> _hospitals;
    private static LocationUpdate _lastLocation;
    private static boolean requestingLocationUpdates = false;
    private static boolean canUpdateLocation = false;

    private static LocationSettingsRequest locationSettingsRequest;
    private static LocationRequest locationRequest;

    private FusedLocationProviderClient fusedLocationClient;
    private SharedPreferences sharedPreferences;

    // Rate at which locations should be pulled in
    // @ 70 mph gives an accuracy of about 30m
    private static final long UPDATE_INTERVAL = 1 * 1000;
    private static final long FASTEST_UPDATE_INTERVAL = UPDATE_INTERVAL / 2;
    // Group all updates to be done every minute
    private static final long MAX_WAIT_TIME = 60 * 1000;

    public class Actions {
        public final static String LOGIN = "org.emstrack.ambulance.ambulanceforegroundservice.action.LOGIN";
        public final static String GET_AMBULANCE = "org.emstrack.ambulance.ambulanceforegroundservice.action.GET_AMBULANCE";
        public final static String GET_HOSPITALS = "org.emstrack.ambulance.ambulanceforegroundservice.action.GET_HOSPITALS";
        public final static String START_LOCATION_UPDATES = "org.emstrack.ambulance.ambulanceforegroundservice.action.START_LOCATION_UPDATES";
        public final static String STOP_LOCATION_UPDATES = "org.emstrack.ambulance.ambulanceforegroundservice.action.STOP_LOCATION_UPDATES";
        public final static String LOGOUT = "org.emstrack.ambulance.ambulanceforegroundservice.action.LOGOUT";
    }

    public class BroadcastActions {
        public final static String HOSPITALS_UPDATE = "org.emstrack.ambulance.ambulanceforegroundservice.action.HOSPITALS_UPDATE";
        public final static String AMBULANCE_UPDATE = "org.emstrack.ambulance.ambulanceforegroundservice.action.AMBULANCE_UPDATE";
        public final static String LOCATION_UPDATE = "org.emstrack.ambulance.ambulanceforegroundservice.action.LOCATION_UPDATE";
    }

    public static class LocationUpdatesBroadcastReceiver extends BroadcastReceiver {

        public boolean bulkUpdates = true;
        public final String TAG = LocationUpdatesBroadcastReceiver.class.getSimpleName();

        public static String getUpdateString(LocationUpdate lastLocation) {
            double latitude = lastLocation.location.getLatitude();
            double longitude = lastLocation.location.getLongitude();
            double orientation = lastLocation.bearing;

            // format timestamp
            DateFormat df = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'");
            df.setTimeZone(TimeZone.getTimeZone("UTC"));
            String timestamp = df.format(lastLocation.timestamp);

            String updateString =  "{\"orientation\":" + orientation + ",\"location\":{" +
                    "\"latitude\":"+ latitude + ",\"longitude\":" + longitude +"},\"timestamp\":\"" + timestamp + "\"}";

            return updateString;
        }

        @Override
        public void onReceive(Context context, Intent intent) {
            if (intent != null) {
                final String action = intent.getAction();
                if (BroadcastActions.LOCATION_UPDATE.equals(action)) {
                    LocationResult result = LocationResult.extractResult(intent);
                    if (result != null) {

                        List<android.location.Location> locations = result.getLocations();
                        Log.i(TAG, "Received " + locations.size() + " location updates");

                        Ambulance ambulance = getAmbulance();
                        List<String> mqttUpdates = new ArrayList<>();
                        for (android.location.Location newLocation : locations) {

                            // update bearing
                            float bearing = newLocation.getBearing();
                            if (bearing != 0.0) {
                                _lastLocation.bearing = bearing;
                                Log.i(TAG, "newBearing = " + _lastLocation.bearing);
                            }

                            // calculate distance from last update
                            double distance = LatLon.CalculateDistanceHaversine(newLocation, _lastLocation.location);
                            Log.i(TAG, "distance = " + distance + ", newLocation = " + newLocation);

                            // Have we moved yet?
                            if (distance < LatLon.stationaryRadius)
                                continue;

                            // otherwise set last location
                            // NOTE: Needs to update both old and last because last may be changed by
                            // ambulance update during processing
                            Log.i(TAG, "Will update location on server");
                            _lastLocation.location = newLocation;
                            _lastLocation.timestamp = new Date(newLocation.getTime());

                            // Create update string
                            String updateString = getUpdateString(_lastLocation);

                            if (bulkUpdates) {

                                // add to update list
                                mqttUpdates.add(updateString);

                            } else {

                                // Publish to MQTT right away
                                try {
                                    final MqttProfileClient profileClient = getProfileClient();
                                    profileClient.publish("user/" + profileClient.getUsername() + "/ambulance/" +
                                                    ambulance.getId() + "/data",
                                            updateString, 1, false);
                                    Log.i(TAG, "onLocationChanged: update sent to server\n" + updateString);
                                } catch (MqttException e) {
                                    Log.i(TAG, "Could not update location on server");
                                    e.printStackTrace();
                                } catch (Exception e) {
                                    Log.i(TAG, "Could not update location on server");
                                    e.printStackTrace();
                                }
                            }

                        }

                        // bulk updates?
                        if (bulkUpdates && mqttUpdates.size() > 0) {

                            String updateString = "[" + TextUtils.join(",", mqttUpdates) + "]";

                            // Publish to MQTT right away
                            try {
                                final MqttProfileClient profileClient = getProfileClient();
                                profileClient.publish("user/" + profileClient.getUsername() + "/ambulance/" +
                                                ambulance.getId() + "/data",
                                        updateString, 1, false);
                                Log.i(TAG, "onLocationChanged: bulk update sent to server\n" + updateString);
                            } catch (MqttException e) {
                                Log.i(TAG, "Could not update location on server");
                                e.printStackTrace();
                            } catch (Exception e) {
                                Log.i(TAG, "Could not update location on server");
                                e.printStackTrace();
                            }

                        }

                    }
                }
            }
        }
    }

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    @Override
    public void onCreate() {

        // Create notification channel
        if (Build.VERSION.SDK_INT >= 26) {

            NotificationChannel channel = new NotificationChannel(PRIMARY_CHANNEL,
                    PRIMARY_CHANNEL_LABEL, NotificationManager.IMPORTANCE_DEFAULT);
            channel.setLightColor(Color.GREEN);
            channel.setLockscreenVisibility(Notification.VISIBILITY_PRIVATE);
            getNotificationManager().createNotificationChannel(channel);

        }

        // Create profile client
        getProfileClient(this);

        // Create shared preferences editor
        sharedPreferences = getSharedPreferences(PREFERENCES_NAME, MODE_PRIVATE);

        // Initialize fused location client
        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this);

    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {

        if (intent.getAction().equals(Actions.LOGIN)) {

            Log.i(TAG, "LOGIN Foreground Intent ");

            // Retrieve username and password
            String[] loginInfo = intent.getStringArrayExtra("CREDENTIALS");
            String username = loginInfo[0];
            String password = loginInfo[1];

            // Notify user
            Toast.makeText(this, "Logging in '" + username + "'", Toast.LENGTH_SHORT).show();

            // Login user
            login(username, password);

            // Ticker message
            String ticker = "Welcome " + username + ".";

            // Create notification
            Intent notificationIntent = new Intent(this, MainActivity.class);
            notificationIntent.setAction(MainActivity.MAIN_ACTION);
            notificationIntent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK
                    | Intent.FLAG_ACTIVITY_CLEAR_TASK);
            PendingIntent pendingIntent = PendingIntent.getActivity(this, 0,
                    notificationIntent, 0);

            Bitmap icon = BitmapFactory.decodeResource(getResources(),
                    R.mipmap.ic_launcher);

            NotificationCompat.Builder notificationBuilder;
            if (Build.VERSION.SDK_INT >= 26)
                notificationBuilder = new NotificationCompat.Builder(this, PRIMARY_CHANNEL);
            else
                notificationBuilder = new NotificationCompat.Builder(this);

            Notification notification = notificationBuilder
                    .setContentTitle("EMSTrack")
                    .setTicker(ticker)
                    .setContentText(ticker)
                    .setSmallIcon(R.mipmap.ic_launcher)
                    .setLargeIcon(Bitmap.createScaledBitmap(icon, 128, 128, false))
                    .setContentIntent(pendingIntent)
                    .setOngoing(true)
                    .build();

            startForeground(101, notification);

        } else if (intent.getAction().equals(Actions.LOGOUT)) {

            Log.i(TAG, "LOGOUT Foreground Intent");

            // Popup toast
            Toast.makeText(this, "Stopping service", Toast.LENGTH_SHORT).show();

            // logout
            logout();

            // stop service
            stopForeground(true);
            stopSelf();

        } else if (intent.getAction().equals(Actions.GET_AMBULANCE)) {

            Log.i(TAG, "GET_AMBULANCE Foreground Intent");

            // Retrieve ambulance
            int ambulanceId = intent.getIntExtra("AMBULANCE_ID", -1);
            retrieveAmbulance(ambulanceId);

        } else if (intent.getAction().equals(Actions.GET_HOSPITALS)) {

            Log.i(TAG, "GET_HOSPITALS Foreground Intent");

            // Retrieve hospitals
            retrieveHospitals();

        } else if (intent.getAction().equals(Actions.START_LOCATION_UPDATES)) {

            Log.i(TAG, "START_LOCATION_UPDATES Foreground Intent");

            if (canUpdateLocation())
                // start requesting location updates
                startLocationUpdates();
            else
                Log.i(TAG,"Cannot update location. Ignoring intent.");

        } else if (intent.getAction().equals(Actions.STOP_LOCATION_UPDATES)) {

            Log.i(TAG, "STOP_LOCATION_UPDATES Foreground Intent");

            if (canUpdateLocation())
                // stop requesting location updates
                removeLocationUpdates();
            else
                Log.i(TAG,"Cannot update location. Ignoring intent.");

        } else

            Log.i(TAG, "Unknown Intent");

        return START_STICKY;
    }

    /**
     * Get the NotificationManager.
     *
     * @return The system service NotificationManager
     */
    private NotificationManager getNotificationManager() {
        // lazy initialization
        if (notificationManager == null) {
            notificationManager = (NotificationManager) this.getSystemService(
                    Context.NOTIFICATION_SERVICE);
        }
        return notificationManager;
    }

    /**
     * Get the LocalBroadcastManager
     *
     * @return The system LocalBroadcastManager
     */
    private LocalBroadcastManager getLocalBroadcastManager() {
        return LocalBroadcastManager.getInstance(this);
    }

    /**
     * Get the MqttProfileClient.
     *
     * @param context Context
     * @return The MqttProfileClient
     */
    public static MqttProfileClient getProfileClient(Context context) {
        // lazy initialization
        if (client == null) {
            MqttAndroidClient androidClient = new MqttAndroidClient(context, serverUri, clientId);
            client = new MqttProfileClient(androidClient);
        }
        return client;
    }

    /**
     * Get the MqttProfileClient.
     *
     * @return The MqttProfileClient
     */
    public static MqttProfileClient getProfileClient() throws Exception {
        if (client == null)
            throw new Exception("ProfileClient has not been created yet!");
        return client;
    }

    /**
     * Get current ambulance
     *
     * @return the ambulance
     */
    public static Ambulance getAmbulance() {
        return _ambulance;
    }

    /**
     * Get current hospitals
     *
     * @return the list of hospitals
     */
    public static List<Hospital> getHospitals() {
        return _hospitals;
    }

    /**
     * Return true if requesting location updates
     *
     * @return the location updates status
     */
    public static boolean isRequestingLocationUpdates() { return requestingLocationUpdates; }

    /**
     * Return can update location
     *
     * @return the location update status
     */
    public static boolean canUpdateLocation() { return canUpdateLocation; }

    /**
     * Set can update location status
     *
     * @param canUpdateLocation the location update status
     */
    public static void setCanUpdateLocation(boolean canUpdateLocation) { AmbulanceForegroundService.canUpdateLocation = canUpdateLocation; }

    /**
     * Return the LocationRequest
     * @return the location request
     */
    public static LocationRequest getLocationRequest() {

        if (locationRequest == null) {

            // Create request for location updates
            locationRequest = LocationRequest.create()
                    .setPriority(LocationRequest.PRIORITY_HIGH_ACCURACY)
                    .setInterval(UPDATE_INTERVAL)
                    .setFastestInterval(FASTEST_UPDATE_INTERVAL)
                    .setMaxWaitTime(MAX_WAIT_TIME);

        }

        return locationRequest;

    }

    /**
     * Return the LocationSettingsRequest
     *
     * @return the location settings request
     */
    public static LocationSettingsRequest getLocationSettingsRequest() {

        if (locationSettingsRequest == null) {

            // Build location setting request
            LocationSettingsRequest.Builder builder = new LocationSettingsRequest.Builder();
            builder.addLocationRequest(getLocationRequest());
            locationSettingsRequest = builder.build();

        }

        return locationSettingsRequest;

    }

    /**
     * Logout
     */
    public void logout() {

        // remove ambulance
        removeAmbulance();

        // remove hospital list
        removeHospitals();

        // disconnect mqttclient
        MqttProfileClient profileClient = getProfileClient(this);
        try {
            profileClient.disconnect();
        } catch (MqttException e) {
            Log.d(TAG, "Failed to disconnect.");
        }

    }

    /**
     * Login user
     *
     * @param username Username
     * @param password Password
     */
    public void login(final String username, final String password) {

        // logout first
        logout();

        // Retrieve client
        final MqttProfileClient profileClient = getProfileClient(this);

        // Set callback to be called after profile is retrieved
        profileClient.setCallback(new MqttProfileCallback() {

            @Override
            public void onSuccess() {

                // Get preferences editor
                SharedPreferences.Editor editor = sharedPreferences.edit();

                // Save credentials
                Log.d(TAG, "Storing credentials");
                editor.putString(PREFERENCES_USERNAME, username);
                editor.putString(PREFERENCES_PASSWORD, password);
                editor.apply();

                // Toast
                Toast.makeText(AmbulanceForegroundService.this,
                        "User '" + username + "' successfully logged in.", Toast.LENGTH_SHORT).show();

                // Initiate AmbulanceListActivity
                Intent intent = new Intent(AmbulanceForegroundService.this,
                        AmbulanceListActivity.class);
                startActivity(intent);

            }

            @Override
            public void onFailure(Throwable exception) {

                Log.d(TAG, "Failed to retrieve profile.");

                // Alert user
                Toast.makeText(AmbulanceForegroundService.this,
                        "Could not log in user '" + username + "'.\n" + exception.toString(),
                        Toast.LENGTH_SHORT).show();

                // Initiate LoginActivity
                Intent intent = new Intent(AmbulanceForegroundService.this,
                        LoginActivity.class);
                startActivity(intent);

            }

        });

        try {

            // Attempt to connect
            profileClient.connect(username, password, new MqttProfileCallback() {

                @Override
                public void onSuccess() {
                    Log.d(TAG, "Successfully connected to broker.");
                }

                @Override
                public void onFailure(Throwable exception) {

                    Log.d(TAG, "Failed to connected to broker.");

                    String message;
                    if (exception instanceof MqttException) {
                        int reason = ((MqttException) exception).getReasonCode();
                        if (reason == MqttException.REASON_CODE_FAILED_AUTHENTICATION ||
                                reason == MqttException.REASON_CODE_NOT_AUTHORIZED ||
                                reason == MqttException.REASON_CODE_INVALID_CLIENT_ID)
                            message = getResources().getString(R.string.error_invalid_credentials);
                        else
                            message = String.format(getResources().getString(R.string.error_connection_failed),
                                    exception.toString());
                    } else {
                        message = exception.toString();
                    }

                    // Alert user
                    Toast.makeText(AmbulanceForegroundService.this, message,
                            Toast.LENGTH_SHORT).show();

                    // Initiate LoginActivity
                    Intent intent = new Intent(AmbulanceForegroundService.this,
                            LoginActivity.class);
                    startActivity(intent);

                }

            });

        } catch (MqttException exception) {

            // Alert user
            Toast.makeText(AmbulanceForegroundService.this,
                    String.format(getResources().getString(R.string.error_connection_failed),
                            exception.toString()),
                    Toast.LENGTH_SHORT).show();

            // Initiate LoginActivity
            Intent intent = new Intent(AmbulanceForegroundService.this,
                    LoginActivity.class);
            startActivity(intent);

        }

    }

    /**
     * Retrieve ambulance
     *
     * @param ambulanceId the ambulance id
     */
    public void retrieveAmbulance(final int ambulanceId) {

        // Is ambulance id valid?
        if (ambulanceId < 0) {

            // Alert user
            Toast.makeText(this,
                    "Invalid ambulance id",
                    Toast.LENGTH_SHORT).show();

            // Initiate LoginActivity
            Intent intent = new Intent(AmbulanceForegroundService.this,
                    AmbulanceListActivity.class);
            startActivity(intent);

            return;
        }

        // Is ambulance new?
        Ambulance ambulance = getAmbulance();
        if (ambulance != null && ambulance.getId() == ambulanceId) {
            return;
        }

        // Remove current ambulance
        removeAmbulance();

        // Retrieve client
        final MqttProfileClient profileClient = getProfileClient(this);

        try {

            // Start retrieving data
            profileClient.subscribe("ambulance/" + ambulanceId + "/data",
                    1, new MqttProfileMessageCallback() {

                        @Override
                        public void messageArrived(String topic, MqttMessage message) {

                            // Keep subscription to ambulance to make sure we receive
                            // the latest updates.

                            Log.d(TAG, "Retrieving ambulance.");

                            // first time we receive ambulance data
                            GsonBuilder gsonBuilder = new GsonBuilder();
                            gsonBuilder.setFieldNamingPolicy(FieldNamingPolicy.LOWER_CASE_WITH_UNDERSCORES);
                            Gson gson = gsonBuilder.create();

                            try {

                                // Parse and set ambulance
                                // TODO: Check for potential errors
                                Ambulance ambulance = gson
                                        .fromJson(new String(message.getPayload()),
                                                Ambulance.class);

                                // Has location been updated?
                                if (_lastLocation == null || ambulance.getTimestamp().after(_lastLocation.timestamp)) {
                                    // Update last location
                                    _lastLocation = new LocationUpdate();
                                    _lastLocation.location =
                                            new android.location.Location("FusedLocationClient");
                                    _lastLocation.location.setLatitude(ambulance.getLocation().getLatitude());
                                    _lastLocation.location.setLongitude(ambulance.getLocation().getLongitude());
                                    _lastLocation.bearing = (float) ambulance.getOrientation();
                                    _lastLocation.timestamp = ambulance.getTimestamp();
                                }

                                // Set current ambulance
                                _ambulance = ambulance;

                                // Broadcast ambulance update
                                Intent localIntent = new Intent(BroadcastActions.AMBULANCE_UPDATE);
                                getLocalBroadcastManager().sendBroadcast(localIntent);

                                // Initiate MainActivity
                                // Intent intent = new Intent(AmbulanceForegroundService.this,
                                //        MainActivity.class);
                                //startActivity(intent);

                            } catch (Exception e) {

                                Log.i(TAG, "Could not parse ambulance update.");

                                // Alert user
                                Toast.makeText(AmbulanceForegroundService.this,
                                        "Could not parse ambulance",
                                        Toast.LENGTH_SHORT).show();

                                // Go back to ambulance selection if no ambulance is selected
                                if (getAmbulance() == null) {

                                    // Initiate LoginActivity
                                    Intent intent = new Intent(AmbulanceForegroundService.this,
                                            AmbulanceListActivity.class);
                                    startActivity(intent);

                                }

                            }

                        }

                    });

        } catch (MqttException e) {

            Log.d(TAG, "Could not subscribe to ambulance data");

            // Alert user
            Toast.makeText(this,
                    "Could not subscribe to ambulance",
                    Toast.LENGTH_SHORT).show();

            // Initiate LoginActivity
            Intent intent = new Intent(AmbulanceForegroundService.this,
                    AmbulanceListActivity.class);
            startActivity(intent);

        }

    }

    /**
     * Remove current ambulance
     */
    public void removeAmbulance() {

        Ambulance ambulance = getAmbulance();
        if (ambulance == null ) {
            Log.i(TAG,"No ambulance to remove.");
            return;
        }

        // remove location updates
        removeLocationUpdates();

        // Retrieve client
        final MqttProfileClient profileClient = getProfileClient(this);
        
        try {

            // Unsubscribe to ambulance data
            profileClient.unsubscribe("ambulance/" + ambulance.getId() + "/data");

        } catch (MqttException exception) {
            Log.d(TAG, "Could not unsubscribe to 'ambulance/" + ambulance.getId() + "/data'");
        }
        
        // Remove current ambulance
        _ambulance = null;
        _lastLocation = null;
        
    }

    /**
     * Retrieve hospitals
     */
    public void retrieveHospitals() {

        // Retrieve hospital data
        final MqttProfileClient profileClient = getProfileClient(this);

        // Get list of hospitals
        final List<HospitalPermission> hospitalPermissions = profileClient.getProfile().getHospitals();

        // Initialize hospitals
        _hospitals = new ArrayList<Hospital>();

        // Loop over all hospitals
        for (HospitalPermission hospitalPermission : hospitalPermissions) {

            final int hospitalId = hospitalPermission.getHospitalId();

            try {

                // Start retrieving data
                profileClient.subscribe("hospital/" + hospitalId + "/data",
                        1, new MqttProfileMessageCallback() {

                            @Override
                            public void messageArrived(String topic, MqttMessage message) {

                                try {

                                    // Unsubscribe to hospital data
                                    profileClient.unsubscribe("hospital/" + hospitalId + "/data");

                                } catch (MqttException exception) {
                                    Log.d(TAG, "Could not unsubscribe to 'hospital/" + hospitalId + "/data'");
                                    return;
                                }

                                // Parse to hospital metadata
                                GsonBuilder gsonBuilder = new GsonBuilder();
                                gsonBuilder.setFieldNamingPolicy(FieldNamingPolicy.LOWER_CASE_WITH_UNDERSCORES);
                                Gson gson = gsonBuilder.create();

                                // Found hospital
                                final Hospital hospital = gson.fromJson(message.toString(), Hospital.class);

                                // Add to hospital list
                                _hospitals.add(hospital);

                                // Done yet?
                                if (getHospitals().size() == hospitalPermissions.size()) {

                                    Log.d(TAG, "Done retrieving all hospitals");

                                    // Broadcast hospitals update
                                    Intent localIntent = new Intent(BroadcastActions.HOSPITALS_UPDATE);
                                    getLocalBroadcastManager().sendBroadcast(localIntent);

                                }
                            }
                        });

            } catch (MqttException e) {
                Log.d(TAG, "Could not subscribe to hospital data");
            }

        }

    }

    /**
     * Remove current hospitals
     */
    public void removeHospitals() {

        List<Hospital> hospitals = getHospitals();
        if (hospitals == null || hospitals.size() == 0) {
            Log.i(TAG, "No hospital to remove.");
            return;
        }

        // Retrieve client
        final MqttProfileClient profileClient = getProfileClient(this);

        // Loop over all hospitals
        for (Hospital hospital : hospitals) {

            try {

                // Unsubscribe to hospital data
                profileClient.unsubscribe("hospital/" + hospital.getId() + "/data");

            } catch (MqttException exception) {
                Log.d(TAG, "Could not unsubscribe to 'hospital/" + hospital.getId() + "/data'");
            }

        }

        // Remove hospitals
        _hospitals = null;

    }


    private void startLocationUpdates() {

        // Logged in?
        if (getAmbulance() == null) {
            Log.i(TAG, "No ambulance selected.");
            return;
        }

        // Already started?
        if (requestingLocationUpdates) {
            Log.i(TAG, "Already requesting location updates. Skipping.");
            return;
        }

        // Create settings client
        SettingsClient settingsClient = LocationServices.getSettingsClient(this);

        // Check if the device has the necessary location settings.
        settingsClient.checkLocationSettings(locationSettingsRequest)
                .addOnSuccessListener(new OnSuccessListener<LocationSettingsResponse>() {
                    @SuppressLint("MissingPermission")
                    @Override
                    public void onSuccess(LocationSettingsResponse locationSettingsResponse) {
                        Log.i(TAG, "All location settings are satisfied. Starting location updates.");
                        requestLocationUpdates();
                    }
                })
                .addOnFailureListener(new OnFailureListener() {
                    @Override
                    public void onFailure(@NonNull Exception e) {
                        int statusCode = ((ApiException) e).getStatusCode();
                        String message = "Location settings are inadequate, and cannot be fixed here. ";
                        switch (statusCode) {
                            case LocationSettingsStatusCodes.RESOLUTION_REQUIRED:
                                message += "Try restarting app.";
                                break;
                            case LocationSettingsStatusCodes.SETTINGS_CHANGE_UNAVAILABLE:
                                message += "Please fix in Settings.";
                        }
                        Log.e(TAG, message);

                        // notify user
                        Toast.makeText(AmbulanceForegroundService.this, message, Toast.LENGTH_SHORT).show();

                    }
                });

    }

    /**
     * Get the service's PendingIntent.
     * Override to customize.
     * @return the service's PendingIntent
     */
    protected PendingIntent getPendingIntent() {
        Intent intent = new Intent(this, LocationUpdatesBroadcastReceiver.class);
        intent.setAction(BroadcastActions.LOCATION_UPDATE);
        return PendingIntent.getBroadcast(this, 0, intent, PendingIntent.FLAG_UPDATE_CURRENT);
    }

    /**
     * Handles the Request Updates button and requests start of location updates.
     */
    public void requestLocationUpdates() {

        try {

            fusedLocationClient.requestLocationUpdates(getLocationRequest(), getPendingIntent())
                    .addOnSuccessListener(new OnSuccessListener<Void>() {
                        @Override
                        public void onSuccess(Void aVoid) {
                            Log.i(TAG, "Starting location updates");
                            requestingLocationUpdates = true;
                        }
                    })
                    .addOnFailureListener(new OnFailureListener() {
                        @Override
                        public void onFailure(@NonNull Exception e) {
                            Log.i(TAG, "Failed to start location updates");
                            e.printStackTrace();
                        }
                    });

        } catch (SecurityException e) {
            Log.i(TAG, "Failed to start location updates");
            e.printStackTrace();
        }
    }

    /**
     * Handles the Remove Updates button, and requests removal of location updates.
     */
    public void removeLocationUpdates() {

        // Already started?
        if (!requestingLocationUpdates) {
            Log.i(TAG, "Not requesting location updates. Skipping.");
            return;
        }

        fusedLocationClient.removeLocationUpdates(getPendingIntent())
                .addOnSuccessListener(new OnSuccessListener<Void>() {
                    @Override
                    public void onSuccess(Void aVoid) {
                        Log.i(TAG, "Stopping location updates");
                        requestingLocationUpdates = false;
                    }
                })
                .addOnFailureListener(new OnFailureListener() {
                    @Override
                    public void onFailure(@NonNull Exception e) {
                        Log.i(TAG, "Failed to stop location updates");
                        e.printStackTrace();
                    }
                });
    }

}
