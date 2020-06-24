package org.emstrack.models;

import com.google.gson.annotations.Expose;
import com.google.gson.annotations.SerializedName;

/**
 * A class representing an API token login.
 */
public class TokenLogin {

    @SerializedName("url")
    @Expose
    private String url;

    private String token;
    private String username;

    /**
     *
     * @param url
     */
    public TokenLogin(String url) {
        this.url = url;
    }

    public String getUrl() {
        return url;
    }

    public void setUrl(String url) { this.url = url; }

    public String getToken() { return token; }

    public void setToken(String token) { this.token = token; }

    public String getUsername() { return username; }

    public void setUsername() { this.username = username; }

}