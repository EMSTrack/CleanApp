package org.emstrack.models.api;

import org.emstrack.models.Ambulance;
import org.emstrack.models.Client;
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
import org.emstrack.models.Version;

import java.util.List;

import retrofit2.http.Body;
import retrofit2.http.GET;
import retrofit2.http.Header;
import retrofit2.http.POST;
import retrofit2.Call;
import retrofit2.http.Path;

/**
 * Retrofit interface to models API
 *
 * @author mauricio
 * @since 1/19/2019
 */
public interface APIService {

    /**
     * Retrieve token
     *
     * @param credentials the user credentials
     * @return the api call
     */
    @POST("/en/auth/token/")
    Call<Token> getToken(@Body Credentials credentials);

    /**
     * Retrieve the user profile
     *
     * @param username the username
     * @return the api call
     */
    @GET("user/{username}/profile/")
    Call<Profile> getProfile(@Path("username") String username);

    /**
     * Retrieve the settings
     *
     * @return the api call
     */
    @GET("settings/")
    Call<Settings> getSettings();

    /**
     * Retrieve locations by type
     *
     * @param type the type
     * @return the api call
     */
    @GET("location/{type}/")
    Call<List<Location>> getLocationsByType(@Path("type") String type);

    /**
     * Retrieve locations
     *
     * @return the api call
     */
    @GET("location/")
    Call<List<Location>> getLocations();

    /**
     * Retrieve hospitals
     *
     * @return the api call
     */
    @GET("hospital/")
    Call<List<Hospital>> getHospitals();

    /**
     * Retrieve ambulances
     *
     * @return the api call
     */
    @GET("ambulance/")
    Call<List<Ambulance>> getAmbulances();

    /**
     * Retrieve ambulance
     *
     * @return the api call
     */
    @GET("ambulance/{id}/")
    Call<Ambulance> getAmbulance(@Path("id") int id);

    /**
     * Retrieve ambulance's calls
     *
     * @return the api call
     */
    @GET("ambulance/{id}/calls/")
    Call<List<org.emstrack.models.Call>> getCalls(@Path("id") int id);

    /**
     * Retrieve ambulance's equipment
     *
     * @return the equipment
     */
    @GET("ambulance/{id}/equipment/")
    Call<List<org.emstrack.models.EquipmentItem>> getAmbulanceEquipment(@Path("id") int id);

    /**
     * Retrieve call
     *
     * @return the api call
     */
    @GET("call/{id}/")
    Call<org.emstrack.models.Call> getCall(@Path("id") int id);

    /**
     * Retrieve api version
     *
     * @return the api call
     */
    @GET("version/")
    Call<Version> getVersion();

    /**
     * Retrieve radio codes
     *
     * @return the api call
     */
    @GET("radio/")
    Call<List<RadioCode>> getRadioCodes();

    /**
     * Retrieve priority codes
     *
     * @return the api call
     */
    @GET("priority/")
    Call<List<PriorityCode>> getPriorityCodes();

    /**
     * Retrieve radio codes
     *
     * @return the api call
     */
    @GET("priority/classification")
    Call<List<PriorityClassification>> getPriorityClassification();

    /**
     * Set client
     *
     * @return the api call
     */
    @POST("/en/api/client/")
    Call<Client> setClient(@Body Client client);

    /**
     * Get servers
     *
     * @return the list of available servers
     */
    @GET("https://emstrack.org/servers.json")
    Call<List<String>> getServers();

}
