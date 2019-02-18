package org.emstrack.ambulance.fragments;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v4.content.LocalBroadcastManager;
import android.support.v7.app.AlertDialog;
import android.util.Log;
import android.util.Pair;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import org.emstrack.ambulance.MainActivity;
import org.emstrack.ambulance.R;
import org.emstrack.ambulance.models.AmbulanceAppData;
import org.emstrack.ambulance.services.AmbulanceForegroundService;
import org.emstrack.models.Ambulance;
import org.emstrack.models.AmbulanceCall;
import org.emstrack.models.Call;
import org.emstrack.models.CallStack;
import org.emstrack.models.Location;
import org.emstrack.models.Patient;
import org.emstrack.models.Settings;
import org.emstrack.models.Waypoint;

import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;

public class AmbulanceFragment extends Fragment {

    private static final String TAG = AmbulanceFragment.class.getSimpleName();

    private static DecimalFormat df = new DecimalFormat();

    private View view;

    private Button statusButton;

    private TextView capabilityText;
    private TextView commentText;
    private TextView updatedOnText;

    private AmbulancesUpdateBroadcastReceiver receiver;

    private LinearLayout callLayout;
    private TextView callDescriptionTextView;
    private Button callPriorityButton;
    private TextView callAddressTextView;
    private Button callEndButton;
    private TextView callPatientsTextView;
    private TextView callDistanceTextView;
    private Button callAddWaypointButton;
    private TextView callNextWaypointTypeTextView;
    private TextView callNumberWayointsView;

    private RelativeLayout callInformationLayout;
    private TextView callInformationText;

    private LinearLayout callResumeLayout;
    private Spinner callResumeSpinner;
    private Button callResumeButton;

    private View callSkipLayout;
    private Button callSkipWaypoinButton;
    private Button callVisitingWaypointButton;

    private Map<String,String> ambulanceStatus;
    private List<String> ambulanceStatusList;
    private ArrayList<Integer> ambulanceStatusBackgroundColorList;
    private ArrayList<Integer> ambulanceStatusTextColorList;
    private Map<String,String> ambulanceCapabilities;
    private List<String> ambulanceCapabilityList;

    private int currentCallId;
    private View callNextWaypointLayout;
    private StatusButtonClickListener statusButtonClickListerner;

    public class AmbulancesUpdateBroadcastReceiver extends BroadcastReceiver {

        @Override
        public void onReceive(Context context, Intent intent ) {
            if (intent != null) {

                // Get app data
                AmbulanceAppData appData = AmbulanceForegroundService.getAppData();

                // Get calls
                CallStack calls = appData.getCalls();

                final String action = intent.getAction();
                switch (action) {
                    case AmbulanceForegroundService.BroadcastActions.AMBULANCE_UPDATE:

                        Log.i(TAG, "AMBULANCE_UPDATE");
                        updateAmbulance(appData.getAmbulance());

                        break;
                    case AmbulanceForegroundService.BroadcastActions.CALL_UPDATE:

                        Log.i(TAG, "CALL_UPDATE");
                        if (currentCallId > 0)
                            updateCall(appData.getAmbulance(), calls.getCurrentCall());

                        break;
                    case AmbulanceForegroundService.BroadcastActions.CALL_ACCEPTED: {

                        Log.i(TAG, "CALL_ACCEPTED");

                        // Toast to warn user
                        Toast.makeText(getContext(), R.string.CallStarted, Toast.LENGTH_LONG).show();

                        int callId = intent.getIntExtra(AmbulanceForegroundService.BroadcastExtras.CALL_ID, -1);
                        currentCallId = -1;
                        updateCall(appData.getAmbulance(), calls.getCurrentCall());

                        break;
                    }
                    case AmbulanceForegroundService.BroadcastActions.CALL_COMPLETED: {

                        Log.i(TAG, "CALL_COMPLETED");

                        // Toast to warn user
                        Toast.makeText(getContext(), R.string.CallFinished, Toast.LENGTH_LONG).show();

                        int callId = intent.getIntExtra(AmbulanceForegroundService.BroadcastExtras.CALL_ID, -1);
                        Ambulance ambulance = appData.getAmbulance();
                        if (currentCallId == callId)
                            updateCall(ambulance, null);
                        else
                            /* makes sure that requested and suspended get updated */
                            updateAmbulance(ambulance);

                        break;
                    }
                }

            }
        }
    }

