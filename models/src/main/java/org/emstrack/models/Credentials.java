package org.emstrack.models;

import com.google.gson.annotations.Expose;
import com.google.gson.annotations.SerializedName;

import org.emstrack.models.gson.Exclude;

/**
 * A class representing user's credentials.
 *
 * @author mauricio
 * @since 01/19/2019
 */
public class Credentials {

    @SerializedName("username")
    @Expose
    private String username;
    @SerializedName("password")
    @Expose
    private String password;
    @Exclude
    private String apiServerUri;
    @Exclude
    private String mqttServerUri;

    /**
     *
     * @param username the username
     * @param password the password
     * @param apiServerUri the api server uri
     * @param mqttServerUri the mqtt server uri
     */
    public Credentials(String username, String password, String apiServerUri, String mqttServerUri) {
        this.username = username;
        this.password = password;
        this.apiServerUri = apiServerUri;
        this.mqttServerUri = mqttServerUri;
    }

    /**
     *
     * @param password the password
     */
    public void setPassword(String password) {
        this.password = password;
    }

    /**
     *
     * @return the password
     */
    public String getPassword() {
        return password;
    }

    /**
     *
     * @param username the username
     */
    public void setUsername(String username) {
        this.username = username;
    }

    /**
     *
     * @return the username
     */
    public String getUsername() {
        return username;
    }

    /**
     *
     * @param apiServerUri the api server uri
     */
    public void setApiServerUri(String apiServerUri) {
        this.apiServerUri = apiServerUri;
    }

    /**
     *
     * @return the api server uri
     */
    public String getApiServerUri() {
        return apiServerUri;
    }

    /**
     *
     * @param mqttServerUri the mqtt server uri
     */
    public void setMqttServerUri(String mqttServerUri) {
        this.mqttServerUri = mqttServerUri;
    }

    /**
     *
     * @return the mqtt server uri
     */
    public String getMqttServerUri() {
        return mqttServerUri;
    }
}