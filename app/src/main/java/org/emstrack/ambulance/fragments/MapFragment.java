package org.emstrack.ambulance.fragments;

import static org.emstrack.ambulance.util.AnimationHelper.crossfade;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.os.Bundle;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.fragment.app.Fragment;
import androidx.localbroadcastmanager.content.LocalBroadcastManager;

import android.util.Log;
import android.util.SparseArray;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;

import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.CameraPosition;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.LatLngBounds;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.android.material.floatingactionbutton.FloatingActionButton;

import org.emstrack.ambulance.MainActivity;
import org.emstrack.ambulance.models.AmbulanceAppData;
import org.emstrack.ambulance.services.AmbulanceForegroundService;
import org.emstrack.ambulance.R;
import org.emstrack.ambulance.util.BitmapUtils;
import org.emstrack.ambulance.util.SparseArrayUtils;
import org.emstrack.models.Call;
import org.emstrack.models.Settings;
import org.emstrack.models.Ambulance;
import org.emstrack.models.AmbulanceCall;
import org.emstrack.models.GPSLocation;
import org.emstrack.models.Hospital;
import org.emstrack.models.Waypoint;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

// TODO: Implement listener to ambulance changes

public class MapFragment extends Fragment implements OnMapReadyCallback, GoogleMap.OnCameraIdleListener {

    private static final String TAG = MapFragment.class.getSimpleName();

    private final float defaultZoom = 15;
    private final int defaultPadding = 50;

    View rootView;
    private Map<String, String> ambulanceStatus;
    private Map<Integer, Marker> ambulanceMarkers;
    private Map<Integer, Marker> hospitalMarkers;
    private Map<Integer, Marker> waypointMarkers;

    private boolean showToolbar = false;

    private ImageView showLocationButton;
    private boolean centerAmbulances = false;

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

    private GoogleMap googleMap;
    private AmbulancesUpdateBroadcastReceiver receiver;

    private GPSLocation defaultLocation;

    private boolean centerAtDefault;
    private MainActivity activity;
    private LatLng centerLatLng;
    private int buttonOnColor;
    private int buttonAlertColor;

    private FloatingActionButton mapFAB;
    private View mapToolbarLayout;

    public class AmbulancesUpdateBroadcastReceiver extends BroadcastReceiver {

        @Override
        public void onReceive(Context context, Intent intent ) {
            if (intent != null) {

                final String action = intent.getAction();
                assert action != null;

                if (action.equals(AmbulanceForegroundService.BroadcastActions.AMBULANCE_UPDATE)) {

                    if (centerAmbulances) {

                        Log.i(TAG, "AMBULANCE_UPDATE");

                        // center ambulances
                        centerAmbulances();

                    }

                } else if (action.equals(AmbulanceForegroundService.BroadcastActions.OTHER_AMBULANCES_UPDATE)) {

                    Log.i(TAG, "OTHER_AMBULANCES_UPDATE");

                    // updateAmbulance markers without centering
                    updateMarkers();

                } else if (action.equals(AmbulanceForegroundService.BroadcastActions.CALL_UPDATE)) {

                    Log.i(TAG, "CALL_UPDATE");

                    // updateAmbulance markers without centering
                    updateMarkers();

                } else if (action.equals(AmbulanceForegroundService.BroadcastActions.CALL_COMPLETED)) {

                    Log.i(TAG, "CALL_COMPLETED");

                    // updateAmbulance markers without centering
                    updateMarkers();

                }

            }
        }
    }

    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {

        rootView = inflater.inflate(R.layout.fragment_map, container, false);
        activity = (MainActivity) requireActivity();

        buttonOnColor = getResources().getColor(R.color.mapButtonOn);
        buttonAlertColor = getResources().getColor(R.color.mapButtonAlert);

        mapToolbarLayout = rootView.findViewById(R.id.mapToolbarLayout);
        ImageView mapToolbarClose = mapToolbarLayout.findViewById(R.id.mapToolbarClose);
        mapFAB = rootView.findViewById(R.id.mapFAB);

        int animationTime = getResources().getInteger(android.R.integer.config_shortAnimTime);
        mapToolbarClose.setOnClickListener(v -> {
            showToolbar = false;
            crossfade(mapFAB, mapToolbarLayout, animationTime);
        });

        mapFAB.setOnClickListener(v -> {
            showToolbar = true;
            crossfade(mapToolbarLayout, mapFAB, animationTime);
        });


        // Retrieve location button
        showLocationButton = rootView.findViewById(R.id.showLocationButton);
        showLocationButton.setOnLongClickListener(v -> {
            // toggle show ambulances
            centerAmbulances = !centerAmbulances;
            setButtonColor(showLocationButton, centerAmbulances);
            return true;
        });
        showLocationButton.setOnClickListener(v -> centerAmbulances());

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

            // update markers and center
            updateMarkersAndCenter(showWaypoints);

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

        // Initialize map
        SupportMapFragment mapFragment = (SupportMapFragment) getChildFragmentManager()
                .findFragmentById(R.id.mapFragment);
        mapFragment.getMapAsync(this);

        // get arguments
        Bundle arguments = getArguments();
        if (arguments != null) {
            centerLatLng = (LatLng) getArguments().getParcelable("latLng");
        }

        return rootView;

    }

