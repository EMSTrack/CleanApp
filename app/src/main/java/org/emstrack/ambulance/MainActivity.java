package org.emstrack.ambulance;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.res.Configuration;
import android.graphics.PorterDuff;
import android.os.Bundle;
import android.support.design.widget.NavigationView;
import android.support.design.widget.TabLayout;
import android.support.v4.app.Fragment;
import android.support.v4.content.ContextCompat;
import android.support.v4.content.LocalBroadcastManager;
import android.support.v4.view.ViewPager;
import android.support.v4.widget.DrawerLayout;
import android.support.v7.app.ActionBarDrawerToggle;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.MenuItem;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import org.emstrack.ambulance.adapters.FragmentPager;
import org.emstrack.ambulance.dialogs.AboutDialog;
import org.emstrack.ambulance.dialogs.AlertSnackbar;
import org.emstrack.ambulance.dialogs.LogoutDialog;
import org.emstrack.ambulance.fragments.AmbulanceFragment;
import org.emstrack.ambulance.fragments.HospitalFragment;
import org.emstrack.ambulance.fragments.MapFragment;
import org.emstrack.ambulance.services.AmbulanceForegroundService;
import org.emstrack.models.util.BroadcastActions;
import org.emstrack.models.util.OnServiceComplete;
import org.emstrack.models.Ambulance;
import org.emstrack.models.AmbulanceCall;
import org.emstrack.models.AmbulancePermission;
import org.emstrack.models.Call;
import org.emstrack.models.HospitalPermission;
import org.emstrack.models.Location;
import org.emstrack.models.Patient;
import org.emstrack.models.Profile;
import org.emstrack.models.Waypoint;
import org.emstrack.mqtt.MqttProfileClient;

import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * This is the main activity -- the default screen
 */
public class MainActivity extends AppCompatActivity {

    private static final String TAG = MainActivity.class.getSimpleName();

    private static DecimalFormat df = new DecimalFormat();

    private static final float enabledAlpha = 1.0f;
    private static final float disabledAlpha = 0.25f;

    private Button ambulanceButton;

    private List<AmbulancePermission> ambulancePermissions;
    private ArrayAdapter<String> ambulanceListAdapter;
    private List<HospitalPermission> hospitalPermissions;
    private ArrayAdapter<String> hospitalListAdapter;
    private List<Location> baseList;
    private ArrayAdapter<String> baseListAdapter;

    private DrawerLayout mDrawer;
    private ActionBarDrawerToggle drawerToggle;
    private Toolbar toolbar;
    private ImageView onlineIcon;
    private ImageView trackingIcon;
    private MainActivityBroadcastReceiver receiver;
    private Map<String, Integer> callPriorityBackgroundColors;
    private Map<String, Integer> callPriorityForegroundColors;

    private boolean logoutAfterFinish;

    public class AmbulanceButtonClickListener implements View.OnClickListener {

        @Override
        public void onClick(View v) {

            new AlertDialog.Builder(
                    MainActivity.this)
                    .setTitle(R.string.selectAmbulance)
                    .setAdapter(ambulanceListAdapter,
                            (dialog, which) -> {

                                AmbulancePermission selectedAmbulance = ambulancePermissions.get(which);
                                Log.d(TAG, "Selected ambulance " + selectedAmbulance.getAmbulanceIdentifier());

                                // Any ambulance currently selected?
                                Ambulance ambulance = AmbulanceForegroundService.getCurrentAmbulance();

                                // Warn if current ambulance
                                if (ambulance != null) {

                                    Log.d(TAG, "Current ambulance " + ambulance.getIdentifier());
                                    Log.d(TAG, "Requesting location updates? " +
                                            (AmbulanceForegroundService.isUpdatingLocation() ? "TRUE" : "FALSE"));

                                    // If same ambulance, just return
                                    if (ambulance.getId() == selectedAmbulance.getAmbulanceId())
                                        return;

                                    if (AmbulanceForegroundService.isUpdatingLocation()) {

                                        // confirm first
                                        switchAmbulanceDialog(selectedAmbulance);
                                        return;
                                    }

                                }

                                // otherwise go ahead!
                                retrieveAmbulance(selectedAmbulance);
                                dialog.dismiss();

                            })
                    .setOnCancelListener(
                            dialog -> {

                                // Any ambulance currently selected?
                                if (AmbulanceForegroundService.getCurrentAmbulance()== null) {

                                    // User must always choose ambulance
                                    promptMustChooseAmbulance();

                                }

                            })
                    .create()
                    .show();
        }
    }

    public class MainActivityBroadcastReceiver extends BroadcastReceiver {

