package org.emstrack.ambulance;

import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.content.res.Configuration;
import android.graphics.BitmapFactory;
import android.graphics.PorterDuff;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.CountDownTimer;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemSelectedListener;
import android.widget.ArrayAdapter;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.IdRes;
import androidx.annotation.NonNull;
import androidx.appcompat.app.ActionBar;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.browser.customtabs.CustomTabsCallback;
import androidx.browser.customtabs.CustomTabsClient;
import androidx.browser.customtabs.CustomTabsIntent;
import androidx.browser.customtabs.CustomTabsServiceConnection;
import androidx.browser.customtabs.CustomTabsSession;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;
import androidx.localbroadcastmanager.content.LocalBroadcastManager;
import androidx.navigation.NavController;
import androidx.navigation.NavDestination;
import androidx.navigation.fragment.NavHostFragment;
import androidx.navigation.ui.NavigationUI;
import androidx.preference.PreferenceManager;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.LinearSnapHelper;
import androidx.recyclerview.widget.RecyclerView;
import androidx.recyclerview.widget.SnapHelper;

import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.google.android.material.navigationrail.NavigationRailView;

import org.emstrack.ambulance.adapters.WaypointInfoRecyclerAdapter;
import org.emstrack.ambulance.dialogs.AlertSnackbar;
import org.emstrack.ambulance.fragments.EquipmentFragment;
import org.emstrack.ambulance.fragments.MessagesFragment;
import org.emstrack.ambulance.fragments.SettingsFragment;
import org.emstrack.ambulance.models.AmbulanceAppData;
import org.emstrack.ambulance.services.AmbulanceForegroundService;
import org.emstrack.models.Ambulance;
import org.emstrack.models.AmbulanceCall;
import org.emstrack.models.AmbulancePermission;
import org.emstrack.models.Call;
import org.emstrack.models.CallStack;
import org.emstrack.models.Client;
import org.emstrack.models.Credentials;
import org.emstrack.models.HospitalPermission;
import org.emstrack.models.Location;
import org.emstrack.models.Patient;
import org.emstrack.models.PriorityCode;
import org.emstrack.models.Profile;
import org.emstrack.models.RadioCode;
import org.emstrack.models.Settings;
import org.emstrack.models.TokenLogin;
import org.emstrack.models.api.APIService;
import org.emstrack.models.api.APIServiceGenerator;
import org.emstrack.models.api.OnAPICallComplete;
import org.emstrack.models.util.BroadcastActions;
import org.emstrack.models.util.OnServiceComplete;

import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;


/**
 * This is the main activity
 */
public class MainActivity extends AppCompatActivity {

    private static final String TAG = MainActivity.class.getSimpleName();

    private static final DecimalFormat df = new DecimalFormat();

    private ArrayList<String> ambulanceStatusList;
    private Map<String, String> ambulanceStatusMap;
    private HashMap<String, Integer> ambulanceStatusBackgroundColorMap;
    private HashMap<String, Integer> ambulanceStatusTextColorMap;

    private Map<String, String> ambulanceCapabilitiesMap;
    private ArrayList<String> ambulanceCapabilityList;
    private boolean promptingNextWaypoint;

    public enum BackButtonMode {
        UP,
        FINISH,
        LOGOUT
    }

    private static final int enabledAlpha = 255;
    private static final int disabledAlpha = 255/4;

    private List<AmbulancePermission> ambulancePermissions;
    private List<HospitalPermission> hospitalPermissions;
    private List<Location> bases;
    private List<Location> otherLocations;

    private ArrayAdapter<String> hospitalListAdapter;
    private ArrayAdapter<String> baseListAdapter;
    private ArrayAdapter<String> othersListAdapter;

    private Drawable onlineIcon;
    private Drawable trackingIcon;
    private MainActivityBroadcastReceiver receiver;
    private Map<String, Integer> callPriorityBackgroundColors;
    private Map<String, Integer> callPriorityForegroundColors;

    private boolean logoutAfterFinish;

    private AlertDialog promptVideoCallDialog;
    private CustomTabsClient customTabsClient;

    private NavHostFragment navHostFragment;
    private BottomNavigationView bottomNavigationView;
    private NavigationRailView navigationRailView;
    private Menu actionBarMenu;
    private BackButtonMode backButtonMode;

    public class MainActivityBroadcastReceiver extends BroadcastReceiver {

