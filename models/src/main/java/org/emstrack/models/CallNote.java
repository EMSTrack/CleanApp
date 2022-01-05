package org.emstrack.models;

import java.util.Calendar;

/**
 * A class representing an ambulance call note.
 */
public class CallNote extends Note {

    public CallNote(String comment, String updatedByUsername, int updatedBy, Calendar updatedOn) {
        super(comment, updatedByUsername, updatedBy, updatedOn);
    }

    public CallNote(String comment) {
        super(comment);
    }

}
