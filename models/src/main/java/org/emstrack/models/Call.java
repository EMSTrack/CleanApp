package org.emstrack.models;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * A class representing a call.
 * @author Leon
 * @since 5/8/2018
 */
public class Call {

    public static final String STATUS_PENDING = "P";
    public static final String STATUS_STARTED = "S";
    public static final String STATUS_ENDED = "E";

    public class CallException extends Exception {

        public CallException(String message) {
            super(message);
        }
    }

    private int id;
    private String status;
    private String details;
    private String priority;
    private int priorityCode;
    private int radioCode;
    private Date createdAt;
    private Date pendingAt;
    private Date startedAt;
    private Date endedAt;
    private String comment;
    private int updatedBy;
    private Date updatedOn;
    private List<AmbulanceCall> ambulancecallSet = new ArrayList<>();
    private List<Patient> patientSet = new ArrayList <>();
    private List<CallNote> callNoteSet = new ArrayList<>();

    private AmbulanceCall currentAmbulanceCall;
    private boolean sorted;

    public Call() {
        id = -1;
        updatedBy = -1;
        radioCode = -1;
        priorityCode = -1;
        this.currentAmbulanceCall = null;
        this.sorted = false;
    }

    public Call(int id, String status, String details, String priority, 
                Date createdAt, Date pendingAt, Date startedAt, Date endedAt,
                String comment, int updatedBy, Date updatedOn,
                List<AmbulanceCall> ambulancecallSet, List<Patient> patientSet,
                List<CallNote> callNoteSet) {
    
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
        this.callNoteSet = callNoteSet;

        this.priorityCode = -1;
        this.radioCode = -1;
        this.currentAmbulanceCall = null;
        this.sorted = false;
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

    public int getRadioCode() {
        return radioCode;
    }

    public void setRadioCode(int radioCode) {
        this.radioCode = radioCode;
    }

    public int getPriorityCode() {
        return priorityCode;
    }

    public void setPriorityCode(int priorityCode) {
        this.priorityCode = priorityCode;
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

    public List<CallNote> getCallNoteSet() {
        return callNoteSet;
    }

    public void setCallNoteSet(List<CallNote> callNoteSet) {
        this.callNoteSet = callNoteSet;
    }

    public AmbulanceCall getAmbulanceCall(int ambulance_id) {
        for (AmbulanceCall ambulanceCall : ambulancecallSet) {
            if (ambulanceCall.getAmbulanceId() == ambulance_id) {
                return ambulanceCall;
            }
        }
        return null;
    }

    public void setCurrentAmbulanceCall(int ambulance_id) throws CallException {
        currentAmbulanceCall = getAmbulanceCall(ambulance_id);
        if (currentAmbulanceCall == null)
            throw new CallException("Ambulance is not part of call.");
    }

    public AmbulanceCall getCurrentAmbulanceCall() {
        return currentAmbulanceCall;
    }

    public boolean isSorted() {
        return sorted;
    }

    public void setSorted(boolean sorted) {
        this.sorted = sorted;
    }

    public void sortWaypoints() {
        sortWaypoints(false);
    }

    public void sortWaypoints(boolean force) {
        if (force || !this.sorted) {
            for (AmbulanceCall ambulanceCall : this.ambulancecallSet)
                ambulanceCall.sortWaypoints();
            this.sorted = true;
        }

    }
}