        @Override
        public void onReceive(Context context, Intent intent ) {

            if (intent != null) {

                final String action = intent.getAction();
                if (action == null)
                    return;

                switch (action) {
                    case AmbulanceForegroundService.BroadcastActions.AMBULANCE_UPDATE:

                        Log.i(TAG, "AMBULANCE_UPDATE");
                        // setupNavigationBar();

                        break;
                    case AmbulanceForegroundService.BroadcastActions.LOCATION_UPDATE_CHANGE:

                        Log.i(TAG, "LOCATION_UPDATE_CHANGE");

                        if (AmbulanceForegroundService.isUpdatingLocation())
                            trackingIcon.setAlpha(enabledAlpha);
                        else {
                            trackingIcon.setAlpha(disabledAlpha);
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
                        int myVectorColor = ContextCompat.getColor(MainActivity.this, R.color.iconCallAccepted);
                        trackingIcon.setColorFilter(myVectorColor, PorterDuff.Mode.SRC_IN);
                        navigate(R.id.ambulanceFragment);

                        break;
                    case AmbulanceForegroundService.BroadcastActions.CALL_COMPLETED:

                        Log.i(TAG, "CALL_COMPLETED");

                        // change button color to black
                        myVectorColor = ContextCompat.getColor(MainActivity.this, R.color.iconColor);
                        trackingIcon.setColorFilter(myVectorColor, PorterDuff.Mode.SRC_IN);

                        // Logout?
                        if (logoutAfterFinish) {
                            logoutAfterFinish = false;
                            promptLogout();
                        }

                        break;
                    case AmbulanceForegroundService.BroadcastActions.WEBRTC_MESSAGE:

                        Log.i(TAG, "WEBRTC_NEW_CALL");

                        String type = intent.getStringExtra(AmbulanceForegroundService.BroadcastExtras.WEBRTC_TYPE);
                        String username = intent.getStringExtra(AmbulanceForegroundService.BroadcastExtras.WEBRTC_CLIENT_USERNAME);
                        String clientId = intent.getStringExtra(AmbulanceForegroundService.BroadcastExtras.WEBRTC_CLIENT_ID);

                        assert type != null;
                        if (type.equals("call")) {

                            promptVideoCallAccept(username, clientId);

                        } else if (type.equals("cancel")) {

                            if (promptVideoCallDialog != null) {

                                Log.d(TAG, "Cancelling call");
                                promptVideoCallDialog.dismiss();
                                promptVideoCallDialog = null;

                                Toast.makeText(MainActivity.this,
                                        R.string.video_call_cancelled,
                                        Toast.LENGTH_SHORT).show();
                            } else
                                Log.d(TAG, "Canceled call was not been prompted");

                        } else {

                            Log.d(TAG, String.format("Unknown WebRTC message type '%1$s'", type));

                        }

                        break;
                }
            }
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {

        // Inflate the action bar menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.action_bar, menu);

        // save menu
        this.actionBarMenu = menu;

        // Hide video button if video is not enabled
        hideVideoCallButton();

        // Online icon
        onlineIcon = menu.findItem(R.id.onlineIcon).getIcon();
        if (AmbulanceForegroundService.isOnline())
            onlineIcon.setAlpha(enabledAlpha);
        else
            onlineIcon.setAlpha(disabledAlpha);

        // Tracking icon
        trackingIcon = menu.findItem(R.id.trackingIcon).getIcon();
        if (AmbulanceForegroundService.isUpdatingLocation()) {
            trackingIcon.setAlpha(enabledAlpha);
        } else {
            trackingIcon.setAlpha(disabledAlpha);
        }

        return super.onCreateOptionsMenu(menu);
    }

    /**
     * @param savedInstanceState the saved instance state
     */
    @Override
    protected void onCreate(Bundle savedInstanceState) {

        super.onCreate(savedInstanceState);

        Log.d(TAG, "onCreate");

        // Create custom tab service
        CustomTabsClient.bindCustomTabsService(this, "com.android.chrome", new CustomTabsServiceConnection() {
            @Override
            public void onCustomTabsServiceConnected(@NonNull ComponentName name, @NonNull CustomTabsClient client) {
                // mClient is now valid.
                Log.d(TAG, "Got valid customTabsClient");
                customTabsClient = client;
            }

            @Override
            public void onServiceDisconnected(ComponentName name) {
                // mClient is no longer valid. This also invalidates sessions.
                Log.d(TAG, "Invalidated customTabsClient");
                customTabsClient = null;
            }
        });

        // Do not logout
        logoutAfterFinish = false;

        // not prompting new video call
        promptVideoCallDialog = null;

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
        bottomNavigationView = findViewById(R.id.bottomNavigationBar);
        navigationRailView = findViewById(R.id.navigationRail);
        navHostFragment = (NavHostFragment) getSupportFragmentManager().findFragmentById(R.id.nav_host_fragment);

        // setup toolbar
        Toolbar myToolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(myToolbar);

        // set back as UP
        backButtonMode = BackButtonMode.UP;

        // not prompting next waypoint
        promptingNextWaypoint = false;

        // setup navigation
        setUpNavigation();
    }

    public void initialize() {

        // Get appData
        AmbulanceAppData appData = AmbulanceForegroundService.getAppData();

        // Get profile
        Profile profile = appData.getProfile();

        // ambulance permissions
        if (profile != null) {
            ambulancePermissions = profile.getAmbulances();
        } else {
            ambulancePermissions = new ArrayList<>();
        }

        // hospital permissions
        if (profile != null) {
            hospitalPermissions = profile.getHospitals();
        } else {
            hospitalPermissions = new ArrayList<>();
        }

        // bases and other locations
        bases = appData.getBases();
        otherLocations = appData.getOtherLocations();

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

        // Creates list of other location names
        ArrayList<String> othersList = new ArrayList<>();
        othersList.add(getString(R.string.selectOthers));
        for (Location other : otherLocations)
            othersList.add(other.getName());

        // Create the adapter
        othersListAdapter = new ArrayAdapter<>(this,
                android.R.layout.simple_spinner_dropdown_item, othersList);
        othersListAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);

        // get settings
        Settings settings = appData.getSettings();

        // Get settings, status and capabilities
        ambulanceStatusMap = settings.getAmbulanceStatus();
        ambulanceStatusBackgroundColorMap = new HashMap<>();
        ambulanceStatusTextColorMap = new HashMap<>();
        for (Map.Entry<String,String> entry : ambulanceStatusMap.entrySet()) {
            ambulanceStatusBackgroundColorMap
                    .put(entry.getKey(), getResources().getColor(Ambulance
                            .statusBackgroundColorMap.get(entry.getKey())));
            ambulanceStatusTextColorMap
                    .put(entry.getKey(), getResources().getColor(Ambulance
                            .statusTextColorMap.get(entry.getKey())));
        }

        ambulanceCapabilitiesMap = settings.getAmbulanceCapability();
    }

    public HashMap<String, Integer> getAmbulanceStatusBackgroundColorMap() {
        return ambulanceStatusBackgroundColorMap;
    }

    public HashMap<String, Integer> getAmbulanceStatusTextColorMap() {
        return ambulanceStatusTextColorMap;
    }

    public void setUpNavigation() {
         Log.i(TAG, "setupNavigation");
         int orientation = getResources().getConfiguration().orientation;
         if (orientation == Configuration.ORIENTATION_LANDSCAPE) {
             // In landscape
             navigationRailView.setVisibility(View.VISIBLE);
             bottomNavigationView.setVisibility(View.GONE);
             navigationRailView.setOnItemSelectedListener(item -> {
                 navigate(item.getItemId());
                 return true;
             });
         } else {
             // In portrait
             bottomNavigationView.setVisibility(View.VISIBLE);
             navigationRailView.setVisibility(View.GONE);
             NavigationUI.setupWithNavController(bottomNavigationView, navHostFragment.getNavController());
         }
    }

    public void setBackButtonMode(BackButtonMode backButtonMode) {
        this.backButtonMode = backButtonMode;
    }

    public void setupNavigationBar() {
        setupNavigationBar(null);
    }

    public void setupNavigationBar(Fragment fragment) {
        Log.i(TAG, "setupNavigationBar");

        ActionBar actionBar = getSupportActionBar();
        if (fragment != null) {

            if (fragment.getClass().equals(SettingsFragment.class) ||
                    fragment.getClass().equals(EquipmentFragment.class)||
                    fragment.getClass().equals(MessagesFragment.class)) {

                // hide action bar and bottom navigation bar
                hideBottomNavigationBar();
                hideNavigationRail();

                // set back button as up
                setBackButtonMode(BackButtonMode.UP);

                // set title
                if (actionBar != null) {
                    if (fragment.getClass().equals(SettingsFragment.class)) {
                        actionBar.setTitle(R.string.settings);
                    } else if (fragment.getClass().equals(MessagesFragment.class)) {
                            actionBar.setTitle(R.string.messages);
                    } else {
                        actionBar.setTitle(R.string.equipment);
                    }
                    actionBar.setDisplayHomeAsUpEnabled(true);
                }

            }

        } else {

            // set back button as logout
            setBackButtonMode(BackButtonMode.LOGOUT);

            // set title
            if (actionBar != null) {
                actionBar.setTitle(R.string.EMSTrack);
                actionBar.setDisplayHomeAsUpEnabled(false);
            }

            // get menu
            int orientation = getResources().getConfiguration().orientation;
            Menu menu;
            if (orientation == Configuration.ORIENTATION_LANDSCAPE) {
                // In landscape
                menu = navigationRailView.getMenu();
                hideBottomNavigationBar();
                showNavigationRail();
            } else {
                // In portrait
                menu = bottomNavigationView.getMenu();
                showBottomNavigationBar();
                hideNavigationRail();
            }

            AmbulanceAppData appData = AmbulanceForegroundService.getAppData();
            if (appData != null && appData.getAmbulance() != null) {

                // show ambulance
                if (menu.size() == 3) {
                    menu.add(Menu.NONE, R.id.ambulanceFragment, 3, getString(R.string.ambulance))
                            .setIcon(R.drawable.ic_car_solid)
                            .setShowAsAction(MenuItem.SHOW_AS_ACTION_ALWAYS);
                }

                // show or hide call
                if (appData.getCalls().hasCurrentCall()) {
                    if (menu.size() == 4) {
                        menu.add(Menu.NONE, R.id.callFragment, 4, getString(R.string.call))
                                .setIcon(R.drawable.ic_phone_solid)
                                .setShowAsAction(MenuItem.SHOW_AS_ACTION_ALWAYS);
                    }
                } else if (menu.size() == 5) {
                    menu.removeItem(R.id.callFragment);
                }

            } else {
                // no ambulance nor call
                // hide ambulance
                if (menu.size() == 5) {
                    menu.removeItem(R.id.callFragment);
                }
                // hide call
                if (menu.size() == 4) {
                    menu.removeItem(R.id.ambulanceFragment);
                }
            }

            NavController navController = navHostFragment.getNavController();
            NavDestination destination = navController.getCurrentDestination();
            if (destination != null) {
                int selectedItemId = destination.getId();
                if (orientation == Configuration.ORIENTATION_LANDSCAPE) {
                    // In landscape
                    if (navigationRailView.getSelectedItemId() != selectedItemId) {
                        navigationRailView.setSelectedItemId(selectedItemId);
                    }
                } else {
                    // In portrait
                    if (bottomNavigationView.getSelectedItemId() != selectedItemId) {
                        bottomNavigationView.setSelectedItemId(selectedItemId);
                    }
                }
            }
        }
    }

    public void setVideoCallButtonVisibility(boolean visible) {
        AmbulanceAppData appData = AmbulanceForegroundService.getAppData();
        Settings settings = appData.getSettings();
        if (settings != null && !settings.isEnableVideo()) {
            actionBarMenu.findItem(R.id.videoCallButton).setVisible(visible);
        }
    }

    public void hideVideoCallButton() {
        this.setVideoCallButtonVisibility(false);
    }

    public void showVideoCallButton() {
        this.setVideoCallButtonVisibility(true);
    }

    public void hideActionBar() {
        // hide action bar
        Objects.requireNonNull(getSupportActionBar()).hide();
    }

    public void showActionBar() {
        // show action bar
        Objects.requireNonNull(getSupportActionBar()).show();
    }

    public void showBottomNavigationBar() {
        // show bottom navigation bar
        bottomNavigationView.setVisibility(View.VISIBLE);
    }

    public void hideBottomNavigationBar() {
        // show bottom navigation bar
        bottomNavigationView.setVisibility(View.GONE);
    }

    public void showNavigationRail() {
        // show navigation rail
        navigationRailView.setVisibility(View.VISIBLE);
    }

    public void hideNavigationRail() {
        // show navigation rail
        navigationRailView.setVisibility(View.GONE);
    }

    public void hideBottomNavigationBarItem(int id) {
        bottomNavigationView.findViewById(id).setVisibility(View.GONE);
    }

    public void showBottomNavigationBarItem(int id) {
        bottomNavigationView.findViewById(id).setVisibility(View.VISIBLE);
    }

    public void navigateUp() {
        NavController navController = navHostFragment.getNavController();
        navController.navigateUp();
    }

    public void navigatePopBackStack(@IdRes int destinationId, boolean inclusive) {
        NavController navController = navHostFragment.getNavController();
        navController.popBackStack(destinationId, inclusive);
    }

    public void navigatePopBackStack() {
        NavController navController = navHostFragment.getNavController();
        navController.popBackStack();
    }

    public void navigate(int id, Bundle bundle) {
        // navigate
        NavController navController = navHostFragment.getNavController();
        NavDestination destination = navController.getCurrentDestination();
        if (destination != null && destination.getId() != id) {
            navController.navigate(id, bundle);
        }
    }

    public void navigate(int id) {
        // navigate
        NavController navController = navHostFragment.getNavController();
        NavDestination destination = navController.getCurrentDestination();
        if (destination != null && destination.getId() != id) {
            navController.navigate(id);
        }
    }

    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem item) {
        Log.d(TAG, String.format("onOptionsItemsSelected: %1$d", item.getItemId()));
        int itemId = item.getItemId();
        if (itemId == R.id.settingsFragment) {
            navigate(R.id.settingsFragment);
            return true;
        } else if (itemId == R.id.panicButton) {
            panicPopUp();
            return true;
        } else if (itemId == R.id.videoCallButton) {
            promptVideoCallNew();
            return true;
        } else if (itemId == R.id.trackingIcon || itemId == R.id.onlineIcon) {
            return false;
        } else if (itemId == android.R.id.home) {
            navigatePopBackStack();
            return true;
        } else {
            return super.onOptionsItemSelected(item);
        }
    }

