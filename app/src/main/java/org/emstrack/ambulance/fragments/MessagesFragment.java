package org.emstrack.ambulance.fragments;

import android.os.Bundle;
import android.util.SparseArray;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.RecyclerView;

import org.emstrack.ambulance.MainActivity;
import org.emstrack.ambulance.R;
import org.emstrack.ambulance.models.AmbulanceAppData;
import org.emstrack.ambulance.services.AmbulanceForegroundService;
import org.emstrack.models.Ambulance;

public class MessagesFragment extends Fragment {

    private static final String TAG = AmbulanceFragment.class.getSimpleName();
    private View rootView;
    private MainActivity activity;
    private RecyclerView recyclerView;

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {

        rootView = inflater.inflate(R.layout.fragment_messages, container, false);
        activity = (MainActivity) requireActivity();

        recyclerView = rootView.findViewById(R.id.messages_recycler_view);

        AmbulanceAppData appData = AmbulanceForegroundService.getAppData();

        return rootView;
    }

    @Override
    public void onResume() {
        super.onResume();

        // setup navigation
        activity.setupNavigationBar(this);

        // Refresh data
        // refreshData();

    }

}
