package org.emstrack.ambulance.services;

import android.annotation.SuppressLint;
import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.os.Build;
import android.os.Bundle;
import android.os.IBinder;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.app.NotificationCompat;
import android.support.v4.app.NotificationManagerCompat;
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
import org.emstrack.ambulance.MainActivity;
import org.emstrack.ambulance.R;
import org.emstrack.ambulance.util.LocationFilter;
import org.emstrack.ambulance.util.LocationUpdate;
import org.emstrack.models.Ambulance;
import org.emstrack.models.AmbulancePermission;
import org.emstrack.models.Hospital;
import org.emstrack.models.HospitalPermission;
import org.emstrack.mqtt.MqttProfileCallback;
import org.emstrack.mqtt.MqttProfileClient;
import org.emstrack.mqtt.MqttProfileMessageCallback;

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.TimeZone;
import java.util.UUID;

/**
 * Created by mauricio on 3/18/2018.
 */

public class  AmbulanceForegroundService extends BroadcastService implements MqttProfileCallback {

    final static String TAG = AmbulanceForegroundService.class.getSimpleName();

    // Rate at which locations should be pulled in
    // @ 70 mph gives an accuracy of about 30m
    private static final long UPDATE_INTERVAL = 1 * 1000;
    private static final long FASTEST_UPDATE_INTERVAL = UPDATE_INTERVAL / 2;
    // Group all updates to be done every minute
    private static final long MAX_WAIT_TIME = 60 * 1000;

    // Notification channel
    public static final int NOTIFICATION_ID = 101;
    public static final int ERROR_NOTIFICATION_ID = 102;
    public static final int WARNING_NOTIFICATION_ID = 103;
    public static final String PRIMARY_CHANNEL = "default";
    public static final String PRIMARY_CHANNEL_LABEL = "Default channel";

    // SharedPreferences
    public static final String PREFERENCES_NAME = "org.emstrack.ambulance";
    public static final String PREFERENCES_USERNAME = "USERNAME";
    public static final String PREFERENCES_PASSWORD = "PASSWORD";

    private static final String serverUri = "ssl://cruzroja.ucsd.edu:8883";
    private static final String baseClientId = "v_0_2_1_AndroidAppClient_";

    private static MqttProfileClient client;
    private static Ambulance _ambulance;
    private static Map<Integer, Hospital> _hospitals;
    private static Map<Integer, Ambulance> _ambulances;
    private static LocationUpdate _lastLocation;
    private static Date _lastServerUpdate;
    private static boolean requestingLocationUpdates = false;
    private static boolean canUpdateLocation = false;
    private static ArrayList<String> _updateBuffer = new ArrayList<>();

    private static LocationSettingsRequest locationSettingsRequest;
    private static LocationRequest locationRequest;

    private NotificationManager notificationManager;

    private FusedLocationProviderClient fusedLocationClient;
    private SharedPreferences sharedPreferences;

    public class Actions {
        public final static String LOGIN = "org.emstrack.ambulance.ambulanceforegroundservice.action.LOGIN";
        public final static String GET_AMBULANCE = "org.emstrack.ambulance.ambulanceforegroundservice.action.GET_AMBULANCE";
        public final static String GET_AMBULANCES= "org.emstrack.ambulance.ambulanceforegroundservice.action.GET_AMBULANCES";
        public final static String STOP_AMBULANCES= "org.emstrack.ambulance.ambulanceforegroundservice.action.STOP_AMBULANCES";
        public final static String GET_HOSPITALS = "org.emstrack.ambulance.ambulanceforegroundservice.action.GET_HOSPITALS";
        public final static String REQUEST_LOCATION_UPDATES = "org.emstrack.ambulance.ambulanceforegroundservice.action.REQUEST_LOCATION_UPDATES";
        public final static String START_LOCATION_UPDATES = "org.emstrack.ambulance.ambulanceforegroundservice.action.START_LOCATION_UPDATES";
        public final static String STOP_LOCATION_UPDATES = "org.emstrack.ambulance.ambulanceforegroundservice.action.STOP_LOCATION_UPDATES";
        public final static String UPDATE_AMBULANCE = "org.emstrack.ambulance.ambulanceforegroundservice.action.UPDATE_AMBULANCE";
        public final static String UPDATE_NOTIFICATION = "org.emstrack.ambulance.ambulanceforegroundservice.action.UPDATE_NOTIFICATION";
        public final static String LOGOUT = "org.emstrack.ambulance.ambulanceforegroundservice.action.LOGOUT";
    }

    public class BroadcastExtras {
        public final static String MESSAGE = "org.emstrack.ambulance.ambulanceforegroundservice.broadcastextras.MESSAGE";
    }

    public class BroadcastActions {
        public final static String HOSPITALS_UPDATE = "org.emstrack.ambulance.ambulanceforegroundservice.broadcastaction.HOSPITALS_UPDATE";
        public static final String AMBULANCES_UPDATE = "org.emstrack.ambulance.ambulanceforegroundservice.broadcastaction.AMBULANCES_UPDATE";
        public final static String AMBULANCE_UPDATE = "org.emstrack.ambulance.ambulanceforegroundservice.broadcastaction.AMBULANCE_UPDATE";
        public final static String LOCATION_UPDATE = "org.emstrack.ambulance.ambulanceforegroundservice.broadcastaction.LOCATION_UPDATE";
        public final static String SUCCESS = "org.emstrack.ambulance.ambulanceforegroundservice.broadcastaction.SUCCESS";
        public final static String FAILURE = "org.emstrack.ambulance.ambulanceforegroundservice.broadcastaction.FAILURE";
    }

    public static class LocationUpdatesBroadcastReceiver extends BroadcastReceiver {

        public final String TAG = LocationUpdatesBroadcastReceiver.class.getSimpleName();
        public LocationFilter filter = new LocationFilter(null);