    public class StatusButtonClickListener implements View.OnClickListener {

        private boolean enabled;
        ArrayAdapter<String> ambulanceListAdapter;

        public StatusButtonClickListener() {
            this.enabled = true;

            // Create the adapter
            this.ambulanceListAdapter =
                    new ArrayAdapter<>(AmbulanceFragment.this.getContext(),
                            android.R.layout.simple_spinner_dropdown_item,
                            ambulanceStatusList);
            ambulanceListAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);

        }

        public void setEnabled(boolean enabled) {
            this.enabled = enabled;
        }

        public boolean isEnabled() {
            return enabled;
        }

        @Override
        public void onClick(View v) {

            // short return if disabled
            if (!enabled)
                return;

            new AlertDialog.Builder(
                    getActivity())
                    .setTitle(R.string.selectAmbulanceStatus)
                    .setAdapter(ambulanceListAdapter,
                            (dialog, which) -> {

                                // Get selected status
                                Log.i(TAG, "Status at position '" + which + "' selected.");

                                // Update ambulance status
                                updateAmbulanceStatus(which);

                            })
                    .create()
                    .show();
        }
    }

    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {

        Log.d(TAG, "onCreateView");

        // set formatter
        df.setMaximumFractionDigits(3);

        // inflate view
        view = inflater.inflate(R.layout.fragment_ambulance, container, false);

        // retrieveObject callInformationLayout
        callInformationLayout = view.findViewById(R.id.callInformationLayout);

        // Retrieve callInformationLayout parts
        callInformationText = callInformationLayout.findViewById(R.id.callInformationText);

        // retrieveObject callResumeLayout
        callResumeLayout = view.findViewById(R.id.callResumeLayout);

        // Retrieve callResumeLayout parts
        callResumeSpinner = callResumeLayout.findViewById(R.id.callResumeSpinner);
        callResumeButton= callResumeLayout.findViewById(R.id.callResumeButton);
        
        // setup callLayout
        callLayout = view.findViewById(R.id.callLayout);

        // Retrieve callLayout parts
        callPriorityButton = callLayout.findViewById(R.id.callPriorityButton);
        callDescriptionTextView = callLayout.findViewById(R.id.callDetailsText);
        callPatientsTextView = callLayout.findViewById(R.id.callPatientsText);
        callNumberWayointsView = callLayout.findViewById(R.id.callNumberWaypointsText);

        callEndButton = callLayout.findViewById(R.id.callEndButton);
        callAddWaypointButton = callLayout.findViewById(R.id.callAddWaypointButton);

        // setup callNextWaypointLayout
        callNextWaypointLayout = callLayout.findViewById(R.id.callNextWaypointLayout);

        // Retrieve callNextWaypointLayout parts
        callDistanceTextView = callLayout.findViewById(R.id.callDistanceText);
        callNextWaypointTypeTextView = callLayout.findViewById(R.id.callWaypointTypeText);
        callAddressTextView = callLayout.findViewById(R.id.callAddressText);

        // setup callSkipLayout
        callSkipLayout = callLayout.findViewById(R.id.callSkipLayout);

        callSkipWaypoinButton = callSkipLayout.findViewById(R.id.callSkipWaypointButton);
        callVisitingWaypointButton = callSkipLayout.findViewById(R.id.callVisitingWaypoingButton);

        AmbulanceAppData appData = AmbulanceForegroundService.getAppData();
        Settings settings = appData.getSettings();
        if (settings != null) {

            // Get settings, status and capabilities
            ambulanceStatus = settings.getAmbulanceStatus();
            ambulanceStatusList = new ArrayList<>();
            for (String status : settings.getAmbulanceStatusOrder())
                ambulanceStatusList.add(ambulanceStatus.get(status));
            // Collections.sort(ambulanceStatusList);

            ambulanceStatusBackgroundColorList = new ArrayList<>();
            ambulanceStatusTextColorList = new ArrayList<>();
            for (String value : ambulanceStatusList)
                for (Map.Entry<String,String> entry : ambulanceStatus.entrySet())
                    if (value.equals(entry.getValue())) {
                        ambulanceStatusBackgroundColorList
                                .add(getResources()
                                        .getColor(Ambulance
                                                .statusBackgroundColorMap.get(entry.getKey())));
                        ambulanceStatusTextColorList
                                .add(getResources()
                                        .getColor(Ambulance
                                                .statusTextColorMap.get(entry.getKey())));
                    }

            ambulanceCapabilities = settings.getAmbulanceCapability();
            ambulanceCapabilityList = new ArrayList<>();
            for (String status : settings.getAmbulanceCapabilityOrder())
                ambulanceCapabilityList.add(ambulanceCapabilities.get(status));
            // Collections.sort(ambulanceCapabilityList);

        } else {
            
            ambulanceStatusList = new ArrayList<>();
            ambulanceCapabilityList = new ArrayList<>();
        }

        // Other text
        capabilityText = view.findViewById(R.id.capabilityText);
        commentText = view.findViewById(R.id.commentText);
        updatedOnText = view.findViewById(R.id.updatedOnText);

        // Set status button
        statusButton = view.findViewById(R.id.statusButton);

        // Set the ambulance button's adapter
        statusButtonClickListerner = new StatusButtonClickListener();
        statusButton.setOnClickListener(statusButtonClickListerner);

        // Update ambulance
        Ambulance ambulance = appData.getAmbulance();
        if (ambulance != null)
            updateAmbulance(ambulance);
        else
            callLayout.setVisibility(View.GONE);

        // Is there a current call?
        currentCallId = -1;
        Call call = appData.getCalls().getCurrentCall();
        if (call != null) {
            Log.d(TAG, String.format("Is currently handling call '%1$d'", call.getId()));
            updateCall(ambulance, call);
        } else {
            Log.d(TAG, "Is currently not handling any call");
            callResumeLayout.setVisibility(View.GONE);
        }

        return view;
    }

    @Override
    public void onResume() {
        super.onResume();

        Log.d(TAG, "onResume");

        // Set auxiliary panels gone
        callLayout.setVisibility(View.GONE);
        callResumeLayout.setVisibility(View.GONE);

        // Get app data
        AmbulanceAppData appData = AmbulanceForegroundService.getAppData();

        // Update ambulance
        Ambulance ambulance = appData.getAmbulance();
        if (ambulance != null)
            updateAmbulance(ambulance);

        // Are there any call been currently handled?
        currentCallId = -1;
        Call call = appData.getCalls().getCurrentCall();
        if (call != null) {
            Log.d(TAG, String.format("Is currently handling call '%1$d'", call.getId()));
            updateCall(ambulance, call);
        }

        // Register receiver
        IntentFilter filter = new IntentFilter();
        filter.addAction(AmbulanceForegroundService.BroadcastActions.AMBULANCE_UPDATE);
        filter.addAction(AmbulanceForegroundService.BroadcastActions.CALL_UPDATE);
        filter.addAction(AmbulanceForegroundService.BroadcastActions.CALL_ACCEPTED);
        filter.addAction(AmbulanceForegroundService.BroadcastActions.CALL_COMPLETED);
        filter.addAction(AmbulanceForegroundService.BroadcastActions.CALL_DECLINED);
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

    public void updateCall(final Ambulance ambulance, final Call call) {

        if ((call == null ^ currentCallId <= 0)) {

            Log.d(TAG, "Call changed, will initialize layout");

            // create new layout
            if (call == null) {

                Log.d(TAG, "NO CALL Layout");

                callInformationLayout.setVisibility(View.VISIBLE);
                callLayout.setVisibility(View.GONE);
                currentCallId = -1;

                // update ambulance to show resume panel
                updateAmbulance(ambulance);

                statusButtonClickListerner.setEnabled(true);

            } else {

                Log.d(TAG, "CALL Layout");

                callInformationLayout.setVisibility(View.GONE);
                callResumeLayout.setVisibility(View.GONE);
                callLayout.setVisibility(View.VISIBLE);
                currentCallId = call.getId();

                callEndButton.setOnClickListener(
                        v -> {
                            // Prompt end of call
                            ((MainActivity) getActivity()).promptEndCallDialog(call.getId());
                        }
                );

                callAddWaypointButton.setOnClickListener(
                        v -> {
                            // Prompt add new waypoint
                            ((MainActivity) getActivity()).promptNextWaypointDialog(call.getId());
                        }
                );

                statusButtonClickListerner.setEnabled(false);

            }

        }

        // Update call content
        if (call != null) {

            Log.d(TAG, "Creating call layout");

            // retrieveObject ambulanceCall
            AmbulanceCall ambulanceCall = call.getCurrentAmbulanceCall();
            if (ambulanceCall == null)
                Log.d(TAG, "Call does not have a current ambulance!");

            callPriorityButton.setText(call.getPriority());
            callPriorityButton.setBackgroundColor(
                    ((MainActivity) getActivity())
                            .getCallPriorityBackgroundColors()
                            .get(call.getPriority()));
            callPriorityButton.setTextColor(
                    ((MainActivity) getActivity())
                            .getCallPriorityForegroundColors()
                            .get(call.getPriority()));

            ((TextView) view.findViewById(R.id.callPriorityLabel)).setText(R.string.currentCall);

            callDescriptionTextView.setText(call.getDetails());

            // patients
            List<Patient> patients = call.getPatientSet();
            if (patients != null && patients.size() > 0) {
                String text = "";
                for (Patient patient : patients) {
                    if (!text.isEmpty())
                        text += ", ";
                    text += patient.getName();
                    if (patient.getAge() != null)
                        text += " (" + patient.getAge() + ")";
                }

                callPatientsTextView.setText(text);
            } else
                callPatientsTextView.setText(R.string.noPatientAvailable);

            int numberOfWaypoints =
                    (ambulanceCall == null ? 0 : ambulanceCall.getWaypointSet().size());
            callNumberWayointsView.setText(String.valueOf(numberOfWaypoints));

            final Waypoint waypoint =
                    (ambulanceCall != null
                            ? ambulanceCall.getNextWaypoint()
                            : null);

            if (waypoint != null) {

                Log.d(TAG, "Setting up next waypoint");

                // Get Location
                Location location = waypoint.getLocation();

                // Update waypoint type
                AmbulanceAppData appData = AmbulanceForegroundService.getAppData();
                callNextWaypointTypeTextView.setText(
                        appData.getSettings()
                                .getLocationType()
                                .get(location.getType()));

                // Update address
                callAddressTextView.setText(location.toString());

                // Update call distance to next waypoint
                callDistanceTextView.setText(updateCallDistance(location));

                // Setup visiting button text
                String visitingWaypointText; // = "Mark as ";
                if (waypoint.isCreated()) {
                    // visitingWaypointText += Waypoint.statusLabel.get(Waypoint.STATUS_VISITING);
                    visitingWaypointText = getString(R.string.markAsVisiting);
                    callVisitingWaypointButton.setBackgroundColor(getResources().getColor(R.color.bootstrapWarning));
                    callVisitingWaypointButton.setTextColor(getResources().getColor(R.color.bootstrapDark));
                } else { // if (waypoint.isVisting())
                    // visitingWaypointText += Waypoint.statusLabel.get(Waypoint.STATUS_VISITED);
                    visitingWaypointText = getString(R.string.markAsVisited);
                    callVisitingWaypointButton.setBackgroundColor(getResources().getColor(R.color.bootstrapInfo));
                    callVisitingWaypointButton.setTextColor(getResources().getColor(R.color.bootstrapLight));
                }
                callVisitingWaypointButton.setText(visitingWaypointText);

                // Make callNextWaypointLayout visible
                callNextWaypointLayout.setVisibility(View.VISIBLE);

                // Make callSkipLayout visible
                callSkipLayout.setVisibility(View.VISIBLE);

                // Setup skip buttons
                callSkipWaypoinButton.setOnClickListener(
                        v -> promptSkipVisitingOrVisited(Waypoint.STATUS_SKIPPED,
                                waypoint.getId(), call.getId(), ambulance.getId(),
                                getString(R.string.pleaseConfirm),
                                getString(R.string.skipCurrentWaypoint),
                                getString(R.string.skippingWaypoint))
                );

                callVisitingWaypointButton.setOnClickListener(
                        v -> {

                            if (waypoint.isCreated())

                                promptSkipVisitingOrVisited(Waypoint.STATUS_VISITING,
                                        waypoint.getId(), call.getId(), ambulance.getId(),
                                        getString(R.string.pleaseConfirm),
                                        getString(R.string.visitCurrentWaypoint),
                                        getString(R.string.visitingWaypoint));
                            else

                                promptSkipVisitingOrVisited(Waypoint.STATUS_VISITED,
                                        waypoint.getId(), call.getId(), ambulance.getId(),
                                        getString(R.string.pleaseConfirm),
                                        getString(R.string.visitedCurrentWaypoint),
                                        getString(R.string.visitedWaypoint));

                        }
                );

            } else {

                Log.d(TAG, "Call does not have a next waypoint!");

                callNextWaypointTypeTextView.setText(R.string.nextWaypointHasntBeenSetYet);
                callAddressTextView.setText("---");
                callDistanceTextView.setText("---");

                // Make callNextWaypointLayout invisible
                callNextWaypointLayout.setVisibility(View.GONE);

                // Make callSkipLayout invisible
                callSkipLayout.setVisibility(View.GONE);

            }

        } else
            // update ambulance to set suspended/requested count correct
            updateAmbulance(ambulance);

    }

    public void updateAmbulance(Ambulance ambulance) {

        // set status button
        setStatusButton(ambulanceStatusList.indexOf(ambulanceStatus.get(ambulance.getStatus())));

        // set identifier
        ((MainActivity) getActivity()).setAmbulanceButtonText(ambulance.getIdentifier());

        Log.d(TAG,"Summarizing pending call_current");

        // get calls
        CallStack calls = AmbulanceForegroundService.getAppData().getCalls();

        // Set call_current info
        AmbulanceAppData appData = AmbulanceForegroundService.getAppData();
        Map<String, Integer> callSummary
                = calls.summary(appData.getSettings().getAmbulancecallStatus().keySet(),
                                ambulance.getId());
        Log.d(TAG, "Call summary = " + callSummary.toString());
        final String summaryText = String.format(getString(R.string.requestedSuspended),
                callSummary.get(AmbulanceCall.STATUS_REQUESTED),
                callSummary.get(AmbulanceCall.STATUS_SUSPENDED));
        callInformationText.setText(summaryText);

        // deploy resume panel
        if (currentCallId < 0 &&
                (callSummary.get(AmbulanceCall.STATUS_REQUESTED) +
                        callSummary.get(AmbulanceCall.STATUS_SUSPENDED)) > 0) {

            Log.d(TAG,"Will add resume call view");

            // List of call_current
            ArrayList<String> pendingCallList = new ArrayList<>();

            // Create lists of suspended call_current
            final ArrayList<Pair<Call,AmbulanceCall>> suspendedCallList = new ArrayList<>();
            if (callSummary.get(AmbulanceCall.STATUS_SUSPENDED) > 0) {
                for (Map.Entry<Integer, Pair<Call,AmbulanceCall>> ambulanceCallEntry : calls
                        .filter(ambulance.getId(),
                                AmbulanceCall.STATUS_SUSPENDED)
                        .entrySet()) {
                    Pair<Call,AmbulanceCall> ambulanceCallPair = ambulanceCallEntry.getValue();
                    suspendedCallList.add(ambulanceCallPair);
                    pendingCallList.add("(S) " + ambulanceCallPair.second.getUpdatedOn());
                }
            }

            // Create lists of requested call_current
            final ArrayList<Pair<Call,AmbulanceCall>> requestedCallList = new ArrayList<>();
            if (callSummary.get(AmbulanceCall.STATUS_REQUESTED) > 0) {
                for (Map.Entry<Integer, Pair<Call,AmbulanceCall>> ambulanceCallEntry : calls
                        .filter(ambulance.getId(),
                                AmbulanceCall.STATUS_REQUESTED)
                        .entrySet()) {
                    Pair<Call,AmbulanceCall> ambulanceCall = ambulanceCallEntry.getValue();
                    requestedCallList.add(ambulanceCall);
                    pendingCallList.add("(R) " + ambulanceCall.second.getUpdatedOn());
                }
            }

            // Create the spinner adapter
            ArrayAdapter<String> pendingCallListAdapter = new ArrayAdapter<>(getContext(),
                    android.R.layout.simple_spinner_dropdown_item, pendingCallList);
            pendingCallListAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);

            // set adapter
            callResumeSpinner.setAdapter(pendingCallListAdapter);

            //final Button callResumeButton= child.findViewById(R.id.callResumeButton);
            callResumeButton.setOnClickListener(
                    v -> {

                        // retrieve spinner selection
                        int position = callResumeSpinner.getSelectedItemPosition();

                        // retrieve corresponding call
                        Call call = (position < suspendedCallList.size() ?
                                suspendedCallList.get(position).first :
                                requestedCallList.get(position - suspendedCallList.size()).first);

                        // prompt user
                        Log.d(TAG,"Will prompt user to accept call");
                        ((MainActivity) getActivity()).promptCallAccept(call.getId());

                    });

            callResumeLayout.setVisibility(View.VISIBLE);

        } else {
            callResumeLayout.setVisibility(View.GONE);
        }

        // update call distance?
        if (currentCallId > 0) {

            Call call = AmbulanceForegroundService.getAppData().getCalls().getCurrentCall();
            if (call != null) {

                AmbulanceCall ambulanceCall = call.getCurrentAmbulanceCall();

                Waypoint waypoint = ambulanceCall.getNextWaypoint();
                if (waypoint != null) {

                    String distanceText = updateCallDistance(waypoint.getLocation());
                    callDistanceTextView.setText(distanceText);
                } else
                    callDistanceTextView.setText("---");

            } else
                callDistanceTextView.setText("---");

        }

        // set status and comment
        commentText.setText(ambulance.getComment());
        updatedOnText.setText(ambulance.getUpdatedOn().toString());

        // set capability
        capabilityText.setText(ambulanceCapabilities.get(ambulance.getCapability()));

    }

    public String updateCallDistance(Location location) {

        if (location == null)
            return null;

        Log.d(TAG,"Will calculate distance");

        // Get current location
        android.location.Location lastLocation = AmbulanceForegroundService.getLastLocation();

        // Calculate distance to patient
        float distance = -1;
        if (lastLocation != null) {
            Log.d(TAG,"last location = " + lastLocation);
            distance = lastLocation.distanceTo(location.getLocation().toLocation()) / 1000;
        }
        String distanceText = getString(R.string.noDistanceAvailable);
        Log.d(TAG,"Distance = " + distance);
        if (distance > 0) {
            distanceText = df.format(distance) + " km";
        }

        return distanceText;

    }

    public void setStatusButton(int position) {

        // set status button
        statusButton.setText(ambulanceStatusList.get(position));
        statusButton.setTextColor(ambulanceStatusTextColorList.get(position));
        statusButton.setBackgroundColor(ambulanceStatusBackgroundColorList.get(position));

    }

    public void updateAmbulanceStatus(int position) {

        try {

            if (!((MainActivity) getActivity()).canWrite()) {

                // Toast to warn user
                Toast.makeText(getContext(),
                        R.string.cantModifyAmbulance,
                        Toast.LENGTH_LONG).show();

                // Return
                return;

            }

            // Get selected status
            String status = ambulanceStatusList.get(position);

            // Search for entry in ambulanceStatus map
            String statusCode = "";
            for (Map.Entry<String, String> entry : ambulanceStatus.entrySet()) {
                if (status.equals(entry.getValue())) {
                    statusCode = entry.getKey();
                    break;
                }
            }

            // format timestamp
            // DateFormat df = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'");
            // df.setTimeZone(TimeZone.getTimeZone("UTC"));
            // String timestamp = df.format(new Date());

            // Set updateAmbulance string
            // String updateString = "{\"status\":\"" + statusCode + "\",\"timestamp\":\"" + timestamp + "\"}";

            // Update on server
            // TODO: Update along with locations because it will be recorded with
            //       the wrong location on the server
            Ambulance ambulance = AmbulanceForegroundService.getAppData().getAmbulance();
            Intent intent = new Intent(getContext(), AmbulanceForegroundService.class);
            intent.setAction(AmbulanceForegroundService.Actions.UPDATE_AMBULANCE_STATUS);
            Bundle bundle = new Bundle();
            bundle.putInt(AmbulanceForegroundService.BroadcastExtras.AMBULANCE_ID, ambulance.getId());
            bundle.putString(AmbulanceForegroundService.BroadcastExtras.AMBULANCE_STATUS, statusCode);
            intent.putExtras(bundle);
            getActivity().startService(intent);

        } catch (Exception e) {

            Log.i(TAG, "updateAmbulanceStatus exception: " + e);

        }

    }

    public void promptSkipVisitingOrVisited(final String status,
                                            final int waypointId, final int callId, final int ambulanceId,
                                            final String title, final String message, final String doneMessage) {

        Log.d(TAG, "Creating promptSkipVisitingOrVisited dialog");

        // Use the Builder class for convenient dialog construction
        AlertDialog.Builder builder = new AlertDialog.Builder(getContext());
        builder.setTitle(title)
                .setMessage(message)
                .setNegativeButton(R.string.no,
                        (dialog, id) -> Log.i(TAG, "Continuing..."))
                .setPositiveButton(R.string.yes,
                        (dialog, id) -> {

                            Log.i(TAG, String.format("Will mark as '%1$s'", status));

                            Toast.makeText(getContext(), doneMessage, Toast.LENGTH_SHORT).show();

                            String action;
                            if (status == Waypoint.STATUS_SKIPPED)
                                action = AmbulanceForegroundService.Actions.WAYPOINT_SKIP;
                            else if (status == Waypoint.STATUS_VISITING)
                                action = AmbulanceForegroundService.Actions.WAYPOINT_ENTER;
                            else // if (status == Waypoint.STATUS_VISITED)
                                action = AmbulanceForegroundService.Actions.WAYPOINT_EXIT;

                            // update waypoint status on server
                            Intent intent = new Intent(getContext(),
                                    AmbulanceForegroundService.class);
                            intent.setAction(action);
                            Bundle bundle = new Bundle();
                            bundle.putInt(AmbulanceForegroundService.BroadcastExtras.WAYPOINT_ID, waypointId);
                            bundle.putInt(AmbulanceForegroundService.BroadcastExtras.AMBULANCE_ID, ambulanceId);
                            bundle.putInt(AmbulanceForegroundService.BroadcastExtras.CALL_ID, callId);
                            intent.putExtras(bundle);
                            getActivity().startService(intent);

                        });

        // Create the AlertDialog object and return it
        builder.create().show();
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