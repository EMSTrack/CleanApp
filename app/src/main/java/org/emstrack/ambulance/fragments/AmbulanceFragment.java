package org.emstrack.ambulance.fragments;

import static org.emstrack.ambulance.util.FormatUtils.formatDateTime;

import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Bundle;
import android.util.Log;
import android.util.Pair;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.button.MaterialButton;

import org.emstrack.ambulance.MainActivity;
import org.emstrack.ambulance.R;
import org.emstrack.ambulance.adapters.CallRecyclerAdapter;
import org.emstrack.ambulance.dialogs.SimpleAlertDialog;
import org.emstrack.ambulance.models.AmbulanceAppData;
import org.emstrack.ambulance.models.EquipmentType;
import org.emstrack.ambulance.models.MessageType;
import org.emstrack.ambulance.services.AmbulanceForegroundService;
import org.emstrack.ambulance.util.FragmentWithLocalBroadcastReceiver;
import org.emstrack.models.Ambulance;
import org.emstrack.models.AmbulanceCall;
import org.emstrack.models.Call;
import org.emstrack.models.CallStack;
import org.emstrack.models.Settings;
import org.emstrack.models.util.BroadcastActions;
import org.emstrack.models.util.OnServiceComplete;

import java.text.DateFormat;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class AmbulanceFragment extends FragmentWithLocalBroadcastReceiver {

    private static final String TAG = AmbulanceFragment.class.getSimpleName();

    private MaterialButton ambulanceStatusButton;

    private TextView capabilityText;
    private TextView updatedOnText;
    private TextView commentText;

    private TextView callInformationText;

    private Map<String,String> ambulanceStatus;
    private List<String> ambulanceStatusList;
    private ArrayList<Integer> ambulanceStatusBackgroundColorList;
    private ArrayList<Integer> ambulanceStatusTextColorList;
    private Map<String,String> ambulanceCapabilities;

    private TextView ambulanceLabel;

    private View commentLabel;
    private RecyclerView ambulanceCallRecyclerView;

    @Override
    public void onReceive(Context context, @NonNull Intent intent ) {
        final String action = intent.getAction();
        if (action != null) {

            AmbulanceAppData appData = AmbulanceForegroundService.getAppData();
            Ambulance ambulance = appData.getAmbulance();

            switch (action) {

                case AmbulanceForegroundService.BroadcastActions.AMBULANCE_UPDATE:

                    Log.i(TAG, "AMBULANCE_UPDATE");
                    if (ambulance != null) {
                        updateAmbulance(ambulance);
                    } else {
                        try {
                            // setup navigation bar
                            ((MainActivity) requireActivity()).navigate(R.id.ambulancesFragment);
                        } catch (IllegalStateException e) {
                            Log.d(TAG, "Activity out of context. Ignoring");
                        }
                    }

                    break;

                case AmbulanceForegroundService.BroadcastActions.CALL_DECLINED:

                    Log.i(TAG, "CALL_DECLINED");
                    updateCall(ambulance);
                    break;

                case AmbulanceForegroundService.BroadcastActions.CALL_UPDATE:

                    Log.i(TAG, "CALL_UPDATE");
                    updateCall(ambulance);
                    break;

                case AmbulanceForegroundService.BroadcastActions.CALL_COMPLETED:

                    Log.i(TAG, "CALL_COMPLETED");
                    updateCall(ambulance);

                    try {
                        // setup navigation bar
                        ((MainActivity) requireActivity()).setupNavigationBar();
                    } catch (IllegalStateException e) {
                        Log.d(TAG, "Activity out of context. Ignoring");
                    }

                    break;

                default:
                    Log.i(TAG, "Unknown broadcast action");
            }
        } else {
            Log.i(TAG, "Action is null");
        }

    }

    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {

        Log.d(TAG, "onCreateView");

        // inflate view
        View view = inflater.inflate(R.layout.fragment_ambulance, container, false);
        MainActivity activity = (MainActivity) requireActivity();

        // retrieve ambulance selection button
        ambulanceLabel = view.findViewById(R.id.ambulanceLabel);

        // Retrieve callInformationLayout parts
        callInformationText = view.findViewById(R.id.callInformationText);

        // retrieve call recycler view
        ambulanceCallRecyclerView = view.findViewById(R.id.ambulanceCallRecyclerView);

        // Get appData
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
                                        .getColor(Ambulance.statusBackgroundColorMap.get(entry.getKey())));
                        ambulanceStatusTextColorList
                                .add(getResources()
                                        .getColor(Ambulance.statusTextColorMap.get(entry.getKey())));
                    }
            ambulanceCapabilities = settings.getAmbulanceCapability();
        } else {
            ambulanceStatusList = new ArrayList<>();
        }

        // Other text
        capabilityText = view.findViewById(R.id.capabilityText);
        updatedOnText = view.findViewById(R.id.updatedOnText);
        commentText = view.findViewById(R.id.commentText);
        commentLabel = view.findViewById(R.id.commentLabel);

        // Set login button
        view.findViewById(R.id.ambulanceLogin).setVisibility(View.GONE);

        // Set logout button
        ImageView ambulanceLogoutButton = view.findViewById(R.id.ambulanceLogout);
        ambulanceLogoutButton.setOnClickListener(v -> activity.logoutAmbulance() );

        // Set location button
        ImageView ambulanceLocationButton = view.findViewById(R.id.ambulanceLocation);
        ambulanceLocationButton.setOnClickListener(v -> activity.navigate(R.id.action_ambulance_to_map) );

        // Set equipment button
        ImageView ambulanceEquipmentButton = view.findViewById(R.id.ambulanceEquipment);
        ambulanceEquipmentButton.setOnClickListener(v -> {
            int ambulanceId = AmbulanceForegroundService.getAppData().getAmbulanceId();
            Bundle bundle = new Bundle();
            bundle.putSerializable("type", EquipmentType.AMBULANCE);
            bundle.putInt("id", ambulanceId);
            activity.navigate(R.id.action_ambulance_to_equipment, bundle);
        });

        // set ambulance message button
        ImageView ambulanceMessageButton = view.findViewById(R.id.ambulanceMessage);
        ambulanceMessageButton.setOnClickListener(v -> {
            int ambulanceId = AmbulanceForegroundService.getAppData().getAmbulanceId();
            Bundle bundle = new Bundle();
            bundle.putSerializable("type", MessageType.AMBULANCE);
            bundle.putInt("id", ambulanceId);
            activity.navigate(R.id.action_ambulance_to_messages, bundle);
        });

        // Set status button
        // Create the adapter
        ArrayAdapter<String> ambulanceStatusListAdapter = new ArrayAdapter<>(AmbulanceFragment.this.requireContext(),
                android.R.layout.simple_spinner_dropdown_item,
                ambulanceStatusList);
        ambulanceStatusListAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);

        ambulanceStatusButton = view.findViewById(R.id.statusButton);
        ambulanceStatusButton.setOnClickListener(v -> {

            if (AmbulanceForegroundService.getAppData().getCalls().getCurrentCallId() >= 0) {
                // handling a call, alert and quit

                new SimpleAlertDialog(activity, getString(R.string.alert_warning_title))
                        .alert(getString(R.string.cannotUpdateStatusDuringACall));

                return;
            }

            new AlertDialog.Builder(activity, R.style.ambulance_status_dialog_style)
                    .setTitle(R.string.selectAmbulanceStatus)
                    .setAdapter(ambulanceStatusListAdapter,
                            (dialog, which) -> {
                                // Get selected status
                                Log.i(TAG, String.format("Status at position '%d' selected.", which));

                                // Get selected status
                                String status = ambulanceStatusList.get(which);

                                // Search for entry in ambulanceStatus map
                                String statusCode = "";
                                for (Map.Entry<String, String> entry : ambulanceStatus.entrySet()) {
                                    if (status.equals(entry.getValue())) {
                                        statusCode = entry.getKey();
                                        break;
                                    }
                                }

                                // Update on server
                                Ambulance ambulance = AmbulanceForegroundService.getAppData().getAmbulance();
                                Intent intent = new Intent(getContext(), AmbulanceForegroundService.class);
                                intent.setAction(AmbulanceForegroundService.Actions.UPDATE_AMBULANCE_STATUS);
                                Bundle bundle = new Bundle();
                                bundle.putInt(AmbulanceForegroundService.BroadcastExtras.AMBULANCE_ID, ambulance.getId());
                                bundle.putString(AmbulanceForegroundService.BroadcastExtras.AMBULANCE_STATUS, statusCode);
                                intent.putExtras(bundle);

                                // disable button before updating
                                ambulanceStatusButton.setEnabled(false);

                                new OnServiceComplete(requireContext(),
                                        AmbulanceForegroundService.BroadcastActions.AMBULANCE_UPDATE,
                                        BroadcastActions.FAILURE,
                                        intent) {

                                    @Override
                                    public void onSuccess(Bundle extras) {

                                        Log.d(TAG, "Successfully updated status");

                                        // enable button after update is submitted
                                        ambulanceStatusButton.setEnabled(true);


                                    }

                                    @Override
                                    public void onFailure(Bundle extras) {
                                        super.onFailure(extras);

                                        Log.d(TAG, "Failed to update status");

                                        // enable button after update is submitted
                                        ambulanceStatusButton.setEnabled(true);

                                        new SimpleAlertDialog(activity, getString(R.string.alert_warning_title))
                                                .alert(getString(R.string.couldNotUpdateAmbulanceStatus));

                                    }
                                }
                                        .setSuccessIdCheck(false)
                                        .start();

                            })
                    .setCancelable(true)
                    .create()
                    .show();
        });

        // Update ambulance
        Ambulance ambulance = appData.getAmbulance();
        updateAmbulance(ambulance);
        updateCall(ambulance);

        // Register receiver
        IntentFilter filter = new IntentFilter();
        filter.addAction(AmbulanceForegroundService.BroadcastActions.AMBULANCE_UPDATE);
        filter.addAction(AmbulanceForegroundService.BroadcastActions.CALL_UPDATE);
        filter.addAction(AmbulanceForegroundService.BroadcastActions.CALL_COMPLETED);
        filter.addAction(AmbulanceForegroundService.BroadcastActions.CALL_DECLINED);
        setupReceiver(filter);

        return view;
    }

    @Override
    public void onResume() {
        super.onResume();

        Log.d(TAG, "onResume");

        // get activity
        MainActivity activity = (MainActivity) requireActivity();
        activity.setupNavigationBar();

    }

    public void updateAmbulance(Ambulance ambulance) {

        // quick return if null
        if (ambulance == null) {
            return;
        }

        // set selection button label
        ambulanceLabel.setText(ambulance.getIdentifier());

        // set status button
        setAmbulanceStatusButton(ambulanceStatusList.indexOf(ambulanceStatus.get(ambulance.getStatus())));

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
        updatedOnText.setText(formatDateTime(ambulance.getUpdatedOn(), DateFormat.SHORT));

        // set capability
        capabilityText.setText(ambulanceCapabilities.get(ambulance.getCapability()));

    }

    private void updateCall(Ambulance ambulance) {

        if (ambulance == null) {
            Log.d(TAG, "Ambulance is null");
            return;
        }

        Log.d(TAG,"Updating call information");
        AmbulanceAppData appData = AmbulanceForegroundService.getAppData();

        // get calls
        CallStack calls = appData.getCalls();

        // Set call_current info
        Map<String, Integer> callSummary = calls.summary(appData.getSettings().getAmbulancecallStatus().keySet(), ambulance.getId());
        Log.d(TAG, "Call summary = " + callSummary.toString());

        final String summaryText = getString(R.string.requestedSuspended,
                callSummary.get(AmbulanceCall.STATUS_REQUESTED),
                callSummary.get(AmbulanceCall.STATUS_SUSPENDED));
        callInformationText.setText(summaryText);

        // sort current calls
        List<Pair<Call, AmbulanceCall>> callList = new ArrayList<>(calls.filter(ambulance.getId()).values());

        // Install adapter
        LinearLayoutManager linearLayoutManager = new LinearLayoutManager(requireContext());
        CallRecyclerAdapter adapter =  new CallRecyclerAdapter(getActivity(), callList);
        ambulanceCallRecyclerView.setLayoutManager(linearLayoutManager);
        ambulanceCallRecyclerView.setAdapter(adapter);

    }

    public void setAmbulanceStatusButton(int position) {

        // set status button
        ambulanceStatusButton.setText(ambulanceStatusList.get(position));
        ambulanceStatusButton.setTextColor(ambulanceStatusTextColorList.get(position));
        ambulanceStatusButton.setBackgroundColor(ambulanceStatusBackgroundColorList.get(position));

    }

}