package org.emstrack.ambulance.services;

import android.annotation.SuppressLint;
import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.os.Build;
import android.os.Bundle;
import android.os.IBinder;
import android.support.annotation.Nullable;
import android.support.v4.app.NotificationCompat;
import android.support.v4.app.NotificationManagerCompat;
import android.support.v4.content.LocalBroadcastManager;
import android.text.TextUtils;
import android.util.Log;
import android.util.Pair;
import android.util.SparseArray;
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
import com.google.android.gms.location.LocationSettingsStatusCodes;
import com.google.android.gms.location.SettingsClient;
import com.google.gson.FieldNamingPolicy;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

import org.eclipse.paho.android.service.MqttAndroidClient;
import org.eclipse.paho.client.mqttv3.MqttException;
import org.emstrack.ambulance.LoginActivity;
import org.emstrack.ambulance.R;
import org.emstrack.ambulance.models.AmbulanceAppData;
import org.emstrack.ambulance.util.AmbulanceUpdate;
import org.emstrack.ambulance.util.AmbulanceUpdateFilter;
import org.emstrack.ambulance.util.Geofence;
import org.emstrack.ambulance.util.SparseArrayUtils;
import org.emstrack.models.Ambulance;
import org.emstrack.models.AmbulanceCall;
import org.emstrack.models.Call;
import org.emstrack.models.CallStack;
import org.emstrack.models.Credentials;
import org.emstrack.models.GPSLocation;
import org.emstrack.models.Hospital;
import org.emstrack.models.Location;
import org.emstrack.models.Profile;
import org.emstrack.models.Settings;
import org.emstrack.models.Token;
import org.emstrack.models.Waypoint;
import org.emstrack.models.api.APIService;
import org.emstrack.models.api.APIServiceGenerator;
import org.emstrack.models.api.OnAPICallComplete;
import org.emstrack.models.util.BroadcastActions;
import org.emstrack.models.util.OnComplete;
import org.emstrack.models.util.OnServiceComplete;
import org.emstrack.mqtt.MishandledTopicException;
import org.emstrack.mqtt.MqttProfileCallback;
import org.emstrack.mqtt.MqttProfileClient;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Created by mauricio on 3/18/2018.
 */

public class  AmbulanceForegroundService extends BroadcastService implements MqttProfileCallback {

    final static String TAG = AmbulanceForegroundService.class.getSimpleName();

    // Rate at which locations should be pulled in
    // @ 70 mph gives an accuracy of about 30m
    private final static long UPDATE_INTERVAL = 1 * 1000;
    private final static long FASTEST_UPDATE_INTERVAL = UPDATE_INTERVAL / 2;

    // Group all updates to be done every minute
    private final static long MAX_WAIT_TIME = 60 * 1000;

    // Notification channel
    public final static int NOTIFICATION_ID = 101;
    public final static String PRIMARY_CHANNEL = "default";
    public final static String PRIMARY_CHANNEL_LABEL = "Default channel";

    // Notification id
    private final static AtomicInteger notificationId = new AtomicInteger(NOTIFICATION_ID + 1);

    // SharedPreferences
    public final static String PREFERENCES_NAME = "org.emstrack.ambulance";
    public final static String PREFERENCES_USERNAME = "USERNAME";
    public final static String PREFERENCES_PASSWORD = "PASSWORD";
    public final static String PREFERENCES_MQTT_SERVER = "MQTT_SERVER";
    public final static String PREFERENCES_API_SERVER = "API_SERVER";

    // Server URI
    private static String _serverUri = "ssl://cruzroja.ucsd.edu:8883";
    private static String _serverApiUri = "https://cruzroja.ucsd.edu/";

    private static MqttProfileClient client;
    private static AmbulanceAppData appData;

    private static AmbulanceUpdate _lastLocation;
    private static Date _lastServerUpdate;
    private static boolean _updatingLocation = false;
    private static boolean _canUpdateLocation = false;
    private static ArrayList<Pair<String, String>> _MQTTMessageBuffer = new ArrayList<>();
    private static boolean _reconnecting = false;
    private static boolean _online = false;
    private static ReconnectionInformation _reconnectionInformation;

    // Geofences
    private final static AtomicInteger geofencesId = new AtomicInteger(1);
    private static Map<String, Geofence> _geofences;
    private static float _defaultGeofenceRadius = 100.f;

    private static LocationSettingsRequest locationSettingsRequest;
    private static LocationRequest locationRequest;

    private NotificationManager notificationManager;

    private FusedLocationProviderClient fusedLocationClient;
    private SharedPreferences sharedPreferences;

    public AmbulanceUpdateFilter ambulanceUpdateFilter = new AmbulanceUpdateFilter();

    private LocationCallback locationCallback;
    private GeofencingClient fenceClient;
    private PendingIntent geofenceIntent;

    public class Actions {
        public final static String START_SERVICE = "org.emstrack.ambulance.ambulanceforegroundservice.action.START_SERVICE";
        public final static String LOGIN = "org.emstrack.ambulance.ambulanceforegroundservice.action.LOGIN";
        public final static String GET_AMBULANCE = "org.emstrack.ambulance.ambulanceforegroundservice.action.GET_AMBULANCE";
        public final static String GET_AMBULANCES= "org.emstrack.ambulance.ambulanceforegroundservice.action.GET_AMBULANCES";
        public final static String STOP_AMBULANCES= "org.emstrack.ambulance.ambulanceforegroundservice.action.STOP_AMBULANCES";
        public final static String START_LOCATION_UPDATES = "org.emstrack.ambulance.ambulanceforegroundservice.action.START_LOCATION_UPDATES";
        public final static String STOP_LOCATION_UPDATES = "org.emstrack.ambulance.ambulanceforegroundservice.action.STOP_LOCATION_UPDATES";
        public final static String UPDATE_AMBULANCE_STATUS = "org.emstrack.ambulance.ambulanceforegroundservice.action.UPDATE_AMBULANCE_STATUS";
        public final static String UPDATE_AMBULANCE = "org.emstrack.ambulance.ambulanceforegroundservice.action.UPDATE_AMBULANCE";
        public final static String UPDATE_NOTIFICATION = "org.emstrack.ambulance.ambulanceforegroundservice.action.UPDATE_NOTIFICATION";
        public final static String LOGOUT = "org.emstrack.ambulance.ambulanceforegroundservice.action.LOGOUT";
        public final static String GEOFENCE_START = "org.emstrack.ambulance.ambulanceforegroundservice.action.GEOFENCE_START";
        public final static String GEOFENCE_STOP = "org.emstrack.ambulance.ambulanceforegroundservice.action.GEOFENCE_STOP";
        public final static String GEOFENCE_ENTER = "org.emstrack.ambulance.ambulanceforegroundservice.action.GEOFENCE_ENTER";
        public final static String GEOFENCE_EXIT = "org.emstrack.ambulance.ambulanceforegroundservice.action.GEOFENCE_EXIT";
        public final static String STOP_SERVICE = "org.emstrack.ambulance.ambulanceforegroundservice.action.STOP_SERVICE";
        public final static String CALL_ACCEPT = "org.emstrack.ambulance.ambulanceforegroundservice.action.CALL_ACCEPT";
        public final static String CALL_DECLINE = "org.emstrack.ambulance.ambulanceforegroundservice.action.CALL_DECLINE";
        public final static String CALL_SUSPEND = "org.emstrack.ambulance.ambulanceforegroundservice.action.CALL_SUSPEND";
        public final static String CALL_FINISH = "org.emstrack.ambulance.ambulanceforegroundservice.action.CALL_FINISH";
        public final static String WAYPOINT_ENTER = "org.emstrack.ambulance.ambulanceforegroundservice.action.WAYPOINT_ENTER";
        public final static String WAYPOINT_EXIT = "org.emstrack.ambulance.ambulanceforegroundservice.action.WAYPOINT_EXIT";
        public final static String WAYPOINT_SKIP = "org.emstrack.ambulance.ambulanceforegroundservice.action.WAYPOINT_SKIP";
        public final static String WAYPOINT_ADD = "org.emstrack.ambulance.ambulanceforegroundservice.action.WAYPOINT_ADD";
    }

    public class BroadcastExtras {
        public final static String CALL_ID = "org.emstrack.ambulance.ambulanceforegroundservice.broadcastextras.CALL_ID";
        public final static String AMBULANCE_ID = "org.emstrack.ambulance.ambulanceforegroundservice.broadcastextras.AMBULANCE_ID";
        public final static String HOSPITAL_ID = "org.emstrack.ambulance.ambulanceforegroundservice.broadcastextras.HOSPITAL_ID";
        public final static String WAYPOINT_ID = "org.emstrack.ambulance.ambulanceforegroundservice.broadcastextras.WAYPOINT_ID";
        public final static String WAYPOINT_UPDATE = "org.emstrack.ambulance.ambulanceforegroundservice.broadcastextras.WAYPOINT_UPDATE";
        public final static String RECONNECT = "org.emstrack.ambulance.ambulanceforegroundservice.broadcastextras.RECONNECT";
        public final static String AMBULANCE_UPDATE = "org.emstrack.ambulance.ambulanceforegroundservice.broadcastextras.AMBULANCE_UPDATE";
        public final static String AMBULANCE_UPDATE_ARRAY = "org.emstrack.ambulance.ambulanceforegroundservice.broadcastextras.AMBULANCE_UPDATE_ARRAY";
        public final static String AMBULANCE_STATUS = "org.emstrack.ambulance.ambulanceforegroundservice.broadcastextras.AMBULANCE_STATUS";
        public final static String CREDENTIALS = "org.emstrack.ambulance.ambulanceforegroundservice.broadcastextras.CREDENTIALS";
        public final static String GEOFENCE_TRANSITION = "org.emstrack.ambulance.ambulanceforegroundservice.broadcastextras.GEOFENCE_TRANSTION";
        public final static String GEOFENCE_TYPE = "org.emstrack.ambulance.ambulanceforegroundservice.broadcastextras.GEOFENCE_TYPE";
        public final static String GEOFENCE_LATITUDE = "org.emstrack.ambulance.ambulanceforegroundservice.broadcastextras.GEOFENCE_LATITUDE";
        public final static String GEOFENCE_LONGITUDE = "org.emstrack.ambulance.ambulanceforegroundservice.broadcastextras.GEOFENCE_LONGITUDE";
        public final static String GEOFENCE_RADIUS = "org.emstrack.ambulance.ambulanceforegroundservice.broadcastextras.GEOFENCE_RADIUS";
        public final static String GEOFENCE_REQUESTID = "org.emstrack.ambulance.ambulanceforegroundservice.broadcastextras.GEOFENCE_REQUESTID";
        public final static String GEOFENCE_TRIGGERED = "org.emstrack.ambulance.ambulanceforegroundservice.broadcastextras.GEOFENCE_TRIGGERED";
    }

    public class BroadcastActions {
        public final static String HOSPITALS_UPDATE = "org.emstrack.ambulance.ambulanceforegroundservice.broadcastaction.HOSPITALS_UPDATE";
        public final static String OTHER_AMBULANCES_UPDATE = "org.emstrack.ambulance.ambulanceforegroundservice.broadcastaction.OTHER_AMBULANCES_UPDATE";
        public final static String AMBULANCE_UPDATE = "org.emstrack.ambulance.ambulanceforegroundservice.broadcastaction.AMBULANCE_UPDATE";
        public final static String LOCATION_UPDATE_CHANGE = "org.emstrack.ambulance.ambulanceforegroundservice.broadcastaction.LOCATION_UPDATE_CHANGE";
        public final static String GEOFENCE_EVENT = "org.emstrack.ambulance.ambulanceforegroundservice.broadcastaction.GEOFENCE_EVENT";
        public final static String CONNECTIVITY_CHANGE = "org.emstrack.ambulance.ambulanceforegroundservice.broadcastaction.CONNECTIVITY_CHANGE";
        public final static String PROMPT_CALL_ACCEPT = "org.emstrack.ambulance.ambulanceforegroundservice.broadcastaction.PROMPT_CALL_ACCEPT";
        public final static String PROMPT_CALL_END = "org.emstrack.ambulance.ambulanceforegroundservice.broadcastaction.PROMPT_CALL_END";
        public final static String PROMPT_NEXT_WAYPOINT = "org.emstrack.ambulance.ambulanceforegroundservice.broadcastaction.PROMPT_NEXT_WAYPOINT";
        public final static String CALL_ACCEPTED = "org.emstrack.ambulance.ambulanceforegroundservice.broadcastaction.CALL_ACCEPTED";
        public final static String CALL_DECLINED = "org.emstrack.ambulance.ambulanceforegroundservice.broadcastaction.CALL_DECLINED";
        public final static String CALL_COMPLETED = "org.emstrack.ambulance.ambulanceforegroundservice.broadcastaction.CALL_COMPLETED";
        public final static String CALL_UPDATE = "org.emstrack.ambulance.ambulanceforegroundservice.broadcastaction.CALL_UPDATE";
    }

    public class AmbulanceForegroundServiceException extends Exception {
        public AmbulanceForegroundServiceException(String message) {
            super(message);
        }
    }

    public static class ProfileClientException extends Exception {
        public ProfileClientException(String message) {
            super(message);
        }
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
                    PRIMARY_CHANNEL_LABEL, NotificationManager.IMPORTANCE_LOW);
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
        _geofences = new HashMap<>();

        // Initialize geofence client
        fenceClient = LocationServices.getGeofencingClient(this);

