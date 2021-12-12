package org.emstrack.ambulance;

import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.res.Configuration;
import android.graphics.BitmapFactory;
import android.graphics.PorterDuff;
import android.net.Uri;
import android.os.Bundle;
import android.os.CountDownTimer;
import android.util.Log;
import android.view.MenuItem;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.ActionBarDrawerToggle;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.browser.customtabs.CustomTabsCallback;
import androidx.browser.customtabs.CustomTabsClient;
import androidx.browser.customtabs.CustomTabsIntent;
import androidx.browser.customtabs.CustomTabsServiceConnection;
import androidx.browser.customtabs.CustomTabsSession;
import androidx.core.content.ContextCompat;
import androidx.drawerlayout.widget.DrawerLayout;
import androidx.fragment.app.Fragment;
import androidx.localbroadcastmanager.content.LocalBroadcastManager;
import androidx.viewpager2.widget.ViewPager2;

import com.google.android.material.navigation.NavigationView;
import com.google.android.material.tabs.TabLayout;
import com.google.android.material.tabs.TabLayoutMediator;

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
import org.emstrack.models.Waypoint;
import org.emstrack.models.api.APIService;
import org.emstrack.models.api.APIServiceGenerator;
import org.emstrack.models.api.OnAPICallComplete;

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

    private static final int MAP_TAB = 0;
    private static final int HOSPITALS_TAB = 1;
    private static final int AMBULANCE_TAB = 2;
    private static final int EQUIPMENT_TAB = 3;

    private static final DecimalFormat df = new DecimalFormat();

    private static final float enabledAlpha = 1.0f;
    private static final float disabledAlpha = 0.25f;

    private List<HospitalPermission> hospitalPermissions;
    private List<Location> bases;
    private List<Location> otherLocations;

    private ArrayAdapter<String> hospitalListAdapter;
    private ArrayAdapter<String> baseListAdapter;
    private ArrayAdapter<String> othersListAdapter;

    private DrawerLayout mDrawer;
    private ActionBarDrawerToggle drawerToggle;
    private Toolbar toolbar;
    private ImageView onlineIcon;
    private ImageView trackingIcon;
    private MainActivityBroadcastReceiver receiver;
    private Map<String, Integer> callPriorityBackgroundColors;
    private Map<String, Integer> callPriorityForegroundColors;

    private boolean logoutAfterFinish;
    private ViewPager2 viewPager;

    private AlertDialog promptVideoCallDialog;
    private CustomTabsClient customTabsClient;
    private FragmentPager adapter;

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
                        if (AmbulanceForegroundService.getAppData().getAmbulance() == null) {
                            // no selected ambulance, hide tab
                            Log.i(TAG, "Will hide equipment tab");
                            adapter.hideTab(EQUIPMENT_TAB);
                        } else {
                            // selected ambulance, show tab
                            Log.i(TAG, "Will show equipment tab");
                            adapter.addTab(EQUIPMENT_TAB);
                        }

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
                        int myVectorColor = ContextCompat.getColor(MainActivity.this, R.color.colorRed);
                        trackingIcon.setColorFilter(myVectorColor, PorterDuff.Mode.SRC_IN);

                        if (viewPager.getCurrentItem() != AMBULANCE_TAB) {
                            // set current pager to ambulance
                            viewPager.setCurrentItem(AMBULANCE_TAB);
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

        // Get appData
        AmbulanceAppData appData = AmbulanceForegroundService.getAppData();

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

        // Panic button
        ImageButton panicButton = findViewById(R.id.panicButton);
        panicButton.setOnLongClickListener(
                v -> panicPopUp());

        // Video Call button
        ImageButton videoCallButton = findViewById(R.id.videoCallButton);
        videoCallButton.setOnClickListener(v -> promptVideoCallNew());

        // Hide video button if video is not enabled
        Settings settings = appData.getSettings();
        if (!settings.isEnableVideo()) {
            videoCallButton.setVisibility(View.GONE);
        }

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
        adapter = new FragmentPager(
                getSupportFragmentManager(),
                getLifecycle(),
                new Fragment[]{
                        new MapFragment(),
                        new HospitalFragment(),
                        new AmbulanceFragment(),
                        new EquipmentFragment()
                },
                new int[] {R.drawable.ic_globe, R.drawable.ic_hospital, R.drawable.ic_ambulance, R.drawable.ic_briefcase_medical},
                R.layout.tab_icon
        );
        adapter.setUserInputEnabled(MAP_TAB, false);
        viewPager.setAdapter(adapter);
        viewPager.registerOnPageChangeCallback(new ViewPager2.OnPageChangeCallback() {
            @Override
            public void onPageSelected(int index) {
                super.onPageSelected(index);
                // set input enabled
                int position = adapter.getPagePosition(index);
                boolean inputEnabled = adapter.getUserInputEnabled(position);
                if (inputEnabled != viewPager.isUserInputEnabled()) {
                    viewPager.setUserInputEnabled(inputEnabled);
                }
            }
        });

        TabLayout tabLayout = findViewById(R.id.tab_layout_home);
        tabLayout.setTabGravity(TabLayout.GRAVITY_FILL);

        adapter.setTabLayoutMediator(tabLayout, viewPager);
        // hide equipment tab
        adapter.hideTab(EQUIPMENT_TAB);

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

        hospitalPermissions = new ArrayList<>();
        Profile profile = appData.getProfile();
        if (profile != null) {
            hospitalPermissions = profile.getHospitals();
        }
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

    }

    @Override
    protected void onPostCreate(Bundle savedInstanceState) {
        super.onPostCreate(savedInstanceState);

        // Sync the toggle state after onRestoreInstanceState has occurred.
        drawerToggle.syncState();
    }

    @Override
    public void onConfigurationChanged(@NonNull Configuration newConfig) {
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
        if (ambulance == null) {
            // no ambulance is selected
            // hide equipment tab
            adapter.hideTab(EQUIPMENT_TAB);
        } else {
            // ambulance is selected
            // show equipment tab
            adapter.addTab(EQUIPMENT_TAB);
            // check calls
            CallStack pendingCalls = appData.getCalls();
            Call call = pendingCalls.getCurrentCall();
            if (call != null) {
                // handling call, go to ambulance tab
                if (viewPager.getCurrentItem() != AMBULANCE_TAB)
                    viewPager.setCurrentItem(AMBULANCE_TAB);
            } else {
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
        if (appData.getSettings().isEnableVideo())
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

            // TODO: add settings page

        }

    }

    public boolean panicPopUp() {
        final long DIALOG_DISMISS_TIME = 3000; // miliseconds

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

        //timer to dismiss dialog after DIALOG_DISMISS_TIME ms
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
                        .setPositiveButton(R.string.call,
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

        ((TextView) view.findViewById(R.id.callPriorityLabel)).setText(R.string.nextCall);

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
                new AdapterView.OnItemSelectedListener() {
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
                new AdapterView.OnItemSelectedListener() {
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
                new AdapterView.OnItemSelectedListener() {
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