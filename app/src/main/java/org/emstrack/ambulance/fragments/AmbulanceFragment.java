package org.emstrack.ambulance.fragments;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.os.Build;
import android.os.Bundle;

import androidx.fragment.app.Fragment;
import androidx.localbroadcastmanager.content.LocalBroadcastManager;
import androidx.appcompat.app.AlertDialog;
import androidx.preference.PreferenceManager;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.LinearSnapHelper;
import androidx.recyclerview.widget.RecyclerView;
import androidx.recyclerview.widget.SnapHelper;

import android.os.Handler;
import android.util.Log;
import android.util.Pair;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.material.button.MaterialButton;

import org.emstrack.ambulance.MainActivity;
import org.emstrack.ambulance.R;
import org.emstrack.ambulance.adapters.WaypointInfoRecyclerAdapter;
import org.emstrack.ambulance.dialogs.AlertSnackbar;
import org.emstrack.ambulance.models.AmbulanceAppData;
import org.emstrack.ambulance.models.EquipmentType;
import org.emstrack.ambulance.models.MessageType;
import org.emstrack.ambulance.services.AmbulanceForegroundService;
import org.emstrack.ambulance.util.RequestPermission;
import org.emstrack.ambulance.views.WaypointInfoRecyclerViewViewHolder;
import org.emstrack.models.Ambulance;
import org.emstrack.models.AmbulanceCall;
import org.emstrack.models.AmbulancePermission;
import org.emstrack.models.Call;
import org.emstrack.models.CallStack;
import org.emstrack.models.Patient;
import org.emstrack.models.PriorityCode;
import org.emstrack.models.Profile;
import org.emstrack.models.RadioCode;
import org.emstrack.models.Settings;
import org.emstrack.models.Waypoint;
import org.emstrack.models.util.BroadcastActions;
import org.emstrack.models.util.OnServiceComplete;

