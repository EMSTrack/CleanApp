package org.emstrack.models;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collections;
import java.util.List;

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
    private Calendar createdAt;
    private Calendar pendingAt;
    private Calendar startedAt;
    private Calendar endedAt;
    private String comment;
    private int updatedBy;
    private Calendar updatedOn;
    private List<AmbulanceCall> ambulancecallSet = new ArrayList<>();
    private List<Patient> patientSet = new ArrayList <>();

    private List<CallNote> callnoteSet = new ArrayList<>();
    private CallNote lastUpdatedOnNote;

    private AmbulanceCall currentAmbulanceCall;
    private boolean sortedWaypoints;
    private boolean sortedNotes;

    public Call() {
        id = -1;
        updatedBy = -1;
        radioCode = -1;
        priorityCode = -1;
        currentAmbulanceCall = null;
        sortedWaypoints = false;
        sortedNotes = false;
        lastUpdatedOnNote = null;
    }

    public Call(int id, String status, String details, String priority, 
                Calendar createdAt, Calendar pendingAt, Calendar startedAt, Calendar endedAt,
                String comment, int updatedBy, Calendar updatedOn,
                List<AmbulanceCall> ambulancecallSet, List<Patient> patientSet,
                List<CallNote> callnoteSet) {
        this();
    
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
        this.callnoteSet = callnoteSet;
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

    public Calendar getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(Calendar createdAt) {
        this.createdAt = createdAt;
    }

    public Calendar getPendingAt() {
        return pendingAt;
    }

    public void setPendingAt(Calendar pendingAt) {
        this.pendingAt = pendingAt;
    }

    public Calendar getStartedAt() {
        return startedAt;
    }

    public void setStartedAt(Calendar startedAt) {
        this.startedAt = startedAt;
    }

    public Calendar getEndedAt() {
        return endedAt;
    }

    public void setEndedAt(Calendar endedAt) {
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

    public Calendar getUpdatedOn() {
        return updatedOn;
    }

    public void setUpdatedOn(Calendar updatedOn) {
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

    public List<CallNote> getCallnoteSet() {
        return callnoteSet;
    }

    public void setCallnoteSet(List<CallNote> callnoteSet) {
        this.callnoteSet = callnoteSet;
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

    public boolean isSortedWaypoints() {
        return sortedWaypoints;
    }

    public void setSortedWaypoints(boolean sortedWaypoints) {
        this.sortedWaypoints = sortedWaypoints;
    }

    public void sortWaypoints() {
        sortWaypoints(false);
    }

    public void sortWaypoints(boolean force) {
        if (force || !this.sortedWaypoints) {
            for (AmbulanceCall ambulanceCall : this.ambulancecallSet)
                ambulanceCall.sortWaypoints();
            this.sortedWaypoints = true;
        }

    }

    public CallNote getLastUpdatedOnNote() {
        return lastUpdatedOnNote;
    }

    public void setLastUpdatedOnNote() {
        sortNotes();
        if (callnoteSet.size() > 0) {
            this.lastUpdatedOnNote = callnoteSet.get(callnoteSet.size() - 1);
        } else {
            setLastUpdatedOnNote(null);
        }
    }

    public void setLastUpdatedOnNote(CallNote note) {
        this.lastUpdatedOnNote = note;
    }

    public void sortNotes() {
        sortNotes(false);
    }

    public void sortNotes(boolean force) {
        if (force || !this.sortedNotes) {
            Collections.sort(callnoteSet, new Note.SortAscending());
            this.sortedNotes = true;
        }
    }

    public int getNumberOfUnreadNotes() {
        // easy if never read
        if (lastUpdatedOnNote == null) {
            return callnoteSet.size();
        }

        sortNotes();
        int index = Collections.binarySearch(callnoteSet, lastUpdatedOnNote, new Note.SortAscending());
        if (index == -1) {
            return callnoteSet.size();
        } else {
            return callnoteSet.size() - index - 1;
        }

    }

}
