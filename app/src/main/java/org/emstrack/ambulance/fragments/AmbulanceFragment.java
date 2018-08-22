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
import android.widget.Spinner;
import android.widget.Switch;
import android.widget.TextView;
import android.widget.Toast;

import org.emstrack.ambulance.services.AmbulanceForegroundService;
import org.emstrack.ambulance.MainActivity;
import org.emstrack.ambulance.R;
import org.emstrack.models.Ambulance;
import org.emstrack.mqtt.MqttProfileClient;

import java.io.StringBufferInputStream;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.TimeZone;

public class AmbulanceFragment extends Fragment implements AdapterView.OnItemSelectedListener {

    private static final String TAG = AmbulanceFragment.class.getSimpleName();;

    private View view;

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
    private ArrayList<Integer> ambulanceStatusColorList;
    private Map<String,String> ambulanceCapabilities;
    private List<String> ambulanceCapabilityList;

    AmbulancesUpdateBroadcastReceiver receiver;
    private int requestingToStreamLocation;
    private final int MAX_NUMBER_OF_LOCATION_REQUESTS_ATTEMPTS = 2;

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

        try {

            // Get settings, status and capabilities
            final MqttProfileClient profileClient = AmbulanceForegroundService.getProfileClient();

            ambulanceStatus = profileClient.getSettings().getAmbulanceStatus();
            ambulanceStatusList = new ArrayList<String>(ambulanceStatus.values());
            Collections.sort(ambulanceStatusList);

            int colors [] = getResources().getIntArray(R.array.statusColors);
            ambulanceStatusColorList = new ArrayList<Integer>();
            for (int i = 0; i < ambulanceStatusList.size(); i++) {
                ambulanceStatusColorList.add(colors[i]);
            }

            ambulanceCapabilities = profileClient.getSettings().getAmbulanceCapability();
            ambulanceCapabilityList = new ArrayList<String>(ambulanceCapabilities.values());
            Collections.sort(ambulanceCapabilityList);

        } catch (AmbulanceForegroundService.ProfileClientException e) {

            ambulanceStatusList = new ArrayList<String>();

        }

        // Retrieve location
        latitudeText = (TextView) view.findViewById(R.id.latitudeText);
        longitudeText = (TextView) view.findViewById(R.id.longitudeText);
        timestampText = (TextView) view.findViewById(R.id.timestampText);
        orientationText = (TextView) view.findViewById(R.id.orientationText);

        // Other text
        capabilityText = (TextView) view.findViewById(R.id.capabilityText);
        commentText = (TextView) view.findViewById(R.id.commentText);
        updatedOnText = (TextView) view.findViewById(R.id.updatedOnText);

        // Set status spinner
        statusSpinner = (Spinner) view.findViewById(R.id.statusSpinner);
        // Create an ArrayAdapter using the string array and a default spinner layout
        ArrayAdapter<String> statusSpinnerAdapter =
                new ArrayAdapter<String>(getContext(),
                        R.layout.status_spinner_item,
                        ambulanceStatusList) {

                    @Override
                    public View getView(int pos, View view, ViewGroup parent)
                    {
                        Context context = AmbulanceFragment.this.getContext();
                        LayoutInflater inflater = LayoutInflater.from(context);
                        view = inflater.inflate(R.layout.status_spinner_dropdown_item, null);
                        TextView textView = (TextView) view.findViewById(R.id.statusSpinnerDropdownItemText);
                        textView.setBackgroundColor(ambulanceStatusColorList.get(pos));
                        textView.setTextSize(context.getResources().getDimension(R.dimen.statusTextSize));
                        textView.setText(ambulanceStatusList.get(pos));
                        return view;
                    }

                    @Override
                    public View getDropDownView(int pos, View view, ViewGroup parent) {
                        return getView(pos, view, parent);
                    }

                };
        //statusSpinnerAdapter.setDropDownViewResource(R.layout.status_spinner_dropdown_item);
        statusSpinner.setAdapter(statusSpinnerAdapter);

        // Update ambulance
        Ambulance ambulance = AmbulanceForegroundService.getAmbulance();
        if (ambulance != null) {

            update(ambulance);

            // TODO: REMOVE, JUST FOR TESTING
            // Add geofence
//            Log.i(TAG, "Adding geofence");
//            Intent serviceIntent = new Intent(getActivity(),
//                    AmbulanceForegroundService.class);
//            serviceIntent.setAction(AmbulanceForegroundService.Actions.GEOFENCE_START);
//            serviceIntent.putExtra("LATITUDE", (float) 32.881150);
//            serviceIntent.putExtra("LONGITUDE", (float) -117.238200);
//            serviceIntent.putExtra("RADIUS", 50.f);
//            getActivity().startService(serviceIntent);

        }

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

    @Override
    public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {

        Log.i(TAG, "Item '" + position + "' selected.");

        // Should only update on server as a result of user interaction
        // Otherwise this will create a loop with mqtt updating ambulance
        // TODO: Debug spinner multiple updates
        // This may not be easy with the updates being called from a service

        Log.i(TAG, "Processing status spinner update.");

        if (!((MainActivity) getActivity()).canWrite()) {

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