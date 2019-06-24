package org.emstrack.models;

import com.google.gson.annotations.Expose;
import com.google.gson.annotations.SerializedName;

/**
 * A class representing a priority code.
 */
public class PriorityCode {

    private int id;
    private int prefix;
    private String priority;
    private String suffix;
    private String label;


    /**
     *
     * @param id
     * @param prefix
     * @param priority
     * @param suffix
     * @param label
     */
    public PriorityCode(int id, int prefix, String priority, String suffix, String label) {
        this.id = id;
        this.prefix = prefix;
        this.priority = priority;
        this.suffix = suffix;
        this.label = label;
    }

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public int getPrefix() {
        return prefix;
    }

    public void setPrefix(int prefix) {
        this.prefix = prefix;
    }

    public String getPriority() {
        return priority;
    }

    public void setPriority(String priority) {
        this.priority = priority;
    }

    public String getSuffix() {
        return suffix;
    }

    public void setSuffix(String suffix) {
        this.suffix = suffix;
    }

    public String getLabel() {
        return label;
    }

    public void setLabel(String label) {
        this.label = label;
    }

}