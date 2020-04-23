package org.emstrack.ambulance.models;

import org.emstrack.models.EquipmentItem;

/**
 * This is NOT being used--EquipmentViewHolder.java stores all the information now
 * Contains the information we want to store for Equipment items
 * @author James Basa
 * @since 2/17/2020
 */
public class EquipmentExpandableGroup {

    private EquipmentItem equipments;
    //value can be string, boolean, or int
    private String value;
    private String type;
    private String description;

    public EquipmentExpandableGroup(EquipmentItem equipments, String value, String type, String description) {
        this.equipments = equipments;
        this.value = value;
        this.type = type;
        this.description = description;
    }

    public EquipmentItem getEquipments() {
        return equipments;
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