    @Override
    public void onResume() {
        super.onResume();

        Log.d(TAG, "onResume");

        AmbulanceAppData appData = AmbulanceForegroundService.getAppData();
        int nextCallId = -1;
        if (appData != null && appData.getProfile() != null) {

            // already logged in, initialize
            initialize();

            // Is there a requested call that needs to be prompted for?
            Ambulance ambulance = appData.getAmbulance();
            if (ambulance != null) {
                // ambulance is selected
                // navigate(R.id.ambulance);
                // check calls
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
        }

        // setup bottom navigation bar
        setupNavigationBar();

        // Update location icon
        if (trackingIcon != null) {
            if (AmbulanceForegroundService.isUpdatingLocation())
                trackingIcon.setAlpha(enabledAlpha);
            else
                trackingIcon.setAlpha(disabledAlpha);
        }

        if (onlineIcon != null) {
            // Online icon
            if (AmbulanceForegroundService.isOnline())
                onlineIcon.setAlpha(enabledAlpha);
            else
                onlineIcon.setAlpha(disabledAlpha);
        }

        // Register receiver
        IntentFilter filter = new IntentFilter();
        filter.addAction(AmbulanceForegroundService.BroadcastActions.AMBULANCE_UPDATE);
        filter.addAction(AmbulanceForegroundService.BroadcastActions.LOCATION_UPDATE_CHANGE);
        filter.addAction(AmbulanceForegroundService.BroadcastActions.CONNECTIVITY_CHANGE);
        filter.addAction(AmbulanceForegroundService.BroadcastActions.PROMPT_CALL_ACCEPT);
        filter.addAction(AmbulanceForegroundService.BroadcastActions.PROMPT_CALL_END);
        filter.addAction(AmbulanceForegroundService.BroadcastActions.PROMPT_NEXT_WAYPOINT);
        filter.addAction(AmbulanceForegroundService.BroadcastActions.CALL_ACCEPTED);
        filter.addAction(AmbulanceForegroundService.BroadcastActions.CALL_DECLINED);
        filter.addAction(AmbulanceForegroundService.BroadcastActions.CALL_COMPLETED);

        // Enable video
        if (appData != null && appData.getSettings() != null && appData.getSettings().isEnableVideo())
            filter.addAction(AmbulanceForegroundService.BroadcastActions.WEBRTC_MESSAGE);

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

    public boolean panicPopUp() {
        final long DIALOG_DISMISS_TIME = 3000; // milliseconds

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

        AlertDialog alertDialog = builder.create();

        // timer to dismiss dialog after DIALOG_DISMISS_TIME ms
        final CountDownTimer timer = new CountDownTimer(DIALOG_DISMISS_TIME, 1000) {
            @Override
            public void onTick(long l) {
                alertDialog.setMessage("Seconds remaining: "+((l/1000)+1));
            }
            @Override
            public void onFinish() {
                alertDialog.dismiss();
            }
        };
        //if dismissed before timer's up, cancel timer
        alertDialog.setOnDismissListener(dialog -> timer.cancel());
        alertDialog.show();
        timer.start();
        return true;
    }

    /**
     * Get LocalBroadcastManager
     *
     * @return the LocalBroadcastManager
     */
    private LocalBroadcastManager getLocalBroadcastManager() {
        return LocalBroadcastManager.getInstance(this);
    }


    /**
     * @param newAmbulanceId
     *
     * BEWARE: need to check permissions before calling this function
     */
    public void selectAmbulance(int newAmbulanceId) {

        Log.d(TAG, String.format("on selectAmbulance(%d)", newAmbulanceId));

        if (newAmbulanceId == -1) {
            Log.d(TAG, "No ambulance was given.");
            return;
        }

        // If currently handling ambulance
        Ambulance ambulance = AmbulanceForegroundService.getAppData().getAmbulance();
        if (ambulance != null) {

            Log.d(TAG, "Current ambulance " + ambulance.getIdentifier());
            Log.d(TAG, "Requesting location updates? " + (AmbulanceForegroundService.isUpdatingLocation() ? "TRUE" : "FALSE"));

            if (ambulance.getId() != newAmbulanceId) {

                Log.d(TAG, "Will ask user about switching ambulance");

                // If another ambulance, confirm first
                switchAmbulanceDialog(newAmbulanceId);

            } else if (!AmbulanceForegroundService.isUpdatingLocation()) {

                Log.d(TAG, "Will retrieve new ambulance");

                // else, if current ambulance is not updating location, retrieve again
                retrieveAmbulance(newAmbulanceId);

            }

        } else {

            Log.d(TAG, "Will retrieve new ambulance");

            // otherwise go ahead!
            retrieveAmbulance(newAmbulanceId);

        }

    }

    private String getAmbulanceIdentifier(final int ambulanceId) {
        String ambulanceIdentifier = "";
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            AmbulancePermission newAmbulance = ambulancePermissions
                    .stream()
                    .filter(ambulancePermission -> ambulancePermission.getAmbulanceId() == ambulanceId)
                    .findAny()
                    .orElse(null);
            if (newAmbulance != null)
                ambulanceIdentifier = newAmbulance.getAmbulanceIdentifier();
        } else {
            for (AmbulancePermission ambulancePermission: ambulancePermissions) {
                if (ambulancePermission.getAmbulanceId() == ambulanceId) {
                    ambulanceIdentifier = ambulancePermission.getAmbulanceIdentifier();
                    break;
                }
            }
        }
        return ambulanceIdentifier;
    }

    private void switchAmbulanceDialog(final int newAmbulanceId) {

        if (newAmbulanceId == -1) {
            Log.i(TAG, "ambulanceId was -1");
            return;
        }

        Log.i(TAG, "Creating switch ambulance dialog");

        String ambulanceIdentifier = getAmbulanceIdentifier(newAmbulanceId);

        // Use the Builder class for convenient dialog construction
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle(R.string.switchAmbulance)
                .setMessage(String.format(getString(R.string.switchToAmbulance), ambulanceIdentifier))
                .setNegativeButton(R.string.no,
                        (dialog, id) -> Log.i(TAG, "Continue with same ambulance"))
                .setPositiveButton(R.string.yes,
                        (dialog, id) -> {

                            Log.d(TAG, String.format("Switching to ambulance %1$s", ambulanceIdentifier));

                            // logout and retrieve new ambulance
                            logoutAmbulance(newAmbulanceId, true);

                        });

        // Create the AlertDialog object and return it
        builder.create().show();

    }

    private void retrieveAmbulance(int ambulanceId) {

        // Get location preference
        SharedPreferences sharedPreferences = PreferenceManager.getDefaultSharedPreferences(this);
        boolean useApproximateLocationAccuracy= sharedPreferences.getBoolean(getString(R.string.useApproximateLocationAccuracyPreferenceKey),
                getResources().getBoolean(R.bool.useApproximateLocationAccuracyDefault));

        // Retrieve ambulance
        Intent ambulanceIntent = new Intent(this, AmbulanceForegroundService.class);
        ambulanceIntent.setAction(AmbulanceForegroundService.Actions.GET_AMBULANCE);
        ambulanceIntent.putExtra(AmbulanceForegroundService.BroadcastExtras.AMBULANCE_ID, ambulanceId);
        ambulanceIntent.putExtra(AmbulanceForegroundService.BroadcastExtras.PRECISE_LOCATION,
                !useApproximateLocationAccuracy);

        // set toast
        Toast.makeText(this, getString(R.string.retrievingAmbulance, getAmbulanceIdentifier(ambulanceId)), Toast.LENGTH_SHORT).show();

        // What to do when GET_AMBULANCE service completes?
        String ambulanceIdentifier = getAmbulanceIdentifier(ambulanceId);
        new OnServiceComplete(this,
                BroadcastActions.SUCCESS,
                BroadcastActions.FAILURE,
                ambulanceIntent) {

            @Override
            public void onSuccess(Bundle extras) {

                // navigate to ambulance fragment
                navigate(R.id.ambulanceFragment);

            }

        }
                .setFailureMessage(getResources().getString(R.string.couldNotRetrieveAmbulance, ambulanceIdentifier))
                .setAlert(new org.emstrack.ambulance.dialogs.AlertDialog(this,
                        getResources().getString(R.string.couldNotStartLocationUpdates)))
                .start();

        // TODO: WHAT SHOULD WE DO HERE?

    }

    public void logoutAmbulance() {
        logoutAmbulance(-1, false);
    }

    public void logoutAmbulance(final int nextAmbulanceId) { logoutAmbulance(nextAmbulanceId, false); }

    public void logoutAmbulance(final int nextAmbulanceId, boolean skipConfirmation) {

        AmbulanceAppData appData = AmbulanceForegroundService.getAppData();
        Call call = AmbulanceForegroundService.getAppData().getCalls().getCurrentCall();
        if (call == null || !call.getCurrentAmbulanceCall().getStatus().equals(AmbulanceCall.STATUS_ACCEPTED))

            // no calls
            if (skipConfirmation) {

                // retrieve next ambulance
                if (nextAmbulanceId != -1) {
                    retrieveAmbulance(nextAmbulanceId);
                }

            } else {

                // ask for confirmation
                Ambulance ambulance = appData.getAmbulance();
                new AlertDialog.Builder(this)
                        .setTitle(getString(R.string.releaseAmbulance))
                        .setMessage(R.string.confirmAmbulanceReleaseMessage)
                        .setPositiveButton(android.R.string.ok,
                                (dialog, which) -> {

                                    // set toast
                                    Toast.makeText(this, getString(R.string.logoutAmbulance, ambulance.getIdentifier()), Toast.LENGTH_SHORT).show();

                                    // Stop current ambulance
                                    Intent intent = new Intent(this, AmbulanceForegroundService.class);
                                    intent.setAction(AmbulanceForegroundService.Actions.STOP_AMBULANCE);

                                    // Chain services
                                    new OnServiceComplete(this,
                                            BroadcastActions.SUCCESS,
                                            BroadcastActions.FAILURE,
                                            intent) {

                                        @Override
                                        public void onSuccess(Bundle extras) {
                                            Log.i(TAG, "ambulance released");

                                            // retrieve next ambulance
                                            if (nextAmbulanceId != -1) {
                                                retrieveAmbulance(nextAmbulanceId);
                                            }
                                        }

                                    }
                                            .setFailureMessage(getString(R.string.couldNotReleaseAmbulance))
                                            .setAlert(new AlertSnackbar(this))
                                            .start();

                                })
                        .setNegativeButton(android.R.string.cancel,
                                (dialog, which) -> { /* do nothing */ })
                        .create()
                        .show();
            }

        else {

            // Prompt to end call first
            promptEndCallDialog(call.getId(), -1, nextAmbulanceId);

        }
    }

    public void startVideoCall(Client client, String callMode) {

        // Get app data
        AmbulanceAppData appData = AmbulanceForegroundService.getAppData();

        // Get client id
        String clientId = AmbulanceForegroundService.getProfileClientId(MainActivity.this);
        String username = appData.getCredentials().getUsername();

        // get url
        Credentials credentials = AmbulanceForegroundService.getAppData().getCredentials();
        String[] baseUrl = credentials.getApiServerUri().split("://");
        Log.d(TAG, "baseUrl = ");
        for (String url: baseUrl){
            Log.d(TAG, url);
        }

        // Build user url token login
        Uri.Builder builder = new Uri.Builder();
        builder.scheme(baseUrl[0])
                .authority(baseUrl[1])
                .appendPath("video")
                .appendQueryParameter("callUsername", client.getUsername())
                .appendQueryParameter("callClientId", client.getClientId())
                .appendQueryParameter("callMode", callMode);
        if (callMode.equals("new")) {
            // Append as proxy
            builder.appendQueryParameter("callProxyUsername", username)
                    .appendQueryParameter("callProxyClientId", clientId);
        }
        String url = builder.build().toString();
        TokenLogin tokenLogin = new TokenLogin(url);

        APIService service = APIServiceGenerator.createService(APIService.class);
        retrofit2.Call<TokenLogin> callTokenLogin = service.getTokenLogin(username, tokenLogin);

        new OnAPICallComplete<TokenLogin>(callTokenLogin) {

            @Override
            public void onSuccess(TokenLogin token) {

                Log.d(TAG, "Successfully posted token login, will redirect to browser");

                Uri.Builder uriBuilder = new Uri.Builder();
                uriBuilder.scheme(baseUrl[0])
                        .authority(baseUrl[1])
                        .appendPath("en")
                        .appendPath("auth")
                        .appendPath("login")
                        .appendPath(token.getToken());
                Uri uri = uriBuilder.build();

                // Custom tab integration
                CustomTabsSession session = customTabsClient.newSession(new CustomTabsCallback() {
                    @Override
                    public void onNavigationEvent(int navigationEvent, Bundle extras) {
                        if (navigationEvent == NAVIGATION_ABORTED || navigationEvent == NAVIGATION_FAILED || navigationEvent == TAB_HIDDEN) {
                            Log.d(TAG, String.format("Navigation event: %1$d, will send bye", navigationEvent));

                            // send bye
                            Intent serviceIntent = new Intent(MainActivity.this,
                                    AmbulanceForegroundService.class);
                            serviceIntent.setAction(AmbulanceForegroundService.Actions.WEBRTC_MESSAGE);
                            serviceIntent.putExtra(AmbulanceForegroundService.BroadcastExtras.WEBRTC_TYPE, "bye");
                            serviceIntent.putExtra(AmbulanceForegroundService.BroadcastExtras.WEBRTC_CLIENT_USERNAME, client.getUsername());
                            serviceIntent.putExtra(AmbulanceForegroundService.BroadcastExtras.WEBRTC_CLIENT_ID, client.getClientId());
                            startService(serviceIntent);

                        }
                    }
                });

                CustomTabsIntent.Builder builder = new CustomTabsIntent.Builder();
                assert session != null;
                builder.setSession(session);
                builder.setCloseButtonIcon(BitmapFactory.decodeResource(getResources(), R.drawable.ic_arrow_back));

                CustomTabsIntent customTabsIntent = builder.build();
                customTabsIntent.launchUrl(MainActivity.this, uri);
            }

            @Override
            public void onFailure(Throwable t) {
                super.onFailure(t);

                Toast.makeText(MainActivity.this,
                        R.string.couldNotRedirectToBrowser,
                        Toast.LENGTH_SHORT).show();

            }

        }
                .start();

    }

    public void promptVideoCallAccept(String username, String clientId) {

        Log.i(TAG, "Creating accept video call dialog");

        // return if already prompting for call
        if (promptVideoCallDialog != null) {
            Log.i(TAG, "Already prompting for call. Simply return.");
            return;
        }

        // Use the Builder class for convenient dialog construction
        AlertDialog.Builder builder = new AlertDialog.Builder(this);

        // Create call view
        View view = getLayoutInflater().inflate(R.layout.video_answer_call_dialog, null);

        TextView fromText = view.findViewById(R.id.video_answer_from_text);
        fromText.setText(username);

        TextView atText = view.findViewById(R.id.video_answer_at_text);
        atText.setText(clientId);

        // build dialog
        builder.setTitle(R.string.video_call)
                .setView(view)
                .setCancelable(false)
                .setPositiveButton(R.string.answer, (dialog, id) -> {

                    Log.i(TAG, "Video call accepted");

                    Toast.makeText(MainActivity.this,
                            R.string.calling_client,
                            Toast.LENGTH_SHORT).show();

                    Client client = new Client(username, clientId);
                    startVideoCall(client, "answer");

                    promptVideoCallDialog = null;

                })
                .setNegativeButton(R.string.decline, (dialog, id) -> {

                    Log.i(TAG, "Video call cancelled");

                    Intent serviceIntent = new Intent(MainActivity.this,
                            AmbulanceForegroundService.class);
                    serviceIntent.setAction(AmbulanceForegroundService.Actions.WEBRTC_MESSAGE);
                    serviceIntent.putExtra(AmbulanceForegroundService.BroadcastExtras.WEBRTC_TYPE, "decline");
                    serviceIntent.putExtra(AmbulanceForegroundService.BroadcastExtras.WEBRTC_CLIENT_USERNAME, username);
                    serviceIntent.putExtra(AmbulanceForegroundService.BroadcastExtras.WEBRTC_CLIENT_ID, clientId);
                    startService(serviceIntent);

                    promptVideoCallDialog = null;

                } );

        // Create the AlertDialog object and display it
        promptVideoCallDialog = builder.create();
        promptVideoCallDialog.show();

    }

    public void promptVideoCallNew() {

        Log.i(TAG, "Creating new video call dialog");

        MainActivity self = this;

        // Retrieve list of online clients

        APIService service = APIServiceGenerator.createService(APIService.class);
        retrofit2.Call<List<Client>> callOnlineClients = service.getOnlineClients();

        new OnAPICallComplete<List<Client>>(callOnlineClients) {

            @Override
            public void onSuccess(List<Client> clients) {

                Log.d(TAG, "Got list of online clients");

                // Get app data
                AmbulanceAppData appData = AmbulanceForegroundService.getAppData();

                // Get client id
                String clientId = AmbulanceForegroundService.getProfileClientId(MainActivity.this);
                String username = appData.getCredentials().getUsername();

                // Use the Builder class for convenient dialog construction
                AlertDialog.Builder builder = new AlertDialog.Builder(self);

                // Create call view
                View view = getLayoutInflater().inflate(R.layout.video_new_call_dialog, null);

                // Populate client list
                ArrayAdapter<String> userNames = new ArrayAdapter<>(self, android.R.layout.simple_spinner_item);

                // add select server message
                userNames.add(getString(R.string.select_user));

                int selfIndex = -1;
                int i = 0;
                for (Client client: clients) {
                    if (client.getUsername().equals(username) && client.getClientId().equals(clientId)) {
                        // self
                        selfIndex = i++;
                        continue;
                    }
                    userNames.add( client.getUsername() + " @ " + client.getClientId() );
                    i++;
                }

                // Attach to spinner
                Spinner spinnerVideoCall = view.findViewById(R.id.spinner_video_call);
                userNames.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
                spinnerVideoCall.setAdapter(userNames);

                // build dialog
                int finalSelfIndex = selfIndex;
                builder.setTitle(R.string.new_video_call_title)
                        .setView(view)
                        .setCancelable(false)
                        .setPositiveButton(R.string.tocall,
                                (dialog, id) -> {

                                    // first entry is prompt
                                    int index = spinnerVideoCall.getSelectedItemPosition() - 1;
                                    if (index >= 0) {

                                        // increment index if past self
                                        if (finalSelfIndex >= 0 && index >= finalSelfIndex)
                                            index++;

                                        final Client client = clients.get(index);
                                        Log.i(TAG, "Calling: index=" + index + ", client=" + client);

                                        Toast.makeText(MainActivity.this,
                                                R.string.calling_client,
                                                Toast.LENGTH_SHORT).show();

                                        startVideoCall(client, "new");

                                    } else {

                                        new AlertSnackbar(self).alert(getResources().getString(R.string.error_invalid_client));

                                    }

                                })
                        .setNegativeButton(R.string.cancel, (dialog, id) -> Log.i(TAG, "Video call cancelled"));

                // Create the AlertDialog object and display it
                builder.create().show();

            }

            @Override
            public void onFailure(Throwable t) {
                super.onFailure(t);

                Toast.makeText(MainActivity.this,
                        R.string.could_not_retrieve_onilne_clients,
                        Toast.LENGTH_SHORT).show();

            }

        }
                .start();

    }

    public void promptCallAccept(final int nextCallId) {

        Log.i(TAG, "Creating accept dialog");

        // Get app data
        AmbulanceAppData appData = AmbulanceForegroundService.getAppData();

        // Get calls
        CallStack calls = appData.getCalls();

        // Gather call details
        Call call = calls.get(nextCallId);
        if (call == null) {
            Log.d(TAG, "Invalid call id '" + nextCallId + "'");
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

        // Does it have a current call?
        Call currentCall = calls.getCurrentCall();
        if (currentCall != null) {

            // Already currently handling this call
            int currentCallId = currentCall.getId();
            Log.d(TAG, "Already handling call " + currentCallId);

            if (currentCallId != nextCallId) {

                Log.d(TAG, "Will prompt to end it first.");
                promptEndCallDialog(currentCall.getId(), nextCallId);

            }
            return;

        }

        // Use the Builder class for convenient dialog construction
        AlertDialog.Builder builder = new AlertDialog.Builder(this);

        // Build patient list
        StringBuilder patientsText = new StringBuilder();
        List<Patient> patients = call.getPatientSet();
        if (patients != null && patients.size() > 0) {
            for (Patient patient : patients) {
                if (patientsText.length() > 0)
                    patientsText.append(", ");
                patientsText.append(patient.getName());
                if (patient.getAge() != null)
                    patientsText.append(" (").append(patient.getAge()).append(")");
            }
        } else
            patientsText = new StringBuilder(getResources().getString(R.string.noPatientAvailable));

        // Get number of waypoints
        int numberOfWaypoints = ambulanceCall.getWaypointSet().size();

        // Create call view
        View view = getLayoutInflater().inflate(R.layout.call_dialog, null);

        ((TextView) view.findViewById(R.id.callPriorityLabel)).setText(R.string.nextCall);

        // hide browser buttons
        view.findViewById(R.id.callEndButton).setVisibility(View.GONE);
        view.findViewById(R.id.callMessageButton).setVisibility(View.GONE);
        view.findViewById(R.id.callPatientShowAddIcon).setVisibility(View.GONE);

        // waypoint browser
        View waypointBrowser = view.findViewById(R.id.callWaypointBrowser);
        waypointBrowser.findViewById(R.id.waypointBrowserToolbar).setVisibility(View.GONE);
        RecyclerView waypointBrowserRecyclerView = waypointBrowser.findViewById(R.id.waypointBrowserRecyclerView);

        // initialize recylcer view
        LinearLayoutManager waypointLinearLayoutManager = new LinearLayoutManager(this,
                LinearLayoutManager.HORIZONTAL, false);
        waypointBrowserRecyclerView.setLayoutManager(waypointLinearLayoutManager);

        // attach snap helper
        SnapHelper snapHelper = new LinearSnapHelper();
        snapHelper.attachToRecyclerView(waypointBrowserRecyclerView);

        // Install adapter
        WaypointInfoRecyclerAdapter adapter =
                new WaypointInfoRecyclerAdapter(this, ambulanceCall.getWaypointSet(), true);
        waypointBrowserRecyclerView.setAdapter(adapter);

        // go to next waypoint
        int position = ambulanceCall.getNextWaypointPosition();
        if (position != -1) {
            waypointLinearLayoutManager.scrollToPosition(position);
        }

        // Set priority
        TextView callPriorityTextView = view.findViewById(R.id.callPriorityTextView);
        callPriorityTextView.setText(call.getPriority());
        callPriorityTextView.setBackgroundColor(callPriorityBackgroundColors.get(call.getPriority()));
        callPriorityTextView.setTextColor(callPriorityForegroundColors.get(call.getPriority()));

        int priorityCodeInt = call.getPriorityCode();
        if (priorityCodeInt < 0) {
            ((TextView) view.findViewById(R.id.callPriorityPrefix)).setText("");
            ((TextView) view.findViewById(R.id.callPrioritySuffix)).setText("");
        } else {
            PriorityCode priorityCode = appData.getPriorityCodes().get(priorityCodeInt);
            ((TextView) view.findViewById(R.id.callPriorityPrefix)).setText(String.format("%d-", priorityCode.getPrefix()));
            ((TextView) view.findViewById(R.id.callPrioritySuffix)).setText(String.format("-%s", priorityCode.getSuffix()));
        }

        // Set radio code
        int radioCodeInt = call.getRadioCode();
        if (radioCodeInt < 0) {
            ((TextView) view.findViewById(R.id.callRadioCodeText)).setText(R.string.unavailable);
        } else {
            RadioCode radioCode = appData.getRadioCodes().get(radioCodeInt);
            ((TextView) view.findViewById(R.id.callRadioCodeText)).setText(String.format("%d: %s", radioCode.getId(), radioCode.getLabel()));
        }

        ((TextView) view.findViewById(R.id.callDetailsText)).setText(call.getDetails());

        ((TextView) view.findViewById(R.id.callPatientsText)).setText(patientsText.toString());
        ((TextView) view.findViewById(R.id.callNumberOfWaypointsText)).setText(String.valueOf(numberOfWaypoints));

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
                            serviceIntent.putExtra(AmbulanceForegroundService.BroadcastExtras.CALL_ID, nextCallId);
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
                            serviceIntent.putExtra(AmbulanceForegroundService.BroadcastExtras.CALL_ID, nextCallId);
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
                            serviceIntent.putExtra(AmbulanceForegroundService.BroadcastExtras.CALL_ID, nextCallId);
                            startService(serviceIntent);

                        });

        // Create the AlertDialog object and display it
        builder.create().show();

    }

