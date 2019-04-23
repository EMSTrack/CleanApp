package org.emstrack.ambulance.adapters;
import android.content.Context;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;  //not sure if we will use images
import android.widget.TextView;
import java.util.ArrayList;

import org.emstrack.models.Waypoint;

public class WaypointInfo extends RecyclerView.Adapter<WaypointInfo.MyViewHolder>{

    private LayoutInflater inflater;
    private ArrayList<Waypoint> waypointArrayList;

    public WaypointInfo(Context ctx, ArrayList<Waypoint> waypointArrayList) {
        inflater = LayoutInflater.from(ctx);
        this.waypointArrayList = waypointArrayList;
    }

    @Override
    public WaypointInfo.MyViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        View view = inflater.inflate(R.layout.recycler_item, parent, false);
        MyViewHolder holder = new MyViewHolder(view);

        return holder;
    }

    @Override
    public void onBindViewHolder(WaypointInfo.MyViewHolder holder, int position) {
        holder.time.setText(waypointArrayList.get(position).getLocation().getName());
        //TODO set relevant waypoint information here
    }

    @Override
    public int getItemCount() {
        return waypointArrayList.size();
    }

    class MyViewHolder extends RecyclerView.ViewHolder {
        TextView time;
        //TODO replace variables with waypoint information

        public MyViewHolder(View itemView) {
            super(itemView);
            time = (TextView) itemView.findViewById(R.id.tv);
        }
    }
}
