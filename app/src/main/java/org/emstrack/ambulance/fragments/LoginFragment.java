package org.emstrack.ambulance.fragments;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.text.SpannableString;
import android.text.method.LinkMovementMethod;
import android.text.util.Linkify;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.fragment.app.Fragment;
import androidx.localbroadcastmanager.content.LocalBroadcastManager;

import org.emstrack.ambulance.MainActivity;
import org.emstrack.ambulance.R;
import org.emstrack.ambulance.dialogs.AlertDialog;
import org.emstrack.ambulance.dialogs.AlertSnackbar;
import org.emstrack.ambulance.models.AmbulanceAppData;
import org.emstrack.ambulance.services.AmbulanceForegroundService;
import org.emstrack.models.Credentials;
import org.emstrack.models.util.BroadcastActions;
import org.emstrack.models.util.OnServiceComplete;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

public class LoginFragment extends Fragment {

    private static final String TAG = LoginFragment.class.getSimpleName();

    private Button loginSubmitButton;
    private TextView usernameField;
    private TextView passwordField;
    private Spinner serverField;
    private boolean logout;

    private ArrayAdapter<CharSequence> serverNames;
    private List<String> serverMqttURIs;
    private List<String> serverAPIURIs;
    private MainActivity activity;
    private Button loginAsDemoButton;


    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {

        View rootView = inflater.inflate(R.layout.fragment_login, container, false);

        // get activity
        activity = (MainActivity) requireActivity();

        // Find username and password from layout
        usernameField = rootView.findViewById(R.id.editUserName);
        passwordField = rootView.findViewById(R.id.editPassword);

        // Retrieve stored list of servers
        serverNames = new ArrayAdapter<>(this.requireContext(), android.R.layout.simple_spinner_item);
        serverMqttURIs = new ArrayList<>();
        serverAPIURIs = new ArrayList<>();

        // add select server message
        serverNames.add(this.getString(R.string.server_select));
        serverMqttURIs.add("");
        serverAPIURIs.add("");

        // Create server spinner
        serverField = rootView.findViewById(R.id.spinnerServer);
        serverNames.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        serverField.setAdapter(serverNames);

        // Retrieving credentials
        SharedPreferences sharedPreferences =
                activity.getSharedPreferences(AmbulanceForegroundService.PREFERENCES_NAME, AppCompatActivity.MODE_PRIVATE);

        // Retrieve past credentials
        usernameField.setText(sharedPreferences.getString(AmbulanceForegroundService.PREFERENCES_USERNAME, null));
        passwordField.setText(sharedPreferences.getString(AmbulanceForegroundService.PREFERENCES_PASSWORD, null));

        // Submit button
        loginSubmitButton = rootView.findViewById(R.id.buttonLogin);
        loginSubmitButton.setOnClickListener(new ClickListener());

        loginAsDemoButton = rootView.findViewById(R.id.buttonDemoLogin);
        loginAsDemoButton.setOnClickListener(view -> {
            Log.i(TAG, "Login as demo");
            usernameField.setText(R.string.demoUsername);
            passwordField.setText(R.string.demoPassword);
            int serverPos = serverMqttURIs.indexOf(getString(R.string.demoServer));
            if (serverPos >= 0)
                serverField.setSelection(serverPos);

            TextView textView = new TextView(requireContext());
            final SpannableString s =
                    new SpannableString(getString(R.string.demoLoginMessage));
            Linkify.addLinks(s, Linkify.WEB_URLS);
            textView.setText(s);
            textView.setMovementMethod(LinkMovementMethod.getInstance());
            textView.setPadding(50,20,10,32);

            new android.app.AlertDialog.Builder(requireContext())
                    .setTitle("Demo Session")
                    .setPositiveButton(android.R.string.ok, null)
                    .setView(textView)
                    .create()
                    .show();
        });

        return rootView;
    }

    @Override
    public void onResume() {
        super.onResume();

        Log.d(TAG, "onResume");

        // hide action bar and bottom navigation bar
        activity.hideActionBar();
        activity.hideBottomNavigationBar();
        activity.hideNavigationRail();

        // set back button as finish
        activity.setBackButtonMode(MainActivity.BackButtonMode.FINISH);

        // disable login buttons
        loginSubmitButton.setEnabled(false);
        loginAsDemoButton.setEnabled(false);

    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        // enable login
        enableLogin();
    }

