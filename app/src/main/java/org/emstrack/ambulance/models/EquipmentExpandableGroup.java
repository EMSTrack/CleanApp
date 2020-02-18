package org.emstrack.ambulance.models;

import com.thoughtbot.expandablerecyclerview.models.ExpandableGroup;

import org.emstrack.models.Hospital;
import org.emstrack.models.EquipmentItem;

import java.util.List;

/**
 * Created by James on 2/17/2020.
 */

public class EquipmentExpandableGroup extends ExpandableGroup<EquipmentItem> {

    public EquipmentExpandableGroup(List<EquipmentItem> items) {
        super(items);
    }
}