package org.emstrack.ambulance.models;

import android.support.annotation.NonNull;
import android.util.SparseArray;

import org.emstrack.models.Ambulance;
import org.emstrack.models.CallStack;
import org.emstrack.models.Credentials;
import org.emstrack.models.EquipmentItem;
import org.emstrack.models.Hospital;
import org.emstrack.models.Location;
import org.emstrack.models.Profile;
import org.emstrack.models.Settings;
import org.emstrack.models.Token;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.ListIterator;
import java.util.Map;

public class AmbulanceAppData {

    final static String TAG = AmbulanceAppData.class.getSimpleName();

    private Token token;
    private Credentials credentials;
    private Profile profile;
    private Settings settings;
    private Ambulance ambulance;
    private SparseArray<Ambulance> ambulances;
    private SparseArray<Hospital> hospitals;
    private SparseArray<EquipmentItem> equipment;
    private List<Location> bases;
    private CallStack calls;

    /**
     *
     */
    public AmbulanceAppData() {
        ambulances = new SparseArray<>();
        hospitals = new SparseArray<>();
        equipment = new SparseArray<>();
        bases = new ArrayList<>();
        calls = new CallStack();
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
     * @param equipment the SparseArray of equipment
     */
    public void setEquipment(SparseArray<EquipmentItem> equipment) {
        this.equipment = equipment;
    }

    public void setEquipment(List<EquipmentItem> equipment) {
        this.equipment = new SparseArray<>();
        for (EquipmentItem item : equipment) {
            this.equipment.put(item.getEquipmentId(), item);
        }
    }

    public SparseArray<EquipmentItem> getEquipment() {
        return equipment;
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

