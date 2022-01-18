package org.emstrack.ambulance.fragments;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import androidx.localbroadcastmanager.content.LocalBroadcastManager;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import org.emstrack.ambulance.MainActivity;
import org.emstrack.ambulance.R;
import org.emstrack.ambulance.adapters.MessageRecyclerAdapter;
import org.emstrack.ambulance.dialogs.SimpleAlertDialog;
import org.emstrack.ambulance.models.AmbulanceAppData;
import org.emstrack.ambulance.models.MessageType;
import org.emstrack.ambulance.services.AmbulanceForegroundService;
import org.emstrack.ambulance.util.ViewTextWatcher;
import org.emstrack.models.Ambulance;
import org.emstrack.models.AmbulanceNote;
import org.emstrack.models.Call;
import org.emstrack.models.CallStack;
import org.emstrack.models.api.APIService;
import org.emstrack.models.api.APIServiceGenerator;
import org.emstrack.models.api.OnAPICallComplete;
import org.emstrack.models.util.BroadcastActions;
import org.emstrack.models.util.OnServiceComplete;

import java.util.List;

public class MessagesFragment extends Fragment {

    private static final String TAG = AmbulanceFragment.class.getSimpleName();
    private MainActivity activity;
    private RecyclerView recyclerView;
    private TextView refreshingData;
    private EditText sendText;
    private ImageView sendIcon;
    private MessageType type;
    private int id;
    private String username;
    private MessagesUpdateBroadcastReceiver receiver;
    private TextView messageType;

    public class MessagesUpdateBroadcastReceiver extends BroadcastReceiver {

