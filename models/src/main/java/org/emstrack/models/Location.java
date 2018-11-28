package org.emstrack.models;

public class Location {

    private String name;
    private String type;
    private String number;
    private String street;
    private String unit;
    private String neighborhood;
    private String city;
    private String state;
    private String zipcode;
    private String country;
    private GPSLocation location;

    public Location(String name, String type,
                    String number, String street, String unit, String neighborhood, String city,
                    String state, String zipcode, String country,
                    GPSLocation location) {
        this.name = name;
        this.type = type;
        this.number = number;
        this.street = street;
        this.unit = unit;
        this.neighborhood = neighborhood;
        this.city = city;
        this.state = state;
        this.zipcode = zipcode;
        this.country = country;
        this.location = location;
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

    public Object getUnit() {
        return unit;
    }

    public void setUnit(String unit) {
        this.unit = unit;
    }

    public Object getNeighborhood() {
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

    public GPSLocation getLocation() {
        return location;
    }

    public void setLocation(GPSLocation location) {
        this.location = location;
    }

    public Address getAddress() {
        return new Address(this.number, this.street, this.unit, this.neighborhood,this.city,this.state,this.zipcode,this.country);
    }

}
