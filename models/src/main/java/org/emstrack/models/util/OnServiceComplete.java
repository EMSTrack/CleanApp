package org.emstrack.models.util;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Bundle;
import android.os.Handler;
import android.support.v4.content.LocalBroadcastManager;
import android.util.Log;

/**
 * Created by mauricio on 3/21/2018.
 */

public abstract class OnServiceComplete extends BroadcastReceiver {

    private final String TAG = OnServiceComplete.class.getSimpleName();

    private final String successAction;
    private final String failureAction;
    private final String uuid;
    private String failureMessage;
    private Alert alert;

    private boolean successFlag;
    private boolean completeFlag;

    public OnServiceComplete(final Context context,
                             final String successAction,
                             final String failureAction,
                             Intent intent,
                             int timeout) {

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
        this.alert = new Alert(TAG);

        // Default failure message
        this.failureMessage = "Failed to complete service request";

        // Run
        this.run();

        // Run with uuid
        this.run(this.uuid);

        // Start service
        if (intent != null) {

            // Start service
            intent.putExtra(BroadcastExtras.UUID, this.uuid);
            context.startService(intent);

        }

        // Start timeout timer
        new Handler().postDelayed(() -> {

            if (!isComplete()) {

                Log.i(TAG, "TIMEOUT FAILURE");
                this.successFlag = false;

                // Make bundle
                Bundle bundle = new Bundle();
                bundle.putString(BroadcastExtras.UUID, uuid);
                bundle.putString(BroadcastExtras.MESSAGE,
                        "Timed out without completing service.");

                // Call failure
                onFailure(bundle);

            }

        }, timeout);

    }

    public OnServiceComplete(final Context context,
                             final String successAction,
                             final String failureAction,
                             Intent intent) {

        this(context, successAction, failureAction, intent, 10000);

    }

    public String getUuid() { return uuid; }

    public boolean isSuccess() {
        return successFlag;
    }

    public boolean isComplete() {
        return completeFlag;
    }

    public OnServiceComplete setAlert(Alert alert) {
        this.alert = alert;
        return this;
    }

    public OnServiceComplete setFailureMessage(String failureMessage) {
        this.failureMessage = failureMessage;
        return this;
    }

    @Override
    public void onReceive(Context context, Intent intent) {

        Log.d(TAG, "onReceive");

        // quick return if no intent
        if (intent == null)
            return;

        // retrieve uuid
        String uuid = intent.getStringExtra(BroadcastExtras.UUID);

        Log.d(TAG, "uuid = " + uuid);

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

    public void run(String uuid) { }

    public abstract void onSuccess(Bundle extras);

    public void onFailure(Bundle extras) {

        // Alert user
        String message = failureMessage;
        if (extras != null) {
            String msg = extras.getString(BroadcastExtras.MESSAGE);
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
