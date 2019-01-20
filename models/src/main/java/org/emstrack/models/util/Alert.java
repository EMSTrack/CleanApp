package org.emstrack.models.util;

import android.util.Log;

/**
 * Created by mauricio on 3/22/2018.
 */

/**
 * A simple alert class that logs alerts
 */
public class Alert  {

    private String TAG;

    /**
     * Tag defaults to the class name
     */
    public Alert() {
        this.TAG = this.getClass().getName();
    }

    /**
     *
     * @param TAG the alert tag
     */
    public Alert(String TAG) {
        this.TAG = TAG;
    }

    /**
     *
     * @param TAG the alert tag
     */
    public void setTag(String TAG) {
        this.TAG = TAG;
    }

    /**
     *
     * @return the alert tag
     */
    public String getTag() {
        return TAG;
    }

    /**
     * Logs the alert message
     *
     * <p>
     *     Overload to customize alerts.
     * </p>
     * @param message the alert message
     */
    public void alert(String message) {
        
        // Log message
        Log.i(TAG, message);

    }

}