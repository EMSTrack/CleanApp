package org.emstrack.models;

import org.emstrack.models.gson.Exclude;

import java.util.Calendar;
import java.util.Comparator;

/**
 * A class representing an ambulance or call note.
 */
public class Note {

    static public class SortAscending implements Comparator<Note> {
        public int compare(Note a, Note b)
        {
            return a.getUpdatedOn().compareTo(b.getUpdatedOn());
        }
    }

    private String comment;
    @Exclude
    private String updatedByUsername;
    @Exclude
    private int updatedBy;
    @Exclude
    private Calendar updatedOn;

    public Note(String comment, String updatedByUsername, int updatedBy, Calendar updatedOn) {
        this.comment = comment;
        this.updatedBy = updatedBy;
        this.updatedByUsername = updatedByUsername;
        this.updatedOn = updatedOn;
    }

    public Note(String comment) {
        this.comment = comment;
        this.updatedByUsername = "";
        this.updatedBy = -1;
        this.updatedOn = Calendar.getInstance();
    }

    public String getComment() {
        return comment;
    }

    public void setComment(String comment) {
        this.comment = comment;
    }

    public String getUpdatedByUsername() {
        return updatedByUsername;
    }

    public void setUpdatedByUsername(String updatedByUsername) {
        this.updatedByUsername = updatedByUsername;
    }

    public Calendar getUpdatedOn() {
        return updatedOn;
    }

    public void setUpdatedBy(int updatedBy) {
        this.updatedBy = updatedBy;
    }

    public int getUpdatedBy() {
        return updatedBy;
    }

    public void setUpdatedOn(Calendar updatedOn) {
        this.updatedOn = updatedOn;
    }

}
