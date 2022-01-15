package org.emstrack.ambulance.fragments;

import static org.emstrack.ambulance.util.LatLon.calculateDistanceHaversine;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.graphics.PorterDuff;
import android.location.Location;
import android.os.Bundle;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;
import androidx.localbroadcastmanager.content.LocalBroadcastManager;

import android.util.Log;
import android.util.SparseArray;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;

import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationCallback;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationResult;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.BitmapDescriptor;
import com.google.android.gms.maps.model.CameraPosition;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.LatLngBounds;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;

import org.emstrack.ambulance.MainActivity;
import org.emstrack.ambulance.models.AmbulanceAppData;
import org.emstrack.ambulance.services.AmbulanceForegroundService;
import org.emstrack.ambulance.R;
import org.emstrack.ambulance.util.LatLngInterpolator;
import org.emstrack.ambulance.util.MarkerAnimation;
import org.emstrack.ambulance.util.VehicleUpdate;
import org.emstrack.ambulance.util.VehicleUpdateFilter;
import org.emstrack.ambulance.util.BitmapUtils;
import org.emstrack.ambulance.util.DragHelper;
import org.emstrack.ambulance.util.SparseArrayUtils;
import org.emstrack.models.Call;
import org.emstrack.models.Settings;
import org.emstrack.models.Ambulance;
import org.emstrack.models.AmbulanceCall;
import org.emstrack.models.GPSLocation;
import org.emstrack.models.Hospital;
import org.emstrack.models.Waypoint;

import java.util.Collection;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

// TODO: Implement listener to ambulance changes

public class MapFragment extends Fragment implements OnMapReadyCallback, GoogleMap.OnCameraIdleListener {

    public final int ZOOM_LEVEL_STOPPED = 19;
    public final int ZOOM_LEVEL_SMALL_SPEEDS = 18;
    public final int ZOOM_LEVEL_MEDIUM_SPEEDS = 16;
    public final int ZOOM_LEVEL_HIGH_SPEEDS = 15;

    private static final String TAG = MapFragment.class.getSimpleName();

    private final float defaultZoom = 17;
    private final int defaultPadding = 50;

    private MainActivity activity;
    View rootView;
    private View mapToolbarLayout;
    private DragHelper toolbarDragHelper;

    private Map<String, String> ambulanceStatus;

    private Map<Integer, Marker> ambulanceMarkers;
    private Map<Integer, Marker> hospitalMarkers;
    private Map<Integer, Marker> waypointMarkers;

    private boolean showToolbar = false;

    private ImageView compassButton;
    private boolean centerCurrentAmbulance = false;

    private ImageView showAmbulanceButton;

    private ImageView showAmbulancesButton;
    private boolean showAmbulances = false;
    private boolean showOfflineAmbulances = false;

    private ImageView showHospitalsButton;
    private boolean showHospitals = false;

    private ImageView showWaypointsButton;
    private boolean showWaypoints = false;

    private boolean myLocationEnabled;
    private boolean useMyLocation = false;

    private float zoomLevel = defaultZoom;
    private LatLng target;
    private float bearing;

    private GoogleMap googleMap;
    private AmbulancesUpdateBroadcastReceiver receiver;

    private GPSLocation defaultLocation;

    private boolean centerAtDefault;

    private int buttonOnColor;
    private int buttonOffColor;
    private int buttonAlertColor;

    private LatLng centerLatLng;
    private boolean doneOnResume;

    private FusedLocationProviderClient fusedLocationClient;
    private LocationCallback locationCallback;
    private VehicleUpdateFilter updateFilter;
    private boolean isAnimatingMarkerAndCamera;
    private AnimateBuffer animateBuffer;

    private static Map<String, BitmapDescriptor> iconBitmapDescriptors = new HashMap<>();

