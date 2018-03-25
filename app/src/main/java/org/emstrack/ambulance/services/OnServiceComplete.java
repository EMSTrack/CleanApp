package org.emstrack.ambulance.services;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Bundle;
import android.support.v4.content.LocalBroadcastManager;
import android.util.Log;

import org.emstrack.ambulance.dialogs.AlertSnackbar;

/**
 * Created by mauricio on 3/21/2018.
 */

public abstract class OnServiceComplete extends BroadcastReceiver {

    public static final String UUID = "_UUID_";

    private final String TAG = OnServiceComplete.class.getSimpleName();

    private final String successAction;
    private final String failureAction;
    private final String uuid;
    private String failureMessage;
    private AlertSnackbar alert;

    private boolean successFlag;
    private boolean completeFlag;

    public OnServiceComplete(Context context,
                             String successAction,
                             String failureAction,
                             Intent intent) {

        // uuid
        this.uuid = java.util.UUID.randomUUID().toString();

        // actions
        this.successAction = successAction;
        this.failureAction = failureAction;

        // success and complete flags
        this.successFlag = false;
        this.completeFlag = false;

        // Register actions for broadcasting
        IntentFilter successIntentFilter = new IntentFilter(successAction);
        getLocalBroadcastManager(context).registerReceiver(this, successIntentFilter);

        IntentFilter failureIntentFilter = new IntentFilter(failureAction);
        getLocalBroadcastManager(context).registerReceiver(this, failureIntentFilter);

        // Default alert is AlertLog
        this.alert = new AlertSnackbar(TAG);

        // Defaiult failure message
        this.failureMessage = "Failed to complete service request";

        // Run
        this.run();

        // Start service
        if (intent != null) {

            // Start service
            intent.putExtra(UUID, this.uuid);
            context.startService(intent);

        }

    }

    public String getUuid() { return uuid; }

    public boolean isSuccess() {
        return successFlag;
    }

    public boolean isComplete() {
        return completeFlag;
    }

    public OnServiceComplete setAlert(AlertSnackbar alert) {
        this.alert = alert;
        return this;
    }

    public OnServiceComplete setFailureMessage(String failureMessage) {
        this.failureMessage = failureMessage;
        return this;
    }

    @Override
    public void onReceive(Context context, Intent intent) {

        // quick return if no intent
        if (intent == null)
            return;

        // get uuid
        String uuid = intent.getStringExtra(UUID);

        // quick return if not same uuid
        if (!this.uuid.equals(uuid))
            return;


        // unregister first
        unregister(context);

        // Process actions
        final String action = intent.getAction();
        if (action.equals(successAction)) {

            Log.i(TAG, "SUCCESS");
            this.successFlag = true;
            onSuccess(intent.getExtras());

        } else if (action.equals(failureAction)) {

            Log.i(TAG, "FAILURE");
            this.successFlag = false;
            onFailure(intent.getExtras());

        } else
            Log.i(TAG, "Unknown action '" + action + "'");

        // complete
        this.completeFlag = true;

    }

    public void run() { }

    public abstract void onSuccess(Bundle extras);

    public void onFailure(Bundle extras) {

        // Alert user
        String message = failureMessage;
        if (extras != null) {
            String msg = extras.getString(AmbulanceForegroundService.BroadcastExtras.MESSAGE);
            if (msg != null)
               message +=  '\n' + msg;

        }

        // Alert user
        alert.alert(message);

    }

    public void unregister(Context context) {
        getLocalBroadcastManager(context).unregisterReceiver(this);
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