    public void setServers(List<String> serverList) {

        Log.i(TAG, "Setting server list");

        // Populate server list
        // Log.d(TAG, "Populating server list");
        serverNames = new ArrayAdapter<>(requireContext(), android.R.layout.simple_spinner_item);
        serverMqttURIs = new ArrayList<>();
        serverAPIURIs = new ArrayList<>();

        // add select server message
        serverNames.add(getString(R.string.server_select));
        serverMqttURIs.add("");
        serverAPIURIs.add("");

        for (String server: serverList) {
            try {
                String[] splits = server.split(":", 3);
                serverNames.add(splits[0]);
                if (!splits[1].isEmpty()) {
                    serverMqttURIs.add("ssl://" + splits[1] + ":" + splits[2]);
                    serverAPIURIs.add("https://" + splits[1]);
                } else {
                    serverMqttURIs.add("");
                    serverAPIURIs.add("");
                }
            } catch (Exception e) {
                Log.d(TAG, "Malformed server string. Skipping.");
            }
        }

        // Create server spinner
        serverNames.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        serverField.setAdapter(serverNames);

        // Retrieving credentials
        SharedPreferences sharedPreferences = activity.getSharedPreferences(
                AmbulanceForegroundService.PREFERENCES_NAME, AppCompatActivity.MODE_PRIVATE);

        // Retrieve past credentials
        String serverMqttUri = sharedPreferences.getString(AmbulanceForegroundService.PREFERENCES_MQTT_SERVER, null);

        // set server item
        int serverPos = 0;
        if (serverMqttUri != null) {
            serverPos = serverMqttURIs.indexOf(serverMqttUri);
        }
        if (serverPos < 0)
            serverPos = 0;
        serverField.setSelection(serverPos);

    }

    private void doEnableLogin(List<String> serverList) {

        Log.d(TAG, "Servers = " + serverList);

        setServers(serverList);

        Log.d(TAG, "Will enable login button");

        // Enable login button
        loginSubmitButton.setEnabled(true);
        loginAsDemoButton.setEnabled(true);

    }

    public void enableLogin() {

        Log.d(TAG, "enableLogin");

        // Already logged in?
        AmbulanceAppData appData = AmbulanceForegroundService.getAppData();
        if (appData != null && appData.getProfile() != null) {

            Log.i(TAG, "Already logged in, starting MainActivity.");

            // Get username
            Credentials credentials = appData.getCredentials();
            if (credentials != null) {

                // final String username = usernameField.getText().toString().trim();
                final String username = credentials.getUsername();

                // Toast
                Toast.makeText(activity,
                        getResources().getString(R.string.loginSuccessMessage, username),
                        Toast.LENGTH_SHORT).show();

                // navigate to map fragment
                navigateToMap();

            } else {

                // Toast
                Toast.makeText(activity,
                        getResources().getString(R.string.couldNotLogin, "unknown"),
                        Toast.LENGTH_SHORT).show();

            }

        } else{

            Log.i(TAG, "Could not find profile, starting service");

            // Initialize service to make sure it gets bound to service
            Intent intent = new Intent(activity,
                    AmbulanceForegroundService.class);
            intent.putExtra("ADD_STOP_ACTION", true);
            intent.setAction(AmbulanceForegroundService.Actions.START_SERVICE);

            // Initialize service to make sure it gets bound to service
            Intent serverIntent = new Intent(activity,
                    AmbulanceForegroundService.class);
            serverIntent.setAction(AmbulanceForegroundService.Actions.GET_SERVERS);

            if (appData != null && appData.getServersList().size() > 0) {

                new OnServiceComplete(activity,
                        BroadcastActions.SUCCESS,
                        BroadcastActions.FAILURE,
                        intent) {

                    @Override
                    public void onSuccess(Bundle extras) {

                        Log.i(TAG, "Successfully started service");

                        // already has servers
                        List<String> serverList = appData.getServersList();
                        doEnableLogin(serverList);

                    }

                }
                        .setFailureMessage(getString(R.string.couldNotStartService))
                        .setAlert(new AlertSnackbar(activity))
                        .start();

            } else {

                new OnServiceComplete(activity,
                        BroadcastActions.SUCCESS,
                        BroadcastActions.FAILURE,
                        intent) {

                    @Override
                    public void onSuccess(Bundle extras) {

                        Log.i(TAG, "Successfully started service");

                    }

                }
                        .setNext(new OnServiceComplete(activity,
                                BroadcastActions.SUCCESS,
                                BroadcastActions.FAILURE,
                                serverIntent) {

                            @Override
                            public void onSuccess(Bundle extras) {

                                Log.d(TAG, "Will set servers dropdown");

                                // Retrieve list of servers
                                AmbulanceAppData appData = AmbulanceForegroundService.getAppData();
                                List<String> serverList = appData.getServersList();

                                doEnableLogin(serverList);

                            }

                            @Override
                            public void onFailure(Bundle extras) {
                                super.onFailure(extras);

                                // Retrieve past servers
                                SharedPreferences sharedPreferences = activity.getSharedPreferences(
                                        AmbulanceForegroundService.PREFERENCES_NAME, AppCompatActivity.MODE_PRIVATE);
                                Set<String> serversSet = sharedPreferences.getStringSet(AmbulanceForegroundService.PREFERENCES_SERVERS, null);
                                ArrayList<String> serverList = null;
                                if (serversSet != null) {

                                    new AlertSnackbar(activity)
                                            .alert("Could not retrieve servers. List of servers may be outdated.");

                                    serverList = new ArrayList<>(serversSet);

                                    doEnableLogin(serverList);

                                } else {

                                    new AlertSnackbar(activity)
                                            .alert("Could not retrieve servers. Check your internet connection.");

                                }
                            }

                        })
                        .setFailureMessage(getString(R.string.couldNotStartService))
                        .setAlert(new AlertSnackbar(activity))
                        .start();
            }
        }

    }

