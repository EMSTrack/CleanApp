package org.emstrack.ambulance;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.res.Configuration;
import android.os.Bundle;
import android.support.design.widget.NavigationView;
import android.support.design.widget.TabLayout;
import android.support.v4.app.Fragment;
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
    private LocationChangeBroadcastReceiver receiver;
    private int requestingToStreamLocation;

    public class LocationChangeBroadcastReceiver extends BroadcastReceiver {

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
        headerText = (TextView) findViewById(R.id.headerText);

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

        // Tracking icon
        trackingIcon = (ImageView) findViewById(R.id.trackingIcon);
        trackingIcon.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {

                if (AmbulanceForegroundService.isUpdatingLocation()) {

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

                } else {

                    // start updating location

                    if (canWrite()) {

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
            }
        });

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

        } // else if (itemId == R.id.home) {}

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
        receiver = new LocationChangeBroadcastReceiver();
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

}