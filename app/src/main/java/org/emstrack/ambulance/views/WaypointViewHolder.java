package org.emstrack.ambulance.views;

import static org.emstrack.ambulance.util.FormatUtils.formatDistance;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Intent;
import android.content.SharedPreferences;
import android.net.Uri;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.TextView;

import androidx.preference.PreferenceManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.gms.maps.model.LatLng;

import org.emstrack.ambulance.MainActivity;
import org.emstrack.ambulance.R;
import org.emstrack.ambulance.services.AmbulanceForegroundService;
import org.emstrack.models.GPSLocation;
import org.emstrack.models.Location;
import org.emstrack.models.Waypoint;

import java.net.URLEncoder;
import java.text.DecimalFormat;
import java.util.Locale;

/**
 * Holds the waypoint data
 * @author Mauricio de Oliveira
 * @since 7/07/2020
 */

public class WaypointViewHolder extends RecyclerView.ViewHolder {

    private static final String TAG = WaypointViewHolder.class.getSimpleName();

    private final TextView waypointAddressLine0;
    private final TextView waypointAddressLine1;
    private final TextView waypointAddressLine2;
    private final TextView waypointDistance;

    private final View callNextWaypointLocationButton;
    private final View callNextWaypointToMapsButton;
    private final Activity activity;
    private final View waypointInfoLayout;
    private final boolean hideButtons;
    private Waypoint waypoint;


    public WaypointViewHolder(Activity activity, View view, boolean hideButtons) {
        super(view);

        this.activity = activity;
        this.hideButtons = hideButtons;

        waypointInfoLayout = view.findViewById(R.id.waypointInfoLayout);
        waypointAddressLine0 = view.findViewById(R.id.waypointAddressLine0);
        waypointAddressLine1 = view.findViewById(R.id.waypointAddressLine1);
        waypointAddressLine2 = view.findViewById(R.id.waypointAddressLine2);
        waypointDistance = view.findViewById(R.id.waypointDistance);

        callNextWaypointLocationButton = view.findViewById(R.id.callNextWaypointLocationButton);
        callNextWaypointToMapsButton = view.findViewById(R.id.callNextWaypointToMapsButton);
        if (hideButtons) {
            callNextWaypointLocationButton.setVisibility(View.GONE);
            callNextWaypointToMapsButton.setVisibility(View.GONE);
        }
    }

    public void setAsCurrent() {
        Log.d(TAG, "Will set waypoint holder as current");
        // set background color
        if (waypoint.isCreated()) {
            waypointInfoLayout.setBackgroundColor(activity.getResources().getColor(R.color.bootstrapWarning));
            waypointInfoLayout.getBackground().setAlpha(127);
        }
    }

