package org.emstrack.ambulance.models;

import com.thoughtbot.expandablerecyclerview.models.ExpandableGroup;

import org.emstrack.models.Hospital;
import org.emstrack.models.EquipmentItem;

import java.util.List;

/**
 * Created by James on 2/17/2020. This file is called from EquipmentFragment
 * It contains whatever information you want to store for Equipment items
 * TODO: fix the instance variables and the instantiation, depending on
 *      what information you want to store for each equipment item
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