    private static void initializeMarkers(Context context) {

        if (iconBitmapDescriptors.size() == 0) {

            iconBitmapDescriptors.put(
                    "AMBULANCE_CURRENT",
                    new BitmapUtils.BitmapDescriptorFromVectorBuilder(context,
                            R.drawable.ambulance_red)
                            .setBackground(context, R.drawable.ic_oval_regular)
                            .setBackgroundScale(0.09f)
                            .setBackgroundColor(ContextCompat.getColor(context, R.color.bootstrapSecondary))
                            .setScale(0.1f)
                            .setOffset(9,18)
                            .build());

            iconBitmapDescriptors.put(
                    "AMBULANCE_" + Ambulance.STATUS_AVAILABLE,
                    new BitmapUtils.BitmapDescriptorFromVectorBuilder(context,
                            R.drawable.ambulance_green)
                            .setScale(0.1f)
                            .build());

            iconBitmapDescriptors.put(
                    "AMBULANCE_" + Ambulance.STATUS_OUT_OF_SERVICE,
                    new BitmapUtils.BitmapDescriptorFromVectorBuilder(context,
                            R.drawable.ambulance_gray)
                            .setScale(0.1f)
                            .build());

            iconBitmapDescriptors.put(
                    "AMBULANCE_" + Ambulance.STATUS_UNKNOWN,
                    new BitmapUtils.BitmapDescriptorFromVectorBuilder(context,
                            R.drawable.ambulance_gray)
                            .setScale(0.1f)
                            .build());

            iconBitmapDescriptors.put(
                    "AMBULANCE_" + Ambulance.STATUS_AT_BASE,
                    new BitmapUtils.BitmapDescriptorFromVectorBuilder(context,
                            R.drawable.ambulance_green)
                            .setScale(0.1f)
                            .build());

            iconBitmapDescriptors.put(
                    "AMBULANCE_" + Ambulance.STATUS_BASE_BOUND,
                    new BitmapUtils.BitmapDescriptorFromVectorBuilder(context,
                            R.drawable.ambulance_yellow)
                            .setScale(0.1f)
                            .build());

            iconBitmapDescriptors.put(
                    "AMBULANCE_" + Ambulance.STATUS_AT_HOSPITAL,
                    new BitmapUtils.BitmapDescriptorFromVectorBuilder(context,
                            R.drawable.ambulance_orange)
                            .setScale(0.1f)
                            .build());

            iconBitmapDescriptors.put(
                    "AMBULANCE_" + Ambulance.STATUS_HOSPITAL_BOUND,
                    new BitmapUtils.BitmapDescriptorFromVectorBuilder(context,
                            R.drawable.ambulance_orange)
                            .setScale(0.1f)
                            .build());

            iconBitmapDescriptors.put(
                    "AMBULANCE_" + Ambulance.STATUS_AT_PATIENT,
                    new BitmapUtils.BitmapDescriptorFromVectorBuilder(context,
                            R.drawable.ambulance_red)
                            .setScale(0.1f)
                            .build());

            iconBitmapDescriptors.put(
                    "AMBULANCE_" + Ambulance.STATUS_PATIENT_BOUND,
                    new BitmapUtils.BitmapDescriptorFromVectorBuilder(context,
                            R.drawable.ambulance_red)
                            .setScale(0.1f)
                            .build());

            iconBitmapDescriptors.put(
                    "AMBULANCE_" + Ambulance.STATUS_AT_WAYPOINT,
                    new BitmapUtils.BitmapDescriptorFromVectorBuilder(context,
                            R.drawable.ambulance_blue)
                            .setScale(0.1f)
                            .build());

            iconBitmapDescriptors.put(
                    "AMBULANCE_" + Ambulance.STATUS_WAYPOINT_BOUND,
                    new BitmapUtils.BitmapDescriptorFromVectorBuilder(context,
                            R.drawable.ambulance_blue)
                            .setScale(0.1f)
                            .build());

            iconBitmapDescriptors.put(
                    "HOSPITAL",
                    new BitmapUtils.BitmapDescriptorFromVectorBuilder(context,
                            R.drawable.ic_hospital_15)
                            .setColor(ContextCompat.getColor(context, R.color.bootstrapDark))
                            .build());

            iconBitmapDescriptors.put(
                    "WAYPOINT_" + org.emstrack.models.Location.TYPE_BASE,
                    new BitmapUtils.BitmapDescriptorFromVectorBuilder(context,
                            R.drawable.ic_home_15)
                            .setBackground(context, R.drawable.ic_marker_15)
                            .setBackgroundScale(1.5f)
                            .setBackgroundColor(ContextCompat.getColor(context, R.color.bootstrapSuccess))
                            .setColor(ContextCompat.getColor(context, R.color.bootstrapLight))
                            .setScale(0.85f)
                            .setOffset(15, 5)
                            .build());

            iconBitmapDescriptors.put(
                    "WAYPOINT_" + org.emstrack.models.Location.TYPE_AED,
                    new BitmapUtils.BitmapDescriptorFromVectorBuilder(context,
                            R.drawable.ic_heartbeat_solid)
                            .setBackground(context, R.drawable.ic_marker_15)
                            .setBackgroundScale(1.5f)
                            .setBackgroundColor(ContextCompat.getColor(context, R.color.bootstrapInfo))
                            .setColor(ContextCompat.getColor(context, R.color.bootstrapLight))
                            .setOffset(14, 10)
                            .setScale(.03f)
                            .build());

            iconBitmapDescriptors.put(
                    "WAYPOINT_" + org.emstrack.models.Location.TYPE_INCIDENT,
                    new BitmapUtils.BitmapDescriptorFromVectorBuilder(context,
                            R.drawable.ic_car_crash_solid)
                            .setBackground(context, R.drawable.ic_marker_15)
                            .setBackgroundScale(1.5f)
                            .setBackgroundColor(ContextCompat.getColor(context, R.color.bootstrapDanger))
                            .setColor(ContextCompat.getColor(context, R.color.bootstrapLight))
                            .setOffset(14, 10)
                            .setScale(.03f)
                            .build());

            iconBitmapDescriptors.put(
                    "WAYPOINT_" + org.emstrack.models.Location.TYPE_HOSPITAL,
                    new BitmapUtils.BitmapDescriptorFromVectorBuilder(context,
                            R.drawable.ic_hospital_15)
                            .setBackground(context, R.drawable.ic_marker_15)
                            .setBackgroundScale(1.5f)
                            .setBackgroundColor(ContextCompat.getColor(context, R.color.bootstrapWarning))
                            .setColor(ContextCompat.getColor(context, R.color.bootstrapLight))
                            .setOffset(15, 5)
                            .setScale(.85f)
                            .build());

            iconBitmapDescriptors.put(
                    "WAYPOINT_" + org.emstrack.models.Location.TYPE_WAYPOINT,
                    new BitmapUtils.BitmapDescriptorFromVectorBuilder(context,
                            R.drawable.ic_location_arrow)
                            .setBackground(context, R.drawable.ic_marker_15)
                            .setBackgroundScale(1.5f)
                            .setBackgroundColor(ContextCompat.getColor(context, R.color.bootstrapPrimary))
                            .setColor(ContextCompat.getColor(context, R.color.bootstrapLight))
                            .setOffset(14, 10)
                            .setScale(.03f)
                            .build());

            iconBitmapDescriptors.put(
                    "WAYPOINT_" + org.emstrack.models.Location.TYPE_OTHER,
                    new BitmapUtils.BitmapDescriptorFromVectorBuilder(context,
                            R.drawable.ic_location_arrow)
                            .setBackground(context, R.drawable.ic_marker_15)
                            .setBackgroundScale(1.5f)
                            .setBackgroundColor(ContextCompat.getColor(context, R.color.bootstrapPrimary))
                            .setColor(ContextCompat.getColor(context, R.color.bootstrapLight))
                            .setOffset(14, 10)
                            .setScale(.03f)
                            .build());

        }

    }

    public class AmbulancesUpdateBroadcastReceiver extends BroadcastReceiver {

