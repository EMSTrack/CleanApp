package org.emstrack.ambulance;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseExpandableListAdapter;
import android.widget.TextView;

import org.emstrack.models.Hospital;
import org.emstrack.models.HospitalEquipment;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by rawaa_ali on 6/1/17.
 */

public class HospitalAdapter extends BaseExpandableListAdapter {
    private Context mContext;
    private LayoutInflater mInflater;
    private List<Hospital> hospitals;

    public HospitalAdapter(Context context, List<Hospital> hospitals) {
        mContext = context;
        //Log.d("hospitalitems",items.get(0).getName());
        this.hospitals = hospitals;
        mInflater = (LayoutInflater) mContext.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
    }

    @Override
    public Object getChild(int groupPosition, int childPosition) {
        return hospitals.get(groupPosition).getHospitalEquipment().get(childPosition);
    }

    @Override
    public long getChildId(int groupPosition, int childPosition) {
        return childPosition;
    }


    public View getChildView(int groupPosition, int childPosition, boolean isLastChild,
                             View view, ViewGroup parent) {

        HospitalEquipment equipment = (HospitalEquipment) getChild(groupPosition, childPosition);
        if (view == null) {
//            LayoutInflater infalInflater = (LayoutInflater) mContext.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
            view = mInflater.inflate(R.layout.equipment_list_body, null);
        }
        TextView equipmentItem = (TextView) view.findViewById(R.id.equipment_item);
        equipmentItem.setText(equipment.getEquipmentName().trim() + " " + equipment.getValue());

        return view;
    }

    @Override
    public int getChildrenCount(int groupPosition) {

        List<HospitalEquipment> equipmentList = hospitals.get(groupPosition).getHospitalEquipment();
        return equipmentList.size();

    }

    @Override
    public Object getGroup(int groupPosition) {
        return hospitals.get(groupPosition);
    }

    @Override
    public int getGroupCount() {
        return hospitals.size();
    }

    @Override
    public long getGroupId(int groupPosition) {
        return groupPosition;
    }

    @Override
    public View getGroupView(int groupPosition, boolean isLastChild, View view,
                             ViewGroup parent) {

        Hospital headerInfo = (Hospital) getGroup(groupPosition);
        if (view == null) {
//            LayoutInflater inf = (LayoutInflater) mContext.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
            view = mInflater.inflate(R.layout.equipment_list_header, null);
        }

        TextView heading = (TextView) view.findViewById(R.id.equipment_header);
        heading.setText(headerInfo.getHospitalName().trim());

        return view;
    }

    @Override
    public boolean hasStableIds() {
        return true;
    }

    @Override
    public boolean isChildSelectable(int groupPosition, int childPosition) {
        return true;
    }

}
