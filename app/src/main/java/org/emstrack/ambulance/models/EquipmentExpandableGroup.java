package org.emstrack.ambulance.models;

import com.thoughtbot.expandablerecyclerview.models.ExpandableGroup;

import org.emstrack.models.EquipmentItem;

import java.util.List;

/**
 * Created by James on 2/17/2020. This file is called from EquipmentFragment
 * It contains whatever information you want to store for Equipment items.
 */

public class EquipmentExpandableGroup {

    private EquipmentItem equipment;
    //value can be string, boolean, or int
    private String value;
    private String type;
    private String description;

    //TODO: change this function according to these instance variables
    public EquipmentExpandableGroup(EquipmentItem equipment, String value, String type, String description) {
        this.equipment = equipment;
        this.value = value;
        this.type = type;
        this.description = description;
    }

    public EquipmentItem getEquipment() {
        return equipment;
    }

    public String getValue() {
        return value;
    }

    public String getType() {
        return type;
    }

    public String getDescription() {
        return description;
    }
}