package org.emstrack.mqtt;

import com.google.gson.annotations.Expose;
import com.google.gson.annotations.SerializedName;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

/**
 * A class representing an API token.
 */
public class Client {

    public static final String STATUS_ONLINE = "O";
    public static final String STATUS_OFFLINE = "F";
    public static final String STATUS_DISCONNECTED = "D";
    public static final String STATUS_RECONNECTED = "R";

    public static final Map<String, String> statusLabel;
    static {

        Map<String, String> map = new HashMap<>();

        map.put(STATUS_ONLINE, "Online");
        map.put(STATUS_OFFLINE, "Offline");
        map.put(STATUS_DISCONNECTED, "Disconnected");
        map.put(STATUS_RECONNECTED, "Reconnected");

        statusLabel = Collections.unmodifiableMap(map);
    }

}