        // initialize app data
        appData = new AmbulanceAppData();

    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {

        // quick return
        if (intent == null) {
            Log.i(TAG, "null intent, return.");
            return START_STICKY;
        }

        // retrieve uuid
        final String uuid = intent.getStringExtra(org.emstrack.models.util.BroadcastExtras.UUID);

        final String action = intent.getAction();
        if (action.equals(Actions.START_SERVICE)) {

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

            Notification.Builder notificationBuilder;
            if (Build.VERSION.SDK_INT >= 26)
                notificationBuilder = new Notification.Builder(AmbulanceForegroundService.this,
                        AmbulanceForegroundService.PRIMARY_CHANNEL);
            else
                notificationBuilder = new Notification.Builder(AmbulanceForegroundService.this);

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
            Intent localIntent = new Intent(org.emstrack.models.util.BroadcastActions.SUCCESS);
            sendBroadcastWithUUID(localIntent, uuid);

        } else if (action.equals(Actions.STOP_SERVICE)) {

            Log.i(TAG, "STOP_SERVICE Foreground Intent");

            // What to do when logout completes?
            new OnServiceComplete(this,
                    org.emstrack.models.util.BroadcastActions.SUCCESS,
                    org.emstrack.models.util.BroadcastActions.FAILURE,
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
                    Intent localIntent = new Intent(org.emstrack.models.util.BroadcastActions.SUCCESS);
                    sendBroadcastWithUUID(localIntent, uuid);

                }

                @Override
                public void onFailure(Bundle extras) {

                    Log.d(TAG, "STOP_SERVICE::onFailure.");

                    // Broadcast failure
                    Intent localIntent = new Intent(org.emstrack.models.util.BroadcastActions.FAILURE);
                    if (extras != null)
                        localIntent.putExtras(extras);
                    sendBroadcastWithUUID(localIntent, uuid);

                }

            }
                    .start();


        } else if (action.equals(Actions.LOGIN)) {

            Log.i(TAG, "LOGIN Foreground Intent ");

            // Retrieve username and password
            String[] loginInfo = intent.getStringArrayExtra(AmbulanceForegroundService.BroadcastExtras.CREDENTIALS);
            final String username = loginInfo[0];
            final String password = loginInfo[1];
            final String server = loginInfo[2];
            final String serverApi = loginInfo[3];

            // Notify user
            Toast.makeText(this,
                    String.format(getString(R.string.loggingIn), username),
                    Toast.LENGTH_SHORT).show();

            // Set online false
            setOnline(false);

            // What to do when login completes?
            new OnServiceComplete(this,
                    org.emstrack.models.util.BroadcastActions.SUCCESS,
                    org.emstrack.models.util.BroadcastActions.FAILURE,
                    null) {

                public void run() {

                    // Login user
                    login(username, password, server, serverApi, getUuid());

                }

                @Override
                public void onSuccess(Bundle extras) {

                    // Set online true
                    setOnline(true);

                    // Update notification
                    updateNotification(getString(R.string.welcomeUser, username));

                    // Broadcast success
                    Intent localIntent = new Intent(org.emstrack.models.util.BroadcastActions.SUCCESS);
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
                    Intent localIntent = new Intent(org.emstrack.models.util.BroadcastActions.FAILURE);
                    if (extras != null)
                        localIntent.putExtras(extras);
                    sendBroadcastWithUUID(localIntent, uuid);

                }

            }
                    .start();

        } else if (action.equals(Actions.LOGOUT)) {

            Log.i(TAG, "LOGOUT Foreground Intent");

            // Set online false
            setOnline(false);

            // logout
            logout(uuid);

        } else if (action.equals(Actions.GET_AMBULANCE)) {

            Log.i(TAG, "GET_AMBULANCE Foreground Intent");

            // Retrieve ambulance
            int ambulanceId = intent.getIntExtra(AmbulanceForegroundService.BroadcastExtras.AMBULANCE_ID, -1);
            boolean reconnect = intent.getBooleanExtra(AmbulanceForegroundService.BroadcastExtras.RECONNECT, false);
            retrieveAmbulance(ambulanceId, uuid, reconnect);

        } else if (action.equals(Actions.GET_AMBULANCES)) {

            Log.i(TAG, "GET_AMBULANCES Foreground Intent");

            // Retrieve ambulances
            boolean reconnect = intent.getBooleanExtra(AmbulanceForegroundService.BroadcastExtras.RECONNECT, false);
            retrieveOtherAmbulances(uuid, reconnect);

        } else if (action.equals(Actions.STOP_AMBULANCES)) {

            Log.i(TAG, "STOP_AMBULANCES Foreground Intent");

            // Stop ambulances
            stopAmbulances(uuid);

        } else if (action.equals(Actions.START_LOCATION_UPDATES)) {

            Log.i(TAG, "START_LOCATION_UPDATES Foreground Intent");

            boolean reconnect = intent.getBooleanExtra(AmbulanceForegroundService.BroadcastExtras.RECONNECT, false);
            startLocationUpdates(uuid, reconnect);

        } else if (action.equals(Actions.STOP_LOCATION_UPDATES)) {

            Log.i(TAG, "STOP_LOCATION_UPDATES Foreground Intent");

            new OnServiceComplete(this,
                    org.emstrack.models.util.BroadcastActions.SUCCESS,
                    org.emstrack.models.util.BroadcastActions.FAILURE,
                    null) {

                @Override
                public void run() {

                    // stop call updates
                    stopCallUpdates(getUuid());

                }

                @Override
                public void onSuccess(Bundle extras) {

                }

                @Override
                public void onFailure(Bundle extras) {
                    super.onFailure(extras);

                    // broadcast failure
                    broadcastFailure("Failed to stop location updates", uuid);

                }

            }.setNext(new OnServiceComplete(this,
                    org.emstrack.models.util.BroadcastActions.SUCCESS,
                    org.emstrack.models.util.BroadcastActions.FAILURE,
                    null) {

                @Override
                public void run() {

                    // stop requesting location updates
                    stopLocationUpdates(uuid);

                }

                @Override
                public void onSuccess(Bundle extras) {

                    // broadcast success
                    broadcastSuccess("Successfully stopped location updates", uuid);

                }

                @Override
                public void onFailure(Bundle extras) {
                    super.onFailure(extras);

                    // broadcast failure
                    broadcastFailure("Failed to stop location updates", uuid);

                }

            })
                    .start();

        } else if (action.equals(Actions.UPDATE_AMBULANCE_STATUS)) {

            Log.i(TAG, "UPDATE_AMBULANCE_STATUS Foreground Intent");

            Bundle bundle = intent.getExtras();

            // Retrieve ambulanceId
            int ambulanceId = bundle.getInt(AmbulanceForegroundService.BroadcastExtras.AMBULANCE_ID);

            // Retrieve updateAmbulance string
            String status = bundle.getString(AmbulanceForegroundService.BroadcastExtras.AMBULANCE_STATUS);
            Log.d(TAG, "AMBULANCE_ID = " + ambulanceId + ", STATUS = " + status);

            // update status
            updateAmbulanceStatus(uuid, ambulanceId, status);

        } else if (action.equals(Actions.UPDATE_AMBULANCE)) {

            Log.i(TAG, "UPDATE_AMBULANCE Foreground Intent");

            Bundle bundle = intent.getExtras();

            // Retrieve ambulanceId
            int ambulanceId = bundle.getInt(AmbulanceForegroundService.BroadcastExtras.AMBULANCE_ID);
            Log.d(TAG, "AMBULANCE_ID = " + ambulanceId);

            // Retrieve updateAmbulance string
            String update = bundle.getString(AmbulanceForegroundService.BroadcastExtras.AMBULANCE_UPDATE);
            if (update != null) {

                // updateAmbulance mqtt server
                updateAmbulance(ambulanceId, update);

            }

            // Retrieve updateAmbulance string array
            ArrayList<String> updateArray = bundle.getStringArrayList(AmbulanceForegroundService.BroadcastExtras.AMBULANCE_UPDATE_ARRAY);
            if (updateArray != null) {

                // updateAmbulance mqtt server
                updateAmbulance(ambulanceId, updateArray);

            }

        } else if (action.equals(Actions.UPDATE_NOTIFICATION)) {

            Log.i(TAG, "UPDATE_NOTIFICATION Foreground Intent");

            // Retrieve notification string
            String message = intent.getStringExtra("MESSAGE");
            if (message != null)
                updateNotification(message);

        } else if (action.equals(Actions.WAYPOINT_ENTER)
                || action.equals(Actions.WAYPOINT_EXIT)
                || action.equals(Actions.WAYPOINT_SKIP)
                || action.equals(Actions.WAYPOINT_ADD)) {

            Log.i(TAG, "WAYPOINT_ENTER/EXIT/SKIP/ADD Foreground Intent");

            Bundle bundle = intent.getExtras();

            // Retrieve waypoint string
            int waypointId = bundle.getInt(AmbulanceForegroundService.BroadcastExtras.WAYPOINT_ID, -1);
            int ambulanceId = bundle.getInt(AmbulanceForegroundService.BroadcastExtras.AMBULANCE_ID);
            int callId = bundle.getInt(AmbulanceForegroundService.BroadcastExtras.CALL_ID);

            // update waypoint on mqtt server
            try {

                Ambulance ambulance = appData.getAmbulance();
                CallStack calls = appData.getCalls();
                Call call = calls.getCurrentCall();
                AmbulanceCall ambulanceCall = call.getCurrentAmbulanceCall();

                if (ambulance.getId() != ambulanceId)
                    throw new Exception("Invalid ambulance id");

                if (call.getId() != callId)
                    throw new Exception("Invalid call id");

                if (action.equals(Actions.WAYPOINT_ADD)) {

                    // New waypoint
                    updateWaypoint(bundle.getString(BroadcastExtras.WAYPOINT_UPDATE),
                            waypointId, ambulance.getId(), call.getId());

                } else {

                    // Existing waypoint
                    Waypoint waypoint = ambulanceCall.getWaypoint(waypointId);

                    if (action.equals(Actions.WAYPOINT_ENTER))
                        updateAmbulanceEnterWaypointStatus(uuid, ambulanceCall, call, waypoint);
                    else if (action.equals(Actions.WAYPOINT_EXIT))
                        updateAmbulanceExitWaypointStatus(uuid, ambulanceCall, call, waypoint);
                    else if (action.equals(Actions.WAYPOINT_SKIP))
                        updateWaypointStatus(Waypoint.STATUS_SKIPPED, waypoint,
                                ambulance.getId(), call.getId());
                }

                // Broadcast success
                Intent localIntent = new Intent(org.emstrack.models.util.BroadcastActions.SUCCESS);
                sendBroadcastWithUUID(localIntent, uuid);

            } catch (Exception e) {

                Log.d(TAG, "WAYPOINT_ENTER/EXIT/SKIP/ADD exception: " + e);

                // Broadcast failure
                Intent localIntent = new Intent(org.emstrack.models.util.BroadcastActions.FAILURE);
                localIntent.putExtra(org.emstrack.models.util.BroadcastExtras.MESSAGE, e.toString());
                sendBroadcastWithUUID(localIntent, uuid);

            }

        } else if (action.equals(Actions.GEOFENCE_START)) {

            Log.i(TAG, "GEOFENCE_START Foreground Intent");

            // Retrieve latitude and longitude
            String type = intent.getStringExtra(AmbulanceForegroundService.BroadcastExtras.GEOFENCE_TYPE);
            Float latitude = intent.getFloatExtra(AmbulanceForegroundService.BroadcastExtras.GEOFENCE_LATITUDE, 0.f);
            Float longitude = intent.getFloatExtra(AmbulanceForegroundService.BroadcastExtras.GEOFENCE_LONGITUDE, 0.f);
            Float radius = intent.getFloatExtra(AmbulanceForegroundService.BroadcastExtras.GEOFENCE_RADIUS, 50.f);

            startGeofence(uuid, new Geofence(new GPSLocation(latitude, longitude), radius, type));

        } else if (action.equals(Actions.GEOFENCE_STOP)) {

            Log.i(TAG, "GEOFENCE_STOP Foreground Intent");

            // Retrieve request ids
            String requestId = intent.getStringExtra(AmbulanceForegroundService.BroadcastExtras.GEOFENCE_REQUESTID);
            List<String> requestIds = new ArrayList<String>();
            requestIds.add(requestId);

            stopGeofence(uuid, requestIds);

        } else if (action.equals(Actions.CALL_ACCEPT)) {

            Log.i(TAG, "CALL_ACCEPT Foreground Intent");

            // get the ambulance that accepted the call and the call id
            int callId = intent.getIntExtra(AmbulanceForegroundService.BroadcastExtras.CALL_ID, -1);

            // next steps to publish information to server (steps 3, 4)
            setAmbulanceCallStatus(callId,
                    AmbulanceCall.statusLabel.get(AmbulanceCall.STATUS_ACCEPTED), uuid);

        } else if (action.equals(Actions.CALL_DECLINE)) {

            Log.i(TAG, "CALL_DECLINE Foreground Intent");

            // get the ambulance that declined the call and the call id
            int callId = intent.getIntExtra(AmbulanceForegroundService.BroadcastExtras.CALL_ID, -1);

            // next steps to publish information to server (steps 3, 4)
            requestToDeclineCurrentCall(callId, uuid);

        } else if (action.equals(Actions.CALL_SUSPEND)) {

            Log.i(TAG, "CALL_SUSPEND Foreground Intent");

            // get the ambulance that suspended the call and the call id
            int callId = intent.getIntExtra(AmbulanceForegroundService.BroadcastExtras.CALL_ID, -1);

            // next steps to publish information to server (steps 3, 4)
            requestToSuspendCurrentCall(callId, uuid);

        } else if (action.equals(Actions.CALL_FINISH)) {

            Log.i(TAG, "CALL_FINISH Foreground Intent");

            // finish call
            requestToFinishCurrentCall(uuid);

        } else if (action.equals(Actions.GEOFENCE_ENTER) ||
                action.equals(Actions.GEOFENCE_EXIT)) {

            Log.i(TAG, "GEOFENCE_ENTER/EXIT Foreground Intent");

            // get list of geofence ids that were entered
            String[] geofences = intent.getStringArrayExtra(AmbulanceForegroundService.BroadcastExtras.GEOFENCE_TRIGGERED);

            // process geofences
            for (String geoId : geofences) {
                Geofence geofence = _geofences.get(geoId);
                replyToGeofenceTransitions(uuid, geofence, action);
            }

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
     *
     * @return the ambulance app data
     */
    public static AmbulanceAppData getAppData() {
        return appData;
    }

    /**
     * @param context the context
     * @return the MqttProfileClient
     */
    protected static MqttProfileClient getProfileClient(Context context) {

        // lazy initialization
        if (client == null) {

            // TODO: This is deprecated. New Firebase service is buggy and poorly documented.
            String clientId = context.getString(R.string.app_version)
                    + context.getString(R.string.client_name)
                    + InstanceID.getInstance(context).getId();
                    //+ UUID.randomUUID().toString();
            MqttAndroidClient androidClient = new MqttAndroidClient(context, _serverUri, clientId);
            client = new MqttProfileClient(androidClient);

        }

        return client;
    }

    /**
     *
     * @return <code>true</code> if client is not null
     */
    public static boolean hasProfileClient() { return client != null; }

    /**
     * @return the last location
     */
    public static android.location.Location getLastLocation() {
        return _lastLocation.getLocation();
    }

    /**
     * @return <code>true</code> if online
     */
    public static boolean isOnline() { return _online; }

    /**
     *
     * @param online <code>true</code> if online
     */
    protected void setOnline(boolean online) {
        setOnline(online, this);
    }

    /**
     *
     * @param online <code>true</code> if online
     * @param context the current context
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
     * @return <code>true</code> if reconnecting
     */
    public static boolean isReconnecting() { return _reconnecting; }

    /**
     * @return <code>true</code> if updating location
     */
    public static boolean isUpdatingLocation() { return _updatingLocation; }

    /**
     * @return <code>true</code> if app can update location
     */
    public static boolean canUpdateLocation() { return _canUpdateLocation; }

    /**
     * @param canUpdateLocation <code>true</code> if app can update location
     */
    public static void setCanUpdateLocation(boolean canUpdateLocation) { AmbulanceForegroundService._canUpdateLocation = canUpdateLocation; }

    /**
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
     * Send updates in bulk for ambulance with id <code>ambulanceId</code> to server
     *
     * @param ambulanceId the ambulance id
     * @param updates a list of {@link AmbulanceUpdate}s
     */
    public boolean updateAmbulance(int ambulanceId, List<AmbulanceUpdate> updates) {

        ArrayList<String> updateString = new ArrayList<>();
        for (AmbulanceUpdate update : updates) {

            if (update.hasLocation())
                // Set last location
                _lastLocation = update;

            // updateAmbulance ambulance string
            updateString.add(update.toUpdateString());

        }

        boolean success = updateAmbulance(ambulanceId, updateString);
        if (!success) {

            // updateAmbulance locally as well
            Ambulance ambulance = getAppData().getAmbulance();
            if (ambulance != null) {

                // Update ambulance
                android.location.Location location = _lastLocation.getLocation();
                ambulance.setLocation(new GPSLocation(location.getLatitude(), location.getLongitude()));
                ambulance.setOrientation(_lastLocation.getBearing());

                // Broadcast ambulance updateAmbulance
                Intent localIntent = new Intent(BroadcastActions.AMBULANCE_UPDATE);
                sendBroadcastWithUUID(localIntent);

            }

        }

        return success;
    }

    /**
     * Send bulk updates for ambulance with id <code>ambulanceId</code> to server
     *
     * @param ambulanceId the ambulance id
     * @param updates a <code>ArrayList</code> of json update <code>String</code>s
     */
    public boolean updateAmbulance(int ambulanceId, ArrayList<String> updates) {

        // Join updates in array
        String updateArray = "[" + TextUtils.join(",", updates) + "]";

        // send to server
        return updateAmbulance(ambulanceId, updateArray);
    }

    /**
     * Send updates for ambulance with id <code>ambulanceId</code> to server
     *
     * @param ambulanceId the ambulance id
     * @param update the update json string
     */
    public boolean updateAmbulance(int ambulanceId, String update) {

        Log.i(TAG, "On updateAmbulance, ambulanceId='" + ambulanceId + "', update='" + update + "'");

        // Form topic and send message
        String topic = String.format("ambulance/%1$d/data", ambulanceId);
        return sendMQTTMessage(topic, update);

    }

    /**
     * Send waypoint status updates to server
     *
     * @param status the status
     * @param waypoint the waypoint
     * @param ambulanceId the ambulance id
     * @param callId the call id
     */
    public void updateWaypointStatus(String status, Waypoint waypoint, int ambulanceId, int callId) {

        Log.d(TAG, "Updating waypoint '" + waypoint.getId() + "' to status '" + status + "'");

        // Update locally
        waypoint.setStatus(status);

        // send to server
        updateWaypoint("{\"status\":\"" + status + "\"}", waypoint.getId(),
                ambulanceId, callId);

    }

    /**
     * Send waypoint updates to server
     *
     * @param update the update json <code>String</code>
     * @param waypointId the waypoint id
     * @param ambulanceId the ambulance id
     * @param callId the call id
     */
    public void updateWaypoint(String update, int waypointId, int ambulanceId, int callId) {

        // Form topic and send message
        String topic = String.format("ambulance/%1$d/call/%2$d/waypoint/%3$d/data",
                ambulanceId, callId, waypointId);
        sendMQTTMessage(topic, update);

    }

    /**
     * Send status updates for ambulance with id <code>ambulanceId</code> to the server
     *
     * @param uuid a unique identifier
     * @param ambulanceId the ambulance id
     * @param status the status
     */
    public void updateAmbulanceStatus(String uuid, int ambulanceId, String status) {
        updateAmbulanceStatus(uuid, ambulanceId, status, new Date());
    }

    /**
     * Send status updates for ambulance with id <code>ambulanceId</code> to the server
     *
     * @param uuid a unique identifier
     * @param ambulanceId the ambulance id
     * @param status the status
     * @param timestamp the timestamp
     */
    public void updateAmbulanceStatus(String uuid, int ambulanceId, String status, Date timestamp) {

        if (ambulanceId == getAppData().getAmbulanceId()) {

            // add status update to ambulanceUpdateFilter
            ambulanceUpdateFilter.update(status, timestamp);

            // Update locally
            Ambulance ambulance = getAppData().getAmbulance();
            ambulance.setStatus(status);

            // Broadcast ambulance updateAmbulance
            Intent localIntent = new Intent(BroadcastActions.AMBULANCE_UPDATE);
            sendBroadcastWithUUID(localIntent, uuid);

        } else {

            // publish status to server
            String update = String.format("{\"status\":\"%1$s\"}", status);
            updateAmbulance(ambulanceId, update);

            // Has ambulance?
            Ambulance ambulance = getAppData().getAmbulance();
            if (ambulance != null) {

                // TODO: Should we modify locally as well?
                // ambulance.getId();

            }
        }
    }

    /**
     * Add message to buffer for later processing
     *
     * @param topic string
     * @param message string
     */
    public void addToMQTTBuffer(String topic, String message) {

        // buffer updates and return
        // TODO: limit size of buffer or write to disk

        _MQTTMessageBuffer.add(new Pair<>(topic, message));

        // Log
        Log.d(TAG, "MQTT Client is not online. Buffering messages...");

    }

    /**
     * Attempt to consume MQTT buffer
     *
     * @return
     */
    public boolean consumeMQTTBuffer() {

        // fast return
        if (_MQTTMessageBuffer.size() == 0)
            return true;

        // Get client, initialize if not present
        final MqttProfileClient profileClient = getProfileClient(AmbulanceForegroundService.this);

        // Log and add notification
        Log.d(TAG, String.format("Attempting to consume MQTT buffer of size %1$d entries.", _MQTTMessageBuffer.size()));

        // Loop through buffer unless it failed
        Iterator<Pair<String,String>> iterator = _MQTTMessageBuffer.iterator();
        boolean success = true;
        while (success && iterator.hasNext()) {

            // Retrieve updateAmbulance and remove from buffer
            Pair<String, String> message = iterator.next();
            iterator.remove();

            // updateAmbulance ambulance
            success = sendMQTTMessage(message.first, message.second);

        }

        return success;

    }

    /**
     * Send message to MQTT or buffer it when not online
     *
     * @param topic string
     * @param message string
     */
    public boolean sendMQTTMessage(String topic, String message) {

        // TODO: Allow qos and retained to be passed on. Is that necessary?
        Log.d(TAG, "On sendMQTTMessage, topic='" + topic + "', message='" + message + "'");

        String error = "";
        try {

            // is not online or not connected?
            final MqttProfileClient profileClient = getProfileClient(AmbulanceForegroundService.this);
            if (!isOnline() || !profileClient.isConnected()) {

                // add to buffer and return
                addToMQTTBuffer(topic, message);
                return false;

            }

            // Otherwise, publish to MQTT
            profileClient.publish(String.format("user/%1$s/client/%2$s/%3$s",
                        profileClient.getUsername(), profileClient.getClientId(), topic),
                        message, 2, false);

            // Set updateAmbulance time
            _lastServerUpdate = new Date();

            return true;

        } catch (MqttException e) {
            error += "\n" + e.toString();
        } catch (Exception e) {
            error += "\n" + e.toString();
        }

        // Log and build a notification in case of error
        Log.i(TAG,message);

        // Create notification
        NotificationCompat.Builder mBuilder = new NotificationCompat.Builder(this, PRIMARY_CHANNEL)
                .setSmallIcon(R.mipmap.ic_launcher)
                .setContentTitle("EMSTrack")
                .setContentText(error)
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
        if (!consumeMQTTBuffer()) {

            // could not consume entire buffer, log and empty anyway
            Log.e(TAG, "Could not empty buffer before logging out.");
            _MQTTMessageBuffer = new ArrayList<>();

        }

        // get profile client
        MqttProfileClient profileClient = getProfileClient(this);

        new OnServiceComplete(this,
                org.emstrack.models.util.BroadcastActions.SUCCESS,
                org.emstrack.models.util.BroadcastActions.FAILURE,
                null) {

            @Override
            public void run() {

                // remove current ambulance
                removeAmbulance(getUuid(), false);

            }

            @Override
            public void onSuccess(Bundle extras) {

                // remove hospital map
                removeHospitals();

                // remove ambulance map
                removeOtherAmbulances();

                // stop geofences
                // TODO: Should we wait for completion?
                removeAllGeofences(null);

                try {

                    // disconnect mqttclient
                    profileClient.disconnect(new MqttProfileCallback() {

                        @Override
                        public void onReconnect() {

                            Log.d(TAG, "onReconnect during disconnect. Should never happen.");

                        }

                        @Override
                        public void onSuccess() {

                            // Broadcast success
                            broadcastSuccess("Successfully disconnected from broker.", uuid);

                        }

                        @Override
                        public void onFailure(Throwable t) {

                            // Broadcast failure
                            broadcastFailure(getString(R.string.failedToDisconnectFromBroker), uuid, t);

                        }

                    });

                } catch (Exception t) {

                    // Broadcast failure
                    broadcastFailure(getString(R.string.failedToDisconnectFromBroker), uuid, t);

                }


            }

            @Override
            public void onFailure(Bundle extras) {
                super.onFailure(extras);

                // Broadcast failure
                broadcastFailure(getString(R.string.couldNotLogout), uuid);

            }

        }
                .start();

    }

    @Override
    public void onReconnect() {

        Log.d(TAG, "onReconnect.");

        // Suppress changes in updating location until reconnect is complete
        _reconnecting = true;

        // Store reconnection information
        _reconnectionInformation = new ReconnectionInformation(
                appData.getAmbulance() != null,
                appData.getAmbulances().size() > 0,
                appData.getHospitals().size() > 0,
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
     * APICallback after handling successful connection
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

            final int ambulanceId = appData.getAmbulance().getId();
            final String ambulanceIdentifier = appData.getAmbulance().getIdentifier();

            // Remove current ambulance
            // TODO: Does it need to be asynchrounous?
            removeAmbulance(null, true);

            // Retrieve ambulance
            Intent ambulanceIntent = new Intent(this, AmbulanceForegroundService.class);
            ambulanceIntent.setAction(Actions.GET_AMBULANCE);
            ambulanceIntent.putExtra(AmbulanceForegroundService.BroadcastExtras.AMBULANCE_ID, ambulanceId);
            ambulanceIntent.putExtra(AmbulanceForegroundService.BroadcastExtras.RECONNECT, true);

            // What to do when GET_AMBULANCE service completes?
            new OnServiceComplete(this,
                    BroadcastActions.AMBULANCE_UPDATE,
                    org.emstrack.models.util.BroadcastActions.FAILURE,
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
                        Ambulance ambulance = getAppData().getAmbulance();
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

                            // ambulance is available, request updateAmbulance
                            Log.d(TAG, "Ambulance is available: requesting updates.");

                            // Update location_client on server
                            new OnServiceComplete(AmbulanceForegroundService.this,
                                    BroadcastActions.AMBULANCE_UPDATE,
                                    org.emstrack.models.util.BroadcastActions.FAILURE,
                                    null) {

                                public void run() {

                                    // updateAmbulance ambulance
                                    String payload = String.format("{\"location_client_id\":\"%1$s\"}", clientId);
                                    updateAmbulance(ambulanceId, payload);

                                }

                                @Override
                                public void onSuccess(Bundle extras) {

                                    // TODO: Can fail

                                    // ambulance is available, request updateAmbulance
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
                                        intent.putExtra(org.emstrack.models.util.BroadcastExtras.UUID, getUuid());

                                    // Call super
                                    super.onReceive(context, intent);
                                }

                            }
                                    .start();

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
                        ambulanceIntent.putExtra(AmbulanceForegroundService.BroadcastExtras.RECONNECT, true);

                    }

                    if (_reconnectionInformation.hasHospitals()) {

                        Log.i(TAG, "Subscribing to hospitals.");

                        // Remove hospitals
                        // TODO: Does it need to be asynchrounous?
                        retrieveHospitals(getUuid(),true);

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
                            ambulanceIdentifier))
                    .start();

        } else {

            // clear reconnecting flag
            _reconnecting = false;

        }

    }

