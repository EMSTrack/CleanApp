package org.emstrack.models.api;

import org.emstrack.models.Ambulance;
import org.emstrack.models.AmbulanceNote;
import org.emstrack.models.CallNote;
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
import org.emstrack.models.TokenLogin;
import org.emstrack.models.Version;
import org.emstrack.models.Waypoint;

import java.util.List;

import retrofit2.http.Body;
import retrofit2.http.GET;
import retrofit2.http.Header;
import retrofit2.http.Headers;
import retrofit2.http.PATCH;
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
     * IMPORTANT: Add language to POST request
     *
     * @param credentials the user credentials
     * @return the api call
     */
    @POST("/en/auth/token/")
    Call<Token> getToken(@Body Credentials credentials);

    /**
     * Retrieve token login
     *
     * IMPORTANT: Add language to POST request
     *
     * @param tokenLogin
     * @return the api call
     */
    @POST("/en/api/user/{username}/tokenlogin/")
    Call<TokenLogin> getTokenLogin(@Path("username") String username, @Body TokenLogin tokenLogin);

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
     * Retrieve the online clients
     *
     * @return the api call
     */
    @GET("client/")
    Call<List<Client>> getOnlineClients();

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
     * Retrieve ambulance's notes
     *
     * @return the api call
     */
    @GET("ambulance/{id}/note/")
    Call<List<AmbulanceNote>> getAmbulanceNote(@Path("id") int id);

    /**
     * Retrieve hospital's equipment
     *
     * @return the equipment
     */
    @GET("hospital/{id}/equipment/")
    Call<List<org.emstrack.models.EquipmentItem>> getHospitalEquipment(@Path("id") int id);

    /**
     * Retrieve call
     *
     * @return the api call
     */
    @GET("call/{id}/")
    Call<org.emstrack.models.Call> getCall(@Path("id") int id);

    /**
     * Retrieve call notes
     *
     * @return the api call
     */
    @GET("call/{id}/note/")
    Call<List<CallNote>> getCallNote(@Path("id") int id);

    /**
     * Add call note
     *
     * IMPORTANT: Add language to POST request
     *
     * @return the api call
     */
    @POST("/en/api/call/{id}/note/")
    Call<CallNote> addCallNote(@Path("id") int id, @Body CallNote callNote);

    /**
     * Create call waypoint
     *
     * IMPORTANT: Add language to PATCH request
     *
     * @return the api call
     */
    @Headers("Content-Type: application/json")
    @POST("/en/api/call/{callId}/ambulance/{ambulanceId}/waypoint/")
    Call<Waypoint> postCallWaypoint(@Path("callId") int callId, @Path("ambulanceId") int ambulanceId, @Body String waypoint);

    /**
     * Patch call waypoint
     *
     * IMPORTANT: Add language to PATCH request
     *
     * @return the api call
     */
    @PATCH("/en/api/call/{callId}/ambulance/{ambulanceId}/waypoint/")
    Call<Waypoint> patchCallWaypoint(@Path("callId") int callId, @Path("ambulanceId") int ambulanceId, @Body Waypoint waypoint);

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
    @GET("priority/classification/")
    Call<List<PriorityClassification>> getPriorityClassification();

    /**
     * Set client
     *
     * IMPORTANT: Add language to POST request
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
