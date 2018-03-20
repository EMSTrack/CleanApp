package org.emstrack.ambulance.fragments;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v4.content.LocalBroadcastManager;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.CompoundButton;
import android.widget.Spinner;
import android.widget.Switch;
import android.widget.TextView;

import org.emstrack.ambulance.AmbulanceForegroundService;
import org.emstrack.ambulance.LoginActivity;
import org.emstrack.ambulance.MainActivity;
import org.emstrack.ambulance.R;
import org.emstrack.models.Ambulance;
import org.emstrack.mqtt.MqttProfileClient;

import java.util.ArrayList;
import java.util.Collections;
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

    AmbulancesUpdateBroadcastReceiver receiver;

    public class AmbulancesUpdateBroadcastReceiver extends BroadcastReceiver {

        @Override
        public void onReceive(Context context, Intent intent ) {
            if (intent != null) {
                final String action = intent.getAction();
                if (action.equals(AmbulanceForegroundService.BroadcastActions.AMBULANCE_UPDATE)) {

                    Log.i(TAG, "AMBULANCE_UPDATE");
                    update(AmbulanceForegroundService.getAmbulance());

                }
            }
        }
    };

    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {

        // inflate view
        view = inflater.inflate(R.layout.fragment_ambulance, container, false);

        // Get settings, status and capabilities
        final MqttProfileClient profileClient = AmbulanceForegroundService.getProfileClient(getContext());

        ambulanceStatus = profileClient.getSettings().getAmbulanceStatus();

        ambulanceStatusList = new ArrayList<String>(ambulanceStatus.values());
        Collections.sort(ambulanceStatusList);

        ambulanceCapabilities = profileClient.getSettings().getAmbulanceCapability();
        ambulanceCapabilityList = new ArrayList<String>(ambulanceCapabilities.values());
        Collections.sort(ambulanceCapabilityList);

        // Retrieve identifier
        identifierText = (TextView) view.findViewById(R.id.headerText);

        // Retrieve location
        latitudeText = (TextView) view.findViewById(R.id.latitudeText);
        longitudeText = (TextView) view.findViewById(R.id.longitudeText);
        timestampText = (TextView) view.findViewById(R.id.timestampText);
        orientationText = (TextView) view.findViewById(R.id.orientationText);

        // To track or not to track?
        startTrackingSwitch = (Switch) view.findViewById(R.id.startTrackingSwitch);
        startTrackingSwitch.setChecked(AmbulanceForegroundService.isRequestingLocationUpdates());
        startTrackingSwitch.setOnCheckedChangeListener(this);

        // Can track
        startTrackingSwitch.setEnabled(AmbulanceForegroundService.canUpdateLocation());

        // Other text
        capabilityText = (TextView) view.findViewById(R.id.capabilityText);
        commentText = (TextView) view.findViewById(R.id.commentText);
        updatedOnText = (TextView) view.findViewById(R.id.updatedOnText);


        // Set status spinner
        statusSpinner = (Spinner) view.findViewById(R.id.statusSpinner);
        // Create an ArrayAdapter using the string array and a default spinner layout
        ArrayAdapter<String> statusSpinnerAdapter =
                new ArrayAdapter<>(getContext(),
                        R.layout.status_spinner_item,
                        ambulanceStatusList);
        statusSpinnerAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        statusSpinner.setAdapter(statusSpinnerAdapter);

        // Update ambulance
        Ambulance ambulance = AmbulanceForegroundService.getAmbulance();
        if (ambulance != null)
            update(ambulance);

        return view;
    }

    @Override
    public void onResume() {
        super.onResume();

        // Update ambulance
        Ambulance ambulance = AmbulanceForegroundService.getAmbulance();
        if (ambulance != null)
            update(ambulance);

        // Register receiver
        IntentFilter filter = new IntentFilter();
        filter.addAction(AmbulanceForegroundService.BroadcastActions.AMBULANCE_UPDATE);
        receiver = new AmbulanceFragment.AmbulancesUpdateBroadcastReceiver();
        getLocalBroadcastManager().registerReceiver(receiver, filter);

    }

    @Override
    public void onPause() {
        super.onPause();

        // Unregister receiver
        if (receiver != null) {
            getLocalBroadcastManager().unregisterReceiver(receiver);
            receiver = null;
        }
        super.onDestroy();
    }

    public void update(Ambulance ambulance) {

        // set identifier
        identifierText.setText(ambulance.getIdentifier());
        ((MainActivity) getActivity()).setHeader(ambulance.getIdentifier());

        // set location
        latitudeText.setText(String.format("%.6f", ambulance.getLocation().getLatitude()));
        longitudeText.setText(String.format("%.6f", ambulance.getLocation().getLongitude()));
        orientationText.setText(String.format("%.1f", ambulance.getOrientation()));
        timestampText.setText(ambulance.getTimestamp().toString());

        // set status and comment
        commentText.setText(ambulance.getComment());
        updatedOnText.setText(ambulance.getUpdatedOn().toString());

        // set capability
        capabilityText.setText(ambulanceCapabilities.get(ambulance.getCapability()));

        // set spinner
        statusSpinner.setSelection(ambulanceStatusList.indexOf(ambulanceStatus.get(ambulance.getStatus())));

    }

    @Override
    public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {

        if (isChecked) {

            // turn on tracking
            Intent intent = new Intent(getContext(), AmbulanceForegroundService.class);
            intent.setAction(AmbulanceForegroundService.Actions.START_LOCATION_UPDATES);
            getActivity().startService(intent);

        } else {

            // turn off tracking
            Intent intent = new Intent(getContext(), AmbulanceForegroundService.class);
            intent.setAction(AmbulanceForegroundService.Actions.STOP_LOCATION_UPDATES);
            getActivity().startService(intent);

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