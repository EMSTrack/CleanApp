package org.emstrack.models;

import java.util.Calendar;

/**
 * A class representing an ambulance call note.
 */
public class AmbulanceNote extends Note {

    public AmbulanceNote(String comment, String updateByUsername, int updatedBy, Calendar updatedOn) {
        super(comment, updateByUsername, updatedBy, updatedOn);
    }

    public AmbulanceNote(String comment) {
        super(comment);
    }

}
