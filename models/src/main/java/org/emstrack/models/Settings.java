package org.emstrack.models;

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * A class representing global settings.
 * @author mauricio
 * @since 2/7/18
 */
public class Settings {

    public Map<String, String> ambulanceStatus;
    public List<String> ambulanceStatusOrder;

    public Map<String, String> ambulanceCapability;
    public List<String> ambulanceCapabilityOrder;

    public Map<String, String> callPriority;
    public List<String> callPriorityOrder;

    public Map<String, String> callStatus;
    public List<String> callStatusOrder;

    public Map<String, String> ambulancecallStatus;

    public Map<String, String> locationType;
    public List<String> locationTypeOrder;

    public Map<String, String> waypointStatus;

    public Map<String, String> equipmentType;
    public Map<String, String> equipmentTypeDefaults;

    private Defaults defaults;

    /**
     *
     * @return the ambulance status map
     */
    public Map<String, String> getAmbulanceStatus() {
        return ambulanceStatus;
    }

    /**
     *
     * @param ambulanceStatus the ambulance status map
     */
    public void setAmbulanceStatus(Map<String, String> ambulanceStatus) {
        this.ambulanceStatus = ambulanceStatus;
    }

    /**
     *
     * @return the ambulance status order list
     */
    public List<String> getAmbulanceStatusOrder() {
        return ambulanceStatusOrder;
    }

    /**
     *
     * @param ambulanceStatusOrder the ambulance status order list
     */
    public void setAmbulanceStatusOrder(List<String> ambulanceStatusOrder) {
        this.ambulanceStatusOrder = ambulanceStatusOrder;
    }

    /**
     *
     * @return the ambulance capability map
     */
    public Map<String, String> getAmbulanceCapability() {
        return ambulanceCapability;
    }

    /**
     *
     * @param ambulanceCapability the ambulance capability map
     */
    public void setAmbulanceCapability(Map<String, String> ambulanceCapability) {
        this.ambulanceCapability = ambulanceCapability;
    }

    /**
     *
     * @return the ambulance capability order list
     */
    public List<String> getAmbulanceCapabilityOrder() {
        return ambulanceCapabilityOrder;
    }

    /**
     *
     * @param ambulanceCapabilityOrder the ambulance capability order list
     */
    public void setAmbulanceCapabilityOrder(List<String> ambulanceCapabilityOrder) {
        this.ambulanceCapabilityOrder = ambulanceCapabilityOrder;
    }

    /**
     *
     * @return the call priority map
     */
    public Map<String, String> getCallPriority() {
        return callPriority;
    }

    /**
     *
     * @param callPriority the call priority map
     */
    public void setCallPriority(Map<String, String> callPriority) {
        this.callPriority = callPriority;
    }

    /**
     *
     * @return the call priority order list
     */
    public List<String> getCallPriorityOrder() {
        return callPriorityOrder;
    }

    /**
     *
     * @param callPriorityOrder the call priority order list
     */
    public void setCallPriorityOrder(List<String> callPriorityOrder) {
        this.callPriorityOrder = callPriorityOrder;
    }

    /**
     *
     * @return the call status map
     */
    public Map<String, String> getCallStatus() {
        return callStatus;
    }

    /**
     *
     * @param callStatus the call status map
     */
    public void setCallStatus(Map<String, String> callStatus) {
        this.callStatus = callStatus;
    }

    /**
     *
     * @return the call status order list
     */
    public List<String> getCallStatusOrder() {
        return callStatusOrder;
    }

    /**
     *
     * @param callStatusOrder the call status order list
     */
    public void setCallStatusOrder(List<String> callStatusOrder) {
        this.callStatusOrder = callStatusOrder;
    }

    /**
     *
     * @return the ambulance call status map
     */
    public Map<String, String> getAmbulancecallStatus() {
        return ambulancecallStatus;
    }

    /**
     *
     * @param ambulancecallStatus the ambulance call status map
     */
    public void setAmbulancecallStatus(Map<String, String> ambulancecallStatus) {
        this.ambulancecallStatus = ambulancecallStatus;
    }

    /**
     *
     * @param locationType the location type map
     */
    public void setLocationType(Map<String,String> locationType) {
        this.locationType = locationType;
    }

    /**
     *
     * @return the location type map
     */
    public Map<String,String> getLocationType() { return locationType; }

    /**
     *
     * @return the location type order list
     */
    public List<String> getLocationTypeOrder() {
        return locationTypeOrder;
    }

