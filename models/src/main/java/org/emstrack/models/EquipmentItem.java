package org.emstrack.models;

import android.os.Parcel;
import android.os.Parcelable;
import android.util.Log;

import java.util.Date;

/**
 * A class representing an equipment item.
 * @author Fabian Choi
 * @since 5/4/2017
 */
public class EquipmentItem implements Parcelable {

    private static final String TAG = EquipmentItem.class.getSimpleName();

    private int equipmentHolderId;
    private int equipmentId;
    private String equipmentName;
    private Character equipmentType;
    private String value;
    private String comment;
    private int updatedBy;
    private Date updatedOn;

    public EquipmentItem(int equipmentHolderId,
                         int equipmentId, String equipmentName,
                         Character equipmentType,
                         String value, String comment,
                         int updatedBy, Date updatedOn) {
        this.equipmentHolderId = equipmentHolderId;
        // this.hospitalName = hospitalName;
        this.equipmentId = equipmentId;
        this.equipmentName = equipmentName;
        this.equipmentType = equipmentType;
        this.value = value;
        this.comment = comment;
        this.updatedBy = updatedBy;
        this.updatedOn = updatedOn;
    }

    private EquipmentItem(Parcel in) {
        this.equipmentHolderId = in.readInt();
        this.equipmentName = in.readString();
        this.value = in.readString();
    }

    public int getEquipmentHolderId() {
        return equipmentHolderId;
    }

    public void setEquipmentHolderId(int equipmentHolderId) {
        this.equipmentHolderId = equipmentHolderId;
    }

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

    public Character getEquipmentType() {
        return equipmentType;
    }

    public void setEquipmentType(Character equipmentType) {
        this.equipmentType = equipmentType;
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

    public boolean isInteger() { return this.equipmentType == 'I'; }

    public boolean isString() { return this.equipmentType == 'S'; }

    public boolean isBoolean() { return this.equipmentType == 'B'; }

    public boolean valueToBoolean() {
        return Boolean.parseBoolean(this.value);
    }

    public int valueToInteger() {
        return Integer.parseInt(this.value);
    }

    public String valueToString() {

        switch (this.equipmentType) {

            case 'I':

                int intVal = 0;
                try {
                    intVal = Integer.parseInt(this.value);
                } catch (NumberFormatException e) {
                    Log.i(TAG, "Equipment Value is not number but is: " + this.value);
                    intVal = -1;
                }
                return Integer.toString(intVal);

            case 'B':

                return Boolean.parseBoolean(this.value) ? "true" : "false";

            default:
                return this.value;

        }

    }

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel parcel, int i) {
        parcel.writeInt(equipmentId);
        parcel.writeString(equipmentName);
        parcel.writeString(value);

    }

    public static final Parcelable.Creator<EquipmentItem> CREATOR = new
            Parcelable.Creator<EquipmentItem>() {
                public EquipmentItem createFromParcel(Parcel in) {
                    return new EquipmentItem(in);
                }

                public EquipmentItem[] newArray(int size) {
                    return new EquipmentItem[size];
                }
    };
}
