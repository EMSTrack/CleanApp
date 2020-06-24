package org.emstrack.models;

import org.emstrack.models.gson.Exclude;

import java.util.Date;

/**
 * A class representing the API client
 *
 * @author mauricio
 * @since 02/22/2019
 */

public class Client {

    public static final String STATUS_ONLINE = "O";
    public static final String STATUS_OFFLINE = "F";
    public static final String STATUS_DISCONNECTED = "D";
    public static final String STATUS_RECONNECTED = "R";


    private String clientId;
    @Exclude
    private String username;
    private String status;
    private Integer ambulance;
    private Integer hospital;
    @Exclude
    private Date updatedOn;

    /**
     *
     * @param clientId the client id
     * @param status the status
     * @param ambulance the ambulance id or <code>null</code>
     * @param hospital the hospital id or <code>null</code>
     */
    public Client(String clientId, String status, Integer ambulance, Integer hospital) {
        this.username = "";
        this.clientId = clientId;
        this.status = status;
        this.ambulance = ambulance;
        this.hospital = hospital;
    }

    /**
     *
     * @param username the username
     * @param clientId the client id
     */
    public Client(String username, String clientId) {
        this.username = username;
        this.clientId = clientId;
        this.status = STATUS_ONLINE;
        this.ambulance = -1;
        this.hospital = -1;
    }

    /**
     *
     * @return the client id
     */
    public String getClientId() {
        return clientId;
    }

    /**
     *
     * @param clientId the client id
     */
    public void setClientId(String clientId) {
        this.clientId = clientId;
    }

    /**
     *
     * @return the username
     */
    public String getUsername() {
        return username;
    }

    /**
     *
     * @param username the username
     */
    public void setUsername(String username) {
        this.username = username;
    }

    /**
     *
     * @return the status
     */
    public String getStatus() {
        return status;
    }

    /**
     *
     * @param status the status
     */
    public void setStatus(String status) {
        this.status = status;
    }

    /**
     *
     * @return the ambulance id
     */
    public Integer getAmbulance() {
        return ambulance;
    }

    /**
     *
     * @param ambulance the ambulance id
     */
    public void setAmbulance(Integer ambulance) {
        this.ambulance = ambulance;
    }

    /**
     *
     * @return the hospital id
     */
    public Integer getHospital() {
        return hospital;
    }

    /**
     *
     * @param hospital the hospital id
     */
    public void setHospital(Integer hospital) {
        this.hospital = hospital;
    }

    /**
     *
     * @return the update date
     */
    public Date getUpdatedOn() {
        return updatedOn;
    }

    /**
     *
     * @param updatedOn the update date
     */
    public void setUpdatedOn(Date updatedOn) {
        this.updatedOn = updatedOn;
    }


    /**
     *
     * @return string representaion
     */
    public String toString() {
        return username + " @ " + clientId;
    }

}
