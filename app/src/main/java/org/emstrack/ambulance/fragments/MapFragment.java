package org.emstrack.ambulance.fragments;

import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.MarkerOptions;

import org.emstrack.ambulance.services.AmbulanceForegroundService;
import org.emstrack.ambulance.R;
import org.emstrack.models.Ambulance;
import org.emstrack.models.Location;
import org.emstrack.mqtt.MqttProfileClient;

import java.util.Map;

import static android.content.ContentValues.TAG;

/**
 * Created by justingil1748 on 4/26/17.
 */

public class MapFragment extends Fragment implements View.OnClickListener, OnMapReadyCallback {

    View rootView;
    private Map<String, String> ambulanceStatus;

    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {

        rootView = inflater.inflate(R.layout.fragment_map, container, false);

        // Get settings, status and capabilities
        final MqttProfileClient profileClient = AmbulanceForegroundService.getProfileClient(getContext());

        ambulanceStatus = profileClient.getSettings().getAmbulanceStatus();

        SupportMapFragment mapFragment = (SupportMapFragment) getChildFragmentManager()
                .findFragmentById(R.id.map);
        mapFragment.getMapAsync(this);

        return rootView;

    }

    /*
    Functionality of google map button
     */
    @Override
    public void onClick(View v) {

    }

    @Override
    public void onMapReady(GoogleMap googleMap) {

        boolean myLocationEnabled = false;
        if (AmbulanceForegroundService.canUpdateLocation()) {

            try {

                Log.i(TAG, "Enable my location on google map.");
                googleMap.setMyLocationEnabled(true);
                myLocationEnabled = true;

            } catch (SecurityException e) {
                Log.i(TAG, "Could not enable my location on google map.");
            }

        }

        // Update ambulance
        Ambulance ambulance = AmbulanceForegroundService.getAmbulance();
        if (ambulance != null) {

            Location location = ambulance.getLocation();
            LatLng latLng = new LatLng(location.getLatitude(),location.getLongitude());

            if (!myLocationEnabled) {

                // place marker
                googleMap.addMarker(new MarkerOptions()
                        .position(latLng)
                        .title(ambulance.getIdentifier())
                        .snippet(ambulanceStatus.get(ambulance.getStatus())));

            }
            // move camera
            googleMap.moveCamera(CameraUpdateFactory.newLatLngZoom(latLng, 15));

        }

    }


/*
    @Override
    public void onMapReady(GoogleMap googleMap) {
        mGoogleMap = googleMap;
        goToLocationZoom(32.8693185, -117.2130499, 15);
        mGoogleMap.setMyLocationEnabled(true);
    }

    private void goToLocation(double lat, double lon) {
        LatLng location = new LatLng(lat, lon);
        CameraUpdate updateLocation = CameraUpdateFactory.newLatLng(location);
        mGoogleMap.moveCamera(updateLocation);
    }

    private void goToLocationZoom(double lat, double lon, float zoom) {
        LatLng location = new LatLng(lat, lon);
        CameraUpdate updateLocation = CameraUpdateFactory.newLatLngZoom(location, zoom);
        mGoogleMap.moveCamera(updateLocation);

        mGoogleMap.setOnMapClickListener(new GoogleMap.OnMapClickListener() {
            @Override
            public void onMapClick(LatLng point) {
                if(marker != null)
                    marker.remove();
                else
                    marker = mGoogleMap.addMarker(new MarkerOptions().position(point));
            }
        });

    }

    public void geoLocate(){
        String location = addressSearchText.getText().toString();
        Geocoder gc = new Geocoder(getActivity());
        List<android.location.Address> list = null;

        try {
            list = gc.getFromLocationName(location + ", Tijuana, Mexico", 5);
        } catch (IOException e) {
            e.printStackTrace();
        }

        if(list.size() > 0) {
            Address address = list.get(0);
            String locality = address.getLocality();
            //Toast.makeText(getActivity(), locality, Toast.LENGTH_SHORT).show();
            double lat = address.getLatitude();
            double lon = address.getLongitude();
            goToLocationZoom(lat, lon, 15);

            if (marker != null) {
                marker.remove();
            }

            MarkerOptions options = new MarkerOptions().title(locality).position(new LatLng(lat, lon));
            marker = mGoogleMap.addMarker(options);
        }
        else{
            Toast.makeText(getActivity(), "Can't find the address", Toast.LENGTH_SHORT).show();
        }
    */
}