        @Override
        public void onReceive(Context context, Intent intent ) {
            if (intent != null) {

                final String action = intent.getAction();
                assert action != null;

                switch (action) {
                    case AmbulanceForegroundService.BroadcastActions.AMBULANCE_UPDATE: {

                        Log.i(TAG, "AMBULANCE_UPDATE");

                        if (!centerCurrentAmbulance || fusedLocationClient == null) {
                            // update ambulance marker only if location service is not on
                            // TODO: update status if changed
                            Ambulance ambulance = AmbulanceForegroundService.getAppData().getAmbulance();
                            updateAmbulanceMarker(ambulance);
                        }

                        break;
                    }

                    case AmbulanceForegroundService.BroadcastActions.OTHER_AMBULANCES_UPDATE: {

                        Log.i(TAG, "OTHER_AMBULANCES_UPDATE");
                        int ambulanceId = intent.getIntExtra(AmbulanceForegroundService.BroadcastExtras.AMBULANCE_ID, -1);

                        if (ambulanceId == -1) {
                            // update all markers
                            LatLngBounds.Builder builder = new LatLngBounds.Builder();
                            updateAmbulanceMarkers(builder);

                            // update current ambulance as well
                            Ambulance ambulance = AmbulanceForegroundService.getAppData().getAmbulance();
                            updateAmbulanceMarker(ambulance);
                        } else {
                            SparseArray<Ambulance> ambulances = AmbulanceForegroundService.getAppData().getAmbulances();
                            updateAmbulanceMarker(ambulances.get(ambulanceId));
                        }

                        break;
                    }

                    case AmbulanceForegroundService.BroadcastActions.CALL_COMPLETED:

                        Log.i(TAG, "CALL_COMPLETED");

                    case AmbulanceForegroundService.BroadcastActions.CALL_UPDATE:

                        Log.i(TAG, "CALL_UPDATE");

                        // updateAmbulance markers without centering
                        LatLngBounds.Builder builder = new LatLngBounds.Builder();
                        updateWaypointMarkers(builder);

                        break;
                }

            }
        }
    }

    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {

        rootView = inflater.inflate(R.layout.fragment_map, container, false);
        activity = (MainActivity) requireActivity();

        buttonOnColor = getResources().getColor(R.color.mapButtonOn);
        buttonOffColor = getResources().getColor(R.color.mapButtonOff);
        buttonAlertColor = getResources().getColor(R.color.mapButtonAlert);

        // setup toolbar
        toolbarDragHelper = new DragHelper();
        toolbarDragHelper.setUp(getResources().getDimensionPixelOffset(R.dimen.mapToolbarHeightOffset));
        toolbarDragHelper.setAnimateTime(getResources().getInteger(android.R.integer.config_shortAnimTime));

        mapToolbarLayout = rootView.findViewById(R.id.mapToolbarLayout);
        mapToolbarLayout.setOnTouchListener(toolbarDragHelper);

        // Retrieve compass button
        compassButton = rootView.findViewById(R.id.compassButton);
        compassButton.setOnClickListener(v -> {
            // toggle center current ambulance
            centerCurrentAmbulance = !centerCurrentAmbulance;
            setButtonColor(compassButton, centerCurrentAmbulance);
            if (centerCurrentAmbulance) {
                startLocationUpdates();
            } else {
                stopLocationUpdates();
            }
        });

        // Retrieve show ambulance button
        showAmbulanceButton = rootView.findViewById(R.id.showAmbulanceButton);
        showAmbulanceButton.setOnClickListener(v ->
                centerMap(AmbulanceForegroundService.getAppData().getAmbulance())
        );

        // Retrieve ambulance button
        showAmbulancesButton = rootView.findViewById(R.id.showAmbulancesButton);
        showAmbulancesButton.setOnLongClickListener(v -> {
            // toggle show offline
            showAmbulances = showOfflineAmbulances = !showOfflineAmbulances;
            setButtonColor(showAmbulancesButton, showAmbulances, showOfflineAmbulances ? buttonAlertColor : buttonOnColor);
            updateMarkersAndCenter(showAmbulances);
            return true;
        });
        showAmbulancesButton.setOnClickListener(v -> {

            // toggle show ambulances
            showAmbulances = !showAmbulances;
            setButtonColor(showAmbulancesButton, showAmbulances, showOfflineAmbulances ? buttonAlertColor : buttonOnColor);

            // updateAmbulance markers without centering
            updateMarkersAndCenter(showAmbulances);
        });

        // Retrieve hospitals button
        showHospitalsButton = rootView.findViewById(R.id.showHospitalsButton);
        showHospitalsButton.setOnClickListener(v -> {

            // toggle show hospitals
            showHospitals = !showHospitals;
            setButtonColor(showHospitalsButton, showHospitals);

            // update markers without centering
            updateMarkersAndCenter(showHospitals);

        });

        // Retrieve waypoints button
        showWaypointsButton = rootView.findViewById(R.id.showWaypointsButton);
        showWaypointsButton.setOnClickListener(v -> {

            // toggle show waypoints
            showWaypoints = !showWaypoints;
            setButtonColor(showWaypointsButton, showWaypoints);

            // update markers
            updateMarkers();

            if (showWaypoints) {
                // calculate call boundaries
                LatLngBounds.Builder builder = new LatLngBounds.Builder();

                // add waypoints
                addToBounds(builder, waypointMarkers);

                // add current ambulance
                addToBounds(builder, getCurrentAmbulanceMarker());

                // center map
                LatLngBounds bounds = builder.build();
                centerMap(bounds);
            }

        });

        // Initialize markers maps
        ambulanceMarkers = new HashMap<>();
        hospitalMarkers = new HashMap<>();
        waypointMarkers = new HashMap<>();

        // Get settings, status and capabilities
        AmbulanceAppData appData = AmbulanceForegroundService.getAppData();
        Settings settings = appData.getSettings();
        if (settings != null) {

            ambulanceStatus = settings.getAmbulanceStatus();
            defaultLocation = settings.getDefaults().getLocation();

        } else {

            ambulanceStatus = new HashMap<>();
            defaultLocation = new GPSLocation(0,0);

        }

        // configure buttons
        configureButtons();

        // Initialize map
        SupportMapFragment mapFragment = (SupportMapFragment) getChildFragmentManager()
                .findFragmentById(R.id.mapFragment);
        Objects.requireNonNull(mapFragment).getMapAsync(this);

        // get arguments
        Bundle arguments = getArguments();
        if (arguments != null) {
            centerLatLng = (LatLng) getArguments().getParcelable("latLng");
        } else {
            centerLatLng = null;
        }

        Log.d(TAG, String.format("centerLatLng = %s", centerLatLng));

        // set done on resume to false
        doneOnResume = false;

        return rootView;

    }

