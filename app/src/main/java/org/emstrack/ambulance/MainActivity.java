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
import android.widget.ImageButton;
import android.widget.ImageView;
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
import org.emstrack.ambulance.services.OnServiceComplete;
import org.emstrack.ambulance.services.OnServicesComplete;
import org.emstrack.models.Ambulance;
import org.emstrack.models.AmbulancePermission;
import org.emstrack.models.Call;
import org.emstrack.mqtt.MqttProfileClient;

/**
 * This is the main activity -- the default screen
 */
public class MainActivity extends AppCompatActivity {

    private static final String TAG = MainActivity.class.getSimpleName();

    private static final float enabledAlpha = 1.0f;
    private static final float disabledAlpha = 0.25f;
    private static final int MAX_NUMBER_OF_LOCATION_REQUESTS_ATTEMPTS = 2;

    private ViewPager viewPager;
    private FragmentPager adapter;

    private DrawerLayout mDrawer;
    private NavigationView nvDrawer;
    private ActionBarDrawerToggle drawerToggle;
    private Toolbar toolbar;
    private TextView headerText;
    private ImageButton panicButton;
    private ImageView onlineIcon;
    private ImageView trackingIcon;
    private MainActivityBroadcastReceiver receiver;
    private int requestingToStreamLocation;

    public class TrackingClickListener implements View.OnClickListener {