    /**
     * APICallback after handling successful connection
     *
     * @param exception
     */
    @Override
    public void onFailure(Throwable exception) {

        // Mishandled topics are ignored and logged
        if (exception instanceof MishandledTopicException) {

            Log.d(TAG, exception.toString());
            return;
        }

        // Other exceptions
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
                    username, profileClient.getClientId()),1,
                    (topic, message) -> {

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
    public void login(final String username, final String password, final String serverUri, final String serverApi, final String uuid) {

        // Retrieve current profile client
        MqttProfileClient profileClient = getProfileClient(this);

        // Has server changed?
        final String serverURI;
        if (serverUri != null && !serverUri.isEmpty())
            serverURI = serverUri;
        else
            serverURI = _serverUri;

        final boolean serverChange;
        if (serverURI.equals(profileClient.getServerURI()))
            serverChange = false;
        else
            serverChange = true;

        // Logout first
        new OnServiceComplete(this,
                org.emstrack.models.util.BroadcastActions.SUCCESS,
                org.emstrack.models.util.BroadcastActions.FAILURE,
                null) {

            public void run() {

                // logout
                logout(getUuid());

            }

            @Override
            public void onSuccess(Bundle extras) {

                // Initialize credentials
                Credentials credentials = new Credentials(username, password, serverApi, serverURI);

                // Initialize appData
                appData = new AmbulanceAppData(credentials);

                // Has server changed?
                if (serverChange) {
                    // invalidate server and set new uri before connecting
                    client = null;
                    _serverUri = serverURI;
                    _serverApiUri = serverApi;
                    Log.d(TAG,"Server has changed. Invalidating current client");
                } else
                    Log.d(TAG,"Server has not changed.");

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
                        editor.putString(PREFERENCES_MQTT_SERVER, serverURI);
                        editor.putString(PREFERENCES_API_SERVER, serverApi);
                        editor.apply();

                        // Subscribe to error topic
                        subscribeToError(username);

                        // Retrieve token
                        APIServiceGenerator.setServerUri(credentials.getApiServerUri());
                        APIServiceGenerator.setToken(null);
                        APIService service = APIServiceGenerator.createService(APIService.class);
                        retrofit2.Call<Token> call = service.getToken(credentials);
                        new OnAPICallComplete<Token>(call) {

                            @Override
                            public void onSuccess(Token token) {

                                // save token
                                APIServiceGenerator.setToken(token.getToken());
                                appData.setToken(token);

                                Log.d(TAG, "Retrieving profile, settings, bases, hospitals, and ambulances...");

                                // Retrieve profile
                                APIService service = APIServiceGenerator.createService(APIService.class);
                                retrofit2.Call<Profile> profileCall = service.getProfile(username);
                                retrofit2.Call<Settings> settingsCall = service.getSettings();
                                retrofit2.Call<List<Location>> basesCall = service.getLocationsByType("Base");
                                new OnAPICallComplete<Profile>(profileCall) {

                                    @Override
                                    public void onSuccess(Profile profile) {

                                        Log.d(TAG, "Got profile");

                                        // Sort ambulances and hospitals
                                        profile.sortAmbulances();
                                        profile.sortHospitals();

                                        // save profile
                                        appData.setProfile(profile);

                                    }

                                    @Override
                                    public void onFailure(Throwable t) {

                                        // Broadcast failure
                                        broadcastFailure("Could not retrieve profile", uuid, t);

                                    }

                                }.setNext(new OnAPICallComplete<Settings>(settingsCall) {

                                    @Override
                                    public void onSuccess(Settings settings) {

                                        Log.d(TAG, "Got settings");

                                        // save profile
                                        appData.setSettings(settings);

                                    }

                                    @Override
                                    public void onFailure(Throwable t) {

                                        // Broadcast failure
                                        broadcastFailure("Could not retrieve settings", uuid, t);

                                    }

                                }.setNext(new OnAPICallComplete<List<Location>>(basesCall) {

                                    @Override
                                    public void onSuccess(List<Location> locations) {

                                        Log.d(TAG, String.format("Got %1$d bases", locations.size()));

                                        // sort bases
                                        Collections.sort(locations, (a, b) -> a.getName().compareTo(b.getName()) );

                                        // save bases
                                        appData.setBases(locations);

                                    }

                                    @Override
                                    public void onFailure(Throwable t) {

                                        // Broadcast failure
                                        broadcastFailure("Could not retrieve bases", uuid, t);

                                    }

                                }.setNext(new OnServiceComplete(AmbulanceForegroundService.this,
                                        org.emstrack.models.util.BroadcastActions.SUCCESS,
                                        org.emstrack.models.util.BroadcastActions.FAILURE,
                                        null) {

                                    @Override
                                    public void run() {

                                        // Retrieve hospitals
                                        retrieveHospitals(getUuid(), false);

                                    }

                                    @Override
                                    public void onSuccess(Bundle extras) {

                                    }

                                    @Override
                                    public void onFailure(Bundle extras) {

                                        // Broadcast failure
                                        broadcastFailure("Could not retrieve hospitals", uuid);

                                    }

                                }.setNext(new OnComplete() {

                                    @Override
                                    public void run() {

                                        // Broadcast success
                                        broadcastSuccess("Login succeeded.", uuid);

                                    }

                                }))))
                                        .start();

                            }

                            @Override
                            public void onFailure(Throwable t) {

                                // Broadcast failure
                                broadcastFailure("Could not retrieve API token", uuid, t);

                            }

                        }.start();

                        // set callback for handling loss of connection/reconnection
                        getProfileClient(AmbulanceForegroundService.this).setCallback(AmbulanceForegroundService.this);

                    }

                    @Override
                    public void onFailure(Throwable t) {

                        // Broadcast failure
                        broadcastFailure("Failed to retrieve profile", uuid, t);

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
                        public void onFailure(Throwable t) {

                            String message = getString(R.string.failedToConnectToBrocker);

                            if (t instanceof MqttException) {

                                int reason = ((MqttException) t).getReasonCode();
                                if (reason == MqttException.REASON_CODE_FAILED_AUTHENTICATION ||
                                        reason == MqttException.REASON_CODE_NOT_AUTHORIZED ||
                                        reason == MqttException.REASON_CODE_INVALID_CLIENT_ID) {

                                    message += getResources().getString(R.string.error_invalid_credentials);

                                    // Broadcast failure
                                    broadcastFailure(message, uuid);

                                    return;

                                }

                            }

                            // Broadcast failure
                            broadcastFailure(message, uuid, t);

                        }

                    });

                } catch (MqttException t) {

                    // Broadcast failure
                    broadcastFailure(getString(R.string.error_connection_failed), uuid, t);

                }

            }

