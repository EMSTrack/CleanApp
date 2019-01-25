package org.emstrack.ambulance.services;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.support.v4.content.LocalBroadcastManager;
import android.util.Log;

import com.google.android.gms.location.Geofence;
import com.google.android.gms.location.GeofencingEvent;

import org.eclipse.paho.client.mqttv3.MqttException;
import org.emstrack.ambulance.R;
import org.emstrack.ambulance.fragments.AmbulanceFragment;
import org.emstrack.models.Ambulance;
import org.emstrack.mqtt.MqttProfileClient;

import java.util.ArrayList;
import java.util.List;

public class GeofenceBroadcastReceiver extends BroadcastReceiver {

    final String TAG = GeofenceBroadcastReceiver.class.getSimpleName();

    /**
     * Receives incoming intents.
     *
     * @param context the application context.
     * @param intent  sent by GPSLocation Services. This Intent is provided to GPSLocation
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

        List<Geofence> triggeringGeofences = geofencingEvent.getTriggeringGeofences();
        List<String> geofenceIds = new ArrayList<String>();

        if (triggeringGeofences != null) {
            for (Geofence geofence : triggeringGeofences) {

                String geoId = geofence.getRequestId();
                geofenceIds.add(geoId);

                Log.i(TAG, "TRIGGERED GEOFENCE: " + geoId);
            }
        }

        String[] triggeredGeofences = geofenceIds.toArray(new String[geofenceIds.size()]);

        if (geofenceTransition == Geofence.GEOFENCE_TRANSITION_ENTER || geofenceTransition == Geofence.GEOFENCE_TRANSITION_EXIT) {
            if (geofenceTransition == Geofence.GEOFENCE_TRANSITION_ENTER) {

                // broadcast change on ambulance status when it enters geofence
                Intent localIntent = new Intent(context, AmbulanceForegroundService.class);
                localIntent.setAction(AmbulanceForegroundService.Actions.GEOFENCE_ENTER);
                localIntent.putExtra(AmbulanceForegroundService.BroadcastExtras.GEOFENCE_TRIGGERED, triggeredGeofences);
                context.startService(localIntent);

                Log.i(TAG, "GEOFENCE_TRIGGERED: ENTER");
            } else if (geofenceTransition == Geofence.GEOFENCE_TRANSITION_EXIT) {

                // broadcast change on ambulance status when it exits geofence
                Intent localIntent = new Intent(context, AmbulanceForegroundService.class);
                localIntent.setAction(AmbulanceForegroundService.Actions.GEOFENCE_EXIT);
                localIntent.putExtra(AmbulanceForegroundService.BroadcastExtras.GEOFENCE_TRIGGERED, triggeredGeofences);
                context.startService(localIntent);

                Log.i(TAG, "GEOFENCE_TRIGGERED: EXIT");
            }

            // Broadcast event
            Intent localIntent = new Intent(AmbulanceForegroundService.BroadcastActions.GEOFENCE_EVENT);
            localIntent.putExtra(AmbulanceForegroundService.BroadcastExtras.GEOFENCE_TRANSITION,
                    geofenceTransition);
            getLocalBroadcastManager(context).sendBroadcast(localIntent);

        } else {
            Log.i(TAG, "GEOFENCE_TRIGGERED: UNKNOWN EVENT " + String.valueOf(geofenceTransition));
        }

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