    @Override
    public void setUserVisibleHint(boolean isVisibleToUser) {

        Log.d(TAG, "setUserVisibleHint");

        super.setUserVisibleHint(isVisibleToUser);
        if (isVisibleToUser) {

            if (centerAtDefault) {

                centerMap(updateMarkers());

            }

        }
    }

    @Override
    public void onResume() {
        super.onResume();

        Log.d(TAG, "onResume");
        activity.setupNavigationBar();

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

        // Retrieve button state
        showToolbar = sharedPreferences.getBoolean(AmbulanceForegroundService.PREFERENCES_MAP_SHOW_TOOLBAR, false);
        showAmbulances = sharedPreferences.getBoolean(AmbulanceForegroundService.PREFERENCES_MAP_SHOW_AMBULANCES, false);
        showOfflineAmbulances = sharedPreferences.getBoolean(AmbulanceForegroundService.PREFERENCES_MAP_SHOW_OFFLINE_AMBULANCES, false);
        showHospitals = sharedPreferences.getBoolean(AmbulanceForegroundService.PREFERENCES_MAP_SHOW_HOSPITALS, false);
        showWaypoints = sharedPreferences.getBoolean(AmbulanceForegroundService.PREFERENCES_MAP_SHOW_WAYPOINTS, false);
        centerAmbulances = sharedPreferences.getBoolean(AmbulanceForegroundService.PREFERENCES_MAP_CENTER_AMBULANCES, false);

        // show toolbar?
        if (showToolbar) {
            mapToolbarLayout.setVisibility(View.VISIBLE);
            mapFAB.setVisibility(View.GONE);
        } else {
            mapToolbarLayout.setVisibility(View.GONE);
            mapFAB.setVisibility(View.VISIBLE);
        }

        // set button colors
        setButtonColor(showAmbulancesButton, showAmbulances, showOfflineAmbulances ? buttonAlertColor : buttonOnColor);
        setButtonColor(showHospitalsButton, showHospitals);
        setButtonColor(showWaypointsButton, showWaypoints);
        setButtonColor(showLocationButton, centerAmbulances);

    }

    @Override
    public void onPause() {
        super.onPause();

        // Unregister receiver
        if (receiver != null) {
            getLocalBroadcastManager().unregisterReceiver(receiver);
            receiver = null;
        }

        // save button state
        SharedPreferences sharedPreferences =
                activity.getSharedPreferences(AmbulanceForegroundService.PREFERENCES_NAME, AppCompatActivity.MODE_PRIVATE);

        // Get preferences editor
        SharedPreferences.Editor editor = sharedPreferences.edit();

        // Save credentials
        Log.d(TAG, "Storing buttons");
        editor.putBoolean(AmbulanceForegroundService.PREFERENCES_MAP_SHOW_TOOLBAR, showToolbar);
        editor.putBoolean(AmbulanceForegroundService.PREFERENCES_MAP_SHOW_AMBULANCES, showAmbulances);
        editor.putBoolean(AmbulanceForegroundService.PREFERENCES_MAP_SHOW_OFFLINE_AMBULANCES, showOfflineAmbulances);
        editor.putBoolean(AmbulanceForegroundService.PREFERENCES_MAP_SHOW_HOSPITALS, showHospitals);
        editor.putBoolean(AmbulanceForegroundService.PREFERENCES_MAP_SHOW_WAYPOINTS, showWaypoints);
        editor.putBoolean(AmbulanceForegroundService.PREFERENCES_MAP_CENTER_AMBULANCES, centerAmbulances);
        editor.apply();

    }

    @Override
    public void onCameraIdle() {
        zoomLevel = googleMap.getCameraPosition().zoom;
    }

    @Override
    public void onMapReady(@NonNull GoogleMap googleMap) {

        Log.d(TAG, "onMapReady");

        // save map
        this.googleMap = googleMap;

        myLocationEnabled = false;
        if (AmbulanceForegroundService.canUpdateLocation()) {

            if (useMyLocation) {

                // Use google map's my location

                try {

                    Log.i(TAG, "Enable my location on google map.");
                    googleMap.setMyLocationEnabled(true);
                    myLocationEnabled = true;

                } catch (SecurityException e) {
                    Log.i(TAG, "Could not enable my location on google map.");
                }

            }

        }

        // Add listener to track zoom
        googleMap.setOnCameraIdleListener(this);

        // Update markers and center map
        LatLngBounds bounds = updateMarkers();
        if (centerLatLng == null) {
            centerMap(bounds);
        } else {
            centerMap(centerLatLng, true);
        }

    }

    private void setButtonColor(View view, boolean condition) {
        setButtonColor(view, condition, buttonOnColor);
    }

