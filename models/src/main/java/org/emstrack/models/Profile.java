package org.emstrack.models;

import android.os.Build;

import java.util.Collections;
import java.util.Comparator;
import java.util.List;

/**
 * A class representing a user profile.
 * @author mauricio
 * @since 2/5/18
 */
public class Profile {

    private List<AmbulancePermission> ambulances = null;
    private List<HospitalPermission> hospitals = null;

    public List<AmbulancePermission> getAmbulancePermissions() {
        return ambulances;
    }

    public void setAmbulancePermissions(List<AmbulancePermission> ambulancePermissions) {
        this.ambulances = ambulancePermissions;
    }

    public List<HospitalPermission> getHospitalPermissions() {
        return hospitals;
    }

    public void setHospitalPermissions(List<HospitalPermission> hospitalPermissions) {
        this.hospitals = hospitalPermissions;
    }

    public void sortHospitals() {

        if (hospitals != null) {
            // Sort hospitals
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                Collections.sort(hospitals, Comparator.comparing(HospitalPermission::getHospitalName));
            } else {
                Collections.sort(hospitals, (a, b) -> a.getHospitalName().compareTo(b.getHospitalName()));
            }
        }

    }

    public void sortAmbulances() {

        // Sort ambulances
        if (ambulances != null) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                Collections.sort(ambulances, Comparator.comparing(AmbulancePermission::getAmbulanceIdentifier));
            } else {
                Collections.sort(ambulances, (a, b) -> a.getAmbulanceIdentifier().compareTo(b.getAmbulanceIdentifier()));
            }
        }

    }

    public AmbulancePermission getPermission(Ambulance ambulance) {
        int ambulanceId = ambulance.getId();
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.N) {
            return ambulances.stream()
                    .filter(permission -> permission.getAmbulanceId() == ambulanceId)
                    .findAny()
                    .orElse(null);
        } else {
            for (AmbulancePermission permission : ambulances) {
                if (permission.getAmbulanceId() == ambulanceId) {
                    return permission;
                }
            }
            return null;
        }
    }

    public HospitalPermission getPermission(Hospital hospital) {
        int hospitalId = hospital.getId();
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.N) {
            return hospitals.stream()
                    .filter(permission -> permission.getHospitalId() == hospitalId)
                    .findAny()
                    .orElse(null);
        } else {
            for (HospitalPermission permission : hospitals) {
                if (permission.getHospitalId() == hospitalId) {
                    return permission;
                }
            }
            return null;
        }
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