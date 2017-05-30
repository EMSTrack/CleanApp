package com.project.cruzroja.hospital.models;

import java.util.ArrayList;

/**
 * Created by Fabian Choi on 5/4/2017.
 * Represents a Hospital from the database
 */

public class Hospital {
    private Integer id;
    private String name;
    private ArrayList<Equipment> equipment;

    public Hospital() {
        equipment = new ArrayList<>();
    }

    /**
     * Getter for the name of the hospital
     * @return the name of the hospital
     */
    public String getName() { return name; }

    /**
     * Setter for the name of the hospital
     * @param name the name of the hospital
     */
    public void setName(String name) { this.name = name; }

    /**
     * Getter for the equipments of the hospital
     * @return the equipments of the hospital
     */
    public ArrayList<Equipment> getEquipments() { return equipment; }

    public int getId() { return id; }
    public void setId(int id) { this.id = id; }
}
