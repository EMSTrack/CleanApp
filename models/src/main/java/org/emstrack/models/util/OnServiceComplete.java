package org.emstrack.models.util;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Bundle;
import android.os.Handler;
import androidx.localbroadcastmanager.content.LocalBroadcastManager;
import android.util.Log;

/**
 * Safely run and chain services by listening to success and failure actions
 *
 * <p>
 *     The <code>OnServiceComplete</code> class provides a convenient way to safely run services.
 *     It automatically registers to listen to success and failure messages and has a
 *     built in timeout mechanism that prevents the task from running forever in case
 *     the service fails to respond.
 * </p>
 *
 * <p>
 *     Typical usage is as follows:
 * </p>
 *
 * <pre>
 * // create object
 * OnServiceComplete service =
 *     new OnServiceComplete(context,
 *                           successAction, failureAction,
 *                           intent,
 *                           timeout) {
 *     &#64;Override
 *     public void run() {
 *         // code to run before initiating intent
 *     }
 *
 *     &#64;Override
 *     public void onSuccess(Bundle extras) {
 *         // code to run in case the service succeeds
 *     }
 *
 *     &#64;Override
 *     public void onFailure(Bundle extras) {
 *         // code to run in case the service fails
 *     }
 *
 * };
 *
 * // start intent and listen to actions
 * service.start();
 * </pre>
 *
 * <p>
 *     The {@link #start()} method runs the method {@link #run()}, then fires the intent
 *     <code>intent</code> and starts listening to the actions <code>successAction</code> and
 *     <code>failureActions</code>.
 *  </p>
 *
 *  <p>
 *     The method <code>onSuccess</code> will be called in case <code>successAction</code>
 *     is received.
 *  </p>
 *
 *  <p>
 *     The method <code>onFailure</code> will be called in case <code>failureAction</code>
 *     is received or <code>timeout</code> milliseconds have elapsed.
 * </p>
 *
 * <p>
 *     <b>IMPORTANT:</b> The intent <code>intent</code> is passed a unique identifier as
 *     the <code>BroadcastExtras.UUID</code> bundle parameter. This unique identifier should be
 *     added to the intent's success and failure actions. Without the unique identifier,
 *     <code>OnServiceComplete</code> will not work properly.
 * </p>
 *
 * <p>
 *     Services can be chained by using the argument <code>next</code> or {@link #setNext}.
 *     For example:
 * </p>
 *
 * <pre>
 * new OnServiceComplete(
 *     context,
 *     successAction1, failureAction1,
 *     intent1) {
 *
 *         &#64;Override
 *         public void onSuccess(Bundle extras) {
 *             // code to run in case service1 succeeds
 *         }
 *
 * }.setNext(
 *     new OnServiceComplete(
 *         context,
 *         successAction2, failureAction2,
 *         intent2) {
 *
 *           &#64;Override
 *           public void onSuccess(Bundle extras) {
 *              // code to run in case service2 succeeds
 *          }
 *
 *     }
 * )
 * .start();
 * </pre>
 *
 * <p>
 *     will first run <code>intent1</code> and then, if <code>intent1</code> is successful, run
 *     and listen to the actions of <code>intent2</code>.
 * </p>
 *
 * @author mauricio
 * @since 3/21/2018
 */
public abstract class OnServiceComplete extends BroadcastReceiver implements StartableTask<OnServiceComplete> {

    protected final String TAG = OnServiceComplete.class.getSimpleName();
    private static final int DEFAULT_TIMEOUT = 20000; // 20s

    private final Context context;
    private final String successAction;
    private final String failureAction;
    private final String uuid;
    private final int timeout;
    private StartableTask next;
    private final Intent intent;

    private String failureMessage;
    private Alert alert;
    private boolean successFlag;
    private boolean completeFlag;
    private boolean timedOutFlag;
    private boolean startedFlag;
    private boolean successIdCheck;
    private boolean failureIdCheck;

