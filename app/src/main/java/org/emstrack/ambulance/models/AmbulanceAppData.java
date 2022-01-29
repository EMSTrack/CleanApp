package org.emstrack.ambulance.models;

import android.util.SparseArray;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import org.emstrack.models.Ambulance;
import org.emstrack.models.CallStack;
import org.emstrack.models.Credentials;
import org.emstrack.models.Hospital;
import org.emstrack.models.Location;
import org.emstrack.models.PriorityClassification;
import org.emstrack.models.PriorityCode;
import org.emstrack.models.Profile;
import org.emstrack.models.RadioCode;
import org.emstrack.models.Settings;
import org.emstrack.models.Token;

import java.util.ArrayList;
import java.util.List;

public class AmbulanceAppData {

    final static String TAG = AmbulanceAppData.class.getSimpleName();

    // nullable properties
    private Token token;
    private Credentials credentials;
    private Profile profile;
    private Settings settings;
    private Ambulance ambulance;

    private SparseArray<Ambulance> ambulances;
    private SparseArray<Hospital> hospitals;
    private List<Location> bases;
    private List<Location> others;
    private final CallStack calls;

    private SparseArray<RadioCode> radioCodes;
    private SparseArray<PriorityCode> priorityCodes;
    private SparseArray<PriorityClassification> priorityClassifications;
    private List<String> serversList;

    /**
     *
     */
    public AmbulanceAppData() {
        ambulances = new SparseArray<>();
        hospitals = new SparseArray<>();
        bases = new ArrayList<>();
        others = new ArrayList<>();
        calls = new CallStack();

        radioCodes = new SparseArray<>();
        priorityCodes = new SparseArray<>();
        priorityClassifications = new SparseArray<>();
        serversList = new ArrayList<>();
    }

    /**
     *
     * @param credentials the credentials
     */
    public AmbulanceAppData(@NonNull Credentials credentials) {
        this();
        this.credentials = credentials;
    }

    /**
     * @param token the token
     */
    public void setToken(Token token) {
        this.token = token;
    }

    /**
     *
     * @return the token
     */
    @Nullable
    public Token getToken() {
        return token;
    }

    /**
     *
     * @param credentials the credentials
     */
    public void setCredentials(@NonNull Credentials credentials) {
        this.credentials = credentials;
    }

    /**
     *
     * @return the credentials
     */
    @Nullable
    public Credentials getCredentials() {
        return credentials;
    }

    /**
     *
     * @param profile the profile
     */
    public void setProfile(@NonNull Profile profile) {
        this.profile = profile;
    }

    /**
     *
     * @return the profile
     */
    @Nullable
    public Profile getProfile() {
        return profile;
    }

    /**
     *
     * @param settings the settings
     */
    public void setSettings(@NonNull Settings settings) {
        this.settings = settings;
    }

    /**
     *
     * @return the settings
     */
    @Nullable
    public Settings getSettings() { return settings; }

    /**
     * Clear current ambulance
     */
    public void clearAmbulance() { this.ambulance = null; }

    /**
     *
     * @param ambulance the current ambulance
     */
    public void setAmbulance(@NonNull Ambulance ambulance) {
        this.ambulance = ambulance;
    }

    /**
     *
     * @return the current ambulance or null if no current ambulance is set
     */
    @Nullable
    public Ambulance getAmbulance() { return ambulance; }

    /**
     *
     * @return the current ambulance id or -1 if no current ambulance is set
     */
    public int getAmbulanceId() {
        if (this.ambulance == null)
            return -1;
        else
            return this.ambulance.getId();
    }

    /**
     * Get ambulance by id
     *
     * @param id the ambulance id
     * @return the ambulance or null if none found
     */
    @Nullable
    public Ambulance getAmbulanceById(int id) {
        if (ambulance != null && ambulance.getId() == id) {
            return ambulance;
        } else if (ambulances != null) {
            // search in array
            return ambulances.get(id);
        }
        return null;
    }

    /**
     * @param serversList the servers list
     */
    public void setServersList(@NonNull List<String> serversList) { this.serversList = serversList; }

    /**
     *
     * @return the servers list
     */
    @NonNull
    public List<String> getServersList() { return serversList; }

    /**
     *
     * @param bases the list of bases
     */
    public void setBases(@NonNull List<Location> bases) {
        this.bases = bases;
    }

    /**
     *
     * @return the list of bases
     */
    @NonNull
    public List<Location> getBases() {
        return bases;
    }

    /**
     *
     * @param others  list of other locations
     */
    public void setOtherLocations(@NonNull List<Location> others) {
        this.others= others;
    }

    /**
     *
     * @return the list of other locations
     */
    @NonNull
    public List<Location> getOtherLocations() {
        return others;
    }

    /**
     *
     * @param radioCodes list of radio codes
     */
    public void setRadioCodes(@NonNull List<RadioCode> radioCodes) {
        this.radioCodes = new SparseArray<>();
        for (RadioCode code : radioCodes) {
            this.radioCodes.put(code.getId(), code);
        }
    }

    /**
     *
     * @return the list of radio codes
     */
    @NonNull
    public SparseArray<RadioCode> getRadioCodes() {
        return radioCodes;
    }

    /**
     *
     * @param priorityCodes list of priority codes
     */
    public void setPriorityCodes(@NonNull List<PriorityCode> priorityCodes) {
        this.priorityCodes = new SparseArray<>();
        for (PriorityCode code : priorityCodes) {
            this.priorityCodes.put(code.getId(), code);
        }
    }

    /**
     *
     * @return the list of priority codes
     */
    @NonNull
    public SparseArray<PriorityCode> getPriorityCodes() {
        return priorityCodes;
    }

    /**
     *
     * @param priorityClassifications list of priority classifications
     */
    public void setPriorityClassifications(@NonNull List<PriorityClassification> priorityClassifications) {
        this.priorityClassifications = new SparseArray<>();
        for (PriorityClassification classification : priorityClassifications) {
            this.priorityClassifications.put(classification.getId(), classification);
        }
    }

    /**
     *
     * @return the list of priority classifications
     */
    @NonNull
    public SparseArray<PriorityClassification> getPriorityClassifications() {
        return priorityClassifications;
    }

    /**
     *
     * @param ambulances the SparseArray of ambulances
     */
    public void setAmbulances(@NonNull SparseArray<Ambulance> ambulances) {
        this.ambulances = ambulances;
    }

    /**
     *
     * @param ambulances the list of ambulances
     */
    public void setAmbulances(@NonNull List<Ambulance> ambulances) {
        this.ambulances = new SparseArray<>();
        for (Ambulance ambulance : ambulances) {
            this.ambulances.put(ambulance.getId(), ambulance);
        }
    }

    /**
     *
     * @return the list of ambulances
     */
    @NonNull
    public SparseArray<Ambulance> getAmbulances() {
        return ambulances;
    }

    /**
     *
     * @param hospitals the SparseArray of hospitals
     */
    public void setHospitals(@NonNull SparseArray<Hospital> hospitals) {
        this.hospitals = hospitals;
    }

    /**
     *
     * @param hospitals the list of hospitals
     */
    public void setHospitals(@NonNull List<Hospital> hospitals) {
        this.hospitals = new SparseArray<>();
        for (Hospital hospital: hospitals) {
            this.hospitals.put(hospital.getId(), hospital);
        }
    }

    /**
     *
     * @return the list of hospitals
     */
    @NonNull
    public SparseArray<Hospital> getHospitals() {
        return hospitals;
    }


    /**
     *
     * @return the call stack
     */
    @NonNull
    public CallStack getCalls() {
        return calls;
    }

}

