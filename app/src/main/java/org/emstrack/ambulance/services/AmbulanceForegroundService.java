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
import org.emstrack.ambulance.util.Geofence;
import org.emstrack.ambulance.util.AmbulanceUpdateFilter;
import org.emstrack.ambulance.util.AmbulanceUpdate;
import org.emstrack.models.Token;
import org.emstrack.models.api.APIService;
import org.emstrack.models.api.APIServiceGenerator;
import org.emstrack.models.Ambulance;
import org.emstrack.models.AmbulanceCall;
import org.emstrack.models.AmbulancePermission;
import org.emstrack.models.Call;
import org.emstrack.models.CallStack;
import org.emstrack.models.Credentials;
import org.emstrack.models.GPSLocation;
import org.emstrack.models.Hospital;
import org.emstrack.models.HospitalPermission;
import org.emstrack.models.Location;
import org.emstrack.models.api.OnAPICallComplete;
import org.emstrack.models.util.OnServiceComplete;
import org.emstrack.models.Waypoint;
import org.emstrack.mqtt.MishandledTopicException;
import org.emstrack.mqtt.MqttProfileCallback;
import org.emstrack.mqtt.MqttProfileClient;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
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
    private static Ambulance _ambulance;
    private static Map<Integer, Hospital> _hospitals;
    private static List<Location> _bases;
    private static Map<Integer, Ambulance> _otherAmbulances;
    private static AmbulanceUpdate _lastLocation;
    private static Date _lastServerUpdate;
    private static boolean _updatingLocation = false;
    private static boolean _canUpdateLocation = false;
    private static ArrayList<Pair<String, String>> _MQTTMessageBuffer = new ArrayList<Pair<String, String>>();
    private static boolean _reconnecting = false;
    private static boolean _online = false;
    private static ReconnectionInformation _reconnectionInformation;

    // hold the callIds of pending call_current
    // private static int pendingCalls.getCurrentCallId();
    private static CallStack pendingCalls;

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
        public final static String GET_HOSPITALS = "org.emstrack.ambulance.ambulanceforegroundservice.action.GET_HOSPITALS";
        public final static String GET_BASES = "org.emstrack.ambulance.ambulanceforegroundservice.action.GET_BASES";
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
        private boolean hasBases;
        private boolean isUpdatingLocation;

        public ReconnectionInformation(boolean hasAmbulance, boolean hasOtherAmbulances,
                                       boolean hasHospitals, boolean hasBases,
                                       boolean isUpdatingLocation) {

            this.hasAmbulance = hasAmbulance;
            this.hasOtherAmbulances = hasOtherAmbulances;
            this.hasHospitals = hasHospitals;
            this.hasBases = hasBases;
            this.isUpdatingLocation = isUpdatingLocation;

        }

        public boolean hasAmbulance() { return hasAmbulance; }
        public boolean hasOtherAmbulances() { return hasOtherAmbulances; }
        public boolean hasHospitals() { return hasHospitals; }
        public boolean hasBases() { return hasBases; }
        public boolean isUpdatingLocation() { return isUpdatingLocation; }

        @Override
        public String toString() {
            return String.format("hasAmbulance = %1$b, hasOtherAmbulances = %2$b, hasHospitals = %3$b, hasBases = %4$b, isUpdatingLocation = %5$b",
                    hasAmbulance, hasOtherAmbulances, hasHospitals, hasBases, isUpdatingLocation);
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

        // initialize list of pending call_current
        // currentCallId = -1;
        pendingCalls = new CallStack();

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

        } else if (action.equals(Actions.GET_HOSPITALS)) {

            Log.i(TAG, "GET_HOSPITALS Foreground Intent");

            // Retrieve hospitals
            boolean reconnect = intent.getBooleanExtra(AmbulanceForegroundService.BroadcastExtras.RECONNECT, false);
            retrieveHospitals(uuid, reconnect);

        } else if (action.equals(Actions.GET_BASES)) {

            Log.i(TAG, "GET_BASES Foreground Intent");

            // Retrieve bases
            boolean reconnect = intent.getBooleanExtra(AmbulanceForegroundService.BroadcastExtras.RECONNECT, false);
            retrieveBases(uuid, reconnect);

        } else if (action.equals(Actions.START_LOCATION_UPDATES)) {

            Log.i(TAG, "START_LOCATION_UPDATES Foreground Intent");

            boolean reconnect = intent.getBooleanExtra(AmbulanceForegroundService.BroadcastExtras.RECONNECT, false);
            startLocationUpdates(uuid, reconnect);

        } else if (action.equals(Actions.STOP_LOCATION_UPDATES)) {

            Log.i(TAG, "STOP_LOCATION_UPDATES Foreground Intent");

            // stop call updates
            stopCallUpdates(uuid);

            // stop requesting location updates
            stopLocationUpdates(uuid);

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

                AmbulanceCall ambulanceCall = AmbulanceForegroundService.getCurrentAmbulanceCall();
                Ambulance ambulance = AmbulanceForegroundService.getCurrentAmbulance();
                Call call = AmbulanceForegroundService.getCurrentCall();

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

            // retrieveObject the ambulance that accepted the call and the call id
            int callId = intent.getIntExtra(AmbulanceForegroundService.BroadcastExtras.CALL_ID, -1);

            // next steps to publish information to server (steps 3, 4)
            setAmbulanceCallStatus(callId,
                    AmbulanceCall.statusLabel.get(AmbulanceCall.STATUS_ACCEPTED), uuid);

        } else if (action.equals(Actions.CALL_DECLINE)) {

            Log.i(TAG, "CALL_DECLINE Foreground Intent");

            // retrieveObject the ambulance that declined the call and the call id
            int callId = intent.getIntExtra(AmbulanceForegroundService.BroadcastExtras.CALL_ID, -1);

            // next steps to publish information to server (steps 3, 4)
            requestToDeclineCurrentCall(callId, uuid);

        } else if (action.equals(Actions.CALL_SUSPEND)) {

            Log.i(TAG, "CALL_SUSPEND Foreground Intent");

            // retrieveObject the ambulance that suspended the call and the call id
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

            // retrieveObject list of geofence ids that were entered
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
            MqttAndroidClient androidClient = new MqttAndroidClient(context, _serverUri, clientId);
            client = new MqttProfileClient(androidClient);

        }

        return client;
    }

    public static MqttProfileClient getProfileClient() throws ProfileClientException {

        // lazy initialization
        if (client != null)
            return client;

        // otherwise log and throw exception
        String message = "Failed to retrieveObject profile client.";
        Log.e(TAG, message);
        throw new ProfileClientException(message);

    }

    public static boolean hasProfileClient() { return client != null; }

    /**
     * Get last location
     *
     * @return the last location
     */
    public static android.location.Location getLastLocation() {
        return _lastLocation.getLocation();
    }

    /**
     * Get current ambulance
     *
     * @return the ambulance
     */
    public static Ambulance getCurrentAmbulance() {
        return _ambulance;
    }

    /**
     * Convenience method to retrieveObject current ambulance id.
     * Returns -1 if one does not exist.
     *
     * @return
     */
    public static int getAmbulanceId() {
        if (_ambulance == null)
            return -1;
        else
            return getCurrentAmbulance().getId();
    }

    /**
     * @return current call or nul
     */
    public static Call getCurrentCall() {
        return pendingCalls.getCurrentCall();
    }

    /**
     * @return current ambulancecall or nul
     */
    public static AmbulanceCall getCurrentAmbulanceCall() {
        Call call = pendingCalls.getCurrentCall();
        if (call == null)
            return null;
        return call.getCurrentAmbulanceCall();
    }

    /**
     * @param callId
     * @return call with id callId or null
     */
    public static Call getCall(int callId) {
        return pendingCalls.get(callId);
    }

    /**
     * Get current bases
     *
     * @return the list of bases
     */
    public static List<Location> getBases() {
        return _bases;
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
     * Get pending call_current stack
     *
     * @return pending call_current stack
     */
    public static CallStack getPendingCalls() {
        return pendingCalls;
    }

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
     * Return can updateAmbulance location
     *
     * @return the location updateAmbulance status
     */
    public static boolean canUpdateLocation() { return _canUpdateLocation; }

    /**
     * Set can updateAmbulance location status
     *
     * @param canUpdateLocation the location updateAmbulance status
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
     * Send bulk updates to ambulance
     *
     * @param updates string array
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
            Ambulance ambulance = getCurrentAmbulance();
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
     * Send bulk updates to ambulance
     *
     * @param updates string array
     */
    public boolean updateAmbulance(int ambulanceId, ArrayList<String> updates) {

        // Join updates in array
        String updateArray = "[" + TextUtils.join(",", updates) + "]";

        // send to server
        return updateAmbulance(ambulanceId, updateArray);
    }

    /**
     * Send updateAmbulance to ambulance
     *
     * @param ambulanceId int
     * @param update string
     */
    public boolean updateAmbulance(int ambulanceId, String update) {

        Log.i(TAG, "On updateAmbulance, ambulanceId='" + ambulanceId + "', update='" + update + "'");

        // Form topic and send message
        String topic = String.format("ambulance/%1$d/data", ambulanceId);
        return sendMQTTMessage(topic, update);

    }

    /**
     * Update Waypoint status
     *
     * @param status String
     * @param waypoint Waypoint
     * @param ambulanceId int
     * @param callId int
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
     * Send update waypoint
     *
     * @param update string
     * @param waypointId int
     * @param ambulanceId int
     * @param callId int
     */
    public void updateWaypoint(String update, int waypointId, int ambulanceId, int callId) {

        // Form topic and send message
        String topic = String.format("ambulance/%1$d/call/%2$d/waypoint/%3$d/data",
                ambulanceId, callId, waypointId);
        sendMQTTMessage(topic, update);

    }

    /**
     * Update ambulance status on the server
     *
     * @param ambulanceId int
     * @param status string
     */
    public void updateAmbulanceStatus(String uuid, int ambulanceId, String status) {
        updateAmbulanceStatus(uuid, ambulanceId, status, new Date());
    }

    /**
     * Update ambulance status on the server
     *
     * @param ambulanceId int
     * @param status string
     */
    public void updateAmbulanceStatus(String uuid, int ambulanceId, String status, Date timestamp) {

        if (ambulanceId == getAmbulanceId()) {

            // add status update to ambulanceUpdateFilter
            ambulanceUpdateFilter.update(status, timestamp);

            // Update locally
            Ambulance ambulance = getCurrentAmbulance();
            ambulance.setStatus(status);

            // Broadcast ambulance updateAmbulance
            Intent localIntent = new Intent(BroadcastActions.AMBULANCE_UPDATE);
            sendBroadcastWithUUID(localIntent, uuid);

        } else {

            // publish status to server
            String update = String.format("{\"status\":\"%1$s\"}", status);
            updateAmbulance(ambulanceId, update);

            // Has ambulance?
            Ambulance ambulance = getCurrentAmbulance();
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

        // remove ambulance
        removeAmbulance();

        // remove hospital map
        removeHospitals();

        // remove bases map
        removeBases();

        // remove ambulance map
        removeOtherAmbulances();

        // stop geofences
        // TODO: Should we wait for completion?
        removeAllGeofences(null);

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
                    Intent localIntent = new Intent(org.emstrack.models.util.BroadcastActions.SUCCESS);
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
                    Intent localIntent = new Intent(org.emstrack.models.util.BroadcastActions.FAILURE);
                    localIntent.putExtra(org.emstrack.models.util.BroadcastExtras.MESSAGE, message);
                    sendBroadcastWithUUID(localIntent, uuid);

                }

            });

        } catch (MqttException e) {

            Log.d(TAG, "Failed to disconnect.");

            // Broadcast failure
            Intent localIntent = new Intent(org.emstrack.models.util.BroadcastActions.FAILURE);
            localIntent.putExtra(org.emstrack.models.util.BroadcastExtras.MESSAGE, "Could not disconnect: " + e.toString());
            sendBroadcastWithUUID(localIntent, uuid);

        } catch (Exception e) {

            Log.d(TAG, "Failed to disconnect.");

            // Broadcast failure
            Intent localIntent = new Intent(org.emstrack.models.util.BroadcastActions.FAILURE);
            localIntent.putExtra(org.emstrack.models.util.BroadcastExtras.MESSAGE, "Could not disconnect: " + e.toString());
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
                _bases != null,
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

            final int ambulanceId = _ambulance.getId();
            final String ambulanceIdentifier = _ambulance.getIdentifier();

            // Remove current ambulance
            // TODO: Does it need to be asynchrounous?
            removeAmbulance(true);

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
                        Ambulance ambulance = getCurrentAmbulance();
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
                        removeHospitals(true);

                        // Retrieve hospital
                        Intent hospitalIntent = new Intent(AmbulanceForegroundService.this,
                                AmbulanceForegroundService.class);
                        hospitalIntent.setAction(Actions.GET_HOSPITALS);
                        hospitalIntent.putExtra(AmbulanceForegroundService.BroadcastExtras.RECONNECT, true);
                        startService(hospitalIntent);
                        
                    }

                    if (_reconnectionInformation.hasBases()) {

                        Log.i(TAG, "Retrieving bases.");

                        // Remove bases
                        removeBases(true);

                        // Retrieve bases
                        Intent basesIntent = new Intent(AmbulanceForegroundService.this,
                                AmbulanceForegroundService.class);
                        basesIntent.setAction(Actions.GET_BASES);
                        basesIntent.putExtra(AmbulanceForegroundService.BroadcastExtras.RECONNECT, true);
                        startService(basesIntent);

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

                        // Broadcast success
                        Intent localIntent = new Intent(org.emstrack.models.util.BroadcastActions.SUCCESS);
                        sendBroadcastWithUUID(localIntent, uuid);

                        // Subscribe to error topic
                        subscribeToError(username);

                        // Get token for api access
                        Credentials credentials = new Credentials(username, password, serverApi);

                        // Retrieve token
                        APIServiceGenerator.setServerUri(credentials.getServerURI());
                        APIServiceGenerator.setToken(null);
                        APIService service = APIServiceGenerator.createService(APIService.class);
                        retrofit2.Call<Token> call = service.getToken(credentials);
                        new OnAPICallComplete<Token>(call) {

                            @Override
                            public void onSuccess(Token token) {

                                // save token
                                APIServiceGenerator.setToken(token.getToken());

                                // Retrieve bases
                                retrieveBases(uuid, false);

                            }

                            @Override
                            public void onFailure(Throwable t) {
                                Log.d(TAG, "Could not retrieve API token.");
                            }

                        }.start();

                        // set callback for handling loss of connection/reconnection
                        getProfileClient(AmbulanceForegroundService.this).setCallback(AmbulanceForegroundService.this);

                    }

                    @Override
                    public void onFailure(Throwable exception) {

                        Log.d(TAG, "Failed to retrieve profile.");

                        // Build error message
                        String message = "Failed to retrieve profile";
                        if (exception != null)
                            message += ": " + exception.toString();

                        // Broadcast failure
                        Intent localIntent = new Intent(org.emstrack.models.util.BroadcastActions.FAILURE);
                        localIntent.putExtra(org.emstrack.models.util.BroadcastExtras.MESSAGE, message);
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
                            Intent localIntent = new Intent(org.emstrack.models.util.BroadcastActions.FAILURE);
                            localIntent.putExtra(org.emstrack.models.util.BroadcastExtras.MESSAGE, message);
                            sendBroadcastWithUUID(localIntent, uuid);

                        }

                    });

                } catch (MqttException exception) {

                    // Build error message
                    String message = getResources().getString(R.string.error_connection_failed, exception.toString());

                    // Broadcast failure
                    Intent localIntent = new Intent(org.emstrack.models.util.BroadcastActions.FAILURE);
                    localIntent.putExtra(org.emstrack.models.util.BroadcastExtras.MESSAGE, message);
                    sendBroadcastWithUUID(localIntent, uuid);

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

        // Is ambulance id valid?
        if (ambulanceId < 0) {

            // Broadcast failure
            Intent localIntent = new Intent(org.emstrack.models.util.BroadcastActions.FAILURE);
            localIntent.putExtra(org.emstrack.models.util.BroadcastExtras.MESSAGE, getString(R.string.invalidAmbulanceId));
            sendBroadcastWithUUID(localIntent, uuid);

            return;
        }

        // Is ambulance new and not reconnect?
        Ambulance ambulance = getCurrentAmbulance();
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
            Intent localIntent = new Intent(org.emstrack.models.util.BroadcastActions.FAILURE);
            localIntent.putExtra(org.emstrack.models.util.BroadcastExtras.MESSAGE, getString(R.string.couldNotSubscribeToAmbulance));
            sendBroadcastWithUUID(localIntent, uuid);

        }

        try {

            // Start retrieving data
            profileClient.subscribe(String.format("ambulance/%1$d/data", ambulanceId),1,
                    (topic, message) -> {

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
                            Ambulance ambulance1 = gson
                                    .fromJson(new String(message.getPayload()),
                                            Ambulance.class);

                            // Has location been updated?
                            if (_lastLocation == null || ambulance1.getTimestamp().after(_lastLocation.getTimestamp())) {

                                // Update last location
                                _lastLocation = new AmbulanceUpdate();
                                android.location.Location location = new android.location.Location("FusedLocationClient");
                                location.setLatitude(ambulance1.getLocation().getLatitude());
                                location.setLongitude(ambulance1.getLocation().getLongitude());
                                _lastLocation.setLocation(location);
                                _lastLocation.setBearing(ambulance1.getOrientation());
                                _lastLocation.setTimestamp(ambulance1.getTimestamp());

                            }

                            // Set current ambulance
                            _ambulance = ambulance1;

                            // stop updates?
                            MqttProfileClient profileClient1 = getProfileClient(AmbulanceForegroundService.this);
                            String clientId = profileClient1.getClientId();
                            if (isUpdatingLocation() &&
                                    (ambulance1.getLocationClientId() == null ||
                                            !clientId.equals(ambulance1.getLocationClientId()))) {

                                // turn off tracking
                                Intent localIntent = new Intent(AmbulanceForegroundService.this, AmbulanceForegroundService.class);
                                localIntent.setAction(Actions.STOP_LOCATION_UPDATES);
                                startService(localIntent);

                            }

                            if (firstTime) {

                                // Broadcast success
                                Intent localIntent = new Intent(org.emstrack.models.util.BroadcastActions.SUCCESS);
                                sendBroadcastWithUUID(localIntent, uuid);

                            }

                            // Broadcast ambulance updateAmbulance
                            Intent localIntent = new Intent(BroadcastActions.AMBULANCE_UPDATE);
                            sendBroadcastWithUUID(localIntent, uuid);

                        } catch (Exception e) {

                            Log.i(TAG, "Could not parse ambulance update.");

                            // Broadcast failure
                            Intent localIntent = new Intent(org.emstrack.models.util.BroadcastActions.FAILURE);
                            localIntent.putExtra(org.emstrack.models.util.BroadcastExtras.MESSAGE, getString(R.string.couldNotParseAmbulance));
                            sendBroadcastWithUUID(localIntent, uuid);

                        }

                    });

        } catch (MqttException e) {

            Log.d(TAG, "Could not subscribe to ambulance data");

            // Broadcast failure
            Intent localIntent = new Intent(org.emstrack.models.util.BroadcastActions.FAILURE);
            localIntent.putExtra(org.emstrack.models.util.BroadcastExtras.MESSAGE, getString(R.string.couldNotSubscribeToAmbulance));
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

        Ambulance ambulance = getCurrentAmbulance();
        if (ambulance == null ) {
            Log.i(TAG,"No ambulance to remove.");
            return;
        }

        // Logout and unsubscribe if not a reconnect
        if (!reconnect) {

            // remove call updates
            stopCallUpdates(null);

            // remove location updates
            stopLocationUpdates(null);

            // retrieveObject ambulance id
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
                profileClient.subscribe("hospital/" + hospitalId + "/data",1,
                        (topic, message) -> {

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

                                    // Broadcast hospitals updateAmbulance
                                    Intent localIntent = new Intent(org.emstrack.models.util.BroadcastActions.SUCCESS);
                                    sendBroadcastWithUUID(localIntent, uuid);

                                }

                            } else {

                                // Modify hospital map
                                hospitals.put(hospitalId, hospital);

                                // Broadcast hospitals updateAmbulance
                                Intent localIntent = new Intent(BroadcastActions.HOSPITALS_UPDATE);
                                sendBroadcastWithUUID(localIntent);

                            }
                        });

            } catch (MqttException e) {
                Log.d(TAG, "Could not subscribe to hospital data");

                // Broadcast failure
                Intent localIntent = new Intent(org.emstrack.models.util.BroadcastActions.FAILURE);
                localIntent.putExtra(org.emstrack.models.util.BroadcastExtras.MESSAGE, getString(R.string.couldNotSubscribeToHospital));
                sendBroadcastWithUUID(localIntent, uuid);

            }

        }

    }

    /**
     * Retrieve bases
     */
    public void retrieveBases(final String uuid, boolean reconnect) {

        // Remove current bases
        removeBases(reconnect);

        // Log
        Log.i(TAG, "Retrieving bases...");

        // Retrieve bases
        new OnAPICallComplete<List<Location>>(
                APIServiceGenerator.createService(APIService.class).getLocationsByType("Base")
        ) {

            @Override
            public void onSuccess(List<Location> locations) {

                Log.d(TAG, String.format("%1$d bases retrieved.", locations.size()));
                _bases = locations;

            }

            @Override
            public void onFailure(Throwable t) {

                Log.i(TAG, "Could not retrieve bases, exception: " + t.toString());

                // Broadcast failure
                Intent localIntent = new Intent(org.emstrack.models.util.BroadcastActions.FAILURE);
                localIntent.putExtra(org.emstrack.models.util.BroadcastExtras.MESSAGE,
                        org.emstrack.ambulance.R.string.couldNotRetrieveBases);
                sendBroadcastWithUUID(localIntent, uuid);

            }

        }.start();

    }

    /**
     * Remove current bases
     */
    public void removeBases() { removeBases(false); }

    /**
     * Remove current bases
     */
    public void removeBases(boolean reconnect) {

        List<Location> bases = getBases();
        if (bases == null || bases.size() == 0) {
            Log.i(TAG, "No bases to remove.");
            return;
        }

        // Unsubscribe only if not reconnect
        if (!reconnect) {

            // Remove bases
            _bases = null;

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
                profileClient.subscribe("ambulance/" + ambulanceId + "/data",1,
                        (topic, message) -> {

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
                                    Intent localIntent = new Intent(org.emstrack.models.util.BroadcastActions.SUCCESS);
                                    sendBroadcastWithUUID(localIntent, uuid);

                                }

                            } else {

                                // Update ambulance map
                                ambulances.put(ambulanceId, ambulance);

                                // Broadcast ambulances updateAmbulance
                                Intent localIntent = new Intent(BroadcastActions.OTHER_AMBULANCES_UPDATE);
                                sendBroadcastWithUUID(localIntent);

                            }
                        });

            } catch (MqttException e) {
                Log.d(TAG, "Could not subscribe to ambulance data");

                // Broadcast failure
                Intent localIntent = new Intent(org.emstrack.models.util.BroadcastActions.FAILURE);
                localIntent.putExtra(org.emstrack.models.util.BroadcastExtras.MESSAGE, getString(R.string.couldNotSubscribeToAmbulance));
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
            Intent localIntent = new Intent(org.emstrack.models.util.BroadcastActions.SUCCESS);
            sendBroadcastWithUUID(localIntent, uuid);
            return;

        }

        if (!canUpdateLocation()) {

            // Broadcast failure and return
            Intent localIntent = new Intent(org.emstrack.models.util.BroadcastActions.FAILURE);
            localIntent.putExtra(org.emstrack.models.util.BroadcastExtras.MESSAGE, getString(R.string.cannotUseLocationServices));
            sendBroadcastWithUUID(localIntent, uuid);
            return;

        }

        // Logged in?
        Ambulance ambulance = AmbulanceForegroundService.getCurrentAmbulance();
        if (ambulance == null) {

            // Broadcast failure and return
            Intent localIntent = new Intent(org.emstrack.models.util.BroadcastActions.FAILURE);
            localIntent.putExtra(org.emstrack.models.util.BroadcastExtras.MESSAGE, getString(R.string.noAmbulanceSelected));
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
                                Log.e(TAG, message);

                                // Broadcast failure and return
                                Intent localIntent = new Intent(org.emstrack.models.util.BroadcastActions.FAILURE);
                                localIntent.putExtra(org.emstrack.models.util.BroadcastExtras.MESSAGE, message);
                                sendBroadcastWithUUID(localIntent, uuid);

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

/*
            // What to do when service completes?
            new OnServicesComplete(this,
                    new String[] {
                            org.emstrack.models.util.BroadcastActions.SUCCESS,
                            BroadcastActions.AMBULANCE_UPDATE
                    },
                    new String[] {org.emstrack.models.util.BroadcastActions.FAILURE},
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

                @Override
                public void onReceive(Context context, Intent intent) {

                    Log.i(TAG, "onReceive");

                    // Retrieve action
                    String action = intent.getAction();

                    // Intercept success
                    if (action.equals(org.emstrack.models.util.BroadcastActions.SUCCESS))
                        // prevent propagation, still waiting for AMBULANCE_UPDATE
                        return;

                    // Intercept AMBULANCE_UPDATE
                    if (action.equals(BroadcastActions.AMBULANCE_UPDATE))
                        // Inject uuid into AMBULANCE_UPDATE
                        intent.putExtra(org.emstrack.models.util.BroadcastExtras.UUID, getUuid());

                    // Call super
                    super.onReceive(context, intent);
                }

            };
*/

        } else {

            Log.i(TAG, "Ambulance is not available");

            // ambulance is not available, report failure
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

                    // retrieveObject profile client, initialize if not present
                    final MqttProfileClient profileClient = AmbulanceForegroundService.getProfileClient(AmbulanceForegroundService.this);

                    // retrieve ambulance
                    Ambulance ambulance = getCurrentAmbulance();
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

        _lastLocation = null;

        // Already started?
        if (!_updatingLocation) {
            Log.i(TAG, "Not requesting location updates. Skipping.");
            return;
        }

        // retrieveObject profile client
        MqttProfileClient profileClient = AmbulanceForegroundService.getProfileClient(this);

        // clear location_client on server?
        Ambulance ambulance = getCurrentAmbulance();
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
            startService(intent);

        } else {

            Log.i(TAG, "No need to clear location client on server");

        }

        // remove on fusedLocationclient
        fusedLocationClient.removeLocationUpdates(locationCallback)
                .addOnSuccessListener(
                        aVoid -> {

                            Log.i(TAG, "Stopping location updates");
                            _updatingLocation = false;

                            // Broadcast location change
                            Intent changeIntent = new Intent(BroadcastActions.LOCATION_UPDATE_CHANGE);
                            sendBroadcastWithUUID(changeIntent, uuid);

                        })
                .addOnFailureListener(
                        e -> {
                            Log.i(TAG, "Failed to stop location updates");
                            e.printStackTrace();
                        });

    }

    /**
     * Begin call updates
     *
     * @param uuid
     */
    public void beginCallUpdates(final String uuid) {

        Log.i(TAG, "Subscribing to call status");

        // Logged in?
        final Ambulance ambulance = AmbulanceForegroundService.getCurrentAmbulance();
        if (ambulance == null) {

            // Broadcast failure and return
            Intent localIntent = new Intent(org.emstrack.models.util.BroadcastActions.FAILURE);
            localIntent.putExtra(org.emstrack.models.util.BroadcastExtras.MESSAGE, getString(R.string.noAmbulanceSelected));
            sendBroadcastWithUUID(localIntent, uuid);
            return;

        }

        // retrieveObject profile client
        MqttProfileClient profileClient = AmbulanceForegroundService.getProfileClient(this);

        // subscribe to call status
        try {

            final int ambulanceId = ambulance.getId();
            profileClient.subscribe(
                    String.format("ambulance/%1$d/call/+/status", ambulanceId),2,
                    (topic, message) -> {

                        // Keep subscription to call_current to make sure we receive latest updates
                        Log.i(TAG, "Retrieving call statuses, currentCallId=" + pendingCalls.getCurrentCallId());

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
                                if (!pendingCalls.contains(callId)) {

                                    Log.d(TAG, "New call");

                                    // this is a new call, subscribe
                                    processOrSubscribeToCall(callId, uuid);

                                } else {

                                    // this is an existing call

                                    Log.d(TAG, "Existing call");

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
                                        if (pendingCalls.isCurrentCall(callId)) {

                                            // clean up
                                            cleanUpCall(callId, uuid);

                                            // broadcast CALL_COMPLETED
                                            Intent callCompletedIntent = new Intent(BroadcastActions.CALL_COMPLETED);
                                            callCompletedIntent.putExtra(AmbulanceForegroundService.BroadcastExtras.CALL_ID, callId);
                                            sendBroadcastWithUUID(callCompletedIntent, uuid);

                                        }

                                    }

                                    // Update status, server updates may take a while
                                    pendingCalls.get(callId).getAmbulanceCall(ambulanceId).setStatus(newStatus);

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

        if (pendingCalls.contains(callId) && !pendingCalls.hasCurrentOrPendingCall()) {

            // Is this an existing call and no call_current are currently being served?

            processNextCall(uuid);
            return;

        }

        // Subscribe to call to retrieveObject call information

        MqttProfileClient profileClient = getProfileClient(this);

        try {

            Log.i(TAG, "Subscribing to call/" + callId);

            profileClient.subscribe(String.format("call/%1$s/data", callId),2,
                    (topic, message) -> {

                        // Keep subscription to call_current to make sure we receive latest updates
                        Log.i(TAG, "Retrieving call data");

                        // parse call id data
                        GsonBuilder gsonBuilder = new GsonBuilder();
                        gsonBuilder.setFieldNamingPolicy(FieldNamingPolicy.LOWER_CASE_WITH_UNDERSCORES);
                        Gson gson = gsonBuilder.create();

                        String payload = new String(message.getPayload());
                        if (payload == null || payload.isEmpty())
                            // This can happen when clearing a call
                            // It will also broadcast a "ended" state so call will be finalized then
                            return;

                        try {

                            // Parse call data
                            Call call = gson.fromJson(payload, Call.class);
                            if (call == null)
                                throw new Exception("Got null call");

                            // Sort waypoints
                            call.sortWaypoints(true);

                            // Is this an existing call?
                            if (pendingCalls.contains(call.getId())) {

                                // Update call information
                                updateCallInformation(call, uuid);

                            } else if (call.getStatus() == "E") {

                                // Call has ended and is not longer on the queue, ignore
                                Log.d(TAG, "Ignoring call " + call.getId());

                            } else {

                                // Add to pending calls
                                pendingCalls.put(call);

                                // if no current, process next
                                if (!pendingCalls.hasCurrentOrPendingCall())
                                    processNextCall(uuid);

                            }


                        } catch (Exception e) {

                            Log.d(TAG, "Failed to parse call '" + payload + "'. Excpetion='" + e + "'");

                            // Broadcast failure
                            Intent localIntent = new Intent(org.emstrack.models.util.BroadcastActions.FAILURE);
                            localIntent.putExtra(org.emstrack.models.util.BroadcastExtras.MESSAGE, getString(R.string.couldNotParseCallData));
                            sendBroadcastWithUUID(localIntent, uuid);

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

        if (call.getId() == pendingCalls.getCurrentCallId()) {

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
                pendingCalls.remove(pendingCalls.getCurrentCallId());

            } else {

                // Update call information
                pendingCalls.put(call);

            }

        }

    }

    public void stopCallUpdates(final String uuid) {

        // retrieveObject profile client
        MqttProfileClient profileClient = AmbulanceForegroundService.getProfileClient(this);
        Ambulance ambulance = getCurrentAmbulance();
        if (ambulance != null && profileClient != null) {

            // terminate current call, does not process next
            if (pendingCalls.hasCurrentCall())
                cleanUpCall(pendingCalls.getCurrentCallId(), uuid, false);

            CallStack.CallStackIterator iterator = pendingCalls.iterator();
            while (iterator.hasNext()) {

                Map.Entry<Integer, Call> pair = iterator.next();
                int callId = pair.getKey();

                // unsubscribe from call
                Log.i(TAG, "Unsubscribe from call/" + callId + "/data");
                try {
                    profileClient.unsubscribe("call/" + callId + "/data");
                } catch (MqttException e) {
                    Log.d(TAG, "Could not unsubscribe to 'call/" + callId + "/data'");
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



        } else {

            Log.i(TAG, "No need to stop call updates");

        }

    }

    public void cleanUpCall(int callId, String uuid) {
        cleanUpCall(callId, uuid, true);
    }

    public void cleanUpCall(int callId, String uuid, boolean processNext) {

        Log.d(TAG, "Cleaning up call '" + callId + "'");

        if (callId < 0) {
            Log.d(TAG, "CallId < 0, abort!");
            return;
        }

        try {

            // If current call, stop geofences first
            if (callId == pendingCalls.getCurrentCallId()) {

                // Get current call
                Call call = pendingCalls.getCurrentCall();

                // call is current call
                Ambulance ambulance = getCurrentAmbulance();
                AmbulanceCall ambulanceCall = call.getCurrentAmbulanceCall();

                // Stop geofences
                Log.i(TAG, "Stopping geofences");

                List<String> requestIds = new ArrayList<>();
                for (Map.Entry<String, Geofence> entry : _geofences.entrySet()) {

                    Waypoint waypoint = entry.getValue().getWaypoint();
                    if (ambulanceCall.containsWaypoint(waypoint)) {
                        // Add to list to remove
                        requestIds.add(entry.getKey());
                    }

                }

                // Stop geofences
                stopGeofence(uuid, requestIds);

                // Set status as available
                Log.d(TAG, "Set ambulance '" + getCurrentAmbulance().getId() + "' available");
                updateAmbulanceStatus(uuid, ambulance.getId(), Ambulance.STATUS_AVAILABLE);

                // Release current call
                pendingCalls.setPendingCall(false);

            } else
                Log.d(TAG, "Call '" + callId + "' is not current call (" + pendingCalls.getCurrentCallId() + ")");

            // Should stop listening to call?
            if (callId > 0) {

                // Get ambulancecall
                Ambulance ambulance = getCurrentAmbulance();
                AmbulanceCall ambulanceCall = pendingCalls.get(callId).getAmbulanceCall(ambulance.getId());

                if (ambulanceCall.getStatus().equals(AmbulanceCall.STATUS_COMPLETED)) {

                    Log.i(TAG, "Unsubscribe from call/" + callId + "/data");

                    // unsubscribe from call
                    try {
                        // retrieveObject profile client
                        MqttProfileClient profileClient = AmbulanceForegroundService.getProfileClient(this);
                        profileClient.unsubscribe("call/" + callId + "/data");
                    } catch (MqttException e) {
                        Log.d(TAG, "Could not unsubscribe from 'call/" + callId + "/data'");
                    }

                    // remove call from the queue
                    pendingCalls.remove(callId);

                }

            }

            if (pendingCalls.hasPendingCall()) {

                // It was prompting user, most likely declined
                Log.d(TAG, "Was prompting user, opening up to new call_current");

                // open up to new call_current
                pendingCalls.setPendingCall(false);

            }

            if (processNext) {

                Log.d(TAG, "Processing next call...");

                // process next call, expect if callId
                processNextCall(uuid, callId);

            }

        } catch (Exception e) {

            Log.d(TAG, "Could not clean up call. Exception='" + e + "'");

        }

    }

    public void requestToFinishCurrentCall(String uuid) {

        // if currently not serving call
        if (!pendingCalls.hasCurrentCall()) {

            Log.d(TAG, "Can't finish call: not serving any call.");

            Intent localIntent = new Intent(org.emstrack.models.util.BroadcastActions.FAILURE);
            localIntent.putExtra(org.emstrack.models.util.BroadcastExtras.MESSAGE, getString(R.string.couldNotFinishCall));
            sendBroadcastWithUUID(localIntent, uuid);

            return;

        }

        // Set ambulanceCall status to completed locally
        // This prevents further processing of this call
        Call call = pendingCalls.getCurrentCall();
        call.getCurrentAmbulanceCall().setStatus(AmbulanceCall.STATUS_COMPLETED);

        // publish finished status to server
        setAmbulanceCallStatus(call.getId(),
                AmbulanceCall.statusLabel.get(AmbulanceCall.STATUS_COMPLETED), uuid);

    }

    public void requestToDeclineCurrentCall(int callId, String uuid) {

        // if currently serving call, can't decline
        if (pendingCalls.hasCurrentCall()) {

            Log.d(TAG, "Can't decline call: currently serving call/" + pendingCalls.getCurrentCallId());

            Intent localIntent = new Intent(org.emstrack.models.util.BroadcastActions.FAILURE);
            localIntent.putExtra(org.emstrack.models.util.BroadcastExtras.MESSAGE, getString(R.string.couldNotDeclineCall));
            sendBroadcastWithUUID(localIntent, uuid);

            return;

        }

        try {

            // Set ambulanceCall status to suspended locally
            // This prevents further processing of this call
            Call call = pendingCalls.get(callId);
            Ambulance ambulance = AmbulanceForegroundService.getCurrentAmbulance();
            call.getAmbulanceCall(ambulance.getId()).setStatus(AmbulanceCall.STATUS_DECLINED);

        } catch (Exception e) {

            Log.d(TAG, "Could not set ambulance call status to declined. Moving on...");

        }

        // publish declined as status
        setAmbulanceCallStatus(callId,
                AmbulanceCall.statusLabel.get(AmbulanceCall.STATUS_DECLINED), uuid);

    }

    public void requestToSuspendCurrentCall(int callId, String uuid) {

        // if not currently serving call, can't suspend
        if (!pendingCalls.hasCurrentCall()) {

            Log.d(TAG, "Can't suspend call: not currently serving any call");

            Intent localIntent = new Intent(org.emstrack.models.util.BroadcastActions.FAILURE);
            localIntent.putExtra(org.emstrack.models.util.BroadcastExtras.MESSAGE, getString(R.string.couldNotSuspendCall));
            sendBroadcastWithUUID(localIntent, uuid);

            return;

        }

        // Set ambulanceCall status to suspended locally
        // This prevents further processing of this call
        Call call = pendingCalls.getCurrentCall();
        call.getCurrentAmbulanceCall().setStatus(AmbulanceCall.STATUS_SUSPENDED);

        // publish suspended as status
        setAmbulanceCallStatus(callId,
                AmbulanceCall.statusLabel.get(AmbulanceCall.STATUS_SUSPENDED), uuid);

    }

    public void processNextCall(String uuid) {
        processNextCall(uuid, -1);
    }

    public void processNextCall(String uuid, int exceptCallId) {

        // if current call, bark
        if (pendingCalls.hasCurrentCall()) {

            Log.d(TAG, "Will not process next call: currently serving call/" + pendingCalls.getCurrentCallId());

            Intent localIntent = new Intent(org.emstrack.models.util.BroadcastActions.FAILURE);
            localIntent.putExtra(org.emstrack.models.util.BroadcastExtras.MESSAGE, getString(R.string.willNotProcessNextCall));
            sendBroadcastWithUUID(localIntent, uuid);
            return;

        }

        // if prompting user, ignore
        if (pendingCalls.hasPendingCall()) {

            Log.d(TAG, "Will not process next call: currently prompting user to accept call.");
            return;

        }

        // Get current ambulance
        Ambulance ambulance = AmbulanceForegroundService.getCurrentAmbulance();
        if (ambulance == null) {

            Log.d(TAG, "Ambulance is null. This should never happen");

            Intent localIntent = new Intent(org.emstrack.models.util.BroadcastActions.FAILURE);
            localIntent.putExtra(org.emstrack.models.util.BroadcastExtras.MESSAGE,
                    getString(R.string.willNotProcessNextCall));
            sendBroadcastWithUUID(localIntent, uuid);
            return;

        }

        // Get next suitable call
        Call nextCall = pendingCalls.getNextCall(ambulance.getId());
        if (nextCall != null && nextCall.getId() != exceptCallId) {

            Log.i(TAG, "Will prompt user to accept call");

            // set pending call
            pendingCalls.setPendingCall(true);

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
            sendBroadcastWithUUID(callPromptIntent, uuid);

        } else {

            Log.i(TAG, "No more pending or suspended calls");

        }

    }

    // handles steps 3 and 4 of Accepting Calls
    public void setAmbulanceCallStatus(int callId, String status, String uuid) {

        Log.i(TAG, "Setting call/" + callId + "/status to '" + status + "'");

        MqttProfileClient profileClient = getProfileClient(AmbulanceForegroundService.this);
        Ambulance ambulance = getCurrentAmbulance();
        if (ambulance != null) {

            // step 4: publish accepted to server
            String path = String.format("user/%1$s/client/%2$s/ambulance/%3$s/call/%4$s/status",
                    profileClient.getUsername(), profileClient.getClientId(), ambulance.getId(), callId);

            // publish status to server
            publishToPath(status, path, uuid);

        } else {

            Log.d(TAG, "Ambulance not found while in acceptCall()");

            Intent localIntent = new Intent(org.emstrack.models.util.BroadcastActions.FAILURE);
            localIntent.putExtra(org.emstrack.models.util.BroadcastExtras.MESSAGE, getString(R.string.couldNotFindAmbulance));
            sendBroadcastWithUUID(localIntent, uuid);
        }

    }

    // handles steps 6 to 7
    public void setCallAccepted(final int callId, final String uuid) {

        // Retrieve call
        Call call = pendingCalls.get(callId);
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

        // Fail if servicing another call
        if (pendingCalls.hasCurrentCall() && pendingCalls.getCurrentCallId() != call.getId()) {
            String message = String.format("Can't set call as accepted: already servicing call '%1$d'", pendingCalls.getCurrentCallId());
            throw new AmbulanceForegroundServiceException(message);
        }

        // Fails if call is not in stack
        if (!pendingCalls.contains(call.getId())) {
            String message = String.format("Call '%1$d' is not in the current call stack", pendingCalls.getCurrentCallId());
            throw new AmbulanceForegroundServiceException(message);
        }

        // Get current ambulance and set call to it
        Ambulance ambulance = AmbulanceForegroundService.getCurrentAmbulance();
        call.setCurrentAmbulanceCall(ambulance.getId());
        AmbulanceCall ambulanceCall = call.getCurrentAmbulanceCall();

        // This is an update, what is next waypoint?
        // Do this before updating...
        Waypoint nextWaypoint = null;
        if (pendingCalls.hasCurrentCall()) {

            // Get current ambulancecall and next waypoint
            AmbulanceCall currentAmbulanceCall = AmbulanceForegroundService.getCurrentAmbulanceCall();
            nextWaypoint = currentAmbulanceCall.getNextWaypoint();

        }
        Log.d(TAG, "Next waypoint is " + (nextWaypoint == null ? "'null'" : "'" + nextWaypoint.getId() + "'"));

        // Update call in stack and set as current call
        pendingCalls.put(call);
        pendingCalls.setCurrentCall(call.getId());

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

            Log.i(TAG, "publishing " + payload + " to " + path);

            profileClient.publish(path, payload, 2, false);

        } catch (MqttException e) {

            Log.d(TAG, path);

            Intent localIntent = new Intent(org.emstrack.models.util.BroadcastActions.FAILURE);
            localIntent.putExtra(org.emstrack.models.util.BroadcastExtras.MESSAGE, getString(R.string.couldNotPublish, path));
            sendBroadcastWithUUID(localIntent, uuid);
        }
    }

    /*
     **
     */
    public void replyToGeofenceTransitions(String uuid, Geofence geofence, String action) {

        // if currently not serving call
        if (!pendingCalls.hasCurrentOrPendingCall()) {

            Log.d(TAG, "Ignoring geofence transition: not serving any call.");

            return;

        }

        Ambulance ambulance = getCurrentAmbulance();
        if (ambulance == null) {

            Log.d(TAG, "Ambulance not found while in replyToTransition()");

            Intent localIntent = new Intent(org.emstrack.models.util.BroadcastActions.FAILURE);
            localIntent.putExtra(org.emstrack.models.util.BroadcastExtras.MESSAGE, getString(R.string.couldNotFindAmbulance));
            sendBroadcastWithUUID(localIntent, uuid);
            return;
        }

        Call call = getCurrentCall();
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

        // We use FLAG_UPDATE_CURRENT so that we retrieveObject the same pending intent back when
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
                            Intent localIntent = new Intent(org.emstrack.models.util.BroadcastActions.SUCCESS);
                            sendBroadcastWithUUID(localIntent, uuid);

                        })
                .addOnFailureListener(
                        e -> {

                            // Failed to remove geofences
                            Log.e(TAG, "FAILED TO REMOVE GEOFENCES: " + e.toString());

                            // Broadcast failure and return
                            Intent localIntent = new Intent(org.emstrack.models.util.BroadcastActions.FAILURE);
                            localIntent.putExtra(org.emstrack.models.util.BroadcastExtras.MESSAGE, getString(R.string.failedToRemoveGeofence));
                            sendBroadcastWithUUID(localIntent, uuid);
                            return;

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

                            })
                    .addOnFailureListener(
                            e -> {
                                // Failed to remove geofences
                                Log.e(TAG, "FAILED TO REMOVE GEOFENCES: " + e.toString());
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