    @Override
    public void onResume() {
        super.onResume();

        Log.d(TAG, "onResume");
        activity.setupNavigationBar();

        // configure buttons
        configureButtons();

        // Register receiver
        IntentFilter filter = new IntentFilter();
        filter.addAction(AmbulanceForegroundService.BroadcastActions.OTHER_AMBULANCES_UPDATE);
        filter.addAction(AmbulanceForegroundService.BroadcastActions.AMBULANCE_UPDATE);
        filter.addAction(AmbulanceForegroundService.BroadcastActions.CALL_UPDATE);
        receiver = new AmbulancesUpdateBroadcastReceiver();
        getLocalBroadcastManager().registerReceiver(receiver, filter);

        // Retrieving button state
        SharedPreferences sharedPreferences =
                activity.getSharedPreferences(AmbulanceForegroundService.PREFERENCES_NAME, AppCompatActivity.MODE_PRIVATE);

        // Retrieve state
        showToolbar = sharedPreferences.getBoolean(AmbulanceForegroundService.PREFERENCES_MAP_SHOW_TOOLBAR, false);
        showAmbulances = sharedPreferences.getBoolean(AmbulanceForegroundService.PREFERENCES_MAP_SHOW_AMBULANCES, false);
        showOfflineAmbulances = sharedPreferences.getBoolean(AmbulanceForegroundService.PREFERENCES_MAP_SHOW_OFFLINE_AMBULANCES, false);
        showHospitals = sharedPreferences.getBoolean(AmbulanceForegroundService.PREFERENCES_MAP_SHOW_HOSPITALS, false);
        showWaypoints = sharedPreferences.getBoolean(AmbulanceForegroundService.PREFERENCES_MAP_SHOW_WAYPOINTS, false);
        centerCurrentAmbulance = sharedPreferences.getBoolean(AmbulanceForegroundService.PREFERENCES_MAP_CENTER_AMBULANCES, false);

        zoomLevel = sharedPreferences.getFloat(AmbulanceForegroundService.PREFERENCES_MAP_ZOOM, defaultZoom);

        if (sharedPreferences.contains(AmbulanceForegroundService.PREFERENCES_MAP_LONGITUDE) &&
                sharedPreferences.contains(AmbulanceForegroundService.PREFERENCES_MAP_LATITUDE)) {
            target = new LatLng(sharedPreferences.getFloat(AmbulanceForegroundService.PREFERENCES_MAP_LATITUDE, 0),
                    sharedPreferences.getFloat(AmbulanceForegroundService.PREFERENCES_MAP_LONGITUDE, 0));
            bearing = sharedPreferences.getFloat(AmbulanceForegroundService.PREFERENCES_MAP_BEARING, 0);
        }

        // show toolbar?
        if ((showToolbar && toolbarDragHelper.isUp()) || (!showToolbar && toolbarDragHelper.isDown())) {
            toolbarDragHelper.toggleSnap(mapToolbarLayout, 0);
        }

        // set button colors
        setButtonColor(showAmbulancesButton, showAmbulances, showOfflineAmbulances ? buttonAlertColor : buttonOnColor);
        setButtonColor(showHospitalsButton, showHospitals);
        setButtonColor(showWaypointsButton, showWaypoints);
        setButtonColor(compassButton, centerCurrentAmbulance);

        // set done on resume to true
        doneOnResume = true;

        if (googleMap != null) {

            // already got map
            Log.d(TAG, "Will initialize map in onResume");

            initializeMap();

        } else {

            // already got map
            Log.d(TAG, "Did not initialized map in onResume");

            SupportMapFragment mapFragment = (SupportMapFragment) getChildFragmentManager()
                    .findFragmentById(R.id.mapFragment);
            Objects.requireNonNull(mapFragment).getMapAsync(this);

        }

    }

    @Override
    public void onPause() {
        super.onPause();

        Log.d(TAG, "onPause");
        // Unregister receiver
        if (receiver != null) {
            getLocalBroadcastManager().unregisterReceiver(receiver);
            receiver = null;
        }

        // show toolbar
        showToolbar = toolbarDragHelper.isDown();

        // save button state
        SharedPreferences sharedPreferences =
                activity.getSharedPreferences(AmbulanceForegroundService.PREFERENCES_NAME, AppCompatActivity.MODE_PRIVATE);

        // Get preferences editor
        SharedPreferences.Editor editor = sharedPreferences.edit();

        // Save credentials
        Log.d(TAG, "Storing state");
        editor.putBoolean(AmbulanceForegroundService.PREFERENCES_MAP_SHOW_TOOLBAR, showToolbar);
        editor.putBoolean(AmbulanceForegroundService.PREFERENCES_MAP_SHOW_AMBULANCES, showAmbulances);
        editor.putBoolean(AmbulanceForegroundService.PREFERENCES_MAP_SHOW_OFFLINE_AMBULANCES, showOfflineAmbulances);
        editor.putBoolean(AmbulanceForegroundService.PREFERENCES_MAP_SHOW_HOSPITALS, showHospitals);
        editor.putBoolean(AmbulanceForegroundService.PREFERENCES_MAP_SHOW_WAYPOINTS, showWaypoints);
        editor.putBoolean(AmbulanceForegroundService.PREFERENCES_MAP_CENTER_AMBULANCES, centerCurrentAmbulance);
        editor.putFloat(AmbulanceForegroundService.PREFERENCES_MAP_ZOOM, zoomLevel);
        if (target != null) {
            editor.putFloat(AmbulanceForegroundService.PREFERENCES_MAP_LONGITUDE, (float) target.longitude);
            editor.putFloat(AmbulanceForegroundService.PREFERENCES_MAP_LATITUDE, (float) target.latitude);
        }
        editor.putFloat(AmbulanceForegroundService.PREFERENCES_MAP_BEARING, (float) bearing);
        editor.apply();

        // set done on resume to false
        doneOnResume = false;
        googleMap = null;

        // stop location updates
        stopLocationUpdates();

    }

