package org.emstrack.models.util;

import android.util.Log;

/**
 * Created by mauricio on 3/22/2018.
 */

public class Alert  {

    private static String TAG = Alert.class.getSimpleName();

    public Alert() {

    }

    public Alert(String TAG) {
        this.TAG = TAG;
    }
    
    public void setTAG(String TAG) {
        this.TAG = TAG;
    }

    public void alert(String message) {
        
        // Log message
        Log.d(TAG, message);

    }

}