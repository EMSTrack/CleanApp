package org.emstrack.models;

/**
 * A class representing a priority classification.
 */
public class PriorityClassification {

    private int id;
    private String label;


    /**
     *
     * @param id
     * @param label
     */
    public PriorityClassification(int id, String label) {
        this.id = id;
        this.label = label;
    }

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public String getLabel() {
        return label;
    }

    public void setLabel(String label) {
        this.label = label;
    }

}