    @Override
    public void onCameraIdle() {
        if (googleMap != null) {
            zoomLevel = googleMap.getCameraPosition().zoom;
            target = googleMap.getCameraPosition().target;
            bearing = googleMap.getCameraPosition().bearing;
        }
    }

    @Override
    public void onMapReady(@NonNull GoogleMap googleMap) {

        Log.d(TAG, "onMapReady");

        // save map
        this.googleMap = googleMap;

        // initialize static markers
        initializeMarkers(requireContext());

        myLocationEnabled = false;
        if (useMyLocation && AmbulanceForegroundService.canUpdateLocation()) {

            // Use google map's my location
            try {

                Log.i(TAG, "Enable my location on google map.");
                googleMap.setMyLocationEnabled(true);
                myLocationEnabled = true;

            } catch (SecurityException e) {
                Log.i(TAG, "Could not enable my location on google map.");
            }

        }

        if (doneOnResume) {

            // already did onResume and did not initialize map
            Log.d(TAG, "Will initialize map in onMapReady");

            initializeMap();
        } else {

            Log.d(TAG, "Did not initialized map in onMapReady");

        }

    }

    private class AnimateBuffer {
        LatLng latLng;
        float bearing;
        int animateTimeInMs;

        AnimateBuffer(LatLng latLng, float bearing, int animateTimeInMs) {
            this.latLng = latLng; this.bearing = bearing; this.animateTimeInMs = animateTimeInMs;
        }
    }

    private synchronized void doAnimateMarkerAndCamera(Ambulance ambulance, LatLng latLng, float bearing, int animateTimeInMs) {

        // set flag
        isAnimatingMarkerAndCamera = true;

        // update marker
        updateAmbulanceMarker(ambulance, latLng, bearing, animateTimeInMs);

        // center map
        centerMap(latLng, bearing, false, animateTimeInMs, new GoogleMap.CancelableCallback() {

            @Override
            public void onCancel() {

                Log.d(TAG, "onCancel");
                isAnimatingMarkerAndCamera = false;

                // this will cause the pending buffer to be lost

            }

            @Override
            public void onFinish() {

                Log.d(TAG, "onFinish");
                if (animateBuffer != null) {

                    Log.d(TAG, "Consume buffer");

                    // animate buffer
                    doAnimateMarkerAndCamera(ambulance,
                            animateBuffer.latLng, animateBuffer.bearing,
                            animateBuffer.animateTimeInMs);

                    // release buffer
                    animateBuffer = null;

                } else {

                    Log.d(TAG, "Releasing flag");

                    // release animating flag
                    isAnimatingMarkerAndCamera = false;

                }

            }

        });

    }

    private synchronized void animateMarkerAndCamera(Ambulance ambulance, Location lastLocation, int animateTimeInMs) {

        LatLng latLng = new LatLng(lastLocation.getLatitude(), lastLocation.getLongitude());
        float bearing = lastLocation.getBearing();

        if (isAnimatingMarkerAndCamera) {

            // override buffer
            animateBuffer = new AnimateBuffer(latLng, bearing, animateTimeInMs);

        } else {

            // do animate
            doAnimateMarkerAndCamera(ambulance, latLng, bearing, animateTimeInMs);

        }

    }

    private void startLocationUpdates() {
        startLocationUpdates(true);
    }