    private void handleNextCallAndAmbulance(int nextCallId, int nextAmbulanceId) {
        if (nextCallId != -1) {
            Log.d(TAG, "Will prompt next call id = " + nextCallId);
            promptCallAccept(nextCallId);
        } else if (nextAmbulanceId != -1) {
            Log.d(TAG, "Will retrieve next ambulance id = " + nextAmbulanceId);
            // TODO: Should we ask user again?
            retrieveAmbulance(nextAmbulanceId);
        } else {
            Log.d(TAG, "Will do nothing");
        }
    }

    private void endOrSuspendCall(int callId, int nextCallId, int nextAmbulanceId,
                                  String toastMessage, String action) {

        Log.i(TAG, toastMessage);
        Toast.makeText(MainActivity.this,
                toastMessage,
                Toast.LENGTH_SHORT).show();

        Intent serviceIntent = new Intent(MainActivity.this,
                AmbulanceForegroundService.class);
        serviceIntent.setAction(action);
        serviceIntent.putExtra(AmbulanceForegroundService.BroadcastExtras.CALL_ID, callId);

        new OnServiceComplete(this,
                AmbulanceForegroundService.BroadcastActions.CALL_COMPLETED,
                BroadcastActions.FAILURE,
                serviceIntent) {

            @Override
            public void onSuccess(Bundle extras) {

                Log.d(TAG, "Successfully completed call");
                handleNextCallAndAmbulance(nextCallId, nextAmbulanceId);

            }
        }
                .setSuccessIdCheck(false)
                .start();

    }

