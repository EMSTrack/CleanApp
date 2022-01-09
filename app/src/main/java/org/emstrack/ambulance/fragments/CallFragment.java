package org.emstrack.ambulance.fragments;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Bundle;
import android.os.Handler;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewTreeObserver;
import android.widget.Button;
import android.widget.EditText;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.OptIn;
import androidx.appcompat.app.AlertDialog;
import androidx.fragment.app.Fragment;
import androidx.localbroadcastmanager.content.LocalBroadcastManager;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.LinearSnapHelper;
import androidx.recyclerview.widget.RecyclerView;
import androidx.recyclerview.widget.SnapHelper;

import com.google.android.material.badge.BadgeDrawable;
import com.google.android.material.badge.BadgeUtils;

import org.emstrack.ambulance.MainActivity;
import org.emstrack.ambulance.R;
import org.emstrack.ambulance.adapters.PatientRecyclerAdapter;
import org.emstrack.ambulance.adapters.WaypointInfoRecyclerAdapter;
import org.emstrack.ambulance.models.AmbulanceAppData;
import org.emstrack.ambulance.models.MessageType;
import org.emstrack.ambulance.services.AmbulanceForegroundService;
import org.emstrack.ambulance.views.WaypointViewHolder;
import org.emstrack.models.Ambulance;
import org.emstrack.models.AmbulanceCall;
import org.emstrack.models.Call;
import org.emstrack.models.CallNote;
import org.emstrack.models.PriorityCode;
import org.emstrack.models.RadioCode;
import org.emstrack.models.Settings;
import org.emstrack.models.Waypoint;

import java.util.List;
import java.util.Locale;
import java.util.Map;

@com.google.android.material.badge.ExperimentalBadgeUtils
public class CallFragment extends Fragment {

    private static final String TAG = CallFragment.class.getSimpleName();
    private static final int MAX_RETRIES = 10;

    private CallUpdateBroadcastReceiver receiver;
    private MainActivity activity;

    private LinearLayoutManager waypointLinearLayoutManager;
    private RecyclerView waypointBrowserRecyclerView;

    private LinearLayoutManager patientLinearLayoutManager;
    private RecyclerView patientRecyclerView;

    private Button waypointToolbarPreviousButton;
    private Button waypointToolbarNextButton;
    private Button waypointToolbarVisitingButton;
    private Button waypointToolbarSkipButton;
    private Button waypointToolbarAddWaypointButton;

    private TextView callPriorityTextView;
    private TextView callPriorityPrefixTextView;
    private TextView callPrioritySuffixTextView;
    private TextView callRadioCodeTextView;
    private TextView callDescriptionTextView;
    private TextView callNumberWaypointsView;
    private TextView ambulanceIdentifierText;
    private TextView ambulanceStatusText;

    private TextView callPatientsTextView;
    private ImageView callPatientShowAddIcon;
    private EditText callAddPatientNameEditText;
    private EditText callAddPatientAgeEditText;
    private View callPatientAddLayout;
    private ImageView callPatientAddIcon;
    private ImageView callPatientCancelAddIcon;
    private ImageView callMessageButton;
    private FrameLayout callMessageButtonFrameLayout;

    public class CallUpdateBroadcastReceiver extends BroadcastReceiver {

