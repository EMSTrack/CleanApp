package org.emstrack.models.api;

import org.emstrack.models.Credentials;
import org.emstrack.models.Location;
import org.emstrack.models.Profile;
import org.emstrack.models.Token;

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
    @POST("/auth/token/")
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

}