    public void promptEndCallDialog(int callId) {
        promptEndCallDialog(callId, -1, -1);
    }

    public void promptEndCallDialog(int callId, int nextCallId) {
        promptEndCallDialog(callId, nextCallId, -1);
    }

    public void promptEndCallDialog(int callId, int nextCallId, int nextAmbulanceId) {

        Log.d(TAG, "Creating end call dialog");

        // Gather call details
        final Call call = AmbulanceForegroundService.getAppData().getCalls().getCurrentCall();
        if (call == null) {

            // Not currently handling call
            Log.d(TAG, "Not currently handling call");

            handleNextCallAndAmbulance(nextCallId, nextAmbulanceId);
            return;

        } else if (call.getId() != callId) {

            // Not currently handling this call
            Log.d(TAG, "Not currently handling call " + call.getId() + ". Aborting...");
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

                            endOrSuspendCall(call.getId(), nextCallId, nextAmbulanceId,
                                    getString(R.string.suspendingCall), AmbulanceForegroundService.Actions.CALL_SUSPEND);

//                            Toast.makeText(MainActivity.this,
//                                    R.string.suspendingCall,
//                                    Toast.LENGTH_SHORT).show();
//
//                            Log.i(TAG, "Suspending call");
//
//                            Intent serviceIntent = new Intent(MainActivity.this,
//                                    AmbulanceForegroundService.class);
//                            serviceIntent.setAction(AmbulanceForegroundService.Actions.CALL_SUSPEND);
//                            serviceIntent.putExtra(AmbulanceForegroundService.BroadcastExtras.CALL_ID, call.getId());
//
//                            new OnServiceComplete(this,
//                                    AmbulanceForegroundService.BroadcastActions.CALL_COMPLETED,
//                                    BroadcastActions.FAILURE,
//                                    serviceIntent) {
//
//                                @Override
//                                public void onSuccess(Bundle extras) {
//
//                                    Log.d(TAG, "Successfully completed call");
//                                    handleNextCallAndAmbulance(nextCallId, nextAmbulanceId);
//
//                                }
//                            }
//                                    .setSuccessIdCheck(false)
//                                    .start();

                        })
                .setPositiveButton(R.string.end,
                        (dialog, id) -> {

                            endOrSuspendCall(call.getId(), nextCallId, nextAmbulanceId,
                                    getString(R.string.endingCall), AmbulanceForegroundService.Actions.CALL_FINISH);

//                            Toast.makeText(MainActivity.this,
//                                    R.string.endingCall,
//                                    Toast.LENGTH_SHORT).show();
//
//                            Log.i(TAG, "Ending call");
//
//                            Intent serviceIntent = new Intent(MainActivity.this,
//                                    AmbulanceForegroundService.class);
//                            serviceIntent.setAction(AmbulanceForegroundService.Actions.CALL_FINISH);
//
//                            new OnServiceComplete(this,
//                                    AmbulanceForegroundService.BroadcastActions.CALL_COMPLETED,
//                                    BroadcastActions.FAILURE,
//                                    serviceIntent) {
//
//                                @Override
//                                public void onSuccess(Bundle extras) {
//
//                                    Log.d(TAG, "Successfully completed call");
//                                    handleNextCallAndAmbulance(nextCallId, nextAmbulanceId);
//
//                                }
//                            }
//                                    .setSuccessIdCheck(false)
//                                    .start();


                        })
                .setOnCancelListener(
                        dialog -> {

                            Log.i(TAG, "Cancelling dialog");

                            if (logoutAfterFinish)
                                logoutAfterFinish = false;

                        });

