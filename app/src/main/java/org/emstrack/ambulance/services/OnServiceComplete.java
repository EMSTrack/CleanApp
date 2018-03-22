package org.emstrack.ambulance.services;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Bundle;
import android.support.v4.content.LocalBroadcastManager;
import android.util.Log;

/**
 * Created by mauricio on 3/21/2018.
 */

public abstract class OnServiceComplete extends BroadcastReceiver {

    final private String TAG = OnServiceComplete.class.getSimpleName();

    private final String successAction;
    private final String failureAction;

    private boolean oneShot;
    private boolean successFlag;
    private boolean completeFlag;

    public OnServiceComplete(Context context, String successAction, String failureAction) {
        this(context, successAction, failureAction, true);
    }

    public OnServiceComplete(Context context, String successAction, String failureAction, boolean oneShot) {

        // one shot?
        this.oneShot = oneShot;

        // actions
        this.successAction = successAction;
        this.failureAction = failureAction;

        // success and complete flags
        this.successFlag = false;
        this.completeFlag = false;

        Log.d(TAG,"Registering receivers");

        // Register actions for broadcasting
        IntentFilter successIntentFilter = new IntentFilter(successAction);
        getLocalBroadcastManager(context).registerReceiver(this, successIntentFilter);

        IntentFilter failureIntentFilter = new IntentFilter(failureAction);
        getLocalBroadcastManager(context).registerReceiver(this, failureIntentFilter);

    }

    public void onSuccess(Bundle extras) {
        this.completeFlag = true;
        this.successFlag = true;
    };

    public void onFailure(Bundle extras) {
        this.completeFlag = true;
        this.successFlag = false;
    };

    public boolean isSuccess() {
        return successFlag;
    }

    public boolean isComplete() {
        return completeFlag;
    }

    @Override
    public void onReceive(Context context, Intent intent) {
        if (intent != null) {

            // Unregister?
            if (oneShot)
                unregister(context);

            // Process actions
            final String action = intent.getAction();
            if (action.equals(successAction)) {

                Log.i(TAG, "SUCCESS");
                onSuccess(intent.getExtras());

            } else if (action.equals(failureAction)) {

                Log.i(TAG, "FAILURE");
                onFailure(intent.getExtras());

            } else
                Log.i(TAG, "Unknown action '" + action + "'");

        }
    }

    public void unregister(Context context) {
        Log.d(TAG,"Unregistering receivers");
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
