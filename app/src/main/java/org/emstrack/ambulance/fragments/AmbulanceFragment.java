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
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.CompoundButton;
import android.widget.Spinner;
import android.widget.Switch;
import android.widget.TextView;

import org.emstrack.ambulance.services.AmbulanceForegroundService;
import org.emstrack.ambulance.MainActivity;
import org.emstrack.ambulance.R;
import org.emstrack.models.Ambulance;
import org.emstrack.mqtt.MqttProfileClient;

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.TimeZone;

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
    private boolean suppressNextSpinnerUpdate = false;

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

        // Process change of status
        statusSpinner.setOnItemSelectedListener(
                new AdapterView.OnItemSelectedListener() {
                    @Override
                    public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                        Log.i(TAG, "Item '" + position + "' selected.");

                        // Should only update on server as a result of user interaction
                        // Otherwise this will create a loop with mqtt updating ambulance
                        // TODO: Debug spinner multiple updates
                        // This may not be easy with the updates being called from a service
                        if (suppressNextSpinnerUpdate) {

                            Log.i(TAG, "Skipping status spinner update.");

                            // reset the update flag
                            suppressNextSpinnerUpdate = false;

                        } else {

                            Log.i(TAG, "Processing status spinner update.");

                            // Get status from spinner
                            String status = (String) parent.getItemAtPosition(position);

                            // Search for entry in ambulanceStatus map
                            String statusCode = "";
                            for (Map.Entry<String, String> entry : ambulanceStatus.entrySet()) {
                                if (status.equals(entry.getValue())) {
                                    statusCode = entry.getKey();
                                    break;
                                }
                            }

                            // format timestamp
                            DateFormat df = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'");
                            df.setTimeZone(TimeZone.getTimeZone("UTC"));
                            String timestamp = df.format(new Date());

                            // Set update string
                            String updateString = "{\"status\":\"" + statusCode + "\",\"timestamp\":\"" + timestamp + "\"}";

                            // Update on server
                            // TODO: Update along with locations because it will be recorded with
                            // the wrong location on the server
                            Intent intent = new Intent(getContext(), AmbulanceForegroundService.class);
                            intent.setAction(AmbulanceForegroundService.Actions.UPDATE_AMBULANCE);
                            intent.putExtra("UPDATES",updateString);
                            getActivity().startService(intent);

                        }
                    }

                    @Override
                    public void onNothingSelected(AdapterView<?> parent) {
                        Log.i(TAG, "Nothing selected: this should never happen.");
                    }
                }
        );

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

        // set spinner only if position changed
        // this helps to prevent a possible server loop
        int position = ambulanceStatusList.indexOf(ambulanceStatus.get(ambulance.getStatus()));
        int currentPosition = statusSpinner.getSelectedItemPosition();
        if (currentPosition != position) {

            Log.i(TAG,"Spinner changed from " + currentPosition + " to " + position);

            // set flag to prevent spinner update from triggering server update
            suppressNextSpinnerUpdate = true;

            // update spinner
            statusSpinner.setSelection(position);
        } else {

            Log.i(TAG, "Spinner continues to be at position " + position + ". Skipping update");

        }

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