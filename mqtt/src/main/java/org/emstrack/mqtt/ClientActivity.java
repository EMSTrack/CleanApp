package org.emstrack.mqtt;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

/**
 * A class representing an API token.
 */
public class ClientActivity {

    public static final String ACTIVITY_HANDSHAKE = "HS";
    public static final String ACTIVITY_AMBULANCE_LOGIN = "AI";
    public static final String ACTIVITY_AMBULANCE_LOGOUT = "AO";
    public static final String ACTIVITY_AMBULANCE_STOP_LOCATION = "TL";
    public static final String ACTIVITY_AMBULANCE_START_LOCATION = "SL";
    public static final String ACTIVITY_HOSPITAL_LOGIN = "HI";
    public static final String ACTIVITY_HOSPITAL_LOGOUT = "HO";

    public static final Map<String, String> activityLabel;
    static {

        Map<String, String> map = new HashMap<>();

        map.put(ACTIVITY_HANDSHAKE, "handshake");
        map.put(ACTIVITY_AMBULANCE_LOGIN, "ambulance login");
        map.put(ACTIVITY_AMBULANCE_LOGOUT, "ambulance logout");
        map.put(ACTIVITY_AMBULANCE_STOP_LOCATION, "ambulance stop location");
        map.put(ACTIVITY_AMBULANCE_START_LOCATION, "ambulance start location");
        map.put(ACTIVITY_HOSPITAL_LOGIN, "hospital login");
        map.put(ACTIVITY_HOSPITAL_LOGOUT, "hospital logout");

        activityLabel = Collections.unmodifiableMap(map);
    }

}