    /**
     * @param context the current context
     * @param successAction the success action to listen to
     * @param failureAction the failure action to listen to
     * @param intent the intent to fire
     * @param timeout timeout in milliseconds
     * @param next the next {@link StartableTask} to execute
     */
    public OnServiceComplete(final Context context,
                             final String successAction,
                             final String failureAction,
                             Intent intent,
                             int timeout,
                             StartableTask next) {

        // context and next
        this.context = context;
        this.next = next;
        this.timeout = timeout;
        this.intent = intent;
        this.successIdCheck = true;
        this.failureIdCheck = true;

        // uuid
        this.uuid = java.util.UUID.randomUUID().toString();

        // actions
        this.successAction = successAction;
        this.failureAction = failureAction;

        // success and complete flags
        this.successFlag = false;
        this.completeFlag = false;
        this.startedFlag = false;
        this.timedOutFlag = false;

        // Register actions for broadcasting
        IntentFilter successIntentFilter = new IntentFilter(successAction);
        getLocalBroadcastManager(context).registerReceiver(this, successIntentFilter);

        IntentFilter failureIntentFilter = new IntentFilter(failureAction);
        getLocalBroadcastManager(context).registerReceiver(this, failureIntentFilter);

        // Default alert is AlertLog
        this.alert = new Alert(TAG);

        // Default failure message
        this.failureMessage = "Failed to complete service request";

    }

    /**
     * Defaults to no next <code>OnServiceComplete</code>
     *
     * @param context the current context
     * @param successAction the success action to listen to
     * @param failureAction the failure action to listen to
     * @param intent the intent to fire
     * @param timeout timeout in milliseconds
     */
    public OnServiceComplete(final Context context,
                             final String successAction,
                             final String failureAction,
                             Intent intent,
                             int timeout) {
        this(context, successAction, failureAction, intent, timeout, null);
    }

    /**
     * Defaults to 10 seconds timeout
     *
     * @param context the current context
     * @param successAction the success action to listen to
     * @param failureAction the failure action to listen to
     * @param intent the intent to fire
     * @param next the next {@link StartableTask} to execute
     */
    public OnServiceComplete(final Context context,
                             final String successAction,
                             final String failureAction,
                             Intent intent,
                             StartableTask next) {
        this(context, successAction, failureAction, intent, DEFAULT_TIMEOUT, next);
    }

    /**
     * Defaults to no next <code>OnServiceComplete</code> and 10 seconds timeout
     *
     * @param context the current context
     * @param successAction the success action to listen to
     * @param failureAction the failure action to listen to
     * @param intent the intent to fire
      */
    public OnServiceComplete(final Context context,
                             final String successAction,
                             final String failureAction,
                             Intent intent) {

        this(context, successAction, failureAction, intent, DEFAULT_TIMEOUT);
    }

    /**
     * Run {@link #run()} and start the intent
     *
     * @return this object
     */
    @Override
    public OnServiceComplete start() {

        // Run
        this.run();

        // Start service
        if (this.intent != null) {

            // Start service
            this.intent.putExtra(BroadcastExtras.UUID, this.uuid);
            this.context.startService(this.intent);

        }

        // Start timeout timer
        new Handler().postDelayed(() -> {

            // unregister to prevent memory leak
            unregister(this.context);

            if (!isComplete()) {

                Log.e(TAG, "TIMEOUT");
                this.successFlag = false;
                this.timedOutFlag = true;

                // Make bundle
                Bundle bundle = new Bundle();
                bundle.putString(BroadcastExtras.UUID, uuid);
                bundle.putString(org.emstrack.models.util.BroadcastExtras.MESSAGE,
                        String.format("Timed out without completing service in %d seconds.", this.timeout/1000));

                // Call failure
                onFailure(bundle);

            }

        }, this.timeout);

        // set started
        this.startedFlag = true;

        // return this
        return this;
    }

    /**
     * Set next {@link StartableTask} to execute after a successful action is received
     *
     * <p>
     *     <b>IMPORTANT:</b> The next {@link StartableTask} is not executed if a failure action is received.
     * </p>
     *
     * @param next the next {@link StartableTask}
     * @return this object
     */
    @Override
    public OnServiceComplete setNext(StartableTask next) {
        this.next = next;
        return this;
    }

    /**
     * If true discard success actions that do not match unique id
     *
     * @param value <code>true</code> or <code>false</code>
     * @return this object
     */
    public OnServiceComplete setSuccessIdCheck(boolean value) {
        this.successIdCheck = value;
        return this;
    }

    /**
     *
     * @return whether will check for success action id
     */
    public boolean isSuccessIdCheck() {
        return failureIdCheck;
    }

