package org.emstrack.ambulance.services;

import android.annotation.SuppressLint;
import android.app.AlertDialog;
import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
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
import com.google.android.gms.iid.InstanceID;
import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.GeofencingClient;
import com.google.android.gms.location.GeofencingRequest;
import com.google.android.gms.location.LocationCallback;
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
import org.emstrack.ambulance.LoginActivity;
import org.emstrack.ambulance.MainActivity;
import org.emstrack.ambulance.R;
import org.emstrack.ambulance.fragments.AmbulanceFragment;
import org.emstrack.ambulance.util.Geofence;
import org.emstrack.ambulance.util.LocationFilter;
import org.emstrack.ambulance.util.LocationUpdate;
import org.emstrack.models.Ambulance;
import org.emstrack.models.AmbulancePermission;
import org.emstrack.models.Call;
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
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.TimeZone;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicInteger;

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
    public static final String PRIMARY_CHANNEL = "default";
    public static final String PRIMARY_CHANNEL_LABEL = "Default channel";

    // Notification id
    private final static AtomicInteger notificationId = new AtomicInteger(NOTIFICATION_ID + 1);

    // SharedPreferences
    public static final String PREFERENCES_NAME = "org.emstrack.ambulance";
    public static final String PREFERENCES_USERNAME = "USERNAME";
    public static final String PREFERENCES_PASSWORD = "PASSWORD";

    private static final String serverUri = "ssl://cruzroja.ucsd.edu:8883";

    private static MqttProfileClient client;
    private static Ambulance _ambulance;
    private static Map<Integer, Hospital> _hospitals;
    private static Map<Integer, Ambulance> _otherAmbulances;
    private static LocationUpdate _lastLocation;
    private static Date _lastServerUpdate;
    private static boolean _updatingLocation = false;
    private static boolean _canUpdateLocation = false;
    private static ArrayList<String> _updateBuffer = new ArrayList<>();
    private static boolean _reconnecting = false;
    private static boolean _online = false;
    private static ReconnectionInformation _reconnectionInformation;

    // Geofences
    private final static AtomicInteger geofencesId = new AtomicInteger(1);
    private static Map<String, Geofence> _geofences;

    private static LocationSettingsRequest locationSettingsRequest;
    private static LocationRequest locationRequest;

    private NotificationManager notificationManager;

    private FusedLocationProviderClient fusedLocationClient;
    private SharedPreferences sharedPreferences;

    public LocationFilter locationFilter = new LocationFilter(null);

    private LocationCallback locationCallback;
    private GeofencingClient fenceClient;
    private PendingIntent geofenceIntent;

    public class Actions {
        public final static String START_SERVICE = "org.emstrack.ambulance.ambulanceforegroundservice.action.START_SERVICE";
        public final static String LOGIN = "org.emstrack.ambulance.ambulanceforegroundservice.action.LOGIN";
        public final static String GET_AMBULANCE = "org.emstrack.ambulance.ambulanceforegroundservice.action.GET_AMBULANCE";
        public final static String GET_AMBULANCES= "org.emstrack.ambulance.ambulanceforegroundservice.action.GET_AMBULANCES";
        public final static String STOP_AMBULANCES= "org.emstrack.ambulance.ambulanceforegroundservice.action.STOP_AMBULANCES";
        public final static String GET_HOSPITALS = "org.emstrack.ambulance.ambulanceforegroundservice.action.GET_HOSPITALS";
        public final static String START_LOCATION_UPDATES = "org.emstrack.ambulance.ambulanceforegroundservice.action.START_LOCATION_UPDATES";
        public final static String STOP_LOCATION_UPDATES = "org.emstrack.ambulance.ambulanceforegroundservice.action.STOP_LOCATION_UPDATES";
        public final static String UPDATE_AMBULANCE = "org.emstrack.ambulance.ambulanceforegroundservice.action.UPDATE_AMBULANCE";
        public final static String UPDATE_NOTIFICATION = "org.emstrack.ambulance.ambulanceforegroundservice.action.UPDATE_NOTIFICATION";
        public final static String LOGOUT = "org.emstrack.ambulance.ambulanceforegroundservice.action.LOGOUT";
        public final static String GEOFENCE_START = "org.emstrack.ambulance.ambulanceforegroundservice.action.GEOFENCE_START";
        public final static String GEOFENCE_STOP = "org.emstrack.ambulance.ambulanceforegroundservice.action.GEOFENCE_STOP";
        public final static String STOP_SERVICE = "org.emstrack.ambulance.ambulanceforegroundservice.action.STOP_SERVICE";
        public final static String CALL_ACCEPTED = "org.emstrack.ambulance.ambulanceforegroundservice.action.CALL_ACCEPTED";
        public final static String CALL_ONGOING = "org.emstrack.ambulance.ambulanceforegroundservice.action.CALL_ONGOING";
    }

    public class BroadcastExtras {
        public final static String MESSAGE = "org.emstrack.ambulance.ambulanceforegroundservice.broadcastextras.MESSAGE";
        public final static String GEOFENCE_TRANSITION = "org.emstrack.ambulance.ambulanceforegroundservice.broadcastextras.GEOFENCE_TRANSTION";
    }

    public class BroadcastActions {
        public final static String HOSPITALS_UPDATE = "org.emstrack.ambulance.ambulanceforegroundservice.broadcastaction.HOSPITALS_UPDATE";
        public static final String OTHER_AMBULANCES_UPDATE = "org.emstrack.ambulance.ambulanceforegroundservice.broadcastaction.OTHER_AMBULANCES_UPDATE";
        public final static String AMBULANCE_UPDATE = "org.emstrack.ambulance.ambulanceforegroundservice.broadcastaction.AMBULANCE_UPDATE";
        public final static String LOCATION_UPDATE_CHANGE = "org.emstrack.ambulance.ambulanceforegroundservice.broadcastaction.LOCATION_UPDATE_CHANGE";
        public final static String GEOFENCE_EVENT = "org.emstrack.ambulance.ambulanceforegroundservice.broadcastaction.GEOFENCE_EVENT";
        public final static String CONNECTIVITY_CHANGE = "org.emstrack.ambulance.ambulanceforegroundservice.broadcastaction.CONNECTIVITY_CHANGE";
        public final static String SUCCESS = "org.emstrack.ambulance.ambulanceforegroundservice.broadcastaction.SUCCESS";
        public final static String FAILURE = "org.emstrack.ambulance.ambulanceforegroundservice.broadcastaction.FAILURE";
        public final static String PROMPT_CALL = "org.emstrack.ambulance.ambulanceforegroundservice.broadcastaction.PROMPT_CALL";
    }

    public static class ProfileClientException extends Exception {

    }

    public class ReconnectionInformation {

        private boolean hasAmbulance;
        private boolean hasOtherAmbulances;
        private boolean hasHospitals;
        private boolean isUpdatingLocation;

        public ReconnectionInformation(boolean hasAmbulance, boolean hasOtherAmbulances,
                                       boolean hasHospitals, boolean isUpdatingLocation) {

            this.hasAmbulance = hasAmbulance;
            this.hasOtherAmbulances = hasOtherAmbulances;
            this.hasHospitals = hasHospitals;
            this.isUpdatingLocation = isUpdatingLocation;

        }

        public boolean hasAmbulance() { return hasAmbulance; }
        public boolean hasOtherAmbulances() { return hasOtherAmbulances; }
        public boolean hasHospitals() { return hasHospitals; }
        public boolean isUpdatingLocation() { return isUpdatingLocation; }

        @Override
        public String toString() {
            return String.format("hasAmbulance = %1$b, hasOtherAmbulances = %2$b, hasHospitals = %3$b, isUpdatingLocation = %4$b",
                    hasAmbulance, hasOtherAmbulances, hasHospitals, isUpdatingLocation);
        }

    }

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

        // Initialize geofence map
        _geofences = new HashMap<String, Geofence>();

        // Initialize geofence client
        fenceClient = LocationServices.getGeofencingClient(this);

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

        if (intent.getAction().equals(Actions.START_SERVICE)) {

            Log.i(TAG, "START_SERVICE Foreground Intent ");

            final boolean addStopAction = intent.getBooleanExtra("ADD_STOP_ACTION", false);

            // Make sure client is bound to service
            AmbulanceForegroundService.getProfileClient(this);

            // Create notification

            // Login intent
            Intent notificationIntent = new Intent(AmbulanceForegroundService.this, LoginActivity.class);
            notificationIntent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
            PendingIntent pendingIntent = PendingIntent.getActivity(AmbulanceForegroundService.this, 0,
                    notificationIntent, 0);

            // Restart intent
            Intent restartServiceIntent = new Intent(AmbulanceForegroundService.this, LoginActivity.class);
            restartServiceIntent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
            restartServiceIntent.setAction(LoginActivity.LOGOUT);
            PendingIntent restartServicePendingIntent = PendingIntent.getActivity(AmbulanceForegroundService.this, 0,
                    restartServiceIntent, 0);

            // Stop intent
            Intent stopServiceIntent = new Intent(AmbulanceForegroundService.this, AmbulanceForegroundService.class);
            stopServiceIntent.setAction(Actions.STOP_SERVICE);
            PendingIntent stopServicePendingIntent = PendingIntent.getService(AmbulanceForegroundService.this, 0,
                    stopServiceIntent, 0);
            
            // Icon
            Bitmap icon = BitmapFactory.decodeResource(getResources(),
                    R.mipmap.ic_launcher);

            NotificationCompat.Builder notificationBuilder;
            if (Build.VERSION.SDK_INT >= 26)
                notificationBuilder = new NotificationCompat.Builder(AmbulanceForegroundService.this,
                        AmbulanceForegroundService.PRIMARY_CHANNEL);
            else
                notificationBuilder = new NotificationCompat.Builder(AmbulanceForegroundService.this);

            notificationBuilder
                    .setContentTitle("EMSTrack")
                    .setTicker(getString(R.string.pleaseLogin))
                    .setContentText(getString(R.string.pleaseLogin))
                    .setSmallIcon(R.mipmap.ic_launcher)
                    .setLargeIcon(Bitmap.createScaledBitmap(icon, 128, 128, false))
                    .setContentIntent(pendingIntent)
                    .setOngoing(true)
                    .addAction(android.R.drawable.ic_menu_close_clear_cancel, getString(R.string.restartText), restartServicePendingIntent);

            if (addStopAction)
                notificationBuilder.addAction(android.R.drawable.ic_menu_close_clear_cancel, getString(R.string.stopText), stopServicePendingIntent);

            Notification notification = notificationBuilder.build();

            // start service
            startForeground(NOTIFICATION_ID, notification);

            // Broadcast success
            Intent localIntent = new Intent(BroadcastActions.SUCCESS);
            sendBroadcastWithUUID(localIntent, uuid);

        } else if (intent.getAction().equals(Actions.STOP_SERVICE)) {

            Log.i(TAG, "STOP_SERVICE Foreground Intent");

            // What to do when logout completes?
            new OnServiceComplete(this,
                    AmbulanceForegroundService.BroadcastActions.SUCCESS,
                    AmbulanceForegroundService.BroadcastActions.FAILURE,
                    null) {

                public void run() {

                    // logout
                    logout(getUuid());

                }

                @Override
                public void onSuccess(Bundle extras) {

                    Log.d(TAG, "STOP_SERVICE::onSuccess.");

                    // Set online false
                    setOnline(false);

                    // close client
                    client.close();

                    // set client to null
                    client = null;

                    // stop service
                    stopForeground(true);
                    stopSelf();

                    // Broadcast success
                    Intent localIntent = new Intent(BroadcastActions.SUCCESS);
                    sendBroadcastWithUUID(localIntent, uuid);

                }

                @Override
                public void onFailure(Bundle extras) {

                    Log.d(TAG, "STOP_SERVICE::onFailure.");

                    // Broadcast failure
                    Intent localIntent = new Intent(BroadcastActions.FAILURE);
                    if (extras != null)
                        localIntent.putExtras(extras);
                    sendBroadcastWithUUID(localIntent, uuid);

                }

            };


        } else if (intent.getAction().equals(Actions.LOGIN)) {

            Log.i(TAG, "LOGIN Foreground Intent ");

            // Retrieve username and password
            String[] loginInfo = intent.getStringArrayExtra("CREDENTIALS");
            final String username = loginInfo[0];
            final String password = loginInfo[1];

            // Notify user
            Toast.makeText(this, "Logging in '" + username + "'", Toast.LENGTH_SHORT).show();

            // Set online false
            setOnline(false);

            // What to do when login completes?
            new OnServiceComplete(this,
                    AmbulanceForegroundService.BroadcastActions.SUCCESS,
                    AmbulanceForegroundService.BroadcastActions.FAILURE,
                    null) {

                public void run() {

                    // Login user
                    login(username, password, getUuid());

                }

                @Override
                public void onSuccess(Bundle extras) {

                    // Set online true
                    setOnline(true);

                    // Update notification
                    updateNotification(getString(R.string.welcomeUser, username));

                    // Broadcast success
                    Intent localIntent = new Intent(BroadcastActions.SUCCESS);
                    if (extras != null)
                        localIntent.putExtras(extras);
                    sendBroadcastWithUUID(localIntent, uuid);

                }

                @Override
                public void onFailure(Bundle extras) {

                    // Set online false
                    setOnline(false);

                    // Create notification
                    NotificationCompat.Builder mBuilder = new NotificationCompat.Builder(AmbulanceForegroundService.this, PRIMARY_CHANNEL)
                            .setSmallIcon(R.mipmap.ic_launcher)
                            .setContentTitle("EMSTrack")
                            .setContentText(getString(R.string.couldNotLogin, username))
                            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
                            .setAutoCancel(true);

                    NotificationManagerCompat notificationManager = NotificationManagerCompat.from(AmbulanceForegroundService.this);
                    notificationManager.notify(notificationId.getAndIncrement(), mBuilder.build());

                    // Broadcast failure
                    Intent localIntent = new Intent(BroadcastActions.FAILURE);
                    if (extras != null)
                        localIntent.putExtras(extras);
                    sendBroadcastWithUUID(localIntent, uuid);

                }

            };

        } else if (intent.getAction().equals(Actions.LOGOUT)) {

            Log.i(TAG, "LOGOUT Foreground Intent");

            // Set online false
            setOnline(false);

            // logout
            logout(uuid);

        } else if (intent.getAction().equals(Actions.GET_AMBULANCE)) {

            Log.i(TAG, "GET_AMBULANCE Foreground Intent");

            // Retrieve ambulance
            int ambulanceId = intent.getIntExtra("AMBULANCE_ID", -1);
            boolean reconnect = intent.getBooleanExtra("RECONNECT", false);
            retrieveAmbulance(ambulanceId, uuid, reconnect);

        } else if (intent.getAction().equals(Actions.GET_AMBULANCES)) {

            Log.i(TAG, "GET_AMBULANCES Foreground Intent");

            // Retrieve ambulances
            boolean reconnect = intent.getBooleanExtra("RECONNECT", false);
            retrieveOtherAmbulances(uuid, reconnect);

        } else if (intent.getAction().equals(Actions.STOP_AMBULANCES)) {

            Log.i(TAG, "STOP_AMBULANCES Foreground Intent");

            // Stop ambulances
            stopAmbulances(uuid);

        } else if (intent.getAction().equals(Actions.GET_HOSPITALS)) {

            Log.i(TAG, "GET_HOSPITALS Foreground Intent");

            // Retrieve hospitals
            boolean reconnect = intent.getBooleanExtra("RECONNECT", false);
            retrieveHospitals(uuid, reconnect);

        } else if (intent.getAction().equals(Actions.START_LOCATION_UPDATES)) {

            Log.i(TAG, "START_LOCATION_UPDATES Foreground Intent");

            boolean reconnect = intent.getBooleanExtra("RECONNECT", false);
            startLocationUpdates(uuid, reconnect);

        } else if (intent.getAction().equals(Actions.STOP_LOCATION_UPDATES)) {

            Log.i(TAG, "STOP_LOCATION_UPDATES Foreground Intent");

            // stop requesting location updates
            stopLocationUpdates(uuid);

        } else if (intent.getAction().equals(Actions.UPDATE_AMBULANCE)) {

            Log.i(TAG, "UPDATE_AMBULANCE Foreground Intent");

            Bundle bundle = intent.getExtras();

            // Retrieve update string
            String update = bundle.getString("UPDATE");
            if (update != null) {

                // update mqtt server
                updateAmbulance(update);

            }

            // Retrieve update string array
            ArrayList<String> updateArray = bundle.getStringArrayList("UPDATES");
            if (updateArray != null) {

                // update mqtt server
                updateAmbulance(updateArray);

            }

        } else if (intent.getAction().equals(Actions.UPDATE_NOTIFICATION)) {

            Log.i(TAG, "UPDATE_NOTIFICATION Foreground Intent");

            // Retrieve update string
            String message = intent.getStringExtra("MESSAGE");
            if (message != null)
                updateNotification(message);

        } else if (intent.getAction().equals(Actions.GEOFENCE_START)) {

            Log.i(TAG, "GEOFENCE_START Foreground Intent");

            // Retrieve latitude and longitude
            // TODO: consider how this works with multiple geofences
            String username = intent.getStringExtra("USERNAME");
            String clientId = intent.getStringExtra("CLIENTID");
            String callId = intent.getStringExtra("CALLID");
            Float latitude = intent.getFloatExtra("LATITUDE", 0.f);
            Float longitude = intent.getFloatExtra("LONGITUDE", 0.f);
            Float radius = intent.getFloatExtra("RADIUS", 5.f);
            // CHANGED:
            startGeofence(uuid, new Geofence(new Location(latitude, longitude), radius), callId);

        } else if (intent.getAction().equals(Actions.GEOFENCE_STOP)) {

            Log.i(TAG, "GEOFENCE_STOP Foreground Intent");

            // Retrieve request ids
            // TODO: consider how this works with mutliple geofences
            // TODO: find somewhere to create intent to remove
            String requestId = intent.getStringExtra("REQUESTID");
            List<String> requestIds = new ArrayList<String>();
            requestIds.add(requestId);

            stopGeofence(uuid, requestIds);

        } else if (intent.getAction().equals(Actions.CALL_ACCEPTED)) {

            Log.i(TAG, "CALL_ACCEPTED Foreground Intent");

            // get the ambulance that accepted the call and the call id
            String callId = intent.getStringExtra("callId");

            // next steps to publish information to server (steps 3, 4)
            replyToAcceptCall(callId, uuid);

        } else if (intent.getAction().equals(Actions.CALL_ONGOING)) {

            Log.i(TAG, "CALL_ONGOING Foreground Intent");

            String callId = intent.getStringExtra("callId");

            replyToOngoingCall(callId, uuid);

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
    protected static MqttProfileClient getProfileClient(Context context) {

        // lazy initialization
        if (client == null) {

            // TODO: This is deprecated. New Firebase service is buggy and poorly documented.
            String clientId = context.getString(R.string.app_version)
                    + context.getString(R.string.client_name)
                    + InstanceID.getInstance(context).getId();
                    //+ UUID.randomUUID().toString();
            MqttAndroidClient androidClient = new MqttAndroidClient(context, serverUri, clientId);
            client = new MqttProfileClient(androidClient);

        }

        return client;
    }

    public static MqttProfileClient getProfileClient() throws ProfileClientException {

        // lazy initialization
        if (client != null)
            return client;

        // otherwise log and throw exception
        Log.e(TAG,"Failed to get profile client.");
        throw new ProfileClientException();

    }

    public static boolean hasProfileClient() { return client != null; }

    /**
     * Get current ambulance
     *
     * @return the ambulance
     */
    public static Ambulance getAmbulance() {
        return _ambulance;
    }

    /**
     * Convenience method to get current ambulance id.
     * Returns -1 if one does not exist.
     *
     * @return
     */
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
    public static Map<Integer, Ambulance> getOtherAmbulances() { return _otherAmbulances; }

    /**
     * Return true if online
     *
     * @return true if online
     */
    public static boolean isOnline() { return _online; }

    /**
     * Set online status
     *
     * @return true if online
     */
    public void setOnline(boolean online) { setOnline(online, this); }

    /**
     * Set online status
     *
     * @return true if online
     */
    public static void setOnline(boolean online, Context context) {

        if (context != null && online != _online) {

            // broadcast change of connectivity status
            Intent localIntent = new Intent(BroadcastActions.CONNECTIVITY_CHANGE);
            localIntent.putExtra("ONLINE", online);
            getLocalBroadcastManager(context).sendBroadcast(localIntent);

        }

        _online = online;

    }

    /**
     * Return true if reconnecting
     *
     * @return true if reconnecting
     */
    public static boolean isReconnecting() { return _reconnecting; }

    /**
     * Return true if requesting location updates
     *
     * @return the location updates status
     */
    public static boolean isUpdatingLocation() { return _updatingLocation; }

    /**
     * Return can update location
     *
     * @return the location update status
     */
    public static boolean canUpdateLocation() { return _canUpdateLocation; }

    /**
     * Set can update location status
     *
     * @param canUpdateLocation the location update status
     */
    public static void setCanUpdateLocation(boolean canUpdateLocation) { AmbulanceForegroundService._canUpdateLocation = canUpdateLocation; }

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
     * Add update to buffer for later processing
     *
     * @param update
     */
    public void addToBuffer(String update) {

        // buffer updates and return
        // TODO: limit size of buffer or write to disk
        _updateBuffer.add(update);

        // Log
        Log.d(TAG, "MQTT Client is not online. Buffering update.");

    }

    public boolean consumeBuffer() {

        // fast return
        if (_updateBuffer.size() == 0)
            return true;

        // Get client
        final MqttProfileClient profileClient = getProfileClient(AmbulanceForegroundService.this);

        // Log and add notification
        Log.d(TAG, String.format("Attempting to consume update buffer with %1$d entries.", _updateBuffer.size()));

        // Loop through buffer unless it failed
        Iterator<String> iterator = _updateBuffer.iterator();
        boolean success = true;
        while (success && iterator.hasNext()) {

            // Retrieve update and remove from buffer
            String update = iterator.next();
            iterator.remove();

            // update ambulance
            success = updateAmbulance(update);

        }

        return success;

    }

    /**
     * Send bulk updates to current ambulance
     * Allowing arbitrary updates might be too broad and a security concern
     *
     * @param updates string array
     */
    public boolean updateAmbulance(List<LocationUpdate> updates) {

        ArrayList<String> updateString = new ArrayList<>();
        for (LocationUpdate update : updates) {

            // Set last location
            _lastLocation = update;

            // update ambulance string
            updateString.add(getUpdateString(update));

        }

        boolean success = updateAmbulance(updateString);
        if (!success) {

            // update locally as well
            Ambulance ambulance = getAmbulance();
            if (ambulance != null) {

                // Update ambulance
                android.location.Location location = _lastLocation.getLocation();
                ambulance.setLocation(new Location(location.getLatitude(), location.getLongitude()));
                ambulance.setOrientation(_lastLocation.getBearing());

                // Broadcast ambulance update
                Intent localIntent = new Intent(BroadcastActions.AMBULANCE_UPDATE);
                sendBroadcastWithUUID(localIntent);

            }

        }

        return success;
    }

    /**
     * Send bulk updates to current ambulance
     * Allowing arbitrary updates might be too broad and a security concern
     *
     * @param updates string array
     */
    public boolean updateAmbulance(ArrayList<String> updates) {

        // Join updates in array
        String updateArray = "[" + TextUtils.join(",", updates) + "]";

        // send to server
        return updateAmbulance(updateArray);
    }

    /**
     * Send update to current ambulance
     * Allowing arbitrary updates might be too broad and a security concern
     *
     * @param update string
     */
    public boolean updateAmbulance(String update) {

        // Error message
        String message = getString(R.string.couldNotUpdateAmbulanceOnServer);

        try {

            // is online or connected?
            final MqttProfileClient profileClient = getProfileClient(AmbulanceForegroundService.this);
            if (!isOnline() || !profileClient.isConnected()) {

                // add to buffer and return
                addToBuffer(update);
                return false;

            }

            // Has ambulance?
            Ambulance ambulance = getAmbulance();
            if (ambulance != null ) {

                // Publish current update to MQTT
                profileClient.publish(String.format("user/%1$s/client/%2$s/ambulance/%3$d/data",
                        profileClient.getUsername(), profileClient.getClientId(), ambulance.getId()),
                        update, 1, false);

                // Set update time
                _lastServerUpdate = new Date();

                return true;

            } else {

                message += "\n" + "Could not find ambulance";

            }

        }
        catch (MqttException e) { message += "\n" + e.toString(); }
        catch (Exception e) { message += "\n" + e.toString(); }

        // Log and build a notification in case of error
        Log.i(TAG,message);

        // Create notification
        NotificationCompat.Builder mBuilder = new NotificationCompat.Builder(this, PRIMARY_CHANNEL)
                .setSmallIcon(R.mipmap.ic_launcher)
                .setContentTitle("EMSTrack")
                .setContentText(message)
                .setPriority(NotificationCompat.PRIORITY_DEFAULT)
                .setAutoCancel(true);

        NotificationManagerCompat notificationManager = NotificationManagerCompat.from(this);
        notificationManager.notify(notificationId.getAndIncrement(), mBuilder.build());

        // and return false
        return false;

    }

    /**
     * Update current notification message
     *
     * @param message the message
     */
    public void updateNotification(String message) {

        // Create notification

        // Login intent
        Intent notificationIntent = new Intent(AmbulanceForegroundService.this, LoginActivity.class);
        notificationIntent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        PendingIntent pendingIntent = PendingIntent.getActivity(AmbulanceForegroundService.this, 0,
                notificationIntent, 0);

        // Stop intent
        Intent stopServiceIntent = new Intent(AmbulanceForegroundService.this, LoginActivity.class);
        stopServiceIntent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        stopServiceIntent.setAction(LoginActivity.LOGOUT);
        PendingIntent stopServicePendingIntent = PendingIntent.getActivity(AmbulanceForegroundService.this, 0,
                stopServiceIntent, 0);

        Notification notification = new NotificationCompat.Builder(this, PRIMARY_CHANNEL)
                .setContentTitle("EMSTrack")
                .setTicker(message)
                .setContentText(message)
                .setStyle(new NotificationCompat.BigTextStyle()
                        .bigText(message))
                .setSmallIcon(R.mipmap.ic_launcher)
                .setContentIntent(pendingIntent)
                .setOngoing(true)
                .addAction(android.R.drawable.ic_menu_close_clear_cancel, getString(R.string.restartText), stopServicePendingIntent)
                .build();

        NotificationManagerCompat notificationManager = NotificationManagerCompat.from(this);
        notificationManager.notify(NOTIFICATION_ID, notification);

    }

    /**
     * Logout
     */
    public void logout(final String uuid) {

        // buffer to be consumed?
        if (!consumeBuffer()) {

            // could not consume entire buffer, log and empty anyway
            Log.e(TAG, "Could not empty buffer before logging out.");
            _updateBuffer = new ArrayList<>();

        }

        // remove ambulance
        removeAmbulance();

        // remove hospital map
        removeHospitals();

        // remove ambulance map
        removeOtherAmbulances();

        // stop geofences
        // TODO: Should we wait for completion?
        removeGeofences(null);

        // disconnect mqttclient
        MqttProfileClient profileClient = getProfileClient(this);
        try {

            profileClient.disconnect(new MqttProfileCallback() {

                @Override
                public void onReconnect() {

                    Log.d(TAG, "onReconnect during disconnect. Should never happen.");

                }

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
            localIntent.putExtra(BroadcastExtras.MESSAGE, "Could not disconnect: " + e.toString());
            sendBroadcastWithUUID(localIntent, uuid);

        } catch (Exception e) {

            Log.d(TAG, "Failed to disconnect.");

            // Broadcast failure
            Intent localIntent = new Intent(BroadcastActions.FAILURE);
            localIntent.putExtra(BroadcastExtras.MESSAGE, "Could not disconnect: " + e.toString());
            sendBroadcastWithUUID(localIntent, uuid);

        }

    }

    @Override
    public void onReconnect() {

        Log.d(TAG, "onReconnect.");

        // Suppress changes in updating location until reconnect is complete
        _reconnecting = true;

        // Store reconnection information
        _reconnectionInformation = new ReconnectionInformation(
                _ambulance != null,
                _otherAmbulances != null,
                _hospitals != null,
                isUpdatingLocation());

        // Set online false
        setOnline(false);

        // Create notification
        NotificationCompat.Builder mBuilder = new NotificationCompat.Builder(this, PRIMARY_CHANNEL)
                .setSmallIcon(R.mipmap.ic_launcher)
                .setContentTitle("EMSTrack")
                .setContentText(getString(R.string.attemptingToReconnect))
                .setPriority(NotificationCompat.PRIORITY_DEFAULT)
                .setAutoCancel(true);

        NotificationManagerCompat notificationManager = NotificationManagerCompat.from(this);
        notificationManager.notify(notificationId.getAndIncrement(), mBuilder.build());

    }

    /**
     * Callback after handling successful connection
     */
    @Override
    public void onSuccess() {

        Log.d(TAG, "onSuccess after reconnect. Restoring subscriptions.");

        // Set online true
        setOnline(true);

        if (_reconnectionInformation == null) {

            Log.e(TAG, "Null reconnection information. Aborting...");
            return;

        }

        Log.d(TAG, "Reconnection information = " + _reconnectionInformation.toString());

        // Retrieve credentials
        String username = sharedPreferences.getString(PREFERENCES_USERNAME, null);

        // Subscribe to error topic
        if (username != null)
            subscribeToError(username);

        if (_reconnectionInformation.hasAmbulance()) {

            final int ambulanceId = _ambulance.getId();
            final String ambulanceIdentifier = _ambulance.getIdentifier();

            // Remove current ambulance
            // TODO: Does it need to be asynchrounous?
            removeAmbulance(true);

            // Retrieve ambulance
            Intent ambulanceIntent = new Intent(this, AmbulanceForegroundService.class);
            ambulanceIntent.setAction(Actions.GET_AMBULANCE);
            ambulanceIntent.putExtra("AMBULANCE_ID", ambulanceId);
            ambulanceIntent.putExtra("RECONNECT", true);

            // What to do when GET_AMBULANCE service completes?
            new OnServiceComplete(this,
                    BroadcastActions.AMBULANCE_UPDATE,
                    BroadcastActions.FAILURE,
                    ambulanceIntent) {

                @Override
                public void onSuccess(Bundle extras) {

                    Log.i(TAG, String.format("Subscribed to ambulance %1$s", ambulanceIdentifier));

                    // clear reconnecting flag
                    _reconnecting = false;

                    // Create notification
                    NotificationCompat.Builder mBuilder = new NotificationCompat.Builder(AmbulanceForegroundService.this, PRIMARY_CHANNEL)
                            .setSmallIcon(R.mipmap.ic_launcher)
                            .setContentTitle("EMSTrack")
                            .setContentText(getString(R.string.serverIsBackOnline))
                            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
                            .setAutoCancel(true);

                    NotificationManagerCompat notificationManager = NotificationManagerCompat.from(AmbulanceForegroundService.this);
                    notificationManager.notify(notificationId.getAndIncrement(), mBuilder.build());

                    if (_reconnectionInformation.isUpdatingLocation()) {

                        // can stream location?
                        MqttProfileClient profileClient = getProfileClient(AmbulanceForegroundService.this);
                        Ambulance ambulance = getAmbulance();
                        String ambulanceLocationClientId = ambulance.getLocationClientId();
                        final String clientId = profileClient.getClientId();
                        if (clientId.equals(ambulanceLocationClientId)) {

                            // ambulance is available, start updates
                            Log.d(TAG, "Ambulance is available: starting updates.");

                            // Start location updates
                            Intent locationUpdatesIntent = new Intent(AmbulanceForegroundService.this,
                                    AmbulanceForegroundService.class);
                            locationUpdatesIntent.setAction(Actions.START_LOCATION_UPDATES);
                            startService(locationUpdatesIntent);

                        } else if (ambulanceLocationClientId == null) {

                            // ambulance is available, request update
                            Log.d(TAG, "Ambulance is available: requesting updates.");

                            // Update location_client on server
                            new OnServiceComplete(AmbulanceForegroundService.this,
                                    BroadcastActions.AMBULANCE_UPDATE,
                                    BroadcastActions.FAILURE,
                                    null) {

                                public void run() {

                                    // update ambulance
                                    String payload = String.format("{\"location_client_id\":\"%1$s\"}", clientId);
                                    updateAmbulance(payload);

                                }

                                @Override
                                public void onSuccess(Bundle extras) {

                                    // TODO: Can fail

                                    // ambulance is available, request update
                                    Log.d(TAG, "Ambulance is available: starting updates.");

                                    // Start location updates
                                    Intent locationUpdatesIntent = new Intent(AmbulanceForegroundService.this,
                                            AmbulanceForegroundService.class);
                                    locationUpdatesIntent.setAction(Actions.START_LOCATION_UPDATES);
                                    startService(locationUpdatesIntent);

                                }

                                @Override
                                public void onReceive(Context context, Intent intent) {

                                    // Retrieve action
                                    String action = intent.getAction();

                                    // Intercept AMBULANCE_UPDATE
                                    if (action.equals(BroadcastActions.AMBULANCE_UPDATE))
                                        // Inject uuid into AMBULANCE_UPDATE
                                        intent.putExtra(OnServicesComplete.UUID, getUuid());

                                    // Call super
                                    super.onReceive(context, intent);
                                }

                            };

                        } else {

                            // ambulance is no longer available
                            // TODO: What to do?
                            Log.d(TAG, "Ambulance is not longer available for updates.");

                        }

                    } else {

                        // This may not be necessary
                        // Stop location updates
                        Intent locationUpdatesIntent = new Intent(AmbulanceForegroundService.this,
                                AmbulanceForegroundService.class);
                        locationUpdatesIntent.setAction(Actions.STOP_LOCATION_UPDATES);
                        startService(locationUpdatesIntent);

                    }

                    if (_reconnectionInformation.hasOtherAmbulances()) {

                        Log.i(TAG, "Subscribing to ambulances.");

                        // Remove ambulances
                        // TODO: Does it need to be asynchrounous?
                        removeOtherAmbulances(true);

                        // Retrieve ambulance
                        Intent ambulanceIntent = new Intent(AmbulanceForegroundService.this,
                                AmbulanceForegroundService.class);
                        ambulanceIntent.setAction(Actions.GET_AMBULANCES);
                        ambulanceIntent.putExtra("RECONNECT", true);

                    }

                    if (_reconnectionInformation.hasHospitals()) {

                        Log.i(TAG, "Subscribing to hospitals.");

                        // Remove hospitals
                        // TODO: Does it need to be asynchrounous?
                        removeHospitals(true);

                        // Retrieve hospital
                        Intent hospitalIntent = new Intent(AmbulanceForegroundService.this,
                                AmbulanceForegroundService.class);
                        hospitalIntent.setAction(Actions.GET_HOSPITALS);
                        hospitalIntent.putExtra("RECONNECT", true);
                        startService(hospitalIntent);
                        
                    }

                }

                @Override
                public void onFailure(Bundle extras) {

                    // clear reconnecting flag
                    _reconnecting = false;

                    // Create notification
                    NotificationCompat.Builder mBuilder = new NotificationCompat.Builder(AmbulanceForegroundService.this, PRIMARY_CHANNEL)
                            .setSmallIcon(R.mipmap.ic_launcher)
                            .setContentTitle("EMSTrack")
                            .setContentText(getString(R.string.failedToReconnect))
                            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
                            .setAutoCancel(true);

                    NotificationManagerCompat notificationManager = NotificationManagerCompat.from(AmbulanceForegroundService.this);
                    notificationManager.notify(notificationId.getAndIncrement(), mBuilder.build());


                    super.onFailure(extras);

                }

            }
                    .setFailureMessage(getResources().getString(R.string.couldNotRetrieveAmbulance,
                            ambulanceIdentifier));

        } else {

            // clear reconnecting flag
            _reconnecting = false;

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

        if (exception instanceof MqttException) {

            int reason = ((MqttException) exception).getReasonCode();

            if (reason == MqttException.REASON_CODE_CLIENT_CONNECTED) {

                // Not an error, already connected, just log
                Log.d(TAG, "Tried to connect, but already connected.");

                // Set online true
                setOnline(true);

                return;

            }

        }

        // Set online false
        setOnline(false);

        // Notify user and return
        NotificationCompat.Builder mBuilder = new NotificationCompat.Builder(this, PRIMARY_CHANNEL)
                .setSmallIcon(R.mipmap.ic_launcher)
                .setContentTitle(getString(R.string.EMSTrack))
                .setContentText(getString(R.string.serverIsOffline))
                .setPriority(NotificationCompat.PRIORITY_DEFAULT)
                .setAutoCancel(true);

        NotificationManagerCompat notificationManager = NotificationManagerCompat.from(this);
        notificationManager.notify(notificationId.getAndIncrement(), mBuilder.build());

        return;

    }


    public void subscribeToError(final String username) {

        MqttProfileClient profileClient = getProfileClient(AmbulanceForegroundService.this);
        try {

            profileClient.subscribe(String.format("user/%1$s/client/%2$s/error",
                    username, profileClient.getClientId()),
                    1, new MqttProfileMessageCallback() {

                        @Override
                        public void messageArrived(String topic, MqttMessage message) {

                            Log.d(TAG, "MQTT error message.");

                            // Create notification
                            NotificationCompat.Builder mBuilder = new NotificationCompat.Builder(AmbulanceForegroundService.this, PRIMARY_CHANNEL)
                                    .setSmallIcon(R.mipmap.ic_launcher)
                                    .setContentTitle("EMSTrack")
                                    .setContentText(getString(R.string.serverError, String.valueOf(message.getPayload())))
                                    .setPriority(NotificationCompat.PRIORITY_DEFAULT)
                                    .setAutoCancel(true);

                            NotificationManagerCompat notificationManager = NotificationManagerCompat.from(AmbulanceForegroundService.this);
                            notificationManager.notify(notificationId.getAndIncrement(), mBuilder.build());

                        }

                    });

        } catch (MqttException e1) {

            Log.d(TAG, "Could not subscribe to error topic.");

        }

    }



    /**
     * Login user
     *
     * @param username Username
     * @param password Password
     */
    public void login(final String username, final String password, final String uuid) {

        // What to do when logout completes?
        new OnServiceComplete(this,
                AmbulanceForegroundService.BroadcastActions.SUCCESS,
                AmbulanceForegroundService.BroadcastActions.FAILURE,
                null) {

            public void run() {

                // logout
                logout(getUuid());

            }

            @Override
            public void onSuccess(Bundle extras) {

                // Retrieve client
                final MqttProfileClient profileClient = getProfileClient(AmbulanceForegroundService.this);

                // Set callback to be called after profile is retrieved
                profileClient.setCallback(new MqttProfileCallback() {

                    @Override
                    public void onReconnect() {

                        Log.d(TAG, "onReconnect after connection. Could happen.");
                        // TODO: but I am not sure how to handle it yet.

                    }

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

                        // Subscribe to error topic
                        subscribeToError(username);

                        // set callback for handling loss of connection/reconnection
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
                        public void onReconnect() {

                            Log.d(TAG, "onReconnect during connection. Should not happen.");

                        }

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

            @Override
            public void onFailure(Bundle extras) {

                // Broadcast failure
                Intent localIntent = new Intent(BroadcastActions.FAILURE);
                if (extras != null)
                    localIntent.putExtras(extras);
                sendBroadcastWithUUID(localIntent, uuid);

            }

        };

    }

    /**
     * Retrieve ambulance
     *
     * @param ambulanceId the ambulance id
     */
    public void retrieveAmbulance(final int ambulanceId, final String uuid, final boolean reconnect) {

        // Is ambulance id valid?
        if (ambulanceId < 0) {

            // Broadcast failure
            Intent localIntent = new Intent(BroadcastActions.FAILURE);
            localIntent.putExtra(BroadcastExtras.MESSAGE, getString(R.string.invalidAmbulanceId));
            sendBroadcastWithUUID(localIntent, uuid);

            return;
        }

        // Is ambulance new and not reconnect?
        Ambulance ambulance = getAmbulance();
        if (!reconnect && ambulance != null && ambulance.getId() == ambulanceId) {
            return;
        }

        // Remove current ambulance
        // TODO: Does it need to be asynchrounous?
        removeAmbulance(reconnect);

        // Remove current ambulances
        // TODO: Does it need to be asynchrounous?
        removeOtherAmbulances(reconnect);

        // Retrieve client
        MqttProfileClient profileClient = getProfileClient(this);

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
            profileClient.subscribe(String.format("ambulance/%1$d/data", ambulanceId),
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

                                // stop updates?
                                MqttProfileClient profileClient = getProfileClient(AmbulanceForegroundService.this);
                                String clientId = profileClient.getClientId();
                                if (isUpdatingLocation() &&
                                        (ambulance.getLocationClientId() == null ||
                                                !clientId.equals(ambulance.getLocationClientId()))) {

                                    // turn off tracking
                                    Intent localIntent = new Intent(AmbulanceForegroundService.this, AmbulanceForegroundService.class);
                                    localIntent.setAction(AmbulanceForegroundService.Actions.STOP_LOCATION_UPDATES);
                                    startService(localIntent);

                                }

                                if (firstTime) {

                                    // Broadcast success
                                    Intent localIntent = new Intent(BroadcastActions.SUCCESS);
                                    sendBroadcastWithUUID(localIntent, uuid);

                                }

                                // Broadcast ambulance update
                                Intent localIntent = new Intent(BroadcastActions.AMBULANCE_UPDATE);
                                sendBroadcastWithUUID(localIntent, uuid);

                            } catch (Exception e) {

                                Log.i(TAG, "Could not parse ambulance update.");

                                // Broadcast failure
                                Intent localIntent = new Intent(BroadcastActions.FAILURE);
                                localIntent.putExtra(BroadcastExtras.MESSAGE, getString(R.string.couldNotParseAmbulance));
                                sendBroadcastWithUUID(localIntent, uuid);

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

        // subscribe to call stauses
        try {
            Log.i(TAG, "Subscribing to statuses");

            final String clientId = profileClient.getClientId();

            profileClient.subscribe(String.format("ambulance/%1$d/call/+/status", ambulanceId),
                    2, new MqttProfileMessageCallback() {
                        @Override
                        public void messageArrived(String topic, MqttMessage message) {

                            // Keep subscription to calls to make sure we receive latest updates

                            Log.i(TAG, "Retrieving stauses");

                            try {
                                // parse the status
                                String status = new String(message.getPayload());
                                Log.i(TAG, "payload is: " + status);

                                // check if message is "requested"
                                // literally has to be "requested" in quotes
                                if (status.equalsIgnoreCase("\"requested\"")) {
                                    // retrieve call id
                                    Log.i(TAG, "STATUS MESSAGE TOPIC: " + topic);

                                    // the call id is the 3rd value
                                    String callId = topic.split("/")[3];

                                    Log.i(TAG, "STATUS CALL ID: " + callId);

                                    // TODO: broadcast intent to main activity to create dialog
                                    // include clientId and callId in intent
                                    // assume user has accepted call for now
                                    Intent callPromptIntent = new Intent(BroadcastActions.PROMPT_CALL);
                                    callPromptIntent.putExtra("CALLID", callId);
                                    sendBroadcastWithUUID(callPromptIntent, uuid);

//                                    Intent localIntent = new Intent(AmbulanceForegroundService.this,
//                                            AmbulanceForegroundService.class);
//                                    localIntent.setAction(Actions.CALL_ACCEPTED);
//                                    localIntent.putExtra("callId", callId);
//                                    startService(localIntent);

                                } else {
                                    Log.i(TAG, "DID NOT GET REQUESTED STATUS");
                                }


                            } catch (Exception e) {

                                Log.i(TAG, "Could not parse status");

                                // Broadcast failure
                                Intent localIntent = new Intent(BroadcastActions.FAILURE);
                                localIntent.putExtra(BroadcastExtras.MESSAGE, getString(R.string.couldNotParseStatus));
                                sendBroadcastWithUUID(localIntent, uuid);
                            }
                        }
                    });
        } catch (MqttException e) {

            Log.d(TAG, "Could not subscribe to statuses");

            // Broadcast failure
            Intent localIntent = new Intent(BroadcastActions.FAILURE);
            localIntent.putExtra(BroadcastExtras.MESSAGE, getString(R.string.couldNotSubscribeToStatuses));
            sendBroadcastWithUUID(localIntent, uuid);
        }

    }

    // handles steps 3 and 4 of Accepting Calls
    public void replyToAcceptCall(final String callId, String uuid) {
        Log.i(TAG, "Replying to server with username, client id, and call id");

        MqttProfileClient profileClient = getProfileClient(AmbulanceForegroundService.this);

        Ambulance ambulance = getAmbulance();

        if (ambulance != null) {

            try {

                Log.i(TAG, "publishing location client to user/{username}/client/{client-id}" +
                        "/ambulance/{call-id}/data");
                // TODO: ask Mauricio about qos and retained
                // step 3: publish client id to server
                profileClient.publish(String.format("user/%1$s/client/%2$s/ambulance/%3$s/data",
                        profileClient.getUsername(), profileClient.getClientId(), callId),
                        String.format("{\"location_client\": %1$s}", profileClient.getClientId()),
                        2, false);

                // start streaming location
                startLocationUpdates(uuid, false);

            } catch (MqttException e) {

                String path = String.format("Could not publish to user/%1$s/client/%2$s/ambulance" +
                                "/%3$s/data",
                        profileClient.getUsername(), profileClient.getClientId(), callId);

                Log.d(TAG, path);

                // TODO: ask Mauricio if we need this for publishing
                Intent localIntent = new Intent(BroadcastActions.FAILURE);
                localIntent.putExtra(BroadcastExtras.MESSAGE, getString(R.string.couldNotPublish, path));
                sendBroadcastWithUUID(localIntent, uuid);
            }

            try {

                Log.i(TAG, "publishing accepted to user/{username}/client/{client-id}/" +
                        "ambulance/{ambulance-id}/call/{call-id}/status");
                // publish accepted to server (step 4)
                profileClient.publish(String.format("user/%1$s/client/%2$s/" +
                                "ambulance/%3$s/call/%4$s/status",
                        profileClient.getUsername(), profileClient.getClientId(), ambulance.getId(),
                        callId), "\"Accepted\"", 2, false);

            } catch (MqttException e) {

                String path = String.format("Could not publish to user/%1$s/client/%2$s/ambulance/" +
                                "%3$s/call/%4$s/status",
                        profileClient.getUsername(), profileClient.getClientId(), ambulance.getId(),
                        callId);

                Log.d(TAG, path);

                Intent localIntent = new Intent(BroadcastActions.FAILURE);
                localIntent.putExtra(BroadcastExtras.MESSAGE, getString(R.string.couldNotPublish, path));
                sendBroadcastWithUUID(localIntent, uuid);
            }

            try {
                profileClient.subscribe(String.format("ambulance/%1$d/call/+/status", ambulance.getId()),
                        2, new MqttProfileMessageCallback() {
                            @Override
                            public void messageArrived(String topic, MqttMessage message) {
                                Log.i(TAG, "Received ongoing");

                                // step 5
                                Intent localIntent = new Intent(AmbulanceForegroundService.this,
                                        AmbulanceForegroundService.class);
                                localIntent.setAction(Actions.CALL_ONGOING);
                                localIntent.putExtra("callId", callId);
                                startService(localIntent);
                            }
                        });
            } catch (MqttException e) {

                String path = String.format("Could not subscribe to ambulance/%1$s/call/%2$s/status",
                        ambulance.getId(), callId);

                Log.d(TAG, path);

                Intent localIntent = new Intent(BroadcastActions.FAILURE);
                localIntent.putExtra(BroadcastExtras.MESSAGE, path);
                sendBroadcastWithUUID(localIntent, uuid);
            }

        } else {
            Log.d(TAG, "Ambulance not found while in replyToAcceptCall()");

            Intent localIntent = new Intent(BroadcastActions.FAILURE);
            localIntent.putExtra(BroadcastExtras.MESSAGE, getString(R.string.couldNotFindAmbulance));
            sendBroadcastWithUUID(localIntent, uuid);
        }
    }

    // TODO: wesley start here
    // do steps 6 and 7 here, create geofence intent right after step 7
    // do steps 8 and 9 in GeofenceBroadcastReceiver
    // handles steps 6 to 10
    public void replyToOngoingCall(final String callId, final String uuid) {
        // step 6 & 7
        Log.i(TAG, "Replying to server with client id, and call id");

        // TODO: ask Mauricio if we need to check if online or not
        final MqttProfileClient profileClient = getProfileClient(AmbulanceForegroundService.this);

        final Ambulance ambulance = getAmbulance();

        // step 6
        // subscribe to call id
        try {

            Log.i(TAG, "Subscribing to call data");

            final String clientId = profileClient.getClientId();

            profileClient.subscribe(String.format("call/%1$s/data", callId),
                    2, new MqttProfileMessageCallback() {
                        @Override
                        public void messageArrived(String topic, MqttMessage message) {

                            // Keep subscription to calls to make sure we receive latest updates

                            Log.i(TAG, "Retrieving call data");

                            // parse call id data
                            GsonBuilder gsonBuilder = new GsonBuilder();
                            gsonBuilder.setFieldNamingPolicy(FieldNamingPolicy.LOWER_CASE_WITH_UNDERSCORES);
                            Gson gson = gsonBuilder.create();

                            try {

                                // Parse and set call id data
                                Call call = gson
                                        .fromJson(new String(message.getPayload()),
                                                Call.class);

                                // Add geofence
                                Log.i(TAG, "Adding geofence");
                                Intent serviceIntent = new Intent(AmbulanceForegroundService.this,
                                        AmbulanceForegroundService.class);
                                serviceIntent.setAction(AmbulanceForegroundService.Actions.GEOFENCE_START);
                                serviceIntent.putExtra("USERNAME",
                                        profileClient.getUsername());
                                serviceIntent.putExtra("CLIENTID",
                                        profileClient.getClientId());
                                serviceIntent.putExtra("CALLID",
                                        callId);
                                serviceIntent.putExtra("LATITUDE",
                                        (float) call.getLocation().getLatitude());
                                serviceIntent.putExtra("LONGITUDE",
                                        (float) call.getLocation().getLongitude());
                                serviceIntent.putExtra("RADIUS", 50.f);
                                startService(serviceIntent);

                            } catch (Exception e) {

                                Log.i(TAG, "Could not parse call id update.");

                                // Broadcast failure
                                Intent localIntent = new Intent(BroadcastActions.FAILURE);
                                localIntent.putExtra(BroadcastExtras.MESSAGE, getString(R.string.couldNotParseCallData));
                                sendBroadcastWithUUID(localIntent, uuid);

                            }
                        }
                    });
        } catch (MqttException e) {

            Log.d(TAG, "Could not subscribe to call id data");

            // Broadcast failure
            Intent localIntent = new Intent(BroadcastActions.FAILURE);
            localIntent.putExtra(BroadcastExtras.MESSAGE, getString(R.string.couldNotSubscribeToStatuses));
            sendBroadcastWithUUID(localIntent, uuid);
        }

        // step 7
        if (ambulance != null) {
            try {

                // TODO: ask Mauricio about qos and retained
                // step 3: publish patient bound to server
                profileClient.publish(String.format("user/%1$s/client/%2$s/ambulance/%3$s/data",
                        profileClient.getUsername(), profileClient.getClientId(), callId),
                        "patient bound", 2, false);

            } catch (MqttException e) {

                String path = String.format("Could not publish to user/%1$s/client/%2$s/ambulance" +
                                "/%3$s/data",
                        profileClient.getUsername(), profileClient.getClientId(), callId);

                Log.d(TAG, path);

                // TODO: ask Mauricio if we need this for publishing
                Intent localIntent = new Intent(BroadcastActions.FAILURE);
                localIntent.putExtra(BroadcastExtras.MESSAGE, getString(R.string.couldNotPublish, path));
                sendBroadcastWithUUID(localIntent, uuid);
            }
        } else {
            Log.d(TAG, "Ambulance not found while in replyToOngoingCall()");

            Intent localIntent = new Intent(BroadcastActions.FAILURE);
            localIntent.putExtra(BroadcastExtras.MESSAGE, getString(R.string.couldNotFindAmbulance));
            sendBroadcastWithUUID(localIntent, uuid);
        }
    }

    /**
     * Remove current ambulance
     */
    public void removeAmbulance() { removeAmbulance(false); }

    /**
     * Remove current ambulance
     */
    public void removeAmbulance(boolean reconnect) {

        Ambulance ambulance = getAmbulance();
        if (ambulance == null ) {
            Log.i(TAG,"No ambulance to remove.");
            return;
        }

        // Logout and unsubscribe if not a reconnect
        if (!reconnect) {

            // remove location updates
            stopLocationUpdates(null);

            // get ambulance id
            int ambulanceId = ambulance.getId();

            // Retrieve client
            final MqttProfileClient profileClient = getProfileClient(this);

            try {

                // Publish ambulance logout
                String topic = String.format("user/%1$s/client/%2$s/ambulance/%3$s/status",
                        profileClient.getUsername(), profileClient.getClientId(), ambulanceId);
                profileClient.publish(topic, "ambulance logout", 2, true);

            } catch (MqttException e) {
                Log.d(TAG, "Could not logout from ambulance");
            }

            try {

                // Unsubscribe to ambulance data
                profileClient.unsubscribe("ambulance/" + ambulanceId + "/data");

                // TODO: unsubscribe from statuses and call data

            } catch (MqttException exception) {
                Log.d(TAG, "Could not unsubscribe to 'ambulance/" + ambulanceId + "/data'");
            }


            // Remove ambulance
            _ambulance = null;

        }

    }

    /**
     * Retrieve hospitals
     */
    public void retrieveHospitals(final String uuid, boolean reconnect) {

        // Remove current hospital map
        // TODO: Does it need to be asynchrounous?
        removeHospitals(reconnect);

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
    public void removeHospitals() { removeHospitals(false); }

    /**
     * Remove current hospitals
     */
    public void removeHospitals(boolean reconnect) {

        Map<Integer, Hospital> hospitals = getHospitals();
        if (hospitals == null || hospitals.size() == 0) {
            Log.i(TAG, "No hospital to remove.");
            return;
        }

        // Unsubscribe only if not reconnect
        if (!reconnect) {

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

    }

    /**
     * Retrieve ambulances
     */
    public void retrieveOtherAmbulances(final String uuid, boolean reconnect) {

        // Remove current ambulance map
        // TODO: Does it need to be asynchrounous?
        removeOtherAmbulances(reconnect);

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
                                boolean firstTime = (_otherAmbulances == null);

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

                                        // set _otherAmbulances
                                        _otherAmbulances = ambulances;

                                        // Broadcast success
                                        Intent localIntent = new Intent(BroadcastActions.SUCCESS);
                                        sendBroadcastWithUUID(localIntent, uuid);

                                    }

                                } else {

                                    // Update ambulance map
                                    ambulances.put(ambulanceId, ambulance);

                                    // Broadcast ambulances update
                                    Intent localIntent = new Intent(BroadcastActions.OTHER_AMBULANCES_UPDATE);
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
    public void removeOtherAmbulances() { removeOtherAmbulances(false); }

    /**
     * Remove current ambulances
     */
    public void removeOtherAmbulances(boolean reconnect) {

        Map<Integer, Ambulance> ambulances = getOtherAmbulances();
        if (ambulances == null || ambulances.size() == 0) {
            Log.i(TAG, "No ambulances to remove.");
            return;
        }

        // Remove subscriptions if not reconnect
        if (!reconnect) {

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
            _otherAmbulances = null;

        }

    }

    /**
     * Stop subscribing to ambulances
     */
    public void stopAmbulances(final String uuid) {

        // Remove current ambulance map
        // TODO: Does it need to be asynchrounous?
        removeOtherAmbulances();

        // Broadcast success
        Intent localIntent = new Intent(BroadcastActions.SUCCESS);
        sendBroadcastWithUUID(localIntent, uuid);

    }

    private void startLocationUpdates(final String uuid, final boolean reconnect) {

        // Already started?
        if (_updatingLocation) {
            Log.i(TAG, "Already requesting location updates. Skipping.");

            Log.i(TAG, "Consume buffer.");
            consumeBuffer();

            // Broadcast success and return
            Intent localIntent = new Intent(BroadcastActions.SUCCESS);
            sendBroadcastWithUUID(localIntent, uuid);
            return;

        }

        if (!canUpdateLocation()) {

            // Broadcast failure and return
            Intent localIntent = new Intent(BroadcastActions.FAILURE);
            localIntent.putExtra(BroadcastExtras.MESSAGE, getString(R.string.cannotUseLocationServices));
            sendBroadcastWithUUID(localIntent, uuid);
            return;

        }

        // Logged in?
        Ambulance ambulance = AmbulanceForegroundService.getAmbulance();
        if (ambulance == null) {

            // Broadcast failure and return
            Intent localIntent = new Intent(BroadcastActions.FAILURE);
            localIntent.putExtra(BroadcastExtras.MESSAGE, getString(R.string.noAmbulanceSelected));
            sendBroadcastWithUUID(localIntent, uuid);
            return;

        }

        // is location_client available?
        final MqttProfileClient profileClient = AmbulanceForegroundService.getProfileClient(this);

        // can stream location?
        String ambulanceLocationClientId = ambulance.getLocationClientId();
        String clientId = profileClient.getClientId();
        if (clientId.equals(ambulanceLocationClientId)) {

            // ambulance is available, start updates

            // Create settings client
            SettingsClient settingsClient = LocationServices.getSettingsClient(this);

            // Check if the device has the necessary location settings.
            settingsClient.checkLocationSettings(getLocationSettingsRequest())
                    .addOnSuccessListener(new OnSuccessListener<LocationSettingsResponse>() {
                        @SuppressLint("MissingPermission")
                        @Override
                        public void onSuccess(LocationSettingsResponse locationSettingsResponse) {

                            Log.i(TAG, "All location settings are satisfied.");

                            Log.i(TAG, "Consume buffer.");
                            consumeBuffer();

                            Log.i(TAG, "Starting location updates.");
                            beginLocationUpdates(uuid);

                        }
                    })
                    .addOnFailureListener(new OnFailureListener() {
                        @Override
                        public void onFailure(@NonNull Exception e) {
                            int statusCode = ((ApiException) e).getStatusCode();
                            String message = getString(R.string.inadequateLocationSettings);
                            switch (statusCode) {
                                case LocationSettingsStatusCodes.RESOLUTION_REQUIRED:
                                    message += "Try restarting app.";
                                    break;
                                case LocationSettingsStatusCodes.SETTINGS_CHANGE_UNAVAILABLE:
                                    message += "Please fix in Settings.";
                            }
                            Log.e(TAG, message);

                            // Broadcast failure and return
                            Intent localIntent = new Intent(BroadcastActions.FAILURE);
                            localIntent.putExtra(BroadcastExtras.MESSAGE, message);
                            sendBroadcastWithUUID(localIntent, uuid);
                            return;

                        }
                    });


        } else if (ambulanceLocationClientId == null) {

            // ambulance is available, request update

            // Update location_client on server, listening to updates already
            String payload = String.format("{\"location_client_id\":\"%1$s\"}", clientId);
            Intent intent = new Intent(this, AmbulanceForegroundService.class);
            intent.setAction(Actions.UPDATE_AMBULANCE);
            Bundle bundle = new Bundle();
            bundle.putString("UPDATE", payload);
            intent.putExtras(bundle);

            // What to do when service completes?
            new OnServicesComplete(this,
                    new String[] {
                            BroadcastActions.SUCCESS,
                            BroadcastActions.AMBULANCE_UPDATE
                    },
                    new String[] {BroadcastActions.FAILURE},
                    intent) {

                @Override
                public void onSuccess(Bundle extras) {

                    Log.i(TAG, "onSuccess");

                    // Try again with right permissions
                    // TODO: This could potentially lead to a loop, add counter to prevent infinite recursion
                    startLocationUpdates(uuid, reconnect);

                }

                @Override
                public void onFailure(Bundle extras) {

                    // Broadcast failure
                    String message = extras.getString(BroadcastExtras.MESSAGE);
                    Intent localIntent = new Intent(BroadcastActions.FAILURE);
                    localIntent.putExtra(BroadcastExtras.MESSAGE, message);
                    sendBroadcastWithUUID(localIntent, uuid);

                }

                @Override
                public void onReceive(Context context, Intent intent) {

                    // Retrieve action
                    String action = intent.getAction();

                    // Intercept success
                    if (action.equals(BroadcastActions.SUCCESS))
                        // prevent propagation, still waiting for AMBULANCE_UPDATE
                        return;

                    // Intercept AMBULANCE_UPDATE
                    if (action.equals(BroadcastActions.AMBULANCE_UPDATE))
                        // Inject uuid into AMBULANCE_UPDATE
                        intent.putExtra(OnServicesComplete.UUID, getUuid());

                    // Call super
                    super.onReceive(context, intent);
                }

            };

        } else {

            // ambulance is not available, report failure
            Intent localIntent = new Intent(BroadcastActions.FAILURE);
            localIntent.putExtra(BroadcastExtras.MESSAGE, getString(R.string.anotherClientReporting));
            sendBroadcastWithUUID(localIntent, uuid);
            return;

        }

    }

    /**
     * Handles the Request Updates button and requests start of location updates.
     */
    public void beginLocationUpdates(final String uuid) {

        // Create location callback
        locationCallback = new LocationCallback() {

            @Override
            public void onLocationResult(LocationResult result) {

                // get profile client
                final MqttProfileClient profileClient = AmbulanceForegroundService.getProfileClient(AmbulanceForegroundService.this);

                // Retrieve results
                if (result != null) {

                    List<android.location.Location> locations = result.getLocations();
                    Log.i(TAG, "Received " + locations.size() + " location updates");

                    // Initialize locationFilter
                    if (_lastLocation != null)
                        locationFilter.setLocation(_lastLocation);

                    // Filter location
                    List<LocationUpdate> filteredLocations = locationFilter.update(locations);

                    // Publish update
                    if (filteredLocations.size() > 0)
                        updateAmbulance(filteredLocations);

                    // Notification message
                    // TODO: These need to be internationalized but cannot be retrieved without a context
                    String message = "Last location update at "
                            + new SimpleDateFormat("d MMM HH:mm:ss z", Locale.getDefault()).format(new Date());

                    if (_lastServerUpdate != null)
                        message += "\nLast server update at "
                                + new SimpleDateFormat("d MMM HH:mm:ss z", Locale.getDefault()).format(_lastServerUpdate);

                    if (isOnline())
                        message += "\nServer is online";
                    else
                        message += "\nServer is offline";

                    if (_updateBuffer.size() > 1)
                        message += ", " + String.format("%1$d messages on buffer", _updateBuffer.size());
                    else if (_updateBuffer.size() > 0)
                        message += ", " + String.format("1 message on buffer");

                    // modify foreground service notification
                    Intent notificationIntent = new Intent(AmbulanceForegroundService.this, AmbulanceForegroundService.class);
                    notificationIntent.setAction(AmbulanceForegroundService.Actions.UPDATE_NOTIFICATION);
                    notificationIntent.putExtra("MESSAGE", message);
                    startService(notificationIntent);

                }

            }

        };

        try {

            fusedLocationClient.requestLocationUpdates(getLocationRequest(), locationCallback, null)
                    .addOnSuccessListener(new OnSuccessListener<Void>() {
                        @Override
                        public void onSuccess(Void aVoid) {

                            Log.i(TAG, "Starting location updates");
                            _updatingLocation = true;

                            // Broadcast success
                            Intent localIntent = new Intent(BroadcastActions.SUCCESS);
                            sendBroadcastWithUUID(localIntent, uuid);

                            // Broadcast location change
                            Intent changeIntent = new Intent(BroadcastActions.LOCATION_UPDATE_CHANGE);
                            sendBroadcastWithUUID(changeIntent, uuid);

                        }
                    })
                    .addOnFailureListener(new OnFailureListener() {
                        @Override
                        public void onFailure(@NonNull Exception e) {
                            Log.i(TAG, "Failed to start location updates");
                            e.printStackTrace();

                            // Broadcast failure
                            Intent localIntent = new Intent(BroadcastActions.FAILURE);
                            localIntent.putExtra(BroadcastExtras.MESSAGE, getString(R.string.failedToStartLocationUpdates));
                            sendBroadcastWithUUID(localIntent, uuid);

                        }
                    });

        } catch (SecurityException e) {
            Log.i(TAG, "Failed to start location updates");
            e.printStackTrace();

            // Broadcast failure
            Intent localIntent = new Intent(BroadcastActions.FAILURE);
            localIntent.putExtra(BroadcastExtras.MESSAGE, getString(R.string.failedToStartLocationUpdates));
            sendBroadcastWithUUID(localIntent, uuid);

        }
    }

    /**
     * Handles the Remove Updates button, and requests removal of location updates.
     */
    public void stopLocationUpdates(final String uuid) {

        _lastLocation = null;

        // Already started?
        if (!_updatingLocation) {
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
        fusedLocationClient.removeLocationUpdates(locationCallback)
                .addOnSuccessListener(new OnSuccessListener<Void>() {
                    @Override
                    public void onSuccess(Void aVoid) {

                        Log.i(TAG, "Stopping location updates");
                        _updatingLocation = false;

                        // Broadcast location change
                        Intent changeIntent = new Intent(BroadcastActions.LOCATION_UPDATE_CHANGE);
                        sendBroadcastWithUUID(changeIntent, uuid);

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

    private GeofencingRequest getGeofencingRequest(com.google.android.gms.location.Geofence geofence) {

        Log.i(TAG, "GEOFENCE_REQUEST: Built Geofencing Request");

        GeofencingRequest.Builder builder = new GeofencingRequest.Builder();
        builder.setInitialTrigger(GeofencingRequest.INITIAL_TRIGGER_ENTER);
        builder.addGeofence(geofence);

        return builder.build();

    }
    // CHANGED
    private PendingIntent getGeofencePendingIntent(final String callId) {

        // Reuse the PendingIntent if we already have it.
        if (geofenceIntent != null) {
            return geofenceIntent;
        }
        Intent intent = new Intent(this, GeofenceBroadcastReceiver.class);
        intent.putExtra("CALLID", callId);

        // We use FLAG_UPDATE_CURRENT so that we get the same pending intent back when
        // calling addGeofences() and removeGeofences().
        geofenceIntent = PendingIntent.getBroadcast(this, 0,
                intent, PendingIntent.FLAG_UPDATE_CURRENT);

        return geofenceIntent;
    }

    // CHANGED:
    private void startGeofence(final String uuid, final Geofence geofence, final String callId) {

        Log.d(TAG,String.format("GEOFENCE(%1$s, %2$f)", geofence.getLocation().toString(), geofence.getRadius()));

        // Set unique id
        final String id = "GEOFENCE_" + geofencesId.getAndIncrement();

        // Create settings client
        SettingsClient settingsClient = LocationServices.getSettingsClient(this);

        // Check if the device has the necessary location settings.
        settingsClient.checkLocationSettings(getLocationSettingsRequest())
                .addOnSuccessListener(new OnSuccessListener<LocationSettingsResponse>() {
                    @SuppressLint("MissingPermission")
                    @Override
                    public void onSuccess(LocationSettingsResponse locationSettingsResponse) {

                        Log.i(TAG, "All location settings are satisfied.");

                        Log.i(TAG, "Adding GEOFENCE.");
                        // CHANGED
                        fenceClient.addGeofences(getGeofencingRequest(geofence.build(id)),
                                getGeofencePendingIntent(callId))
                                .addOnSuccessListener(new OnSuccessListener<Void>() {

                                    @Override
                                    public void onSuccess(Void aVoid) {
                                        // Geofences added
                                        Log.i(TAG, "GEOFENCES ADDED.");

                                        // Add to map
                                        _geofences.put(id, geofence);

                                        // Broadcast success
                                        Intent localIntent = new Intent(BroadcastActions.SUCCESS);
                                        sendBroadcastWithUUID(localIntent, uuid);

                                    }
                                })
                                .addOnFailureListener(new OnFailureListener() {
                                    @Override
                                    public void onFailure(@NonNull Exception e) {

                                        // Failed to add geofences
                                        Log.e(TAG, "FAILED TO ADD GEOFENCES: " + e.toString());

                                        // Broadcast failure and return
                                        Intent localIntent = new Intent(BroadcastActions.FAILURE);
                                        localIntent.putExtra(BroadcastExtras.MESSAGE, getString(R.string.failedToAddGeofence));
                                        sendBroadcastWithUUID(localIntent, uuid);
                                        return;

                                    }
                                });

                    }
                })
                .addOnFailureListener(new OnFailureListener() {
                    @Override
                    public void onFailure(@NonNull Exception e) {
                        int statusCode = ((ApiException) e).getStatusCode();
                        String message = getString(R.string.inadequateLocationSettings);
                        switch (statusCode) {
                            case LocationSettingsStatusCodes.RESOLUTION_REQUIRED:
                                message += "Try restarting app.";
                                break;
                            case LocationSettingsStatusCodes.SETTINGS_CHANGE_UNAVAILABLE:
                                message += "Please fix in Settings.";
                        }
                        Log.e(TAG, message);

                        // Broadcast failure and return
                        Intent localIntent = new Intent(BroadcastActions.FAILURE);
                        localIntent.putExtra(BroadcastExtras.MESSAGE, message);
                        sendBroadcastWithUUID(localIntent, uuid);
                        return;

                    }
                });
    }


    private void removeGeofences(final String uuid) {
        // CHANGED: Ask if this is even okay?
        fenceClient.removeGeofences(getGeofencePendingIntent(""))
                .addOnSuccessListener(new OnSuccessListener<Void>() {
                    @Override
                    public void onSuccess(Void aVoid) {
                        // Geofences removed
                        Log.i(TAG, "GEOFENCES REMOVED.");

                        // Clear all geofence ids
                        _geofences.clear();

                        // Broadcast success
                        Intent localIntent = new Intent(BroadcastActions.SUCCESS);
                        sendBroadcastWithUUID(localIntent, uuid);

                    }
                })
                .addOnFailureListener(new OnFailureListener() {
                    @Override
                    public void onFailure(@NonNull Exception e) {

                        // Failed to remove geofences
                        Log.e(TAG, "FAILED TO REMOVE GEOFENCES: " + e.toString());

                        // Broadcast failure and return
                        Intent localIntent = new Intent(BroadcastActions.FAILURE);
                        localIntent.putExtra(BroadcastExtras.MESSAGE, getString(R.string.failedToRemoveGeofence));
                        sendBroadcastWithUUID(localIntent, uuid);
                        return;

                    }
                });

    }

    private void stopGeofence(final String uuid, final List<String> requestIds) {

        fenceClient.removeGeofences(requestIds)
                .addOnSuccessListener(new OnSuccessListener<Void>() {
                    @Override
                    public void onSuccess(Void aVoid) {

                        // Geofences removed
                        Log.i(TAG, "GEOFENCES REMOVED.");

                        // Loop through list of ids and remove them
                        for (String id : requestIds) {
                            _geofences.remove(id);
                        }

                    }
                })
                .addOnFailureListener(new OnFailureListener() {
                    @Override
                    public void onFailure(@NonNull Exception e) {
                        // Failed to remove geofences
                        Log.e(TAG, "FAILED TO REMOVE GEOFENCES: " + e.toString());
                    }
                });

    }

    /**
     * Get LocalBroadcastManager
     *
     * @return the LocalBroadcastManager
     */
    private LocalBroadcastManager getLocalBroadcastManager() {
        return LocalBroadcastManager.getInstance(this);
    }

    /**
     * Get LocalBroadcastManager
     *
     * @return the LocalBroadcastManager
     */
    private static LocalBroadcastManager getLocalBroadcastManager(Context context) {
        return LocalBroadcastManager.getInstance(context);
    }
}
