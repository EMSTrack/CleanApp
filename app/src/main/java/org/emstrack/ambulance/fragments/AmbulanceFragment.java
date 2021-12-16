package org.emstrack.ambulance.fragments;

import android.Manifest;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.fragment.app.Fragment;
import androidx.localbroadcastmanager.content.LocalBroadcastManager;
import androidx.appcompat.app.AlertDialog;
import androidx.preference.Preference;
import androidx.preference.PreferenceManager;

import android.util.Log;
import android.util.Pair;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.material.button.MaterialButton;

import org.emstrack.ambulance.BuildConfig;
import org.emstrack.ambulance.MainActivity;
import org.emstrack.ambulance.R;
import org.emstrack.ambulance.dialogs.AlertSnackbar;
import org.emstrack.ambulance.models.AmbulanceAppData;
import org.emstrack.ambulance.services.AmbulanceForegroundService;
import org.emstrack.ambulance.util.RequestPermissionHelper;
import org.emstrack.models.Ambulance;
import org.emstrack.models.AmbulanceCall;
import org.emstrack.models.AmbulancePermission;
import org.emstrack.models.Call;
import org.emstrack.models.CallNote;
import org.emstrack.models.CallStack;
import org.emstrack.models.Location;
import org.emstrack.models.Patient;
import org.emstrack.models.PriorityCode;
import org.emstrack.models.Profile;
import org.emstrack.models.RadioCode;
import org.emstrack.models.Settings;
import org.emstrack.models.Waypoint;
import org.emstrack.models.util.BroadcastActions;
import org.emstrack.models.util.OnServiceComplete;

import java.net.URLEncoder;
import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

public class AmbulanceFragment extends Fragment {

    private static final String TAG = AmbulanceFragment.class.getSimpleName();

    private static final DecimalFormat df = new DecimalFormat();

    private View view;

    private MaterialButton statusButton;
    private View releaseButton;

    private TextView capabilityText;
    private TextView callNotesText;
    private TextView updatedOnText;
    private TextView commentText;

    private AmbulancesUpdateBroadcastReceiver receiver;

    private View ambulanceFragmentLayout;

    private LinearLayout callLayout;
    private TextView callDescriptionTextView;
    private TextView callRadioCodeTextView;
    private TextView callPriorityPrefixTextView;
    private TextView callPriorityTextView;
    private TextView callPrioritySuffixTextView;
    private TextView callAddressTextView;
    private Button callEndButton;
    private TextView callPatientsTextView;
    private TextView callDistanceTextView;
    private Button callAddWaypointButton;
    private TextView callNextWaypointTypeTextView;
    private TextView callNumberWaypointsView;
    private ImageButton toMapsButton;

    private RelativeLayout callInformationLayout;
    private TextView callInformationText;

    private LinearLayout callResumeLayout;
    private Spinner callResumeSpinner;
    private Button callResumeButton;

    private View callSkipLayout;
    private Button callSkipWaypointButton;
    private Button callVisitingWaypointButton;

    private Button addCallNoteButton;

    private Map<String,String> ambulanceStatus;
    private List<String> ambulanceStatusList;
    private ArrayList<Integer> ambulanceStatusBackgroundColorList;
    private ArrayList<Integer> ambulanceStatusTextColorList;
    private Map<String,String> ambulanceCapabilities;
    private List<String> ambulanceCapabilityList;

    private int currentCallId;
    private View callNextWaypointLayout;
    private StatusButtonClickListener statusButtonClickListener;

    private Button ambulanceSelectionButton;
    private TextView ambulanceSelectionMessage;

    private ArrayList<String> ambulanceList;
    private List<AmbulancePermission> ambulancePermissions;
    private MainActivity activity;

    public class AmbulancesUpdateBroadcastReceiver extends BroadcastReceiver {

