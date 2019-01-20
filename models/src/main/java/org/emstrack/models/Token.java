package org.emstrack.models;

import com.google.gson.annotations.Expose;
import com.google.gson.annotations.SerializedName;

/**
 * A class representing an API token.
 */
public class Token {

    @SerializedName("token")
    @Expose
    private String token;


    /**
     *
     * @param token
     */
    public Token(String token) {
        this.token = token;
    }

    public String getToken() {
        return token;
    }

    public void setToken(String token) {
        this.token = token;
    }

}