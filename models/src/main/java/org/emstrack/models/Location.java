package org.emstrack.models;

public class Location extends Address {

    private String name;
    private String type;

    public Location(String name, String type,
                    String number, String street, String unit, String neighborhood, String city,
                    String state, String zipcode, String country,
                    GPSLocation location) {
        super(number, street, unit, neighborhood, city, state, zipcode, country, location);
        this.name = name;
        this.type = type;
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

}
