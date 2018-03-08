package org.emstrack.ambulance.fragments;

import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.TextView;

import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.CameraPosition;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.MarkerOptions;

import org.emstrack.ambulance.MainActivity;
import org.emstrack.ambulance.R;

import static android.content.ContentValues.TAG;

/**
 * Created by justingil1748 on 4/26/17.
 */

public class DispatcherFragment extends Fragment implements View.OnClickListener, OnMapReadyCallback {

    View rootView;

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

        rootView = inflater.inflate(R.layout.fragment_dispatcher, container, false);


        addressText = ((TextView) rootView.findViewById(R.id.address));

        SupportMapFragment mapFragment = (SupportMapFragment) getChildFragmentManager()
                .findFragmentById(R.id.map);
        mapFragment.getMapAsync(this);


        // addressText.setText(AmbulanceApp.globalAddress);

        /*
        addressButton = (Button) view.findViewById(R.id.addressButton);
        addressButton.setOnClickListener(this);
        addressSearchText = (EditText) view.findViewById(R.id.addressSearch);
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

       // if(v == mapButton){

/*
            gps = new GPSTracker(view.getContext(), 500, -1);
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

        //}
        /*
        else if(v == addressButton){
            geoLocate();
        }
        */
    }

    @Override
    public void onMapReady(GoogleMap googleMap) {
        // Add a marker in Sydney, Australia,
        // and move the map's camera to the same location.
        LatLng sydney = new LatLng(-33.852, 151.211);
        googleMap.addMarker(new MarkerOptions().position(sydney)
                .title("Marker in Sydney"));
        CameraPosition cameraPosition = new CameraPosition.Builder()
                .target(sydney).zoom(15).build();
        googleMap.animateCamera(CameraUpdateFactory
                .newCameraPosition(cameraPosition));

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
