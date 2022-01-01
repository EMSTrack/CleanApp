package org.emstrack.models;

import java.util.Date;

/**
 * A class representing an ambulance call note.
 */
public class AmbulanceNote extends Note {

    public AmbulanceNote(String comment, int updatedBy, Date updatedOn) {
        super(comment, updatedBy, updatedOn);
    }

    public AmbulanceNote(String comment) {
        super(comment);
    }

}
