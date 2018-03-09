package org.emstrack.ambulance.fragments;

import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v7.widget.GridLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;

import org.emstrack.ambulance.R;
import org.emstrack.ambulance.adapters.HospitalEquipmentRVAdapter;
import org.emstrack.models.HospitalEquipment;

import java.util.ArrayList;

/**
 * Created by tina on 3/6/18.
 */

public class HospitalEquipmentFragment extends Fragment {

    private static String TAG = HospitalEquipmentFragment.class.getSimpleName();

    String hospitalName;
    ArrayList<HospitalEquipment> hospitalEquipment;

    public void onCreate(Bundle savedInstanceState) {

        super.onCreate(savedInstanceState);
        Bundle bundle = this.getArguments();

        if (bundle != null) {
            this.hospitalName = bundle.getString("hospitalName");
            this.hospitalEquipment = bundle.getParcelableArrayList("hospitalEquipment");
        }

    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {

        View rootView = inflater.inflate(R.layout.fragment_hospital_equipment, container, false);
        ImageView mapIV = rootView.findViewById(R.id.map_image);

        RecyclerView recyclerView = rootView.findViewById(R.id.rv);

        GridLayoutManager layoutManager = new GridLayoutManager(getContext(), 3);
        layoutManager.setSpanSizeLookup(new GridLayoutManager.SpanSizeLookup() {
            @Override
            public int getSpanSize(int position) {
                if (position == 0) {
                    return 3;
                }
                return 1;
            }
        });
        recyclerView.setLayoutManager(layoutManager);

        HospitalEquipmentRVAdapter hospitalEquipmentRVAdapter = new HospitalEquipmentRVAdapter(hospitalName, hospitalEquipment);
        recyclerView.setAdapter(hospitalEquipmentRVAdapter);

        return rootView;
    }
}
