package org.emstrack.ambulance.models;

import com.thoughtbot.expandablerecyclerview.models.ExpandableGroup;

import org.emstrack.models.Hospital;
import org.emstrack.models.EquipmentItem;

import java.util.List;

/**
 * Created by mauricio on 3/11/2018.
 */

public class HospitalExpandableGroup extends ExpandableGroup<EquipmentItem> {

    private Hospital hospital;

    public HospitalExpandableGroup(String title, List<EquipmentItem> items, Hospital hospital) {
        super(title, items);
        this.hospital = hospital;
    }

    public Hospital getHospital() {
        return hospital;
    }
}