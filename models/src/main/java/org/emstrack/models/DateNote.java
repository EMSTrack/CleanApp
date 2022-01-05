package org.emstrack.models;

import java.util.Calendar;

public class DateNote extends Note {

    public DateNote(Note note) {
        super("", "", -1, note.getUpdatedOn());
    }

    public DateNote(Calendar updatedOn) {
        super("", "", -1, updatedOn);
    }

}