    public void setWaypoint(Waypoint waypoint, Activity activity, int position) {

        this.waypoint = waypoint;
        Location location = waypoint.getLocation();

        // set address
        String label;
        if (location.getType().equals(Location.TYPE_HOSPITAL)) {
            label = activity.getString(R.string.Hospital);
        } else if (location.getType().equals(Location.TYPE_INCIDENT)) {
            label = activity.getString(R.string.Incident);
        } else if (location.getType().equals(Location.TYPE_BASE)) {
            label = activity.getString(R.string.Base);
        } else if (location.getType().equals(Location.TYPE_WAYPOINT)) {
            label = activity.getString(R.string.Waypoint);
        } else { // if (location.getType().equals(Location.TYPE_OTHER)) {
            label = activity.getString(R.string.Location);
        }
        waypointAddressLine0.setText(activity.getString(R.string.WaypointNumber, position + 1));
        waypointAddressLine1.setText(label);
        waypointAddressLine2.setText(location.toAddress());

        // set background color
        if (waypoint.isVisited()) {
            waypointInfoLayout.setBackgroundColor(activity.getResources().getColor(R.color.bootstrapPrimary));
            waypointInfoLayout.getBackground().setAlpha(127);
        } else if (waypoint.isSkipped()) {
            waypointInfoLayout.setBackgroundColor(activity.getResources().getColor(R.color.bootstrapSecondary));
            waypointInfoLayout.getBackground().setAlpha(127);
        } else if (waypoint.isVisiting()) {
            waypointInfoLayout.setBackgroundColor(activity.getResources().getColor(R.color.bootstrapPrimary));
            waypointInfoLayout.getBackground().setAlpha(127);
        }


        // Calculate distance to patient
        float distance = waypoint.calculateDistance(AmbulanceForegroundService.getLastLocation());
        String distanceText = activity.getString(R.string.noDistanceInformationAvailable);
        if (distance > 0) {
            distanceText = formatDistance(distance, AmbulanceForegroundService.getAppData().getSettings().getUnits());
        }
        waypointDistance.setText(distanceText);

        if (!hideButtons) {

            // Get navigation app preference
            SharedPreferences sharedPreferences = PreferenceManager.getDefaultSharedPreferences(activity);
            String navigationAppKey = sharedPreferences.getString(activity.getString(R.string.navigationAppPreferenceKey),
                    activity.getResources().getString(R.string.navigationAppPreferenceDefault));

            // set maps buttons
            String navigationAppLabel;
            final Intent mapIntent;
            if (navigationAppKey.equals("waze")) {

                // setup label
                navigationAppLabel = activity.getString(R.string.Waze);

                // set up waze location
                GPSLocation gpsLocation = location.getLocation();
                String url = String.format(Locale.ENGLISH,
                        "https://waze.com/ul?ll=%f,%f&navigate=yes", gpsLocation.getLatitude(), gpsLocation.getLongitude());
                mapIntent = new Intent(Intent.ACTION_VIEW, Uri.parse(url));

            } else { // if (navigationApp.equals("google_maps")) {

                // setup label
                navigationAppLabel = activity.getString(R.string.GoogleMaps);

                String query = null;
                try {
                    query = URLEncoder.encode(location.toAddress(), "utf-8");
                } catch (java.io.UnsupportedEncodingException e) {
                    Log.d(TAG, "Could not parse location into url for map intent");
                }

                if (query != null) {
                    Uri gmmIntentUri = Uri.parse("google.navigation:q=" + query);
                    mapIntent = new Intent(Intent.ACTION_VIEW, gmmIntentUri);
                    mapIntent.setPackage("com.google.android.apps.maps");
                } else {
                    mapIntent = null;
                }

            }

            if (mapIntent != null) {

                callNextWaypointToMapsButton.setOnClickListener(v -> {

                    //checks if google maps or any other map app is installed
                    if (mapIntent.resolveActivity(activity.getPackageManager()) != null) {

                        // Alert before opening in google maps
                        new AlertDialog.Builder(activity)
                                .setTitle(activity.getString(R.string.directions))
                                .setMessage(activity.getString(R.string.wouldYouLikeToNavigate, navigationAppLabel))
                                .setPositiveButton(android.R.string.ok,
                                        (dialog, which) -> activity.startActivity(mapIntent))
                                .setNegativeButton(android.R.string.cancel,
                                        (dialog, which) -> { /* do nothing */ })
                                .create()
                                .show();

                    } else {

                        // Alert that it could not open google maps
                        new org.emstrack.ambulance.dialogs.AlertDialog(activity,
                                activity.getString(R.string.directions))
                                .alert(activity.getString(R.string.couldNotOpenGoogleMaps));

                    }

                });
            }

            // set location button
            callNextWaypointLocationButton.setOnClickListener(v -> {
                Bundle bundle = new Bundle();
                GPSLocation gpsLocation = location.getLocation();
                LatLng latLng = new LatLng(gpsLocation.getLatitude(), gpsLocation.getLongitude());
                bundle.putParcelable("latLng", latLng);
                ((MainActivity) activity).navigate(R.id.mapFragment, bundle);
            });

        }
    }

}