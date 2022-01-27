package org.emstrack.ambulance.fragments;

import android.annotation.SuppressLint;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Bundle;
import android.os.Handler;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewTreeObserver;
import android.view.inputmethod.InputMethodManager;
import android.widget.EditText;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.LinearSnapHelper;
import androidx.recyclerview.widget.RecyclerView;
import androidx.recyclerview.widget.SnapHelper;

import com.google.android.material.badge.BadgeDrawable;
import com.google.android.material.badge.BadgeUtils;
import com.google.android.material.bottomsheet.BottomSheetBehavior;

import org.emstrack.ambulance.MainActivity;
import org.emstrack.ambulance.R;
import org.emstrack.ambulance.adapters.PatientRecyclerAdapter;
import org.emstrack.ambulance.adapters.WaypointInfoRecyclerAdapter;
import org.emstrack.ambulance.dialogs.SimpleAlertDialog;
import org.emstrack.ambulance.models.AmbulanceAppData;
import org.emstrack.ambulance.models.MessageType;
import org.emstrack.ambulance.services.AmbulanceForegroundService;
import org.emstrack.ambulance.util.FragmentWithLocalBroadcastReceiver;
import org.emstrack.ambulance.util.ViewTextWatcher;
import org.emstrack.ambulance.views.WaypointViewHolder;
import org.emstrack.models.Ambulance;
import org.emstrack.models.AmbulanceCall;
import org.emstrack.models.Call;
import org.emstrack.models.Patient;
import org.emstrack.models.PriorityCode;
import org.emstrack.models.RadioCode;
import org.emstrack.models.Settings;
import org.emstrack.models.Waypoint;

import java.util.List;
import java.util.Locale;

@com.google.android.material.badge.ExperimentalBadgeUtils
public class CallFragment extends FragmentWithLocalBroadcastReceiver {

    private static final String TAG = CallFragment.class.getSimpleName();
    private static final int MAX_RETRIES = 10;

    private MainActivity activity;

    private LinearLayoutManager waypointLinearLayoutManager;
    private RecyclerView waypointBrowserRecyclerView;

    private RecyclerView patientRecyclerView;

    private ImageView bottomSheetNextIcon;
    private ImageView bottomSheetAddIcon;
    private ImageView bottomSheetUndoIcon;

    private TextView callPriorityTextView;
    private TextView callPriorityPrefixTextView;
    private TextView callPrioritySuffixTextView;
    private TextView callRadioCodeTextView;
    private TextView callDescriptionTextView;
    private TextView callNumberWaypointsView;
    private TextView ambulanceIdentifierText;
    private TextView ambulanceStatusText;

    private ImageView callPatientShowAddIcon;
    private View callPatientAddLayout;
    private ImageView callMessageButton;
    private FrameLayout callMessageButtonFrameLayout;
    private WaypointInfoRecyclerAdapter waypointAdapter;
    private BottomSheetBehavior<View> bottomSheetBehavior;
    private ImageView waypointMaximize;
    private ImageView waypointMinimize;
    private View patientScrollView;
    private View callPatientsTextView;
    private View bottomFillerView;

