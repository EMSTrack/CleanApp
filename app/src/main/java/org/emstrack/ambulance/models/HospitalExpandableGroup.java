package org.emstrack.ambulance.models;

import com.thoughtbot.expandablerecyclerview.models.ExpandableGroup;

import org.emstrack.models.Hospital;
import org.emstrack.models.HospitalEquipment;

import java.util.List;

/**
 * Created by mauri on 3/11/2018.
 */

public class HospitalExpandableGroup extends ExpandableGroup<HospitalEquipment> {

    private Hospital hospital;

    public HospitalExpandableGroup(String title, List<HospitalEquipment> items, Hospital hospital) {
        super(title, items);
        this.hospital = hospital;
    }

    public Hospital getHospital() {
        return hospital;
    }
}