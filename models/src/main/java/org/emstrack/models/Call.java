package org.emstrack.models;

/**
 * Created by Leon on 5/8/2018.
 */

import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class Call {

    private int id;
    private String status;
    private String details;
    private String priority;
    private String number;
    private String street;
    private String unit;
    private String neighborhood;
    private String city;
    private String state;
    private String zipcode;
    private String country;
    private Location location;
    private Date createdAt;
    private Date pendingAt;
    private Date startedAt;
    private Date endedAt;
    private String comment;
    private int updatedBy;
    private Date updatedOn;
    private List<Object> ambulancecallSet = new ArrayList<>();
    private List<Object> ambulanceupdateSet = new ArrayList<>();
    private List<Patient> patientSet = new ArrayList <>();

    public Call() {
        id = -1;
        updatedBy = -1;
    }

    public Call(int id, String status, String details, String priority, 
                String number, String street, String unit, String neighborhood, String city, 
                String state, String zipcode, String country, 
                Location location, 
                Date createdAt, Date pendingAt, Date startedAt, Date endedAt, 
                String comment, int updatedBy, Date updatedOn,
                List<Object> ambulancecallSet, List<Object> ambulanceupdateSet, List<Patient> patientSet) {
    
        this.id = id;
        this.status = status;
        this.details = details;
        this.priority = priority;
        this.number = number;
        this.street = street;
        this.unit = unit;
        this.neighborhood = neighborhood;
        this.city = city;
        this.state = state;
        this.zipcode = zipcode;
        this.country = country;
        this.location = location;
        this.pendingAt = pendingAt;
        this.createdAt = createdAt;
        this.endedAt = endedAt;
        this.startedAt = startedAt;
        this.endedAt = endedAt;
        this.comment = comment;
        this.updatedBy = updatedBy;
        this.updatedOn = updatedOn;

        this.ambulancecallSet = ambulancecallSet;
        this.ambulanceupdateSet = ambulanceupdateSet;
        this.patientSet = patientSet;
    
    }
    
    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public String getDetails() {
        return details;
    }

    public void setDetails(String details) {
        this.details = details;
    }

    public String getPriority() {
        return priority;
    }

    public void setPriority(String priority) {
        this.priority = priority;
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

    public Location getLocation() {
        return location;
    }

    public void setLocation(Location location) {
        this.location = location;
    }

    public Date getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(Date createdAt) {
        this.createdAt = createdAt;
    }

    public Date getPendingAt() {
        return pendingAt;
    }

    public void setPendingAt(Date pendingAt) {
        this.pendingAt = pendingAt;
    }

    public Date getStartedAt() {
        return startedAt;
    }

    public void setStartedAt(Date startedAt) {
        this.startedAt = startedAt;
    }

    public Date getEndedAt() {
        return endedAt;
    }

    public void setEndedAt(Date endedAt) {
        this.endedAt = endedAt;
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

    public void setUpdatedBy(Integer updatedBy) {
        this.updatedBy = updatedBy;
    }

    public Date getUpdatedOn() {
        return updatedOn;
    }

    public void setUpdatedOn(Date updatedOn) {
        this.updatedOn = updatedOn;
    }

    public List<Object> getAmbulancecallSet() {
        return ambulancecallSet;
    }

    public void setAmbulancecallSet(List<Object> ambulancecallSet) {
        this.ambulancecallSet = ambulancecallSet;
    }
    public void setAmbulanceupdateSet(List<Object> ambulanceupdateSet) {
        this.ambulanceupdateSet = ambulanceupdateSet;
    }

    public List<Object> getAmbulanceupdateSet() {
        return ambulanceupdateSet;
    }

    public List<Patient> getPatientSet() {
        return patientSet;
    }

    public void setPatientSet(List<Patient> patientSet) {
        this.patientSet = patientSet;
    }

    public Address getAddress() {
        return new Address(this.number, this.street, this.unit, this.neighborhood,this.city,this.state,this.zipcode,this.country);
    }

}
