package org.emstrack.ambulance.fragments;

import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.CompoundButton;
import android.widget.Spinner;
import android.widget.Switch;
import android.widget.TextView;

import org.emstrack.ambulance.AmbulanceApp;
import org.emstrack.ambulance.MainActivity;
import org.emstrack.ambulance.R;
import org.emstrack.models.Ambulance;
import org.emstrack.mqtt.MqttProfileClient;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.Map;


/**
 * Java Class AND ACTIVITY
 * implements code for the AmbulanceFragment Activity
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
public class AmbulanceFragment extends Fragment implements CompoundButton.OnCheckedChangeListener{

    private static final String TAG = AmbulanceFragment.class.getSimpleName();;

    private View view;

    private TextView identifierText;

    private Spinner statusSpinner;

    private TextView latitudeText;
    private TextView longitudeText;
    private TextView timestampText;
    private TextView orientationText;

    private Switch startTrackingSwitch;

    private TextView capabilityText;

    private TextView commentText;

    private TextView updatedOnText;

    private Map<String,String> ambulanceStatus;
    private List<String> ambulanceStatusList;
    private Map<String,String> ambulanceCapabilities;
    private List<String> ambulanceCapabilityList;

    /*
     * Default method
     * Always called when an activity is created.
     * @param savedInstanceState
     */    @Override

    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {

        // inflate view
        view = inflater.inflate(R.layout.fragment_ambulance, container, false);

        // Retrieve identifier
        identifierText = (TextView) view.findViewById(R.id.identifierText);

        // Retrieve location
        latitudeText = (TextView) view.findViewById(R.id.latitudeText);
        longitudeText = (TextView) view.findViewById(R.id.longitudeText);
        timestampText = (TextView) view.findViewById(R.id.timestampText);
        orientationText = (TextView) view.findViewById(R.id.orientationText);

        // To track or not to track?
        startTrackingSwitch = (Switch) view.findViewById(R.id.startTrackingSwitch);
        startTrackingSwitch.setOnCheckedChangeListener(this);

        // Other text
        capabilityText = (TextView) view.findViewById(R.id.capabilityText);
        commentText = (TextView) view.findViewById(R.id.commentText);
        updatedOnText = (TextView) view.findViewById(R.id.updatedOnText);

        // Get settings, status and capabilities
        final MqttProfileClient profileClient = ((AmbulanceApp) getActivity().getApplication()).getProfileClient();
        ambulanceStatus = profileClient.getSettings().getAmbulanceStatus();
        ambulanceStatusList = new ArrayList<String>(ambulanceStatus.values());
        Collections.sort(ambulanceStatusList);
        ambulanceCapabilities = profileClient.getSettings().getAmbulanceCapability();
        ambulanceCapabilityList = new ArrayList<String>(ambulanceCapabilities.values());
        Collections.sort(ambulanceCapabilityList);

        // Set status spinner
        statusSpinner = (Spinner) view.findViewById(R.id.statusSpinner);
        // Create an ArrayAdapter using the string array and a default spinner layout
        ArrayAdapter<String> statusSpinnerAdapter =
                new ArrayAdapter<>(getContext(),
                        R.layout.status_spinner_item,
                        ambulanceStatusList);
        statusSpinnerAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        statusSpinner.setAdapter(statusSpinnerAdapter);

        // update
        Ambulance ambulance = ((MainActivity) getActivity()).getAmbulance();
        if (ambulance != null)
            updateAmbulance(ambulance);

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

    public void updateAmbulance(Ambulance ambulance) {

        // set identifier
        identifierText.setText(ambulance.getIdentifier());

        // set location
        setLatitudeText(String.format("%.6f", ambulance.getLocation().getLatitude()));
        setLongitudeText(String.format("%.6f", ambulance.getLocation().getLongitude()));
        setOrientationText(String.format("%.1f", ambulance.getOrientation()));
        setTimestampText(ambulance.getTimestamp().toString());

        // set status and comment
        commentText.setText(ambulance.getComment());
        updatedOnText.setText(ambulance.getUpdatedOn().toString());

        // set capability
        capabilityText.setText(ambulanceCapabilities.get(ambulance.getCapability()));

        // set spinner
        statusSpinner.setSelection(ambulanceStatusList.indexOf(ambulanceStatus.get(ambulance.getStatus())));

    }

/*
    @Override
    public void onPause() {
        Log.i(TAG, "onPause: AmbulanceFragment");
        super.onPause(); // This is required for some reason.
    }

    @Override
    public void onStop(){
        Log.i(TAG, "onStop: AmbulanceFragment");
        // startTrackingSwitch.setChecked(false);
        super.onStop(); // Same.
    }

    @Override
    public void onDestroy() {
        Log.i(TAG, "onDestroy: AmbulanceFragment");
        // startTrackingSwitch.setChecked(false);
        super.onDestroy(); // Same.
    }
*/

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