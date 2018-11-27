package org.emstrack.models;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.List;

/**
 * Created by mauricio on 3/11/2018.
 */

public class Hospital{

    private int id;
    private String number;
    private String street;
    private String unit;
    private String neighborhood;
    private String city;
    private String state;
    private String zipcode;
    private String country;
    private GPSLocation location;
    private String name;
    private String comment;
    private int updatedBy;
    private Date updatedOn;
    private List<HospitalEquipment> hospitalequipmentSet = new ArrayList <HospitalEquipment>();

    public Hospital(int id,
                    String number,
                    String street,
                    String unit,
                    String neighborhood,
                    String city,
                    String state,
                    String zipcode,
                    String country,
                    String name,
                    GPSLocation location,
                    String comment,
                    int updatedBy, Date updatedOn,
                    List<HospitalEquipment> hospitalequipmentSet) {
        this.id = id;

        this.number = number;
        this.street= street;
        this.unit = unit;
        this.neighborhood= neighborhood;
        this.city = city;
        this.state = state;
        this.zipcode = zipcode;
        this.country = country;

        this.name = name;

        this.location = location;
        this.comment = comment;
        this.updatedBy = updatedBy;
        this.updatedOn = updatedOn;

        this.hospitalequipmentSet = hospitalequipmentSet;
    }

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
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

    public GPSLocation getLocation() {
        return location;
    }

    public void setLocation(GPSLocation location) {
        this.location = location;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getComment() {
        return comment;
    }

    public void setComment(String comment) {
        this.comment = comment;
    }

    public int getUpdatedBy() {
        return updatedBy;
    }

    public void setUpdatedBy(int updatedBy) {
        this.updatedBy = updatedBy;
    }

    public Date getUpdatedOn() {
        return updatedOn;
    }

    public void setUpdatedOn(Date updatedOn) {
        this.updatedOn = updatedOn;
    }

    public List<HospitalEquipment> getHospitalequipmentSet() {
        return hospitalequipmentSet;
    }

    public void setHospitalequipmentSet(List<HospitalEquipment> hospitalequipmentSet) {
        this.hospitalequipmentSet = hospitalequipmentSet;
    }

    @Override
    public String toString() {
        return "Hospital '" + name + "'" +
                "\nequipment = " + Arrays.toString(hospitalequipmentSet.toArray());
    }

}