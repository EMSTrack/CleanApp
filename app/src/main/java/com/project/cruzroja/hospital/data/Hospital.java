package com.project.cruzroja.hospital.data;

/**
 * Created by Fabian Choi on 5/4/2017.
 * Represents a Hospital from the database
 */

public class Hospital {
    private String name;

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
}
