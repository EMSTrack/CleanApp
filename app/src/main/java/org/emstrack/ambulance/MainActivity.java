package org.emstrack.ambulance;

import android.Manifest;
import android.annotation.SuppressLint;
import android.app.Activity;
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentSender;
import android.content.pm.PackageManager;
import android.content.res.Configuration;
import android.net.Uri;
import android.os.Bundle;
import android.os.Looper;
import android.provider.Settings;
import android.support.annotation.NonNull;
import android.support.design.widget.FloatingActionButton;
import android.support.design.widget.NavigationView;
import android.support.design.widget.Snackbar;
import android.support.design.widget.TabLayout;
import android.support.v4.app.ActivityCompat;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentTransaction;
import android.support.v4.view.ViewPager;
import android.support.v4.widget.DrawerLayout;
import android.support.v7.app.ActionBarDrawerToggle;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.MenuItem;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.ImageButton;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.gms.common.api.ApiException;
import com.google.android.gms.common.api.ResolvableApiException;
import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationCallback;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationResult;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.location.LocationSettingsRequest;
import com.google.android.gms.location.LocationSettingsResponse;
import com.google.android.gms.location.LocationSettingsStatusCodes;
import com.google.android.gms.location.SettingsClient;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.gms.tasks.Task;
import com.google.gson.FieldNamingPolicy;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

import org.eclipse.paho.client.mqttv3.MqttException;
import org.eclipse.paho.client.mqttv3.MqttMessage;
import org.emstrack.ambulance.adapters.Pager;
import org.emstrack.ambulance.dialogs.LogoutDialog;
import org.emstrack.ambulance.fragments.DispatcherFragment;
import org.emstrack.ambulance.fragments.GPSFragment;
import org.emstrack.ambulance.fragments.HospitalFragment;
import org.emstrack.models.AmbulanceData;
import org.emstrack.mqtt.MqttProfileClient;
import org.emstrack.mqtt.MqttProfileMessageCallback;

import org.emstrack.ambulance.viewModels.HospitalViewModel;

import java.util.Date;

import static org.emstrack.ambulance.FeatureFlags.ADMIN;
import static org.emstrack.ambulance.FeatureFlags.OLD_HOSPITAL_UI;

/**
 * This is the main activity -- the default screen
 */
public class MainActivity extends AppCompatActivity {

    private static final String TAG = MainActivity.class.getSimpleName();
    private static final int REQUEST_PERMISSIONS_REQUEST_CODE = 34;
    private static final int REQUEST_CHECK_SETTINGS = 0x1;

    private int ambulanceId = -1;
    private AmbulanceData ambulanceData;

    private android.location.Location lastLocation;

    private LocationRequest locationRequest;
    private FusedLocationProviderClient fusedLocationClient;
    private SettingsClient settingsClient;
    private LocationSettingsRequest locationSettingsRequest;
    private LocationCallback locationCallback;
    private boolean requestingLocationUpdates;

    private ViewPager viewPager;
    private Pager adapter;

    private DrawerLayout mDrawer;
    private NavigationView nvDrawer;
    private ActionBarDrawerToggle drawerToggle;
    private Toolbar toolbar;
    static TextView statusText;
    private ImageButton panicButton;
    private FloatingActionButton navButton;

