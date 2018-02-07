package org.emstrack.models;

/**
 * Created by Fabian Choi on 5/4/2017.
 * Represents a HospitalEquipmentMetadata from the database
 */

public class HospitalEquipmentMetadata {
    private Integer id;
    private String name;
    private Character etype;
    private boolean toggleable;

    public HospitalEquipmentMetadata(int id, String name,
                                     Character etype, boolean toggleable) {
        this.id = id;
        this.name = name;
        this.etype = etype;
        this.toggleable = toggleable;
    }

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public Character getEtype() {
        return etype;
    }

    public void setEtype(Character etype) {
        this.etype = etype;
    }

    public boolean isToggleable() {
        return toggleable;
    }

    public void setToggleable(boolean toggleable) {
        this.toggleable = toggleable;
    }

}
