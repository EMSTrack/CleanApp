package org.emstrack.ambulance.services;

import android.app.IntentService;
import android.content.Intent;
import android.support.annotation.Nullable;
import android.util.Log;

import com.google.android.gms.location.Geofence;
import com.google.android.gms.location.GeofencingEvent;

/**
 * Created by mauricio on 4/23/18.
 */

public class GeofenceTransitionsIntentService extends IntentService {

    static final String TAG = GeofenceTransitionsIntentService.class.getSimpleName();

    /**
     * Creates an IntentService.  Invoked by your subclass's constructor.
     *
     * @param name Used to name the worker thread, important only for debugging.
     */
    public GeofenceTransitionsIntentService(String name) {
        super(name);
    }

    @Override
    protected void onHandleIntent(@Nullable Intent intent) {

        Log.i(TAG,"GEOFENCE_TRANS_SERV: Connected to Geofence Transitions Intent Service");
        GeofencingEvent geofencingEvent = GeofencingEvent.fromIntent(intent);

        if (geofencingEvent.hasError()) {
            //String errorMessage = GeofenceErrorMessages.getErrorString(this,
             //       geofencingEvent.getErrorCode());
            //Log.e(TAG, errorMessage);
            Log.d(TAG, "Geofencing Error");
            return;
        }

        // Get the transition type.
        int geofenceTransition = geofencingEvent.getGeofenceTransition();

        if (geofenceTransition == Geofence.GEOFENCE_TRANSITION_ENTER) {
            Log.i(TAG, "GEOFENCE_TRIGGERED: ENTER");
        } else if (geofenceTransition == Geofence.GEOFENCE_TRANSITION_EXIT) {
            Log.i(TAG, "GEOFENCE_TRIGGERED: EXIT");
        } else {
            String message = "Did not pass through Geofence\n" + String.valueOf(geofenceTransition);
            Log.i("GEOFENCE_OTHER", message);
        }
    }

}
