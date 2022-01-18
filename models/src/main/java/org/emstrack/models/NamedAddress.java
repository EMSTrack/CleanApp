package org.emstrack.models;

import android.content.Context;

import androidx.annotation.NonNull;

public class NamedAddress extends Address {

    private String name;

    /**
     * An address without a GPS location.
     *
     * @param name         the address name
     * @param number       the address number
     * @param street       the address street
     * @param unit         the address unit
     * @param neighborhood the address neighborhood
     * @param city         the address city
     * @param state        the address state (3 characters max)
     * @param zipcode      the address zipcode
     * @param country      the address country code (2 characters max)
     */
    public NamedAddress(String name,
                        String number,
                        String street,
                        String unit,
                        String neighborhood,
                        String city,
                        String state,
                        String zipcode,
                        String country) {
        super(number, street, unit, neighborhood, city, state, zipcode, country);
        this.name = name;
    }

    /**
     * An address with a GPS location.
     *
     * @param name         the address name
     * @param number       the address number
     * @param street       the address street
     * @param unit         the address unit
     * @param neighborhood the address neighborhood
     * @param city         the address city
     * @param state        the address state (3 characters max)
     * @param zipcode      the address zipcode
     * @param country      the address country code (2 characters max)
     * @param location     the address GPS location
     */
    public NamedAddress(String name,
                        String number,
                        String street,
                        String unit,
                        String neighborhood,
                        String city,
                        String state,
                        String zipcode,
                        String country,
                        GPSLocation location) {
        super(number, street, unit, neighborhood, city, state, zipcode, country, location);
        this.name = name;
    }

    /**
     * An address with only a GPS location.
     *
     * @param name         the address name
     * @param location the address GPS location
     */
    public NamedAddress(String name, GPSLocation location) {
        super(location);
        this.name = name;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    @Override
    @NonNull
    public String toString() {
        return this.name + "\n" + super.toString();
    }

    @NonNull
    public String toAddress() {
        return super.toString();
    }

    @NonNull
    public String toAddress(Context context) { return super.toString(); }

}