    @SuppressLint("NotifyDataSetChanged")
    @Override
    public void onReceive(Context context, @NonNull Intent intent) {
        final String action = intent.getAction();
        if (action != null) {
            switch (action) {
                case AmbulanceForegroundService.BroadcastActions.AMBULANCE_UPDATE:
                    Log.i(TAG, "AMBULANCE_UPDATE");
                    if (waypointAdapter != null) {
                        waypointAdapter.notifyDataSetChanged();
                        configureWaypointEditor();
                    }
                    break;
                case AmbulanceForegroundService.BroadcastActions.CALL_UPDATE:
                    Log.i(TAG, "CALL_UPDATE");
                    refreshData();
                    break;
                case AmbulanceForegroundService.BroadcastActions.CALL_COMPLETED:
                    Log.i(TAG, "CALL_COMPLETED");
                    activity.navigate(R.id.ambulanceFragment);
                    break;
                default:
                    Log.i(TAG, "Unknown broadcast action");
                    break;
            }
        } else {
            Log.i(TAG, "Action is null");
        }

    }

    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {

        Log.d(TAG, "onCreateView");

        // inflate view
        View view = inflater.inflate(R.layout.fragment_call, container, false);
        activity = (MainActivity) requireActivity();

        // bottom sheet
        View waypointBrowserBottomSheet = view.findViewById(R.id.waypointBrowserBottomSheet);
        bottomSheetBehavior = BottomSheetBehavior.from(waypointBrowserBottomSheet);

        // Retrieve callLayout parts
        View callEndButton = view.findViewById(R.id.callEndButton);
        callMessageButtonFrameLayout = view.findViewById(R.id.callMessageButtonFrameLayout);
        callMessageButton = view.findViewById(R.id.callMessageButton);

        ambulanceIdentifierText = view.findViewById(R.id.ambulanceIdentifierText);
        ambulanceStatusText = view.findViewById(R.id.ambulanceStatusText);

        callPriorityTextView = view.findViewById(R.id.callPriorityTextView);
        callPriorityPrefixTextView = view.findViewById(R.id.callPriorityPrefix);
        callPrioritySuffixTextView = view.findViewById(R.id.callPrioritySuffix);

        callRadioCodeTextView = view.findViewById(R.id.callRadioCodeText);
        callDescriptionTextView = view.findViewById(R.id.callDetailsText);
        callPatientsTextView = view.findViewById(R.id.callPatientsText);
        callNumberWaypointsView = view.findViewById(R.id.callNumberOfWaypointsText);
        bottomFillerView = view.findViewById(R.id.bottomFillerView);

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
        callPatientAddLayout = view.findViewById(R.id.callPatientAddLayout);

        callPatientShowAddIcon = view.findViewById(R.id.callPatientShowAddIcon);
        callPatientShowAddIcon.setOnClickListener(v -> togglePatientAddVisibility() );

        EditText callAddPatientNameEditText = callPatientAddLayout.findViewById(R.id.patientNameEditText);
        EditText callAddPatientAgeEditText = callPatientAddLayout.findViewById(R.id.patientAgeEditText);

        ImageView callPatientAddIcon = callPatientAddLayout.findViewById(R.id.patientAddIcon);
        callPatientAddIcon.setEnabled(false);
        callPatientAddIcon.setOnClickListener(v -> addPatient());

        ImageView callPatientCancelAddIcon = callPatientAddLayout.findViewById(R.id.patientCancelAddIcon);
        callPatientCancelAddIcon.setOnClickListener(v -> togglePatientAddVisibility());


        // enable plus only when text is not empty
        callAddPatientNameEditText.addTextChangedListener(new ViewTextWatcher(callPatientAddIcon));

        // Set patient recycler view
        patientScrollView = view.findViewById(R.id.patientScrollView);
        patientRecyclerView = view.findViewById(R.id.patientRecyclerView);
        LinearLayoutManager patientLinearLayoutManager = new LinearLayoutManager(requireContext(),
                LinearLayoutManager.VERTICAL, false);
        patientRecyclerView.setLayoutManager(patientLinearLayoutManager);

        // Set waypoint browser
        waypointBrowserRecyclerView = view.findViewById(R.id.waypointBrowserRecyclerView);

        bottomSheetUndoIcon = waypointBrowserBottomSheet.findViewById(R.id.bottomSheetUndoIcon);
        bottomSheetAddIcon = waypointBrowserBottomSheet.findViewById(R.id.bottomSheetAddIcon);
        bottomSheetNextIcon = waypointBrowserBottomSheet.findViewById(R.id.bottomSheetCurrentIcon);

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

        // set waypoint arrows
        waypointMaximize = view.findViewById(R.id.waypointMaximize);
        waypointMinimize = view.findViewById(R.id.waypointMinimize);
        waypointMaximize.setOnClickListener(v -> {
            waypointMaximize.setVisibility(View.GONE);
            waypointMinimize.setVisibility(View.VISIBLE);
            waypointLinearLayoutManager.setOrientation(LinearLayoutManager.VERTICAL);
            waypointAdapter.setHideRightPanel(false);
            waypointBrowserRecyclerView.setAdapter(waypointAdapter);
        });
        waypointMinimize.setOnClickListener(v -> {
            waypointMaximize.setVisibility(View.VISIBLE);
            waypointMinimize.setVisibility(View.GONE);
            waypointLinearLayoutManager.setOrientation(LinearLayoutManager.HORIZONTAL);
            waypointAdapter.setHideRightPanel(true);
            waypointBrowserRecyclerView.setAdapter(waypointAdapter);
        });

        // set up toolbar
        bottomSheetNextIcon
                .setOnClickListener(v -> waypointBrowserRecyclerView.smoothScrollToPosition(getNextWaypointPosition()));

        bottomSheetAddIcon
                .setOnClickListener(v -> activity.navigate(R.id.action_call_to_selectLocation));

        bottomSheetUndoIcon
                .setOnClickListener(v-> new SimpleAlertDialog(requireActivity(), getString(R.string.alert_warning_title))
                        .alert(getString(R.string.notImplementedYet)));

        // bottom sheet callback
        bottomSheetBehavior.addBottomSheetCallback(new BottomSheetBehavior.BottomSheetCallback() {
            @Override
            public void onStateChanged(@NonNull View bottomSheet, int newState) {
                if (newState == BottomSheetBehavior.STATE_EXPANDED) {
                    int orientation = waypointLinearLayoutManager.getOrientation();
                    if (orientation == LinearLayoutManager.VERTICAL) {
                        waypointMaximize.setVisibility(View.GONE);
                        waypointMinimize.setVisibility(View.VISIBLE);
                    } else {
                        waypointMaximize.setVisibility(View.VISIBLE);
                        waypointMinimize.setVisibility(View.GONE);
                    }
                    bottomSheetUndoIcon.setVisibility(View.VISIBLE);
                    bottomSheetNextIcon.setVisibility(View.VISIBLE);
                    bottomSheetAddIcon.setVisibility(View.VISIBLE);
                } else if (newState == BottomSheetBehavior.STATE_COLLAPSED) {
                    waypointMaximize.setVisibility(View.GONE);
                    waypointMinimize.setVisibility(View.GONE);
                    bottomSheetUndoIcon.setVisibility(View.GONE);
                    bottomSheetNextIcon.setVisibility(View.GONE);
                    bottomSheetAddIcon.setVisibility(View.GONE);
                }
            }

            @Override
            public void onSlide(@NonNull View bottomSheet, float slideOffset) {

            }
        });

        // update call
        refreshData();

        // process arguments
        processArguments();

        // Register receiver
        IntentFilter filter = new IntentFilter();
        filter.addAction(AmbulanceForegroundService.BroadcastActions.AMBULANCE_UPDATE);
        filter.addAction(AmbulanceForegroundService.BroadcastActions.CALL_UPDATE);
        filter.addAction(AmbulanceForegroundService.BroadcastActions.CALL_COMPLETED);
        setupReceiver(filter);

        return view;
    }

