package org.emstrack.ambulance.models;

import android.util.SparseArray;

import org.emstrack.models.Ambulance;
import org.emstrack.models.CallStack;
import org.emstrack.models.Credentials;
import org.emstrack.models.EquipmentItem;
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

    private Token token;
    private Credentials credentials;
    private Profile profile;
    private Settings settings;
    private Ambulance ambulance;
    private SparseArray<Ambulance> ambulances;
    private SparseArray<Hospital> hospitals;
    private List<Location> bases;
    private List<Location> others;
    private CallStack calls;

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
     * @param credentials
     */
    public AmbulanceAppData(Credentials credentials) {
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
    public Token getToken() {
        return token;
    }

    /**
     *
     * @param credentials the credentials
     */
    public void setCredentials(Credentials credentials) {
        this.credentials = credentials;
    }

    /**
     *
     * @return the servers lists
     */
    public List<String> getServersList() { return serversList; }

    /**
     *
     * @return the servers lists
     */
    public void setServersList(List<String> serversList) { this.serversList = serversList; }

    /**
     *
     * @return the credentials
     */
    public Credentials getCredentials() {
        return credentials;
    }

    /**
     *
     * @param profile the profile
     */
    public void setProfile(Profile profile) {
        this.profile = profile;
    }

    /**
     *
     * @return the profile
     */
    public Profile getProfile() {
        return profile;
    }

    /**
     *
     * @param settings the settings
     */
    public void setSettings(Settings settings) {
        this.settings = settings;
    }

    /**
     *
     * @return the settings
     */
    public Settings getSettings() {
        return settings;
    }

    /**
     *
     * @param bases the list of bases
     */
    public void setBases(List<Location> bases) {
        this.bases = bases;
    }

    /**
     *
     * @return the list of bases
     */
    public List<Location> getBases() {
        return bases;
    }

    /**
     *
     * @param others  list of other locations
     */
    public void setOtherLocations(List<Location> others) {
        this.others= others;
    }

    /**
     *
     * @return the list of other locations
     */
    public List<Location> getOtherLocations() {
        return others;
    }

    /**
     *
     * @param radioCodes list of radio codes
     */
    public void setRadioCodes(List<RadioCode> radioCodes) {
        this.radioCodes = new SparseArray<>();
        for (RadioCode code : radioCodes) {
            this.radioCodes.put(code.getId(), code);
        }
    }

    /**
     *
     * @return the list of radio codes
     */
    public SparseArray<RadioCode> getRadioCodes() {
        return radioCodes;
    }

    /**
     *
     * @param priorityCodes list of priority codes
     */
    public void setPriorityCodes(List<PriorityCode> priorityCodes) {
        this.priorityCodes = new SparseArray<>();
        for (PriorityCode code : priorityCodes) {
            this.priorityCodes.put(code.getId(), code);
        }
    }

    /**
     *
     * @return the list of priority codes
     */
    public SparseArray<PriorityCode> getPriorityCodes() {
        return priorityCodes;
    }

    /**
     *
     * @param priorityClassifications list of priority classifications
     */
    public void setPriorityClassifications(List<PriorityClassification> priorityClassifications) {
        this.priorityClassifications = new SparseArray<>();
        for (PriorityClassification classification : priorityClassifications) {
            this.priorityClassifications.put(classification.getId(), classification);
        }
    }

    /**
     *
     * @return the list of priority classifications
     */
    public SparseArray<PriorityClassification> getPriorityClassifications() {
        return priorityClassifications;
    }

    /**
     *
     * @param ambulances the SparseArray of ambulances
     */
    public void setAmbulances(SparseArray<Ambulance> ambulances) {
        this.ambulances = ambulances;
    }

    /**
     *
     * @param ambulances the list of ambulances
     */
    public void setAmbulances(List<Ambulance> ambulances) {
        this.ambulances = new SparseArray<>();
        for (Ambulance ambulance : ambulances) {
            this.ambulances.put(ambulance.getId(), ambulance);
        }
    }

    /**
     *
     * @return the list of ambulances
     */
    public SparseArray<Ambulance> getAmbulances() {
        return ambulances;
    }

    /**
     *
     * @param hospitals the SparseArray of hospitals
     */
    public void setHospitals(SparseArray<Hospital> hospitals) {
        this.hospitals = hospitals;
    }

    /**
     *
     * @param hospitals the list of hospitals
     */
    public void setHospitals(List<Hospital> hospitals) {
        this.hospitals = new SparseArray<>();
        for (Hospital hospital: hospitals) {
            this.hospitals.put(hospital.getId(), hospital);
        }
    }

    /**
     *
     * @return the list of hospitals
     */
    public SparseArray<Hospital> getHospitals() {
        return hospitals;
    }

    /**
     *
     * @return the current ambulance or null if no current ambulance is set
     */
    public Ambulance getAmbulance() {
        return ambulance;
    }

    /**
     *
     * @param ambulance the current ambulance
     */
    public void setAmbulance(Ambulance ambulance) {
        this.ambulance = ambulance;
    }

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
     *
     * @return the call stack
     */
    public CallStack getCalls() {
        return calls;
    }

}

