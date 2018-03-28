package org.emstrack.ambulance.fragments;

import android.annotation.SuppressLint;
import android.annotation.TargetApi;
import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.VectorDrawable;
import android.os.Build;
import android.os.Bundle;
import android.support.annotation.ColorInt;
import android.support.annotation.DrawableRes;
import android.support.graphics.drawable.VectorDrawableCompat;
import android.support.v4.app.Fragment;
import android.support.v4.content.ContextCompat;
import android.support.v4.content.res.ResourcesCompat;
import android.support.v4.graphics.drawable.DrawableCompat;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.content.res.AppCompatResources;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;

import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.BitmapDescriptor;
import com.google.android.gms.maps.model.BitmapDescriptorFactory;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.LatLngBounds;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;

import org.emstrack.ambulance.dialogs.AlertSnackbar;
import org.emstrack.ambulance.services.AmbulanceForegroundService;
import org.emstrack.ambulance.R;
import org.emstrack.ambulance.services.OnServiceComplete;
import org.emstrack.models.Ambulance;
import org.emstrack.models.Hospital;
import org.emstrack.models.Location;
import org.emstrack.mqtt.MqttProfileClient;

import java.util.HashMap;
import java.util.Map;

import static android.content.ContentValues.TAG;

/**
 * Created by justingil1748 on 4/26/17.
 */

public class MapFragment extends Fragment implements View.OnClickListener, OnMapReadyCallback {