        @Override
        public void onReceive(Context context, Intent intent ) {
            if (intent != null) {

                final String action = intent.getAction();
                if (action != null) {
                    if (AmbulanceForegroundService.BroadcastActions.CALL_UPDATE.equals(action)) {
                        Log.i(TAG, "CALL_UPDATE");
                        refreshData();
                    } else if (AmbulanceForegroundService.BroadcastActions.CALL_COMPLETED.equals(action)) {
                        Log.i(TAG, "CALL_COMPLETED");
                        activity.navigate(R.id.ambulanceFragment);
                    } else {
                        Log.i(TAG, "Unknown broadcast action");
                    }
                } else {
                    Log.i(TAG, "Action is null");
                }

            }
        }
    }

    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {

        Log.d(TAG, "onCreateView");

        // inflate view
        View view = inflater.inflate(R.layout.fragment_call, container, false);
        activity = (MainActivity) requireActivity();

        // setup callLayout
        View callLayout = view.findViewById(R.id.callLayout);

        // Retrieve callLayout parts
        View callEndButton = callLayout.findViewById(R.id.callEndButton);
        callMessageButtonFrameLayout = callLayout.findViewById(R.id.callMessageButtonFrameLayout);
        callMessageButton = callLayout.findViewById(R.id.callMessageButton);

        ambulanceIdentifierText = callLayout.findViewById(R.id.ambulanceIdentifierText);
        ambulanceStatusText = callLayout.findViewById(R.id.ambulanceStatusText);

        callPriorityTextView = callLayout.findViewById(R.id.callPriorityTextView);
        callPriorityPrefixTextView = callLayout.findViewById(R.id.callPriorityPrefix);
        callPrioritySuffixTextView = callLayout.findViewById(R.id.callPrioritySuffix);

        callRadioCodeTextView = callLayout.findViewById(R.id.callRadioCodeText);
        callDescriptionTextView = callLayout.findViewById(R.id.callDetailsText);
        callPatientsTextView = callLayout.findViewById(R.id.callPatientsText);
        callNumberWaypointsView = callLayout.findViewById(R.id.callNumberOfWaypointsText);

        // setup call buttons
        callEndButton.setOnClickListener(v -> {
            // Prompt end of call
            Call call = AmbulanceForegroundService.getAppData().getCalls().getCurrentCall();
            activity.promptEndCallDialog(call.getId());
        });

        callMessageButton.setOnClickListener(v -> {
            // Go to call messages
            Call call = AmbulanceForegroundService.getAppData().getCalls().getCurrentCall();
            Bundle bundle = new Bundle();
            bundle.putSerializable("type", MessageType.CALL);
            bundle.putInt("id", call.getId());
            activity.navigate(R.id.action_call_to_messages, bundle);
        });

        // add patient
        callPatientAddLayout = callLayout.findViewById(R.id.callPatientAddLayout);

        callPatientShowAddIcon = callLayout.findViewById(R.id.callPatientShowAddIcon);
        callPatientShowAddIcon.setOnClickListener(v -> togglePatientAddVisibility() );

        callAddPatientNameEditText = callPatientAddLayout.findViewById(R.id.patientNameEditText);
        callAddPatientAgeEditText = callPatientAddLayout.findViewById(R.id.patientAgeEditText);

        callPatientAddIcon = callPatientAddLayout.findViewById(R.id.patientAddIcon);
        callPatientAddIcon.setEnabled(false);
        callPatientAddIcon.setOnClickListener(v -> addPatient());

        callPatientCancelAddIcon = callPatientAddLayout.findViewById(R.id.patientCancelAddIcon);
        callPatientCancelAddIcon.setOnClickListener(v -> {
            togglePatientAddVisibility();
        });


        // enable plus only when text is not empty
        callAddPatientNameEditText.addTextChangedListener(new TextWatcher() {
            @Override
            public void onTextChanged(CharSequence charSequence, int i, int i1, int i2) {
                callPatientAddIcon.setEnabled(charSequence.toString().trim().length() != 0);
            }

            @Override
            public void beforeTextChanged(CharSequence charSequence, int i, int i1, int i2) {
            }

            @Override
            public void afterTextChanged(Editable editable) {
            }
        });

        // Set patient recycler view
        patientRecyclerView = callLayout.findViewById(R.id.patientRecyclerView);
        patientLinearLayoutManager = new LinearLayoutManager(requireContext(), LinearLayoutManager.VERTICAL, false);
        patientRecyclerView.setLayoutManager(patientLinearLayoutManager);

        // Set waypoint browser
        View waypointBrowser = callLayout.findViewById(R.id.callWaypointBrowser);
        waypointBrowserRecyclerView = waypointBrowser.findViewById(R.id.waypointBrowserRecyclerView);

        View waypointBrowserToolbar = waypointBrowser.findViewById(R.id.waypointBrowserToolbar);
        waypointToolbarPreviousButton = waypointBrowserToolbar.findViewById(R.id.waypointToolbarPreviousButton);
        waypointToolbarNextButton = waypointBrowserToolbar.findViewById(R.id.waypointToolbarNextButton);
        waypointToolbarVisitingButton = waypointBrowserToolbar.findViewById(R.id.waypointToolbarVisitingButton);
        waypointToolbarSkipButton = waypointBrowserToolbar.findViewById(R.id.waypointToolbarSkipButton);
        waypointToolbarAddWaypointButton = waypointBrowserToolbar.findViewById(R.id.waypointToolbarAddWaypointButton);

        // called after user scrolls waypoint tool
        waypointLinearLayoutManager = new LinearLayoutManager(requireContext(),
                LinearLayoutManager.HORIZONTAL, false) {

            @Override
            public void onScrollStateChanged(int state) {
                super.onScrollStateChanged(state);
                if (state == RecyclerView.SCROLL_STATE_IDLE) {
                    // called after user scrolls
                    int currentPosition = this.findFirstVisibleItemPosition();
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
                    AmbulanceAppData appData = AmbulanceForegroundService.getAppData();
                    Ambulance ambulance = appData.getAmbulance();
                    int currentPosition = waypointLinearLayoutManager.findFirstVisibleItemPosition();
                    Call call = appData.getCalls().getCurrentCall();
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
                        AmbulanceAppData appData = AmbulanceForegroundService.getAppData();
                        Ambulance ambulance = appData.getAmbulance();
                        int currentPosition = waypointLinearLayoutManager.findFirstVisibleItemPosition();
                        Call call = AmbulanceForegroundService.getAppData().getCalls().getCurrentCall();
                        AmbulanceCall ambulanceCall = call.getCurrentAmbulanceCall();
                        List<Waypoint> waypointSet = ambulanceCall.getWaypointSet();
                        Waypoint waypoint = waypointSet.get(currentPosition);

                        if (waypoint.isCreated()) {
                            promptSkipVisitingOrVisited(Waypoint.STATUS_VISITING,
                                    waypoint.getId(), call.getId(), ambulance.getId(),
                                    getString(R.string.pleaseConfirm),
                                    getString(R.string.visitCurrentWaypoint),
                                    getString(R.string.visitingWaypoint));
                        } else {
                            promptSkipVisitingOrVisited(Waypoint.STATUS_VISITED,
                                    waypoint.getId(), call.getId(), ambulance.getId(),
                                    getString(R.string.pleaseConfirm),
                                    getString(R.string.visitedCurrentWaypoint),
                                    getString(R.string.visitedWaypoint));
                        }

                    } catch (NullPointerException e) {
                        Log.d(TAG, "Exception in waypointToolbarSkipButton: " + e);
                    }
                });

        // update call
        refreshData();

        return view;
    }