    @Override
    public void onResume() {
        super.onResume();

        Log.d(TAG, "onResume");
        activity.setupNavigationBar();

        // configure waypoint editor
        configureWaypointEditor();

        // set colors
        setColors();

    }

    public void processArguments() {
        Bundle arguments = getArguments();
        if (arguments != null) {
            Log.d(TAG, "Has arguments");
            String action = arguments.getString(MainActivity.ACTION);
            if (action != null) {

                Log.d(TAG, "Process arguments");
                int ambulanceId = arguments.getInt("ambulanceId", -1);
                int callId = arguments.getInt("callId", -1);
                int waypointId = arguments.getInt("waypointId", -1);
                AmbulanceAppData appData = AmbulanceForegroundService.getAppData();
                Ambulance ambulance = appData.getAmbulance();
                Call call = appData.getCalls().getCurrentCall();

                boolean invalidCall = true;
                if (ambulance != null && call != null) {

                    // get ambulance call and waypoint
                    AmbulanceCall ambulanceCall = call.getCurrentAmbulanceCall();
                    Waypoint waypoint = ambulanceCall.getNextWaypoint();
                    if (ambulance.getId() == ambulanceId &&
                            ambulanceCall.getAmbulanceId() == ambulanceId &&
                            call.getId() == callId) {
                        if (action.equals(MainActivity.ACTION_OPEN_CALL_FRAGMENT)) {
                            Log.d(TAG, "Will simply open call fragment");
                            invalidCall = false;
                        } else if (waypoint.getId() == waypointId) {
                            if (action.equals(MainActivity.ACTION_MARK_AS_VISITING)) {
                                if (waypoint.isCreated()) {
                                    Log.d(TAG, "Will prompt to mark visiting");
                                    WaypointViewHolder.promptSkipVisitingOrVisited(activity,
                                            Waypoint.STATUS_VISITING,
                                            waypointId, callId, ambulanceId,
                                            getString(R.string.pleaseConfirm),
                                            getString(R.string.visitCurrentWaypoint,
                                                    waypoint.getLocation().toAddress(requireContext())),
                                            getString(R.string.visitingWaypoint));
                                    invalidCall = false;
                                }
                            } else if (action.equals(MainActivity.ACTION_MARK_AS_VISITED)) {
                                if (waypoint.isVisiting()) {
                                    Log.d(TAG, "Will prompt to mark visited");
                                    WaypointViewHolder.promptSkipVisitingOrVisited(activity,
                                            Waypoint.STATUS_VISITED,
                                            waypointId, callId, ambulanceId,
                                            getString(R.string.pleaseConfirm),
                                            getString(R.string.visitedCurrentWaypoint,
                                                    waypoint.getLocation().toAddress(requireContext())),
                                            getString(R.string.visitedWaypoint));
                                    invalidCall = false;
                                }
                            }
                        }
                    }
                }

                if (invalidCall) {
                    Log.d(TAG, String.format("Ambulance '%d', call '%d' and waypoint '%d' are not current", ambulanceId, callId, waypointId));
//                    new SimpleAlertDialog(activity, getString(R.string.alert_warning_title))
//                            .alert(getString(R.string.invalidNotification), (dialogInterface, i) -> activity.navigate(R.id.mapFragment));
                }

            }
        } else {
            Log.d(TAG, "Has no arguments");
        }
    }

