package org.emstrack.models;

/**
 * Created by Leon on 5/8/2018.
 */

import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class Call {

    private Integer id;
    private String status;
    private String details;
    private String priority;
    private String number;
    private String street;
    private Object unit;
    private Object neighborhood;
    private String city;
    private String state;
    private String zipcode;
    private String country;
    private Location location;
    private String createdAt;
    private Object pendingAt;
    private Object startedAt;
    private Object endedAt;
    private Object comment;
    private Integer updatedBy;
    private String updatedOn;
    private List<Object> ambulancecallSet = null;
    private List<Object> patientSet = null;
    private Map<String, Object> additionalProperties = new HashMap<String, Object>();

    public Integer getId() {
        return id;
    }

    public void setId(Integer id) {
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

    public void setUnit(Object unit) {
        this.unit = unit;
    }

    public Object getNeighborhood() {
        return neighborhood;
    }

    public void setNeighborhood(Object neighborhood) {
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

    public String getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(String createdAt) {
        this.createdAt = createdAt;
    }

    public Object getPendingAt() {
        return pendingAt;
    }

    public void setPendingAt(Object pendingAt) {
        this.pendingAt = pendingAt;
    }

    public Object getStartedAt() {
        return startedAt;
    }

    public void setStartedAt(Object startedAt) {
        this.startedAt = startedAt;
    }

    public Object getEndedAt() {
        return endedAt;
    }

    public void setEndedAt(Object endedAt) {
        this.endedAt = endedAt;
    }

    public Object getComment() {
        return comment;
    }

    public void setComment(Object comment) {
        this.comment = comment;
    }

    public Integer getUpdatedBy() {
        return updatedBy;
    }

    public void setUpdatedBy(Integer updatedBy) {
        this.updatedBy = updatedBy;
    }

    public String getUpdatedOn() {
        return updatedOn;
    }

    public void setUpdatedOn(String updatedOn) {
        this.updatedOn = updatedOn;
    }

    public List<Object> getAmbulancecallSet() {
        return ambulancecallSet;
    }

    public void setAmbulancecallSet(List<Object> ambulancecallSet) {
        this.ambulancecallSet = ambulancecallSet;
    }

    public List<Object> getPatientSet() {
        return patientSet;
    }

    public void setPatientSet(List<Object> patientSet) {
        this.patientSet = patientSet;
    }

    public Map<String, Object> getAdditionalProperties() {
        return this.additionalProperties;
    }

    public void setAdditionalProperty(String name, Object value) {
        this.additionalProperties.put(name, value);
    }

}