    private void startLocationUpdates(boolean initialize) {

        if (fusedLocationClient != null) {
            Log.d(TAG, "Already started location updates. Skipping.");
            return;
        }

        // Initialize fused location client
        if (AmbulanceForegroundService.getAppData().getAmbulance() != null && centerCurrentAmbulance) {

            // disable button
            compassButton.setEnabled(false);

            // set update filter
            updateFilter = new VehicleUpdateFilter();

            try {

                fusedLocationClient = LocationServices.getFusedLocationProviderClient(requireContext());

                // Create request for location updates
                LocationRequest locationRequest = LocationRequest.create();
                locationRequest.setPriority(LocationRequest.PRIORITY_BALANCED_POWER_ACCURACY)
                        .setInterval(1000) // 30,000 ms = 1s = 1s
                        .setFastestInterval(500) // 1,000 ms = .5s
                        .setMaxWaitTime(1000); // 1,000 ms = 1s = 1s

                locationCallback = new LocationCallback() {

                    @Override
                    public void onLocationResult(@NonNull LocationResult locationResult) {
                        Log.d(TAG, String.format("Got location results: %s", locationResult));

                        Ambulance ambulance = AmbulanceForegroundService.getAppData().getAmbulance();

                        if (ambulance != null && locationResult.getLocations().size() > 0) {

                            updateFilter.update(locationResult.getLocations());

                            if (updateFilter.hasUpdates()) {

                                // Sort updates
                                updateFilter.sort();

                                // update server or buffer
                                List<VehicleUpdate> updates = updateFilter.getFilteredUpdates();

                                // get last location
                                VehicleUpdate lastUpdate = updates.get(updates.size() - 1);
                                Location lastLocation = lastUpdate.getLocation();

                                // reset filter
                                updateFilter.reset();

                                // calculate update distance
                                Marker currentMarker = ambulanceMarkers.get(ambulance.getId());
                                double distance = calculateDistanceHaversine(currentMarker.getPosition(), lastLocation);
                                double time = distance/lastUpdate.getVelocity();
                                int animateTimeInMs = Math.min((int) (1000 * time), 3000);

                                // animate or buffer
                                animateMarkerAndCamera(ambulance, lastLocation, animateTimeInMs);

                            }
                        }
                    }
                };

                fusedLocationClient.requestLocationUpdates(locationRequest, locationCallback, null)
                        .addOnSuccessListener(
                                aVoid -> {
                                    Log.i(TAG, "Starting location updates");

                                    // set animatingMarkerAndCamera = false
                                    isAnimatingMarkerAndCamera = false;

                                    if (initialize) {
                                        // reset zoom
                                        zoomLevel = defaultZoom;
                                        Ambulance ambulance = AmbulanceForegroundService.getAppData().getAmbulance();
                                        if (ambulance != null) {
                                            // center ambulance
                                            centerMap(ambulance, false, 3000);
                                        }
                                    }

                                    // enable button
                                    compassButton.setEnabled(true);

                                })
                        .addOnFailureListener(
                                e -> {

                                    Log.i(TAG, "Failed to start location updates");

                                    // set animatingMarkerAndCamera = false
                                    isAnimatingMarkerAndCamera = false;

                                    fusedLocationClient = null;

                                    // enable button
                                    compassButton.setEnabled(true);

                                });

            } catch (SecurityException e) {
                Log.i(TAG, "Failed to start location updates");
                fusedLocationClient = null;
            }
        } else {
            fusedLocationClient = null;
        }
    }

    private void stopLocationUpdates() {

        if (fusedLocationClient == null) {
            Log.d(TAG, "Not updating locations. Skipping.");
            return;
        }

        // disable button
        compassButton.setEnabled(false);

        // remove on fused location client
        fusedLocationClient.removeLocationUpdates(locationCallback)
                .addOnSuccessListener(
                        aVoid -> {

                            Log.i(TAG, "Location updates stopped");

                            fusedLocationClient = null;

                            // enable button
                            compassButton.setEnabled(true);

                        })
                .addOnFailureListener(
                        e -> {

                            Log.i(TAG, "Location updates could not be stopped");

                            // enable button
                            compassButton.setEnabled(true);

                        });

    }

    private void initializeMap() {

        Log.d(TAG, "initializeMap");

        // Update markers and center map
        updateMarkers();

        if (centerLatLng != null) {
            startLocationUpdates(false);
            centerMap(centerLatLng, bearing, true);
        } else if (target != null) {
            startLocationUpdates(false);
            centerMap(target, bearing, false);
        } else {
            startLocationUpdates();
        }

        // enable zoom buttons
        googleMap.getUiSettings().setZoomControlsEnabled(true);

        // Add listener to track zoom
        googleMap.setOnCameraIdleListener(this);

    }

    private void configureButtons() {
        AmbulanceAppData appData = AmbulanceForegroundService.getAppData();
        Ambulance ambulance = appData.getAmbulance();
        if (ambulance != null) {
            compassButton.setVisibility(View.VISIBLE);
            showAmbulanceButton.setVisibility(View.VISIBLE);
        } else {
            compassButton.setVisibility(View.GONE);
            showAmbulanceButton.setVisibility(View.GONE);
        }
        Call call = appData.getCalls().getCurrentCall();
        if (call != null) {
            showWaypointsButton.setVisibility(View.VISIBLE);
        } else {
            showWaypointsButton.setVisibility(View.GONE);
        }
    }

    private void setButtonColor(ImageView view, boolean condition) {
        setButtonColor(view, condition, buttonOnColor, buttonOffColor);
    }

    private void setButtonColor(ImageView view, boolean condition, int onColor) {
        setButtonColor(view, condition, onColor, buttonOffColor);
    }

    private void setButtonColor(ImageView view, boolean condition, int onColor, int offColor) {
        if (condition) {
            // view.setBackgroundColor(color);
            view.setColorFilter(onColor, PorterDuff.Mode.SRC_IN);
        } else {
            // view.setBackgroundColor(Color.TRANSPARENT);
            view.setColorFilter(offColor, PorterDuff.Mode.SRC_IN);
        }
    }

    private void clearMarkers(Map<Integer,Marker> map) {
        Iterator<Map.Entry<Integer,Marker>> iterator = map.entrySet().iterator();
        while (iterator.hasNext())
        {
            // retrieveObject entry
            Map.Entry<Integer,Marker> entry = iterator.next();

            // remove from map
            entry.getValue().remove();

            // remove from collection
            iterator.remove();

        }
    }

    public void centerMap(LatLng latLng, float bearing) {
        centerMap(latLng, bearing, false, 0, null);
    }

    public void centerMap(LatLng latLng, float bearing, boolean dropMarker) {
        centerMap(latLng, bearing, dropMarker, 0, null);
    }

    public void centerMap(LatLng latLng, float bearing, boolean dropMarker, int animateTimeInMs) {
        centerMap(latLng, bearing, dropMarker, animateTimeInMs, null);
    }

    public void centerMap(LatLng latLng, float bearing, boolean dropMarker, int animateTimeInMs, GoogleMap.CancelableCallback animateCallback) {

        if (googleMap == null) {
            Log.d(TAG, "centerMap: google maps is null. Aborting...");
            return;
        }

        Log.d(TAG, String.format("centerMap latlng = %s, bearing = %f, dropMarker = %b, animateTimeInMs = %d",
                latLng, bearing, dropMarker, animateTimeInMs));
        if (dropMarker) {
            googleMap.addMarker(new MarkerOptions().position(latLng));
        }
        if (animateTimeInMs > 0) {
            CameraPosition cameraPosition = new CameraPosition.Builder()
                    .target(latLng)
                    .bearing(bearing)
                    .zoom(zoomLevel)
                    .build();
            googleMap.animateCamera(CameraUpdateFactory.newCameraPosition(cameraPosition), animateTimeInMs, animateCallback);
        } else {
            googleMap.moveCamera(
                    CameraUpdateFactory.newCameraPosition(
                            new CameraPosition(latLng, zoomLevel, 0, bearing)));
        }

    }

