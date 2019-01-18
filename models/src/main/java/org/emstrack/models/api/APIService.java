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

public interface APIService {

    @POST("/auth/token/")
    Call<Token> getToken(@Body Credentials credentials);

    @GET("user/{username}/profile/")
    Call<Profile> getProfile(@Path("username") String username);

    @GET("location/{type}/")
    Call<List<Location>> getLocationsByType(@Path("type") String type);

    @GET("location/")
    Call<List<Location>> getLocations();

}
