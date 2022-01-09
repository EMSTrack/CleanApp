package org.emstrack.models;

import androidx.annotation.NonNull;

import org.emstrack.models.gson.Exclude;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

/**
 * A class representing a location.
 */
public class Location extends Address {

    public static final String TYPE_BASE = "b";
    public static final String TYPE_AED = "a";
    public static final String TYPE_INCIDENT = "i";
    public static final String TYPE_HOSPITAL = "h";
    public static final String TYPE_WAYPOINT = "w";
    public static final String TYPE_OTHER = "o";

    // @Exclude
    private int id;

    private String name;
    private String type;

    public Location(String name, String type, GPSLocation location) {
        super(location);
        this.id = -1;
        this.name = name;
        this.type = type;
    }
    public Location(String name, String type,
                    String number, String street, String unit, String neighborhood, String city,
                    String state, String zipcode, String country,
                    GPSLocation location) {
        super(number, street, unit, neighborhood, city, state, zipcode, country, location);
        this.id = -1;
        this.name = name;
        this.type = type;
    }

    public Location(int id, String name, String type,
                    String number, String street, String unit, String neighborhood, String city,
                    String state, String zipcode, String country,
                    GPSLocation location) {
        super(number, street, unit, neighborhood, city, state, zipcode, country, location);
        this.id = id;
        this.name = name;
        this.type = type;
    }

    public void setId(int id) {
        this.id = id;
    }

    public int getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    /**
     *
     * @return the location as string
     */
    @NonNull
    @Override
    public String toString() {
        return String.format("{id:'%1$d', name:'%2$s', type:'%3$s', value:'%4$s'}", this.id, this.name, this.type, super.toString());
    }

    /**
     *
     * @return the location address as string
     */
    public String toAddress() {
        return super.toString();
    }

}