        @Override
        public void onReceive(Context context, Intent intent ) {

            if (intent != null) {

                final String action = intent.getAction();
                if (action.equals(AmbulanceForegroundService.BroadcastActions.LOCATION_UPDATE_CHANGE)) {

                    Log.i(TAG, "LOCATION_UPDATE_CHANGE");

                    if (AmbulanceForegroundService.isUpdatingLocation())
                        trackingIcon.setAlpha(enabledAlpha);
                    else
                        trackingIcon.setAlpha(disabledAlpha);

                } else if (action.equals(AmbulanceForegroundService.BroadcastActions.CONNECTIVITY_CHANGE)) {

                    Log.i(TAG, "CONNECTIVITY_CHANGE");

                    if (AmbulanceForegroundService.isOnline())
                        onlineIcon.setAlpha(enabledAlpha);
                    else
                        onlineIcon.setAlpha(disabledAlpha);

                } else if (action.equals(AmbulanceForegroundService.BroadcastActions.PROMPT_CALL_ACCEPT)) {

                    Log.i(TAG, "PROMPT_CALL_ACCEPT");

                    if (logoutAfterFinish)
                        // Ignore
                        return;

                    int callId = intent.getIntExtra("CALL_ID", -1);
                    promptAcceptCallDialog(callId);

                } else if (action.equals(AmbulanceForegroundService.BroadcastActions.PROMPT_CALL_END)) {

                    Log.i(TAG, "PROMPT_CALL_END");

                    int callId = intent.getIntExtra("CALL_ID", -1);
                    promptEndCallDialog(callId);

                } else if (action.equals(AmbulanceForegroundService.BroadcastActions.PROMPT_NEXT_WAYPOINT)) {

                    Log.i(TAG, "PROMPT_NEXT_WAYPOINT");

                    int callId = intent.getIntExtra("CALL_ID", -1);
                    promptNextWaypointDialog(callId);

                } else if (action.equals(AmbulanceForegroundService.BroadcastActions.CALL_ACCEPTED)) {

                    Log.i(TAG, "CALL_ACCEPTED");

                    // change button color to red
                    int myVectorColor = ContextCompat.getColor(MainActivity.this, R.color.colorRed);
                    trackingIcon.setColorFilter(myVectorColor, PorterDuff.Mode.SRC_IN);

                } else if (action.equals(AmbulanceForegroundService.BroadcastActions.CALL_COMPLETED)) {

                    Log.i(TAG, "CALL_COMPLETED");

                    // change button color to black
                    int myVectorColor = ContextCompat.getColor(MainActivity.this, R.color.colorBlack);
                    trackingIcon.setColorFilter(myVectorColor, PorterDuff.Mode.SRC_IN);

                    // Logout?
                    if (logoutAfterFinish) {
                        logoutAfterFinish = false;
                        LogoutDialog.newInstance(MainActivity.this).show();
                    }

                }
            }
        }
    }

