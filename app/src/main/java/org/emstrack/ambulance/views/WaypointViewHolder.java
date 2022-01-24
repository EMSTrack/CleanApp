package org.emstrack.ambulance.views;

import static org.emstrack.ambulance.util.FormatUtils.formatDistance;
import static org.emstrack.ambulance.util.FormatUtils.formatTime;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.app.AlertDialog;
import android.content.Intent;
import android.content.SharedPreferences;
import android.net.Uri;
import android.os.Bundle;
import android.util.Log;
import android.view.MotionEvent;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.cardview.widget.CardView;
import androidx.preference.PreferenceManager;
import androidx.recyclerview.widget.ItemTouchHelper;

import com.google.android.gms.maps.model.LatLng;

import org.emstrack.ambulance.MainActivity;
import org.emstrack.ambulance.R;
import org.emstrack.ambulance.dialogs.SimpleAlertDialog;
import org.emstrack.ambulance.models.AmbulanceAppData;
import org.emstrack.ambulance.services.AmbulanceForegroundService;
import org.emstrack.ambulance.util.EnabledImageView;
import org.emstrack.ambulance.util.ViewHolderWithSelectedPosition;
import org.emstrack.models.Ambulance;
import org.emstrack.models.Call;
import org.emstrack.models.GPSLocation;
import org.emstrack.models.Location;
import org.emstrack.models.Waypoint;

import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.text.DateFormat;
import java.util.Locale;

/**
 * Holds the waypoint data
 * @author Mauricio de Oliveira
 * @since 7/07/2020
 */

public class WaypointViewHolder extends ViewHolderWithSelectedPosition<Waypoint> implements View.OnClickListener {

    private static final String TAG = WaypointViewHolder.class.getSimpleName();

    private final TextView waypointAddressLine1;
    private final TextView waypointAddressLine2;
    private final TextView waypointAddressLine3;
    private final TextView waypointDistance;

    private final CardView waypointBrowserCardView;

    private final ImageView waypointLocationButton;
    private final ImageView waypointToMapsButton;
    private final ImageView waypointBarsButton;
    private final ImageView waypointDoneButton;

    private final EnabledImageView waypointSkipButton;
    private final View waypointLeftPanel;
    private final View waypointRightPanel;

    private final Activity activity;
    private Waypoint waypoint;

    private final boolean hideButtons;
    private final boolean hideMessage;
    private boolean hideLeftPanel;
    private boolean hideRightPanel;

    private boolean selected;


    @SuppressLint("ClickableViewAccessibility")
    public WaypointViewHolder(Activity activity, View view, ItemTouchHelper itemTouchHelper,
                              boolean hideButtons, boolean hideLeftPanel, boolean hideRightPanel,
                              boolean hideMessage) {
        super(view);

        this.activity = activity;
        this.hideButtons = hideButtons;
        this.hideLeftPanel = hideLeftPanel;
        this.hideRightPanel = hideRightPanel;
        this.hideMessage = hideMessage;
        selected = false;

        waypointBrowserCardView = view.findViewById(R.id.waypointBrowserCardView);

        waypointAddressLine1 = view.findViewById(R.id.waypointAddressLine1);
        waypointAddressLine2 = view.findViewById(R.id.waypointAddressLine2);
        waypointAddressLine3 = view.findViewById(R.id.waypointAddressLine3);
        waypointDistance = view.findViewById(R.id.waypointDistance);

        waypointLeftPanel = view.findViewById(R.id.waypointLeftPanel);
        waypointRightPanel = view.findViewById(R.id.waypointRightPanel);

        waypointSkipButton = new EnabledImageView(view.findViewById(R.id.waypointSkipButton),
                activity.getResources().getColor(R.color.bootstrapDark),
                activity.getResources().getColor(R.color.bootstrapSecondary));
        waypointLocationButton = view.findViewById(R.id.waypointLocationButton);
        waypointToMapsButton = view.findViewById(R.id.waypointToMapsButton);

        waypointDoneButton = view.findViewById(R.id.waypointDoneButton);

        waypointBarsButton = view.findViewById(R.id.waypointBarsButton);
        if (!hideRightPanel) {
            waypointBarsButton.setOnTouchListener((v, event) -> {
                if (event.getActionMasked() == MotionEvent.ACTION_DOWN) {
                    itemTouchHelper.startDrag(WaypointViewHolder.this);
                    return true;
                }
                return false;
            });
        }

    }

    public void setHideLeftPanel(boolean hideLeftPanel) {
        this.hideLeftPanel = hideLeftPanel;
    }

    public void setHideRightPanel(boolean hideRightPanel) {
        this.hideRightPanel = hideRightPanel;
    }

