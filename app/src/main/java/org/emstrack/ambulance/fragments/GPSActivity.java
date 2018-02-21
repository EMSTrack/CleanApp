package org.emstrack.ambulance.fragments;

import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CompoundButton;
import android.widget.Switch;
import android.widget.TextView;

import org.emstrack.ambulance.R;


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

    Switch trackByTime;             // The switch for clock enable.
    Switch trackByDistance;
    View rootView;

    // GPSTracker gpsTracker;

    /*
     * Default method
     * Always called when an activity is created.
     * @param savedInstanceState
     */    @Override

    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {

        rootView = inflater.inflate(R.layout.activity_gps, container, false);

        //checkLocationPermission(); //Might be needed, might not.

        // gpsTracker = new GPSTracker(rootView.getContext(), 500, -1);
        // gpsTracker.setLatLongTextView((TextView) rootView.findViewById(R.id.LatLongText));

        //Determine whether to listen by dist changed or time changed
        trackByTime = (Switch) rootView.findViewById(R.id.trackByTimeSwitch);
        trackByTime.setOnCheckedChangeListener(this);

        trackByDistance = (Switch) rootView.findViewById(R.id.trackByDistanceSwitch);
        trackByDistance.setOnCheckedChangeListener(this);

        return rootView;
    }


    @Override
    public void onPause() {
        System.err.println("onPause: GPSActivity");
        super.onPause(); // This is required for some reason.
    }

    @Override
    public void onStop(){
        System.err.println("onStop: GPSActivity");
        trackByTime.setChecked(false);
        super.onStop(); // Same.
    }

    @Override
    public void onDestroy() {
        System.err.print("onDestroy: GPSActivity");
        trackByTime.setChecked(false);
        super.onDestroy(); // Same.
    }

    @Override
    public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {

        if (isChecked) {

            //determine what was turned on
            switch (buttonView.getId()) {

/*
                case R.id.trackByTimeSwitch: //turn on tracking by time
                    //gpsTracker.turnOff();
                    long currDistanceTracking = gpsTracker.getMinDistanceChangeForUpdates();
                    gpsTracker = new GPSTracker(rootView.getContext(), currDistanceTracking, -1);
                    gpsTracker.setLatLongTextView((TextView) rootView.findViewById(R.id.LatLongText));
                    break;
                case R.id.trackByDistanceSwitch: //turn on tracking by distance
                    //gpsTracker.turnOff();
                    long currTimeTracking = gpsTracker.getMinTimeBWUpdates();
                    gpsTracker = new GPSTracker(rootView.getContext(), -1, currTimeTracking);
                    gpsTracker.setLatLongTextView((TextView) rootView.findViewById(R.id.LatLongText));
                    break;
*/
                default:

            }

        } else { // something was turned off

            switch (buttonView.getId()) {
/*
                case R.id.trackByTimeSwitch: //turn off tracking by time
                    gpsTracker.turnOff();
                    long currDistanceTracking = gpsTracker.getMinDistanceChangeForUpdates();
                    //continue tracking by distance
                    gpsTracker = new GPSTracker(rootView.getContext(), 0, currDistanceTracking);
                    break;
                case R.id.trackByDistanceSwitch: //turn off tracking by distance
                    gpsTracker.turnOff();
                    long currTimeTracking = gpsTracker.getMinTimeBWUpdates();
                    //continue tracking by time
                    gpsTracker = new GPSTracker(rootView.getContext(), currTimeTracking, 0);
                    break;
*/
                default:
            }
        }

    }
}


