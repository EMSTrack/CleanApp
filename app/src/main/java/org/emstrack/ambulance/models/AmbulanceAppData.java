package org.emstrack.ambulance.models;

import org.emstrack.models.Credentials;
import org.emstrack.models.Profile;
import org.emstrack.models.Token;

public class AmbulanceAppData {

    final static String TAG = AmbulanceAppData.class.getSimpleName();

    private Credentials credentials;
    private Profile profile;
    private Token token;

    public AmbulanceAppData() {
    }

    public void setCredentials(Credentials credentials) {
        this.credentials = credentials;
    }

}

