package org.emstrack.ambulance.fragments;

import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CompoundButton;
import android.widget.Switch;
import android.widget.TextView;

import org.emstrack.ambulance.GPSTracker;
import org.emstrack.ambulance.MainActivity;
import org.emstrack.ambulance.R;

import java.util.Date;


/**
 * Java Class AND ACTIVITY
 * implements code for the GPSActivity Activity
 * Methods for lists and buttons are here.
 *
 * TODO
 * Location Point should probably be its own entity.
 *
 * Then when the LP is used to store data inside the phone, there
 * might be a method specific to the I/O that will parse the
 * LP Object. Or, LP might have the toString method modified
 * so that when we print to the file, we can just call the
 * toString method, and the save the time.
 *
 * The stack that will try to continually push most recent
 * data to the server might use the LP's method that will
 * return a new JSONObject.
 */
public class GPSActivity extends Fragment implements CompoundButton.OnCheckedChangeListener{

    private static final String TAG = GPSActivity.class.getSimpleName();;

    private View view;

    private TextView latitudeText;
    private TextView longitudeText;
    private TextView timestampText;
    private TextView orientationText;

    private Switch startTrackingSwitch;

    /*
     * Default method
     * Always called when an activity is created.
     * @param savedInstanceState
     */    @Override

    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {

        // inflate view
        view = inflater.inflate(R.layout.activity_gps, container, false);

        // Retrieve texts
        latitudeText = (TextView) view.findViewById(R.id.latitudeText);
        longitudeText = (TextView) view.findViewById(R.id.longitudeText);
        timestampText = (TextView) view.findViewById(R.id.timestampText);
        orientationText = (TextView) view.findViewById(R.id.orientationText);

        // To track or not to track?
        startTrackingSwitch = (Switch) view.findViewById(R.id.startTrackingSwitch);
        startTrackingSwitch.setOnCheckedChangeListener(this);

        // Retrieve last location and update
        android.location.Location lastLocation = ((MainActivity) getActivity()).getLastLocation();
        if (lastLocation != null) {
            updateLocation(lastLocation);
        }

        return view;
    }

    public void setLatitudeText(String latitudeText) {
        this.latitudeText.setText(latitudeText);
    }

    public void setLongitudeText(String longitudeText) {
        this.longitudeText.setText(longitudeText);
    }

    public void setTimestampText(String timestampText) {
        this.timestampText.setText(timestampText);
    }

    public void setOrientationText(String orientationText) {
        this.orientationText.setText(orientationText);
    }

    public void updateLocation(android.location.Location lastLocation) {

        setLatitudeText(Double.toString(lastLocation.getLatitude()));
        setLongitudeText(Double.toString(lastLocation.getLongitude()));
        setOrientationText(Double.toString(lastLocation.getBearing()));
        setTimestampText(new Date(lastLocation.getTime()).toString());

    }

    @Override
    public void onPause() {
        Log.i(TAG, "onPause: GPSActivity");
        super.onPause(); // This is required for some reason.
    }

    @Override
    public void onStop(){
        Log.i(TAG, "onStop: GPSActivity");
        // startTrackingSwitch.setChecked(false);
        super.onStop(); // Same.
    }

    @Override
    public void onDestroy() {
        Log.i(TAG, "onDestroy: GPSActivity");
        // startTrackingSwitch.setChecked(false);
        super.onDestroy(); // Same.
    }

    @Override
    public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {

        if (isChecked) {

            // turn on tracking
            ((MainActivity) getActivity()).startLocationUpdates();

        } else {

            // turn off tracking
            ((MainActivity) getActivity()).stopLocationUpdates();

        }

    }
}


