package org.emstrack.models;

/**
 * Created by mauricio on 3/11/2018.
 */


/**
 * A class representing an address.
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
    private GPSLocation location;

    /**
     * An address without a GPS location.
     *
     * @param number the address number
     * @param street the address street
     * @param unit the address unit
     * @param neighborhood the address neighborhood
     * @param city the address city
     * @param state the address state (3 characters max)
     * @param zipcode the address zipcode
     * @param country the address country code (2 characters max)
     */
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

    /**
     * An address with a GPS location.
     *
     * @param number the address number
     * @param street the address street
     * @param unit the address unit
     * @param neighborhood the address neighborhood
     * @param city the address city
     * @param state the address state (3 characters max)
     * @param zipcode the address zipcode
     * @param country the address country code (2 characters max)
     * @param location the address GPS location
     */
    public Address(String number,
                   String street,
                   String unit,
                   String neighborhood,
                   String city,
                   String state,
                   String zipcode,
                   String country,
                   GPSLocation location) {
        this.number = number;
        this.street= street;
        this.unit = unit;
        this.neighborhood= neighborhood;
        this.city = city;
        this.state = state;
        this.zipcode = zipcode;
        this.country = country;
        this.location = location;
    }

    /**
     * An address with only a GPS location.
     *
     * @param location the address GPS location
     */
    public Address(GPSLocation location) {
        this.number = "";
        this.street= "";
        this.unit = "";
        this.neighborhood= "";
        this.city = "";
        this.state = "";
        this.zipcode = "";
        this.country = "";
        this.location = location;
    }

    /**
     * @return the address number
     */
    public String getNumber() {
        return number;
    }

    /**
     * @param number the address number
     */
    public void setNumber(String number) {
        this.number = number;
    }

    /**
     * @return the address street
     */
    public String getStreet() {
        return street;
    }

    /**
     * @param street the address street
     */
    public void setStreet(String street) {
        this.street = street;
    }

    /**
     * @return the address unit
     */
    public String getUnit() {
        return unit;
    }

    /**
     * @param unit the address unit
     */
    public void setUnit(String unit) {
        this.unit = unit;
    }

    /**
     * @return the address neighborhood
     */
    public String getNeighborhood() {
        return neighborhood;
    }

    /**
     * @param neighborhood the address neighborhood
     */
    public void setNeighborhood(String neighborhood) {
        this.neighborhood = neighborhood;
    }

    /**
     * @return the address city
     */
    public String getCity() {
        return city;
    }

    /**
     * @param city the address city
     */
    public void setCity(String city) {
        this.city = city;
    }

    /**
     * @return the address state
     */
    public String getState() {
        return state;
    }

    /**
     * @param state the address state
     */
    public void setState(String state) {
        this.state = state;
    }

    /**
     * @return the address zipcode
     */
    public String getZipcode() {
        return zipcode;
    }

    /**
     * @param zipcode the address zipcode
     */
    public void setZipcode(String zipcode) {
        this.zipcode = zipcode;
    }

    /**
     * @return the address country
     */
    public String getCountry() {
        return country;
    }

    /**
     * @param country the address country
     */
    public void setCountry(String country) {
        this.country = country;
    }

    /**
     * @return the address GPS location
     */
    public GPSLocation getLocation() {
        return location;
    }

    /**
     * @param location the address GPS location
     */
    public void setLocation(GPSLocation location) {
        this.location = location;
    }

    /**
     * @return a string representation of the address
     */
    public String toString() {

        // TODO: Take into account the locale

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