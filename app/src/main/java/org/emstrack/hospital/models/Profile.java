package org.emstrack.hospital.models;

/**
 * Created by mauricio on 2/5/18.
 */

import java.util.ArrayList;

public class BasePermission {

    private Integer id = -1;
    private String name = "";
    private Boolean can_read = Boolean.FALSE;
    private Boolean can_write = Boolean.FALSE;

}

public class HospitalPermission extends BasePermission {

}

public class AmbulancePermission extends BasePermission {

}

public class Profile {

    private ArrayList<HospitalPermission> hospitals;
    private ArrayList<AmbulancePermission> ambulances;

    /* default constructor */
    public Profile() {
        hospitals = new ArrayList<>();
        ambulances = new ArrayList<>();
    }

    /* json constructor */
    public Profile(String json) {
        hospitals = new ArrayList<>();
        ambulances = new ArrayList<>();
    }

}
