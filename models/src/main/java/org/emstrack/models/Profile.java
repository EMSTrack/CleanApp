package org.emstrack.models;

import java.util.Collections;
import java.util.List;

/**
 * A class representing a user profile.
 * @author mauricio
 * @since 2/5/18
 */
public class Profile {

    private List<AmbulancePermission> ambulances = null;
    private List<HospitalPermission> hospitals = null;

    public List<AmbulancePermission> getAmbulances() {
        return ambulances;
    }

    public void setAmbulances(List<AmbulancePermission> ambulances) {
        this.ambulances = ambulances;
    }

    public List<HospitalPermission> getHospitals() {
        return hospitals;
    }

    public void setHospitals(List<HospitalPermission> hospitals) {
        this.hospitals = hospitals;
    }

    public void sortHospitals() {

        // Sort hospitals
        Collections.sort(this.hospitals, (a, b) -> a.getHospitalName().compareTo(b.getHospitalName()) );

    }

    public void sortAmbulances() {

        // Sort ambulances
        Collections.sort(this.ambulances, (a, b) -> a.getAmbulanceIdentifier().compareTo(b.getAmbulanceIdentifier()) );

    }

    public String toString() {
        String retval = "Profile with " + this.ambulances.size() + " ambulance(s) and " + this.hospitals.size() + " hospital(s)";
        for (AmbulancePermission ambulance: this.ambulances) {
            retval += "\n> Ambulance: " + ambulance.getAmbulanceIdentifier();
        }
        for (HospitalPermission hospital: this.hospitals) {
            retval += "\n> Hospital: " + hospital.getHospitalName();
        }
        return retval;
    }
}