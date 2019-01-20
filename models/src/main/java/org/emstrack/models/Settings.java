package org.emstrack.models;

/**
 * Created by mauricio on 2/7/18.
 */

import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

/**
 * A class representing global settings.
 */
public class Settings {

    public Map<String, String> ambulanceCapability;

    public Map<String, String> ambulanceStatus;

    public Map<String, String> equipmentType;

    public Map<String, String> locationType;

    private Defaults defaults;

    public Map<String, String> getAmbulanceCapability() {
        return ambulanceCapability;
    }

    public void setAmbulanceCapability(Map<String, String> ambulanceCapability) {
        this.ambulanceCapability = ambulanceCapability;
    }

    public Map<String, String> getAmbulanceStatus() {
        return ambulanceStatus;
    }

    public void setAmbulanceStatus(Map<String, String> ambulanceStatus) {
        this.ambulanceStatus = ambulanceStatus;
    }

    public void setEquipmentType(Map<String, String> equipmentType) {
        this.equipmentType = equipmentType;
    }

    public Map<String, String> getEquipmentType() {
        return equipmentType;
    }

    public void setLocationType(Map<String,String> locationType) {
        this.locationType = locationType;
    }

    public Map<String,String> getLocationType() { return locationType; }

    public Defaults getDefaults() {
        return defaults;
    }

    public void setDefaults(Defaults defaults) {
        this.defaults = defaults;
    }

    public Settings(Map<String,String> ambulanceCapability,
                    Map<String,String> ambulanceStatus,
                    Map<String,String> equipmentType,
                    Map<String,String> locationType,
                    Defaults defaults) {
        this.ambulanceCapability = ambulanceCapability;
        this.ambulanceStatus = ambulanceStatus;
        this.equipmentType = new HashMap<>();
        this.equipmentType = equipmentType;
        this.locationType = new HashMap<>();
        this.locationType = locationType;
        this.defaults = defaults;
    }

    @Override
    public String toString() {
        return "settings:" +
                "\nambulanceCapability = " + Arrays.toString(ambulanceCapability.entrySet().toArray()) +
                "\nambulanceStatus = " + Arrays.toString(ambulanceStatus.entrySet().toArray()) +
                "\nequipmentType = " + Arrays.toString(equipmentType.entrySet().toArray()) +
                "\nlocationType = " + Arrays.toString(locationType.entrySet().toArray()) +
                "\ndefaults = " + defaults;
    }
}