    public void centerMap(Ambulance ambulance) {
        centerMap(ambulance, false, 0, null);
    }

    public void centerMap(Ambulance ambulance, boolean dropPin) {
        centerMap(ambulance, dropPin, 0, null);
    }

    public void centerMap(Ambulance ambulance, boolean dropPin, int animateTimeInMs) {
        centerMap(ambulance, dropPin, animateTimeInMs, null);
    }

    public void centerMap(Ambulance ambulance, boolean dropPin, int animateTimeInMs, GoogleMap.CancelableCallback animateCallback) {

        if (ambulance != null) {

            GPSLocation location = ambulance.getLocation();
            LatLng latLng = new LatLng(location.getLatitude(), location.getLongitude());
            centerMap(latLng, (float) ambulance.getOrientation(), dropPin, animateTimeInMs, animateCallback);

        } else {
            Log.d(TAG, "No ambulance is selected");
        }

    }

    public void centerMap(LatLngBounds bounds) {

        Log.d(TAG, "centerMap bounds");
        centerAtDefault = false;

        // Has waypoints?
        if (waypointMarkers.size() > 0 && bounds != null) {


                Log.d(TAG, "center at bounds");

                // move camera
                googleMap.animateCamera(CameraUpdateFactory.newLatLngBounds(bounds, defaultPadding));

                return;

        }

        // Has ambulances?
        if (ambulanceMarkers.size() > 0) {

            // Move camera
            if (ambulanceMarkers.size() == 1) {

                Ambulance ambulance = AmbulanceForegroundService.getAppData().getAmbulance();
                if (ambulance != null) {

                    Log.d(TAG, "center at own ambulance");
                    centerMap(ambulance);

                    return;
                }

            } else if (bounds != null) {

                Log.d(TAG, "center at all ambulances");

                // move camera
                googleMap.animateCamera(CameraUpdateFactory.newLatLngBounds(bounds, defaultPadding));

                return;

            }

        }

        Log.d(TAG, "center at default location");
        centerAtDefault = true;

        // Otherwise center at default location
        LatLng latLng = new LatLng(defaultLocation.getLatitude(), defaultLocation.getLongitude());
        centerMap(latLng, 0);

    }

    private void updateMarkersAndCenter(boolean shouldCenter) {
        LatLngBounds bounds = updateMarkers();
        if (shouldCenter) {
            centerMap(bounds);
        }
    }

    private void updateHospitalMarkers(LatLngBounds.Builder builder) {

        // Clear markers?
        if (!showHospitals || hospitalMarkers.size() > 0) {
            clearMarkers(hospitalMarkers);
        }

        // Update hospitals
        if (showHospitals) {

            // Loop over all hospitals
            for (Hospital hospital : SparseArrayUtils.iterable(AmbulanceForegroundService.getAppData().getHospitals())) {

                // Add marker for hospital
                Marker marker = addMarkerForHospital(hospital);

                // Add to bound builder
                builder.include(marker.getPosition());

            }

        }

    }

    private Marker getCurrentAmbulanceMarker() {

        Ambulance currentAmbulance = AmbulanceForegroundService.getAppData().getAmbulance();
        if (currentAmbulance != null) {
            int ambulanceId = currentAmbulance.getId();
            for (Map.Entry<Integer, Marker> entry : ambulanceMarkers.entrySet()) {
                if (entry.getKey() == ambulanceId) {
                    return entry.getValue();
                }
            }
        }

        return null;
    }

    private void removeMarker(Map<Integer, Marker> map, int key) {

        if ( map.containsKey(key) ) {
            // remove from google map
            map.get(key).remove();

            // remove from collection
            map.remove(key);
        }

    }

    private void addToBounds(LatLngBounds.Builder builder, Map<Integer, Marker> map) {

        // loop through all markers
        for (Marker marker : map.values()) {
            builder.include(marker.getPosition());
        }

    }

    private void addToBounds(LatLngBounds.Builder builder, Marker marker) {

        if (marker != null) {
            builder.include(marker.getPosition());
        }

    }

    private void updateAmbulanceMarker(Ambulance ambulance) {
        updateAmbulanceMarker(ambulance, null, 0, 0);
    }

    private void updateAmbulanceMarker(Ambulance ambulance, LatLng latLng, float orientation, int animateTimeInMs) {

        if (ambulance == null) {
            Log.d(TAG, "updateAmbulanceMarker called with null ambulance");
            return;
        }

        // add new marked
        addMarkerForAmbulance(ambulance, latLng, orientation, animateTimeInMs);

    }

    private void updateAmbulanceMarkers(LatLngBounds.Builder builder) {

        // Clear markers?
        if (!showAmbulances || ambulanceMarkers.size() > 0) {
            clearMarkers(ambulanceMarkers);
        }

        // Update ambulances
        SparseArray<Ambulance> ambulances = AmbulanceForegroundService.getAppData().getAmbulances();
        if (showAmbulances && ambulances != null) {

            // Loop over all ambulances
            for (Ambulance ambulance : SparseArrayUtils.iterable(ambulances)) {
                if (showOfflineAmbulances || ambulance.getClientId() != null) {
                    // Add marker for ambulance
                    Marker marker = addMarkerForAmbulance(ambulance);

                    // Add to bound builder
                    builder.include(marker.getPosition());
                }
            }
        }

    }