            @Override
            public void onFailure(Bundle extras) {

                // Broadcast failure
                Intent localIntent = new Intent(org.emstrack.models.util.BroadcastActions.FAILURE);
                if (extras != null)
                    localIntent.putExtras(extras);
                sendBroadcastWithUUID(localIntent, uuid);

            }

        }
                .start();

    }

    /**
     * Retrieve ambulance
     *
     * @param ambulanceId the ambulance id
     */
    public void retrieveAmbulance(final int ambulanceId, final String uuid, final boolean reconnect) {

        Log.d(TAG, "retrieveAmbulance");

        // Is ambulance id valid?
        if (ambulanceId < 0) {

            // Broadcast failure
            broadcastFailure(getString(R.string.invalidAmbulanceId), uuid);
            return;
        }

        // Is ambulance new and not reconnect?
        Ambulance ambulance = getAppData().getAmbulance();
        if (!reconnect && ambulance != null && ambulance.getId() == ambulanceId) {
            return;
        }

        // Retrieve client
        MqttProfileClient profileClient = getProfileClient(this);

        // Retrieve ambulance data call
        APIService service = APIServiceGenerator.createService(APIService.class);
        retrofit2.Call<Ambulance> ambulanceCall = service.getAmbulance(ambulanceId);

        new OnServiceComplete(this,
                org.emstrack.models.util.BroadcastActions.SUCCESS,
                org.emstrack.models.util.BroadcastActions.FAILURE,
                null) {

            @Override
            public void run() {

                Log.d(TAG, "Removing current ambulance");

                // remove current ambulance
                removeAmbulance(getUuid(), reconnect);

            }

            @Override
            public void onSuccess(Bundle extras) {

                // Remove other ambulances
                // TODO: Does it need to be asynchrounous?
                removeOtherAmbulances(reconnect);

                // Login to ambulance
                String clientId = profileClient.getClientId();
                try {

                    // Publish ambulance login
                    String topic = String.format("user/%1$s/client/%2$s/ambulance/%3$s/status",
                            profileClient.getUsername(), clientId, ambulanceId);
                    profileClient.publish(topic, "ambulance login",2,true);

                } catch (MqttException e) {

                    // Broadcast failure
                    broadcastFailure(getString(R.string.couldNotLoginToAmbulance), uuid);

                    // Set as unsuccessful
                    setSuccess(false);

                }

            }

            @Override
            public void onFailure(Bundle extras) {
                super.onFailure(extras);

                // Broadcast failure
                broadcastFailure(getString(R.string.couldNotLoginToAmbulance), uuid);

            }

        }.setNext(new OnAPICallComplete<Ambulance>(ambulanceCall) {

            @Override
            public void onSuccess(Ambulance ambulance) {

                Log.d(TAG, "Got ambulance");

                // Set current ambulance
                appData.setAmbulance(ambulance);

            }

            @Override
            public void onFailure(Throwable t) {

                // Broadcast failure
                broadcastFailure("Could not retrieve ambulance", uuid, t);

            }

        }.setNext(new OnComplete() {

            @Override
            public void run() {

                // Subscribe and wait for first update
                new OnServiceComplete(AmbulanceForegroundService.this,
                        BroadcastActions.AMBULANCE_UPDATE,
                        org.emstrack.models.util.BroadcastActions.FAILURE,
                        null) {

                    @Override
                    public void run() {

                        try {

                            // Subscribe to ambulance data
                            profileClient.subscribe(String.format("ambulance/%1$d/data", ambulanceId), 1,
                                    (topic, message) -> {

                                        Log.d(TAG, "Current ambulance update");

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
                                                _lastLocation = new AmbulanceUpdate();
                                                android.location.Location location = new android.location.Location("FusedLocationClient");
                                                location.setLatitude(ambulance.getLocation().getLatitude());
                                                location.setLongitude(ambulance.getLocation().getLongitude());
                                                _lastLocation.setLocation(location);
                                                _lastLocation.setBearing(ambulance.getOrientation());
                                                _lastLocation.setTimestamp(ambulance.getTimestamp());

                                            }

                                            // Set current ambulance
                                            appData.setAmbulance(ambulance);

                                            // stop updates?
                                            MqttProfileClient profileClient1 = getProfileClient(AmbulanceForegroundService.this);
                                            String clientId = profileClient1.getClientId();
                                            if (isUpdatingLocation() &&
                                                    (ambulance.getLocationClientId() == null ||
                                                            !clientId.equals(ambulance.getLocationClientId()))) {

                                                // turn off tracking
                                                Intent localIntent = new Intent(AmbulanceForegroundService.this, AmbulanceForegroundService.class);
                                                localIntent.setAction(Actions.STOP_LOCATION_UPDATES);
                                                startService(localIntent);

                                            }

                                            // Broadcast ambulance update
                                            Intent localIntent = new Intent(BroadcastActions.AMBULANCE_UPDATE);
                                            localIntent.putExtra(BroadcastExtras.AMBULANCE_ID, ambulance.getId());
                                            getLocalBroadcastManager().sendBroadcast(localIntent);

                                        } catch (Exception e) {

                                            Log.i(TAG, "Could not parse ambulance update.");

                                            // Broadcast failure
                                            broadcastFailure(getString(R.string.couldNotParseAmbulance));

                                        }

                                    });

                        } catch (MqttException e) {

                            Log.d(TAG, "Could not subscribe to ambulance data");

                            // Broadcast failure
                            broadcastFailure(getString(R.string.couldNotSubscribeToAmbulance), getUuid());
                            return;

                        }

                    }

                    @Override
                    public void onSuccess(Bundle extras) {

                        // Broadcast success
                        broadcastSuccess("Successfully retrieved ambulance.", uuid);


                    }

                    @Override
                    public void onFailure(Bundle extras) {

                        // Broadcast failure
                        broadcastFailure(extras.getString(org.emstrack.models.util.BroadcastExtras.MESSAGE), uuid);

                    }

                }
                        .setSuccessIdCheck(false) // AMBULANCE_UPDATE will have a different UUID
                        .setFailureIdCheck(false) // FAILURE may have a different UUID
                        .start();

            }


        }))
                .start();

    }

    /**
     * Remove current ambulance
     */
    public void removeAmbulance(final String uuid) { removeAmbulance(uuid, false); }

    /**
     * Remove current ambulance
     */
    public void removeAmbulance(final String uuid, boolean reconnect) {

        Log.d(TAG, "removeAmbulance");

        Ambulance ambulance = getAppData().getAmbulance();
        if (ambulance == null ) {

            // broadcast success
            broadcastSuccess("No ambulance to remove", uuid);

            // and return
            return;
        }

        // Logout and unsubscribe if not a reconnect
        if (!reconnect) {

            // Retrieve client
            final MqttProfileClient profileClient = getProfileClient(this);

            new OnServiceComplete(this,
                    org.emstrack.models.util.BroadcastActions.SUCCESS,
                    org.emstrack.models.util.BroadcastActions.FAILURE,
                    null) {

                @Override
                public void run() {

                    // stop call updates
                    stopCallUpdates(getUuid());

                }

                @Override
                public void onSuccess(Bundle extras) {

                }

                @Override
                public void onFailure(Bundle extras) {
                    super.onFailure(extras);

                    // broadcast failure
                    broadcastFailure("Failed to stop location updates", uuid);

                }

            }.setNext(new OnServiceComplete(this,
                    org.emstrack.models.util.BroadcastActions.SUCCESS,
                    org.emstrack.models.util.BroadcastActions.FAILURE,
                    null) {

                @Override
                public void run() {

                    // remove location updates
                    stopLocationUpdates(getUuid());

                }

                @Override
                public void onSuccess(Bundle extras) {

                    // get ambulance id
                    int ambulanceId = ambulance.getId();

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

                    } catch (MqttException exception) {

                        Log.d(TAG, "Could not unsubscribe to 'ambulance/" + ambulanceId + "/data'");

                    }

                    // Remove ambulance
                    appData.setAmbulance(null);

                    // broadcast success
                    broadcastSuccess("Successfully removed ambulance", uuid);

                }

                @Override
                public void onFailure(Bundle extras) {
                    super.onFailure(extras);

                    // broadcast failure
                    broadcastFailure(getString(R.string.couldNotRemoveAmbulance), uuid);

                }

            })
                    .start();


        } else {

            // broadcast success
            broadcastSuccess("Reconnect", uuid);

        }

    }

    /**
     * Retrieve hospitals
     */
    public void retrieveHospitals(final String uuid, boolean reconnect) {

        // Remove current hospital map
        // TODO: Does it need to be asynchrounous?
        removeHospitals(reconnect);

        Log.d(TAG, "Retrieving hospitals...");

        // Retrieve client
        final MqttProfileClient profileClient = getProfileClient(this);

        // Retrieve hospitals data
        APIService service = APIServiceGenerator.createService(APIService.class);
        retrofit2.Call<List<Hospital>> hospitalsCall = service.getHospitals();
        new OnAPICallComplete<List<Hospital>>(hospitalsCall) {

            @Override
            public void onSuccess(List<Hospital> hospitals) {

                Log.d(TAG, "Got hospitals");

                // Set current hospitals
                appData.setHospitals(hospitals);

            }

            @Override
            public void onFailure(Throwable t) {

                // Broadcast failure
                broadcastFailure(getString(R.string.couldNotRetrieveAmbulances), uuid, t);

            }

        }.setNext(new OnComplete() {
            @Override
            public void run() {

                // Loop over all hospitals
                for (Hospital hsptl : SparseArrayUtils.iterable(appData.getHospitals())) {

                    final int hospitalId = hsptl.getId();

                    try {

                        // Subscribe to hospital
                        profileClient.subscribe("hospital/" + hospitalId + "/data",1,
                                (topic, message) -> {

                                    try {

                                        // Parse hospital
                                        GsonBuilder gsonBuilder = new GsonBuilder();
                                        gsonBuilder.setFieldNamingPolicy(FieldNamingPolicy.LOWER_CASE_WITH_UNDERSCORES);
                                        Gson gson = gsonBuilder.create();

                                        // Found hospital
                                        Hospital hospital = gson.fromJson(message.toString(), Hospital.class);

                                        // Add to hospital map
                                        appData.getHospitals().put(hospital.getId(), hospital);

                                        // Broadcast hospitals updateAmbulance
                                        Intent localIntent = new Intent(BroadcastActions.HOSPITALS_UPDATE);
                                        localIntent.putExtra(BroadcastExtras.HOSPITAL_ID, hospital.getId());
                                        getLocalBroadcastManager().sendBroadcast(localIntent);

                                    } catch (Exception e) {

                                        // Broadcast failure
                                        broadcastFailure(getString(R.string.couldNotParseHospitalUpdate));

                                    }

                                });

                    } catch (MqttException e) {

                        // Broadcast failure
                        broadcastFailure(getString(R.string.couldNotSubscribeToHospital), uuid);

                    }

                }

                // Broadcast success
                broadcastSuccess("Successfully subscribed to hospitals", uuid);

            }
        })
                .start();

    }

    /**
     * Remove current hospitals
     */
    public void removeHospitals() { removeHospitals(false); }

    /**
     * Remove current hospitals
     */
    public void removeHospitals(boolean reconnect) {

        SparseArray<Hospital> hospitals = appData.getHospitals();
        if (hospitals.size() == 0) {
            Log.i(TAG, "No hospital to remove.");
            return;
        }

        // Unsubscribe only if not reconnect
        if (!reconnect) {

            // Retrieve client
            final MqttProfileClient profileClient = getProfileClient(this);

            // Loop over all hospitals
            for (Hospital hospital : SparseArrayUtils.iterable(hospitals)) {

                try {

                    // Unsubscribe to hospital data
                    profileClient.unsubscribe("hospital/" + hospital.getId() + "/data");

                } catch (MqttException exception) {
                    Log.d(TAG, "Could not unsubscribe to 'hospital/" + hospital.getId() + "/data'");
                }

            }

            // Remove hospitals
            appData.setHospitals(new SparseArray<>());

        }

    }

    /**
     * Retrieve ambulances
     */
    public void retrieveOtherAmbulances(final String uuid, boolean reconnect) {

        // Remove current ambulance map
        // TODO: Does it need to be asynchrounous?
        removeOtherAmbulances(reconnect);

        Log.d(TAG, "Retrieving ambulances...");

        // Retrieve ambulance data
        final MqttProfileClient profileClient = getProfileClient(this);

        // Retrieve ambulances data
        APIService service = APIServiceGenerator.createService(APIService.class);
        retrofit2.Call<List<Ambulance>> ambulancesCall = service.getAmbulances();
        new OnAPICallComplete<List<Ambulance>>(ambulancesCall) {

            @Override
            public void onSuccess(List<Ambulance> ambulances) {

                Log.d(TAG, "Got ambulances");

                // Current ambulance id
                int currentAmbulanceId = getAppData().getAmbulanceId();

                // Loop over all ambulances
                SparseArray<Ambulance> ambulanceArray = new SparseArray<>();
                for (Ambulance ambulance: ambulances) {

                    // Skip current ambulance
                    if (ambulance.getId() != currentAmbulanceId)
                        ambulanceArray.put(ambulance.getId(), ambulance);

                }

                // Set current ambulances
                appData.setAmbulances(ambulanceArray);

            }

            @Override
            public void onFailure(Throwable t) {

                // Broadcast failure
                broadcastFailure(getString(R.string.couldNotRetrieveAmbulances), uuid, t);

            }

        }.setNext(new OnComplete() {
            @Override
            public void run() {

                // Loop over all ambulances
                for (Ambulance amblnc : SparseArrayUtils.iterable(appData.getAmbulances())) {

                    final int ambulanceId = amblnc.getId();

                    try {

                        // Start retrieving data
                        profileClient.subscribe("ambulance/" + ambulanceId + "/data",1,
                                (topic, message) -> {

                                    try {

                                        // Parse ambulance
                                        GsonBuilder gsonBuilder = new GsonBuilder();
                                        gsonBuilder.setFieldNamingPolicy(FieldNamingPolicy.LOWER_CASE_WITH_UNDERSCORES);
                                        Gson gson = gsonBuilder.create();

                                        // Found ambulance
                                        final Ambulance ambulance = gson.fromJson(message.toString(), Ambulance.class);

                                        // Update ambulance map
                                        appData.getAmbulances().put(ambulanceId, ambulance);

                                        // Broadcast ambulances updateAmbulance
                                        Intent localIntent = new Intent(BroadcastActions.OTHER_AMBULANCES_UPDATE);
                                        localIntent.putExtra(BroadcastExtras.AMBULANCE_ID, ambulance.getId());
                                        getLocalBroadcastManager().sendBroadcast(localIntent);

                                    } catch (Exception e) {

                                        // Broadcast failure
                                        broadcastFailure(getString(R.string.couldNotParseAmbulance));

                                    }

                                });

                    } catch (MqttException e) {

                        // Broadcast failure
                        broadcastFailure(getString(R.string.couldNotSubscribeToAmbulance), uuid);

                    }

                }

                // Broadcast success
                broadcastSuccess("Successfully subscribed to ambulances", uuid);

            }
        })
                .start();

    }

    /**
     * Remove current ambulances
     */
    public void removeOtherAmbulances() { removeOtherAmbulances(false); }

    /**
     * Remove current ambulances
     */
    public void removeOtherAmbulances(boolean reconnect) {

        SparseArray<Ambulance> ambulances = appData.getAmbulances();
        if (ambulances.size() == 0) {
            Log.i(TAG, "No ambulances to remove.");
            return;
        }

        // Remove subscriptions if not reconnect
        if (!reconnect) {

            // Retrieve client
            final MqttProfileClient profileClient = getProfileClient(this);

            // Loop over all ambulances
            for (Ambulance ambulance : SparseArrayUtils.iterable(ambulances)) {

                try {

                    // Unsubscribe to ambulance data
                    profileClient.unsubscribe("ambulance/" + ambulance.getId() + "/data");

                } catch (MqttException exception) {
                    Log.d(TAG, "Could not unsubscribe to 'ambulance/" + ambulance.getId() + "/data'");
                }

            }

            // Remove ambulances
            appData.setAmbulances(new SparseArray<>());

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
        Intent localIntent = new Intent(org.emstrack.models.util.BroadcastActions.SUCCESS);
        sendBroadcastWithUUID(localIntent, uuid);

    }

    private void startLocationUpdates(final String uuid, final boolean reconnect) {

        // Already started?
        if (_updatingLocation) {
            Log.i(TAG, "Already requesting location updates. Skipping.");

            Log.i(TAG, "Consume buffer.");
            consumeMQTTBuffer();

            // Broadcast success and return
            broadcastSuccess("Already requesting location updates", uuid);
            return;

        }

        if (!canUpdateLocation()) {

            // Broadcast failure and return
            broadcastFailure(getString(R.string.cannotUseLocationServices), uuid);
            return;

        }

        // Logged in?
        Ambulance ambulance = AmbulanceForegroundService.getAppData().getAmbulance();
        if (ambulance == null) {

            // Broadcast failure and return
            broadcastFailure(getString(R.string.noAmbulanceSelected), uuid);
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
                    .addOnSuccessListener(
                            locationSettingsResponse -> {

                                Log.i(TAG, "All location settings are satisfied.");

                                Log.i(TAG, "Consume buffer.");
                                consumeMQTTBuffer();

                                Log.i(TAG, "Starting location updates.");
                                beginLocationUpdates(uuid);

                                Log.i(TAG, "Starting call updates.");
                                beginCallUpdates(uuid);

                            })
                    .addOnFailureListener(
                            e -> {
                                int statusCode = ((ApiException) e).getStatusCode();
                                String message = getString(R.string.inadequateLocationSettings);
                                switch (statusCode) {
                                    case LocationSettingsStatusCodes.RESOLUTION_REQUIRED:
                                        message += "Try restarting app.";
                                        break;
                                    case LocationSettingsStatusCodes.SETTINGS_CHANGE_UNAVAILABLE:
                                        message += "Please fix in Settings.";
                                }
                                broadcastFailure(message, uuid);

                            });


        } else if (ambulanceLocationClientId == null) {

            // ambulance is available, request updateAmbulance

            // Update location_client on server, listening to updates already
            String payload = String.format("{\"location_client_id\":\"%1$s\"}", clientId);
            Intent intent = new Intent(this, AmbulanceForegroundService.class);
            intent.setAction(Actions.UPDATE_AMBULANCE);
            Bundle bundle = new Bundle();
            bundle.putInt(AmbulanceForegroundService.BroadcastExtras.AMBULANCE_ID, ambulance.getId());
            bundle.putString(AmbulanceForegroundService.BroadcastExtras.AMBULANCE_UPDATE, payload);
            intent.putExtras(bundle);

            // What to do when service completes?
            new OnServiceComplete(this,
                    BroadcastActions.AMBULANCE_UPDATE,
                    org.emstrack.models.util.BroadcastActions.FAILURE,
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

                    Log.i(TAG, "onFailure");

                    // Broadcast failure
                    String message = extras.getString(org.emstrack.models.util.BroadcastExtras.MESSAGE);
                    Intent localIntent = new Intent(org.emstrack.models.util.BroadcastActions.FAILURE);
                    localIntent.putExtra(org.emstrack.models.util.BroadcastExtras.MESSAGE, message);
                    sendBroadcastWithUUID(localIntent, uuid);

                }

            }
                    .setSuccessIdCheck(false) // AMBULANCE_UPDATE will have a different UUID
                    .start();

        } else {

            Log.i(TAG, "Ambulance is not available");

            // ambulance is not available, Broadcast failure
            Intent localIntent = new Intent(org.emstrack.models.util.BroadcastActions.FAILURE);
            localIntent.putExtra(org.emstrack.models.util.BroadcastExtras.MESSAGE, getString(R.string.anotherClientReporting));
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

                // Retrieve results
                if (result != null) {

                    // get profile client, initialize if not present
                    final MqttProfileClient profileClient = AmbulanceForegroundService.getProfileClient(AmbulanceForegroundService.this);

                    // retrieve ambulance
                    Ambulance ambulance = getAppData().getAmbulance();
                    if (ambulance != null) {

                        List<android.location.Location> locations = result.getLocations();
                        Log.i(TAG, "Received " + locations.size() + " location updates");

                        // Initialize ambulanceUpdateFilter
                        if (_lastLocation != null)
                            ambulanceUpdateFilter.setCurrentAmbulanceUpdate(_lastLocation);

                        // Filter locations
                        ambulanceUpdateFilter.update(locations);

                        // Publish updateAmbulance
                        if (ambulanceUpdateFilter.hasUpdates()) {

                            // Sort updates
                            ambulanceUpdateFilter.sort();

                            // update server or buffer
                            updateAmbulance(ambulance.getId(), ambulanceUpdateFilter.getFilteredAmbulanceUpdates());

                            // reset filter
                            ambulanceUpdateFilter.reset();

                        }

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

                        if (_MQTTMessageBuffer.size() > 1)
                            message += ", " + String.format("%1$d messages on buffer", _MQTTMessageBuffer.size());
                        else if (_MQTTMessageBuffer.size() > 0)
                            message += ", " + String.format("1 message on buffer");

                        // modify foreground service notification
                        Intent notificationIntent = new Intent(AmbulanceForegroundService.this, AmbulanceForegroundService.class);
                        notificationIntent.setAction(AmbulanceForegroundService.Actions.UPDATE_NOTIFICATION);
                        notificationIntent.putExtra("MESSAGE", message);
                        startService(notificationIntent);

                    } else

                        Log.d(TAG, "Got updates but no ambulance!");

                }

            }

        };

        try {

            fusedLocationClient.requestLocationUpdates(getLocationRequest(), locationCallback, null)
                    .addOnSuccessListener(
                            aVoid -> {

                                Log.i(TAG, "Starting location updates");
                                _updatingLocation = true;

                                // Broadcast success
                                Intent localIntent = new Intent(org.emstrack.models.util.BroadcastActions.SUCCESS);
                                sendBroadcastWithUUID(localIntent, uuid);

                                // Broadcast location change
                                Intent changeIntent = new Intent(BroadcastActions.LOCATION_UPDATE_CHANGE);
                                sendBroadcastWithUUID(changeIntent, uuid);

                            })
                    .addOnFailureListener(
                            e -> {
                                Log.i(TAG, "Failed to start location updates");
                                e.printStackTrace();

                                // Broadcast failure
                                Intent localIntent = new Intent(org.emstrack.models.util.BroadcastActions.FAILURE);
                                localIntent.putExtra(org.emstrack.models.util.BroadcastExtras.MESSAGE, getString(R.string.failedToStartLocationUpdates));
                                sendBroadcastWithUUID(localIntent, uuid);

                            });

        } catch (SecurityException e) {
            Log.i(TAG, "Failed to start location updates");
            e.printStackTrace();

            // Broadcast failure
            Intent localIntent = new Intent(org.emstrack.models.util.BroadcastActions.FAILURE);
            localIntent.putExtra(org.emstrack.models.util.BroadcastExtras.MESSAGE, getString(R.string.failedToStartLocationUpdates));
            sendBroadcastWithUUID(localIntent, uuid);

        }
    }

    /**
     * Handles the Remove Updates button, and requests removal of location updates.
     */
    public void stopLocationUpdates(final String uuid) {

        Log.d(TAG, "stopLocationUpdates");

        _lastLocation = null;

        // Already started?
        if (!_updatingLocation) {
            Log.i(TAG, "Not requesting location updates. Skipping.");

            // broadcast success
            broadcastSuccess("Not requesting location updates", uuid);

            // and return
            return;
        }

        // reset updating location flag
        _updatingLocation = false;

        // get profile client
        MqttProfileClient profileClient = AmbulanceForegroundService.getProfileClient(this);

        // clear location_client on server?
        Ambulance ambulance = getAppData().getAmbulance();

        if (ambulance != null && profileClient != null &&
                profileClient.getClientId().equals(ambulance.getLocationClientId())) {

            Log.i(TAG, "Will clear location client on server");

            // clear location_client on server
            String payload = "{\"location_client_id\":\"\"}";

            // Update location_client on server, listening to updates already
            Intent intent = new Intent(this, AmbulanceForegroundService.class);
            intent.setAction(AmbulanceForegroundService.Actions.UPDATE_AMBULANCE);
            Bundle bundle = new Bundle();
            bundle.putInt(AmbulanceForegroundService.BroadcastExtras.AMBULANCE_ID, ambulance.getId());
            bundle.putString(AmbulanceForegroundService.BroadcastExtras.AMBULANCE_UPDATE, payload);
            intent.putExtras(bundle);

            new OnServiceComplete(this,
                    BroadcastActions.AMBULANCE_UPDATE,
                    org.emstrack.models.util.BroadcastActions.FAILURE,
                    intent) {

                @Override
                public void onSuccess(Bundle extras) {

                    Log.d(TAG, "Successfully updated location_client_id on server");

                    // remove on fusedLocationclient
                    fusedLocationClient.removeLocationUpdates(locationCallback)
                            .addOnSuccessListener(
                                    aVoid -> {

                                        Log.i(TAG, "Stopping location updates");

                                        // Broadcast location change
                                        Intent changeIntent = new Intent(BroadcastActions.LOCATION_UPDATE_CHANGE);
                                        getLocalBroadcastManager().sendBroadcast(changeIntent);

                                    })
                            .addOnFailureListener(
                                    e -> {
                                        Log.i(TAG, "Failed to stop location updates");
                                        e.printStackTrace();
                                    });

                    // broadcast success
                    broadcastSuccess("Successfully stopped location updates", uuid);

                }

                @Override
                public void onFailure(Bundle extras) {
                    super.onFailure(extras);

                    // broadcast failure
                    broadcastFailure(getString(R.string.couldNotUpdateAmbulanceOnServer), uuid);

                }

            }
                    .setSuccessIdCheck(false) // AMBULANCE_UPDATE will have a different UUID
                    .setFailureIdCheck(false) // FAILURE may have a different UUID
                    .start();

        } else {

            Log.i(TAG, "No need to clear location client on server");

            // remove on fusedLocationclient
            fusedLocationClient.removeLocationUpdates(locationCallback)
                    .addOnSuccessListener(
                            aVoid -> {

                                Log.i(TAG, "Stopping location updates");

                                // Broadcast location change
                                Intent changeIntent = new Intent(BroadcastActions.LOCATION_UPDATE_CHANGE);
                                getLocalBroadcastManager().sendBroadcast(changeIntent);

                            })
                    .addOnFailureListener(
                            e -> {
                                Log.i(TAG, "Failed to stop location updates");
                                e.printStackTrace();
                            });

            // broadcast success
            broadcastSuccess("Successfully stopped location updates", uuid);

        }
    }

    /**
     * Begin call updates
     *
     * @param uuid
     */
    public void beginCallUpdates(final String uuid) {

        Log.i(TAG, "Subscribing to call status");

        // Logged in?
        final Ambulance ambulance = AmbulanceForegroundService.getAppData().getAmbulance();
        if (ambulance == null) {

            // Broadcast failure and return
            Intent localIntent = new Intent(org.emstrack.models.util.BroadcastActions.FAILURE);
            localIntent.putExtra(org.emstrack.models.util.BroadcastExtras.MESSAGE, getString(R.string.noAmbulanceSelected));
            sendBroadcastWithUUID(localIntent, uuid);
            return;

        }

        // get profile client
        MqttProfileClient profileClient = AmbulanceForegroundService.getProfileClient(this);

        // subscribe to call status
        try {

            final int ambulanceId = ambulance.getId();
            profileClient.subscribe(
                    String.format("ambulance/%1$d/call/+/status", ambulanceId),2,
                    (topic, message) -> {

                        // Get calls
                        CallStack calls = appData.getCalls();

                        // Keep subscription to call_current to make sure we receive latest updates
                        Log.i(TAG, "Retrieving call statuses, currentCallId=" + calls.getCurrentCallId());

                        try {

                            // parse the status
                            String status = new String(message.getPayload());

                            if (status.equals("") || status.length() < 2)
                                Log.i(TAG, "Received empty status, skipping update");

                            else {

                                 status = status.substring(1, status.length() - 1);

                                // the call id is the 3rd value
                                int callId = Integer.valueOf(topic.split("/")[3]);

                                Log.i(TAG, "Received ambulance/" + ambulanceId + "/call/" +
                                        callId + "/status='" + status + "'");

                                // Is this a new call?
                                if (!calls.contains(callId)) {

                                    Log.d(TAG, "New call");

                                    // this is a new call, subscribe
                                    processOrSubscribeToCall(callId, uuid);

                                } else {

                                    // this is an existing call

                                    Log.d(TAG, "Existing call");

                                    // Get ambulance call
                                    AmbulanceCall ambulanceCall = calls.get(callId).getAmbulanceCall(ambulanceId);

                                    String newStatus;
                                    if (status.equalsIgnoreCase(AmbulanceCall.statusLabel.get(AmbulanceCall.STATUS_REQUESTED))) {

                                        // if status is "requested", process call
                                        processOrSubscribeToCall(callId, uuid);
                                        newStatus = AmbulanceCall.STATUS_REQUESTED;

                                    } else if (status.equalsIgnoreCase(AmbulanceCall.statusLabel.get(AmbulanceCall.STATUS_ACCEPTED))) {

                                        // if status is "accepted", set call accepted
                                        setCallAccepted(callId, uuid);
                                        newStatus = AmbulanceCall.STATUS_ACCEPTED;

                                    } else {

                                        if (status.equalsIgnoreCase(AmbulanceCall.statusLabel.get(AmbulanceCall.STATUS_DECLINED)))
                                            newStatus = AmbulanceCall.STATUS_DECLINED;
                                        else if (status.equalsIgnoreCase(AmbulanceCall.statusLabel.get(AmbulanceCall.STATUS_COMPLETED)))
                                            newStatus = AmbulanceCall.STATUS_COMPLETED;
                                        else if (status.equalsIgnoreCase(AmbulanceCall.statusLabel.get(AmbulanceCall.STATUS_SUSPENDED)))
                                            newStatus = AmbulanceCall.STATUS_SUSPENDED;
                                        else
                                            throw new Exception("Unknown status '" + status + "'");

                                        // if current call
                                        if (calls.isCurrentCall(callId)) {

                                            // Clean up call
                                            new OnServiceComplete(AmbulanceForegroundService.this,
                                                    org.emstrack.models.util.BroadcastActions.SUCCESS,
                                                    org.emstrack.models.util.BroadcastActions.FAILURE,
                                                    null) {

                                                @Override
                                                public void run() {

                                                    // clean up
                                                    cleanUpCall(callId, getUuid());

                                                }

                                                @Override
                                                public void onSuccess(Bundle extras) {

                                                    // then broadcast CALL_COMPLETED
                                                    Intent callCompletedIntent = new Intent(BroadcastActions.CALL_COMPLETED);
                                                    callCompletedIntent.putExtra(AmbulanceForegroundService.BroadcastExtras.CALL_ID, callId);
                                                    getLocalBroadcastManager().sendBroadcast(callCompletedIntent);

                                                }

                                                @Override
                                                public void onFailure(Bundle extras) {
                                                    super.onFailure(extras);

                                                    // broadcast failure
                                                    broadcastFailure("Could not clean up call");

                                                }

                                            }
                                                    .start();

                                        }

                                    }

                                    // Update status, server updates may take a while
                                    // This will work even if call is deleted
                                    ambulanceCall.setStatus(newStatus);

                                }

                            }

                        } catch (Exception e) {

                            Log.i(TAG, "Could not parse status '" + topic + ":" + message
                                    + "'. Exception='" + e.toString() + "'");

                            // Broadcast failure
                            Intent localIntent = new Intent(org.emstrack.models.util.BroadcastActions.FAILURE);
                            localIntent.putExtra(org.emstrack.models.util.BroadcastExtras.MESSAGE, getString(R.string.couldNotParseStatus));
                            sendBroadcastWithUUID(localIntent, uuid);

                        }
                    });

        } catch (MqttException e) {

            Log.d(TAG, "Could not subscribe to statuses");

            // Broadcast failure
            Intent localIntent = new Intent(org.emstrack.models.util.BroadcastActions.FAILURE);
            localIntent.putExtra(org.emstrack.models.util.BroadcastExtras.MESSAGE, getString(R.string.couldNotSubscribeToStatuses));
            sendBroadcastWithUUID(localIntent, uuid);
        }

    }

    public void processOrSubscribeToCall(final int callId, final String uuid) {

        // Get calls
        CallStack _calls = appData.getCalls();
        if (_calls.contains(callId) && !_calls.hasCurrentOrPendingCall()) {

            // Is this an existing call and no call_current are currently being served?
            processNextCall();

            // then return
            return;

        }

        // Subscribe to call to get call information
        MqttProfileClient profileClient = getProfileClient(this);

        try {

            Log.i(TAG, "Subscribing to call/" + callId);

            profileClient.subscribe(String.format("call/%1$s/data", callId),2,
                    (topic, message) -> {

                        // Get calls
                        CallStack calls = appData.getCalls();

                        // Keep subscription to call_current to make sure we receive latest updates
                        Log.i(TAG, "Retrieving call data");

                        // parse call id data
                        GsonBuilder gsonBuilder = new GsonBuilder();
                        gsonBuilder.setFieldNamingPolicy(FieldNamingPolicy.LOWER_CASE_WITH_UNDERSCORES);
                        Gson gson = gsonBuilder.create();

                        String payload = new String(message.getPayload());
                        if (payload == null || payload.isEmpty())
                            // This can happen when clearing a call
                            // It will also broadcast an "ended" state so call will be finalized then
                            return;

                        try {

                            // Parse call data
                            Call call = gson.fromJson(payload, Call.class);
                            if (call == null)
                                throw new Exception("Got null call");

                            // Sort waypoints
                            call.sortWaypoints(true);

                            // Is this an existing call?
                            if (calls.contains(call.getId())) {

                                // Update call information
                                updateCallInformation(call, uuid);

                            } else if (call.getStatus() == "E") {

                                // Call has ended and is not longer on the queue, ignore
                                Log.d(TAG, "Ignoring call " + call.getId());

                            } else {

                                // Add to pending calls
                                calls.put(call);

                                // if no current, process next
                                if (!calls.hasCurrentOrPendingCall())
                                    processNextCall();

                            }


                        } catch (Exception e) {

                            // Broadcast failure
                            broadcastFailure(getString(R.string.couldNotParseCallData));

                        }

                    });

        } catch (MqttException e) {

            Log.d(TAG, "Could not subscribe to call data");

            // Broadcast failure
            Intent localIntent = new Intent(org.emstrack.models.util.BroadcastActions.FAILURE);
            localIntent.putExtra(org.emstrack.models.util.BroadcastExtras.MESSAGE,
                    getString(R.string.couldNotSubscribe, "data for call " + callId));
            sendBroadcastWithUUID(localIntent, uuid);

        }

    }

    public void updateCallInformation(final Call call, final String uuid) {

        Log.d(TAG, "Updating call " + call.getId());

        // Get calls
        CallStack calls = appData.getCalls();

        if (call.getId() == calls.getCurrentCallId()) {

            // Call is currently being served
            Log.i(TAG, "Call is current call");

            // Has call not ended yet?
            if (!call.getStatus().equals("E")) {

                try {

                    // call setOrUpdateCall
                    setOrUpdateCall(uuid, call);

                    // Broadcast current call updateAmbulance
                    Intent callFinishedIntent = new Intent(BroadcastActions.CALL_UPDATE);
                    sendBroadcastWithUUID(callFinishedIntent, uuid);

                } catch (AmbulanceForegroundServiceException | CallStack.CallStackException | Call.CallException e) {

                    String message = "Exception in setOrUpdateCall: " + e.toString();
                    Log.d(TAG, message);

                    /// Broadcast failure
                    Intent localIntent = new Intent(org.emstrack.models.util.BroadcastActions.FAILURE);
                    localIntent.putExtra(org.emstrack.models.util.BroadcastExtras.MESSAGE, message);
                    sendBroadcastWithUUID(localIntent, uuid);

                }

            }

            // Call ending is handled by ambulancecall update


        } else {

            // Call is on the queue
            Log.i(TAG, "Call is on the queue");

            // Has call ended?
            if (call.getStatus().equals("E")) {

                // Remove from pending calls
                calls.remove(calls.getCurrentCallId());

            } else {

                // Update call information
                calls.put(call);

            }

        }

    }


    private void unsubscribeFromCalls(Ambulance ambulance) {

        // Get call stack
        CallStack calls = appData.getCalls();

        // get profile client
        MqttProfileClient profileClient = AmbulanceForegroundService.getProfileClient(this);

        CallStack.CallStackIterator iterator = calls.iterator();
        while (iterator.hasNext()) {

            Map.Entry<Integer, Call> pair = iterator.next();
            int callId = pair.getKey();

            // unsubscribe from call
            Log.i(TAG, "Unsubscribe from call/" + callId + "/data");
            try {
                profileClient.unsubscribe("call/" + callId + "/data");
            } catch (MqttException e) {
                Log.d(TAG, "Could not unsubscribe from 'call/" + callId + "/data'");
            }

            // remove from pending call_current
            iterator.remove();

        }

        Log.i(TAG, "Unsubscribe from call updates");
        try {
            profileClient.unsubscribe(String.format("ambulance/%1$d/call/+/status", ambulance.getId()));
        } catch (MqttException e) {
            Log.d(TAG, String.format("ambulance/%1$d/call/+/status", ambulance.getId()));
        }

    }

    public void stopCallUpdates(final String uuid) {

        // Get calls
        CallStack calls = appData.getCalls();

        Ambulance ambulance = getAppData().getAmbulance();
        if (ambulance != null) {

            // terminate current call, does not process next
            if (calls.hasCurrentCall())

                // Clean up call
                new OnServiceComplete(AmbulanceForegroundService.this,
                        org.emstrack.models.util.BroadcastActions.SUCCESS,
                        org.emstrack.models.util.BroadcastActions.FAILURE,
                        null) {

                    @Override
                    public void run() {

                        // terminate current call, does not process next
                        cleanUpCall(calls.getCurrentCallId(), getUuid(), false);

                    }

                    @Override
                    public void onSuccess(Bundle extras) {

                        // unsubscribe
                        unsubscribeFromCalls(ambulance);

                        // broadcast success
                        broadcastSuccess("Successfully stopped call updates", uuid);

                    }

                    @Override
                    public void onFailure(Bundle extras) {
                        super.onFailure(extras);

                        // broadcast failure
                        broadcastFailure(getString(R.string.couldNotStopCallUpdates), uuid);

                    }
                }
                        .start();

            else {

                // unsubscribe
                unsubscribeFromCalls(ambulance);

                // broadcast success
                broadcastSuccess("Successfully stopped call updates", uuid);

            }

        } else {

            // broadcast success
            broadcastSuccess("Successfully stopped call updates", uuid);

        }

    }

    private void removeCurrentGeofences(int callId, String uuid) {

        Log.i(TAG, "Stopping current geofences");

        // Get calls
        CallStack calls = appData.getCalls();

        // If current call, stop geofences first
        if (callId != calls.getCurrentCallId()) {

            // broadcast success
            broadcastSuccess("Not current call, no geofences to remove", uuid);

            // then return
            return;

        }

        // get ambulance
        Ambulance ambulance = getAppData().getAmbulance();

        // get ambulance call
        AmbulanceCall ambulanceCall = calls.getCurrentCall().getCurrentAmbulanceCall();

        // retrieve waypoints
        List<String> requestIds = new ArrayList<>();
        for (Map.Entry<String, Geofence> entry : _geofences.entrySet()) {

            Waypoint waypoint = entry.getValue().getWaypoint();
            if (ambulanceCall.containsWaypoint(waypoint)) {
                // Add to list to remove
                requestIds.add(entry.getKey());
            }
        }

        new OnServiceComplete(this,
                org.emstrack.models.util.BroadcastActions.SUCCESS,
                org.emstrack.models.util.BroadcastActions.FAILURE,
                null) {

            @Override
            public void run() {

                // Stop geofences
                stopGeofence(getUuid(), requestIds);

            }

            @Override
            public void onSuccess(Bundle extras) {

                // Set status as available
                Log.d(TAG, "Set ambulance '" + getAppData().getAmbulance().getId() + "' available");
                updateAmbulanceStatus(uuid, ambulance.getId(), Ambulance.STATUS_AVAILABLE);

                // Release current call
                calls.setPendingCall(false);

                // broadcast success
                broadcastSuccess("Successfully removed current geofences", uuid);

            }

            @Override
            public void onFailure(Bundle extras) {
                super.onFailure(extras);

                // broadcast failure
                broadcastSuccess("Could not remove current geofences", uuid);
            }

        }
                .start();

    }

    public void cleanUpCall(int callId, String uuid) {
        cleanUpCall(callId, uuid, true);
    }

    public void cleanUpCall(int callId, String uuid, boolean processNext) {

        Log.d(TAG, "Cleaning up call '" + callId + "'");

        if (callId < 0) {

            Log.d(TAG, "CallId < 0, abort!");

            // broadcast failure
            broadcastFailure(getString(R.string.couldNotCleanUpCall), uuid);

            // then return
            return;
        }

        // Get calls
        CallStack calls = appData.getCalls();

        // get profile client
        MqttProfileClient profileClient = AmbulanceForegroundService.getProfileClient(this);

        new OnServiceComplete(this,
                org.emstrack.models.util.BroadcastActions.SUCCESS,
                org.emstrack.models.util.BroadcastActions.FAILURE,
                null) {

            @Override
            public void run() {

                removeCurrentGeofences(callId, getUuid());

            }

            @Override
            public void onSuccess(Bundle extras) {

                try {

                    // Should stop listening to call?
                    // callId could also be 0 if pending
                    if (callId > 0) {

                        // Get ambulancecall
                        Ambulance ambulance = getAppData().getAmbulance();
                        AmbulanceCall ambulanceCall = calls.get(callId).getAmbulanceCall(ambulance.getId());

                        if (ambulanceCall.getStatus().equals(AmbulanceCall.STATUS_COMPLETED)) {

                            Log.i(TAG, "Unsubscribe from call/" + callId + "/data");

                            // unsubscribe from call
                            try {
                                profileClient.unsubscribe("call/" + callId + "/data");
                            } catch (MqttException e) {
                                Log.d(TAG, "Could not unsubscribe from 'call/" + callId + "/data'");
                            }

                            // remove call from the queue
                            calls.remove(callId);

                        }

                    }

                    if (calls.hasPendingCall()) {

                        // It was prompting user, most likely declined
                        Log.d(TAG, "Was prompting user, opening up to new call_current");

                        // open up to new call_current
                        calls.setPendingCall(false);

                    }

                    if (processNext) {

                        Log.d(TAG, "Processing next call...");

                        // process next call, except if callId
                        processNextCall(callId);

                    }

                    // broadcast success
                    broadcastSuccess("Successfully cleaned up call", uuid);

                } catch (Exception t) {

                    Log.d(TAG, "Could not clean up call. Exception='" + t + "'");

                    // broadcast failure
                    broadcastFailure(getString(R.string.couldNotCleanUpCall), uuid, t);

                }

            }

        }
                .start();

    }

    public void requestToFinishCurrentCall(String uuid) {

        // Get calls
        CallStack calls = appData.getCalls();

        // if currently not serving call
        if (!calls.hasCurrentCall()) {

            Log.d(TAG, "Can't finish call: not serving any call.");

            // broadcast failure
            broadcastFailure(getString(R.string.couldNotFinishCall), uuid);

            // the return
            return;

        }

        // Set ambulanceCall status to completed locally
        // This prevents further processing of this call
        Call call = calls.getCurrentCall();
        call.getCurrentAmbulanceCall().setStatus(AmbulanceCall.STATUS_COMPLETED);

        // publish finished status to server
        setAmbulanceCallStatus(call.getId(),
                AmbulanceCall.statusLabel.get(AmbulanceCall.STATUS_COMPLETED), uuid);

    }

    public void requestToDeclineCurrentCall(int callId, String uuid) {

        // Get calls
        CallStack calls = appData.getCalls();

        // if currently serving call, can't decline
        if (calls.hasCurrentCall()) {

            Log.d(TAG, "Can't decline call: currently serving call/" + calls.getCurrentCallId());

            Intent localIntent = new Intent(org.emstrack.models.util.BroadcastActions.FAILURE);
            localIntent.putExtra(org.emstrack.models.util.BroadcastExtras.MESSAGE, getString(R.string.couldNotDeclineCall));
            sendBroadcastWithUUID(localIntent, uuid);

            return;

        }

        try {

            // Set ambulanceCall status to suspended locally
            // This prevents further processing of this call
            Call call = calls.get(callId);
            Ambulance ambulance = AmbulanceForegroundService.getAppData().getAmbulance();
            call.getAmbulanceCall(ambulance.getId()).setStatus(AmbulanceCall.STATUS_DECLINED);

        } catch (Exception e) {

            Log.d(TAG, "Could not set ambulance call status to declined. Moving on...");

        }

        // publish declined as status
        setAmbulanceCallStatus(callId,
                AmbulanceCall.statusLabel.get(AmbulanceCall.STATUS_DECLINED), uuid);

    }

    public void requestToSuspendCurrentCall(int callId, String uuid) {

        // Get calls
        CallStack calls = appData.getCalls();

        // if not currently serving call, can't suspend
        if (!calls.hasCurrentCall()) {

            Log.d(TAG, "Can't suspend call: not currently serving any call");

            Intent localIntent = new Intent(org.emstrack.models.util.BroadcastActions.FAILURE);
            localIntent.putExtra(org.emstrack.models.util.BroadcastExtras.MESSAGE, getString(R.string.couldNotSuspendCall));
            sendBroadcastWithUUID(localIntent, uuid);

            return;

        }

        // Set ambulanceCall status to suspended locally
        // This prevents further processing of this call
        Call call = calls.getCurrentCall();
        call.getCurrentAmbulanceCall().setStatus(AmbulanceCall.STATUS_SUSPENDED);

        // publish suspended as status
        setAmbulanceCallStatus(callId,
                AmbulanceCall.statusLabel.get(AmbulanceCall.STATUS_SUSPENDED), uuid);

    }

    public void processNextCall() {
        processNextCall(-1);
    }

    public void processNextCall(int exceptCallId) {

        // Get calls
        CallStack calls = appData.getCalls();

        // if current call, bark
        if (calls.hasCurrentCall()) {

            Log.d(TAG, "Will not process next call: currently serving call/" + calls.getCurrentCallId());

            // then return
            return;

        }

        // if prompting user, ignore
        if (calls.hasPendingCall()) {

            Log.d(TAG, "Will not process next call: currently prompting user to accept call.");

            // then return
            return;

        }

        // Get current ambulance
        Ambulance ambulance = AmbulanceForegroundService.getAppData().getAmbulance();
        if (ambulance == null) {

            Log.d(TAG, "Ambulance is null. This should never happen");

            // then return
            return;

        }

        // Get next suitable call
        Call nextCall = calls.getNextCall(ambulance.getId());
        if (nextCall != null && nextCall.getId() != exceptCallId) {

            Log.i(TAG, "Will prompt user to accept call");

            // set pending call
            calls.setPendingCall(true);

            // Login intent
            Intent notificationIntent = new Intent(AmbulanceForegroundService.this, LoginActivity.class);
            notificationIntent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
            PendingIntent pendingIntent = PendingIntent.getActivity(AmbulanceForegroundService.this, 0,
                    notificationIntent, 0);

            // Create notification
            NotificationCompat.Builder mBuilder = new NotificationCompat.Builder(this, PRIMARY_CHANNEL)
                    .setSmallIcon(R.mipmap.ic_launcher)
                    .setContentTitle("EMSTrack")
                    .setContentText(getString(R.string.newCallRequest))
                    .setPriority(NotificationCompat.PRIORITY_MAX)
                    .setAutoCancel(true)
                    .setContentIntent(pendingIntent);

            NotificationManagerCompat notificationManager = NotificationManagerCompat.from(this);
            notificationManager.notify(notificationId.getAndIncrement(), mBuilder.build());

            // create intent to prompt user
            Intent callPromptIntent = new Intent(BroadcastActions.PROMPT_CALL_ACCEPT);
            callPromptIntent.putExtra(AmbulanceForegroundService.BroadcastExtras.CALL_ID, nextCall.getId());
            getLocalBroadcastManager().sendBroadcast(callPromptIntent);

        } else {

            Log.i(TAG, "No more pending or suspended calls");

        }

    }

    // handles steps 3 and 4 of Accepting Calls
    public void setAmbulanceCallStatus(int callId, String status, String uuid) {

        Log.i(TAG, "Setting call/" + callId + "/status to '" + status + "'");

        MqttProfileClient profileClient = getProfileClient(AmbulanceForegroundService.this);
        Ambulance ambulance = getAppData().getAmbulance();
        if (ambulance != null) {

            // step 4: publish accepted to server
            String path = String.format("user/%1$s/client/%2$s/ambulance/%3$s/call/%4$s/status",
                    profileClient.getUsername(), profileClient.getClientId(), ambulance.getId(), callId);

            // publish status to server
            publishToPath(status, path, uuid);

        } else {

            Log.d(TAG, "Ambulance not found while in acceptCall()");

            // broadcast failure
            broadcastFailure( getString(R.string.couldNotFindAmbulance), uuid);
        }

    }

    // handles steps 6 to 7
    public void setCallAccepted(final int callId, final String uuid) {

        // Retrieve call
        Call call = appData.getCalls().get(callId);
        if (call == null) {

            String message = String.format("Could not retrieve call '%1$d'", callId);
            Log.d(TAG, message);

            Intent localIntent = new Intent(org.emstrack.models.util.BroadcastActions.FAILURE);
            localIntent.putExtra(org.emstrack.models.util.BroadcastExtras.MESSAGE, message);
            sendBroadcastWithUUID(localIntent, uuid);
            return;

        }

        try {

            // Call setOrUpdateCall
            setOrUpdateCall(uuid, call);

            // broadcast CALL_ACCEPTED
            Intent callAcceptedIntent = new Intent(BroadcastActions.CALL_ACCEPTED);
            callAcceptedIntent.putExtra(AmbulanceForegroundService.BroadcastExtras.CALL_ID, callId);
            sendBroadcastWithUUID(callAcceptedIntent, uuid);

        } catch (AmbulanceForegroundServiceException | CallStack.CallStackException | Call.CallException e) {

            String message = "Exception in setOrUpdateCall: " + e.toString();
            Log.d(TAG, message);

            /// Broadcast failure
            Intent localIntent = new Intent(org.emstrack.models.util.BroadcastActions.FAILURE);
            localIntent.putExtra(org.emstrack.models.util.BroadcastExtras.MESSAGE, message);
            sendBroadcastWithUUID(localIntent, uuid);

        }

    }

    public void setOrUpdateCall(final String uuid, Call call)
            throws AmbulanceForegroundServiceException, CallStack.CallStackException, Call.CallException {

        // Get calls
        CallStack calls = appData.getCalls();

        // Fail if servicing another call
        if (calls.hasCurrentCall() && calls.getCurrentCallId() != call.getId()) {
            String message = String.format("Can't set call as accepted: already servicing call '%1$d'", calls.getCurrentCallId());
            throw new AmbulanceForegroundServiceException(message);
        }

        // Fails if call is not in stack
        if (!calls.contains(call.getId())) {
            String message = String.format("Call '%1$d' is not in the current call stack", calls.getCurrentCallId());
            throw new AmbulanceForegroundServiceException(message);
        }

        // Get current ambulance and set call to it
        Ambulance ambulance = AmbulanceForegroundService.getAppData().getAmbulance();
        call.setCurrentAmbulanceCall(ambulance.getId());
        AmbulanceCall ambulanceCall = call.getCurrentAmbulanceCall();

        // This is an update, what is next waypoint?
        // Do this before updating...
        Waypoint nextWaypoint = null;
        if (calls.hasCurrentCall()) {

            // Get current ambulancecall and next waypoint
            // AmbulanceCall currentAmbulanceCall = AmbulanceForegroundService.getCurrentAmbulanceCall();
            AmbulanceCall currentAmbulanceCall = calls.getCurrentCall().getCurrentAmbulanceCall();
            nextWaypoint = currentAmbulanceCall.getNextWaypoint();

        }
        Log.d(TAG, "Next waypoint is " + (nextWaypoint == null ? "'null'" : "'" + nextWaypoint.getId() + "'"));

        // Update call in stack and set as current call
        calls.put(call);
        calls.setCurrentCall(call.getId());

        // It this maybe the next waypoint?
        Waypoint nextUpdatedWaypoint = ambulanceCall.getNextWaypoint();
        Log.d(TAG, "Next updated waypoint is " + (nextUpdatedWaypoint == null ? "'null'" : "'" + nextUpdatedWaypoint.getId() + "'"));

        // Update next waypoint status if update waypoint is a different waypoint
        if ((nextWaypoint == null && nextUpdatedWaypoint != null) ||
                (nextWaypoint != null && nextUpdatedWaypoint == null) ||
                (nextWaypoint != null && nextUpdatedWaypoint != null &&
                        nextWaypoint.getId() != nextUpdatedWaypoint.getId()))
            updateAmbulanceNextWaypointStatus(uuid, ambulanceCall, call);

        // Add geofence
        Log.i(TAG, "Will set waypoints");

        // Sort waypoints
        ambulanceCall.sortWaypoints();

        // Loop through waypoints
        for (Waypoint waypoint : ambulanceCall.getWaypointSet()) {

            // Retrieve location
            startGeofence(uuid, new Geofence(waypoint, _defaultGeofenceRadius));

        }

    }

    public void updateAmbulanceNextWaypointStatus(final String uuid, AmbulanceCall ambulanceCall, Call call) {

        // Get next waypoint
        Waypoint nextWaypoint = ambulanceCall.getNextWaypoint();
        if (nextWaypoint != null) {

            // Update according to waypoint
            String waypointType = nextWaypoint.getLocation().getType();
            Log.d(TAG, "Next waypoint is of type '" + waypointType + "'");
            if (waypointType.equals(Location.TYPE_INCIDENT)) {

                // step 7: publish patient bound to server
                updateAmbulanceStatus(uuid, ambulanceCall.getAmbulanceId(), Ambulance.STATUS_PATIENT_BOUND);

            } else if (waypointType.equals(Location.TYPE_BASE)) {

                // step 7: publish base bound to server
                updateAmbulanceStatus(uuid, ambulanceCall.getAmbulanceId(),Ambulance.STATUS_BASE_BOUND);

            } else if (waypointType.equals(Location.TYPE_HOSPITAL)) {

                // step 7: publish hospital bound to server
                updateAmbulanceStatus(uuid, ambulanceCall.getAmbulanceId(),Ambulance.STATUS_HOSPITAL_BOUND);

            } else if (waypointType.equals(Location.TYPE_WAYPOINT)) {

                // step 7: publish waypoint bound to server
                updateAmbulanceStatus(uuid, ambulanceCall.getAmbulanceId(),Ambulance.STATUS_WAYPOINT_BOUND);

            }

        } else {

            Log.d(TAG, "Next waypoint is not available");
            // broadcast PROMPT_NEXT_WAYPOINT
            Intent nextWaypointIntent = new Intent(BroadcastActions.PROMPT_NEXT_WAYPOINT);
            nextWaypointIntent.putExtra(AmbulanceForegroundService.BroadcastExtras.CALL_ID, call.getId());
            sendBroadcastWithUUID(nextWaypointIntent, uuid);

        }
    }

    public void updateAmbulanceEnterWaypointStatus(final String uuid, AmbulanceCall ambulanceCall, Call call, Waypoint waypoint) {

        Log.d(TAG, "Entering waypoint");

        // Get current waypoint
        if (waypoint == null) {
            // This should never happen
            Log.d(TAG, "Could not retrieve waypoint");
            return;
        }

        // Get next waypoint
        Waypoint nextWaypoint = ambulanceCall.getNextWaypoint();
        if (nextWaypoint == null) {
            // Ignore if not current destination
            Log.d(TAG, "Next waypoint is not available. Ignoring transition...");
            return;
        }

        // Arrived at current destination?
        if (waypoint != nextWaypoint) {
            // Ignore if not current destination
            Log.d(TAG, "Arrived at another waypoint, not current destination. Ignoring transition...");
            return;
        }

        if (waypoint.isSkipped()) {

            // Ignore if not active
            Log.d(TAG, "Arrived at skipped waypoint. Ignoring transition...");
            return;
        }

        if (waypoint.isVisited() || waypoint.isVisiting()) {

            // Ignore if already visited
            Log.d(TAG, "Arrived at visited/visiting waypoint. Ignoring transition...");
            return;
        }

        String waypointType = waypoint.getLocation().getType();
        Log.d(TAG, "Arrived at waypoint of type '" + waypointType + "'");

        if (waypointType.equals(Location.TYPE_INCIDENT)) {

            // publish at patient to server
            updateAmbulanceStatus(uuid, ambulanceCall.getAmbulanceId(),Ambulance.STATUS_AT_PATIENT);

        } else if (waypointType.equals(Location.TYPE_BASE)) {

            // publish base bound to server
            updateAmbulanceStatus(uuid, ambulanceCall.getAmbulanceId(), Ambulance.STATUS_AT_BASE);

        } else if (waypointType.equals(Location.TYPE_HOSPITAL)) {

            // publish hospital bound to server
            updateAmbulanceStatus(uuid, ambulanceCall.getAmbulanceId(), Ambulance.STATUS_AT_HOSPITAL);

        } else if (waypointType.equals(Location.TYPE_WAYPOINT)) {

            // publish waypoint bound to server
            updateAmbulanceStatus(uuid, ambulanceCall.getAmbulanceId(), Ambulance.STATUS_AT_WAYPOINT);

        }

        // Update waypoint status
        updateWaypointStatus(Waypoint.STATUS_VISITING, waypoint,
                ambulanceCall.getAmbulanceId(), call.getId());

    }

    public void updateAmbulanceExitWaypointStatus(final String uuid, AmbulanceCall ambulanceCall, Call call, Waypoint waypoint) {

        Log.d(TAG, "Exiting waypoint");

        // Get current waypoint
        if (waypoint == null) {
            // This should never happen
            Log.d(TAG, "Could not retrieve waypoint");
            return;
        }

        // Get next waypoint
        Waypoint nextWaypoint = ambulanceCall.getNextWaypoint();
        if (nextWaypoint == null) {
            // Ignore if not current destination
            Log.d(TAG, "Next waypoint is not available. Ignoring transition...");
            return;
        }

        // Left current destination?
        if (waypoint != nextWaypoint) {
            // Ignore if not current destination
            Log.d(TAG, "Left another waypoint, not current destination. Ignoring transition...");
            return;
        }

        if (waypoint.isSkipped()) {

            // Ignore if not active
            Log.d(TAG, "Left a skipped waypoint. Ignoring transition...");
            return;
        }

        if (waypoint.isVisited() || waypoint.isCreated()) {

            // Ignore if already visited
            Log.d(TAG, "Left a visited or not visited waypoint. Ignoring transition...");
            return;
        }

        String waypointType = waypoint.getLocation().getType();
        Log.d(TAG, "Left a waypoint of type '" + waypointType + "'");

        // Update waypoint status on server
        updateWaypointStatus(Waypoint.STATUS_VISITED, waypoint,
                ambulanceCall.getAmbulanceId(), call.getId());

        // Go for next waypoint
        updateAmbulanceNextWaypointStatus(uuid, ambulanceCall, call);

    }

    public void publishToPath(final String payload, final String path, final String uuid) {

        MqttProfileClient profileClient = getProfileClient(this);

        try {

            profileClient.publish(path, payload, 2, false);

            // broadcast success
            broadcastSuccess("Successfully published to path", uuid);

        } catch (MqttException e) {

            Log.d(TAG, path);

            // broadcast failure
            broadcastFailure(getString(R.string.couldNotPublish, path), uuid);

        }
    }

    /*
     **
     */
    public void replyToGeofenceTransitions(String uuid, Geofence geofence, String action) {

        // Get calls
        CallStack calls = appData.getCalls();

        // if currently not serving call
        if (!calls.hasCurrentOrPendingCall()) {

            Log.d(TAG, "Ignoring geofence transition: not serving any call.");

            return;

        }

        Ambulance ambulance = appData.getAmbulance();
        if (ambulance == null) {

            Log.d(TAG, "Ambulance not found while in replyToTransition()");

            Intent localIntent = new Intent(org.emstrack.models.util.BroadcastActions.FAILURE);
            localIntent.putExtra(org.emstrack.models.util.BroadcastExtras.MESSAGE, getString(R.string.couldNotFindAmbulance));
            sendBroadcastWithUUID(localIntent, uuid);
            return;
        }

        Call call = calls.getCurrentCall();
        if (call != null) {

            // Transition happened while tracking call
            Log.i(TAG, "GEOFENCE transition during call");

            // Get current ambulance call
            AmbulanceCall ambulanceCall = call.getCurrentAmbulanceCall();
            if (ambulanceCall == null) {

                Log.d(TAG, "AmbulanceCall not found while in replyToTransition()");

                Intent localIntent = new Intent(org.emstrack.models.util.BroadcastActions.FAILURE);
                localIntent.putExtra(org.emstrack.models.util.BroadcastExtras.MESSAGE, getString(R.string.couldNotFindAmbulanceCall));
                sendBroadcastWithUUID(localIntent, uuid);
                return;

            }

            // Process transitions
            if (Actions.GEOFENCE_ENTER.equals(action)) {

                // Entered a geofence
                Log.i(TAG, "GEOFENCE ENTER");
                updateAmbulanceEnterWaypointStatus(uuid, ambulanceCall, call, geofence.getWaypoint());

            } else {

                // Exited a geofence
                Log.i(TAG, "GEOFENCE EXIT");
                updateAmbulanceExitWaypointStatus(uuid, ambulanceCall, call, geofence.getWaypoint());

            }

        } else {

            // Transition happened while not tracking call
            Log.i(TAG, "GEOFENCE transition outside of call. Ignoring transition...");

            // Ignore for now...

        }

    }

    private GeofencingRequest getGeofencingRequest(com.google.android.gms.location.Geofence geofence) {

        Log.i(TAG, "GEOFENCE_REQUEST: Built Geofencing Request");

        GeofencingRequest.Builder builder = new GeofencingRequest.Builder();
        builder.setInitialTrigger(GeofencingRequest.INITIAL_TRIGGER_ENTER);
        builder.addGeofence(geofence);

        return builder.build();

    }

    private PendingIntent getGeofencePendingIntent() {

        // Reuse the PendingIntent if we already have it.
        if (geofenceIntent != null) {
            return geofenceIntent;
        }
        Intent intent = new Intent(this, GeofenceBroadcastReceiver.class);

        // We use FLAG_UPDATE_CURRENT so that we get the same pending intent back when
        // calling addGeofences() and removeAllGeofences().
        geofenceIntent = PendingIntent.getBroadcast(this, 0,
                intent, PendingIntent.FLAG_UPDATE_CURRENT);

        return geofenceIntent;
    }


    @SuppressLint("MissingPermission")
    private void startGeofence(final String uuid, final Geofence geofence) {

        Log.d(TAG,String.format("GEOFENCE(%1$s, %2$f)", geofence.getLocation().toString(), geofence.getRadius()));

        // Get waypoint
        Waypoint waypoint = geofence.getWaypoint();

        // Is it a legit waypoint?
        if (waypoint.getId() < 0) {

            Log.d(TAG, "Waypoint has invalid id");

            // Broadcast failure and return
            Intent localIntent = new Intent(org.emstrack.models.util.BroadcastActions.FAILURE);
            localIntent.putExtra(org.emstrack.models.util.BroadcastExtras.MESSAGE, getString(R.string.failedToAddGeofence));
            sendBroadcastWithUUID(localIntent, uuid);
            return;

        }

        // It this a new waypoint?
        boolean existing = false;
        for (Map.Entry<String, Geofence> entry : _geofences.entrySet()) {

            String[] splits = entry.getKey().split(":", 2);
            if (waypoint.getId() == Integer.valueOf(splits[0])) {
                // Found waypoint
                existing = true;

                // update waypoint on geofence
                entry.getValue().setWaypoint(waypoint);

            }

        }

        // if existing, done
        if (existing) {

            Log.d(TAG, String.format("Geofence '%1$d' updated", waypoint.getId()));

            // Broadcast success
            Intent localIntent = new Intent(org.emstrack.models.util.BroadcastActions.SUCCESS);
            sendBroadcastWithUUID(localIntent, uuid);
            return;

        }

        // Otherwise add geofence

        // Set unique id
        final String id = (waypoint.getId() < 0 ? "_" : waypoint.getId()) + ":" + geofencesId.getAndIncrement();

        // Create settings client
        SettingsClient settingsClient = LocationServices.getSettingsClient(this);

        // Check if the device has the necessary location settings.
        settingsClient.checkLocationSettings(getLocationSettingsRequest())
                .addOnSuccessListener(
                        locationSettingsResponse -> {

                            Log.i(TAG, String.format("Adding geofence '%1$s'...", id));

                            fenceClient.addGeofences(getGeofencingRequest(geofence.build(id)),
                                    getGeofencePendingIntent())
                                    .addOnSuccessListener(
                                            aVoid -> {
                                                // Geofences added
                                                Log.i(TAG, String.format("Geofence '%1$s' added", id));

                                                // Add to map
                                                _geofences.put(id, geofence);

                                                // Broadcast success
                                                Intent localIntent = new Intent(org.emstrack.models.util.BroadcastActions.SUCCESS);
                                                sendBroadcastWithUUID(localIntent, uuid);

                                            })
                                    .addOnFailureListener(
                                            e -> {

                                                // Failed to add geofences
                                                Log.e(TAG, "FAILED TO ADD GEOFENCES: " + e.toString());

                                                // Remove id
                                                geofence.removeId(id);

                                                // Broadcast failure and return
                                                Intent localIntent = new Intent(org.emstrack.models.util.BroadcastActions.FAILURE);
                                                localIntent.putExtra(org.emstrack.models.util.BroadcastExtras.MESSAGE, getString(R.string.failedToAddGeofence));
                                                sendBroadcastWithUUID(localIntent, uuid);

                                            });

                        })
                .addOnFailureListener(
                        e -> {
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
                            Intent localIntent = new Intent(org.emstrack.models.util.BroadcastActions.FAILURE);
                            localIntent.putExtra(org.emstrack.models.util.BroadcastExtras.MESSAGE, message);
                            sendBroadcastWithUUID(localIntent, uuid);

                        });
    }


    private void removeAllGeofences(final String uuid) {

        fenceClient.removeGeofences(getGeofencePendingIntent())
                .addOnSuccessListener(
                        aVoid -> {
                            // Geofences removed
                            Log.i(TAG, "GEOFENCES REMOVED.");

                            // Clear all geofence ids
                            for (Iterator<Map.Entry<String, Geofence>> iterator = _geofences.entrySet().iterator();
                                 iterator.hasNext(); ) {
                                Map.Entry<String, Geofence> entry = iterator.next();
                                entry.getValue().removeId(entry.getKey());
                                iterator.remove();
                            }

                            // This is not necessary, but just in case :)
                            _geofences.clear();

                            // Broadcast success
                            broadcastSuccess("Succesfully removed geofences", uuid);

                        })
                .addOnFailureListener(
                        e -> {

                            // Broadcast failure and return
                            broadcastFailure(getString(R.string.failedToRemoveGeofence), uuid, e);

                        });

    }

    private void stopGeofence(final String uuid, final List<String> requestIds) {

        Log.i(TAG, "Stopping geofences: '" + requestIds + "'");

        if (requestIds.size() > 0)

            fenceClient.removeGeofences(requestIds)
                    .addOnSuccessListener(
                            aVoid -> {

                                // Geofences removed
                                Log.i(TAG, "GEOFENCES REMOVED.");

                                // Loop through list of ids and remove them
                                for (String id : requestIds) {
                                    _geofences.remove(id).removeId(id);

                                }

                                // Broadcast success
                                broadcastSuccess("Succesfully removed geofences", uuid);

                            })
                    .addOnFailureListener(
                            e -> {

                                // Broadcast failure and return
                                broadcastFailure(getString(R.string.failedToRemoveGeofence), uuid, e);

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

    /**
     * Broadcast failure message
     *
     * @param message the message
     */
    public void broadcastFailure(final String message) {

        Log.d(TAG, message);

        // Broadcast failure
        Intent localIntent = new Intent(org.emstrack.models.util.BroadcastActions.FAILURE);
        localIntent.putExtra(org.emstrack.models.util.BroadcastExtras.MESSAGE, message);
        sendBroadcast(localIntent);

    }

    /**
     * Broadcast failure message
     *
     * @param message the message
     * @param uuid the current uuid
     */
    public void broadcastFailure(final String message, final String uuid) {

        Log.d(TAG, message);

        // Broadcast failure
        Intent localIntent = new Intent(org.emstrack.models.util.BroadcastActions.FAILURE);
        localIntent.putExtra(org.emstrack.models.util.BroadcastExtras.MESSAGE, message);
        sendBroadcastWithUUID(localIntent, uuid);

    }

    /**
     * Broadcast failure message
     *
     * @param message the message
     * @param uuid the current uuid
     * @param t the exception
     */
    public void broadcastFailure(String message, final String uuid, final Throwable t) {

        // Build error message
        if (t != null)
            message += ": " + t.toString();

        // Broadcast failure
        broadcastFailure(message, uuid);

    }

    /**
     * Broadcast success message
     *
     * @param message the message
     * @param uuid the current uuid
     */
    public void broadcastSuccess(final String message, final String uuid) {

        Log.d(TAG, message);

        // Broadcast success
        Intent localIntent = new Intent(org.emstrack.models.util.BroadcastActions.SUCCESS);
        sendBroadcastWithUUID(localIntent, uuid);

    }

}