    @Override
    public void setSelected(boolean selected) {
        super.setSelected(selected);
        this.selected = selected;
        if (!selected) {
            if (waypoint!= null && waypoint.isCreated()) {
                waypointBarsButton.setVisibility(View.VISIBLE);
            }
            waypointDoneButton.setVisibility(View.GONE);
            itemView.setOnClickListener(null);
        } else {
            waypointBarsButton.setVisibility(View.GONE);
            waypointDoneButton.setVisibility(View.VISIBLE);
            if (waypoint!= null && !hideMessage) {
                if (waypoint.isCreated()) {
                    waypointAddressLine3.setText(R.string.waypointNext);
                } else if (waypoint.isVisiting()) {
                    waypointAddressLine3.setText(activity.getString(R.string.waypointVisiting,
                            formatTime(waypoint.getUpdatedOn(), DateFormat.SHORT)));
                }
            }
            itemView.setOnClickListener(this);
        }
    }

    public static void promptSkipVisitingOrVisited(Activity activity,
                                                   final String status,
                                                   final int waypointId, final int callId, final int ambulanceId,
                                                   final String title, final String message, final String doneMessage) {

        Log.d(TAG, "Creating promptSkipVisitingOrVisited dialog");

        // Use the Builder class for convenient dialog construction
        AlertDialog.Builder builder = new AlertDialog.Builder(activity);
        builder.setTitle(title)
                .setMessage(message)
                .setNegativeButton(android.R.string.no,
                        (dialog, id) -> Log.i(TAG, "Continuing..."))
                .setPositiveButton(android.R.string.yes,
                        (dialog, id) -> {

                            Log.i(TAG, String.format("Will mark as '%1$s'", status));

                            Toast.makeText(activity, doneMessage, Toast.LENGTH_SHORT).show();

                            String action;
                            if (status.equals(Waypoint.STATUS_SKIPPED))
                                action = AmbulanceForegroundService.Actions.WAYPOINT_SKIP;
                            else if (status.equals(Waypoint.STATUS_VISITING))
                                action = AmbulanceForegroundService.Actions.WAYPOINT_ENTER;
                            else // if (status == Waypoint.STATUS_VISITED)
                                action = AmbulanceForegroundService.Actions.WAYPOINT_EXIT;

                            // update waypoint status on server
                            Intent intent = new Intent(activity,
                                    AmbulanceForegroundService.class);
                            intent.setAction(action);
                            Bundle bundle = new Bundle();
                            bundle.putInt(AmbulanceForegroundService.BroadcastExtras.WAYPOINT_ID, waypointId);
                            bundle.putInt(AmbulanceForegroundService.BroadcastExtras.AMBULANCE_ID, ambulanceId);
                            bundle.putInt(AmbulanceForegroundService.BroadcastExtras.CALL_ID, callId);
                            intent.putExtras(bundle);
                            activity.startService(intent);

                        });

        // Create the AlertDialog object and return it
        builder.create().show();
    }


