package org.emstrack.ambulance;

import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.res.Configuration;
import android.os.Bundle;
import android.support.design.widget.NavigationView;
import android.support.design.widget.TabLayout;
import android.support.v4.view.ViewPager;
import android.support.v4.widget.DrawerLayout;
import android.support.v7.app.ActionBar;
import android.support.v7.app.ActionBarDrawerToggle;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.MenuItem;
import android.view.View;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.TextView;

import org.emstrack.mqtt.MqttProfileClient;

/**
 * This is the main activity -- the default screen
 */
public class MainActivity extends AppCompatActivity {

    private static final String TAG = MainActivity.class.getSimpleName();
    private int ambulanceId = -1;

    private DrawerLayout mDrawer;
    private NavigationView nvDrawer;
    private ActionBarDrawerToggle drawerToggle;
    private Toolbar toolbar;
    static TextView statusText;
    private ImageButton panicButton;
    private String titleText;


    /**
     * @param savedInstanceState
     */
    @Override
    protected void onCreate(Bundle savedInstanceState) {

        super.onCreate(savedInstanceState);

        setContentView(R.layout.activity_main);

        // Get data from Login and place it into the ambulance
        ambulanceId = Integer
                .parseInt(getIntent().getStringExtra("SELECTED_AMBULANCE_ID"));

        statusText = (TextView) findViewById(R.id.statusText);
        statusText.setText(titleText);
        panicButton = (ImageButton) findViewById(R.id.panicButton);
        panicButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                panicPopUp();
            }
        });

        // Set a Toolbar to replace the ActionBar.
        toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        // getSupportActionBar().setDisplayShowTitleEnabled(false);

        // Find our drawer view
        mDrawer = (DrawerLayout) findViewById(R.id.drawer_layout);
        drawerToggle = setupDrawerToggle();
        mDrawer.addDrawerListener(drawerToggle);

        // Find our drawer view
        nvDrawer = (NavigationView) findViewById(R.id.nvView);
        // Setup drawer view
        setupDrawerContent(nvDrawer);

        //set up TabLayout Structure
        TabLayout tabLayout = (TabLayout) findViewById(R.id.tab_layout_home);
        tabLayout.addTab(tabLayout.newTab().setText("Dispatcher"));
        tabLayout.addTab(tabLayout.newTab().setText("Hospital"));
//        tabLayout.addTab(tabLayout.newTab().setText("GPS"));

        //pager
        final ViewPager viewPager = (ViewPager) findViewById(R.id.pager);
        tabLayout.setTabGravity(TabLayout.GRAVITY_FILL);

        //Setup Adapter for tabLayout
        final Pager adapter = new Pager(getSupportFragmentManager(), tabLayout.getTabCount());
        viewPager.setAdapter(adapter);


        viewPager.addOnPageChangeListener(new TabLayout.TabLayoutOnPageChangeListener(tabLayout));
        tabLayout.addOnTabSelectedListener(new TabLayout.OnTabSelectedListener() {
            @Override
            public void onTabSelected(TabLayout.Tab tab) {
                viewPager.setCurrentItem(tab.getPosition());
            }

            @Override
            public void onTabUnselected(TabLayout.Tab tab) {
            }

            @Override
            public void onTabReselected(TabLayout.Tab tab) {
            }
        });

//        ambulanceApp.mqttMaster();
//        titleText = currAmbulance.getLicensePlate() + " - " + ambulanceApp.getCurrStatus();
//        statusText.setText(titleText);

/*

        // Retrieve client
        final MqttProfileClient profileClient = ((AmbulanceApp) getApplication()).getProfileClient();

        // Subscribe to ambulance to retrieve data

        // Test that Ambulance class made it through
        currAmbulance = (Ambulance) getIntent().getSerializableExtra("AmbulanceClass");

        if(currAmbulance != null) {
            Log.d("MAIN_ACTIVITY", "AmbulancePassed: " + currAmbulance.getLicensePlate() + " ID: " + currAmbulance.getId());

            titleText = currAmbulance.getLicensePlate() + " - " + ambulanceApp.getCurrStatus();
            Log.d("MAIN_TITLE", titleText);
        }
*/

    }


    //Hamburger Menu setup
    private ActionBarDrawerToggle setupDrawerToggle(){
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

        Class activityClass;
        switch(menuItem.getItemId()) {
            case R.id.home:
                activityClass = MainActivity.class;
                break;
            case R.id.logout:
                //ambulanceApp.setUserLoggedIn(false);
                //ambulanceApp.logout();
                activityClass = LoginActivity.class;
                break;
            default:
                activityClass = MainActivity.class;
        }

        // Highlight the selected item has been done by NavigationView
        menuItem.setChecked(true);

        Intent i = new Intent(this, activityClass);
        //i.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);

        if(menuItem.getItemId() == R.id.logout){
            finish();
            startActivity(i);
        }

        menuItem.setChecked(false);

        // Close the navigation drawer
        mDrawer.closeDrawers();
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

    public void panicPopUp(){
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

    static void updateStatus(String newStatus){
        // statusText.setText(currAmbulance.getLicensePlate() + " - " + newStatus);
    }
    @Override
    public void onBackPressed() {
        finish();
    }

}