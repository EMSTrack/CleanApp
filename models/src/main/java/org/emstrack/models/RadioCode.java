package org.emstrack.models;

import com.google.gson.annotations.Expose;
import com.google.gson.annotations.SerializedName;

/**
 * A class representing a radio code.
 */
public class RadioCode {

    private int id;
    private String code;
    private String label;


    /**
     *
     * @param id
     * @param code
     * @param label
     */
    public RadioCode(int id, String code, String label) {
        this.id = id;
        this.code = code;
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

    public String getCode() {
        return code;
    }

    public void setCode(String code) {
        this.code = code;
    }

}