    @Override
    public void onResume() {
        super.onResume();

        Log.d(TAG, "onResume");
        activity.setupNavigationBar();

        // Register receiver
        IntentFilter filter = new IntentFilter();
        filter.addAction(AmbulanceForegroundService.BroadcastActions.CALL_UPDATE);
        filter.addAction(AmbulanceForegroundService.BroadcastActions.CALL_COMPLETED);
        receiver = new CallFragment.CallUpdateBroadcastReceiver();
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

    public void refreshData() {

        // Get app data
        AmbulanceAppData appData = AmbulanceForegroundService.getAppData();

        // get current call
        Call call = appData.getCalls().getCurrentCall();
        if (call == null) {
            Log.d(TAG, "No current call");
            return;
        }

        // retrieve ambulanceCall
        AmbulanceCall ambulanceCall = call.getCurrentAmbulanceCall();
        if (ambulanceCall == null) {
            Log.d(TAG, "Call does not have a current ambulance!");
            return;
        }

        // get ambulanceCapabilities
        Settings settings = appData.getSettings();
        Map<String, String> ambulanceStatusMap = settings.getAmbulanceStatus();

        // set ambulance identifier
        Ambulance ambulance = appData.getAmbulance();
        ambulanceIdentifierText.setText(ambulance.getIdentifier());

        // set ambulance status
        String status = ambulance.getStatus();
        ambulanceStatusText.setText(ambulanceStatusMap.get(status));

        // set call priority
        String priority = call.getPriority();
        callPriorityTextView.setText(priority);

        // set colors
        try {
            ambulanceStatusText.setTextColor(activity.getAmbulanceStatusBackgroundColorMap().get(status));
            callPriorityTextView.setBackgroundColor(activity.getCallPriorityBackgroundColors().get(priority));
            callPriorityTextView.setTextColor(activity.getCallPriorityForegroundColors().get(priority));
        } catch (NullPointerException e) {
            Log.d(TAG, "Could not set colors");
        }

        int priorityCodeInt = call.getPriorityCode();
        if (priorityCodeInt < 0) {
            callPriorityPrefixTextView.setText("");
            callPrioritySuffixTextView.setText("");
        } else {
            PriorityCode priorityCode = appData.getPriorityCodes().get(priorityCodeInt);
            callPriorityPrefixTextView.setText(String.format(Locale.ENGLISH, "%d-", priorityCode.getPrefix()));
            callPrioritySuffixTextView.setText(String.format("-%s", priorityCode.getSuffix()));
        }

        // Set radio code
        int radioCodeInt = call.getRadioCode();
        if (radioCodeInt < 0) {
            callRadioCodeTextView.setText(R.string.unavailable);
        } else {
            RadioCode radioCode = appData.getRadioCodes().get(radioCodeInt);
            callRadioCodeTextView.setText(String.format(Locale.ENGLISH, "%d: %s", radioCode.getId(), radioCode.getLabel()));
        }

        // set details
        callDescriptionTextView.setText(call.getDetails());

        // Install patients adapter
        PatientRecyclerAdapter patientAdapter = new PatientRecyclerAdapter(requireActivity(), call.getPatientSet());
        patientRecyclerView.setAdapter(patientAdapter);
        callPatientsTextView.setVisibility(View.GONE);
        callPatientAddLayout.setVisibility(View.GONE);
        patientRecyclerView.setVisibility(View.VISIBLE);

        int numberOfWaypoints = ambulanceCall.getWaypointSet().size();
        callNumberWaypointsView.setText(String.valueOf(numberOfWaypoints));

        if (numberOfWaypoints > 0) {

            // Install adapter
            WaypointInfoRecyclerAdapter adapter =
                    new WaypointInfoRecyclerAdapter(requireActivity(), ambulanceCall.getWaypointSet());
            waypointBrowserRecyclerView.setAdapter(adapter);
            waypointBrowserRecyclerView.setVisibility(View.VISIBLE);

            // configure waypoint editor
            configureWaypointEditor(ambulanceCall.getNextWaypointPosition());
        } else {
            waypointBrowserRecyclerView.setVisibility(View.GONE);
        }

        // set badge
        int numberOfUnreadNotes = call.getNumberOfUnreadNotes();
        if (numberOfUnreadNotes > 0) {
            // add badge to
            final Context context = requireContext();
            callMessageButton.getViewTreeObserver().addOnGlobalLayoutListener(new ViewTreeObserver.OnGlobalLayoutListener() {

                @Override
                public void onGlobalLayout() {

                    BadgeDrawable badgeDrawable = BadgeDrawable.create(context);
                    badgeDrawable.setNumber(numberOfUnreadNotes);
                    badgeDrawable.setBadgeGravity(BadgeDrawable.TOP_START);

                    badgeDrawable.setVerticalOffset(10);
//                badgeDrawable.setHorizontalOffset(15);

                    BadgeUtils.attachBadgeDrawable(badgeDrawable, callMessageButton, callMessageButtonFrameLayout);

                    callMessageButton.getViewTreeObserver().removeOnGlobalLayoutListener(this);
                }

            });
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
                    WaypointViewHolder viewHolder = (WaypointViewHolder) waypointBrowserRecyclerView.findViewHolderForAdapterPosition(position);
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

    private void togglePatientAddVisibility() {
        if (callPatientAddLayout.getVisibility() == View.VISIBLE) {
            callPatientAddLayout.setVisibility(View.GONE);
            callPatientShowAddIcon.setVisibility(View.VISIBLE);
        } else {
            callPatientAddLayout.setVisibility(View.VISIBLE);
            callPatientShowAddIcon.setVisibility(View.GONE);
        }
    }

    private void addPatient() {
        new org.emstrack.ambulance.dialogs.AlertDialog(requireActivity(), getString(R.string.alert_warning_title))
                .alert(getString(R.string.notImplementedYet));
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