    /**
     * @param savedInstanceState
     */
    @Override
    protected void onCreate(Bundle savedInstanceState) {

        super.onCreate(savedInstanceState);

        if (OLD_HOSPITAL_UI) {
            setContentView(R.layout.activity_main_old);
        } else {
            setContentView(R.layout.activity_main);
        }

        // Get data from Login and place it into the ambulanceData
        ambulanceId = Integer
                .parseInt(getIntent().getStringExtra("SELECTED_AMBULANCE_ID"));

        panicButton = (ImageButton) findViewById(R.id.panicButton);
        panicButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                panicPopUp();
            }
        });

        navButton = (FloatingActionButton) findViewById(R.id.navBtn);
        navButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                //
            }
        });

        // Set a Toolbar to replace the ActionBar.
        toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        getSupportActionBar().setDisplayShowTitleEnabled(false); // No text in title bar



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

        if (OLD_HOSPITAL_UI) {
            //set up TabLayout Structure
            TabLayout tabLayout = (TabLayout) findViewById(R.id.tab_layout_home);
            tabLayout.addTab(tabLayout.newTab().setText("Dispatcher"));
            tabLayout.addTab(tabLayout.newTab().setText("Hospitals"));
            if (ADMIN) tabLayout.addTab(tabLayout.newTab().setText("GPS"));


            //pager
            viewPager = (ViewPager) findViewById(R.id.pager);
            tabLayout.setTabGravity(TabLayout.GRAVITY_FILL);

            //Setup Adapter for tabLayout
            adapter = new Pager(getSupportFragmentManager(), tabLayout.getTabCount());
            viewPager.setAdapter(adapter);

            viewPager.addOnPageChangeListener(new TabLayout.TabLayoutOnPageChangeListener(tabLayout));
            tabLayout.addOnTabSelectedListener(new TabLayout.OnTabSelectedListener() {
                @Override
                public void onTabSelected(TabLayout.Tab tab) {
                    viewPager.setCurrentItem(tab.getPosition());
                    if (tab.getPosition() == 0) {
                        navButton.show();
                    } else {
                        navButton.hide();
                    }
                }

                @Override
                public void onTabUnselected(TabLayout.Tab tab) {
                }

                @Override
                public void onTabReselected(TabLayout.Tab tab) {
                }
            });
        } else {
            setupSpinner();
        }

        // Retrieve client
        final MqttProfileClient profileClient = ((AmbulanceApp) getApplication()).getProfileClient();

        try {

            // Start retrieving data
            profileClient.subscribe("ambulance/" + ambulanceId + "/data",
                    1, new MqttProfileMessageCallback() {

                        @Override
                        public void messageArrived(String topic, MqttMessage message) {

                            // Keep subscription to ambulance to make sure we receive
                            // the latest updates.

                            if (ambulanceData == null) {

                                Log.d(TAG, "Setting ambulance.");

                                // first time we receive ambulance data
                                GsonBuilder gsonBuilder = new GsonBuilder();
                                gsonBuilder.setFieldNamingPolicy(FieldNamingPolicy.LOWER_CASE_WITH_UNDERSCORES);
                                Gson gson = gsonBuilder.create();

                                // Parse and set ambulance
                                // TODO: Check for potential errors
                                ambulanceData = gson
                                        .fromJson(new String(message.getPayload()),
                                                AmbulanceData.class);

                                statusText = (TextView) findViewById(R.id.statusText);
                                statusText.setText(ambulanceData.getIdentifier() + " - "
                                        + profileClient.getSettings().getAmbulanceStatus().get(ambulanceData.getStatus()));
                            } else {

                                Log.d(TAG, "Received ambulance update.");

                                // TODO: process update
                            }
                        }

                    });

        } catch (MqttException e) {
            Log.d(TAG, "Could not subscribe to ambulance data");
        }

        HospitalViewModel hospitalViewModel = new HospitalViewModel(profileClient);
        hospitalViewModel.getHospitalMetadata();

        // Setup fused location client
        requestingLocationUpdates = false;
        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this);
        settingsClient = LocationServices.getSettingsClient(this);

        // Setup callback
        locationCallback = new LocationCallback() {

            @Override
            public void onLocationResult(LocationResult locationResult) {
                super.onLocationResult(locationResult);

                // set last location
                lastLocation = locationResult.getLastLocation();
                Log.i(TAG, "lastLocation = " + lastLocation);

                // Update ambulance
                ambulanceData.update(lastLocation);

                // update UI
                GPSFragment gpsFragment;
                if (OLD_HOSPITAL_UI) {
                    gpsFragment = (GPSFragment) adapter.getRegisteredFragment(2);
                } else {
                    String gpsTag = getResources().getString(R.string.gps);
                    gpsFragment = (GPSFragment) getSupportFragmentManager().findFragmentByTag(gpsTag);
                }
                if (gpsFragment != null) {
                    // TODO: make updateLocation take Ambulance instead of Location
                    gpsFragment.updateLocation(lastLocation);
                }

                // PUBLISH TO MQTT
                String updateString = getUpdateString(lastLocation);

                try {
                    profileClient.publish("user/" + profileClient.getUsername() + "/ambulance/" + ambulanceId + "/data", updateString,1, false );
                    Log.e("LocationChangeUpdate", "onLocationChanged: update sent to server\n" + updateString);
                } catch (MqttException e) {
                    e.printStackTrace();
                }

            }

        };

        // Request permission for location services
        locationRequest = LocationRequest.create()
                .setPriority(LocationRequest.PRIORITY_HIGH_ACCURACY)
                .setInterval(10 * 1000)        // 10 seconds, in milliseconds
                .setFastestInterval(1 * 1000); // 1 second, in milliseconds

        // Build location setting request
        LocationSettingsRequest.Builder builder = new LocationSettingsRequest.Builder();
        builder.addLocationRequest(locationRequest);
        locationSettingsRequest = builder.build();

    }

    private void setupSpinner() {
        Spinner spinner = findViewById(R.id.spinner_nav);
        ArrayAdapter<CharSequence> spinnerAdapter = ArrayAdapter.createFromResource(this,
                ADMIN ? R.array.spinner_list_item_array_admin : R.array.spinner_list_item_array, R.layout.custom_spinner);
        spinnerAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinner.setAdapter(spinnerAdapter);
        spinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> adapterView, View view, int position, long l) {
                FragmentManager fragmentManager = getSupportFragmentManager();
                FragmentTransaction fragmentTransaction = fragmentManager.beginTransaction();

                /* When clicking on a new page,
                 * clear the entire back stack of fragments
                 * so pressing back goes to select ambulance
                 */
                fragmentManager.popBackStack(null, FragmentManager.POP_BACK_STACK_INCLUSIVE);
                switch(position) {
                    case 0:
                        String dispatchTag = getResources().getString(R.string.dispatch);
                        Fragment dispatchFragment = fragmentManager.findFragmentByTag(dispatchTag);

                        if (dispatchFragment == null) {
                            fragmentTransaction
                                    .add(R.id.root, new DispatcherFragment(), dispatchTag)
                                    .commit();
                        } else {
                            fragmentTransaction
                                    .replace(R.id.root, dispatchFragment, dispatchTag)
                                    .commit();
                        }
                        break;
                    case 1:
                        String hospitalTag = getResources().getString(R.string.hospital);
                        Fragment hospitalFragment = fragmentManager.findFragmentByTag(hospitalTag);
                        if (hospitalFragment == null) {
                            getSupportFragmentManager().beginTransaction()
                                    .add(R.id.root, new HospitalFragment(), hospitalTag)
                                    .commit();
                        } else {
                            fragmentTransaction
                                    .replace(R.id.root, hospitalFragment, hospitalTag)
                                    .commit();
                        }
                        break;
                    case 2:
                        String gpsTag = getResources().getString(R.string.gps);
                        Fragment gpsFragment = fragmentManager.findFragmentByTag(gpsTag);
                        if (gpsFragment == null) {
                            getSupportFragmentManager().beginTransaction()
                                    .add(R.id.root, new GPSFragment(), gpsTag)
                                    .commit();
                        } else {
                            fragmentTransaction
                                    .replace(R.id.root, gpsFragment, gpsTag)
                                    .commit();
                        }
                        break;
                }
            }

            @Override
            public void onNothingSelected(AdapterView<?> adapterView) {}
        });
        spinner.setSelection(0);
    }

    @Override
    public void onResume() {
        super.onResume();
        // Within {@code onPause()}, we remove location updates. Here, we resume receiving
        // location updates if the user has requested them.
        if (requestingLocationUpdates && checkPermissions()) {
            startLocationUpdates();
        } else if (!checkPermissions()) {
            requestPermissions();
        }
    }

    @Override
    protected void onPause() {
        super.onPause();

        // Remove location updates to save battery.
        stopLocationUpdates();
    }

    /**
     * Return the current state of the permissions needed.
     */
    private boolean checkPermissions() {
        int permissionState = ActivityCompat.checkSelfPermission(this,
                Manifest.permission.ACCESS_FINE_LOCATION);
        return permissionState == PackageManager.PERMISSION_GRANTED;
    }

    /**
     * Requests location updates from the FusedLocationApi. Note: we don't call this unless location
     * runtime permission has been granted.
     */
    public void startLocationUpdates() {

        if (requestingLocationUpdates) {
            Log.d(TAG, "startLocationUpdates: updates already requested, no-op.");
            return;
        }

        // Begin by checking if the device has the necessary location settings.
        settingsClient.checkLocationSettings(locationSettingsRequest)
                .addOnSuccessListener(this, new OnSuccessListener<LocationSettingsResponse>() {
                    @SuppressLint("MissingPermission")
                    @Override
                    public void onSuccess(LocationSettingsResponse locationSettingsResponse) {
                        Log.i(TAG, "All location settings are satisfied. Starting location updates.");

                        //noinspection MissingPermission
                        fusedLocationClient.requestLocationUpdates(locationRequest,
                                locationCallback, Looper.myLooper());
                        requestingLocationUpdates = true;

                    }
                })
                .addOnFailureListener(this, new OnFailureListener() {
                    @Override
                    public void onFailure(@NonNull Exception e) {
                        int statusCode = ((ApiException) e).getStatusCode();
                        switch (statusCode) {
                            case LocationSettingsStatusCodes.RESOLUTION_REQUIRED:
                                Log.i(TAG, "Location settings are not satisfied. Attempting to upgrade " +
                                        "location settings ");
                                try {
                                    // Show the dialog by calling startResolutionForResult(), and check the
                                    // result in onActivityResult().
                                    ResolvableApiException rae = (ResolvableApiException) e;
                                    rae.startResolutionForResult(MainActivity.this, REQUEST_CHECK_SETTINGS);
                                } catch (IntentSender.SendIntentException sie) {
                                    Log.i(TAG, "PendingIntent unable to execute request.");
                                }
                                break;
                            case LocationSettingsStatusCodes.SETTINGS_CHANGE_UNAVAILABLE:
                                String errorMessage = "Location settings are inadequate, and cannot be " +
                                        "fixed here. Fix in Settings.";
                                Log.e(TAG, errorMessage);
                                Toast.makeText(MainActivity.this, errorMessage, Toast.LENGTH_LONG).show();
                                requestingLocationUpdates = false;
                        }

                        // updateUI();
                    }
                });
    }

    /**
     * Removes location updates from the FusedLocationApi.
     */
    public void stopLocationUpdates() {

        if (!requestingLocationUpdates) {
            Log.d(TAG, "stopLocationUpdates: updates never requested, no-op.");
            return;
        }

        // It is a good practice to remove location requests when the activity is in a paused or
        // stopped state. Doing so helps battery performance and is especially
        // recommended in applications that request frequent location updates.
        fusedLocationClient.removeLocationUpdates(locationCallback)
                .addOnCompleteListener(this, new OnCompleteListener<Void>() {
                    @Override
                    public void onComplete(@NonNull Task<Void> task) {
                        Log.d(TAG, "Stoping location updates.");
                        requestingLocationUpdates = false;
                    }
                });
    }

    private void requestPermissions() {
        boolean shouldProvideRationale =
                ActivityCompat.shouldShowRequestPermissionRationale(this,
                        Manifest.permission.ACCESS_FINE_LOCATION);

        // Provide an additional rationale to the user. This would happen if the user denied the
        // request previously, but didn't check the "Don't ask again" checkbox.
        if (shouldProvideRationale) {
            Log.i(TAG, "Displaying permission rationale to provide additional context.");
            showSnackbar(R.string.permission_rationale,
                    android.R.string.ok, new View.OnClickListener() {
                        @Override
                        public void onClick(View view) {
                            // Request permission
                            ActivityCompat.requestPermissions(MainActivity.this,
                                    new String[]{Manifest.permission.ACCESS_FINE_LOCATION},
                                    REQUEST_PERMISSIONS_REQUEST_CODE);
                        }
                    });
        } else {
            Log.i(TAG, "Requesting permission");
            // Request permission. It's possible this can be auto answered if device policy
            // sets the permission in a given state or the user denied the permission
            // previously and checked "Never ask again".
            ActivityCompat.requestPermissions(MainActivity.this,
                    new String[]{Manifest.permission.ACCESS_FINE_LOCATION},
                    REQUEST_PERMISSIONS_REQUEST_CODE);
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        switch (requestCode) {
            // Check for the integer request code originally supplied to startResolutionForResult().
            case REQUEST_CHECK_SETTINGS:
                switch (resultCode) {
                    case Activity.RESULT_OK:
                        Log.i(TAG, "User agreed to make required location settings changes.");
                        // Nothing to do. startLocationupdates() gets called in onResume again.
                        break;
                    case Activity.RESULT_CANCELED:
                        Log.i(TAG, "User chose not to make required location settings changes.");
                        requestingLocationUpdates = false;
                        // updateUI();
                        break;
                }
                break;
        }
    }

    /**
     * Callback received when a permissions request has been completed.
     */
    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions,
                                           @NonNull int[] grantResults) {
        Log.i(TAG, "onRequestPermissionResult");
        if (requestCode == REQUEST_PERMISSIONS_REQUEST_CODE) {
            if (grantResults.length <= 0) {
                // If user interaction was interrupted, the permission request is cancelled and you
                // receive empty arrays.
                Log.i(TAG, "User interaction was cancelled.");
            } else if (grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                if (requestingLocationUpdates) {
                    Log.i(TAG, "Permission granted, updates requested, starting location updates");
                    startLocationUpdates();
                }
            } else {
                // Permission denied.

                // Notify the user via a SnackBar that they have rejected a core permission for the
                // app, which makes the Activity useless. In a real app, core permissions would
                // typically be best requested during a welcome-screen flow.

                // Additionally, it is important to remember that a permission might have been
                // rejected without asking the user for permission (device policy or "Never ask
                // again" prompts). Therefore, a user interface affordance is typically implemented
                // when permissions are denied. Otherwise, your app could appear unresponsive to
                // touches or interactions which have required permissions.
                showSnackbar(R.string.permission_denied_explanation,
                        R.string.settings, new View.OnClickListener() {
                            @Override
                            public void onClick(View view) {
                                // Build intent that displays the App settings screen.
                                Intent intent = new Intent();
                                intent.setAction(
                                        Settings.ACTION_APPLICATION_DETAILS_SETTINGS);
                                Uri uri = Uri.fromParts("package",
                                        BuildConfig.APPLICATION_ID, null);
                                intent.setData(uri);
                                intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                                startActivity(intent);
                            }
                        });
            }
        }
    }

    /**
     * Shows a {@link Snackbar}.
     *
     * @param mainTextStringId The id for the string resource for the Snackbar text.
     * @param actionStringId   The text of the action item.
     * @param listener         The listener associated with the Snackbar action.
     */
    private void showSnackbar(final int mainTextStringId, final int actionStringId,
                              View.OnClickListener listener) {
        Snackbar.make(
                findViewById(android.R.id.content),
                getString(mainTextStringId),
                Snackbar.LENGTH_INDEFINITE)
                .setAction(getString(actionStringId), listener).show();
    }


    //Hamburger Menu setup
    private ActionBarDrawerToggle setupDrawerToggle() {
        return new ActionBarDrawerToggle(this, mDrawer, toolbar, R.string.drawer_open, R.string.drawer_close);
    }

    //Hamburger Menu Listener
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

    //Start selected activity in Hamburger
    public void selectDrawerItem(MenuItem menuItem) {

        // Close drawer
        mDrawer.closeDrawers();

        // Get menuitem
        int itemId = menuItem.getItemId();

        // Actions
        if (itemId == R.id.logout) {
            LogoutDialog ld = LogoutDialog.newInstance();
            ld.show(getFragmentManager(), "logout_dialog");
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

    public String getUpdateString(android.location.Location lastLocation) {
        double latitude = lastLocation.getLatitude();
        double longitude = lastLocation.getLongitude();
        double orientation = lastLocation.getBearing();
        String timestamp = new Date(lastLocation.getTime()).toString();

        String updateString =  "{\"orientation\" :" + orientation + ",\"location\":{" +
                "\"latitude\":"+ latitude + ",\"longitude\":" + longitude +"},\"location_timestamp\":\"" + timestamp + "\"}";
        return updateString;

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

/*
    @Override
    public void onBackPressed() {
        finish();
    }
*/

    public android.location.Location getLastLocation() {
        return lastLocation;
    }

}
