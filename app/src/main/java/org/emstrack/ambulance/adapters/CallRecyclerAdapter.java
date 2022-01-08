package org.emstrack.ambulance.adapters;

import android.app.Activity;
import android.util.Pair;
import android.util.SparseArray;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import org.emstrack.ambulance.R;
import org.emstrack.ambulance.views.CallViewHolder;
import org.emstrack.models.Ambulance;
import org.emstrack.models.AmbulanceCall;
import org.emstrack.models.Call;
import org.emstrack.models.CallStack;

import java.util.Collection;
import java.util.List;
import java.util.Map;

/**
 * Connects Equipment data to the RecyclerView (called from EquipmentFragment)
 * @author Mauricio de Oliveira
 * @since 7/07/2020
 */

public class CallRecyclerAdapter extends RecyclerView.Adapter<CallViewHolder> {

    private static final String TAG = CallRecyclerAdapter.class.getSimpleName();
    private final Activity activity;
    private final List<Pair<Call, AmbulanceCall>> calls;

    public CallRecyclerAdapter(Activity activity, List<Pair<Call, AmbulanceCall>> calls) {
        this.activity = activity;
        this.calls = calls;
    }

    @NonNull
    @Override
    public CallViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        final View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.call_item, parent, false);
        return new CallViewHolder(activity, view);
    }

    @Override
    public void onBindViewHolder(@NonNull CallViewHolder holder, int position) {

        Pair<Call, AmbulanceCall> item = calls.get(position);
        holder.setCall(item.first, item.second, activity);

    }

    @Override
    public int getItemCount() {
        return calls.size();
    }

}