    private void updateWaypointMarkers(LatLngBounds.Builder builder) {

        // Clear markers?
        if (!showWaypoints || waypointMarkers.size() > 0) {
            clearMarkers(waypointMarkers);
        }

        Call call = AmbulanceForegroundService.getAppData().getCalls().getCurrentCall();
        if (showWaypoints && call != null) {

            AmbulanceCall ambulanceCall = call.getCurrentAmbulanceCall();

            // Loop over all waypoints
            for (Waypoint waypoint: ambulanceCall.getWaypointSet()) {

                // Add marker for waypoint
                Marker marker = addMarkerForWaypoint(waypoint);

                // Add to bound builder
                builder.include(marker.getPosition());

            }

        }
    }

    public LatLngBounds updateMarkers() {

        // fast return
        if (googleMap == null)
            return null;

        // Assemble marker bounds
        LatLngBounds.Builder builder = new LatLngBounds.Builder();

        // Get app data
        AmbulanceAppData appData = AmbulanceForegroundService.getAppData();

        // Update hospitals
        updateHospitalMarkers(builder);

        // Update ambulances
        updateAmbulanceMarkers(builder);

        // Update waypoints
        updateWaypointMarkers(builder);

        // Handle my location?
        if (!useMyLocation) {

            Ambulance ambulance = AmbulanceForegroundService.getAppData().getAmbulance();
            if (ambulance != null) {

                // Add marker for ambulance
                Marker marker = addMarkerForAmbulance(ambulance);

                // Add to bound builder
                builder.include(marker.getPosition());

            }

        }

        // Calculate bounds and return
        try {
            return builder.build();
        } catch (IllegalStateException e) {
            return null;
        }

    }

    public Marker addMarkerForAmbulance(Ambulance ambulance) {
        return addMarkerForAmbulance(ambulance, null, 0, 0);
    }

    public Marker addMarkerForAmbulance(Ambulance ambulance,
                                        LatLng latLng, float orientation, int animateTimeInMs) {

        Log.d(TAG,"Adding marker for ambulance " + ambulance.getIdentifier());

        if (latLng == null) {
            // use ambulance's current location
            GPSLocation location = ambulance.getLocation();
            latLng = new LatLng(location.getLatitude(), location.getLongitude());
            orientation = (float) ambulance.getOrientation();
        }

        // Marker exist?
        Marker marker;
        if (ambulanceMarkers.containsKey(ambulance.getId())) {

            // get marker
            marker = ambulanceMarkers.get(ambulance.getId());

            if (animateTimeInMs > 0) {
                marker.setSnippet(ambulanceStatus.get(ambulance.getStatus()));
                MarkerAnimation.animateMarkerToICS(marker, latLng, orientation,
                        animateTimeInMs, new LatLngInterpolator.Linear());
            } else {
                // Just update marker
                marker.setPosition(latLng);
                marker.setRotation(orientation);
                marker.setSnippet(ambulanceStatus.get(ambulance.getStatus()));
            }

        } else {

            Ambulance currentAmbulance = AmbulanceForegroundService.getAppData().getAmbulance();
            BitmapDescriptor ambulanceIcon;
            if (currentAmbulance == null || currentAmbulance.getId() != ambulance.getId()) {
                ambulanceIcon = iconBitmapDescriptors.get("AMBULANCE_" + ambulance.getStatus());
            } else {
                ambulanceIcon = iconBitmapDescriptors.get("AMBULANCE_CURRENT");
            }

            // Create marker, ignores animation
            marker = googleMap.addMarker(new MarkerOptions()
                    .position(latLng)
                    .icon(ambulanceIcon)
                    .anchor(0.5F,0.5F)
                    .rotation(orientation)
                    .flat(true)
                    .title(ambulance.getIdentifier())
                    .snippet(ambulanceStatus.get(ambulance.getStatus())));

            // Save marker
            ambulanceMarkers.put(ambulance.getId(), marker);

        }

        return marker;
    }

    public Marker addMarkerForHospital(Hospital hospital) {

        Log.d(TAG,"Adding marker for hospital " + hospital.getName());

        // Find new location
        GPSLocation location = hospital.getLocation();
        LatLng latLng = new LatLng(location.getLatitude(), location.getLongitude());

        // Marker exist?
        Marker marker;
        if (hospitalMarkers.containsKey(hospital.getId())) {

            // Update marker
            marker = hospitalMarkers.get(hospital.getId());
            marker.setPosition(latLng);;

        } else {

            // Create marker
            marker = googleMap.addMarker(new MarkerOptions()
                    .position(latLng)
                    .icon(iconBitmapDescriptors.get("HOSPITAL"))
                    .anchor(0.5F,0.5F)
                    .flat(true)
                    .title(hospital.getName()));

            // Save marker
            hospitalMarkers.put(hospital.getId(), marker);

        }

        return marker;
    }

    public Marker addMarkerForWaypoint(Waypoint waypoint) {

        Log.d(TAG,"Adding marker for waypoint " + waypoint.getId());

        // Find new location
        GPSLocation location = waypoint.getLocation().getLocation();
        LatLng latLng = new LatLng(location.getLatitude(), location.getLongitude());

        // Marker exist?
        Marker marker;
        if (waypointMarkers.containsKey(waypoint.getId())) {

            // Update marker
            marker = waypointMarkers.get(waypoint.getId());
            marker.setPosition(latLng);

        } else {

            // Create marker
            marker = googleMap.addMarker(new MarkerOptions()
                    .position(latLng)
                    .icon(iconBitmapDescriptors.get("WAYPOINT_" + waypoint.getLocation().getType()))
                    .anchor(0.5F,1.0F)
                    .flat(false));
                    // .title(waypoint.getName()));

            // Save marker
            waypointMarkers.put(waypoint.getId(), marker);

        }

        return marker;
    }

    /**
     * Get LocalBroadcastManager
     *
     * @return the LocalBroadcastManager
     */
    private LocalBroadcastManager getLocalBroadcastManager() {
        return LocalBroadcastManager.getInstance(requireContext());
    }

}
