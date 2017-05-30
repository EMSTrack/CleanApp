package com.project.cruzroja.hospital.items;

/**
 * Created by devinhickey on 5/1/17.
 * Object containing the information for a single Dashboard entry
 */

public class DashboardItem {
    private String title = "";
    private String type = "";

    // Toggle Only Fields

    // Value Only fields
    private String value = "";

    // TODO either leave as each part or pass in whole server xml string to parse into each part
    public DashboardItem (String title, String type, String value) {
        System.out.println("DashboardObject Constructor called");
        this.title = title;
        this.type = type;
        this.value = value;
    }

    /**
     * Getter function for the string type.
     * @return the type of data this object holds
     */
    public String getType() {
        return type;
    }

    /**
     * Getter function for the string title
     * @return the title of the object
     */
    public String getTitle() {
        return title;
    }

    public String getValue() {
        return value;
    }

    public void setValue(String value) { this.value = value; }
}
