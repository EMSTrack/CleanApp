package org.emstrack.ambulance.fragments;

import android.location.Location;
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
import java.util.Observable;
import java.util.Observer;


/**
 * Java Class AND ACTIVITY
 * implements code for the GPSFragment Activity
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
public class GPSFragment extends Fragment implements Observer {

    private static final String TAG = GPSFragment.class.getSimpleName();;

    private TextView latitudeText;
    private TextView longitudeText;
    private TextView timestampText;
    private TextView orientationText;

    private GPSTracker gpsTracker;

    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        final View view = inflater.inflate(R.layout.fragment_gps, container, false);
        final GPSFragment gpsFragment = this;

        latitudeText = view.findViewById(R.id.latitudeText);
        longitudeText = view.findViewById(R.id.longitudeText);
        timestampText = view.findViewById(R.id.timestampText);
        orientationText = view.findViewById(R.id.orientationText);

        Switch startTrackingSwitch = view.findViewById(R.id.startTrackingSwitch);
        startTrackingSwitch.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton compoundButton, boolean isChecked) {
                if (isChecked) {

                    gpsTracker = new GPSTracker(gpsFragment, view.getContext(), 500, -1);

                } else {

                    // TODO: turn off tracking

                }
            }
        });
        return view;
    }

    @Override
    public void update(Observable observable, Object o) {
        Log.e("GPSFRAGMENT", "update called");
        GPSLocation location = gpsTracker.getGPSLocation();
        latitudeText.setText(location.getLatitude());
        longitudeText.setText(location.getLongitude());
        timestampText.setText(location.getTime());
    }
}