    public class ClickListener implements View.OnClickListener {

        @Override
        public void onClick(View view) {

            // Get user info & remove whitespace
            final String username = usernameField.getText().toString().trim();
            final String password = passwordField.getText().toString().trim();

            final String serverUri = serverMqttURIs.get(serverField.getSelectedItemPosition());
            final String serverApiUri = serverAPIURIs.get(serverField.getSelectedItemPosition());
            Log.d(TAG, "Logging into server: " + serverUri);

            if (username.isEmpty())
                new AlertSnackbar(activity).alert(getResources().getString(R.string.error_empty_username));

            else if (password.isEmpty())
                new AlertSnackbar(activity).alert(getResources().getString(R.string.error_empty_password));

            else if (serverUri.isEmpty())
                new AlertSnackbar(activity).alert(getResources().getString(R.string.error_invalid_server));

            else if (serverApiUri.isEmpty())
                new AlertSnackbar(activity).alert(getResources().getString(R.string.error_invalid_server));

            else {

                Log.d(TAG, "Will offer credentials");

                // Login at service
                Intent intent = new Intent(requireContext(),
                        AmbulanceForegroundService.class);
                intent.setAction(AmbulanceForegroundService.Actions.LOGIN);
                intent.putExtra(AmbulanceForegroundService.BroadcastExtras.CREDENTIALS,
                        new String[]{username, password, serverUri, serverApiUri});

                // disable login button
                loginSubmitButton.setEnabled(false);
                loginAsDemoButton.setEnabled(false);

                // What to do when service completes?
                new OnServiceComplete(requireContext(),
                        BroadcastActions.SUCCESS,
                        BroadcastActions.FAILURE,
                        intent) {

                    @Override
                    public void onSuccess(Bundle extras) {
                        Log.i(TAG, "onClick:OnServiceComplete:onSuccess");

                        // Toast
                        Toast.makeText(activity,
                                getResources().getString(R.string.loginSuccessMessage, username),
                                Toast.LENGTH_SHORT).show();

                        // navigate to map fragment
                        navigateToMap();

                    }

                    @Override
                    public void onFailure(Bundle extras) {
                        super.onFailure(extras);
                        loginSubmitButton.setEnabled(true);
                        loginAsDemoButton.setEnabled(true);
                    }
                }
                        .setFailureMessage(null)
                        .setAlert(new AlertDialog(activity,
                                getResources().getString(R.string.couldNotLoginUser, username)))
                        .start();

            }

        }
    }

    public void navigateToMap() {

        // initialize after login
        activity.initialize();

        // navigate to map fragment
        activity.navigate(R.id.map);

        // show action bar and bottom navigation bar
        activity.showActionBar();
        activity.showBottomNavigationBar();

    }

    /**
     * Get the LocalBroadcastManager
     *
     * @return The system LocalBroadcastManager
     */
    private LocalBroadcastManager getLocalBroadcastManager() {
        return LocalBroadcastManager.getInstance(requireContext());
    }

}
