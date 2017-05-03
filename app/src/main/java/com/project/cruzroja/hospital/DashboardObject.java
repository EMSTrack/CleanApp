package com.project.cruzroja.hospital;

/**
 * Created by devinhickey on 5/1/17.
 * Object containing the information for a single Dashboard entry
 */


public class DashboardObject {

    private String title = "";
    private String type = "";

    public DashboardObject () {
        System.out.println("DashboardObject Constructor called");
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

}  // end DashboardObject Class
