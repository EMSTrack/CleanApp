package org.emstrack.ambulance.services;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Bundle;
import android.os.Handler;
import android.support.v4.content.LocalBroadcastManager;
import android.util.Log;

import org.emstrack.ambulance.dialogs.AlertSnackbar;

/**
 * Created by mauricio on 3/21/2018.
 */

public abstract class OnServicesComplete extends BroadcastReceiver {

    public static final String UUID = "_UUID_";

    private final String TAG = OnServicesComplete.class.getSimpleName();

    private final String[] successActions;
    private final String[] failureActions;
    private final String uuid;
    private String failureMessage;
    private AlertSnackbar alert;

    private boolean successFlag;
    private boolean completeFlag;

    public OnServicesComplete(final Context context,
                              final String[] successActions,
                              final String[] failureActions,
                              Intent intent,
                              int timeout) {

        // uuid
        this.uuid = java.util.UUID.randomUUID().toString();

        // actions
        this.successActions = successActions;
        this.failureActions = failureActions;

        // success and complete flags
        this.successFlag = false;
        this.completeFlag = false;

        // Register actions for broadcasting
        IntentFilter intentFilter = new IntentFilter();
        for (String action : successActions)
            intentFilter.addAction(action);
        for (String action: failureActions)
            intentFilter.addAction(action);
        getLocalBroadcastManager(context).registerReceiver(this, intentFilter);

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

        // Start timeout timer
        new Handler().postDelayed(new Runnable() {
            @Override
            public void run() {

                if (!isComplete()) {

                    for (String action: failureActions) {

                        // Broadcast failures
                        Intent localIntent = new Intent(action);
                        localIntent.putExtra(OnServicesComplete.UUID, uuid);
                        localIntent.putExtra(AmbulanceForegroundService.BroadcastExtras.MESSAGE,
                                "Timed out without completing service.");
                        context.sendBroadcast(localIntent);

                    }

                }

                // otherwise die graciously

            }
        }, timeout);

    }

    public OnServicesComplete(final Context context,
                              final String[] successActions,
                              final String[] failureActions,
                              Intent intent) {

        this(context, successActions, failureActions, intent, 10000);

    }

    public String getUuid() { return uuid; }

    public boolean isSuccess() {
        return successFlag;
    }

    public boolean isComplete() {
        return completeFlag;
    }

    public OnServicesComplete setAlert(AlertSnackbar alert) {
        this.alert = alert;
        return this;
    }

    public OnServicesComplete setFailureMessage(String failureMessage) {
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

        // Retrieve action
        final String action = intent.getAction();

        // Process success actions
        boolean match = false;
        for (String _action: successActions) {

            if (action.equals(_action)) {

                Log.i(TAG, "SUCCESS");
                this.successFlag = true;
                onSuccess(intent.getExtras());

                match = true;
                break;
            }

        }

        // Process failure actions
        if (!match)
            for (String _action: failureActions) {

                if (action.equals(_action)) {

                    Log.i(TAG, "FAILURE");
                    this.successFlag = false;
                    onFailure(intent.getExtras());

                    match = true;
                    break;

                }

            }

        if (match)

            // complete
            this.completeFlag = true;

        else

            Log.i(TAG, "Unknown action '" + action + "'");

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