        @Override
        public void onClick(android.view.View v) {

            if (AmbulanceForegroundService.isUpdatingLocation()) {

                // stop updating location
                stopUpdatingLocation();

            } else {

                // start updating location
                startUpdatingLocation();

            }
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

                    int callId = intent.getIntExtra("CALL_ID", -1);
                    acceptCallDialog(callId);

                } else if (action.equals(AmbulanceForegroundService.BroadcastActions.PROMPT_CALL_END)) {

                    Log.i(TAG, "PROMPT_CALL_END");
                    endCallDialog();

                }
                else if (action.equals(AmbulanceForegroundService.BroadcastActions.CALL_ONGOING)) {

                    Log.i(TAG, "CALL_ONGOING");

                    // change button color to red
                    int myVectorColor = ContextCompat.getColor(MainActivity.this, R.color.colorRed);
                    trackingIcon.setColorFilter(myVectorColor, PorterDuff.Mode.SRC_IN);

                } else if (action.equals(AmbulanceForegroundService.BroadcastActions.CALL_FINISHED)) {

                    Log.i(TAG, "CALL_FINISHED");

                    // change button color to black
                    int myVectorColor = ContextCompat.getColor(MainActivity.this, R.color.colorBlack);
                    trackingIcon.setColorFilter(myVectorColor, PorterDuff.Mode.SRC_IN);

                }
            }
        }
    };

    /**
     * @param savedInstanceState
     */
    @Override
    protected void onCreate(Bundle savedInstanceState) {

        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        // Identifier text
        headerText = (TextView) findViewById(R.id.identifierText);

        // Panic button
        panicButton = (ImageButton) findViewById(R.id.panicButton);
        panicButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                panicPopUp();
            }
        });

        // initialize requestingToStreamLocation
        requestingToStreamLocation = MAX_NUMBER_OF_LOCATION_REQUESTS_ATTEMPTS;

        // Set a Toolbar to replace the ActionBar.
        toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        // getSupportActionBar().setDisplayShowTitleEnabled(false);

        // Find our drawer view
        mDrawer = (DrawerLayout) findViewById(R.id.drawer_layout);
        drawerToggle = setupDrawerToggle();
        mDrawer.addDrawerListener(drawerToggle);

        // set hamburger color to be black
        drawerToggle.getDrawerArrowDrawable().setColor(getResources().getColor(R.color.colorBlack));

        // Find our drawer view
        nvDrawer = (NavigationView) findViewById(R.id.nvView);

        // Setup drawer view
        setupDrawerContent(nvDrawer);

        // pager
        viewPager = (ViewPager) findViewById(R.id.pager);

        // Setup Adapter for tabLayout
        adapter = new FragmentPager(getSupportFragmentManager(),
                new Fragment[] {new AmbulanceFragment(), new HospitalFragment(), new MapFragment()},
                new CharSequence[] {"Ambulance", "Hospitals", "Map"});
        viewPager.setAdapter(adapter);

        //set up TabLayout Structure
        TabLayout tabLayout = (TabLayout) findViewById(R.id.tab_layout_home);
        tabLayout.setTabGravity(TabLayout.GRAVITY_FILL);
        tabLayout.setupWithViewPager(viewPager);
        tabLayout.getTabAt(0).setIcon(R.drawable.ic_ambulance);
        tabLayout.getTabAt(1).setIcon(R.drawable.ic_hospital);
        tabLayout.getTabAt(2).setIcon(R.drawable.ic_globe);

        // Online icon
        onlineIcon = (ImageView) findViewById(R.id.onlineIcon);
        if (AmbulanceForegroundService.isOnline())
            onlineIcon.setAlpha(enabledAlpha);
        else
            onlineIcon.setAlpha(disabledAlpha);

        // Tracking icon
        trackingIcon = (ImageView) findViewById(R.id.trackingIcon);
        trackingIcon.setOnClickListener(new TrackingClickListener());

        if (AmbulanceForegroundService.isUpdatingLocation())
            trackingIcon.setAlpha(enabledAlpha);
        else {
            trackingIcon.setAlpha(disabledAlpha);
            // Automatically attempting to start streaming
            Log.i(TAG, "Attempting to start streaming");
            startUpdatingLocation();
        }

    }

    public boolean canWrite() {

        Ambulance ambulance = AmbulanceForegroundService.getAmbulance();

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
            Log.e(TAG, "Failed to get ambulance read/write permissions.");

        }

        return canWrite;

    }

    /**
     * Set header text
     *
     * @param header the header
     */
    public void setHeader(String header) {
        headerText.setText(header);
    }

    // Hamburger Menu setup
    private ActionBarDrawerToggle setupDrawerToggle() {
        return new ActionBarDrawerToggle(this, mDrawer, toolbar, R.string.drawer_open, R.string.drawer_close);
    }

    // Hamburger Menu Listener
    private void setupDrawerContent(NavigationView navigationView) {
        navigationView.setNavigationItemSelectedListener(
                new NavigationView.OnNavigationItemSelectedListener() {
                    @Override
                    public boolean onNavigationItemSelected(MenuItem menuItem) {
                        selectDrawerItem(menuItem);
                        return true;
                    }
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

            LogoutDialog.newInstance(this).show();

        } else if (itemId == R.id.about) {

            AboutDialog.newInstance(this).show();

        } else if (itemId == R.id.settings) {

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
        filter.addAction(AmbulanceForegroundService.BroadcastActions.CALL_ONGOING);
        filter.addAction(AmbulanceForegroundService.BroadcastActions.CALL_FINISHED);
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

    public void panicPopUp() {

        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setCancelable(true);
        builder.setTitle("PANIC!");
        builder.setMessage("Message");
        builder.setPositiveButton("Confirm",
                new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                    }
                });
        builder.setNegativeButton(android.R.string.cancel, new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
            }
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

    private void acceptCallDialog(final int callId) {

        Log.i(TAG, "Creating accept dialog");

        // Gather call details
        Call call = AmbulanceForegroundService.getCall(callId);

        if (call == null) {

            Log.d(TAG, "Invalid call/" + callId);
            return;

        }

        String callDetails = String.format("Priority: %1$s\n%2$s %3$s, %4$s, %5$s %6$s",
                call.getPriority(),
                call.getStreet(), call.getNumber(),
                call.getCity(), call.getState(), call.getZipcode());

        // Use the Builder class for convenient dialog construction
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("Accept Incoming Call?")
                .setMessage(callDetails)
                .setPositiveButton("Accept", new DialogInterface.OnClickListener() {
                        public void onClick(DialogInterface dialog, int id) {

                            Toast.makeText(MainActivity.this, "Call accepted", Toast.LENGTH_SHORT).show();

                            Log.i(TAG, "Call accepted");

                            Intent serviceIntent = new Intent(MainActivity.this, AmbulanceForegroundService.class);
                            serviceIntent.setAction(AmbulanceForegroundService.Actions.CALL_ACCEPT);
                            serviceIntent.putExtra("CALL_ID", callId);
                            startService(serviceIntent);

                        }
                })
                .setNegativeButton("Decline", new DialogInterface.OnClickListener() {
                        public void onClick(DialogInterface dialog, int id) {

                            Toast.makeText(MainActivity.this, "Call declined", Toast.LENGTH_SHORT).show();

                            Log.i(TAG, "Call declined");

                            Intent serviceIntent = new Intent(MainActivity.this, AmbulanceForegroundService.class);
                            serviceIntent.setAction(AmbulanceForegroundService.Actions.CALL_DECLINE);
                            serviceIntent.putExtra("CALL_ID", callId);
                            startService(serviceIntent);

                        }
                });

        // Create the AlertDialog object and display it
        builder.create().show();

    }

    private void endCallDialog() {

        Log.i(TAG, "Creating end call dialog");

        // Use the Builder class for convenient dialog construction
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("Currently handling call")
                .setMessage("What do you want to do?")
                .setNegativeButton("Continue", new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int id) {

                        Toast.makeText(MainActivity.this, "Continuing to handle call", Toast.LENGTH_SHORT).show();

                        Log.i(TAG, "Continuing with call");

                    }
                })
                .setNeutralButton("Suspend", new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int id) {

                        Toast.makeText(MainActivity.this, "Suspending call", Toast.LENGTH_SHORT).show();

                        Log.i(TAG, "Suspending call");

                    }
                })
                .setPositiveButton("End", new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int id) {

                        Toast.makeText(MainActivity.this, "Ending call", Toast.LENGTH_SHORT).show();

                        Log.i(TAG, "Ending call");

                        Intent serviceIntent = new Intent(MainActivity.this, AmbulanceForegroundService.class);
                        serviceIntent.setAction(AmbulanceForegroundService.Actions.CALL_FINISH);
                        startService(serviceIntent);

                    }
                });
        // Create the AlertDialog object and return it
        builder.create().show();
    }

    void startUpdatingLocation() {

        // start updating location

        if (canWrite()) {

            // Toast to warn user
            Toast.makeText(MainActivity.this, R.string.requestToStreamLocation,
                    Toast.LENGTH_SHORT).show();


            // start updating location
            Intent intent = new Intent(MainActivity.this, AmbulanceForegroundService.class);
            intent.setAction(AmbulanceForegroundService.Actions.START_LOCATION_UPDATES);

            new OnServiceComplete(MainActivity.this,
                    AmbulanceForegroundService.BroadcastActions.SUCCESS,
                    AmbulanceForegroundService.BroadcastActions.FAILURE,
                    intent) {

                @Override
                public void onSuccess(Bundle extras) {

                    // reset requestingLocation to maximum number of attempts
                    requestingToStreamLocation = MAX_NUMBER_OF_LOCATION_REQUESTS_ATTEMPTS;

                    // Toast to warn user
                    Toast.makeText(MainActivity.this, R.string.startedStreamingLocation,
                            Toast.LENGTH_SHORT).show();

                }

                @Override
                public void onFailure(Bundle extras) {

                    if (--requestingToStreamLocation > 0) {

                        // call super to display error message
                        super.onFailure(extras);

                    } else {

                        // Otherwise ask user if wants to force
                        AlertDialog.Builder alertDialogBuilder = new AlertDialog.Builder(MainActivity.this);

                        alertDialogBuilder.setTitle(R.string.alert_warning_title);
                        alertDialogBuilder.setMessage(R.string.forceLocationUpdates);

                        // Cancel button
                        alertDialogBuilder.setNegativeButton(
                                R.string.alert_button_negative_text,
                                new DialogInterface.OnClickListener() {
                                    @Override
                                    public void onClick(DialogInterface dialog, int which) {
                                        // do nothing
                                    }
                                });

                        // Create the OK button that logs user out
                        alertDialogBuilder.setPositiveButton(
                                R.string.alert_button_positive_text,
                                new DialogInterface.OnClickListener() {
                                    @Override
                                    public void onClick(DialogInterface dialog, int which) {

                                        Log.i(TAG, "ForceLocationUpdatesDialog: OK Button Clicked");

                                        // Toast to warn user
                                        Toast.makeText(MainActivity.this, R.string.forcingLocationUpdates,
                                                Toast.LENGTH_LONG).show();

                                        // Reset location_client
                                        String payload = "{\"location_client_id\":\"\"}";
                                        Intent intent = new Intent(MainActivity.this, AmbulanceForegroundService.class);
                                        intent.setAction(AmbulanceForegroundService.Actions.UPDATE_AMBULANCE);
                                        Bundle bundle = new Bundle();
                                        bundle.putString("UPDATE", payload);
                                        intent.putExtras(bundle);

                                        // What to do when service completes?
                                        new OnServicesComplete(MainActivity.this,
                                                new String[]{
                                                        AmbulanceForegroundService.BroadcastActions.SUCCESS,
                                                        AmbulanceForegroundService.BroadcastActions.AMBULANCE_UPDATE
                                                },
                                                new String[]{AmbulanceForegroundService.BroadcastActions.FAILURE},
                                                intent) {

                                            @Override
                                            public void onSuccess(Bundle extras) {
                                                Log.i(TAG, "onSuccess");

                                                // Toast to warn user
                                                Toast.makeText(MainActivity.this, R.string.succeededForcingLocationUpdates,
                                                        Toast.LENGTH_LONG).show();

                                            }

                                            @Override
                                            public void onReceive(Context context, Intent intent) {

                                                // Retrieve action
                                                String action = intent.getAction();

                                                // Intercept success
                                                if (action.equals(AmbulanceForegroundService.BroadcastActions.SUCCESS))
                                                    // prevent propagation, still waiting for AMBULANCE_UPDATE
                                                    return;

                                                // Intercept AMBULANCE_UPDATE
                                                if (action.equals(AmbulanceForegroundService.BroadcastActions.AMBULANCE_UPDATE))
                                                    // Inject uuid into AMBULANCE_UPDATE
                                                    intent.putExtra(OnServicesComplete.UUID, getUuid());

                                                // Call super
                                                super.onReceive(context, intent);
                                            }

                                        }
                                                .setFailureMessage(getString(R.string.couldNotForceLocationUpdate))
                                                .setAlert(new AlertSnackbar(MainActivity.this));

                                    }

                                });

                        alertDialogBuilder.create().show();

                    }

                }

            }
                    .setFailureMessage(getResources().getString(R.string.anotherClientIsStreamingLocations))
                    .setAlert(new AlertSnackbar(MainActivity.this));

        } else {

            // Toast to warn user
            Toast.makeText(MainActivity.this, R.string.cantModifyAmbulance, Toast.LENGTH_LONG).show();

        }

    }

    void stopUpdatingLocation() {

        // is handling call?
        Call currentCall = AmbulanceForegroundService.getCurrentCall();
        if (currentCall != null) {

            Log.d(TAG, "In call: prompt user");

            // currently handling call, prompt if want to end call
            endCallDialog();

        } else {

            Log.d(TAG, "No call: stop location updates");

            // stop updating location

            if (canWrite()) {

                // turn off tracking
                Intent intent = new Intent(MainActivity.this, AmbulanceForegroundService.class);
                intent.setAction(AmbulanceForegroundService.Actions.STOP_LOCATION_UPDATES);
                startService(intent);

                // reset requestingLocation to maximum number of attempts
                requestingToStreamLocation = MAX_NUMBER_OF_LOCATION_REQUESTS_ATTEMPTS;

                // Toast to warn user
                Toast.makeText(MainActivity.this, R.string.stopedStreamingLocation,
                        Toast.LENGTH_SHORT).show();

            }
        }

    }

}