package org.emstrack.ambulance;

import android.util.Log;
import org.json.JSONException;
import org.json.JSONObject;

import static android.content.ContentValues.TAG;

/**
 * Created by sinan on 6/6/2017.
 *
 * This class is meant to create a call object that is sent from the dispatcher. It will contain
 * a lot of information, so having it in its own separate class can save time.
 */

public class DispatcherCall  {
    String callString;
    String address1 = "No Address Received";
    static String latitude = "32.879409";
    static String longitude = "-117.2382162";
    String description = "No description received";

    DispatcherCall(JSONObject c) {
        try {
            //GET EVERY SINGLE PART OF THE JSON OUT THE WAY
            int id = c.getInt("id");
            latitude = c.getString("latitude");
            longitude = c.getString("longitude");
            String name = c.getString("name");
            int resU = c.getInt("residential_unit");
            int stmain_number = c.getInt("stmain_number");
            int delegation = c.getInt("delegation");
            int zipcode = c.getInt("zipcode");
            String city = c.getString("city");
            String state = c.getString("state");
            String assignment = c.getString("assignment");
            description = c.getString("description");
            String call_time = c.getString("call_time");
            String departure_time = c.getString("departure_time");
            String transfer_time = c.getString("transfer_time");
            String hospital_time = c.getString("hospital_time");
            String base_time = c.getString("base_time");
            int amb = c.getInt("ambulance");

            //format to save the address. This is temporary and might be changed based on server team
            address1 = stmain_number + " street name pending " + "#" + resU + " " + city + " " + state + " " + zipcode;

            //LOG EVERYTHING JUST TO MAKE SURE OF THIS
            Log.d(TAG, "Call message received: and address is " + address1);
        }
        catch (JSONException e) { Log.d(TAG, "json error"); }

        //save the call
        callString = c.toString();
    }

    String getAddress() {
        return address1;
    }

    public static String getLong() {
        return longitude;
    }

    public static String getLatitude() {
        return latitude;
    }

}