import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class AmbulanceFragment extends Fragment {

    private static final String TAG = AmbulanceFragment.class.getSimpleName();

    private static final DecimalFormat df = new DecimalFormat();
    private static final int MAX_RETRIES = 10;

    private View view;

    private MaterialButton statusButton;

    private TextView capabilityText;
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
    private ImageView callEndButton;
    private TextView callPatientsTextView;
    private TextView callNumberWaypointsView;

    private RelativeLayout callInformationLayout;
    private TextView callInformationText;

    private LinearLayout callResumeLayout;
    private Spinner callResumeSpinner;
    private Button callResumeButton;

    private Map<String,String> ambulanceStatus;
    private List<String> ambulanceStatusList;
    private ArrayList<Integer> ambulanceStatusBackgroundColorList;
    private ArrayList<Integer> ambulanceStatusTextColorList;
    private Map<String,String> ambulanceCapabilities;
    private List<String> ambulanceCapabilityList;

    private int currentCallId;
    private StatusButtonClickListener statusButtonClickListener;

    private TextView ambulanceLabel;
    private TextView ambulanceSelectionMessage;

    private List<AmbulancePermission> ambulancePermissions;
    private MainActivity activity;

    private ImageView ambulanceLogoutButton;
    private View callMessageButton;
    private View waypointBrowser;
    private RecyclerView waypointBrowserRecyclerView;
    private LinearLayoutManager waypointLinearLayoutManager;
    private View waypointToolbarPreviousButton;
    private View waypointToolbarNextButton;
    private Button waypointToolbarVisitingButton;
    private Button waypointToolbarSkipButton;
    private Button waypointToolbarAddWaypointButton;
    private View commentLabel;

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
                            if (currentCallId > 0) {
                                Ambulance ambulance = appData.getAmbulance();
                                Call call = calls.getCurrentCall();
                                updateCall(ambulance, call);
                                configureWaypointEditor(call.getAmbulanceCall(ambulance.getId()).getNextWaypointPosition());
                            }

                            break;
                        case AmbulanceForegroundService.BroadcastActions.CALL_ACCEPTED: {

                            Log.i(TAG, "CALL_ACCEPTED");

                            // Toast to warn user
                            Toast.makeText(getContext(), R.string.CallStarted, Toast.LENGTH_LONG).show();

                            currentCallId = -1;
                            Ambulance ambulance = appData.getAmbulance();
                            Call call = calls.getCurrentCall();
                            updateCall(ambulance, call);
                            configureWaypointEditor(call.getAmbulanceCall(ambulance.getId()).getNextWaypointPosition());
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

                                            // navigate to ambulances
                                            activity.navigate(R.id.ambulances);
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

    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {

        Log.d(TAG, "onCreateView");

        // set formatter
        df.setMaximumFractionDigits(3);

        // inflate view
        view = inflater.inflate(R.layout.fragment_ambulance, container, false);
        activity = (MainActivity) requireActivity();

        // retrieve ambulance selection message
        ambulanceSelectionMessage = view.findViewById(R.id.ambulanceSelectionMessage);

        // retrieve ambulanceFragmentLayout
        ambulanceFragmentLayout = view.findViewById(R.id.ambulanceFragmentLayout);

        // retrieve ambulance selection button
        ambulanceLabel = ambulanceFragmentLayout.findViewById(R.id.ambulanceLabel);

        // retrieveObject callInformationLayout
        callInformationLayout = ambulanceFragmentLayout.findViewById(R.id.callInformationLayout);

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
        callEndButton = callLayout.findViewById(R.id.callEndButton);
        callMessageButton = callLayout.findViewById(R.id.callMessageButton);

        callPriorityTextView = callLayout.findViewById(R.id.callPriorityTextView);
        callPriorityPrefixTextView = callLayout.findViewById(R.id.callPriorityPrefix);
        callPrioritySuffixTextView = callLayout.findViewById(R.id.callPrioritySuffix);
        callRadioCodeTextView = callLayout.findViewById(R.id.callRadioCodeText);
        callDescriptionTextView = callLayout.findViewById(R.id.callDetailsText);
        callPatientsTextView = callLayout.findViewById(R.id.callPatientsText);
        callNumberWaypointsView = callLayout.findViewById(R.id.callNumberWaypointsText);

        // Get appData
        AmbulanceAppData appData = AmbulanceForegroundService.getAppData();

        // ambulance permissions
        ambulancePermissions = new ArrayList<>();
        Profile profile = appData.getProfile();
        if (profile != null) {
            ambulancePermissions = profile.getAmbulances();
        }

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
        updatedOnText = view.findViewById(R.id.updatedOnText);
        commentText = view.findViewById(R.id.commentText);
        commentLabel = view.findViewById(R.id.commentLabel);

        // Set login button
        view.findViewById(R.id.ambulanceLogin).setVisibility(View.GONE);

        // Set logout button
        ambulanceLogoutButton = view.findViewById(R.id.ambulanceLogout);
        ambulanceLogoutButton.setOnClickListener(new ReleaseButtonClickListener());

        // Set location button
        ImageView ambulanceLocationButton = view.findViewById(R.id.ambulanceLocation);
        ambulanceLocationButton.setOnClickListener(view -> {
            ((MainActivity) activity).navigate(R.id.action_ambulance_to_map);
        });

        // Set equipment button
        ImageView ambulanceEquipmentButton = view.findViewById(R.id.ambulanceEquipment);
        ambulanceEquipmentButton.setOnClickListener(view -> {
            int ambulanceId = AmbulanceForegroundService.getAppData().getAmbulanceId();
            Bundle bundle = new Bundle();
            bundle.putSerializable("type", EquipmentType.AMBULANCE);
            bundle.putInt("id", ambulanceId);
            activity.navigate(R.id.action_ambulance_to_equipment, bundle);
        });

        // set ambulance message button
        ImageView ambulanceMessageButton = view.findViewById(R.id.ambulanceMessage);
        ambulanceMessageButton.setOnClickListener(view -> {
            int ambulanceId = AmbulanceForegroundService.getAppData().getAmbulanceId();
            Bundle bundle = new Bundle();
            bundle.putSerializable("type", MessageType.AMBULANCE);
            bundle.putInt("id", ambulanceId);
            activity.navigate(R.id.action_ambulance_to_messages, bundle);
        });

        // Set status button
        statusButton = view.findViewById(R.id.statusButton);
        statusButtonClickListener = new StatusButtonClickListener();
        statusButton.setOnClickListener(statusButtonClickListener);

        // Set waypoint browser
        waypointBrowser = view.findViewById(R.id.callWaypointBrowser);
        waypointBrowserRecyclerView = waypointBrowser.findViewById(R.id.waypointBrowserRecyclerView);

        View waypointBrowserToolbar = waypointBrowser.findViewById(R.id.waypointBrowserToolbar);
        waypointToolbarPreviousButton = waypointBrowserToolbar.findViewById(R.id.waypointToolbarPreviousButton);
        waypointToolbarNextButton = waypointBrowserToolbar.findViewById(R.id.waypointToolbarNextButton);
        waypointToolbarVisitingButton = waypointBrowserToolbar.findViewById(R.id.waypointToolbarVisitingButton);
        waypointToolbarSkipButton = waypointBrowserToolbar.findViewById(R.id.waypointToolbarSkipButton);
        waypointToolbarAddWaypointButton = waypointBrowserToolbar.findViewById(R.id.waypointToolbarAddWaypointButton);

        waypointLinearLayoutManager = new LinearLayoutManager(requireContext(),
                LinearLayoutManager.HORIZONTAL, false) {

            @Override
            public void onScrollStateChanged(int state) {
                super.onScrollStateChanged(state);
                if (state == RecyclerView.SCROLL_STATE_IDLE) {
                    // called after user scrolls
                    int currentPosition = waypointLinearLayoutManager.findFirstVisibleItemPosition();
                    configureWaypointEditor(currentPosition);
                }
            }
        };
        waypointBrowserRecyclerView.setLayoutManager(waypointLinearLayoutManager);
        // attach snap helper
        SnapHelper snapHelper = new LinearSnapHelper();
        snapHelper.attachToRecyclerView(waypointBrowserRecyclerView);

        // set up toolbar
        waypointToolbarPreviousButton
                .setOnClickListener(v -> {
                    int currentPosition = waypointLinearLayoutManager.findFirstVisibleItemPosition();
                    if (currentPosition > 0) {
                        waypointBrowserRecyclerView.smoothScrollToPosition(currentPosition - 1);
                    }
                });
        waypointToolbarNextButton
                .setOnClickListener(v -> {
                    int currentPosition = waypointLinearLayoutManager.findFirstVisibleItemPosition();
                    RecyclerView.Adapter adapter = waypointBrowserRecyclerView.getAdapter();
                    if (adapter != null && currentPosition < adapter.getItemCount()) {
                        waypointBrowserRecyclerView.smoothScrollToPosition(currentPosition + 1);
                    }
                });
        waypointToolbarAddWaypointButton
                .setOnClickListener(v -> {
                    Call call = AmbulanceForegroundService.getAppData().getCalls().getCurrentCall();
                    activity.promptNextWaypointDialog(call.getId());
                });
        waypointToolbarSkipButton
                .setOnClickListener(v -> {
                    AmbulanceAppData appData_ = AmbulanceForegroundService.getAppData();
                    Ambulance ambulance = appData_.getAmbulance();
                    int currentPosition = waypointLinearLayoutManager.findFirstVisibleItemPosition();
                    Call call = appData_.getCalls().getCurrentCall();
                    AmbulanceCall ambulanceCall = call.getCurrentAmbulanceCall();
                    try {
                        List<Waypoint> waypointSet = ambulanceCall.getWaypointSet();
                        Waypoint waypoint = waypointSet.get(currentPosition);
                        promptSkipVisitingOrVisited(Waypoint.STATUS_SKIPPED,
                                waypoint.getId(), call.getId(), ambulance.getId(),
                                getString(R.string.pleaseConfirm),
                                getString(R.string.skipCurrentWaypoint),
                                getString(R.string.skippingWaypoint));
                    } catch (NullPointerException e) {
                        Log.d(TAG, "Exception in waypointToolbarSkipButton: " + e);
                    }
                });
        waypointToolbarVisitingButton
                .setOnClickListener(v -> {
                    try {
                        AmbulanceAppData appData_ = AmbulanceForegroundService.getAppData();
                        Ambulance ambulance = appData_.getAmbulance();
                        int currentPosition = waypointLinearLayoutManager.findFirstVisibleItemPosition();
                        Call call = AmbulanceForegroundService.getAppData().getCalls().getCurrentCall();
                        AmbulanceCall ambulanceCall = call.getCurrentAmbulanceCall();
                        List<Waypoint> waypointSet = ambulanceCall.getWaypointSet();
                        Waypoint waypoint = waypointSet.get(currentPosition);

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

                    } catch (NullPointerException e) {
                        Log.d(TAG, "Exception in waypointToolbarSkipButton: " + e);
                    }
                });


        // get arguments
        Bundle arguments = getArguments();
        final int newAmbulanceId = arguments == null ? -1 : arguments.getInt("id", -1);
        if (newAmbulanceId != -1) {

            // There is a new ambulance
            currentCallId = -1;
            RequestPermission requestPermission = new RequestPermission(this);
            requestPermission.setOnPermissionGranted(granted -> {
                selectAmbulance(newAmbulanceId);
            });
            requestPermission.check();

        }
        currentCallId = -1;

        return view;
    }

    @Override
    public void onResume() {
        super.onResume();

        Log.d(TAG, "onResume");
        activity.setupNavigationBar();

        // Set auxiliary panels gone
        callLayout.setVisibility(View.GONE);
        // callResumeLayout.setVisibility(View.GONE);

        // Get app data
        AmbulanceAppData appData = AmbulanceForegroundService.getAppData();

        // Update ambulance
        Ambulance ambulance = appData.getAmbulance();
        updateAmbulance(ambulance);

        // Are there any calls been currently handled?
        currentCallId = -1;
        Call call = appData.getCalls().getCurrentCall();
        if (call != null) {
            Log.d(TAG, String.format("Is currently handling call '%1$d'", call.getId()));
            updateCall(ambulance, call);
            // get current waypoint position
            AmbulanceCall ambulanceCall = call.getCurrentAmbulanceCall();
            configureWaypointEditor(ambulanceCall.getNextWaypointPosition());
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

    void configureWaypointEditor(int position) {
        configureWaypointEditor(position, 0);
    }

    void configureWaypointEditor(int position, int count) {

        // get current position
        int currentPosition = waypointLinearLayoutManager.findFirstVisibleItemPosition();
        Log.d(TAG, String.format("Waypoint editor; position = %d, currentPosition = %d", position, currentPosition));

        if (currentPosition == -1) {
            if (getContext() != null && count < MAX_RETRIES) {
                // configure waypoint after a while
                new Handler().postDelayed(() -> {
                    Log.d(TAG, "Delaying configuring Waypoint editor");
                    configureWaypointEditor(position, count + 1);
                }, 100);
            } else {
                Log.d(TAG, "Out of context or MAX_RETRIES exceeded. Giving up configuring waypoint editor. Editor is likely not visible");
            }
        }

        if (currentPosition != position) {
            Log.d(TAG, String.format("Will set waypoint position to %d", position));
            // set current position, will configure waypoint editor then
            waypointLinearLayoutManager.scrollToPosition(position);
            return;
        }

        // get adapter and itemCount
        RecyclerView.Adapter adapter = waypointBrowserRecyclerView.getAdapter();
        int itemCount = adapter != null ? adapter.getItemCount() : 0;

        // disable/enable previous/next buttons
        waypointToolbarPreviousButton.setEnabled(position != 0);
        waypointToolbarNextButton.setEnabled(position != itemCount - 1);

        // set visiting button
        try {
            Call call = AmbulanceForegroundService.getAppData().getCalls().getCurrentCall();
            AmbulanceCall ambulanceCall = call.getCurrentAmbulanceCall();
            List<Waypoint> waypointSet = ambulanceCall.getWaypointSet();
            Waypoint waypoint = waypointSet.get(position);
            Waypoint nextWaypoint = ambulanceCall.getNextWaypoint();

            String text;
            int backgroundColor, textColor;

            int nextWaypointId = nextWaypoint == null ? -1 : nextWaypoint.getId();
            int nextWaypointOrder = nextWaypoint == null ? -1 : nextWaypoint.getOrder();

            if (waypoint.getId() == nextWaypointId) {
                // waypoint is next waypoint
                Log.d(TAG, "Waypoint is next waypoint");

                if (waypoint.isCreated()) {

                    text = getString(R.string.markAsVisiting);
                    backgroundColor = getResources().getColor(R.color.bootstrapWarning);
                    textColor = getResources().getColor(R.color.bootstrapDark);

                } else { // if waypoint.isVisiting()

                    text = getString(R.string.markAsVisited);
                    backgroundColor = getResources().getColor(R.color.bootstrapPrimary);
                    textColor = getResources().getColor(R.color.bootstrapLight);

                }

                waypointToolbarVisitingButton.setEnabled(true);
                waypointToolbarAddWaypointButton.setEnabled(true);
                waypointToolbarSkipButton.setEnabled(true);

                if (nextWaypoint != null && waypoint.isCreated()) {
                    // color current waypoint
                    WaypointInfoRecyclerViewViewHolder viewHolder = (WaypointInfoRecyclerViewViewHolder) waypointBrowserRecyclerView.findViewHolderForAdapterPosition(position);
                    if (viewHolder != null) {
                        viewHolder.setAsCurrent();
                    }
                }

            } else if (waypoint.getOrder() < nextWaypointOrder || nextWaypointOrder == -1) {
                Log.d(TAG, "Waypoint comes before current waypoint or there is no next waypoint");

                if (waypoint.isVisited()) {

                    // waypoint is already visited
                    text = getString(R.string.markAsVisited);
                    backgroundColor = getResources().getColor(R.color.bootstrapPrimary);
                    textColor = getResources().getColor(R.color.bootstrapLight);

                } else { // { if (waypoint.isSkipped()) {

                    // waypoint was skipped
                    text = getString(R.string.skipped);
                    backgroundColor = getResources().getColor(R.color.bootstrapSecondary);
                    textColor = getResources().getColor(R.color.bootstrapDark);

                }
                waypointToolbarVisitingButton.setEnabled(false);
                waypointToolbarAddWaypointButton.setEnabled(nextWaypointOrder == -1 && position == waypointSet.size() - 1);
                waypointToolbarSkipButton.setEnabled(false);

            } else { // if (waypoint.getOrder() > nextWaypoint.getOrder()) {
                Log.d(TAG, "Waypoint comes after current waypoint");

                // waypoint is not visited yet
                text = getString(R.string.markAsVisiting);
                backgroundColor = getResources().getColor(R.color.bootstrapWarning);
                textColor = getResources().getColor(R.color.bootstrapDark);

                waypointToolbarVisitingButton.setEnabled(false);
                waypointToolbarAddWaypointButton.setEnabled(true);
                waypointToolbarSkipButton.setEnabled(true);

            }

            waypointToolbarVisitingButton.setText(text);
            waypointToolbarVisitingButton.setBackgroundColor(backgroundColor);
            waypointToolbarVisitingButton.setTextColor(textColor);

        } catch (NullPointerException e) {
            Log.d(TAG, "Exception in setupWaypointToolbar: " + e);
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

                                    ambulanceLogoutButton.performClick();

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

                                    ambulanceLogoutButton.performClick();

                                }

                            }
                                    .setFailureMessage(getString(R.string.couldNotFinishCall))
                                    .setAlert(new AlertSnackbar(activity))
                                    .start();

                        });

        // Create the AlertDialog object and return it
        builder.create().show();
    }

    public void selectAmbulance(int ambulanceId) {

        Log.i(TAG, "Location settings satisfied, select ambulance");

        if (ambulanceId == -1) {

            Log.d(TAG, "No ambulance selected");

        } else {

            // If currently handling ambulance
            Ambulance ambulance = AmbulanceForegroundService.getAppData().getAmbulance();
            if (ambulance != null) {

                Log.d(TAG, "Current ambulance " + ambulance.getIdentifier());
                Log.d(TAG, "Requesting location updates? " +
                        (AmbulanceForegroundService.isUpdatingLocation() ? "TRUE" : "FALSE"));

                if (ambulance.getId() != ambulanceId)
                    // If another ambulance, confirm first
                    switchAmbulanceDialog(ambulanceId);

                else if (!AmbulanceForegroundService.isUpdatingLocation())
                    // else, if current ambulance is not updating location,
                    // retrieve again
                    retrieveAmbulance(ambulanceId);

                // otherwise do nothing

            } else {

                // otherwise go ahead!
                retrieveAmbulance(ambulanceId);
                // dialog.dismiss();

            }

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

                callEndButton.setOnClickListener(v -> {
                    // Prompt end of call
                    activity.promptEndCallDialog(call.getId());
                });

                callMessageButton.setOnClickListener(v -> {
                    Bundle bundle = new Bundle();
                    bundle.putSerializable("type", MessageType.CALL);
                    bundle.putInt("id", call.getId());
                    activity.navigate(R.id.action_ambulance_to_messages, bundle);
                });

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

            if (numberOfWaypoints > 0) {
                // Install adapter
                WaypointInfoRecyclerAdapter adapter =
                        new WaypointInfoRecyclerAdapter(requireActivity(), ambulanceCall.getWaypointSet());
                waypointBrowserRecyclerView.setAdapter(adapter);
            } else {
                waypointBrowserRecyclerView.setVisibility(View.VISIBLE);
            }

        } else
            // update ambulance to set suspended/requested count correct
            updateAmbulance(ambulance);

    }

    public void retrieveAmbulance(int ambulanceId) {

        // Disable equipment tab
        SharedPreferences sharedPreferences = PreferenceManager.getDefaultSharedPreferences(requireActivity());
        boolean useApproximateLocationAccuracy= sharedPreferences.getBoolean(getString(R.string.useApproximateLocationAccuracyKey),
                getResources().getBoolean(R.bool.useApproximateLocationAccuracyDefault));

        // Retrieve ambulance
        Intent ambulanceIntent = new Intent(activity, AmbulanceForegroundService.class);
        ambulanceIntent.setAction(AmbulanceForegroundService.Actions.GET_AMBULANCE);
        ambulanceIntent.putExtra(AmbulanceForegroundService.BroadcastExtras.AMBULANCE_ID, ambulanceId);
        ambulanceIntent.putExtra(AmbulanceForegroundService.BroadcastExtras.PRECISE_LOCATION,
                !useApproximateLocationAccuracy);

        // What to do when GET_AMBULANCE service completes?
        new OnServiceComplete(requireContext(),
                BroadcastActions.SUCCESS,
                BroadcastActions.FAILURE,
                ambulanceIntent) {

            @Override
            public void onSuccess(Bundle extras) {

                // get ambulance
                Ambulance ambulance = AmbulanceForegroundService.getAppData().getAmbulance();

                // set ambulance button text
                ambulanceLabel.setText(ambulance.getIdentifier());

            }

        }
                .setFailureMessage(getResources().getString(R.string.couldNotRetrieveAmbulance,
                        String.format("id = %d", ambulanceId)))
                .setAlert(new org.emstrack.ambulance.dialogs.AlertDialog(activity,
                        getResources().getString(R.string.couldNotStartLocationUpdates)))
                .start();

        // TODO: WHAT SHOULD WE DO HERE?

    }

    public void updateAmbulance(Ambulance ambulance) {

        // quick return if null
        if (ambulance == null) {

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
        ambulanceLabel.setText(ambulance.getIdentifier());

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
        Log.d(TAG, String.format("currentCallId = %d", currentCallId));
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
            callResumeButton.setOnClickListener(v -> {

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
            Log.d(TAG, "Will hide call resume layout");
            callResumeLayout.setVisibility(View.GONE);
        }

        // update call distance?
        if (currentCallId > 0) {

            Call call = AmbulanceForegroundService.getAppData().getCalls().getCurrentCall();
            if (call != null) {
                AmbulanceCall ambulanceCall = call.getCurrentAmbulanceCall();
                Waypoint waypoint = ambulanceCall.getNextWaypoint();
                if (waypoint != null) {
                    // TODO: UPDATE DISTANCES ON TOOLBAR
                    RecyclerView.Adapter adapter = waypointBrowserRecyclerView.getAdapter();
                    if (adapter != null) {
                        adapter.notifyDataSetChanged();
                    }
                }
            }

        }

        // set comment
        String comment = ambulance.getComment();
        if (comment != null && !comment.equals("")) {
            commentText.setText(comment);
            commentText.setVisibility(View.VISIBLE);
            commentLabel.setVisibility(View.VISIBLE);
        } else {
            commentText.setVisibility(View.GONE);
            commentLabel.setVisibility(View.GONE);
        }

        // set updated on
        updatedOnText.setText(ambulance.getUpdatedOn().toString());

        // set capability
        capabilityText.setText(ambulanceCapabilities.get(ambulance.getCapability()));

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

    private void switchAmbulanceDialog(final int ambulanceId) {
        String ambulanceIdentifier = "";
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            AmbulancePermission newAmbulance = ambulancePermissions
                    .stream()
                    .filter(ambulancePermission -> ambulancePermission.getAmbulanceId() == ambulanceId)
                    .findAny()
                    .orElse(null);
            if (newAmbulance != null)
                ambulanceIdentifier = newAmbulance.getAmbulanceIdentifier();
        } else {
            for (AmbulancePermission ambulancePermission: ambulancePermissions) {
                if (ambulancePermission.getAmbulanceId() == ambulanceId) {
                    ambulanceIdentifier = ambulancePermission.getAmbulanceIdentifier();
                    break;
                }
            }
        }
        switchAmbulanceDialog(ambulanceId, ambulanceIdentifier);
    }

    private void switchAmbulanceDialog(final int ambulanceId, final String ambulanceIdentifier) {

        if (ambulanceId == -1) {
            Log.i(TAG, "ambulanceId was -1");
            return;
        }

        Log.i(TAG, "Creating switch ambulance dialog");

        // Use the Builder class for convenient dialog construction
        AlertDialog.Builder builder = new AlertDialog.Builder(requireContext());
        builder.setTitle(R.string.switchAmbulance)
                .setMessage(String.format(getString(R.string.switchToAmbulance), ambulanceIdentifier))
                .setNegativeButton(R.string.no,
                        (dialog, id) -> Log.i(TAG, "Continue with same ambulance"))
                .setPositiveButton(R.string.yes,
                        (dialog, id) -> {

                            Log.d(TAG, String.format("Switching to ambulance %1$s", ambulanceIdentifier));

                            // retrieve new ambulance
                            retrieveAmbulance(ambulanceId);

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