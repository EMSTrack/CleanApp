package org.emstrack.ambulance.adapters;

/**
 * Created By: Andrew N. Sanchez
 * Created On: May 30, 2019
 *
 * WaypointInfoAdapter
 */

import android.app.Activity;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import java.util.List;

import org.emstrack.ambulance.R;
import org.emstrack.ambulance.views.WaypointViewHolder;
import org.emstrack.models.Waypoint;

public class WaypointInfoRecyclerAdapter extends RecyclerView.Adapter<WaypointViewHolder>{

    private static final String TAG = WaypointInfoRecyclerAdapter.class.getSimpleName();
    private final Activity activity;
    private final List<Waypoint> waypoints;
    private final boolean hideButtons;

    public WaypointInfoRecyclerAdapter(Activity activity, List<Waypoint> waypointList, boolean hideButtons) {
        this.activity = activity;
        this.waypoints = waypointList;
        this.hideButtons = hideButtons;
    }

    public WaypointInfoRecyclerAdapter(Activity activity, List<Waypoint> waypointList) {
        this(activity, waypointList, false);
    }

    @NonNull
    @Override
    public WaypointViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.waypoint_info, parent, false);
        return new WaypointViewHolder(activity, view, hideButtons);
    }

    @Override
    public void onBindViewHolder(WaypointViewHolder holder, int position) {
        Waypoint waypoint = waypoints.get(position);
        holder.setWaypoint(waypoint, activity, position);
    }

    @Override
    public void onViewAttachedToWindow(@NonNull WaypointViewHolder holder) {
        super.onViewAttachedToWindow(holder);
        Log.d(TAG, "View attached to window!");
    }

    @Override
    public void onViewDetachedFromWindow(@NonNull WaypointViewHolder holder) {
        super.onViewDetachedFromWindow(holder);
        Log.d(TAG, "View detached from window!");
    }

    @Override
    public int getItemCount() {
        return waypoints.size();
    }

}