package org.emstrack.models;

import java.util.Date;

/**
 * A class representing an ambulance call note.
 */
public class CallNote extends Note {

    public CallNote(String comment, int updatedBy, Date updatedOn) {
        super(comment, updatedBy, updatedOn);
    }

    public CallNote(String comment) {
        super(comment);
    }

}
