package org.emstrack.ambulance.views;

import android.content.Context;
import android.view.View;
import android.widget.TextView;

import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import org.emstrack.ambulance.R;
import org.emstrack.ambulance.adapters.EquipmentRecyclerAdapter;
import org.emstrack.ambulance.dialogs.AlertSnackbar;
import org.emstrack.models.EquipmentItem;
import org.emstrack.models.Hospital;
import org.emstrack.models.TokenLogin;
import org.emstrack.models.api.APIService;
import org.emstrack.models.api.APIServiceGenerator;
import org.emstrack.models.api.OnAPICallComplete;

import java.util.List;

/**
 * Holds the hospital data
 * @author Mauricio de Oliveira
 * @since 7/07/2020
 */

public class HospitalRecyclerViewViewHolder extends RecyclerView.ViewHolder {

    private static final String TAG = HospitalRecyclerViewViewHolder.class.getSimpleName();
    private final View hospitalEquipment;
    private final TextView hospitalName;
    private final TextView hospitalEquipmentRefreshingData;
    private final RecyclerView hospitalEquipmentRecyclerView;
    private Hospital hospital;


    public HospitalRecyclerViewViewHolder(Context context, View view) {
        super(view);

        hospitalName = view.findViewById(R.id.hospital_name);
        hospitalEquipment = view.findViewById(R.id.hospital_equipment);

        hospitalEquipmentRefreshingData = hospitalEquipment.findViewById(R.id.equipment_refreshing_data);
        hospitalEquipmentRecyclerView = hospitalEquipment.findViewById(R.id.equipment_recycler_view);

        view.setOnClickListener(v -> {

            // toggle visibility of the detail view
            if (hospitalEquipment.getVisibility() == View.VISIBLE)
                hospitalEquipment.setVisibility(View.GONE);
            else {

                // retrieve hospital equipment
                APIService service = APIServiceGenerator.createService(APIService.class);
                retrofit2.Call<List<EquipmentItem>> callHospitalEquipment = service.getHospitalEquipment(hospital.getId());

                hospitalEquipmentRefreshingData.setText(R.string.refreshingData);
                hospitalEquipmentRefreshingData.setVisibility(View.VISIBLE);
                hospitalEquipmentRecyclerView.setVisibility(View.GONE);
                hospitalEquipment.setVisibility(View.VISIBLE);

                new OnAPICallComplete<List<EquipmentItem>>(callHospitalEquipment) {

                    @Override
                    public void onSuccess(List<EquipmentItem> equipments) {

                        // hide refresh label
                        hospitalEquipmentRefreshingData.setVisibility(View.GONE);

                        // Install adapter
                        LinearLayoutManager linearLayoutManager = new LinearLayoutManager(context);
                        EquipmentRecyclerAdapter adapter =
                                new EquipmentRecyclerAdapter(context, equipments);
                        hospitalEquipmentRecyclerView.setLayoutManager(linearLayoutManager);
                        hospitalEquipmentRecyclerView.setAdapter(adapter);

                        hospitalEquipmentRecyclerView.setVisibility(View.VISIBLE);
                    }

                    @Override
                    public void onFailure(Throwable t) {
                        super.onFailure(t);

                        hospitalEquipmentRefreshingData.setText(R.string.couldNotRetrieveEquipments);

                    }
                }
                        .start();

            }

        });

    }

    public void setHospital(Hospital item, Context context) {

        hospital = item;
        hospitalName.setText(item.getName());

    }

}