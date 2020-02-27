package org.emstrack.ambulance.models;

import com.thoughtbot.expandablerecyclerview.models.ExpandableGroup;

import org.emstrack.models.Hospital;
import org.emstrack.models.EquipmentItem;

import java.util.List;

/**
 * Created by James on 2/17/2020.
 */

public class EquipmentExpandableGroup extends ExpandableGroup<EquipmentItem> {

    private Hospital hospital;
    private EquipmentItem equipment;

    public EquipmentExpandableGroup(String title, List<EquipmentItem> items, Hospital hospital) {
        super(title, items);
        this.hospital = hospital;
        //this.equipment = null;
    }

    public EquipmentItem getEquipment() {
        return equipment;
    }
}