    /**
     * If true discard actions that do not match unique id
     *
     * @param value <code>true</code> or <code>false</code>
     * @return this object
     */
    public OnServiceComplete setFailureIdCheck(boolean value) {
        this.failureIdCheck = value;
        return this;
    }

    /**
     *
     * @return whether will check for failure action id
     */
    public boolean isFailureIdCheck() {
        return failureIdCheck;
    }

    /**
     * Get unique identifier
     *
     * @return the unique identifier
     */
    public String getUuid() { return uuid; }

    /**
     * Return whether the service was successful
     *
     * @return <code>True</code> if successful
     */
    public boolean isSuccess() {
        return successFlag;
    }

    /**
     * Set success flag
     *
     * @param successFlag <code>True</code> if successful
     */
    public void setSuccess(boolean successFlag) {
        this.successFlag = successFlag;
    }

    /**
     * Return whether the service was completed (as opposed to aborted or still pending)
     *
     * @return <code>True</code> if completed
     */
    public boolean isComplete() {
        return completeFlag;
    }

    /**
     * Return whether the service timed out
     *
     * @return <code>True</code> if timed out
     */
    public boolean isTimedOut() {
        return timedOutFlag;
    }

    /**
     * Return whether the service has started
     *
     * @return <code>True</code> if started
     */
    @Override
    public boolean isStarted() {
        return this.startedFlag;
    }

    /**
     * Set alert mechanism
     *
     * @param alert the alert mechanism
     * @return this object
     */
    public OnServiceComplete setAlert(Alert alert) {
        this.alert = alert;
        return this;
    }

    /**
     * Set the failure message
     * @param failureMessage the failure message
     * @return this object
     */
    public OnServiceComplete setFailureMessage(String failureMessage) {
        this.failureMessage = failureMessage;
        return this;
    }

    /**
     * The method in charge of responding to message arrivals. It will ignore actions that
     * do not match the object's unique identifier.
     *
     * @param context the current context
     * @param intent the intent with the action
     */
    @Override
    public void onReceive(Context context, Intent intent) {

        // Log.d(TAG, "onReceive");

        // quick return if no intent
        if (intent == null)
            return;

        // quick return if no action
        final String action = intent.getAction();
        if (action == null)
            return;

        // success or failure?
        boolean isSuccess;
        boolean checkId;
        if (action.equals(successAction)) {
            isSuccess = true;
            checkId = this.successIdCheck;
        } else if (action.equals(failureAction)) {
            isSuccess = false;
            checkId = this.failureIdCheck;
        } else {
            // This should never happen
            Log.e(TAG, "Unknown action '" + action + "'");
            return;
        }

        // retrieve uuid
        String uuid = intent.getStringExtra(BroadcastExtras.UUID);
        // Log.d(TAG, "uuid = " + uuid);

        // quick return if not same uuid
        if (checkId && !this.uuid.equals(uuid))
            return;

        // complete
        this.completeFlag = true;

        // unregister first
        unregister(context);

        // Process actions
        this.successFlag = isSuccess;
        if (this.successFlag) {

            // Log.i(TAG, "SUCCESS");
            onSuccess(intent.getExtras());

            // has next
            if (this.next != null && this.successFlag)
                this.next.start();

        } else {

            // Log.i(TAG, "FAILURE");
            onFailure(intent.getExtras());

        }

    }

    /**
     * Run before firing the intent
     *
     * Access the unique identifier using {@link #getUuid()}.
     */
    public void run() { }

    /**
     * Called if the intent succeeds
     *
     * @param extras the intent bundle
     */
    public abstract void onSuccess(Bundle extras);

    /**
     * Called if the intent fails
     *
     * @param extras the intent bundle
     */
    public void onFailure(Bundle extras) {

        // Alert user
        String message = failureMessage;
        if (extras != null) {
            String msg = extras.getString(org.emstrack.models.util.BroadcastExtras.MESSAGE);
            if (msg != null)
                if (message != null)
                    message +=  '\n' + msg;
                else
                    message =  msg;

        }

        // Alert user
        alert.alert(message);

    }

    /**
     * Unregister BroadcastReceiver
     *
     * @param context the current context
     */
    private void unregister(Context context) {
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
