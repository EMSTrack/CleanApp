package org.emstrack.models;

/**
 * Created by Leon on 5/8/2018.
 */

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

public class Call {

    private int id;
    private String status;
    private String details;
    private String priority;
    private Date createdAt;
    private Date pendingAt;
    private Date startedAt;
    private Date endedAt;
    private String comment;
    private int updatedBy;
    private Date updatedOn;
    private List<AmbulanceCall> ambulancecallSet = new ArrayList<>();
    private List<Patient> patientSet = new ArrayList <>();

    private AmbulanceCall currentAmbulanceCall;

    public Call() {
        id = -1;
        updatedBy = -1;
        this.currentAmbulanceCall = null;
    }

    public Call(int id, String status, String details, String priority, 
                Date createdAt, Date pendingAt, Date startedAt, Date endedAt,
                String comment, int updatedBy, Date updatedOn,
                List<AmbulanceCall> ambulancecallSet, List<Patient> patientSet) {
    
        this.id = id;
        this.status = status;
        this.details = details;
        this.priority = priority;
        this.createdAt = createdAt;
        this.pendingAt = pendingAt;
        this.startedAt = startedAt;
        this.endedAt = endedAt;
        this.comment = comment;
        this.updatedBy = updatedBy;
        this.updatedOn = updatedOn;

        this.ambulancecallSet = ambulancecallSet;
        this.patientSet = patientSet;

        this.currentAmbulanceCall = null;
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

    public List<AmbulanceCall> getAmbulancecallSet() {
        return ambulancecallSet;
    }

    public void setAmbulancecallSet(List<AmbulanceCall> ambulancecallSet) {
        this.ambulancecallSet = ambulancecallSet;
    }

    public List<Patient> getPatientSet() {
        return patientSet;
    }

    public void setPatientSet(List<Patient> patientSet) {
        this.patientSet = patientSet;
    }

    public AmbulanceCall getAmbulanceCall(int ambulance_id) {
        for (AmbulanceCall ambulanceCall : ambulancecallSet) {
            if (ambulanceCall.getAmbulanceId() == ambulance_id) {
                return ambulanceCall;
            }
        }
        return null;
    }

    public AmbulanceCall setCurrentAmbulanceCall(int ambulance_id) {
        return currentAmbulanceCall = getAmbulanceCall(ambulance_id);
    }

    public AmbulanceCall getCurrentAmbulanceCall() {
        return currentAmbulanceCall;
    }
}