        @Override
        public void onReceive(Context context, Intent intent ) {
            if (intent != null) {

                final String action = intent.getAction();
                if (action != null) {
                    switch (action) {
                        case AmbulanceForegroundService.BroadcastActions.CALL_UPDATE:

                            Log.i(TAG, "CALL_UPDATE");
                            refreshData();
                            break;

                        case AmbulanceForegroundService.BroadcastActions.CALL_COMPLETED: {

                            Log.i(TAG, "CALL_COMPLETED");

                            // Toast to warn user
                            Toast.makeText(getContext(), R.string.CallFinished, Toast.LENGTH_LONG).show();

                            // TODO: navigate back to ambulance
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

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {

        View rootView = inflater.inflate(R.layout.fragment_messages, container, false);
        activity = (MainActivity) requireActivity();

        username = AmbulanceForegroundService.getAppData().getCredentials().getUsername();

        messageType = rootView.findViewById(R.id.message_type);
        refreshingData = rootView.findViewById(R.id.message_refreshing_data);
        recyclerView = rootView.findViewById(R.id.messages_recycler_view);
        sendText = rootView.findViewById(R.id.message_send_text);
        sendIcon = rootView.findViewById(R.id.message_send_icon);

        // enable send button only if text edit is not empty
        sendText.addTextChangedListener(new ViewTextWatcher(sendIcon));

        sendIcon.setEnabled(false);
        sendIcon.setOnClickListener(v -> {
            sendMessage();
        });

        // get arguments
        Bundle arguments = getArguments();
        if (arguments != null) {
            type = (MessageType) arguments.getSerializable("type");
            id = getArguments().getInt("id", -1);
        } else {
            type = MessageType.AMBULANCE;
            id = -1;
        }

        return rootView;
    }

    @Override
    public void onResume() {
        super.onResume();

        // setup navigation
        activity.setupNavigationBar(this);

        // Refresh data
        refreshData();

        // Register receiver
        IntentFilter filter = new IntentFilter();
        filter.addAction(AmbulanceForegroundService.BroadcastActions.CALL_UPDATE);
        filter.addAction(AmbulanceForegroundService.BroadcastActions.CALL_COMPLETED);
        receiver = new MessagesUpdateBroadcastReceiver();
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

    private void refreshData() {

        // retrieve equipment
        refreshingData.setVisibility(View.VISIBLE);
        recyclerView.setVisibility(View.GONE);

        if (id == -1) {

            refreshingData.setText(R.string.messageNotAvailable);

        } else {

            refreshingData.setText(R.string.refreshingData);

            if (type == MessageType.CALL) {

                // Get app data
                AmbulanceAppData appData = AmbulanceForegroundService.getAppData();

                // Get calls
                CallStack calls = appData.getCalls();
                Call call = calls.getCurrentCall();

                // set last call as current call
                call.setLastUpdatedOnNote();

                // hide refresh label
                refreshingData.setVisibility(View.GONE);

                // set label
                messageType.setText(R.string.call_messages);
                messageType.setVisibility(View.VISIBLE);

                // Install adapter
                LinearLayoutManager linearLayoutManager = new LinearLayoutManager(getContext());
                MessageRecyclerAdapter adapter =
                        new MessageRecyclerAdapter(requireContext(), call.getCallnoteSet(), username);
                recyclerView.setLayoutManager(linearLayoutManager);
                recyclerView.setAdapter(adapter);
                recyclerView.scrollToPosition(adapter.getItemCount() - 1);

                recyclerView.setVisibility(View.VISIBLE);

            } else { // if (type == MessageType.AMBULANCE)

                APIService service = APIServiceGenerator.createService(APIService.class);
                retrofit2.Call<List<AmbulanceNote>> callGetMessages = service.getAmbulanceNote(id);

                new OnAPICallComplete<List<AmbulanceNote>>(callGetMessages) {

                    @Override
                    public void onSuccess(List<AmbulanceNote> messages) {

                        // hide refresh label
                        refreshingData.setVisibility(View.GONE);

                        // set label
                        Ambulance ambulance = AmbulanceForegroundService.getAppData().getAmbulanceById(id);
                        if (ambulance != null) {
                            messageType.setText(ambulance.getIdentifier());
                            messageType.setVisibility(View.VISIBLE);
                        } else {
                            messageType.setVisibility(View.GONE);
                        }

                        // Install adapter
                        LinearLayoutManager linearLayoutManager = new LinearLayoutManager(getContext());
                        MessageRecyclerAdapter adapter =
                                new MessageRecyclerAdapter(requireContext(), messages, username);
                        recyclerView.setLayoutManager(linearLayoutManager);
                        recyclerView.setAdapter(adapter);
                        recyclerView.scrollToPosition(adapter.getItemCount() - 1);

                        recyclerView.setVisibility(View.VISIBLE);
                    }

                    @Override
                    public void onFailure(Throwable t) {
                        super.onFailure(t);

                        refreshingData.setText(getString(R.string.couldNotRetrieveMessages, getString(R.string.ambulance)));
                        messageType.setVisibility(View.GONE);
                        sendIcon.setEnabled(false);
                        sendText.setEnabled(false);

                    }
                }
                        .start();

            }
        }
    }

    private void sendMessage() {
        Log.d(TAG, "Will send message");

        String message = sendText.getText().toString().trim();

        if (type == MessageType.CALL) {

            Log.d(TAG, "Message is a call note");

            Intent intent = new Intent(activity, AmbulanceForegroundService.class);
            intent.setAction(AmbulanceForegroundService.Actions.CALLNOTE_CREATE);
            Bundle bundle = new Bundle();
            bundle.putString(AmbulanceForegroundService.BroadcastExtras.CALLNOTE_COMMENT, message);
            bundle.putInt(AmbulanceForegroundService.BroadcastExtras.CALL_ID, id);
            intent.putExtras(bundle);

            // post call note on server
            sendText.setEnabled(false);
            sendIcon.setEnabled(false);
            new OnServiceComplete(activity,
                    BroadcastActions.SUCCESS,
                    BroadcastActions.FAILURE,
                    intent) {

                @Override
                public void onSuccess(Bundle extras) {
                    // clear up send text
                    sendText.setText("");
                    sendText.setEnabled(true);
                }

                @Override
                public void onFailure(Bundle extras) {
                    super.onFailure(extras);
                    sendText.setEnabled(true);
                    sendIcon.setEnabled(true);
                }

            }       .setFailureMessage(null)
                    .setAlert(new SimpleAlertDialog(activity,
                            getResources().getString(R.string.couldNotSendMessage)))
                    .start();

        } else { // if (type == MessageType.AMBULANCE)

            Log.d(TAG, "Message is am ambulance note");

        }

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
