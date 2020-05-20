package org.emstrack.models;

import org.emstrack.models.gson.Exclude;

import java.util.Date;

/**
 * A class representing an ambulance call note.
 */
public class CallNote {

    private String comment;
    @Exclude
    private int updatedBy;
    @Exclude
    private Date updatedOn;

    public CallNote(String comment, int updatedBy, Date updatedOn) {
        this.comment = comment;
        this.updatedBy = updatedBy;
        this.updatedOn = updatedOn;
    }

    public CallNote(String comment) {
        this.comment = comment;
        this.updatedBy = -1;
        this.updatedOn = new Date();
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

}
