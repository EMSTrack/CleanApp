package com.project.cruzroja.hospital.models;

/**
 * Created by Fabian Choi on 5/4/2017.
 * Represents an Equipment from the database
 */
public class Equipment {
    private String name;
    private int quantity;
    private boolean toggleable;

    /**
     * Getter for the name of the equipment
     * @return the name of the equipment
     */
    public String getName() { return name; }

    /**
     * Setter for the name of the equipment
     * @param name the name of the equipment
     */
    public void setName(String name) { this.name = name; }

    /**
     * Getter for the quantity of the equipment
     * @return the quantity available of the equipment
     */
    public int getQuantity() { return quantity; }

    /**
     * Setter for the quantity of the equipment
     * @param quantity the quantity of the equipment
     */
    public void setQuantity(int quantity) { this.quantity = quantity; }

    public boolean isToggleable() {
        return toggleable;
    }

    public void setToggleable(boolean toggleable) {
        this.toggleable = toggleable;
    }
}