    @Override
    public void set(Waypoint waypoint, OnClick<Waypoint> onClick) {
        super.set(waypoint, onClick);

        // save waypoint
        this.waypoint = waypoint;

        Location location = waypoint.getLocation();

        // buttons
        if (hideButtons) {
            waypointSkipButton.getImageView().setVisibility(View.GONE);
            waypointLocationButton.setVisibility(View.GONE);
            waypointToMapsButton.setVisibility(View.GONE);
        }
        if (hideLeftPanel) {
            waypointLeftPanel.setVisibility(View.GONE);
        }
        if (hideRightPanel) {
            waypointRightPanel.setVisibility(View.GONE);
        }

        // set address
        String label;
        switch (location.getType()) {
            case Location.TYPE_HOSPITAL:
                label = activity.getString(R.string.Hospital);
                break;
            case Location.TYPE_INCIDENT:
                label = activity.getString(R.string.Incident);
                break;
            case Location.TYPE_BASE:
                label = activity.getString(R.string.Base);
                break;
            case Location.TYPE_WAYPOINT:
                label = activity.getString(R.string.Waypoint);
                break;
            default:
            case Location.TYPE_OTHER:
                label = activity.getString(R.string.Location);
                break;
        }
        waypointAddressLine1.setText(label);
        waypointAddressLine2.setText(location.toAddress());

        // set waypoint
        String message;
        switch (waypoint.getStatus()) {
            case Waypoint.STATUS_VISITING:
                waypointBarsButton.setVisibility(View.GONE);
                waypointSkipButton.setEnabled(true);
                waypointBrowserCardView.setCardBackgroundColor(activity.getResources().getColor(R.color.bootstrapWarning));
                waypointBrowserCardView.getBackground().setAlpha(63);
                waypointDoneButton.setColorFilter(activity.getResources().getColor(R.color.bootstrapSuccess));
                message = activity.getString(R.string.waypointVisiting, formatTime(waypoint.getUpdatedOn(), DateFormat.SHORT));
                break;
            case Waypoint.STATUS_VISITED:
                waypointBarsButton.setVisibility(View.GONE);
                waypointSkipButton.setEnabled(false);
                waypointBrowserCardView.setCardBackgroundColor(activity.getResources().getColor(R.color.bootstrapSuccess));
                waypointBrowserCardView.getBackground().setAlpha(63);
                message = activity.getString(R.string.waypointVisited, formatTime(waypoint.getUpdatedOn(), DateFormat.SHORT));
                break;
            case Waypoint.STATUS_SKIPPED:
                waypointBarsButton.setVisibility(View.GONE);
                waypointSkipButton.setEnabled(false);
                waypointBrowserCardView.setCardBackgroundColor(activity.getResources().getColor(R.color.bootstrapSecondary));
                waypointBrowserCardView.getBackground().setAlpha(63);
                message = activity.getString(R.string.waypointSkipped, formatTime(waypoint.getUpdatedOn(), DateFormat.SHORT));
                break;
            default:
            case Waypoint.STATUS_CREATED:
                waypointSkipButton.setEnabled(true);
                waypointDoneButton.setColorFilter(activity.getResources().getColor(R.color.bootstrapWarning));
                waypointBrowserCardView.setCardBackgroundColor(activity.getResources().getColor(R.color.bootstrapLight));
                message = activity.getString(R.string.waypointNotVisitedYet);
                break;
        }
        if (hideMessage) {
            waypointAddressLine3.setVisibility(View.GONE);
        } else {
            waypointAddressLine3.setText(message);
        }

        // Calculate distance to patient
        float distance = waypoint.calculateDistance(AmbulanceForegroundService.getLastLocation());
        String distanceText = activity.getString(R.string.dash);
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
            if (navigationAppKey != null && navigationAppKey.equals("waze")) {

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
                } catch (UnsupportedEncodingException e) {
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

                waypointToMapsButton.setOnClickListener(v -> {

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
                        new SimpleAlertDialog(activity,
                                activity.getString(R.string.directions))
                                .alert(activity.getString(R.string.couldNotOpenGoogleMaps));

                    }

                });
            }

            // set location button
            waypointLocationButton.setOnClickListener(v -> {
                Bundle bundle = new Bundle();
                GPSLocation gpsLocation = location.getLocation();
                LatLng latLng = new LatLng(gpsLocation.getLatitude(), gpsLocation.getLongitude());
                bundle.putParcelable("latLng", latLng);
                ((MainActivity) activity).navigate(R.id.mapFragment, bundle);
            });

            // set skip button
            waypointSkipButton.setOnClickListener(v -> {
                AmbulanceAppData appData = AmbulanceForegroundService.getAppData();
                Ambulance ambulance = appData.getAmbulance();
                Call call = appData.getCalls().getCurrentCall();
                if (ambulance != null && call != null) {
                    promptSkipVisitingOrVisited(activity,
                            Waypoint.STATUS_SKIPPED,
                            waypoint.getId(), call.getId(), ambulance.getId(),
                            activity.getString(R.string.skipWaypoint),
                            activity.getString(R.string.skipCurrentWaypoint,
                                    waypoint.getLocation().toAddress(activity)),
                            activity.getString(R.string.skippingWaypoint));
                }
            });

            // waypointDoneButton.setOnClickListener(this);
        }
    }


    @Override
    public void onClick(View view) {
        if (!selected) {
            // ignore
            return;
        }
        AmbulanceAppData appData = AmbulanceForegroundService.getAppData();
        Ambulance ambulance = appData.getAmbulance();
        Call call = AmbulanceForegroundService.getAppData().getCalls().getCurrentCall();
        if (ambulance != null && call != null) {
            if (waypoint.isCreated()) {
                promptSkipVisitingOrVisited(activity,
                        Waypoint.STATUS_VISITING,
                        waypoint.getId(), call.getId(), ambulance.getId(),
                        activity.getString(R.string.pleaseConfirm),
                        activity.getString(R.string.visitCurrentWaypoint,
                                waypoint.getLocation().toAddress(activity)),
                        activity.getString(R.string.visitingWaypoint));
            } else {
                promptSkipVisitingOrVisited(activity,
                        Waypoint.STATUS_VISITED,
                        waypoint.getId(), call.getId(), ambulance.getId(),
                        activity.getString(R.string.pleaseConfirm),
                        activity.getString(R.string.visitedCurrentWaypoint,
                                waypoint.getLocation().toAddress(activity)),
                        activity.getString(R.string.visitedWaypoint));
            }
        }
    }

}