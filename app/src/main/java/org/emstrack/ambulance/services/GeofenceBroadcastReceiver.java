package org.emstrack.ambulance.services;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.support.v4.content.LocalBroadcastManager;
import android.util.Log;

import com.google.android.gms.location.Geofence;
import com.google.android.gms.location.GeofencingEvent;

import org.emstrack.models.Ambulance;

public class GeofenceBroadcastReceiver extends BroadcastReceiver {

    final String TAG = GeofenceBroadcastReceiver.class.getSimpleName();

    /**
     * Receives incoming intents.
     *
     * @param context the application context.
     * @param intent  sent by Location Services. This Intent is provided to Location
     *                Services (inside a PendingIntent) when addGeofences() is called.
     */
    @Override
    public void onReceive(Context context, Intent intent) {
        // Enqueues a JobIntentService passing the context and intent as parameters
        Log.d(TAG, "Got broadcast");

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
            Log.i(TAG, "GEOFENCE_TRIGGERED: UNKNOWN EVENT " + String.valueOf(geofenceTransition));
        }

        // Broadcast event
        Intent localIntent = new Intent(AmbulanceForegroundService.BroadcastActions.GEOFENCE_EVENT);
        localIntent.putExtra(AmbulanceForegroundService.BroadcastExtras.GEOFENCE_TRANSITION,
                geofenceTransition);
        getLocalBroadcastManager(context).sendBroadcast(localIntent);

    }

    /**
     * Get the LocalBroadcastManager
     *
     * @return The system LocalBroadcastManager
     */
    private LocalBroadcastManager getLocalBroadcastManager(Context context) {
        return LocalBroadcastManager.getInstance(context);
    }

}
