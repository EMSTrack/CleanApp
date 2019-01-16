package org.emstrack.models;

import retrofit2.http.Body;
import retrofit2.http.POST;
import retrofit2.Call;

public interface UserService {

    @POST("auth/token/")
    Call<Token> getToken(@Body Credentials credentials);

}