    View rootView;
    private Map<String, String> ambulanceStatus;
    private Map<Integer, Marker> ambulanceMarkers;
    private Button showAmbulancesButton;
    private boolean showAmbulances = true;
    private GoogleMap googleMap;
    private boolean myLocationEnabled;

    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {

        rootView = inflater.inflate(R.layout.fragment_map, container, false);

        // Retrieve ambulance button
        showAmbulancesButton = (Button) rootView.findViewById(R.id.showAmbulancesButton);
        showAmbulancesButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {

                // toggle show ambulances
                showAmbulances = !showAmbulances;

                Log.i(TAG, "Toggle show ambulances: " + (showAmbulances ? "ON" : "OFF"));

                if (googleMap != null) {

                    if (showAmbulances)

                        // retrieve ambulances
                        retrieveAmbulances();

                    else {

                        // Clear all markers
                        googleMap.clear();

                        // forget ambulances
                        forgetAmbulances();

                    }

                }


            }
        });

        // Initialize markers map
        ambulanceMarkers = new HashMap<>();

        // Get settings, status and capabilities
        final MqttProfileClient profileClient = AmbulanceForegroundService.getProfileClient(getContext());

        ambulanceStatus = profileClient.getSettings().getAmbulanceStatus();

        // Retrieve ambulances?
        retrieveAmbulances();

        // Initialize map
        SupportMapFragment mapFragment = (SupportMapFragment) getChildFragmentManager()
                .findFragmentById(R.id.map);
        mapFragment.getMapAsync(this);

        return rootView;

    }

    public void retrieveAmbulances() {

        // Get ambulances
        Map<Integer, Ambulance> ambulances = AmbulanceForegroundService.getAmbulances();

        if (ambulances == null) {

            if (showAmbulances) {

                // Retrieve ambulances first
                Intent ambulancesIntent = new Intent(getContext(), AmbulanceForegroundService.class);
                ambulancesIntent.setAction(AmbulanceForegroundService.Actions.GET_AMBULANCES);

                // What to do when GET_AMBULANCES service completes?
                new OnServiceComplete(getContext(),
                        AmbulanceForegroundService.BroadcastActions.SUCCESS,
                        AmbulanceForegroundService.BroadcastActions.FAILURE,
                        ambulancesIntent) {

                    @Override
                    public void onSuccess(Bundle extras) {

                        updateMarkers();

                    }
                }
                        .setFailureMessage(getString(R.string.couldNotRetrieveAmbulances))
                        .setAlert(new AlertSnackbar(getActivity()));

            }

        } else if (showAmbulances) {

            // Already have ambulances
            updateMarkers();

        }

    }

    public void forgetAmbulances() {

    }

    /*
    Functionality of google map button
     */
    @Override
    public void onClick(View v) {

    }

    @Override
    public void onMapReady(GoogleMap googleMap) {

        // save map
        this.googleMap = googleMap;

        myLocationEnabled = false;
        if (AmbulanceForegroundService.canUpdateLocation()) {

            try {

                Log.i(TAG, "Enable my location on google map.");
                googleMap.setMyLocationEnabled(true);
                myLocationEnabled = true;

            } catch (SecurityException e) {
                Log.i(TAG, "Could not enable my location on google map.");
            }

        }

        // Update markers
        updateMarkers();

    }

    public void updateMarkers() {

        // fast return
        if (googleMap == null)
            return;

        // Assemble marker bounds
        LatLngBounds.Builder builder = new LatLngBounds.Builder();

        // Update ambulances
        if (showAmbulances) {

            // Get ambulances
            Map<Integer, Ambulance> ambulances = AmbulanceForegroundService.getAmbulances();

            if (ambulances != null) {

                // Loop over all ambulances
                for (Map.Entry<Integer, Ambulance> entry : ambulances.entrySet()) {

                    // Get ambulance
                    Ambulance ambulance = entry.getValue();

                    // Find new location
                    Location location = ambulance.getLocation();
                    LatLng latLng = new LatLng(location.getLatitude(), location.getLongitude());

                    // Place marker
                    Marker marker = googleMap.addMarker(new MarkerOptions()
                            .position(latLng)
                            .icon(bitmapDescriptorFromVector(getActivity(), R.drawable.ambulance_blue, 0.1))
                            .anchor(0.5F,0.5F)
                            .rotation((float) ambulance.getOrientation())
                            .flat(true)
                            .title(ambulance.getIdentifier())
                            .snippet(ambulanceStatus.get(ambulance.getStatus())));

                    // Save marker
                    ambulanceMarkers.put(ambulance.getId(), marker);

                    // Add to bound builder
                    builder.include(marker.getPosition());

                }
            }

        }

        // Update own ambulance
        Ambulance ambulance = AmbulanceForegroundService.getAmbulance();
        if (ambulance != null) {

            Location location = ambulance.getLocation();
            LatLng latLng = new LatLng(location.getLatitude(),location.getLongitude());

            if (!myLocationEnabled) {

                // Place marker
                Marker marker = googleMap.addMarker(new MarkerOptions()
                        .position(latLng)
                        .icon(bitmapDescriptorFromVector(getActivity(), R.drawable.ambulance_blue, 0.1))
                        .anchor(0.5F,0.5F)
                        .rotation((float) ambulance.getOrientation())
                        .flat(true)
                        .title(ambulance.getIdentifier())
                        .snippet(ambulanceStatus.get(ambulance.getStatus())));

                // Save marker
                ambulanceMarkers.put(ambulance.getId(), marker);

            }

            // Add to bound builder
            builder.include(latLng);

            // Calculate bounds
            LatLngBounds bounds = builder.build();

            // move camera
            if (ambulanceMarkers.size() == 1) {

                googleMap.moveCamera(CameraUpdateFactory.newLatLngZoom(latLng, 15));

            } else {

                // move camera
                googleMap.moveCamera(CameraUpdateFactory.newLatLngBounds(bounds, 50));

            }

        }

    }

    /*
     * This is from
     * https://stackoverflow.com/questions/42365658/custom-marker-in-google-maps-in-android-with-vector-asset-icon
     */
    private BitmapDescriptor bitmapDescriptorFromVector(Context context, int vectorResId, double scale) {
        Drawable vectorDrawable = ContextCompat.getDrawable(context, vectorResId);
        int width = Math.round((float) Math.ceil(vectorDrawable.getIntrinsicWidth()*scale));
        int height = Math.round((float) Math.ceil(vectorDrawable.getIntrinsicHeight()*scale));
        vectorDrawable.setBounds(0, 0, width, height);
        Bitmap bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888);
        Canvas canvas = new Canvas(bitmap);
        vectorDrawable.draw(canvas);
        return BitmapDescriptorFactory.fromBitmap(bitmap);
    }

}