    public void refreshData() {

        // Get app data
        AmbulanceAppData appData = AmbulanceForegroundService.getAppData();
        Ambulance ambulance = appData.getAmbulance();
        if (ambulance == null) {
            Log.d(TAG, "No current ambulance");
            return;
        }

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

        // get settings
        Settings settings = appData.getSettings();

        // set ambulance status
        String status = ambulance.getStatus();
        ambulanceStatusText.setText(settings != null ? settings.getAmbulanceStatus().get(status) : "");

        // set ambulance identifier
        ambulanceIdentifierText.setText(ambulance.getIdentifier());

        // set call priority
        String priority = call.getPriority();
        callPriorityTextView.setText(priority);

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
        List<Patient> patientSet = call.getPatientSet();
        PatientRecyclerAdapter patientAdapter = new PatientRecyclerAdapter(patientSet);
        patientRecyclerView.setAdapter(patientAdapter);
        if (patientSet.size() > 0) {
            callPatientsTextView.setVisibility(View.GONE);
        }
        callPatientAddLayout.setVisibility(View.GONE);
        patientScrollView.setVisibility(View.VISIBLE);

        int numberOfWaypoints = ambulanceCall.getWaypointSet().size();
        callNumberWaypointsView.setText(String.valueOf(numberOfWaypoints));

        // Install adapter
        waypointAdapter = new WaypointInfoRecyclerAdapter(requireActivity(), ambulanceCall.getWaypointSet(),
                false, false, true, false);
        waypointBrowserRecyclerView.setAdapter(waypointAdapter);
        waypointAdapter
                .getItemTouchHelper()
                .attachToRecyclerView(waypointBrowserRecyclerView);
        configureWaypointEditor();

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

    private void setColors() {

        // Get app data
        AmbulanceAppData appData = AmbulanceForegroundService.getAppData();
        Ambulance ambulance = appData.getAmbulance();
        if (ambulance == null) {
            Log.d(TAG, "No current ambulance");
            return;
        }

        // get current call
        Call call = appData.getCalls().getCurrentCall();
        if (call == null) {
            Log.d(TAG, "No current call");
            return;
        }

        String status = ambulance.getStatus();
        String priority = call.getPriority();

        try {
            ambulanceStatusText.setTextColor(activity.getAmbulanceStatusBackgroundColorMap().get(status));
            callPriorityTextView.setBackgroundColor(activity.getCallPriorityBackgroundColors().get(priority));
            callPriorityTextView.setTextColor(activity.getCallPriorityForegroundColors().get(priority));
        } catch (NullPointerException e) {
            Log.d(TAG, "Could not set colors");
        }

    }

    private int getNextWaypointPosition() {
        Call call = AmbulanceForegroundService.getAppData().getCalls().getCurrentCall();
        if (call != null) {
            return call.getCurrentAmbulanceCall().getNextWaypointPosition();
        }
        return -1;
    }

    private void configureWaypointEditor() {
        configureWaypointEditor(getNextWaypointPosition(), 0);
    }

    private void configureWaypointEditor(int position) {
        configureWaypointEditor(position, 0);
    }

    private void configureWaypointEditor(int position, int numberOfAttempts) {

        // configure buttons
        if (bottomSheetBehavior.getState() == BottomSheetBehavior.STATE_COLLAPSED) {
            bottomSheetUndoIcon.setVisibility(View.GONE);
            bottomSheetNextIcon.setVisibility(View.GONE);
            bottomSheetAddIcon.setVisibility(View.GONE);
            waypointMaximize.setVisibility(View.GONE);
            waypointMinimize.setVisibility(View.GONE);
        } else {
            bottomSheetUndoIcon.setVisibility(View.VISIBLE);
            bottomSheetNextIcon.setVisibility(View.VISIBLE);
            bottomSheetAddIcon.setVisibility(View.VISIBLE);
            if (waypointLinearLayoutManager.getOrientation() == LinearLayoutManager.VERTICAL) {
                waypointAdapter.setHideRightPanel(false);
                waypointMaximize.setVisibility(View.GONE);
                waypointMinimize.setVisibility(View.VISIBLE);
            } else {
                waypointAdapter.setHideRightPanel(true);
                waypointMaximize.setVisibility(View.VISIBLE);
                waypointMinimize.setVisibility(View.GONE);
            }
        }

        // get current position
        int currentPosition = waypointLinearLayoutManager.findFirstVisibleItemPosition();
        Log.d(TAG, String.format("Waypoint editor; position = %d, currentPosition = %d", position, currentPosition));

        if (currentPosition == -1) {
            if (getContext() != null && numberOfAttempts < MAX_RETRIES) {
                // configure waypoint after a while
                new Handler().postDelayed(() -> {
                    Log.d(TAG, "Delaying configuring Waypoint editor");
                    configureWaypointEditor(position, numberOfAttempts + 1);
                }, 100);
            } else {
                Log.d(TAG, "Out of context or MAX_RETRIES exceeded. Giving up configuring waypoint editor. Editor is likely not visible");
            }
        }

        if (currentPosition != position && waypointLinearLayoutManager.getOrientation() == LinearLayoutManager.HORIZONTAL) {
            Log.d(TAG, String.format("Will set waypoint position to %d", position));
            // set current position, will configure waypoint editor then
            waypointLinearLayoutManager.scrollToPosition(position);
            return;
        }

        // get adapter and itemCount
        WaypointInfoRecyclerAdapter adapter = (WaypointInfoRecyclerAdapter) waypointBrowserRecyclerView.getAdapter();
        Call call = AmbulanceForegroundService.getAppData().getCalls().getCurrentCall();
        if (adapter != null && call != null && position != -1) {

            // set visiting button
            AmbulanceCall ambulanceCall = call.getCurrentAmbulanceCall();
            List<Waypoint> waypointSet = ambulanceCall.getWaypointSet();
            if (waypointSet.get(position) == ambulanceCall.getNextWaypoint()) {
                // waypoint is next waypoint
                Log.d(TAG, "Waypoint is next waypoint, set as selected");

                // set as selected
                adapter.setSelectedPosition(position);

            }
        }
    }

    private void togglePatientAddVisibility() {
        if (callPatientAddLayout.getVisibility() == View.VISIBLE) {
            callPatientAddLayout.setVisibility(View.GONE);
            callPatientShowAddIcon.setVisibility(View.VISIBLE);
            bottomFillerView.setVisibility(View.GONE);
            bottomSheetBehavior.setState(BottomSheetBehavior.STATE_COLLAPSED);
            bottomSheetBehavior.setHideable(false);

            // hide keyboard
            View view = activity.getCurrentFocus();
            if (view != null) {
                InputMethodManager imm = (InputMethodManager) activity.getSystemService(Context.INPUT_METHOD_SERVICE);
                imm.hideSoftInputFromWindow(view.getWindowToken(), 0);
            }
        } else {
            callPatientAddLayout.setVisibility(View.VISIBLE);
            callPatientShowAddIcon.setVisibility(View.GONE);
            bottomFillerView.setVisibility(View.VISIBLE);
            bottomSheetBehavior.setHideable(true);
            bottomSheetBehavior.setState(BottomSheetBehavior.STATE_HIDDEN);
        }
    }

    private void addPatient() {
        new SimpleAlertDialog(requireActivity(), getString(R.string.alert_warning_title))
                .alert(getString(R.string.notImplementedYet));
    }

}