    private void setButtonColor(View view, boolean condition, int color) {
        if (condition) {
            view.setBackgroundColor(color);
        } else {
            view.setBackgroundColor(Color.TRANSPARENT);
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

    public void centerMap(LatLng latLng) {
        centerMap(latLng, false);
    }

    public void centerMap(LatLng latLng, boolean dropMarker) {

        if (dropMarker) {
            googleMap.addMarker(new MarkerOptions().position(latLng));
        }
        googleMap.moveCamera(CameraUpdateFactory.newLatLngZoom(latLng, zoomLevel));

    }

    public void centerMap(LatLngBounds bounds) {

        Log.d(TAG, "centerMap");
        centerAtDefault = false;

        // Has waypoints?
        if (waypointMarkers.size() > 0 && bounds != null) {


                Log.d(TAG, "center at all ambulances and waypoints");

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

                    GPSLocation location = ambulance.getLocation();
                    LatLng latLng = new LatLng(location.getLatitude(), location.getLongitude());

                    Log.d(TAG, "center at own ambulance");
                    // googleMap.animateCamera(CameraUpdateFactory.newLatLngZoom(latLng, zoomLevel));
                    centerMap(latLng);

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

        // googleMap.moveCamera(CameraUpdateFactory.newLatLngZoom(latLng, zoomLevel));
        centerMap(latLng);

    }

    private void updateMarkersAndCenter(boolean condition) {
        LatLngBounds bounds = updateMarkers();
        if (condition) {
            centerMap(bounds);
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
        if (showHospitals) {

            // Loop over all hospitals
            for (Hospital hospital : SparseArrayUtils.iterable(appData.getHospitals())) {

                // Add marker for hospital
                Marker marker = addMarkerForHospital(hospital);

                // Add to bound builder
                builder.include(marker.getPosition());

            }

        } else {
            clearMarkers(hospitalMarkers);
        }
        
        // Update ambulances
        if (showAmbulances) {

            // Get ambulances
            SparseArray<Ambulance> ambulances = AmbulanceForegroundService.getAppData().getAmbulances();
            if (ambulances != null) {

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

        } else {
            clearMarkers(ambulanceMarkers);
        }

        // Update waypoints
        if (showWaypoints) {

            // Get current call
            Call call = appData.getCalls().getCurrentCall();
            if (call != null) {

                AmbulanceCall ambulanceCall = call.getCurrentAmbulanceCall();

                // Loop over all waypoints
                for (Waypoint waypoint: ambulanceCall.getWaypointSet()) {

                        // Add marker for waypoint
                        Marker marker = addMarkerForWaypoint(waypoint);

                        // Add to bound builder
                        builder.include(marker.getPosition());

                }
                
            }

        } else {
            clearMarkers(waypointMarkers);
        }

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

        Log.d(TAG,"Adding marker for ambulance " + ambulance.getIdentifier());

        // Find new location
        GPSLocation location = ambulance.getLocation();
        LatLng latLng = new LatLng(location.getLatitude(), location.getLongitude());

        // Marker exist?
        Marker marker;
        if (ambulanceMarkers.containsKey(ambulance.getId())) {

            // Update marker
            marker = ambulanceMarkers.get(ambulance.getId());
            marker.setPosition(latLng);
            marker.setRotation((float) ambulance.getOrientation());
            marker.setSnippet(ambulanceStatus.get(ambulance.getStatus()));

        } else {

            // Create marker
            marker = googleMap.addMarker(new MarkerOptions()
                    .position(latLng)
                    .icon(BitmapUtils.bitmapDescriptorFromVector(getActivity(), R.drawable.ambulance_blue, 0.1))
                    .anchor(0.5F,0.5F)
                    .rotation((float) ambulance.getOrientation())
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
                    .icon(BitmapUtils.bitmapDescriptorFromVector(getActivity(), R.drawable.ic_hospital_15, 1))
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
                    .icon(BitmapUtils.bitmapDescriptorFromVector(getActivity(), R.drawable.ic_marker_15, 1))
                    .anchor(0.5F,1.0F)
                    .flat(false));
                    // .title(waypoint.getName()));

            // Save marker
            waypointMarkers.put(waypoint.getId(), marker);

        }

        return marker;
    }

    public void centerAmbulances() {

        if (googleMap == null) {
            Log.d(TAG, "centerAmbulances: google maps is null. Aborting...");
            return;
        }

        Ambulance ambulance = AmbulanceForegroundService.getAppData().getAmbulance();
        if (ambulance != null) {

            GPSLocation location = ambulance.getLocation();
            LatLng latLng = new LatLng(location.getLatitude(), location.getLongitude());

            // Add marker for ambulance
            addMarkerForAmbulance(ambulance);

            // Center and orient map
            CameraPosition currentPlace = new CameraPosition.Builder()
                    .target(latLng)
                    .bearing((float) ambulance.getOrientation())
                    .zoom(zoomLevel)
                    .build();
            googleMap.animateCamera(CameraUpdateFactory.newCameraPosition(currentPlace));

        } else {
            Log.d(TAG, "No ambulance is selected");
        }

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