    /**
     * @param savedInstanceState
     */
    @Override
    protected void onCreate(Bundle savedInstanceState) {

        super.onCreate(savedInstanceState);

        Log.d(TAG, "onCreate");

        // Do not logout
        logoutAfterFinish = false;

        // Call priority colors
        String[] callPriorityColorsArray = getResources().getStringArray(R.array.call_priority_colors);
        callPriorityBackgroundColors = new HashMap<>();
        callPriorityForegroundColors = new HashMap<>();
        for (String colors: callPriorityColorsArray) {
            try {
                String[] splits = colors.split(":", 3);
                String priority = splits[0];
                callPriorityBackgroundColors.put(priority,
                        ContextCompat.getColor(getApplicationContext(),
                                getResources().getIdentifier(splits[1],"colors", getPackageName())));
                callPriorityForegroundColors.put(priority,
                        ContextCompat.getColor(getApplicationContext(),
                                getResources().getIdentifier(splits[2],"colors", getPackageName())));
            } catch (Exception e) {
                Log.d(TAG, "Malformed color string '" + colors + "'. Skipping. Exception: " + e);
            }
        }
        Log.d(TAG, callPriorityBackgroundColors.toString());
        Log.d(TAG, callPriorityForegroundColors.toString());

        // set formatter
        df.setMaximumFractionDigits(3);

        // set content view
        setContentView(R.layout.activity_main);

        // Ambulance button
        ambulanceButton = findViewById(R.id.ambulanceButton);

        // Panic button
        ImageButton panicButton = findViewById(R.id.panicButton);
        panicButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                panicPopUp();
            }
        });

        // Set a Toolbar to replace the ActionBar.
        toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        // getSupportActionBar().setDisplayShowTitleEnabled(false);

        // Find our drawer view
        mDrawer = findViewById(R.id.drawer_layout);
        drawerToggle = setupDrawerToggle();
        mDrawer.addDrawerListener(drawerToggle);

        // set hamburger color to be black
        drawerToggle.getDrawerArrowDrawable().setColor(getResources().getColor(R.color.colorBlack));

        // Find our drawer view
        NavigationView nvDrawer = findViewById(R.id.nvView);

        // Setup drawer view
        setupDrawerContent(nvDrawer);

        // pager
        ViewPager viewPager = findViewById(R.id.pager);

        // Setup Adapter for tabLayout
        FragmentPager adapter = new FragmentPager(getSupportFragmentManager(),
                new Fragment[]{new AmbulanceFragment(), new MapFragment(), new HospitalFragment()},
                new CharSequence[]{getString(R.string.ambulance),
                        getString(R.string.map),
                        getString(R.string.hospitals)});
        viewPager.setAdapter(adapter);

        //set up TabLayout Structure
        TabLayout tabLayout = (TabLayout) findViewById(R.id.tab_layout_home);
        tabLayout.setTabGravity(TabLayout.GRAVITY_FILL);
        tabLayout.setupWithViewPager(viewPager);
        tabLayout.getTabAt(0).setIcon(R.drawable.ic_ambulance);
        tabLayout.getTabAt(1).setIcon(R.drawable.ic_globe);
        tabLayout.getTabAt(2).setIcon(R.drawable.ic_hospital);

        // Online icon
        onlineIcon = findViewById(R.id.onlineIcon);
        if (AmbulanceForegroundService.isOnline())
            onlineIcon.setAlpha(enabledAlpha);
        else
            onlineIcon.setAlpha(disabledAlpha);

        // Tracking icon
        trackingIcon = findViewById(R.id.trackingIcon);
        // trackingIcon.setOnClickListener(new TrackingClickListener());

        if (AmbulanceForegroundService.isUpdatingLocation()) {
            trackingIcon.setAlpha(enabledAlpha);
        } else {
            trackingIcon.setAlpha(disabledAlpha);
        }

        // Set up ambulance and hospital spinner
        try {

            ambulancePermissions = new ArrayList<>();
            hospitalPermissions = new ArrayList<>();
            MqttProfileClient profileClient = AmbulanceForegroundService.getProfileClient();
            Profile profile = profileClient.getProfile();
            if (profile != null) {
                ambulancePermissions = profile.getAmbulances();
                hospitalPermissions = profile.getHospitals();
            }

            // Creates list of ambulance names
            ArrayList<String> ambulanceList = new ArrayList<>();
            for (AmbulancePermission ambulancePermission : ambulancePermissions)
                ambulanceList.add(ambulancePermission.getAmbulanceIdentifier());

            // Create the adapter
            ambulanceListAdapter = new ArrayAdapter<>(this,
                    android.R.layout.simple_spinner_dropdown_item, ambulanceList);
            ambulanceListAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);

            // Set the ambulance button's adapter
            ambulanceButton.setOnClickListener(new AmbulanceButtonClickListener());

            // Creates list of hospital names
            ArrayList<String> hospitalList = new ArrayList<>();
            hospitalList.add(getString(R.string.selectHospital));
            for (HospitalPermission hospitalPermission : hospitalPermissions)
                hospitalList.add(hospitalPermission.getHospitalName());

            // Create the adapter
            hospitalListAdapter = new ArrayAdapter<>(this,
                    android.R.layout.simple_spinner_dropdown_item, hospitalList);
            hospitalListAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);

            // Creates list of base names
            ArrayList<String> baseList = new ArrayList<>();
            baseList.add(getString(R.string.selectBase));
            // for (HospitalPermission hospitalPermission : hospitalPermissions)
            //    hospitalList.add(hospitalPermission.getHospitalName());

            // Create the adapter
            baseListAdapter = new ArrayAdapter<>(this,
                    android.R.layout.simple_spinner_dropdown_item, baseList);
            baseListAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);

            // Any ambulance currently selected?
            Ambulance ambulance = AmbulanceForegroundService.getCurrentAmbulance();
            if (ambulance != null) {

                // Set button
                setAmbulanceButtonText(ambulance.getIdentifier());

                // Automatically attempting to start streaming
                Log.i(TAG, "Attempting to start streaming");
                startUpdatingLocation();

            } else {

                // Invoke ambulance selection
                ambulanceButton.performClick();

            }

        } catch (AmbulanceForegroundService.ProfileClientException e ){
            Log.e(TAG, "Could not retrieve list of ambulances and hospitals from profile.");
        }

    }

    @Override
    protected void onPostCreate(Bundle savedInstanceState) {
        super.onPostCreate(savedInstanceState);
        // Sync the toggle state after onRestoreInstanceState has occurred.
        drawerToggle.syncState();
    }

    @Override
    public void onConfigurationChanged(Configuration newConfig) {
        super.onConfigurationChanged(newConfig);

        // Pass any configuration change to the drawer toggles
        drawerToggle.onConfigurationChanged(newConfig);

    }

    @Override
    public void onResume() {
        super.onResume();

        // Update location icon
        if (AmbulanceForegroundService.isUpdatingLocation())
            trackingIcon.setAlpha(enabledAlpha);
        else
            trackingIcon.setAlpha(disabledAlpha);

        // Online icon
        onlineIcon = (ImageView) findViewById(R.id.onlineIcon);
        if (AmbulanceForegroundService.isOnline())
            onlineIcon.setAlpha(enabledAlpha);
        else
            onlineIcon.setAlpha(disabledAlpha);

        // Register receiver
        IntentFilter filter = new IntentFilter();
        filter.addAction(AmbulanceForegroundService.BroadcastActions.LOCATION_UPDATE_CHANGE);
        filter.addAction(AmbulanceForegroundService.BroadcastActions.CONNECTIVITY_CHANGE);
        filter.addAction(AmbulanceForegroundService.BroadcastActions.PROMPT_CALL_ACCEPT);
        filter.addAction(AmbulanceForegroundService.BroadcastActions.PROMPT_CALL_END);
        filter.addAction(AmbulanceForegroundService.BroadcastActions.PROMPT_NEXT_WAYPOINT);
        filter.addAction(AmbulanceForegroundService.BroadcastActions.CALL_ACCEPTED);
        filter.addAction(AmbulanceForegroundService.BroadcastActions.CALL_DECLINED);
        filter.addAction(AmbulanceForegroundService.BroadcastActions.CALL_COMPLETED);
        receiver = new MainActivityBroadcastReceiver();
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

    public void retrieveAmbulance(final AmbulancePermission selectedAmbulance) {

        // Retrieve ambulance
        Intent ambulanceIntent = new Intent(this, AmbulanceForegroundService.class);
        ambulanceIntent.setAction(AmbulanceForegroundService.Actions.GET_AMBULANCE);
        ambulanceIntent.putExtra("AMBULANCE_ID", selectedAmbulance.getAmbulanceId());

        // What to do when GET_AMBULANCE service completes?
        new OnServiceComplete(this,
                BroadcastActions.SUCCESS,
                BroadcastActions.FAILURE,
                ambulanceIntent) {

            @Override
            public void onSuccess(Bundle extras) {

                // Start MainActivity
                setAmbulanceButtonText(selectedAmbulance.getAmbulanceIdentifier());

                // Start updating
                startUpdatingLocation();

            }

        }
                .setFailureMessage(getResources().getString(R.string.couldNotRetrieveAmbulance,
                        selectedAmbulance.getAmbulanceIdentifier()))
                .setAlert(new AlertSnackbar(this))
                .start();

    }

    public boolean canWrite() {

        Ambulance ambulance = AmbulanceForegroundService.getCurrentAmbulance();

        // has ambulance?
        if (ambulance == null)
            return false;

        // can write?
        boolean canWrite = false;

        try {

            final MqttProfileClient profileClient = AmbulanceForegroundService.getProfileClient();
            for (AmbulancePermission permission : profileClient.getProfile().getAmbulances()) {
                if (permission.getAmbulanceId() == ambulance.getId()) {
                    if (permission.isCanWrite()) {
                        canWrite = true;
                    }
                    break;
                }
            }

        } catch (AmbulanceForegroundService.ProfileClientException e) {

            /* no need to do anything */
            Log.e(TAG, "Failed to retrieveObject ambulance read/write permissions.");

        }

        return canWrite;

    }

    /**
     * Set ambulance text
     *
     * @param ambulance the ambulance
     */
    public void setAmbulanceButtonText(String ambulance) {
        // Set ambulance selection button
        ambulanceButton.setText(ambulance);
    }

    // Hamburger Menu setup
    private ActionBarDrawerToggle setupDrawerToggle() {
        return new ActionBarDrawerToggle(this, mDrawer, toolbar,
                R.string.drawer_open,
                R.string.drawer_close);
    }

    // Hamburger Menu Listener
    private void setupDrawerContent(NavigationView navigationView) {
        navigationView.setNavigationItemSelectedListener(
                menuItem -> {
                    selectDrawerItem(menuItem);
                    return true;
                });
    }

    // Start selected activity in Hamburger
    public void selectDrawerItem(MenuItem menuItem) {

        // Close drawer
        mDrawer.closeDrawers();

        // Get menuitem
        int itemId = menuItem.getItemId();

        // Actions
        if (itemId == R.id.logout) {

            promptLogout();

        } else if (itemId == R.id.about) {

            AboutDialog.newInstance(this).show();

        } else if (itemId == R.id.settings) {

        }

    }

    public void panicPopUp() {

        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setCancelable(true);
        builder.setTitle(R.string.panicTitle);
        builder.setMessage(R.string.panicMessage);
        builder.setPositiveButton(R.string.confirm,
                (dialog, which) -> {
                });
        builder.setNegativeButton(android.R.string.cancel,
                (dialog, which) -> {
                });

        AlertDialog dialog = builder.create();
        dialog.show();
    }

    /**
     * Get LocalBroadcastManager
     *
     * @return the LocalBroadcastManager
     */
    private LocalBroadcastManager getLocalBroadcastManager() {
        return LocalBroadcastManager.getInstance(this);
    }

    public void promptAcceptCallDialog(final int callId) {

        Log.i(TAG, "Creating accept dialog");

        // Gather call details
        Call call = AmbulanceForegroundService.getCall(callId);
        if (call == null) {
            Log.d(TAG, "Invalid call id '" + callId + "'");
            return;
        }

        // Get current ambulance
        Ambulance ambulance = AmbulanceForegroundService.getCurrentAmbulance();
        if (ambulance == null) {
            Log.d(TAG, "Can't find ambulance; should never happen");
            return;
        }

        // Get ambulanceCall
        AmbulanceCall ambulanceCall = call.getAmbulanceCall(ambulance.getId());
        if (ambulanceCall == null) {
            Log.d(TAG, "Can't find ambulanceCall");
            return;
        }

        // Use the Builder class for convenient dialog construction
        AlertDialog.Builder builder = new AlertDialog.Builder(this);

        // Build patient list
        String patientsText = "";
        List<Patient> patients = call.getPatientSet();
        if (patients != null && patients.size() > 0) {
            for (Patient patient : patients) {
                if (!patientsText.isEmpty())
                    patientsText += ", ";
                patientsText += patient.getName();
                if (patient.getAge() != null)
                    patientsText += " (" + patient.getAge() + ")";
            }
        } else
            patientsText = getResources().getString(R.string.noPatientAvailable);

        // Get number of waypoints
        int numberOfWaypoints = ambulanceCall == null ? 0 : ambulanceCall.getWaypointSet().size();

        // Get next incident waypoint
        Waypoint waypoint = ambulanceCall.getNextWaypoint();
        String distanceText;
        String address;
        String waypointType;
        if (waypoint == null) {

            Log.d(TAG,"No next waypoint available");

            // No upcoming waypoint
            distanceText = getString(R.string.nextWaypointNotAvailable);
            address = "---";
            waypointType = "---";

        } else {

            Log.d(TAG,"Will calculate distance");

            // Get location
            Location location = waypoint.getLocation();

            // Get current location
            android.location.Location lastLocation = AmbulanceForegroundService.getLastLocation();

            // Calculate distance to next waypoint
            float distance = -1;
            if (lastLocation != null && location != null) {
                Log.d(TAG, "location = " + lastLocation);
                distance = lastLocation.distanceTo(location.getLocation().toLocation()) / 1000;
            }
            distanceText = getString(R.string.noDistanceAvailable);
            Log.d(TAG,"Distance = " + distance);
            if (distance > 0)
                distanceText = df.format(distance) + " km";

            address = waypoint.getLocation().toString();
            waypointType = Location.typeLabel.get(location.getType());

        }

        // Create call view
        View view = getLayoutInflater().inflate(R.layout.call_dialog, null);

        Button callPriorityButton = view.findViewById(R.id.callPriorityButton);
        callPriorityButton.setText(call.getPriority());
        callPriorityButton.setBackgroundColor(callPriorityBackgroundColors.get(call.getPriority()));
        callPriorityButton.setTextColor(callPriorityForegroundColors.get(call.getPriority()));

        ((TextView) view.findViewById(R.id.callPriorityLabel)).setText(R.string.nextCall);

        ((TextView) view.findViewById(R.id.callDetailsText)).setText(call.getDetails());
        ((TextView) view.findViewById(R.id.callPatientsText)).setText(patientsText);
        ((TextView) view.findViewById(R.id.callNumberWaypointsText)).setText(String.valueOf(numberOfWaypoints));

        ((TextView) view.findViewById(R.id.callWaypointTypeText)).setText(waypointType);
        ((TextView) view.findViewById(R.id.callDistanceText)).setText(distanceText);
        ((TextView) view.findViewById(R.id.callAddressText)).setText(address);

        if (waypoint == null)

            // Make callNextWaypointLayout invisible
            view.findViewById(R.id.callNextWaypointLayout).setVisibility(View.GONE);

        else
            // Make callNextWaypointLayout visible
            view.findViewById(R.id.callNextWaypointLayout).setVisibility(View.VISIBLE);

        // build dialog
        builder.setTitle(R.string.acceptCall)
                .setView(view)
                .setPositiveButton(R.string.accept,
                        (dialog, id) -> {

                            Toast.makeText(MainActivity.this,
                                    R.string.callAccepted,
                                    Toast.LENGTH_SHORT).show();

                            Log.i(TAG, "Call accepted");

                            Intent serviceIntent = new Intent(MainActivity.this,
                                    AmbulanceForegroundService.class);
                            serviceIntent.setAction(AmbulanceForegroundService.Actions.CALL_ACCEPT);
                            serviceIntent.putExtra("CALL_ID", callId);
                            startService(serviceIntent);

                        })
                .setNegativeButton(R.string.decline,
                        (dialog, id) -> {

                            Toast.makeText(MainActivity.this,
                                    R.string.callDeclined,
                                    Toast.LENGTH_SHORT).show();

                            Log.i(TAG, "Call declined");

                            Intent serviceIntent = new Intent(MainActivity.this,
                                    AmbulanceForegroundService.class);
                            serviceIntent.setAction(AmbulanceForegroundService.Actions.CALL_DECLINE);
                            serviceIntent.putExtra("CALL_ID", callId);
                            startService(serviceIntent);

                        });

        // Create the AlertDialog object and display it
        builder.create().show();

    }

    public void promptEndCallDialog(int callId) {

        Log.d(TAG, "Creating end call dialog");

        // Gather call details
        final Call call = AmbulanceForegroundService.getCurrentCall();
        if (call == null) {

            // Not currently handling call
            Log.d(TAG, "Not currently handling call");
            return;

        } else if (call.getId() != callId) {

            // Not currently handling this call
            Log.d(TAG, "Not currently handling call " + call.getId());
            return;

        }

        // Use the Builder class for convenient dialog construction
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle(R.string.currentlyHandlingCall)
                .setMessage(R.string.whatDoYouWantToDo)
                .setNegativeButton(R.string.toContinue,
                        (dialog, id) -> {

                            Log.i(TAG, "Continuing with call");

                            if (logoutAfterFinish)
                                logoutAfterFinish = false;

                        })
                .setNeutralButton(R.string.suspend,
                        (dialog, id) -> {

                            Toast.makeText(MainActivity.this,
                                    R.string.suspendingCall,
                                    Toast.LENGTH_SHORT).show();

                            Log.i(TAG, "Suspending call");

                            Intent serviceIntent = new Intent(MainActivity.this,
                                    AmbulanceForegroundService.class);
                            serviceIntent.setAction(AmbulanceForegroundService.Actions.CALL_SUSPEND);
                            serviceIntent.putExtra("CALL_ID", call.getId());
                            startService(serviceIntent);

                        })
                .setPositiveButton(R.string.end,
                        (dialog, id) -> {

                            Toast.makeText(MainActivity.this,
                                    R.string.endingCall,
                                    Toast.LENGTH_SHORT).show();

                            Log.i(TAG, "Ending call");

                            Intent serviceIntent = new Intent(MainActivity.this,
                                    AmbulanceForegroundService.class);
                            serviceIntent.setAction(AmbulanceForegroundService.Actions.CALL_FINISH);
                            startService(serviceIntent);

                        });

        // Create the AlertDialog object and return it
        builder.create().show();
    }

    public void promptNextWaypointDialog(final int callId) {

        Log.i(TAG, "Creating next waypoint dialog");

        // Gather call details
        final Call call = AmbulanceForegroundService.getCurrentCall();
        if (call == null) {

            // Not currently handling call
            Log.d(TAG, "Not currently handling call");
            return;

        } else if (call.getId() != callId) {

            // Not currently handling this call
            Log.d(TAG, "Not currently handling call " + call.getId());
            return;

        }

        // Get current ambulance
        final Ambulance ambulance = AmbulanceForegroundService.getCurrentAmbulance();
        if (ambulance == null) {
            Log.d(TAG, "Can't find ambulance; should never happen");
            return;
        }

        // Get ambulanceCall
        AmbulanceCall ambulanceCall = call.getAmbulanceCall(ambulance.getId());
        if (ambulanceCall == null) {
            Log.d(TAG, "Can't find ambulanceCall");
            return;
        }

        // Get waypoints
        final int maximumOrder = ambulanceCall.getNextNewWaypointOrder();

        // Use the Builder class for convenient dialog construction
        AlertDialog.Builder builder = new AlertDialog.Builder(this);

        // Create call view
        View view = getLayoutInflater().inflate(R.layout.next_waypoint_dialog, null);

        // Create hospital spinner
        final Spinner hospitalSpinner = view.findViewById(R.id.spinnerHospitals);
        hospitalSpinner.setAdapter(hospitalListAdapter);

        // Create base spinner
        final Spinner baseSpinner = view.findViewById(R.id.spinnerBases);
        baseSpinner.setAdapter(baseListAdapter);

        // build dialog
        builder.setTitle(R.string.selectNextWaypoint)
                .setView(view)
                .setPositiveButton(R.string.select,
                        (dialog, id) -> {

                            Log.i(TAG, "Waypoint selected");

                            int waypointId = -1;

                            String waypoint = null;
                            int selectedHospital = hospitalSpinner.getSelectedItemPosition();
                            if (selectedHospital > 0) {
                                HospitalPermission hospital = hospitalPermissions.get(selectedHospital - 1);
                                waypoint = "{\"order\":" + maximumOrder + ",\"location\":{\"id\":" + hospital.getHospitalId() + ",\"type\":\"" + Location.TYPE_HOSPITAL + "\"}}";
                            }

                            // Publish waypoint
                            if (waypoint != null) {

                                Intent serviceIntent = new Intent(MainActivity.this, AmbulanceForegroundService.class);
                                serviceIntent.setAction(AmbulanceForegroundService.Actions.WAYPOINT_CREATE);
                                serviceIntent.putExtra("UPDATE", waypoint);
                                serviceIntent.putExtra("WAYPOINT_ID", waypointId);
                                serviceIntent.putExtra("AMBULANCE_ID", ambulance.getId());
                                serviceIntent.putExtra("CALL_ID", callId);
                                startService(serviceIntent);
                            }

                        })
                .setNegativeButton(R.string.cancel,
                        (dialog, id) -> {

                            Log.i(TAG, "No waypoint selected");

                            /*
                            Intent serviceIntent = new Intent(MainActivity.this, AmbulanceForegroundService.class);
                            serviceIntent.setAction(AmbulanceForegroundService.Actions.CALL_DECLINE);
                            serviceIntent.putExtra("CALL_ID", callId);
                            startService(serviceIntent);
                            */

                        })
                .setNeutralButton(R.string.endCall,
                        (dialog, id) -> {

                            Log.i(TAG, "Ending call");

                            promptEndCallDialog(callId);

                        });

        // Create the AlertDialog object and display it
        builder.create().show();

    }

    void startUpdatingLocation() {

        // start updating location

        if (canWrite()) {

            // Toast to warn user
            Toast.makeText(MainActivity.this,
                    R.string.requestToStreamLocation,
                    Toast.LENGTH_SHORT).show();


            // start updating location
            Intent intent = new Intent(MainActivity.this,
                    AmbulanceForegroundService.class);
            intent.setAction(AmbulanceForegroundService.Actions.START_LOCATION_UPDATES);

            new OnServiceComplete(MainActivity.this,
                    BroadcastActions.SUCCESS,
                    BroadcastActions.FAILURE,
                    intent) {

                @Override
                public void onSuccess(Bundle extras) {

                    // Toast to warn user
                    Toast.makeText(MainActivity.this,
                            R.string.startedStreamingLocation,
                            Toast.LENGTH_SHORT).show();

                }

                @Override
                public void onFailure(Bundle extras) {

                    // Otherwise ask user if wants to force
                    AlertDialog.Builder alertDialogBuilder = new AlertDialog.Builder(MainActivity.this);
                    alertDialogBuilder.setTitle(R.string.alert_warning_title)
                            .setCancelable(false)
                            .setMessage(R.string.forceLocationUpdates)
                            .setNegativeButton(
                                    R.string.cancel,
                                    (dialog, which) -> {

                                        // User must always choose ambulance
                                        promptMustChooseAmbulance();

                                    })
                            .setPositiveButton(
                                    R.string.ok,
                                    (dialog, which) -> {

                                        Log.i(TAG, "ForceLocationUpdatesDialog: OK Button Clicked");

                                        Ambulance ambulance = AmbulanceForegroundService.getCurrentAmbulance();
                                        if (ambulance == null) {

                                            Log.d(TAG, "Could not force updates.");

                                            // Toast to warn user
                                            Toast.makeText(MainActivity.this,
                                                    R.string.couldNotForceUpdates,
                                                    Toast.LENGTH_LONG).show();

                                            return;

                                        }

                                        // Toast to warn user
                                        Toast.makeText(MainActivity.this,
                                                R.string.forcingLocationUpdates,
                                                Toast.LENGTH_LONG).show();

                                        // Reset location_client
                                        String payload = "{\"location_client_id\":\"\"}";
                                        Intent intent1 = new Intent(MainActivity.this, AmbulanceForegroundService.class);
                                        intent1.setAction(AmbulanceForegroundService.Actions.UPDATE_AMBULANCE);
                                        Bundle bundle = new Bundle();
                                        bundle.putInt("AMBULANCE_ID", ambulance.getId());
                                        bundle.putString("UPDATE", payload);
                                        intent1.putExtras(bundle);

                                        // What to do when service completes?
                                        new OnServiceComplete(MainActivity.this,
                                                AmbulanceForegroundService.BroadcastActions.AMBULANCE_UPDATE,
                                                BroadcastActions.FAILURE,
                                                intent1) {

                                            @Override
                                            public void onSuccess(Bundle extras1) {
                                                Log.i(TAG, "onSuccess");

                                                // Toast to warn user
                                                Toast.makeText(MainActivity.this,
                                                        R.string.succeededForcingLocationUpdates,
                                                        Toast.LENGTH_LONG).show();

                                                // Start updating locations
                                                startUpdatingLocation();

                                            }

                                        }
                                                .setFailureMessage(getString(R.string.couldNotForceLocationUpdate))
                                                .setAlert(new AlertSnackbar(MainActivity.this))
                                                .setSuccessIdCheck(false) // AMBULANCE_UPDATE will have a different UUID
                                                .start();

/*
                                        // What to do when service completes?
                                        new OnServicesComplete(MainActivity.this,
                                                new String[]{
                                                        BroadcastActions.SUCCESS,
                                                        AmbulanceForegroundService.BroadcastActions.AMBULANCE_UPDATE
                                                },
                                                new String[]{BroadcastActions.FAILURE},
                                                intent) {

                                            @Override
                                            public void onSuccess(Bundle extras) {
                                                Log.i(TAG, "onSuccess");

                                                // Toast to warn user
                                                Toast.makeText(MainActivity.this,
                                                        R.string.succeededForcingLocationUpdates,
                                                        Toast.LENGTH_LONG).show();

                                                // Start updating locations
                                                startUpdatingLocation();

                                            }

                                            @Override
                                            public void onReceive(Context context, Intent intent) {

                                                // Retrieve action
                                                String action = intent.getAction();

                                                // Intercept success
                                                if (action.equals(BroadcastActions.SUCCESS))
                                                    // prevent propagation, still waiting for AMBULANCE_UPDATE
                                                    return;

                                                // Intercept AMBULANCE_UPDATE
                                                if (action.equals(AmbulanceForegroundService.BroadcastActions.AMBULANCE_UPDATE))
                                                    // Inject uuid into AMBULANCE_UPDATE
                                                    intent.putExtra(BroadcastExtras.UUID, getUuid());

                                                // Call super
                                                super.onReceive(context, intent);
                                            }

                                        }
                                                .setFailureMessage(getString(R.string.couldNotForceLocationUpdate))
                                                .setAlert(new AlertSnackbar(MainActivity.this));
*/

                                    });

                    // Create and show dialog
                    alertDialogBuilder.create().show();

                }

            }
                    .setFailureMessage(getResources().getString(R.string.anotherClientIsStreamingLocations))
                    .setAlert(new AlertSnackbar(MainActivity.this))
                    .start();

        } else {

            // Toast to warn user
            Toast.makeText(MainActivity.this,
                    R.string.cantModifyAmbulance,
                    Toast.LENGTH_LONG).show();

        }

    }

    void stopUpdatingServer() {

        if (canWrite()) {

            // turn off tracking
            Intent intent = new Intent(MainActivity.this,
                    AmbulanceForegroundService.class);
            intent.setAction(AmbulanceForegroundService.Actions.STOP_LOCATION_UPDATES);
            startService(intent);

            // Toast to warn user
            Toast.makeText(MainActivity.this,
                    R.string.stopedStreamingLocation,
                    Toast.LENGTH_SHORT).show();

        }

    }

    private void switchAmbulanceDialog(final AmbulancePermission newAmbulance) {

        Log.i(TAG, "Creating switch ambulance dialog");

        // Use the Builder class for convenient dialog construction
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle(R.string.switchAmbulance)
                .setMessage(String.format(getString(R.string.switchToAmbulance), newAmbulance.getAmbulanceIdentifier()))
                .setNegativeButton(R.string.no,
                        (dialog, id) -> Log.i(TAG, "Continue with same ambulance"))
                .setPositiveButton(R.string.yes,
                        (dialog, id) -> {

                            Log.d(TAG, String.format("Switching to ambulance %1$s", newAmbulance.getAmbulanceIdentifier()));

                            // stop updating first
                            stopUpdatingServer();

                            // then retrieve new ambulance
                            retrieveAmbulance(newAmbulance);

                        });

        // Create the AlertDialog object and return it
        builder.create().show();

    }

    public void promptLogout() {

        Call call = AmbulanceForegroundService.getCurrentCall();
        if (call == null)

            // Go straight to dialog
            LogoutDialog.newInstance(this).show();

        else {

            // Will ask to logout
            logoutAfterFinish = true;

            // Prompt to end call first
            promptEndCallDialog(call.getId());

        }

    }

    public void promptMustChooseAmbulance() {

        // Use the Builder class for convenient dialog construction
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle(R.string.pleaseSelectAmbulance)
                .setCancelable(false)
                .setMessage(R.string.mustChooseAmbulance)
                .setPositiveButton(R.string.ok,
                        (dialog, id) -> {

                            Log.i(TAG, "Will choose ambulance");

                            // Invoke ambulance selection
                            ambulanceButton.performClick();

                        })
                .setNegativeButton(R.string.logout,
                        (dialog, id) -> {

                            Log.i(TAG, "Will logout");

                            // Start login activity
                            Intent loginIntent = new Intent(MainActivity.this, LoginActivity.class);
                            loginIntent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP|Intent.FLAG_ACTIVITY_CLEAR_TASK);
                            loginIntent.setAction(LoginActivity.LOGOUT);
                            MainActivity.this.startActivity(loginIntent);

                        });

        // Create the AlertDialog object and return it
        builder.create().show();
    }

    @Override
    public void onBackPressed() {

        promptLogout();

    }

    public Map<String, Integer> getCallPriorityBackgroundColors() {
        return callPriorityBackgroundColors;
    }

    public Map<String, Integer> getCallPriorityForegroundColors() {
        return callPriorityForegroundColors;
    }

}