        // Create the AlertDialog object and return it
        builder.create().show();
    }

    public void promptNextWaypointDialog(final int callId) {

        if (this.promptingNextWaypoint) {
            Log.i(TAG, "Already prompting next waypoint. Returning...");
            return;
        }

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

        // Create base spinner
        final Spinner othersSpinner = view.findViewById(R.id.spinnerOthers);
        othersSpinner.setAdapter(othersListAdapter);

        // Set spinner click listeners to make sure only base or hospital are selected
        hospitalSpinner.setOnItemSelectedListener(
                new OnItemSelectedListener() {
                    @Override
                    public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                        if (position > 0 && baseSpinner.getSelectedItemPosition() > 0) {
                            baseSpinner.setSelection(0);
                        }
                        if (position > 0 && othersSpinner.getSelectedItemPosition() > 0) {
                            othersSpinner.setSelection(0);
                        }
                    }

                    @Override
                    public void onNothingSelected(AdapterView<?> parent) {

                    }
                }
        );

        baseSpinner.setOnItemSelectedListener(
                new OnItemSelectedListener() {
                    @Override
                    public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                        if (position > 0 && hospitalSpinner.getSelectedItemPosition() > 0) {
                            hospitalSpinner.setSelection(0);
                        }
                        if (position > 0 && othersSpinner.getSelectedItemPosition() > 0) {
                            othersSpinner.setSelection(0);
                        }
                    }

                    @Override
                    public void onNothingSelected(AdapterView<?> parent) {

                    }
                }
        );

        othersSpinner.setOnItemSelectedListener(
                new OnItemSelectedListener() {
                    @Override
                    public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                        if (position > 0 && hospitalSpinner.getSelectedItemPosition() > 0) {
                            hospitalSpinner.setSelection(0);
                        }
                        if (position > 0 && baseSpinner.getSelectedItemPosition() > 0) {
                            baseSpinner.setSelection(0);
                        }
                    }

                    @Override
                    public void onNothingSelected(AdapterView<?> parent) {

                    }
                }
        );

        // build dialog
        promptingNextWaypoint = true;
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
                                waypoint = "{\"order\":" + maximumOrder + ",\"location_id\":" + hospital.getHospitalId() + "}";
                            }

                            int selectedBase = baseSpinner.getSelectedItemPosition();
                            if (selectedBase > 0) {
                                Location base = bases.get(selectedBase - 1);
                                Log.d( TAG, "base = " + base);
                                waypoint = "{\"order\":" + maximumOrder + ",\"location_id\":" + base.getId() + "}";
                            }

                            int selectedOthers = othersSpinner.getSelectedItemPosition();
                            if (selectedOthers > 0) {
                                Location others = otherLocations.get(selectedOthers - 1);
                                Log.d( TAG, "other = " + others);
                                waypoint = "{\"order\":" + maximumOrder + ",\"location_id\":" + others.getId() + "}";
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

                            promptingNextWaypoint = false;
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

                            promptingNextWaypoint = false;
                        })
                .setNeutralButton(R.string.endCall,
                        (dialog, id) -> {

                            Log.i(TAG, "Ending call");

                            promptingNextWaypoint = false;
                            promptEndCallDialog(callId);
                        });

        // Create the AlertDialog object and display it
        builder.create().show();

    }

    public void logout() {

        Log.d(TAG,"Logout");

        // Create stop foreground service intent
        Intent stopIntent = new Intent(this, AmbulanceForegroundService.class);
        stopIntent.setAction(AmbulanceForegroundService.Actions.STOP_SERVICE);

        // Chain services
        new OnServiceComplete(this,
                BroadcastActions.SUCCESS,
                BroadcastActions.FAILURE,
                stopIntent) {

            @Override
            public void onSuccess(Bundle extras) {
                Log.i(TAG, "onSuccess");

                // navigate to login
                navigatePopBackStack(R.id.loginFragment, false);

            }

            @Override
            public void onFailure(Bundle extras) {
                super.onFailure(extras);
            }
        }
                .setFailureMessage(getString(R.string.couldNotLogout))
                .setAlert(new AlertSnackbar(this))
                .start();

    }

    public void promptLogout() {

        Call call = AmbulanceForegroundService.getAppData().getCalls().getCurrentCall();
        if (call == null) {

            // Go straight to dialog
            new AlertDialog.Builder(this)
                    .setTitle(R.string.logout)
                    .setMessage(R.string.logout_confirm)
                    .setNegativeButton(R.string.cancel,
                            (dialog, which) -> {
                                /* do nothing */
                            })
                    .setPositiveButton(
                            R.string.ok,
                            (dialog, which) -> {

                                Log.i(TAG, "LogoutDialog: OK Button Clicked");
                                logout();

                            })
                    .create()
                    .show();

        } else {

            // Will ask to logout
            logoutAfterFinish = true;

            // Prompt to end call first
            promptEndCallDialog(call.getId());

        }

    }

    @Override
    public void onBackPressed() {
        Log.d(TAG, "Back button pressed with mode = " + backButtonMode);
        if (backButtonMode == BackButtonMode.LOGOUT) {
            promptLogout();
        } else if (backButtonMode == BackButtonMode.UP){
            navigatePopBackStack();
        } else if (backButtonMode == BackButtonMode.FINISH) {
            finish();
        }
    }

    @Override
    public void supportNavigateUpTo(@NonNull Intent upIntent) {
        NavController navController = navHostFragment.getNavController();
        if ( !navController.navigateUp() )
            super.supportNavigateUpTo(upIntent);
    }

    public Map<String, Integer> getCallPriorityBackgroundColors() {
        return callPriorityBackgroundColors;
    }

    public Map<String, Integer> getCallPriorityForegroundColors() {
        return callPriorityForegroundColors;
    }

}