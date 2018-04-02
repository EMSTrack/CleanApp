package org.emstrack.ambulance.fragments;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v4.content.LocalBroadcastManager;
import android.support.v7.app.AlertDialog;
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
import android.widget.Toast;

import org.emstrack.ambulance.LoginActivity;
import org.emstrack.ambulance.dialogs.AlertSnackbar;
import org.emstrack.ambulance.services.AmbulanceForegroundService;
import org.emstrack.ambulance.MainActivity;
import org.emstrack.ambulance.R;
import org.emstrack.ambulance.services.OnServiceComplete;
import org.emstrack.models.Ambulance;
import org.emstrack.models.AmbulancePermission;
import org.emstrack.mqtt.MqttProfileClient;

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.TimeZone;

public class AmbulanceFragment extends Fragment implements AdapterView.OnItemSelectedListener, CompoundButton.OnCheckedChangeListener{

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
    private int requestingToStreamLocation;
    private final int MAX_NUMBER_OF_LOCATION_REQUESTS_ATTEMPTS = 3;

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

        // initialize requestingToStreamLocation
        requestingToStreamLocation = 0;

        // Update ambulance
        Ambulance ambulance = AmbulanceForegroundService.getAmbulance();
        if (ambulance != null)
            update(ambulance);

        // Process change of status
        statusSpinner.setOnItemSelectedListener(this);

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

    }

    public void update(Ambulance ambulance) {

        if (requestingToStreamLocation > 0)

            if (canStreamLocation()) {

                Log.d(TAG, "Succeeded in request to stream location");

                // Let user know
                Toast.makeText(getContext(), R.string.startedStreamingLocation, Toast.LENGTH_SHORT).show();

                // turn on tracking
                Intent intent = new Intent(getContext(), AmbulanceForegroundService.class);
                intent.setAction(AmbulanceForegroundService.Actions.START_LOCATION_UPDATES);
                getActivity().startService(intent);

                // reset request to stream location
                requestingToStreamLocation = 0;

                // set switch
                setSwitch(true);

            } else {

                if (requestingToStreamLocation-- > 0) {

                    Log.d(TAG, "Failed in requesting to stream location, will try " +
                            requestingToStreamLocation + " more times.");

                } else {

                    // Letting know that tried once before
                    requestingToStreamLocation = -1;

                    // Alert user
                    new AlertSnackbar(getActivity())
                            .alert(getResources().getString(R.string.anotherClientIsStreamingLocations));

                }
            }

        // stop location upates?
        if (!canStreamLocation() && canWrite() && startTrackingSwitch.isChecked()) {

            // set switch off
            // will trigger event handler
            startTrackingSwitch.setChecked(false);

            // Toast to warn user
            Toast.makeText(getContext(), R.string.anotherClientRequestedLocations, Toast.LENGTH_LONG).show();

        }

        // set spinner only if position changed
        // this helps to prevent a possible server loop
        int position = ambulanceStatusList.indexOf(ambulanceStatus.get(ambulance.getStatus()));
        int currentPosition = statusSpinner.getSelectedItemPosition();
        if (currentPosition != position) {

            Log.i(TAG,"Spinner changed from " + currentPosition + " to " + position);

            // set spinner
            setSpinner(position);

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

    public void setSpinner(int position) {

        // temporarily disconnect listener to prevent loop
        Log.i(TAG, "Suppressing listener");
        statusSpinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                /* do nothing */
                Log.i(TAG,"Ignoring change in spinner. Position '" + position + "' selected.");
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {
                Log.i(TAG,"Ignoring change in spinner. Nothing selected.");
            }
        });

        // update spinner
        statusSpinner.setSelection(position, false);

        // connect listener
        // this is tricky, see
        // https://stackoverflow.com/questions/2562248/how-to-keep-onitemselected-from-firing-off-on-a-newly-instantiated-spinner
        statusSpinner.post(new Runnable() {
            public void run() {
                Log.i(TAG, "Restoring listener");
                statusSpinner.setOnItemSelectedListener(AmbulanceFragment.this);
            }
        });

    }

    public void setSwitch(boolean isChecked) {

        // temporarily disconnect listener to prevent loop
        Log.i(TAG, "Suppressing listener");
        startTrackingSwitch.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                /* do nothing */
                Log.i(TAG,"Ignoring change in switch. Checked = '" + isChecked + "' selected.");
            }

        });

        // update spinner
        startTrackingSwitch.setChecked(isChecked);

        // connect listener
        // this is tricky, see
        // https://stackoverflow.com/questions/2562248/how-to-keep-onitemselected-from-firing-off-on-a-newly-instantiated-spinner
        startTrackingSwitch.post(new Runnable() {
            public void run() {
                Log.i(TAG, "Restoring listener");
                startTrackingSwitch.setOnCheckedChangeListener(AmbulanceFragment.this);
            }
        });

    }

    public boolean canWrite() {

        Ambulance ambulance = AmbulanceForegroundService.getAmbulance();

        // has ambulance?
        if (ambulance == null)
            return false;

        // can write?
        final MqttProfileClient profileClient = AmbulanceForegroundService.getProfileClient(getContext());
        boolean canWrite = false;
        for (AmbulancePermission permission : profileClient.getProfile().getAmbulances()) {
            if (permission.getAmbulanceId() == ambulance.getId()) {
                if (permission.isCanWrite()) {
                    canWrite = true;
                }
                break;
            }
        }

        return canWrite;

    }

    public boolean canStreamLocation() {

        Ambulance ambulance = AmbulanceForegroundService.getAmbulance();

        // has ambulance?
        if (ambulance == null)
            return false;

        // is location_client available?
        final MqttProfileClient profileClient = AmbulanceForegroundService.getProfileClient(getContext());
        return (ambulance.getLocationClientId() == null ||
                profileClient.getClientId().equals(ambulance.getLocationClientId()));

    }

    @Override
    public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {

        if (isChecked) {

            Log.d(TAG, "onCheckedChanged: isChecked");

            if (canWrite()) {

                if (canStreamLocation()) {

                    Log.d(TAG, "onCheckedChanged: requesting to stream location");

                    // Let user know
                    Toast.makeText(getContext(), R.string.requestingToStreamLocation, Toast.LENGTH_SHORT).show();

                    // Set requestingLocation to number of attempts
                    requestingToStreamLocation = MAX_NUMBER_OF_LOCATION_REQUESTS_ATTEMPTS;

                    // Set location_client
                    final MqttProfileClient profileClient = AmbulanceForegroundService.getProfileClient(getContext());
                    String payload = String.format("{\"location_client_id\":\"%1$s\"}", profileClient.getClientId());

                    // Update location_client on server, listening to updates already
                    Intent intent = new Intent(getContext(), AmbulanceForegroundService.class);
                    intent.setAction(AmbulanceForegroundService.Actions.UPDATE_AMBULANCE);
                    Bundle bundle = new Bundle();
                    bundle.putString("UPDATE", payload);
                    intent.putExtras(bundle);
                    getActivity().startService(intent);

                    // reset switch
                    startTrackingSwitch.setChecked(false);

                } else {

                    // Ask user to force?
                    if (requestingToStreamLocation < 0) {

                        // Create dialog
                        AlertDialog.Builder alertDialogBuilder = new AlertDialog.Builder(getActivity());

                        alertDialogBuilder.setTitle(R.string.alert_warning_title);
                        alertDialogBuilder.setMessage(R.string.forceLocationUpdates);

                        // Cancel button
                        alertDialogBuilder.setNegativeButton(
                                R.string.alert_button_negative_text,
                                new DialogInterface.OnClickListener() {
                                    @Override
                                    public void onClick(DialogInterface dialog, int which) {

                                        // reset switch
                                        startTrackingSwitch.setChecked(false);

                                    }
                                });

                        // Create the OK button that logs user out
                        alertDialogBuilder.setPositiveButton(
                                R.string.alert_button_positive_text,
                                new DialogInterface.OnClickListener() {
                                    @Override
                                    public void onClick(DialogInterface dialog, int which) {

                                        Log.i(TAG, "ForceLocationUpdatesDialog: OK Button Clicked");

                                        // Reset location_client
                                        final MqttProfileClient profileClient =
                                                AmbulanceForegroundService.getProfileClient(getContext());
                                        String payload = "{\"location_client_id\":\"\"}";

                                        // Update location_client on server, listening to updates already
                                        Intent intent = new Intent(getContext(), AmbulanceForegroundService.class);
                                        intent.setAction(AmbulanceForegroundService.Actions.UPDATE_AMBULANCE);
                                        Bundle bundle = new Bundle();
                                        bundle.putString("UPDATE", payload);
                                        intent.putExtras(bundle);
                                        getActivity().startService(intent);

                                        // Reset request to stream location
                                        requestingToStreamLocation = 0;

                                        // reset switch
                                        startTrackingSwitch.setChecked(false);

                                        // Toast to warn user
                                        Toast.makeText(getContext(), R.string.forcingLocationUpdates,
                                                Toast.LENGTH_LONG).show();


                                    }
                                });

                        alertDialogBuilder.create().show();

                    } else {

                        // Letting know that tried once before
                        requestingToStreamLocation = -1;

                        // reset switch
                        startTrackingSwitch.setChecked(false);

                        // Alert user
                        new AlertSnackbar(getActivity())
                                .alert(getResources().getString(R.string.anotherClientIsStreamingLocations));

                    }

                }

            } else {

                // Toast to warn user
                Toast.makeText(getContext(), R.string.cantModifyAmbulance, Toast.LENGTH_LONG).show();

                // reset switch
                startTrackingSwitch.setChecked(false);

            }

        } else {

            Log.d(TAG, "onCheckedChanged: isNotChecked");

            if (canWrite()) {

                Log.d(TAG, "onCheckedChanged: requesting to stop streaming location");

                // turn off tracking
                Intent intent = new Intent(getContext(), AmbulanceForegroundService.class);
                intent.setAction(AmbulanceForegroundService.Actions.STOP_LOCATION_UPDATES);
                getActivity().startService(intent);

            }

        }

    }

    @Override
    public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {

        Log.i(TAG, "Item '" + position + "' selected.");

        // Should only update on server as a result of user interaction
        // Otherwise this will create a loop with mqtt updating ambulance
        // TODO: Debug spinner multiple updates
        // This may not be easy with the updates being called from a service

        Log.i(TAG, "Processing status spinner update.");

        if (!canWrite()) {

            // Toast to warn user
            Toast.makeText(getContext(), R.string.cantModifyAmbulance, Toast.LENGTH_LONG).show();

            // set spinner
            Ambulance ambulance = AmbulanceForegroundService.getAmbulance();
            if (ambulance != null) {

                int oldPosition = ambulanceStatusList.indexOf(ambulanceStatus.get(ambulance.getStatus()));
                setSpinner(oldPosition);

            } else {
                Log.d(TAG,"Could not retrieve ambulance.");
            }

            // Return
            return;

        }

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
        //       the wrong location on the server
        Intent intent = new Intent(getContext(), AmbulanceForegroundService.class);
        intent.setAction(AmbulanceForegroundService.Actions.UPDATE_AMBULANCE);
        Bundle bundle = new Bundle();
        bundle.putString("UPDATE", updateString);
        intent.putExtras(bundle);
        getActivity().startService(intent);

    }

    @Override
    public void onNothingSelected(AdapterView<?> parent) {
        Log.i(TAG, "Nothing selected: this should never happen.");
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