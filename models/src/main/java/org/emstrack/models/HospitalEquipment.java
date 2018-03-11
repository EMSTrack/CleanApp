package org.emstrack.models;

/**
 * Created by Fabian Choi on 5/4/2017.
 * Represents an HospitalEquipment from the database
 */

import java.util.Date;

public class HospitalEquipment {

    private int hospitalId;
    // private String hospitalName;
    private int equipmentId;
    private String equipmentName;
    private Character equipmentEtype;
    private String value;
    private String comment;
    private int updatedBy;
    private Date updatedOn;

    public HospitalEquipment(int hospitalId, // String hospitalName,
                             int equipmentId, String equipmentName,
                             Character equipmentEtype,
                             String value, String comment,
                             int updatedBy, Date updatedOn) {
        this.hospitalId = hospitalId;
        // this.hospitalName = hospitalName;
        this.equipmentId = equipmentId;
        this.equipmentName = equipmentName;
        this.equipmentEtype = equipmentEtype;
        this.value = value;
        this.comment = comment;
        this.updatedBy = updatedBy;
        this.updatedOn = updatedOn;
    }

    public int getHospitalId() {
        return hospitalId;
    }

    public void setHospitalId(int hospitalId) {
        this.hospitalId = hospitalId;
    }

/*
    public String getHospitalName() {
        return hospitalName;
    }

    public void setHospitalName(String hospitalName) {
        this.hospitalName = hospitalName;
    }
*/

    public int getEquipmentId() {
        return equipmentId;
    }

    public void setEquipmentId(int equipmentId) {
        this.equipmentId = equipmentId;
    }

    public String getEquipmentName() {
        return equipmentName;
    }

    public void setEquipmentName(String equipmentName) {
        this.equipmentName = equipmentName;
    }

    public Character getEquipmentEtype() {
        return equipmentEtype;
    }

    public void setEquipmentEtype(Character equipmentEtype) {
        this.equipmentEtype = equipmentEtype;
    }

    public String getValue() {
        return value;
    }

    public void setValue(String value) {
        this.value = value;
    }

    public String getComment() {
        return comment;
    }

    public void setComment(String comment) {
        this.comment = comment;
    }

    public int getUpdatedBy() {
        return updatedBy;
    }

    public void setUpdatedBy(int updatedBy) {
        this.updatedBy = updatedBy;
    }

    public Date getUpdatedOn() {
        return updatedOn;
    }

    public void setUpdatedOn(Date updatedOn) {
        this.updatedOn = updatedOn;
    }

}