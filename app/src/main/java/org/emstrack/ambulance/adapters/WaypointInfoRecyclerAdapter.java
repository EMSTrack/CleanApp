package org.emstrack.ambulance.adapters;

/*
  Created By: Mauricio de Oliveira
  Created On: Jan 22, 2022
 */

import android.app.Activity;
import android.content.DialogInterface;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.recyclerview.widget.ItemTouchHelper;

import org.emstrack.ambulance.R;
import org.emstrack.ambulance.dialogs.SimpleAlertDialog;
import org.emstrack.ambulance.util.RecyclerAdapterWithSelectedPosition;
import org.emstrack.ambulance.views.WaypointViewHolder;
import org.emstrack.models.Waypoint;

import java.util.List;

public class WaypointInfoRecyclerAdapter extends RecyclerAdapterWithSelectedPosition<Waypoint, WaypointViewHolder> {

    private static final String TAG = WaypointInfoRecyclerAdapter.class.getSimpleName();
    private final Activity activity;
    private final ItemTouchHelper itemTouchHelper;
    private final boolean hideButtons;
    private final boolean hideMessage;
    private boolean hideLeftPanel;
    private boolean hideRightPanel;

    public WaypointInfoRecyclerAdapter(Activity activity, List<Waypoint> waypointList,
                                       boolean hideButtons, boolean hideLeftPanel, boolean hideRightPanel,
                                       boolean hideMessage) {
        super(waypointList, null);
        setSelectOnClick(false);
        this.activity = activity;
        this.hideButtons = hideButtons;
        this.hideMessage = hideMessage;
        this.hideLeftPanel = hideLeftPanel;
        this.hideRightPanel = hideRightPanel;
        this.itemTouchHelper = new ItemTouchHelper(new ItemTouch());
    }

    public WaypointInfoRecyclerAdapter(Activity activity, List<Waypoint> waypointList) {
        this(activity, waypointList, false, false, false, false);
    }

    public ItemTouchHelper getItemTouchHelper() {
        return itemTouchHelper;
    }

    public void setHideRightPanel(boolean hideRightPanel) {
        this.hideRightPanel = hideRightPanel;
    }

    public void setHideLeftPanel(boolean hideLeftPanel) {
        this.hideLeftPanel = hideLeftPanel;
    }

    @NonNull
    @Override
    public WaypointViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.waypoint_info, parent, false);
        return new WaypointViewHolder(activity, view, itemTouchHelper, hideButtons, hideLeftPanel, hideRightPanel, hideMessage);
    }

    @Override
    public boolean moveItem(int from, int to) {
        int selectedPosition = getSelectedPosition();
        if (from > selectedPosition && to > selectedPosition) {
            // actually move
            return super.moveItem(from, to);
        }
        // otherwise ignore
        return false;
    }

    @Override
    public void onItemMoved(int from, int to) {
        new SimpleAlertDialog(activity, activity.getString(R.string.alert_warning_title))
                .alert(activity.getString(R.string.notImplementedYet),
                (d, i) -> {
                    super.moveItem(to, from);
                });
    }
}