    /**
     *
     * @param locationTypeOrder the location type order list
     */
    public void setLocationTypeOrder(List<String> locationTypeOrder) {
        this.locationTypeOrder = locationTypeOrder;
    }

    /**
     *
     * @param waypointStatus the waypoint status map
     */
    public void setWaypointStatus(Map<String, String> waypointStatus) {
        this.waypointStatus = waypointStatus;
    }

    /**
     *
     * @return the waypoint status map
     */
    public Map<String, String> getWaypointStatus() {
        return waypointStatus;
    }

    /**
     *
     * @param equipmentType the equipment type map
     */
    public void setEquipmentType(Map<String, String> equipmentType) {
        this.equipmentType = equipmentType;
    }

    /**
     *
     * @return the equipment type map
     */
    public Map<String, String> getEquipmentType() {
        return equipmentType;
    }

    /**
     *
     * @param equipmentTypeDefaults the equipment type defaults map
     */
    public void setEquipmentTypeDefaults(Map<String, String> equipmentTypeDefaults) {
        this.equipmentTypeDefaults = equipmentTypeDefaults;
    }

    /**
     *
     * @return the equipment type defaults map
     */
    public Map<String, String> getEquipmentTypeDefaults() {
        return equipmentTypeDefaults;
    }

    /**
     *
     * @return the defaults map
     */
    public Defaults getDefaults() {
        return defaults;
    }

    /**
     *
     * @param defaults the defaults map
     */
    public void setDefaults(Defaults defaults) {
        this.defaults = defaults;
    }

    /**
     *
     * @param ambulanceStatus the ambulance status map
     * @param ambulanceStatusOrder the ambulance status order list
     * @param ambulanceCapability the ambulance capability map
     * @param ambulanceCapabilityOrder the ambulance capability order list
     * @param callPriority the call priority map
     * @param callPriorityOrder the call priority order list
     * @param callStatus the call status map
     * @param callStatusOrder the call status order list
     * @param ambulancecallStatus the ambulancecall status map
     * @param locationType the location type map
     * @param locationTypeOrder the location type order list
     * @param equipmentType the equipment type map
     * @param equipmentTypeDefaults the equipment type defaults map
     * @param defaults the defaults map
     */
    public Settings(Map<String,String> ambulanceStatus,
                    List<String> ambulanceStatusOrder,
                    Map<String,String> ambulanceCapability,
                    List<String> ambulanceCapabilityOrder,
                    Map<String,String> callPriority,
                    List<String> callPriorityOrder,
                    Map<String,String> callStatus,
                    List<String> callStatusOrder,
                    Map<String,String> ambulancecallStatus,
                    Map<String,String> locationType,
                    List<String> locationTypeOrder,
                    Map<String,String> equipmentType,
                    Map<String,String> equipmentTypeDefaults,
                    Defaults defaults) {
        this.ambulanceStatus = ambulanceStatus;
        this.ambulanceStatusOrder = ambulanceStatusOrder;
        this.ambulanceCapability = ambulanceCapability;
        this.ambulanceCapabilityOrder = ambulanceCapabilityOrder;
        this.callPriority = callPriority;
        this.callPriorityOrder = callPriorityOrder;
        this.callStatus = callStatus;
        this.callStatusOrder = callStatusOrder;
        this.ambulancecallStatus = ambulancecallStatus;
        this.locationType = locationType;
        this.locationTypeOrder = locationTypeOrder;
        this.equipmentType = equipmentType;
        this.equipmentTypeDefaults = equipmentTypeDefaults;
        this.defaults = defaults;
    }

    /**
     *
     * @return
     */
    @Override
    public String toString() {
        return "settings:" +
                "\nambulanceStatus = " + Arrays.toString(ambulanceStatus.entrySet().toArray()) +
                "\nambulanceCapability = " + Arrays.toString(ambulanceCapability.entrySet().toArray()) +
                "\ncallPriority = " + Arrays.toString(callPriority.entrySet().toArray()) +
                "\ncallStatus = " + Arrays.toString(callStatus.entrySet().toArray()) +
                "\nambulancecallStatus = " + Arrays.toString(ambulancecallStatus.entrySet().toArray()) +
                "\nlocationType = " + Arrays.toString(locationType.entrySet().toArray()) +
                "\nequipmentType = " + Arrays.toString(equipmentType.entrySet().toArray()) +
                "\nequipmentTypeDefaults = " + Arrays.toString(equipmentTypeDefaults.entrySet().toArray()) +
                "\ndefaults = " + defaults;
    }
}
