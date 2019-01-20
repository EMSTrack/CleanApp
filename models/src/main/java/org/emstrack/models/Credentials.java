package org.emstrack.models;

import com.google.gson.annotations.Expose;
import com.google.gson.annotations.SerializedName;

/**
 * A class representing user's credentials.
 */
public class Credentials {

    @SerializedName("username")
    @Expose
    private String username;
    @SerializedName("password")
    @Expose
    private String password;
    private String serverURI;

    /**
     *
     * @param username
     * @param password
     */
    public Credentials(String username, String password, String serverURI) {
        this.username = username;
        this.password = password;
        this.serverURI = serverURI;
    }

    public void setPassword(String password) {
        this.password = password;
    }

    public String getPassword() {
        return password;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public String getUsername() {
        return username;
    }

    public void setServerURI(String serverURI) {
        this.serverURI = serverURI;
    }

    public String getServerURI() {
        return serverURI;
    }
}