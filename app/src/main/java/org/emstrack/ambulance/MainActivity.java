package org.emstrack.ambulance;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.DialogInterface;
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
import android.widget.AdapterView;
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
import org.emstrack.ambulance.fragments.EquipmentFragment;
import org.emstrack.ambulance.fragments.HospitalFragment;
import org.emstrack.ambulance.fragments.MapFragment;
import org.emstrack.ambulance.models.AmbulanceAppData;
import org.emstrack.ambulance.services.AmbulanceForegroundService;
import org.emstrack.models.Ambulance;
import org.emstrack.models.AmbulanceCall;
import org.emstrack.models.AmbulancePermission;
import org.emstrack.models.Call;
import org.emstrack.models.CallStack;
import org.emstrack.models.HospitalPermission;
import org.emstrack.models.Location;
import org.emstrack.models.Patient;
import org.emstrack.models.Profile;
import org.emstrack.models.Waypoint;
import org.emstrack.models.util.BroadcastActions;
import org.emstrack.models.util.OnServiceComplete;

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

    private Button ambulanceSelectionButton;

    private List<AmbulancePermission> ambulancePermissions;
    private List<HospitalPermission> hospitalPermissions;
    private List<Location> bases;

    private ArrayAdapter<String> ambulanceListAdapter;
    private ArrayAdapter<String> hospitalListAdapter;
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
    private ViewPager viewPager;

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

                                // If currently handling ambulance
                                Ambulance ambulance = AmbulanceForegroundService.getAppData().getAmbulance();
                                if (ambulance != null) {

                                    Log.d(TAG, "Current ambulance " + ambulance.getIdentifier());
                                    Log.d(TAG, "Requesting location updates? " +
                                            (AmbulanceForegroundService.isUpdatingLocation() ? "TRUE" : "FALSE"));

                                    if (ambulance.getId() != selectedAmbulance.getAmbulanceId())
                                        // If another ambulance, confirm first
                                        switchAmbulanceDialog(selectedAmbulance);

                                    else if (!AmbulanceForegroundService.isUpdatingLocation()) {
                                        // else, if current ambulance is not updating location,
                                        // retrieve again
                                        retrieveAmbulance(selectedAmbulance);
                                        retrieveEquipmentList(selectedAmbulance);
                                    }

                                    // otherwise do nothing

                                } else {

                                    // otherwise go ahead!
                                    retrieveAmbulance(selectedAmbulance);
                                    retrieveEquipmentList(selectedAmbulance);
                                    // dialog.dismiss();

                                }

                            })
                    .setOnCancelListener(
                            dialog -> {

                                // Any ambulance currently selected?
                                if (AmbulanceForegroundService.getAppData().getAmbulance()== null
                                        || !AmbulanceForegroundService.isUpdatingLocation() ) {

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
                switch (action) {
                    case AmbulanceForegroundService.BroadcastActions.LOCATION_UPDATE_CHANGE:

                        Log.i(TAG, "LOCATION_UPDATE_CHANGE");

                        if (AmbulanceForegroundService.isUpdatingLocation())
                            trackingIcon.setAlpha(enabledAlpha);
                        else {
                            trackingIcon.setAlpha(disabledAlpha);

                            // Alert then prompt for new ambulance
                            new org.emstrack.ambulance.dialogs.AlertDialog(MainActivity.this,
                                    getResources()
                                            .getString(R.string.anotherClientIsStreamingLocations))
                                    .alert(getString(R.string.pleaseChooseAnotherAmbulance),
                                            (dialog, which) -> {

                                                // Invoke ambulance selection
                                                ambulanceSelectionButton.performClick();

                                            });

                        }

                        break;
                    case AmbulanceForegroundService.BroadcastActions.CONNECTIVITY_CHANGE:

                        Log.i(TAG, "CONNECTIVITY_CHANGE");

                        if (AmbulanceForegroundService.isOnline())
                            onlineIcon.setAlpha(enabledAlpha);
                        else
                            onlineIcon.setAlpha(disabledAlpha);

                        break;
                    case AmbulanceForegroundService.BroadcastActions.PROMPT_CALL_ACCEPT:

                        Log.i(TAG, "PROMPT_CALL_ACCEPT");

                        if (logoutAfterFinish)
                            // Ignore
                            return;

                        int callId = intent.getIntExtra(AmbulanceForegroundService.BroadcastExtras.CALL_ID, -1);
                        promptCallAccept(callId);

                        break;
                    case AmbulanceForegroundService.BroadcastActions.PROMPT_CALL_END:

                        Log.i(TAG, "PROMPT_CALL_END");

                        callId = intent.getIntExtra(AmbulanceForegroundService.BroadcastExtras.CALL_ID, -1);
                        promptEndCallDialog(callId);

                        break;
                    case AmbulanceForegroundService.BroadcastActions.PROMPT_NEXT_WAYPOINT:

                        Log.i(TAG, "PROMPT_NEXT_WAYPOINT");

                        callId = intent.getIntExtra(AmbulanceForegroundService.BroadcastExtras.CALL_ID, -1);
                        promptNextWaypointDialog(callId);

                        break;
                    case AmbulanceForegroundService.BroadcastActions.CALL_ACCEPTED:

                        Log.i(TAG, "CALL_ACCEPTED");

                        // change button color to red
                        int myVectorColor = ContextCompat.getColor(MainActivity.this, R.color.colorRed);
                        trackingIcon.setColorFilter(myVectorColor, PorterDuff.Mode.SRC_IN);

                        if (viewPager.getCurrentItem() != 0) {
                            // set current pager got ambulance
                            viewPager.setCurrentItem(0);
                        }

                        break;
                    case AmbulanceForegroundService.BroadcastActions.CALL_COMPLETED:

                        Log.i(TAG, "CALL_COMPLETED");

                        // change button color to black
                        myVectorColor = ContextCompat.getColor(MainActivity.this, R.color.colorBlack);
                        trackingIcon.setColorFilter(myVectorColor, PorterDuff.Mode.SRC_IN);

                        // Logout?
                        if (logoutAfterFinish) {
                            logoutAfterFinish = false;
                            LogoutDialog.newInstance(MainActivity.this).show();
                        }

                        break;
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
        ambulanceSelectionButton = findViewById(R.id.ambulanceButton);

        // Panic button
        ImageButton panicButton = findViewById(R.id.panicButton);
        panicButton.setOnClickListener(
                v -> panicPopUp());

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
        viewPager = findViewById(R.id.pager);

        // Setup Adapter for tabLayout
        FragmentPager adapter = new FragmentPager(getSupportFragmentManager(),
                new Fragment[]{new AmbulanceFragment(),
                        new MapFragment(),
                        new HospitalFragment(),
                        new EquipmentFragment()},
                new CharSequence[]{getString(R.string.ambulance),
                        getString(R.string.map),
                        getString(R.string.hospitals),
                        getString(R.string.equipment)});
        viewPager.setAdapter(adapter);

        //set up TabLayout Structure
        TabLayout tabLayout = findViewById(R.id.tab_layout_home);
        tabLayout.setTabGravity(TabLayout.GRAVITY_FILL);
        tabLayout.setupWithViewPager(viewPager);
        //tabLayout.getTabAt(0).setIcon(R.drawable.ic_ambulance);
        tabLayout.getTabAt(0).setCustomView(createView(R.drawable.ic_ambulance));
        tabLayout.getTabAt(1).setCustomView(createView(R.drawable.ic_globe));
        tabLayout.getTabAt(2).setCustomView(createView(R.drawable.ic_hospital));
        tabLayout.getTabAt(3).setCustomView(createView(R.drawable.ic_briefcase_medical));

        // Online icon
        onlineIcon = findViewById(R.id.onlineIcon);
        if (AmbulanceForegroundService.isOnline())
            onlineIcon.setAlpha(enabledAlpha);
        else
            onlineIcon.setAlpha(disabledAlpha);

        // Tracking icon
        trackingIcon = findViewById(R.id.trackingIcon);

        if (AmbulanceForegroundService.isUpdatingLocation()) {
            trackingIcon.setAlpha(enabledAlpha);
        } else {
            trackingIcon.setAlpha(disabledAlpha);
        }

        ambulancePermissions = new ArrayList<>();
        hospitalPermissions = new ArrayList<>();
        AmbulanceAppData appData = AmbulanceForegroundService.getAppData();
        Profile profile = appData.getProfile();
        if (profile != null) {
            ambulancePermissions = profile.getAmbulances();
            hospitalPermissions = profile.getHospitals();
        }
        bases = appData.getBases();

        // Creates list of ambulance names
        ArrayList<String> ambulanceList = new ArrayList<>();
        for (AmbulancePermission ambulancePermission : ambulancePermissions)
            ambulanceList.add(ambulancePermission.getAmbulanceIdentifier());

        // Create the adapter
        ambulanceListAdapter = new ArrayAdapter<>(this,
                android.R.layout.simple_spinner_dropdown_item, ambulanceList);
        ambulanceListAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);

        // Set the ambulance button's adapter
        ambulanceSelectionButton.setOnClickListener(new AmbulanceButtonClickListener());

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
        for (Location base : bases)
            baseList.add(base.getName());

        // Create the adapter
        baseListAdapter = new ArrayAdapter<>(this,
                android.R.layout.simple_spinner_dropdown_item, baseList);
        baseListAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);

        // Any ambulance currently selected?
        Ambulance ambulance = AmbulanceForegroundService.getAppData().getAmbulance();
        if (ambulance != null) {

            // Set button
            setAmbulanceButtonText(ambulance.getIdentifier());

            // Automatically attempting to start streaming
            // Log.i(TAG, "Attempting to start streaming");
            // startUpdatingLocation();

        } else {

            // Invoke ambulance selection
            ambulanceSelectionButton.performClick();

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

        Log.d(TAG, "onResume");

        // Get app data
        AmbulanceAppData appData = AmbulanceForegroundService.getAppData();

        // Update location icon
        if (AmbulanceForegroundService.isUpdatingLocation())
            trackingIcon.setAlpha(enabledAlpha);
        else
            trackingIcon.setAlpha(disabledAlpha);

        // Online icon
        onlineIcon = findViewById(R.id.onlineIcon);
        if (AmbulanceForegroundService.isOnline())
            onlineIcon.setAlpha(enabledAlpha);
        else
            onlineIcon.setAlpha(disabledAlpha);

        // Is there a requested call that needs to be prompted for?
        int nextCallId = -1;
        Ambulance ambulance = appData.getAmbulance();
        if (ambulance != null) {
            CallStack pendingCalls = appData.getCalls();
            Call call = pendingCalls.getCurrentCall();
            if (call == null) {
                Log.d(TAG, "No calls being handled right now.");
                call = pendingCalls.getNextCall(ambulance.getId());
                if (call != null &&
                        AmbulanceCall.STATUS_REQUESTED.equals(call.getAmbulanceCall(ambulance.getId()).getStatus())) {
                    // next call is requested, prompt user!
                    // promptNextCall = true;
                    nextCallId = call.getId();
                }
            }
        }

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

        if (nextCallId > 0)

            // prompt user?
            promptCallAccept(nextCallId);

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
        ambulanceIntent.putExtra(AmbulanceForegroundService.BroadcastExtras.AMBULANCE_ID,
                selectedAmbulance.getAmbulanceId());

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
                // startUpdatingLocation();

            }

        }
                .setFailureMessage(getResources().getString(R.string.couldNotRetrieveAmbulance,
                        selectedAmbulance.getAmbulanceIdentifier()))
                .setAlert(new org.emstrack.ambulance.dialogs.AlertDialog(this,
                        getResources().getString(R.string.couldNotStartLocationUpdates),
                        (dialog, which) -> ambulanceSelectionButton.callOnClick()))
                .start();

    }

    public void retrieveEquipmentList(final AmbulancePermission selectedAmbulance) {
        // retrieve equipment
        Intent equipmentIntent = new Intent(this, AmbulanceForegroundService.class);
        equipmentIntent.setAction(AmbulanceForegroundService.Actions.GET_EQUIPMENT);
        equipmentIntent.putExtra(AmbulanceForegroundService.BroadcastExtras.AMBULANCE_ID,
                selectedAmbulance.getAmbulanceId());

        // What to do when GET_EQUIPMENT service completes?
        new OnServiceComplete(this,
                BroadcastActions.SUCCESS,
                BroadcastActions.FAILURE,
                equipmentIntent) {

            @Override
            public void onSuccess(Bundle extras) {

                // need to take equipment list and stuff it into recycler view


            }

        }
                .setFailureMessage(getResources().getString(R.string.couldNotRetrieveEquipmentList))
                .start();
    }

    public boolean canWrite() {

        Ambulance ambulance = AmbulanceForegroundService.getAppData().getAmbulance();

        // has ambulance?
        if (ambulance == null)
            return false;

        // can write?
        boolean canWrite = false;

        AmbulanceAppData appData = AmbulanceForegroundService.getAppData();
        for (AmbulancePermission permission : appData.getProfile().getAmbulances()) {
            if (permission.getAmbulanceId() == ambulance.getId()) {
                if (permission.isCanWrite()) {
                    canWrite = true;
                }
                break;
            }
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
        ambulanceSelectionButton.setText(ambulance);
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

    public void promptCallAccept(final int callId) {

        Log.i(TAG, "Creating accept dialog");

        // Get app data
        AmbulanceAppData appData = AmbulanceForegroundService.getAppData();

        // Get calls
        CallStack calls = appData.getCalls();

        // Gather call details
        Call call = calls.get(callId);
        if (call == null) {
            Log.d(TAG, "Invalid call id '" + callId + "'");
            return;
        }

        // Get current ambulance
        Ambulance ambulance = appData.getAmbulance();
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
        int numberOfWaypoints = ambulanceCall.getWaypointSet().size();

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

            // Get location
            Location location = waypoint.getLocation();
            address = waypoint.getLocation().toAddress();
            waypointType = appData.getSettings().getLocationType().get(location.getType());

            Log.d(TAG,"Will calculate distance");

            if (AmbulanceForegroundService.hasLastLocation()) {

                // Get current location
                android.location.Location lastLocation = AmbulanceForegroundService.getLastLocation();

                // Calculate distance to next waypoint
                float distance = -1;
                if (lastLocation != null && location != null) {
                    Log.d(TAG, "location = " + lastLocation);
                    distance = lastLocation.distanceTo(location.getLocation().toLocation()) / 1000;
                }
                distanceText = getString(R.string.noDistanceAvailable);
                Log.d(TAG, "Distance = " + distance);
                if (distance > 0)
                    distanceText = df.format(distance) + " km";

            } else {

                distanceText = "---";

            }

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
                .setCancelable(false)
                .setPositiveButton(R.string.accept,
                        (dialog, id) -> {

                            Toast.makeText(MainActivity.this,
                                    R.string.callAccepted,
                                    Toast.LENGTH_SHORT).show();

                            Log.i(TAG, "Call accepted");

                            Intent serviceIntent = new Intent(MainActivity.this,
                                    AmbulanceForegroundService.class);
                            serviceIntent.setAction(AmbulanceForegroundService.Actions.CALL_ACCEPT);
                            serviceIntent.putExtra(AmbulanceForegroundService.BroadcastExtras.CALL_ID, callId);
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
                            serviceIntent.putExtra(AmbulanceForegroundService.BroadcastExtras.CALL_ID, callId);
                            startService(serviceIntent);

                        })
                .setNeutralButton(R.string.suspend,
                        (dialog, which) -> {

                            Toast.makeText(MainActivity.this,
                                    R.string.suspendingCall,
                                    Toast.LENGTH_SHORT).show();

                            Log.i(TAG, "Suspending call");

                            Intent serviceIntent = new Intent(MainActivity.this,
                                    AmbulanceForegroundService.class);
                            serviceIntent.setAction(AmbulanceForegroundService.Actions.CALL_SUSPEND);
                            serviceIntent.putExtra(AmbulanceForegroundService.BroadcastExtras.CALL_ID, callId);
                            startService(serviceIntent);

                        });

        // Create the AlertDialog object and display it
        builder.create().show();

    }

    public void promptEndCallDialog(int callId) {

        Log.d(TAG, "Creating end call dialog");

        // Gather call details
        final Call call = AmbulanceForegroundService.getAppData().getCalls().getCurrentCall();
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
                            serviceIntent.putExtra(AmbulanceForegroundService.BroadcastExtras.CALL_ID, call.getId());
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

        // Get app data
        AmbulanceAppData appData = AmbulanceForegroundService.getAppData();

        // Gather call details
        final Call call = appData.getCalls().getCurrentCall();
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
        final Ambulance ambulance = appData.getAmbulance();
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

        // Set spinner click listeners to make sure only base or hospital are selected
        hospitalSpinner.setOnItemSelectedListener(
                new AdapterView.OnItemSelectedListener() {
                    @Override
                    public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                        if (position > 0 && baseSpinner.getSelectedItemPosition() > 0) {
                            baseSpinner.setSelection(0);
                        }
                    }

                    @Override
                    public void onNothingSelected(AdapterView<?> parent) {

                    }
                }
        );

        baseSpinner.setOnItemSelectedListener(
                new AdapterView.OnItemSelectedListener() {
                    @Override
                    public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                        if (position > 0 && hospitalSpinner.getSelectedItemPosition() > 0) {
                            hospitalSpinner.setSelection(0);
                        }
                    }

                    @Override
                    public void onNothingSelected(AdapterView<?> parent) {

                    }
                }
        );

        // build dialog
        builder.setTitle(R.string.selectNextWaypoint)
                .setView(view)
                .setCancelable(false)
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

                            int selectedBase = baseSpinner.getSelectedItemPosition();
                            if (selectedBase > 0) {
                                Location base = bases.get(selectedBase - 1);
                                Log.d( TAG, "base = " + base);
                                waypoint = "{\"order\":" + maximumOrder + ",\"location\":{\"id\":" + base.getId() + ",\"type\":\"" + Location.TYPE_BASE + "\"}}";
                            }

                            // Publish waypoint
                            if (waypoint != null) {

                                Intent serviceIntent = new Intent(MainActivity.this, AmbulanceForegroundService.class);
                                serviceIntent.setAction(AmbulanceForegroundService.Actions.WAYPOINT_ADD);
                                serviceIntent.putExtra(AmbulanceForegroundService.BroadcastExtras.WAYPOINT_UPDATE, waypoint);
                                serviceIntent.putExtra(AmbulanceForegroundService.BroadcastExtras.WAYPOINT_ID, waypointId);
                                serviceIntent.putExtra(AmbulanceForegroundService.BroadcastExtras.AMBULANCE_ID, ambulance.getId());
                                serviceIntent.putExtra(AmbulanceForegroundService.BroadcastExtras.CALL_ID, callId);
                                startService(serviceIntent);
                            }

                        })
                .setNegativeButton(R.string.cancel,
                        (dialog, id) -> {

                            Log.i(TAG, "No waypoint selected");

                            /*
                            Intent serviceIntent = new Intent(MainActivity.this, AmbulanceForegroundService.class);
                            serviceIntent.setAction(AmbulanceForegroundService.Actions.CALL_DECLINE);
                            serviceIntent.putExtra(AmbulanceForegroundService.BroadcastExtras.CALL_ID, callId);
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

                            // retrieve new ambulance
                            retrieveAmbulance(newAmbulance);

                            // retrieve new equipment list
                            retrieveEquipmentList(newAmbulance);

                        });

        // Create the AlertDialog object and return it
        builder.create().show();

    }

    public void promptLogout() {

        Call call = AmbulanceForegroundService.getAppData().getCalls().getCurrentCall();
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
                            ambulanceSelectionButton.performClick();

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

    public ImageView createView(int resId) {
        ImageView imageView = new ImageView(this);
        imageView.setImageResource(resId);
        imageView.setPadding(0, 20, 0, 20);
        return imageView;
    }

}