        public static String getUpdateString(LocationUpdate lastLocation) {
            double latitude = lastLocation.getLocation().getLatitude();
            double longitude = lastLocation.getLocation().getLongitude();
            double orientation = lastLocation.getBearing();

            // format timestamp
            DateFormat df = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'");
            df.setTimeZone(TimeZone.getTimeZone("UTC"));
            String timestamp = df.format(lastLocation.getTimestamp());

            String updateString =  "{\"orientation\":" + orientation + ",\"location\":{" +
                    "\"latitude\":"+ latitude + ",\"longitude\":" + longitude +"},\"timestamp\":\"" + timestamp + "\"}";

            return updateString;
        }

        @Override
        public void onReceive(Context context, Intent intent) {
            if (intent != null) {
                final String action = intent.getAction();
                if (BroadcastActions.LOCATION_UPDATE.equals(action)) {

                    // get profile client
                    final MqttProfileClient profileClient = AmbulanceForegroundService.getProfileClient(context);

                    // stop updates?
                    Ambulance ambulance = getAmbulance();
                    if (ambulance != null && profileClient != null &&
                            (ambulance.getLocationClientId() == null ||
                                    !profileClient.getClientId().equals(ambulance.getLocationClientId()))) {

                        // turn off tracking
                        Intent stopIntent = new Intent(context, AmbulanceForegroundService.class);
                        stopIntent.setAction(AmbulanceForegroundService.Actions.STOP_LOCATION_UPDATES);
                        context.startService(stopIntent);

                        return;

                    }

                    // Retrieve results
                    LocationResult result = LocationResult.extractResult(intent);
                    if (result != null) {

                        List<android.location.Location> locations = result.getLocations();
                        Log.i(TAG, "Received " + locations.size() + " location updates");

                        // Initialize filter
                        if (_lastLocation != null)
                            filter.setLocation(_lastLocation);

                        // Filter location
                        List<LocationUpdate> filteredLocations = filter.update(locations);

                        // Add to updates
                        ArrayList<String> mqttUpdates = new ArrayList<>();
                        for (LocationUpdate newLocation : filteredLocations) {

                            // Set last location
                            _lastLocation = newLocation;

                            // add to update list
                            mqttUpdates.add(getUpdateString(_lastLocation));

                        }

                        // any updates?
                        if (mqttUpdates.size() > 0) {

                            // Submit updates
                            Intent updateIntent = new Intent(context, AmbulanceForegroundService.class);
                            updateIntent.setAction(AmbulanceForegroundService.Actions.UPDATE_AMBULANCE);
                            Bundle bundle = new Bundle();
                            bundle.putStringArrayList("UPDATES", mqttUpdates);
                            updateIntent.putExtras(bundle);
                            context.startService(updateIntent);

                        }

                        // Notification message
                        String message = "Last update at "
                                + new SimpleDateFormat("d MMM HH:mm:ss z", Locale.getDefault()).format(new Date());

                        if (_lastServerUpdate != null)
                            message += "\nLast server update at "
                                    + new SimpleDateFormat("d MMM HH:mm:ss z", Locale.getDefault()).format(_lastServerUpdate);

                        // modify foreground service notification
                        Intent notificationIntent = new Intent(context, AmbulanceForegroundService.class);
                        notificationIntent.setAction(AmbulanceForegroundService.Actions.UPDATE_NOTIFICATION);
                        notificationIntent.putExtra("MESSAGE", message);
                        context.startService(notificationIntent);

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

        // quick return
        if (intent == null) {
            Log.i(TAG, "null intent, return.");
            return START_STICKY;
        }

        // retrieve uuid
        final String uuid = intent.getStringExtra(OnServiceComplete.UUID);

        if (intent.getAction().equals(Actions.LOGIN)) {

            Log.i(TAG, "LOGIN Foreground Intent ");

            // Retrieve username and password
            String[] loginInfo = intent.getStringArrayExtra("CREDENTIALS");
            final String username = loginInfo[0];
            final String password = loginInfo[1];

            // Notify user
            Toast.makeText(this, "Logging in '" + username + "'", Toast.LENGTH_SHORT).show();

            // What to do when login completes?
            OnServiceComplete onServiceComplete = new OnServiceComplete(this,
                    AmbulanceForegroundService.BroadcastActions.SUCCESS,
                    AmbulanceForegroundService.BroadcastActions.FAILURE,
                    null) {

                public void run() {

                    // Login user
                    login(username, password, getUuid());

                }

                @Override
                public void onSuccess(Bundle extras) {

                    // Ticker message
                    String ticker = "Welcome " + username + ".";

                    // Create notification
                    Intent notificationIntent = new Intent(AmbulanceForegroundService.this, MainActivity.class);
                    notificationIntent.setAction(MainActivity.MAIN_ACTION);
                    notificationIntent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK
                            | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                    PendingIntent pendingIntent = PendingIntent.getActivity(AmbulanceForegroundService.this, 0,
                            notificationIntent, 0);

                    Bitmap icon = BitmapFactory.decodeResource(getResources(),
                            R.mipmap.ic_launcher);

                    NotificationCompat.Builder notificationBuilder;
                    if (Build.VERSION.SDK_INT >= 26)
                        notificationBuilder = new NotificationCompat.Builder(AmbulanceForegroundService.this,
                                AmbulanceForegroundService.PRIMARY_CHANNEL);
                    else
                        notificationBuilder = new NotificationCompat.Builder(AmbulanceForegroundService.this);

                    Notification notification = notificationBuilder
                            .setContentTitle("EMSTrack")
                            .setTicker(ticker)
                            .setContentText(ticker)
                            .setSmallIcon(R.mipmap.ic_launcher)
                            .setLargeIcon(Bitmap.createScaledBitmap(icon, 128, 128, false))
                            .setContentIntent(pendingIntent)
                            .setOngoing(true)
                            .build();

                    startForeground(NOTIFICATION_ID, notification);

                    // Broadcast success
                    Intent localIntent = new Intent(BroadcastActions.SUCCESS);
                    if (extras != null)
                        localIntent.putExtras(extras);
                    sendBroadcastWithUUID(localIntent, uuid);

                }

                @Override
                public void onFailure(Bundle extras) {

                    // Broadcast failure
                    Intent localIntent = new Intent(BroadcastActions.FAILURE);
                    if (extras != null)
                        localIntent.putExtras(extras);
                    sendBroadcastWithUUID(localIntent, uuid);

                }

            };

        } else if (intent.getAction().equals(Actions.LOGOUT)) {

            Log.i(TAG, "LOGOUT Foreground Intent");

            // logout
            logout(uuid);

            // stop service
            stopForeground(true);
            stopSelf();

        } else if (intent.getAction().equals(Actions.GET_AMBULANCE)) {

            Log.i(TAG, "GET_AMBULANCE Foreground Intent");

            // Retrieve ambulance
            int ambulanceId = intent.getIntExtra("AMBULANCE_ID", -1);
            retrieveAmbulance(ambulanceId, uuid);

        } else if (intent.getAction().equals(Actions.GET_AMBULANCES)) {

            Log.i(TAG, "GET_AMBULANCES Foreground Intent");

            // Retrieve ambulances
            retrieveAmbulances(uuid);

        } else if (intent.getAction().equals(Actions.STOP_AMBULANCES)) {

            Log.i(TAG, "STOP_AMBULANCES Foreground Intent");

            // Stop ambulances
            stopAmbulances(uuid);

        } else if (intent.getAction().equals(Actions.GET_HOSPITALS)) {

            Log.i(TAG, "GET_HOSPITALS Foreground Intent");

            // Retrieve hospitals
            retrieveHospitals(uuid);

        } else if (intent.getAction().equals(Actions.REQUEST_LOCATION_UPDATES)) {

            Log.i(TAG, "REQUEST_LOCATION_UPDATES Foreground Intent");

            if (canUpdateLocation())
                // request location updates
                requestLocationUpdates(uuid);
            else
                Log.i(TAG,"Cannot update location. Ignoring intent.");

        } else if (intent.getAction().equals(Actions.START_LOCATION_UPDATES)) {

            Log.i(TAG, "START_LOCATION_UPDATES Foreground Intent");

            if (canUpdateLocation())
                // start location updates
                startLocationUpdates();
            else
                Log.i(TAG,"Cannot update location. Ignoring intent.");

        } else if (intent.getAction().equals(Actions.STOP_LOCATION_UPDATES)) {

            Log.i(TAG, "STOP_LOCATION_UPDATES Foreground Intent");

            if (canUpdateLocation())
                // stop requesting location updates
                stopLocationUpdates();
            else
                Log.i(TAG,"Cannot update location. Ignoring intent.");

        } else if (intent.getAction().equals(Actions.UPDATE_AMBULANCE)) {

            Log.i(TAG, "UPDATE_AMBULANCE Foreground Intent");

            Bundle bundle = intent.getExtras();

            // Retrieve update string
            String update = bundle.getString("UPDATE");
            if (update != null) {

                // Set update time
                _lastServerUpdate = new Date();

                // update mqtt server
                updateAmbulance(update);

            }

            // Retrieve update string array
            ArrayList<String> updateArray = bundle.getStringArrayList("UPDATES");
            if (updateArray != null) {

                // Set update time
                _lastServerUpdate = new Date();

                // update mqtt server
                updateAmbulance(updateArray);

            }

        } else if (intent.getAction().equals(Actions.UPDATE_NOTIFICATION)) {

            Log.i(TAG, "UPDATE_NOTIFICATION Foreground Intent");

            // Retrieve update string
            String message = intent.getStringExtra("MESSAGE");
            if (message != null)
                updateNotification(message);

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
     * Get the MqttProfileClient.
     *
     * @param context Context
     * @return The MqttProfileClient
     */
    public static MqttProfileClient getProfileClient(Context context) {
        // lazy initialization
        if (client == null) {
            String clientId = baseClientId + UUID.randomUUID().toString();
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

    public static int getAmbulanceId() {
        if (_ambulance == null)
            return -1;
        else
            return getAmbulance().getId();
    }

    /**
     * Get current hospitals
     *
     * @return the list of hospitals
     */
    public static Map<Integer, Hospital> getHospitals() {
        return _hospitals;
    }

    /**
     * Get current ambulances
     *
     * @return the list of ambulances
     */
    public static Map<Integer, Ambulance> getAmbulances() { return _ambulances; }

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
     * Send bulk updates to current ambulance
     * Allowing arbitrary updates might be too broad and a security concern
     *
     * @param updates string array
     */
    public void updateAmbulance(ArrayList<String> updates) {

        // Join updates in array
        String updateArray = "[" + TextUtils.join(",", updates) + "]";

        // send to server
        updateAmbulance(updateArray);
    }

    /**
     * Add update to buffer for later processing
     *
     * @param update
     */
    public void addToBuffer(String update) {

        // buffer updates and return
        // TODO: limit size of buffer or write to disk
        _updateBuffer.add(update);

        // Log and add notification
        Log.d(TAG, "MQTT Client is not online. Buffering updates.");

        // Create an explicit intent for an Activity in your app
        Intent intent = new Intent(this, MainActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        PendingIntent pendingIntent = PendingIntent.getActivity(this, 0, intent, 0);

        NotificationCompat.Builder mBuilder = new NotificationCompat.Builder(this, PRIMARY_CHANNEL)
                .setSmallIcon(R.mipmap.ic_launcher)
                .setContentTitle("EMSTrack")
                .setContentText(String.format("MQTT client is not online, buffering %1$d messages.",
                        _updateBuffer.size()))
                .setPriority(NotificationCompat.PRIORITY_DEFAULT)
                .setContentIntent(pendingIntent)
                .setAutoCancel(true);

        NotificationManagerCompat notificationManager = NotificationManagerCompat.from(this);
        notificationManager.notify(WARNING_NOTIFICATION_ID, mBuilder.build());

    }

    public void processBuffer() {

        // Get client
        final MqttProfileClient profileClient = getProfileClient(AmbulanceForegroundService.this);

        // return if not online
        if (!getProfileClient(this).isConnected() )
            return;

        // Log and add notification
        Log.d(TAG, "MQTT Client is online. Consuming buffer.");

        // Has ambulance?
        Ambulance ambulance = getAmbulance();
        if (ambulance != null ) {

            String topic = String.format("user/%1$s/ambulance/%2$d/data",
                    profileClient.getUsername(), ambulance.getId());

            // Are there pending updates in the buffer?
            Iterator<String> iterator = _updateBuffer.iterator();
            while (iterator.hasNext()) {

                try {

                    // Publish to MQTT
                    profileClient.publish(topic, iterator.next(), 1, false);

                }  catch (Exception e) {

                    // Notify user and return

                    // Create an explicit intent for an Activity in your app
                    Intent intent = new Intent(this, MainActivity.class);
                    intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                    PendingIntent pendingIntent = PendingIntent.getActivity(this, 0, intent, 0);

                    NotificationCompat.Builder mBuilder = new NotificationCompat.Builder(this, PRIMARY_CHANNEL)
                            .setSmallIcon(R.mipmap.ic_launcher)
                            .setContentTitle(getString(R.string.EMSTrack))
                            .setContentText(String.format(getString(R.string.couldNotProcessBuffer), _updateBuffer.size()))
                            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
                            .setContentIntent(pendingIntent)
                            .setAutoCancel(true);

                    NotificationManagerCompat notificationManager = NotificationManagerCompat.from(this);
                    notificationManager.notify(WARNING_NOTIFICATION_ID, mBuilder.build());

                    return;
                }

                // Remove from buffer
                iterator.remove();

            }

        }

        // Create an explicit intent for an Activity in your app
        Intent intent = new Intent(this, MainActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        PendingIntent pendingIntent = PendingIntent.getActivity(this, 0, intent, 0);

        NotificationCompat.Builder mBuilder = new NotificationCompat.Builder(this, PRIMARY_CHANNEL)
                .setSmallIcon(R.mipmap.ic_launcher)
                .setContentTitle(getString(R.string.EMSTrack))
                .setContentText(getString(R.string.localBufferIsEmpty))
                .setPriority(NotificationCompat.PRIORITY_DEFAULT)
                .setContentIntent(pendingIntent)
                .setAutoCancel(true);

        NotificationManagerCompat notificationManager = NotificationManagerCompat.from(this);
        notificationManager.notify(WARNING_NOTIFICATION_ID, mBuilder.build());

    }

    /**
     * Send update to current ambulance
     * Allowing arbitrary updates might be too broad and a security concern
     *
     * @param update string
     */
    public void updateAmbulance(String update) {

        // Error message
        String message = getString(R.string.couldNotUpdateAmbulanceOnServer) + "\n";

        try {

            // is connected?
            final MqttProfileClient profileClient = getProfileClient(AmbulanceForegroundService.this);
            if (!profileClient.isConnected()) {

                // add to buffer and return
                addToBuffer(update);
                return;

            }

            // Has ambulance?
            Ambulance ambulance = getAmbulance();
            if (ambulance != null ) {

                // Are there pending updates in the buffer?
                processBuffer();

                // Publish current update to MQTT
                profileClient.publish(String.format("user/%1$s/ambulance/%2$d/data",
                        profileClient.getUsername(), ambulance.getId()), update, 1, false);
                return;

            } else {

                message += "Could not find ambulance";

            }

        }
        catch (MqttException e) { message += e.toString(); }
        catch (Exception e) { message += e.toString(); }

        // Log and build a notification in case of error
        Log.i(TAG,message);

        // Create an explicit intent for an Activity in your app
        Intent intent = new Intent(this, MainActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        PendingIntent pendingIntent = PendingIntent.getActivity(this, 0, intent, 0);

        NotificationCompat.Builder mBuilder = new NotificationCompat.Builder(this, PRIMARY_CHANNEL)
                .setSmallIcon(R.mipmap.ic_launcher)
                .setContentTitle("EMSTrack")
                .setContentText(message)
                .setPriority(NotificationCompat.PRIORITY_DEFAULT)
                .setContentIntent(pendingIntent)
                .setAutoCancel(true);

        NotificationManagerCompat notificationManager = NotificationManagerCompat.from(this);
        notificationManager.notify(ERROR_NOTIFICATION_ID, mBuilder.build());

    }

    /**
     * Update current notification message
     *
     * @param message the message
     */
    public void updateNotification(String message) {

        // Create an explicit intent for an Activity in your app
        Intent intent = new Intent(this, MainActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        PendingIntent pendingIntent = PendingIntent.getActivity(this, 0, intent, 0);

        Notification notification = new NotificationCompat.Builder(this, PRIMARY_CHANNEL)
                .setContentTitle("EMSTrack")
                .setTicker(message)
                .setContentText(message)
                .setStyle(new NotificationCompat.BigTextStyle()
                        .bigText(message))
                .setSmallIcon(R.mipmap.ic_launcher)
                .setContentIntent(pendingIntent)
                .setOngoing(true).build();

        NotificationManagerCompat notificationManager = NotificationManagerCompat.from(this);
        notificationManager.notify(NOTIFICATION_ID, notification);

    }

    /**
     * Logout
     */
    public void logout(final String uuid) {

        // remove ambulance
        removeAmbulance();

        // remove hospital map
        removeHospitals();

        // remove ambulance map
        removeAmbulances();

        // disconnect mqttclient
        MqttProfileClient profileClient = getProfileClient(this);
        try {

            profileClient.disconnect(new MqttProfileCallback() {

                @Override
                public void onSuccess() {

                    Log.d(TAG, "Successfully disconnected from broker.");

                    // Broadcast success
                    Intent localIntent = new Intent(BroadcastActions.SUCCESS);
                    sendBroadcastWithUUID(localIntent, uuid);

                }

                @Override
                public void onFailure(Throwable exception) {

                    Log.d(TAG, "Failed to disconnect from brocker.");

                    String message = getString(R.string.failedToDisconnectFromBroker) + "\n";
                    if (exception instanceof MqttException) {
                        int reason = ((MqttException) exception).getReasonCode();
                        message += getResources().getString(R.string.error_connection_failed, exception.toString());
                    } else {
                        message += getString(R.string.Exception) + exception.toString();
                    }

                    Log.d(TAG, "message = " + message);

                    // Broadcast failure
                    Intent localIntent = new Intent(BroadcastActions.FAILURE);
                    localIntent.putExtra(BroadcastExtras.MESSAGE, message);
                    sendBroadcastWithUUID(localIntent, uuid);

                }

            });

        } catch (MqttException e) {

            Log.d(TAG, "Failed to disconnect.");

            // Broadcast failure
            Intent localIntent = new Intent(BroadcastActions.FAILURE);
            localIntent.putExtra(BroadcastExtras.MESSAGE, "Could not disconnect.");
            sendBroadcastWithUUID(localIntent, uuid);

        }

    }

    /**
     * Callback after handling successful connection
     */
    @Override
    public void onSuccess() {

        Log.d(TAG, "onSuccess after reconnect. Restoring subscriptions.");

        final boolean hasAmbulance = _ambulance != null;
        final boolean hasAmbulances = _ambulances != null;
        final boolean hasHospitals = _hospitals != null;
        final boolean isRequestingLocationUpdates = isRequestingLocationUpdates();

        if (hasAmbulance) {

            final int ambulanceId = _ambulance.getId();
            final String ambulanceIdentifier = _ambulance.getIdentifier();

            // Remove current ambulance
            // TODO: Does it need to be asynchrounous?
            removeAmbulance();

            // Retrieve ambulance
            Intent ambulanceIntent = new Intent(this, AmbulanceForegroundService.class);
            ambulanceIntent.setAction(AmbulanceForegroundService.Actions.GET_AMBULANCE);
            ambulanceIntent.putExtra("AMBULANCE_ID", ambulanceId);

            // What to do when GET_AMBULANCE service completes?
            new OnServiceComplete(this,
                    AmbulanceForegroundService.BroadcastActions.SUCCESS,
                    AmbulanceForegroundService.BroadcastActions.FAILURE,
                    ambulanceIntent) {

                @Override
                public void onSuccess(Bundle extras) {

                    Log.i(TAG, String.format("Subscribed to ambulance %1$s", ambulanceIdentifier));

                    if (hasAmbulances) {

                        Log.i(TAG, "Subscribing to ambulances.");

                        // Remove ambulances
                        // TODO: Does it need to be asynchrounous?
                        removeAmbulances();

                        // Retrieve ambulance
                        Intent ambulanceIntent = new Intent(AmbulanceForegroundService.this,
                                AmbulanceForegroundService.class);
                        ambulanceIntent.setAction(AmbulanceForegroundService.Actions.GET_AMBULANCES);

                    }

                    if (hasHospitals) {

                        Log.i(TAG, "Subscribing to hospitals.");

                        // Remove hospitals
                        // TODO: Does it need to be asynchrounous?
                        removeHospitals();

                        // Retrieve hospital
                        Intent hospitalIntent = new Intent(AmbulanceForegroundService.this,
                                AmbulanceForegroundService.class);
                        hospitalIntent.setAction(AmbulanceForegroundService.Actions.GET_HOSPITALS);
                        startService(hospitalIntent);
                        
                    }

                    if (isRequestingLocationUpdates) {

                        // Process buffer
                        processBuffer();

                        // Register receiver
                        IntentFilter filter = new IntentFilter();
                        filter.addAction(AmbulanceForegroundService.BroadcastActions.AMBULANCE_UPDATE);
                        BroadcastReceiver receiver = new BroadcastReceiver() {

                            @Override
                            public void onReceive(Context context, Intent intent ) {
                                if (intent != null) {
                                    final String action = intent.getAction();
                                    if (action.equals(AmbulanceForegroundService.BroadcastActions.AMBULANCE_UPDATE)) {

                                        Log.i(TAG, "Restoring location updates.");

                                        // unregister self
                                        getLocalBroadcastManager().unregisterReceiver(this);

                                        // Make sure it can still stream location
                                        String clientId = getProfileClient(AmbulanceForegroundService.this).getClientId();
                                        Ambulance ambulance = getAmbulance();
                                        if (ambulance.getLocationClientId() == null) {
                                            ambulance.setLocationClientId(clientId);
                                        } else if (!ambulance.getLocationClientId().equals(clientId)) {
                                            // Some other client hijacked location streaming, abort
                                            // TODO: Check if slider is back at its position and warn user
                                            return;
                                        }

                                        // Start location updates
                                        Intent locationUpdatesIntent = new Intent(AmbulanceForegroundService.this,
                                                AmbulanceForegroundService.class);
                                        locationUpdatesIntent.setAction(AmbulanceForegroundService.Actions.START_LOCATION_UPDATES);
                                        startService(locationUpdatesIntent);

                                    }
                                }
                            }
                        };
                        getLocalBroadcastManager().registerReceiver(receiver, filter);


                    }

                }

            }
                    .setFailureMessage(getResources().getString(R.string.couldNotRetrieveAmbulance,
                            ambulanceIdentifier));

        }

    }

    /**
     * Callback after handling successful connection
     *
     * @param exception
     */
    @Override
    public void onFailure(Throwable exception) {

        Log.d(TAG, "onFailure: " + exception);

        // Notify user and return

        // Create an explicit intent for an Activity in your app
        Intent intent = new Intent(this, MainActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        PendingIntent pendingIntent = PendingIntent.getActivity(this, 0, intent, 0);

        NotificationCompat.Builder mBuilder = new NotificationCompat.Builder(this, PRIMARY_CHANNEL)
                .setSmallIcon(R.mipmap.ic_launcher)
                .setContentTitle(getString(R.string.EMSTrack))
                .setContentText(getString(R.string.serverIsOffline))
                .setPriority(NotificationCompat.PRIORITY_DEFAULT)
                .setContentIntent(pendingIntent)
                .setAutoCancel(true);

        NotificationManagerCompat notificationManager = NotificationManagerCompat.from(this);
        notificationManager.notify(WARNING_NOTIFICATION_ID, mBuilder.build());

        return;

    }

    /**
     * Login user
     *
     * @param username Username
     * @param password Password
     */
    public void login(final String username, final String password, final String uuid) {

        // logout first
        logout(uuid);

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

                // Broadcast success
                Intent localIntent = new Intent(BroadcastActions.SUCCESS);
                sendBroadcastWithUUID(localIntent, uuid);

                // set callback
                getProfileClient(AmbulanceForegroundService.this).setCallback(AmbulanceForegroundService.this);

            }

            @Override
            public void onFailure(Throwable exception) {

                Log.d(TAG, "Failed to retrieve profile.");

                // Build error message
                String message = exception.toString();

                // Broadcast failure
                Intent localIntent = new Intent(BroadcastActions.FAILURE);
                localIntent.putExtra(BroadcastExtras.MESSAGE, message);
                sendBroadcastWithUUID(localIntent, uuid);

            }

        });

        try {

            // Attempt to connect
            profileClient.connect(username, password, new MqttProfileCallback() {

                @Override
                public void onSuccess() {

                    Log.d(TAG, "Successfully connected to broker.");

                    // Do nothing. All work is done on the callback
                }

                @Override
                public void onFailure(Throwable exception) {

                    String message = getString(R.string.failedToConnectToBrocker) + "\n";

                    if (exception instanceof MqttException) {

                        int reason = ((MqttException) exception).getReasonCode();

                        if (reason == MqttException.REASON_CODE_FAILED_AUTHENTICATION ||
                                reason == MqttException.REASON_CODE_NOT_AUTHORIZED ||
                                reason == MqttException.REASON_CODE_INVALID_CLIENT_ID)

                            message += getResources().getString(R.string.error_invalid_credentials);

                        else

                            message += getResources().getString(R.string.error_connection_failed, exception.toString());

                    } else
                        message += getString(R.string.Exception) + exception.toString();

                    Log.d(TAG, "message = " + message);

                    // Broadcast failure
                    Intent localIntent = new Intent(BroadcastActions.FAILURE);
                    localIntent.putExtra(BroadcastExtras.MESSAGE, message);
                    sendBroadcastWithUUID(localIntent, uuid);

                }

            });

        } catch (MqttException exception) {

            // Build error message
            String message = getResources().getString(R.string.error_connection_failed, exception.toString());

            // Broadcast failure
            Intent localIntent = new Intent(BroadcastActions.FAILURE);
            localIntent.putExtra(BroadcastExtras.MESSAGE, message);
            sendBroadcastWithUUID(localIntent, uuid);

        }

    }

    /**
     * Retrieve ambulance
     *
     * @param ambulanceId the ambulance id
     */
    public void retrieveAmbulance(final int ambulanceId, final String uuid) {

        // Is ambulance id valid?
        if (ambulanceId < 0) {

            // Broadcast failure
            Intent localIntent = new Intent(BroadcastActions.FAILURE);
            localIntent.putExtra(BroadcastExtras.MESSAGE, getString(R.string.invalidAmbulanceId));
            sendBroadcastWithUUID(localIntent, uuid);

            return;
        }

        // Is ambulance new?
        Ambulance ambulance = getAmbulance();
        if (ambulance != null && ambulance.getId() == ambulanceId) {
            return;
        }

        // Remove current ambulance
        // TODO: Does it need to be asynchrounous?
        removeAmbulance();

        // Remove current ambulances
        // TODO: Does it need to be asynchrounous?
        removeAmbulances();

        // Retrieve client
        final MqttProfileClient profileClient = getProfileClient(this);

        try {

            // Publish ambulance login
            String topic = String.format("user/%1$s/client/%2$s/ambulance/%3$s/status",
                    profileClient.getUsername(), profileClient.getClientId(), ambulanceId);
            profileClient.publish(topic, "ambulance login",2,true);

        } catch (MqttException e) {

            Log.d(TAG, "Could not login to ambulance");

            // Broadcast failure
            Intent localIntent = new Intent(BroadcastActions.FAILURE);
            localIntent.putExtra(BroadcastExtras.MESSAGE, getString(R.string.couldNotSubscribeToAmbulance));
            sendBroadcastWithUUID(localIntent, uuid);

        }

        try {

            // Start retrieving data
            profileClient.subscribe("ambulance/" + ambulanceId + "/data",
                    1, new MqttProfileMessageCallback() {

                        @Override
                        public void messageArrived(String topic, MqttMessage message) {

                            // Keep subscription to ambulance to make sure we receive
                            // the latest updates.

                            Log.d(TAG, "Retrieving ambulance.");

                            // First time?
                            boolean firstTime = (_ambulance == null);

                            // parse ambulance data
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
                                if (_lastLocation == null || ambulance.getTimestamp().after(_lastLocation.getTimestamp())) {
                                    // Update last location
                                    _lastLocation = new LocationUpdate();
                                    android.location.Location location = new android.location.Location("FusedLocationClient");
                                    location.setLatitude(ambulance.getLocation().getLatitude());
                                    location.setLongitude(ambulance.getLocation().getLongitude());
                                    _lastLocation.setLocation(location);
                                    _lastLocation.setBearing(ambulance.getOrientation());
                                    _lastLocation.setTimestamp(ambulance.getTimestamp());
                                }

                                // Set current ambulance
                                _ambulance = ambulance;

                                if (firstTime) {

                                    // Broadcast success
                                    Intent localIntent = new Intent(BroadcastActions.SUCCESS);
                                    sendBroadcastWithUUID(localIntent, uuid);

                                } else {

                                    // Broadcast ambulance update
                                    // Don't use uuid so that continuous receiver can capture
                                    Intent localIntent = new Intent(BroadcastActions.AMBULANCE_UPDATE);
                                    sendBroadcastWithUUID(localIntent);

                                }

                            } catch (Exception e) {

                                Log.i(TAG, "Could not parse ambulance update.");

                                // Broadcast failure
                                Intent localIntent = new Intent(BroadcastActions.FAILURE);
                                localIntent.putExtra(BroadcastExtras.MESSAGE, getString(R.string.couldNotParseAmbulance));
                                if (firstTime)
                                    sendBroadcastWithUUID(localIntent, uuid);
                                else
                                    sendBroadcastWithUUID(localIntent);

                            }

                        }

                    });

        } catch (MqttException e) {

            Log.d(TAG, "Could not subscribe to ambulance data");

            // Broadcast failure
            Intent localIntent = new Intent(BroadcastActions.FAILURE);
            localIntent.putExtra(BroadcastExtras.MESSAGE, getString(R.string.couldNotSubscribeToAmbulance));
            sendBroadcastWithUUID(localIntent, uuid);

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

        // Remove current ambulance first
        _ambulance = null;

        // get ambulance id
        int ambulanceId = ambulance.getId();

        // remove location updates
        stopLocationUpdates();

        // Retrieve client
        final MqttProfileClient profileClient = getProfileClient(this);

        try {

            // Publish ambulance logout
            String topic = String.format("user/%1$s/client/%2$s/ambulance/%3$s/status",
                    profileClient.getUsername(), profileClient.getClientId(), ambulanceId);
            profileClient.publish(topic, "ambulance logout",2,true);

        } catch (MqttException e) {
            Log.d(TAG, "Could not logout from ambulance");
        }

        try {

            // Unsubscribe to ambulance data
            profileClient.unsubscribe("ambulance/" + ambulanceId + "/data");

        } catch (MqttException exception) {
            Log.d(TAG, "Could not unsubscribe to 'ambulance/" + ambulanceId + "/data'");
        }
        

    }

    /**
     * Retrieve hospitals
     */
    public void retrieveHospitals(final String uuid) {

        // Remove current hospital map
        // TODO: Does it need to be asynchrounous?
        removeHospitals();

        // Retrieve hospital data
        final MqttProfileClient profileClient = getProfileClient(this);

        // Get list of hospitals
        final List<HospitalPermission> hospitalPermissions = profileClient.getProfile().getHospitals();

        // Initialize hospitals
        final Map<Integer, Hospital> hospitals = new HashMap<>();

        // Loop over all hospitals
        for (HospitalPermission hospitalPermission : hospitalPermissions) {

            final int hospitalId = hospitalPermission.getHospitalId();

            try {

                // Start retrieving data
                profileClient.subscribe("hospital/" + hospitalId + "/data",
                        1, new MqttProfileMessageCallback() {

                            @Override
                            public void messageArrived(String topic, MqttMessage message) {

                                // first time?
                                boolean firstTime = (_hospitals == null);

                                // Parse hospital
                                GsonBuilder gsonBuilder = new GsonBuilder();
                                gsonBuilder.setFieldNamingPolicy(FieldNamingPolicy.LOWER_CASE_WITH_UNDERSCORES);
                                Gson gson = gsonBuilder.create();

                                // Found hospital
                                final Hospital hospital = gson.fromJson(message.toString(), Hospital.class);

                                if (firstTime) {

                                    // Add to hospital map
                                    hospitals.put(hospitalId, hospital);

                                    // Done yet?
                                    if (hospitals.size() == hospitalPermissions.size()) {

                                        Log.d(TAG, "Done retrieving all hospitals");

                                        // set _hospitals
                                        _hospitals = hospitals;

                                        // Broadcast hospitals update
                                        Intent localIntent = new Intent(BroadcastActions.SUCCESS);
                                        sendBroadcastWithUUID(localIntent, uuid);

                                    }

                                } else {

                                    // Modify hospital map
                                    hospitals.put(hospitalId, hospital);

                                    // Broadcast hospitals update
                                    Intent localIntent = new Intent(BroadcastActions.HOSPITALS_UPDATE);
                                    sendBroadcastWithUUID(localIntent);

                                }
                            }
                        });

            } catch (MqttException e) {
                Log.d(TAG, "Could not subscribe to hospital data");

                // Broadcast failure
                Intent localIntent = new Intent(BroadcastActions.FAILURE);
                localIntent.putExtra(BroadcastExtras.MESSAGE, getString(R.string.couldNotSubscribeToHospital));
                sendBroadcastWithUUID(localIntent, uuid);

            }

        }

    }

    /**
     * Remove current hospitals
     */
    public void removeHospitals() {

        Map<Integer, Hospital> hospitals = getHospitals();
        if (hospitals == null || hospitals.size() == 0) {
            Log.i(TAG, "No hospital to remove.");
            return;
        }

        // Retrieve client
        final MqttProfileClient profileClient = getProfileClient(this);

        // Loop over all hospitals
        for (Map.Entry<Integer, Hospital> entry : hospitals.entrySet()) {

            // Get hospital
            Hospital hospital = entry.getValue();

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

    /**
     * Retrieve ambulances
     */
    public void retrieveAmbulances(final String uuid) {

        // Remove current ambulance map
        // TODO: Does it need to be asynchrounous?
        removeAmbulances();

        // Retrieve ambulance data
        final MqttProfileClient profileClient = getProfileClient(this);

        // Get list of ambulances
        final List<AmbulancePermission> ambulancePermissions = profileClient.getProfile().getAmbulances();

        // Initialize ambulances
        final Map<Integer, Ambulance> ambulances = new HashMap<>();

        // Current ambulance id
        int currentAmbulanceId = getAmbulanceId();

        // Loop over all ambulances
        for (AmbulancePermission ambulancePermission : ambulancePermissions) {

            final int ambulanceId = ambulancePermission.getAmbulanceId();

            // Skip if current ambulance
            if (ambulanceId == currentAmbulanceId)
                continue;

            try {

                // Start retrieving data
                profileClient.subscribe("ambulance/" + ambulanceId + "/data",
                        1, new MqttProfileMessageCallback() {

                            @Override
                            public void messageArrived(String topic, MqttMessage message) {

                                // first time?
                                boolean firstTime = (_ambulances == null);

                                // Parse ambulance
                                GsonBuilder gsonBuilder = new GsonBuilder();
                                gsonBuilder.setFieldNamingPolicy(FieldNamingPolicy.LOWER_CASE_WITH_UNDERSCORES);
                                Gson gson = gsonBuilder.create();

                                // Found ambulance
                                final Ambulance ambulance = gson.fromJson(message.toString(), Ambulance.class);

                                if (firstTime) {

                                    // Add to ambulance map
                                    ambulances.put(ambulanceId, ambulance);

                                    // Done yet?
                                    if (ambulances.size() ==
                                            (ambulancePermissions.size() + (getAmbulanceId() == -1 ? 0 : - 1))) {

                                        Log.d(TAG, "Done retrieving all ambulances");

                                        // set _ambulances
                                        _ambulances = ambulances;

                                        // Broadcast ambulances update
                                        Intent localIntent = new Intent(BroadcastActions.SUCCESS);
                                        sendBroadcastWithUUID(localIntent, uuid);

                                    }

                                } else {

                                    // Update ambulance map
                                    ambulances.put(ambulanceId, ambulance);

                                    // Broadcast ambulances update
                                    Intent localIntent = new Intent(BroadcastActions.AMBULANCES_UPDATE);
                                    sendBroadcastWithUUID(localIntent);

                                }
                            }
                        });

            } catch (MqttException e) {
                Log.d(TAG, "Could not subscribe to ambulance data");

                // Broadcast failure
                Intent localIntent = new Intent(BroadcastActions.FAILURE);
                localIntent.putExtra(BroadcastExtras.MESSAGE, getString(R.string.couldNotSubscribeToAmbulance));
                sendBroadcastWithUUID(localIntent, uuid);

            }

        }

    }

    /**
     * Remove current ambulances
     */
    public void removeAmbulances() {

        Map<Integer, Ambulance> ambulances = getAmbulances();
        if (ambulances == null || ambulances.size() == 0) {
            Log.i(TAG, "No ambulances to remove.");
            return;
        }

        // Retrieve client
        final MqttProfileClient profileClient = getProfileClient(this);

        // Loop over all ambulances
        for (Map.Entry<Integer, Ambulance> entry : ambulances.entrySet()) {

            // Get ambulance
            Ambulance ambulance = entry.getValue();

            try {

                // Unsubscribe to ambulance data
                profileClient.unsubscribe("ambulance/" + ambulance.getId() + "/data");

            } catch (MqttException exception) {
                Log.d(TAG, "Could not unsubscribe to 'ambulance/" + ambulance.getId() + "/data'");
            }

        }

        // Remove ambulances
        _ambulances = null;

    }

    /**
     * Stop subscribing to ambulances
     */
    public void stopAmbulances(final String uuid) {

        // Remove current ambulance map
        // TODO: Does it need to be asynchrounous?
        removeAmbulances();

        // Broadcast success
        Intent localIntent = new Intent(BroadcastActions.SUCCESS);
        sendBroadcastWithUUID(localIntent, uuid);

    }

    private void requestLocationUpdates(String uuid) {

        // Logged in?
        if (getAmbulance() == null) {
            Log.i(TAG, "No ambulance selected.");
            return;
        }

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
                        beginLocationUpdates();
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
     * Handles the Request Updates button and requests start of location updates.
     */
    public void beginLocationUpdates() {

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
    public void stopLocationUpdates() {

        _lastLocation = null;

        // Already started?
        if (!requestingLocationUpdates) {
            Log.i(TAG, "Not requesting location updates. Skipping.");
            return;
        }

        // get profile client
        final MqttProfileClient profileClient = AmbulanceForegroundService.getProfileClient(this);

        // clear location_client on server?
        Ambulance ambulance = getAmbulance();
        if (ambulance != null && profileClient != null &&
                profileClient.getClientId().equals(ambulance.getLocationClientId())) {

            Log.i(TAG, "Will clear location client on server");

            // clear location_client on server
            String payload = "{\"location_client_id\":\"\"}";

            // Update location_client on server, listening to updates already
            Intent intent = new Intent(this, AmbulanceForegroundService.class);
            intent.setAction(AmbulanceForegroundService.Actions.UPDATE_AMBULANCE);
            Bundle bundle = new Bundle();
            bundle.putString("UPDATE", payload);
            intent.putExtras(bundle);
            startService(intent);

        } else {

            Log.i(TAG, "No need to clear location client on server");

        }

        // remove on fusedLocationclient
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
     * Get LocalBroadcastManager
     *
     * @return the LocalBroadcastManager
     */
    private LocalBroadcastManager getLocalBroadcastManager() {
        return LocalBroadcastManager.getInstance(this);
    }

}
