package org.emstrack.ambulance.fragments;

import android.app.Activity;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.hardware.SensorManager;
import android.os.Bundle;
import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import androidx.localbroadcastmanager.content.LocalBroadcastManager;
import android.util.Log;
import android.util.SparseArray;
import android.view.LayoutInflater;
import android.view.OrientationEventListener;
import android.view.Surface;
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

import org.emstrack.ambulance.dialogs.AlertSnackbar;
import org.emstrack.ambulance.models.AmbulanceAppData;
import org.emstrack.ambulance.services.AmbulanceForegroundService;
import org.emstrack.ambulance.R;
import org.emstrack.ambulance.util.BitmapUtils;
import org.emstrack.ambulance.util.SparseArrayUtils;
import org.emstrack.models.Call;
import org.emstrack.models.Settings;
import org.emstrack.models.util.BroadcastActions;
import org.emstrack.models.util.OnServiceComplete;
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

    View rootView;
    private Map<String, String> ambulanceStatus;
    private Map<Integer, Marker> ambulanceMarkers;
    private Map<Integer, Marker> hospitalMarkers;
    private Map<Integer, Marker> waypointMarkers;

    private ImageView showLocationButton;
    private boolean centerAmbulances = false;

    private ImageView showAmbulancesButton;
    private boolean showAmbulances = false;

    private ImageView showHospitalsButton;
    private boolean showHospitals = false;

    private ImageView showWaypointsButton;
    private boolean showWaypoints = false;

    private boolean myLocationEnabled;
    private boolean useMyLocation = false;

    private float defaultZoom = 15;
    private int defaultPadding = 50;

    private float zoomLevel = defaultZoom;

    private GoogleMap googleMap;
    private AmbulancesUpdateBroadcastReceiver receiver;
    private int screenOrientation;
    private OrientationEventListener orientationListener;

    static float ROTATIONS[] = { 0.f, 90.f, 180.f, 270.f };
    private GPSLocation defaultLocation;

    private boolean centerAtDefault;

    static int degreesToRotation(int degrees) {
        if (degrees > 315 || degrees < 45)
            return Surface.ROTATION_0;
        else if (degrees > 45 && degrees < 135)
            return Surface.ROTATION_90;
        else if (degrees > 135 && degrees < 225)
            return Surface.ROTATION_180;
        else // if (degrees > 225 && degrees < 315)
            return Surface.ROTATION_270;
    };

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

        // Retrieve location button
        showLocationButton = rootView.findViewById(R.id.showLocationButton);
        showLocationButton.setOnLongClickListener(new View.OnLongClickListener() {
            @Override
            public boolean onLongClick(View v) {

                // toggle show ambulances
                centerAmbulances = !centerAmbulances;

                // Switch color
                if (centerAmbulances)
                    showLocationButton.setBackgroundColor(getResources().getColor(R.color.mapButtonOn));
                else
                    showLocationButton.setBackgroundColor(getResources().getColor(R.color.mapButtonOff));

                Log.i(TAG, "Toggle center ambulances: " + (centerAmbulances ? "ON" : "OFF"));

                return true;

            }
        });
        showLocationButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {

                if (googleMap != null) {

                    // center ambulances
                    centerAmbulances();

                }

            }
        });

        // Retrieve ambulance button
        showAmbulancesButton = rootView.findViewById(R.id.showAmbulancesButton);
        showAmbulancesButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {

                // toggle show ambulances
                showAmbulances = !showAmbulances;

                // Switch color
                if (showAmbulances)
                    showAmbulancesButton.setBackgroundColor(getResources().getColor(R.color.mapButtonOn));
                else
                    showAmbulancesButton.setBackgroundColor(getResources().getColor(R.color.mapButtonOff));

                Log.i(TAG, "Toggle show ambulances: " + (showAmbulances ? "ON" : "OFF"));

                if (googleMap != null) {

                    if (showAmbulances)

                        // retrieve ambulances
                        retrieveAmbulances();

                    else {

                        // forget ambulances
                        forgetAmbulances();

                        // updateAmbulance markers without centering
                        updateMarkers();

                    }

                }


            }
        });

        // Retrieve hospitals button
        showHospitalsButton = rootView.findViewById(R.id.showHospitalsButton);
        showHospitalsButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {

                // toggle show hospitals
                showHospitals = !showHospitals;

                // Switch color
                if (showHospitals)
                    showHospitalsButton.setBackgroundColor(getResources().getColor(R.color.mapButtonOn));
                else
                    showHospitalsButton.setBackgroundColor(getResources().getColor(R.color.mapButtonOff));

                Log.i(TAG, "Toggle show hospitals: " + (showHospitals ? "ON" : "OFF"));

                if (googleMap != null) {

                    if (!showHospitals) {

                        // Clear markers
                        Iterator<Map.Entry<Integer,Marker>> iter = hospitalMarkers.entrySet().iterator();
                        while (iter.hasNext())
                        {
                            // retrieveObject entry
                            Map.Entry<Integer,Marker> entry = iter.next();

                            // remove from map
                            entry.getValue().remove();

                            // remove from collection
                            iter.remove();

                        }

                    }

                    // update markers without centering
                    updateMarkers();

                }

            }
        });

        // Retrieve waypoints button
        showWaypointsButton = rootView.findViewById(R.id.showWaypointsButton);
        showWaypointsButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {

                // toggle show waypoints
                showWaypoints = !showWaypoints;

                // Switch color
                if (showWaypoints)
                    showWaypointsButton.setBackgroundColor(getResources().getColor(R.color.mapButtonOn));
                else
                    showWaypointsButton.setBackgroundColor(getResources().getColor(R.color.mapButtonOff));

                Log.i(TAG, "Toggle show waypoints: " + (showWaypoints ? "ON" : "OFF"));

                if (googleMap != null) {

                    if (!showWaypoints) {

                        // Clear markers
                        Iterator<Map.Entry<Integer,Marker>> iter = waypointMarkers.entrySet().iterator();
                        while (iter.hasNext())
                        {
                            // retrieveObject entry
                            Map.Entry<Integer,Marker> entry = iter.next();

                            // remove from map
                            entry.getValue().remove();

                            // remove from collection
                            iter.remove();

                        }

                    }

                    // update markers and center
                    centerMap(updateMarkers());

                }

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

        // Initialize screen rotation
        if (rootView.getDisplay() != null)
            screenOrientation = rootView.getDisplay().getRotation();
        else
            screenOrientation = Surface.ROTATION_0;

        // Initialize map
        SupportMapFragment mapFragment = (SupportMapFragment) getChildFragmentManager()
                .findFragmentById(R.id.map);
        mapFragment.getMapAsync(this);

        // Orientation listener
        orientationListener = new OrientationEventListener(getContext(), SensorManager.SENSOR_DELAY_NORMAL) {

            @Override
            public void onOrientationChanged(int rotation) {

                Activity activity = getActivity();
                if (activity != null) {

                    //Log.d(TAG,"onOrientationChanged");
                    if (rotation != ORIENTATION_UNKNOWN) {
                        screenOrientation = degreesToRotation(rotation);
                        //Log.d(TAG, "rotation = " + rotation + ", screenOrientation = " + screenOrientation);
                    }
                }

            }

        };

        // Enable sensor
        if (orientationListener.canDetectOrientation())
            orientationListener.enable();
        else
            orientationListener.disable();

        return rootView;

    }

    public void retrieveAmbulances() {

        // Get ambulances
        AmbulanceAppData appData = AmbulanceForegroundService.getAppData();
        SparseArray<Ambulance> ambulances = appData.getAmbulances();

        if (ambulances.size() != appData.getProfile().getAmbulances().size() - 1) {

            Log.i(TAG,"No ambulances.");

            if (showAmbulances) {

                Log.i(TAG,"Retrieving ambulances...");

                // Disable button
                showAmbulancesButton.setEnabled(false);

                // Retrieve ambulances first
                Intent ambulancesIntent = new Intent(getContext(), AmbulanceForegroundService.class);
                ambulancesIntent.setAction(AmbulanceForegroundService.Actions.GET_AMBULANCES);

                // What to do when GET_AMBULANCES service completes?
                new OnServiceComplete(getContext(),
                        BroadcastActions.SUCCESS,
                        BroadcastActions.FAILURE,
                        ambulancesIntent) {

                    @Override
                    public void onSuccess(Bundle extras) {

                        Log.i(TAG,"Got them all. Updating markers.");

                        // updateAmbulance markers and center bounds
                        centerMap(updateMarkers());

                        // Enable button
                        showAmbulancesButton.setEnabled(true);

                    }
                }
                        .setFailureMessage(getString(R.string.couldNotRetrieveAmbulances))
                        .setAlert(new AlertSnackbar(getActivity()))
                        .start();

            }

        } else if (showAmbulances) {

            Log.i(TAG,"Already have ambulances. Updating markers.");

            // Already have ambulances

            // updateAmbulance markers and center bounds
            centerMap(updateMarkers());

        }

    }

    public void forgetAmbulances() {

        // disable button
        showAmbulancesButton.setEnabled(false);

        // Clear markers
        Iterator<Map.Entry<Integer,Marker>> iter = ambulanceMarkers.entrySet().iterator();
        while (iter.hasNext())
        {
            // retrieveObject entry
            Map.Entry<Integer,Marker> entry = iter.next();

            // remove from map
            entry.getValue().remove();

            // remove from collection
            iter.remove();

        }

        // Unsubscribe to ambulances
        Intent intent = new Intent(getActivity(), AmbulanceForegroundService.class);
        intent.setAction(AmbulanceForegroundService.Actions.STOP_AMBULANCES);

        // What to do when service completes?
        new OnServiceComplete(getContext(),
                BroadcastActions.SUCCESS,
                BroadcastActions.FAILURE,
                intent) {

            @Override
            public void onSuccess(Bundle extras) {
                Log.i(TAG, "onSuccess");

                showAmbulancesButton.setEnabled(true);

            }

        }
                .setFailureMessage(getString(R.string.couldNotUnsubscribeToAmbulances))
                .setAlert(new AlertSnackbar(getActivity()))
                .start();

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

        Log.d(TAG, "onResume");

        super.onResume();

        // Enable orientation listener
        if (orientationListener.canDetectOrientation())
            orientationListener.enable();
        else
            orientationListener.disable();

        // Register receiver
        IntentFilter filter = new IntentFilter();
        filter.addAction(AmbulanceForegroundService.BroadcastActions.OTHER_AMBULANCES_UPDATE);
        filter.addAction(AmbulanceForegroundService.BroadcastActions.AMBULANCE_UPDATE);
        filter.addAction(AmbulanceForegroundService.BroadcastActions.CALL_UPDATE);
        receiver = new AmbulancesUpdateBroadcastReceiver();
        getLocalBroadcastManager().registerReceiver(receiver, filter);

        // Switch colors
        if (showAmbulances)
            showAmbulancesButton.setBackgroundColor(getResources().getColor(R.color.mapButtonOn));
        else
            showAmbulancesButton.setBackgroundColor(getResources().getColor(R.color.mapButtonOff));

        if (showHospitals)
            showHospitalsButton.setBackgroundColor(getResources().getColor(R.color.mapButtonOn));
        else
            showHospitalsButton.setBackgroundColor(getResources().getColor(R.color.mapButtonOff));

        if (showWaypoints)
            showWaypointsButton.setBackgroundColor(getResources().getColor(R.color.mapButtonOn));
        else
            showWaypointsButton.setBackgroundColor(getResources().getColor(R.color.mapButtonOff));
        
        if (centerAmbulances)
            showLocationButton.setBackgroundColor(getResources().getColor(R.color.mapButtonOn));
        else
            showLocationButton.setBackgroundColor(getResources().getColor(R.color.mapButtonOff));

    }

    @Override
    public void onPause() {
        super.onPause();

        // Unregister receiver
        if (receiver != null) {
            getLocalBroadcastManager().unregisterReceiver(receiver);
            receiver = null;
        }

        // disable orientation listener
        orientationListener.disable();

    }

    @Override
    public void onCameraIdle() {
        zoomLevel = googleMap.getCameraPosition().zoom;
    }

    @Override
    public void onMapReady(GoogleMap googleMap) {

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

        if (showAmbulances) {

            // retrieve ambulances
            retrieveAmbulances();

        } else {
            
            // Update markers and center map
            centerMap(updateMarkers());
            
        }
                
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

                    googleMap.animateCamera(CameraUpdateFactory.newLatLngZoom(latLng, zoomLevel));

                    Log.d(TAG, "center at own ambulance");

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

        googleMap.moveCamera(CameraUpdateFactory.newLatLngZoom(latLng, zoomLevel));

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

        }
        
        // Update ambulances
        if (showAmbulances) {

            // Get ambulances
            SparseArray<Ambulance> ambulances = AmbulanceForegroundService.getAppData().getAmbulances();

            if (ambulances != null) {

                // Loop over all ambulances
                for (Ambulance ambulance : SparseArrayUtils.iterable(ambulances)) {

                    // Add marker for ambulance
                    Marker marker = addMarkerForAmbulance(ambulance);

                    // Add to bound builder
                    builder.include(marker.getPosition());

                }
            }

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

        Ambulance ambulance = AmbulanceForegroundService.getAppData().getAmbulance();
        if (ambulance != null) {

            GPSLocation location = ambulance.getLocation();
            LatLng latLng = new LatLng(location.getLatitude(), location.getLongitude());

            // Add marker for ambulance
            Marker marker = addMarkerForAmbulance(ambulance);

            // Center and orient map
            CameraPosition currentPlace = new CameraPosition.Builder()
                    .target(latLng)
                    .bearing((float) ambulance.getOrientation() + ROTATIONS[screenOrientation])
                    .zoom(zoomLevel)
                    .build();
            googleMap.animateCamera(CameraUpdateFactory.newCameraPosition(currentPlace));

        }

    }

    /**
     * Get LocalBroadcastManager
     *
     * @return the LocalBroadcastManager
     */
    private LocalBroadcastManager getLocalBroadcastManager() {
        return LocalBroadcastManager.getInstance(getContext());
    }

}
