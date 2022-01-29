package org.emstrack.ambulance.adapters;

import android.app.Activity;
import android.util.Pair;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import org.emstrack.ambulance.R;
import org.emstrack.ambulance.views.CallViewHolder;
import org.emstrack.models.AmbulanceCall;
import org.emstrack.models.Call;

import java.util.List;

/**
 * Recycler view for calls
 * @author Mauricio de Oliveira
 * @since 7/07/2020
 */

public class CallRecyclerAdapter extends RecyclerView.Adapter<CallViewHolder> {

    private static final String TAG = CallRecyclerAdapter.class.getSimpleName();

    private final Activity activity;
    private final List<Pair<Call, AmbulanceCall>> calls;

    public CallRecyclerAdapter(@NonNull Activity activity, @NonNull List<Pair<Call, AmbulanceCall>> calls) {
        this.activity = activity;
        this.calls = calls;
    }

    @NonNull
    @Override
    public CallViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        final View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.call_item, parent, false);
        return new CallViewHolder(view);
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

