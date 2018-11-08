package org.emstrack.models;

import java.util.Date;

/**
 * Created by mauricio on 3/11/2018.
 * This model is not used yet since all database models are flat
 * Could be used in the future but requires a custom serializer
 */

public class Address {

    private String number;
    private String street;
    private String unit;
    private String neighborhood;
    private String city;
    private String state;
    private String zipcode;
    private String country;
    private Location location;

    public Address(String number,
                   String street,
                   String unit,
                   String neighborhood,
                   String city,
                   String state,
                   String zipcode,
                   String country) {

        this.number = number;
        this.street= street;
        this.unit = unit;
        this.neighborhood= neighborhood;
        this.city = city;
        this.state = state;
        this.zipcode = zipcode;
        this.country = country;
    }

    public String getNumber() {
        return number;
    }

    public void setNumber(String number) {
        this.number = number;
    }

    public String getStreet() {
        return street;
    }

    public void setStreet(String street) {
        this.street = street;
    }

    public String getUnit() {
        return unit;
    }

    public void setUnit(String unit) {
        this.unit = unit;
    }

    public String getNeighborhood() {
        return neighborhood;
    }

    public void setNeighborhood(String neighborhood) {
        this.neighborhood = neighborhood;
    }

    public String getCity() {
        return city;
    }

    public void setCity(String city) {
        this.city = city;
    }

    public String getState() {
        return state;
    }

    public void setState(String state) {
        this.state = state;
    }

    public String getZipcode() {
        return zipcode;
    }

    public void setZipcode(String zipcode) {
        this.zipcode = zipcode;
    }

    public String getCountry() {
        return country;
    }

    public void setCountry(String country) {
        this.country = country;
    }

    public Location getLocation() {
        return location;
    }

    public void setLocation(Location location) {
        this.location = location;
    }

    public String toString() {
        String retValue = "";
        retValue += this.number + " " + this.street;
        if (this.unit != null && !this.unit.isEmpty())
            retValue += " " + this.unit;
        if (this.neighborhood != null && !this.neighborhood.isEmpty())
            retValue += ", " + this.neighborhood;
        if (this.city != null && !this.city.isEmpty())
            retValue += ", " + this.city;
        if (this.state != null && !this.state.isEmpty())
            retValue += ", " + this.state;
        if (this.zipcode != null && !this.zipcode.isEmpty())
            retValue += " " + this.zipcode;
        if (this.country != null && !this.country.isEmpty())
            retValue += ", " + this.country;
        return retValue;
    }

}

