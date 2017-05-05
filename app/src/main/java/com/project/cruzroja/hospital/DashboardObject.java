package com.project.cruzroja.hospital;

/**
 * Created by devinhickey on 5/1/17.
 * Object containing the information for a single Dashboard entry
 */


class DashboardObject {

    private String title = "";
    private String type = "";

    // Toggle Only Fields


    // Value Only fields
    private String currValue = "";

    // TODO either leave as each part or pass in whole server xml string to parse into each part
    DashboardObject (String title, String type, String currValue) {
        System.out.println("DashboardObject Constructor called");
        this.title = title;
        this.type = type;
        this.currValue = currValue;

    }


    /**
     * Getter function for the string type.
     * @return the type of data this object holds
     */
    String getType() {
        return type;
    }

    /**
     * Getter function for the string title
     * @return the title of the object
     */
    String getTitle() {
        return title;
    }


    String getValue() {
        return currValue;
    }

}  // end DashboardObject Class
