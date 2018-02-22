package org.emstrack.ambulance.fragments;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;

import org.emstrack.ambulance.R;

import static android.content.ContentValues.TAG;

/**
 * Created by justingil1748 on 4/26/17.
 */

public class DispatcherActivity extends Fragment implements View.OnClickListener {

    View rootView;
    Button mapButton;
    static TextView addressText;

/*
    GPSTracker gps;
    DispatcherCall dCall;
*/

    //GoogleMap mGoogleMap;
    //Button addressButton;
    //EditText addressSearchText;
    //Marker marker;


    //Dispatcher page
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {

        rootView = inflater.inflate(R.layout.activity_dispatcher, container, false);

        mapButton = (Button) rootView.findViewById(R.id.gmap);
        mapButton.setOnClickListener(this);

        addressText = ((TextView) rootView.findViewById(R.id.address));
        // addressText.setText(AmbulanceApp.globalAddress);

        /*
        addressButton = (Button) rootView.findViewById(R.id.addressButton);
        addressButton.setOnClickListener(this);
        addressSearchText = (EditText) rootView.findViewById(R.id.addressSearch);
        marker = null;

        if (googleService()) {
            SupportMapFragment  fragment = (SupportMapFragment) getChildFragmentManager().findFragmentById(R.id.map);
            fragment.getMapAsync(this);
        }
        */

        return rootView;

    }

    public static void updateAddress(String msg) {
        Log.d(TAG, "updateAddress: message re1");
        // addressText.setText(AmbulanceApp.globalAddress);
        Log.d(TAG, "updateAddress: message re2");
    }

    /*
    Functionality of google map button
     */
    @Override
    public void onClick(View v) {

        if(v == mapButton){

/*
            gps = new GPSTracker(rootView.getContext(), 500, -1);
            gps.getLastKnownLocationIfAllowed();
            gps.getLocation();

            String dispatchLong = getLong();
            String dispatchLat = getLatitude();

            double lat = gps.getLatitude(); // returns latitude
            double lon = gps.getLongitude(); // returns longitude

            LocationPoint loc = new LocationPoint(lon, lat);

            String geoUri = "http://maps.google.com/maps?q=loc:" + dispatchLat + "," + dispatchLong + " (" + loc + ")";
            Intent intent = new Intent(Intent.ACTION_VIEW, Uri.parse(geoUri));
            startActivity(intent);
*/

        }
        /*
        else if(v == addressButton){
            geoLocate();
        }
        */
    }


    /*
    public boolean googleService(){
        GoogleApiAvailability api = GoogleApiAvailability.getInstance();
        int isAvailable = api.isGooglePlayServicesAvailable(getActivity());

        if(isAvailable == ConnectionResult.SUCCESS){
            return true;
        }
        else{
            return false;
        }

    }
    */


/*
    @Override
    public void onMapReady(GoogleMap googleMap) {
        mGoogleMap = googleMap;
        goToLocationZoom(32.8693185, -117.2130499, 15);
        mGoogleMap.setMyLocationEnabled(true);
    }

    private void goToLocation(double lat, double lon) {
        LatLng location = new LatLng(lat, lon);
        CameraUpdate update = CameraUpdateFactory.newLatLng(location);
        mGoogleMap.moveCamera(update);
    }

    private void goToLocationZoom(double lat, double lon, float zoom) {
        LatLng location = new LatLng(lat, lon);
        CameraUpdate update = CameraUpdateFactory.newLatLngZoom(location, zoom);
        mGoogleMap.moveCamera(update);

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
