package org.emstrack.models;

import java.util.Calendar;
import java.util.Comparator;

public interface UpdatedOn {

    static public class SortAscending implements Comparator<UpdatedOn> {
        public int compare(UpdatedOn a, UpdatedOn b)
        {
            return a.getUpdatedOn().compareTo(b.getUpdatedOn());
        }
    }

    static public class SortDescending implements Comparator<UpdatedOn> {
        public int compare(UpdatedOn a, UpdatedOn b)
        {
            return b.getUpdatedOn().compareTo(a.getUpdatedOn());
        }
    }

    public Calendar getUpdatedOn();

    public void setUpdatedOn(Calendar updatedOn);

}
