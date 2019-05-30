package org.emstrack.ambulance.adapters;
import android.content.Context;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import java.util.ArrayList;
import java.util.List;

import org.emstrack.ambulance.R;
import org.emstrack.models.Waypoint;

public class WaypointInfo extends RecyclerView.Adapter<WaypointInfo.MyViewHolder>{

    private LayoutInflater inflater;
    private List<Waypoint> waypointList;

    public WaypointInfo(Context ctx, List<Waypoint> waypointList) {
        inflater = LayoutInflater.from(ctx);
        this.waypointList = waypointList;
    }

    @Override
    public WaypointInfo.MyViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        View view = inflater.inflate(R.layout.waypoint_buttons, parent, false);
        MyViewHolder holder = new MyViewHolder(view);

        return holder;
    }

    @Override
    public void onBindViewHolder(WaypointInfo.MyViewHolder holder, int position) {
        holder.addr.setText(waypointList.get(position).getLocation().toAddress());

    }

    @Override
    public int getItemCount() {
        return waypointList.size();
    }

    class MyViewHolder extends RecyclerView.ViewHolder {
        Button addr;

        public MyViewHolder(View itemView) {
            super(itemView);
            addr = (Button) itemView.findViewById(R.id.address);
        }
    }
}
