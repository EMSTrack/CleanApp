package org.emstrack.ambulance.views;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.TextView;

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

/**
 * Holds the waypoint data
 * @author Mauricio de Oliveira
 * @since 7/07/2020
 */

public class WaypointInfoRecyclerViewViewHolder extends RecyclerView.ViewHolder {

    private static final String TAG = WaypointInfoRecyclerViewViewHolder.class.getSimpleName();
    private static final DecimalFormat df = new DecimalFormat();

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


    public WaypointInfoRecyclerViewViewHolder(Activity activity, View view, boolean hideButtons) {
        super(view);

        this.activity = activity;
        this.hideButtons = hideButtons;

        // set formatter
        df.setMaximumFractionDigits(3);

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
        float distance = -1;
        android.location.Location lastLocation = AmbulanceForegroundService.getLastLocation();
        if (lastLocation != null) {
            distance = lastLocation.distanceTo(location.getLocation().toLocation()) / 1000;
        }
        String distanceText = activity.getString(R.string.noDistanceAvailable);
        if (distance > 0) {
            distanceText = df.format(distance) + " km";
        }
        waypointDistance.setText(distanceText);

        if (!hideButtons) {

            // set maps buttons
            try {

                String query = URLEncoder.encode(location.toAddress(), "utf-8");

                Uri gmmIntentUri = Uri.parse("google.navigation:q=" + query);
                Intent mapIntent = new Intent(Intent.ACTION_VIEW, gmmIntentUri);
                mapIntent.setPackage("com.google.android.apps.maps");

                callNextWaypointToMapsButton.setOnClickListener(v -> {

                    //checks if google maps or any other map app is installed
                    if (mapIntent.resolveActivity(activity.getPackageManager()) != null) {

                        // Alert before opening in google maps
                        new AlertDialog.Builder(activity)
                                .setTitle(activity.getString(R.string.directions))
                                .setMessage(R.string.wouldYouLikeToGoogleMaps)
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

            } catch (java.io.UnsupportedEncodingException e) {
                Log.d(TAG, "Could not parse location into url for map intent");
            }

            // set location button
            callNextWaypointLocationButton.setOnClickListener(v -> {
                Bundle bundle = new Bundle();
                GPSLocation gpsLocation = location.getLocation();
                LatLng latLng = new LatLng(gpsLocation.getLatitude(), gpsLocation.getLongitude());
                bundle.putParcelable("latLng", latLng);
                ((MainActivity) activity).navigate(R.id.action_ambulance_to_map, bundle);
            });

        }
    }

}