        @Override
        public void onReceive(Context context, Intent intent ) {
            if (intent != null) {

                // Get app data
                AmbulanceAppData appData = AmbulanceForegroundService.getAppData();

                // Get calls
                CallStack calls = appData.getCalls();

                final String action = intent.getAction();
                if (action != null) {
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
                        default: {
                            Log.i(TAG, "Unknown broadcast action");
                        }
                    }
                } else {
                    Log.i(TAG, "Action is null");
                }

            }
        }
    }

    private class CheckPermissionsClickListener implements View.OnClickListener {

        private final ActivityResultLauncher<String[]> activityResultLauncher;
        private String[] permissions;
        private final ArrayAdapter<String> ambulanceListAdapter;
        private boolean promptIfNotGranted;

        public CheckPermissionsClickListener() {
            // Build permissions
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                Log.i(TAG, "Permissions version >= R");
                if (RequestPermissionHelper.checkPermissions(requireContext(), new String[] {Manifest.permission.ACCESS_COARSE_LOCATION})
                        || RequestPermissionHelper.checkPermissions(requireContext(), new String[] {Manifest.permission.ACCESS_FINE_LOCATION})) {
                    // has coarse or fine location, ask for all
                    Log.i(TAG, "Will ask for BACKGROUND LOCATION first");
                    this.permissions = new String[]{
                            Manifest.permission.ACCESS_COARSE_LOCATION,
                            Manifest.permission.ACCESS_FINE_LOCATION,
                            Manifest.permission.ACCESS_BACKGROUND_LOCATION
                    };
                } else {
                    Log.i(TAG, "Will ask for FOREGROUND LOCATION");
                    // does not have foreground location, start with foreground first
                    this.permissions = new String[]{
                            Manifest.permission.ACCESS_COARSE_LOCATION,
                            Manifest.permission.ACCESS_FINE_LOCATION
                    };
                }
            } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                Log.i(TAG, "Permissions version >= Q");
                this.permissions = new String[]{
                        Manifest.permission.ACCESS_FINE_LOCATION,
                        Manifest.permission.ACCESS_BACKGROUND_LOCATION
                };
            } else {
                Log.i(TAG, "Permissions version < Q");
                this.permissions = new String[]{
                        Manifest.permission.ACCESS_FINE_LOCATION
                };
            }

            // register laucher
            activityResultLauncher =
                    registerForActivityResult(new ActivityResultContracts.RequestMultiplePermissions(),
                        isGrantedMap -> {

                            Log.i(TAG, "Permissions results:");
                            Log.i(TAG, isGrantedMap.toString());

                            // check all permissions
                            boolean granted = true;
                            for (String permission: this.permissions) {
                                //noinspection ConstantConditions
                                if (isGrantedMap.containsKey(permission) && isGrantedMap.get(permission)) {
                                    continue;
                                }
                                granted = false;
                                break;
                            }
                            this.action(granted);
                        }
                );

            // Create the ambulance list adapter
            this.ambulanceListAdapter =
                    new ArrayAdapter<>(AmbulanceFragment.this.requireContext(),
                            android.R.layout.simple_spinner_dropdown_item, ambulanceList);
            ambulanceListAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);

            this.promptIfNotGranted = true;
        }

        private void action(boolean granted) {
            if (granted) {
                Log.i(TAG, "Permissions granted");
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R && !Arrays.asList(this.permissions).contains(Manifest.permission.ACCESS_BACKGROUND_LOCATION)) {
                    Log.i(TAG, "Permissions version >= R, need to ask for BACKGROUND LOCATION");
                    this.permissions = new String[]{
                            Manifest.permission.ACCESS_COARSE_LOCATION,
                            Manifest.permission.ACCESS_FINE_LOCATION,
                            Manifest.permission.ACCESS_BACKGROUND_LOCATION
                    };

                    // create permission helper
                    RequestPermissionHelper requestPermissionHelper = new RequestPermissionHelper(
                            requireContext(), activity, this.permissions);

                    // fire request, permissions will be denied and processed by user
                    this.promptIfNotGranted = false;
                    requestPermissionHelper.checkAndRequest(activityResultLauncher,
                            getString(R.string.locationPermissionMessageVersionRMessage2)
                                    + "\n\n" +
                                    getString(R.string.locationPermissionSettingsMessage,
                                            getString(R.string.versionRPermissionOption, requireContext().getPackageManager().getBackgroundPermissionOptionLabel()))
                                    + "\n\n" +
                                    getString(R.string.locationPermissionMessage)
                    );

                } else {
                    Log.i(TAG, "Check settings");
                    checkLocationSettings();
                }

            } else if (this.promptIfNotGranted) {
                Log.i(TAG, "Permissions have not been granted, will launch prompt.");
                // Notify the user via a SnackBar that they have rejected a core permission for the
                // app, which makes the Activity useless.

                // Additionally, it is important to remember that a permission might have been
                // rejected without asking the user for permission (device policy or "Never ask
                // again" prompts). Therefore, a user interface affordance is typically implemented
                // when permissions are denied. Otherwise, your app could appear unresponsive to
                // touches or interactions which have required permissions.
                final String message = android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.R ?
                        getString(R.string.versionRPermissionOption, requireContext().getPackageManager().getBackgroundPermissionOptionLabel()) :
                        "";

                // dismiss first, then go to settings
                // Build intent that displays the App settings screen.
                // intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                new AlertDialog.Builder(activity)
                        .setTitle(R.string.needPermissions)
                        .setMessage(getString(R.string.locationPermissionMessage) + "\n\n" + getString(R.string.locationPermissionSettingsMessage, message))
                        .setPositiveButton(android.R.string.ok,
                                (dialog, which) -> {

                                    // Build intent that displays the App settings screen.
                                    Intent intent = new Intent();
                                    intent.setAction(android.provider.Settings.ACTION_APPLICATION_DETAILS_SETTINGS);
                                    Uri uri = Uri.fromParts("package", BuildConfig.APPLICATION_ID, null);
                                    intent.setData(uri);
                                    // intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                                    startActivity(intent);

                                })
                        .setNegativeButton(android.R.string.cancel,
                                (dialog, which) -> new AlertSnackbar(activity)
                                        .alert(getString(R.string.expectLimitedFuncionality)))
                        .create()
                        .show();

            }
        }

        private void selectAmbulance() {
            Log.i(TAG, "Location settings satisfied, select ambulance");

            new AlertDialog.Builder(activity)
                    .setTitle(R.string.selectAmbulance)
                    .setAdapter(ambulanceListAdapter,
                            (dialog, which) -> {

                                // Get selected status
                                Log.i(TAG, "Ambulance at position '" + which + "' selected.");

                                // get selected ambulance
                                AmbulancePermission selectedAmbulance = ambulancePermissions.get(which);
                                Log.d(TAG, "Selected ambulance " + selectedAmbulance.getAmbulanceIdentifier());

                                // If currently handling ambulance
                                Ambulance ambulance = AmbulanceForegroundService.getAppData().getAmbulance();
                                if (ambulance != null) {

                                    Log.d(TAG, "Current ambulance " + ambulance.getIdentifier());
                                    Log.d(TAG, "Requesting location updates? " +
                                            (AmbulanceForegroundService.isUpdatingLocation() ? "TRUE" : "FALSE"));

                                    if (ambulance.getId() != selectedAmbulance.getAmbulanceId())
                                        // If another ambulance, confirm first
                                        switchAmbulanceDialog(selectedAmbulance);

                                    else if (!AmbulanceForegroundService.isUpdatingLocation())
                                        // else, if current ambulance is not updating location,
                                        // retrieve again
                                        retrieveAmbulance(selectedAmbulance);

                                    // otherwise do nothing

                                } else {

                                    // otherwise go ahead!
                                    retrieveAmbulance(selectedAmbulance);
                                    // dialog.dismiss();

                                }

                            })
                    .create()
                    .show();
        }

        private void checkLocationSettings() {
            // check location settings
            Intent intent = new Intent(getActivity(), AmbulanceForegroundService.class);
            intent.setAction(AmbulanceForegroundService.Actions.CHECK_LOCATION_SETTINGS);

            new OnServiceComplete(getActivity(),
                    BroadcastActions.SUCCESS,
                    BroadcastActions.FAILURE,
                    intent) {

                @Override
                public void onSuccess(Bundle extras) {
                    selectAmbulance();
                }

            }
                    .setFailureMessage(getString(R.string.expectLimitedFuncionality))
                    .setAlert(new AlertSnackbar(activity))
                    .start();
        }

        @Override
        public void onClick(View view) {
            Log.i(TAG, "Clicked on select ambulances.");

            if (AmbulanceForegroundService.canUpdateLocation()) {
                selectAmbulance();
            } else {
                Log.i(TAG, "Location settings not satisfied, checking for permission");

                String message = Build.VERSION.SDK_INT >= Build.VERSION_CODES.R ?
                        getString(R.string.locationPermissionMessage) + "\n\n" + getString(R.string.locationPermissionMessageVersionRMessage1) :
                        getString(R.string.locationPermissionMessage);

                // create permission helper
                RequestPermissionHelper requestPermissionHelper = new RequestPermissionHelper(requireContext(),
                        activity, this.permissions);

                // fire request
                this.promptIfNotGranted = true;
                if ( requestPermissionHelper.checkAndRequest(activityResultLauncher, message) ) {
                    Log.i(TAG, "Permissions granted but not checked; checking location settings");
                    checkLocationSettings();
                } else {
                    Log.i(TAG, "Permissions denied, interacting with user.");
                }
            }
        }

    }

    public class StatusButtonClickListener implements View.OnClickListener {

        private boolean enabled;
        ArrayAdapter<String> ambulanceStatusListAdapter;

        public StatusButtonClickListener() {
            this.enabled = true;

            // Create the adapter
            this.ambulanceStatusListAdapter =
                    new ArrayAdapter<>(AmbulanceFragment.this.requireContext(),
                            android.R.layout.simple_spinner_dropdown_item,
                            ambulanceStatusList);
            ambulanceStatusListAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);

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

            new AlertDialog.Builder(activity)
                    .setTitle(R.string.selectAmbulanceStatus)
                    .setAdapter(ambulanceStatusListAdapter,
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

    public class ReleaseButtonClickListener implements View.OnClickListener {

        @Override
        public void onClick(View view) {

            Call call = AmbulanceForegroundService.getAppData().getCalls().getCurrentCall();
            if (call == null || !call.getCurrentAmbulanceCall().getStatus().equals(AmbulanceCall.STATUS_ACCEPTED))

                // no calls, ask for confirmation
                new AlertDialog.Builder(activity)
                        .setTitle(getString(R.string.releaseAmbulance))
                        .setMessage(R.string.confirmAmbulanceReleaseMessage)
                        .setPositiveButton( android.R.string.ok,
                                (dialog, which) -> {

                                    // Stop current ambulance
                                    Intent intent = new Intent(getActivity(), AmbulanceForegroundService.class);
                                    intent.setAction(AmbulanceForegroundService.Actions.STOP_AMBULANCE);

                                    // Chain services
                                    new OnServiceComplete(getActivity(),
                                            BroadcastActions.SUCCESS,
                                            BroadcastActions.FAILURE,
                                            intent) {

                                        @Override
                                        public void onSuccess(Bundle extras) {
                                            Log.i(TAG, "ambulance released");
                                        }

                                    }
                                            .setFailureMessage(getString(R.string.couldNotReleaseAmbulance))
                                            .setAlert(new AlertSnackbar(activity))
                                            .start();

                                } )
                        .setNegativeButton( android.R.string.cancel,
                                (dialog, which) -> { /* do nothing */ } )
                        .create()
                        .show();

            else {

                // Prompt to end call first
                promptEndCallDialog();

            }

        }
    }

    public void promptEndCallDialog() {

        Log.d(TAG, "Creating end call dialog");

        // Gather call details
        final Call call = AmbulanceForegroundService.getAppData().getCalls().getCurrentCall();
        if (call == null) {

            // Not currently handling call
            Log.d(TAG, "Not currently handling call");
            return;

        }

        // Use the Builder class for convenient dialog construction
        AlertDialog.Builder builder = new AlertDialog.Builder(activity);
        builder.setTitle(R.string.currentlyHandlingCall)
                .setMessage(R.string.whatDoYouWantToDo)
                .setNegativeButton(R.string.toContinue,
                        (dialog, id) -> {})
                .setNeutralButton(R.string.suspend,
                        (dialog, id) -> {

                            Toast.makeText(getActivity(),
                                    R.string.suspendingCall,
                                    Toast.LENGTH_SHORT).show();

                            Log.i(TAG, "Suspending call");

                            Intent serviceIntent = new Intent(getActivity(),
                                    AmbulanceForegroundService.class);
                            serviceIntent.setAction(AmbulanceForegroundService.Actions.CALL_SUSPEND);
                            serviceIntent.putExtra(AmbulanceForegroundService.BroadcastExtras.CALL_ID, call.getId());

                            // Chain services
                            new OnServiceComplete(getActivity(),
                                    BroadcastActions.SUCCESS,
                                    BroadcastActions.FAILURE,
                                    serviceIntent) {

                                @Override
                                public void onSuccess(Bundle extras) {
                                    Log.i(TAG, "fire ambulance release dialog");

                                    releaseButton.performClick();

                                }

                            }
                                    .setFailureMessage(getString(R.string.couldNotSuspendCall))
                                    .setAlert(new AlertSnackbar(activity))
                                    .start();

                        })
                .setPositiveButton(R.string.end,
                        (dialog, id) -> {

                            Toast.makeText(activity,
                                    R.string.endingCall,
                                    Toast.LENGTH_SHORT).show();

                            Log.i(TAG, "Ending call");

                            Intent serviceIntent = new Intent(getActivity(),
                                    AmbulanceForegroundService.class);
                            serviceIntent.setAction(AmbulanceForegroundService.Actions.CALL_FINISH);

                            // Chain services
                            new OnServiceComplete(getActivity(),
                                    BroadcastActions.SUCCESS,
                                    BroadcastActions.FAILURE,
                                    serviceIntent) {

                                @Override
                                public void onSuccess(Bundle extras) {
                                    Log.i(TAG, "fire ambulance release dialog");

                                    releaseButton.performClick();

                                }

                            }
                                    .setFailureMessage(getString(R.string.couldNotFinishCall))
                                    .setAlert(new AlertSnackbar(activity))
                                    .start();

                        });

        // Create the AlertDialog object and return it
        builder.create().show();
    }

    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {

        Log.d(TAG, "onCreateView");

        // set formatter
        df.setMaximumFractionDigits(3);

        // inflate view
        view = inflater.inflate(R.layout.fragment_ambulance, container, false);
        activity = (MainActivity) requireActivity();

        // retrieve ambulanceMessage
        View ambulanceSelectionLayout = view.findViewById(R.id.ambulanceSelectionLayout);

        // retrieve ambulance selection button and message
        ambulanceSelectionButton = ambulanceSelectionLayout.findViewById(R.id.ambulanceSelectionButton);
        ambulanceSelectionMessage = ambulanceSelectionLayout.findViewById(R.id.ambulanceSelectionMessage);

        // retrieve ambulanceFragmentLayout
        ambulanceFragmentLayout = view.findViewById(R.id.ambulanceFragmentLayout);

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
        callPriorityTextView = callLayout.findViewById(R.id.callPriorityTextView);
        callPriorityPrefixTextView = callLayout.findViewById(R.id.callPriorityPrefix);
        callPrioritySuffixTextView = callLayout.findViewById(R.id.callPrioritySuffix);
        callRadioCodeTextView = callLayout.findViewById(R.id.callRadioCodeText);
        callDescriptionTextView = callLayout.findViewById(R.id.callDetailsText);
        callPatientsTextView = callLayout.findViewById(R.id.callPatientsText);
        callNumberWaypointsView = callLayout.findViewById(R.id.callNumberWaypointsText);

        callEndButton = callLayout.findViewById(R.id.callEndButton);
        callAddWaypointButton = callLayout.findViewById(R.id.callAddWaypointButton);

        toMapsButton = callLayout.findViewById(R.id.toMapsButton);
        toMapsButton.setVisibility(View.VISIBLE);

        // Get appData
        AmbulanceAppData appData = AmbulanceForegroundService.getAppData();

        // ambulance permissions
        ambulancePermissions = new ArrayList<>();
        Profile profile = appData.getProfile();
        if (profile != null) {
            ambulancePermissions = profile.getAmbulances();
        }

        // Creates list of ambulance names
        ambulanceList = new ArrayList<>();
        for (AmbulancePermission ambulancePermission : ambulancePermissions)
            ambulanceList.add(ambulancePermission.getAmbulanceIdentifier());

        // Set the ambulance button's adapter
        ambulanceSelectionButton.setOnClickListener(new CheckPermissionsClickListener());

        // setup callNextWaypointLayout
        callNextWaypointLayout = callLayout.findViewById(R.id.callNextWaypointLayout);

        // Retrieve callNextWaypointLayout parts
        callDistanceTextView = callLayout.findViewById(R.id.callDistanceText);
        callNextWaypointTypeTextView = callLayout.findViewById(R.id.callWaypointTypeText);
        callAddressTextView = callLayout.findViewById(R.id.callAddressText);

        // setup callSkipLayout
        callSkipLayout = callLayout.findViewById(R.id.callSkipLayout);

        callSkipWaypointButton = callSkipLayout.findViewById(R.id.callSkipWaypointButton);
        callVisitingWaypointButton = callSkipLayout.findViewById(R.id.callVisitingWaypointButton);

        addCallNoteButton = callLayout.findViewById(R.id.addCallNoteButton);

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
        callNotesText = view.findViewById(R.id.callNotesText);
        updatedOnText = view.findViewById(R.id.updatedOnText);

        commentText = view.findViewById(R.id.commentText);

        // Set release button
        releaseButton = view.findViewById(R.id.amublanceReleaseButton);
        ReleaseButtonClickListener releaseButtonClickListener = new ReleaseButtonClickListener();
        releaseButton.setOnClickListener(releaseButtonClickListener);

        // Set status button
        statusButton = view.findViewById(R.id.statusButton);
        statusButtonClickListener = new StatusButtonClickListener();
        statusButton.setOnClickListener(statusButtonClickListener);

        // Update ambulance
        Ambulance ambulance = appData.getAmbulance();
        updateAmbulance(ambulance);

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
        activity.setupNavigationBar();

        // Set auxiliary panels gone
        callLayout.setVisibility(View.GONE);
        callResumeLayout.setVisibility(View.GONE);

        // Get app data
        AmbulanceAppData appData = AmbulanceForegroundService.getAppData();

        // Update ambulance
        Ambulance ambulance = appData.getAmbulance();
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

                statusButtonClickListener.setEnabled(true);

            } else {

                Log.d(TAG, "CALL Layout");

                callInformationLayout.setVisibility(View.GONE);
                callResumeLayout.setVisibility(View.GONE);
                callLayout.setVisibility(View.VISIBLE);
                currentCallId = call.getId();

                callEndButton.setOnClickListener(
                        v -> {
                            // Prompt end of call
                            activity.promptEndCallDialog(call.getId());
                        }
                );

                callAddWaypointButton.setOnClickListener(
                        v -> {
                            // Prompt add new waypoint
                            activity.promptNextWaypointDialog(call.getId());
                        }
                );

                statusButtonClickListener.setEnabled(false);

            }

        }

        // Update call content
        if (call != null) {

            Log.d(TAG, "Creating call layout");

            // retrieveObject ambulanceCall
            AmbulanceCall ambulanceCall = call.getCurrentAmbulanceCall();
            if (ambulanceCall == null)
                Log.d(TAG, "Call does not have a current ambulance!");

            ((TextView) view.findViewById(R.id.callPriorityLabel)).setText(R.string.currentCall);

            // Get app data
            AmbulanceAppData appData = AmbulanceForegroundService.getAppData();

            // set priority
            callPriorityTextView.setText(call.getPriority());
            callPriorityTextView.setBackgroundColor(activity
                            .getCallPriorityBackgroundColors()
                            .get(call.getPriority()));
            callPriorityTextView.setTextColor(activity
                            .getCallPriorityForegroundColors()
                            .get(call.getPriority()));

            int priorityCodeInt = call.getPriorityCode();
            if (priorityCodeInt < 0) {
                callPriorityPrefixTextView.setText("");
                callPrioritySuffixTextView.setText("");
            } else {
                PriorityCode priorityCode = appData.getPriorityCodes().get(priorityCodeInt);
                callPriorityPrefixTextView.setText(String.format("%d-", priorityCode.getPrefix()));
                callPrioritySuffixTextView.setText(String.format("-%s", priorityCode.getSuffix()));
            }

            //set call notes
            List<CallNote> callNoteSet = call.getCallnoteSet();
            Log.d(TAG, String.format("Retrieved '%1$d' call notes", callNoteSet.size()));
            if (callNoteSet.size() == 0){
                callNotesText.setText(R.string.noCallNotesAvailable);
            }
            else {
                callNotesText.setText("");
                for (int i = 0; i < callNoteSet.size(); i++) {
                    callNotesText.append(callNoteSet.get(i).getComment());
                    callNotesText.append(" (" + callNoteSet.get(i).getUpdatedOn() + ")\n");
                }
            }

            // Set radio code
            int radioCodeInt = call.getRadioCode();
            if (radioCodeInt < 0) {
                callRadioCodeTextView.setText(R.string.unavailable);
            } else {
                RadioCode radioCode = appData.getRadioCodes().get(radioCodeInt);
                callRadioCodeTextView.setText(String.format("%d: %s", radioCode.getId(), radioCode.getLabel()));
            }

            // set details
            callDescriptionTextView.setText(call.getDetails());

            // patients
            List<Patient> patients = call.getPatientSet();
            if (patients != null && patients.size() > 0) {
                StringBuilder text = new StringBuilder();
                for (Patient patient : patients) {
                    if (text.length() > 0)
                        text.append(", ");
                    text.append(patient.getName());
                    if (patient.getAge() != null)
                        text.append(" (").append(patient.getAge()).append(")");
                }

                callPatientsTextView.setText(text.toString());
            } else
                callPatientsTextView.setText(R.string.noPatientAvailable);

            int numberOfWaypoints =
                    (ambulanceCall == null ? 0 : ambulanceCall.getWaypointSet().size());
            callNumberWaypointsView.setText(String.valueOf(numberOfWaypoints));

            final Waypoint waypoint =
                    (ambulanceCall != null
                            ? ambulanceCall.getNextWaypoint()
                            : null);

            if (waypoint != null) {

                Log.d(TAG, "Setting up next waypoint");

                // Get Location
                Location location = waypoint.getLocation();

                // Update waypoint type
                callNextWaypointTypeTextView.setText(
                        appData.getSettings()
                                .getLocationType()
                                .get(location.getType()));

                // Update address
                callAddressTextView.setText(location.toAddress());


                //create intent for google maps here
                // to launch google turn by turn navigation
                // google.navigation:q=a+street+address
                // google.navigation:q=latitude,longitude
                try {

                    String query = URLEncoder.encode(location.toAddress(), "utf-8");

                    Uri gmmIntentUri = Uri.parse("google.navigation:q=" + query);
                    Intent mapIntent = new Intent(Intent.ACTION_VIEW, gmmIntentUri);
                    mapIntent.setPackage("com.google.android.apps.maps");

                    toMapsButton.setOnClickListener(v -> {

                        //checks if google maps or any other map app is installed
                        if ( mapIntent.resolveActivity(activity.getPackageManager()) != null) {

                            // Alert before opening in google maps
                            new AlertDialog.Builder(activity)
                                    .setTitle(getString(R.string.directions))
                                    .setMessage(R.string.wouldYouLikeToGoogleMaps)
                                    .setPositiveButton( android.R.string.ok,
                                            (dialog, which) -> startActivity( mapIntent ))
                                    .setNegativeButton( android.R.string.cancel,
                                            (dialog, which) -> { /* do nothing */ } )
                                    .create()
                                    .show();

                        } else {

                            // Alert that it could not open google maps
                            new org.emstrack.ambulance.dialogs.AlertDialog(getActivity(),
                                    getString(R.string.directions))
                                    .alert(getString(R.string.couldNotOpenGoogleMaps));

                        }

                    });



                } catch (java.io.UnsupportedEncodingException e) {
                    Log.d( TAG, "Could not parse location into url for map intent" );
                }

                // Update call distance to next waypoint
                callDistanceTextView.setText(updateCallDistance(location));

                // Setup visiting button text
                String visitingWaypointText; // = "Mark as ";
                if (waypoint.isCreated()) {
                    // visitingWaypointText += Waypoint.statusLabel.get(Waypoint.STATUS_VISITING);
                    visitingWaypointText = getString(R.string.markAsVisiting);
                    callVisitingWaypointButton.setBackgroundColor(getResources().getColor(R.color.bootstrapWarning));
                    callVisitingWaypointButton.setTextColor(getResources().getColor(R.color.bootstrapDark));
                } else { // if (waypoint.isVisiting())
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
                callSkipWaypointButton.setOnClickListener(
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

                // Setup add call note button
                addCallNoteButton.setOnClickListener(
                        v -> promptAddCallNote(call.getId(), getString(R.string.addCallNote))
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

    public void retrieveAmbulance(final AmbulancePermission selectedAmbulance) {

        // Disable equipment tab
        // equipmentTabLayout.setEnabled(false); //disable clicking
        SharedPreferences sharedPreferences = PreferenceManager.getDefaultSharedPreferences(requireActivity());
        boolean useApproximateLocationAccuracy= sharedPreferences.getBoolean(getString(R.string.useApproximateLocationAccuracy),
                getResources().getBoolean(R.bool.useApproximateLocationAccuracyDefault));

        // Retrieve ambulance
        Intent ambulanceIntent = new Intent(activity, AmbulanceForegroundService.class);
        ambulanceIntent.setAction(AmbulanceForegroundService.Actions.GET_AMBULANCE);
        ambulanceIntent.putExtra(AmbulanceForegroundService.BroadcastExtras.AMBULANCE_ID,
                selectedAmbulance.getAmbulanceId());
        ambulanceIntent.putExtra(AmbulanceForegroundService.BroadcastExtras.PRECISE_LOCATION,
                !useApproximateLocationAccuracy);

        // What to do when GET_AMBULANCE service completes?
        new OnServiceComplete(requireContext(),
                BroadcastActions.SUCCESS,
                BroadcastActions.FAILURE,
                ambulanceIntent) {

            @Override
            public void onSuccess(Bundle extras) {

                // set ambulance button text
                ambulanceSelectionButton.setText(selectedAmbulance.getAmbulanceIdentifier());

                // Enable equipment tab
                // equipmentTabLayout.setEnabled(true); // enable clicking

                // Start updating
                // startUpdatingLocation();

            }

        }
                .setFailureMessage(getResources().getString(R.string.couldNotRetrieveAmbulance,
                        selectedAmbulance.getAmbulanceIdentifier()))
                .setAlert(new org.emstrack.ambulance.dialogs.AlertDialog(activity,
                        getResources().getString(R.string.couldNotStartLocationUpdates),
                        (dialog, which) -> ambulanceSelectionButton.callOnClick()))
                .start();

    }

    public void updateAmbulance(Ambulance ambulance) {

        // quick return if null
        if (ambulance == null) {
            // set selection button label
            ambulanceSelectionButton.setText(R.string.ambulanceButtonDefaultText);

            // set message visible
            ambulanceSelectionMessage.setVisibility(View.VISIBLE);
            ambulanceFragmentLayout.setVisibility(View.GONE);
            return;
        }

        // set message visibility
        ambulanceSelectionMessage.setVisibility(View.GONE);

        // set layout visible
        ambulanceFragmentLayout.setVisibility(View.VISIBLE);

        // set selection button label
        ambulanceSelectionButton.setText(ambulance.getIdentifier());

        // set status button
        setStatusButton(ambulanceStatusList.indexOf(ambulanceStatus.get(ambulance.getStatus())));

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
            ArrayAdapter<String> pendingCallListAdapter = new ArrayAdapter<>(requireContext(),
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
                        activity.promptCallAccept(call.getId());

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

            if (!activity.canWrite()) {

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
            activity.startService(intent);

        } catch (Exception e) {

            Log.i(TAG, "updateAmbulanceStatus exception: " + e);

        }

    }

    public void promptSkipVisitingOrVisited(final String status,
                                            final int waypointId, final int callId, final int ambulanceId,
                                            final String title, final String message, final String doneMessage) {

        Log.d(TAG, "Creating promptSkipVisitingOrVisited dialog");

        // Use the Builder class for convenient dialog construction
        AlertDialog.Builder builder = new AlertDialog.Builder(requireContext());
        builder.setTitle(title)
                .setMessage(message)
                .setNegativeButton(R.string.no,
                        (dialog, id) -> Log.i(TAG, "Continuing..."))
                .setPositiveButton(R.string.yes,
                        (dialog, id) -> {

                            Log.i(TAG, String.format("Will mark as '%1$s'", status));

                            Toast.makeText(getContext(), doneMessage, Toast.LENGTH_SHORT).show();

                            String action;
                            if (status.equals(Waypoint.STATUS_SKIPPED))
                                action = AmbulanceForegroundService.Actions.WAYPOINT_SKIP;
                            else if (status.equals(Waypoint.STATUS_VISITING))
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
                            activity.startService(intent);

                        });

        // Create the AlertDialog object and return it
        builder.create().show();
    }

    public void promptAddCallNote(final int callId, final String title) {

        Log.d(TAG, "Creating promptAddCallNote dialog");

        // Use the Builder class for convenient dialog construction
        AlertDialog.Builder builder = new AlertDialog.Builder(requireContext());
        final View callNoteDialog = getLayoutInflater().inflate(R.layout.add_callnote_dialog, null);

        builder.setView(callNoteDialog)
                .setTitle(title)
                //.setMessage(message)
                .setNegativeButton(R.string.cancel,
                        (dialog, id) -> Log.i(TAG, "Cancelling add call note"))
                .setPositiveButton(R.string.add,
                        (dialog, id) -> {

                            EditText editText = callNoteDialog.findViewById(R.id.dialog_text);
                            Toast.makeText(getContext(), editText.getText().toString(), Toast.LENGTH_SHORT).show();

                            // post call note on server
                            Intent intent = new Intent(getContext(),
                                    AmbulanceForegroundService.class);
                            intent.setAction(AmbulanceForegroundService.Actions.CALLNOTE_CREATE);
                            Bundle bundle = new Bundle();
                            bundle.putString(AmbulanceForegroundService.BroadcastExtras.CALLNOTE_COMMENT, editText.getText().toString());
                            bundle.putInt(AmbulanceForegroundService.BroadcastExtras.CALL_ID, callId);
                            intent.putExtras(bundle);
                            activity.startService(intent);


                        });

        // Create the AlertDialog object and return it
        builder.create().show();
    }

    private void switchAmbulanceDialog(final AmbulancePermission newAmbulance) {

        Log.i(TAG, "Creating switch ambulance dialog");

        // Use the Builder class for convenient dialog construction
        AlertDialog.Builder builder = new AlertDialog.Builder(requireContext());
        builder.setTitle(R.string.switchAmbulance)
                .setMessage(String.format(getString(R.string.switchToAmbulance), newAmbulance.getAmbulanceIdentifier()))
                .setNegativeButton(R.string.no,
                        (dialog, id) -> Log.i(TAG, "Continue with same ambulance"))
                .setPositiveButton(R.string.yes,
                        (dialog, id) -> {

                            Log.d(TAG, String.format("Switching to ambulance %1$s", newAmbulance.getAmbulanceIdentifier()));

                            // retrieve new ambulance
                            retrieveAmbulance(newAmbulance);

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
        return LocalBroadcastManager.getInstance(requireContext());
    }

}