package org.emstrack.models.api;

import org.emstrack.models.util.Alert;

import retrofit2.Call;
import retrofit2.Response;

/**
 * Created by mauricio on 1/16/2019.
 */

public abstract class OnAPICallComplete<T> {

    public static class Exception extends java.lang.Exception {
        public Exception(String message) {
            super(message);
        }
    }

    private final String TAG = OnAPICallComplete.class.getSimpleName();

    private String failureMessage;
    private Alert alert;

    private boolean successFlag;
    private boolean completeFlag;
    private boolean startedFlag;

    private Call<T> call;
    private OnAPICallComplete next;

    public OnAPICallComplete(Call<T> call) {
        this(call, null);
    }

    public OnAPICallComplete(Call<T> call, OnAPICallComplete next) {

        // success and complete flags
        this.successFlag = false;
        this.completeFlag = false;
        this.startedFlag = false;

        // Default alert is AlertLog
        this.alert = new Alert(TAG);

        // Default failure message
        this.failureMessage = "Failed to complete api call";

        // Set call
        this.call = call;

        // Set next
        this.next = next;
    }

    public void start() {

        // Start call
        this.call.enqueue(new retrofit2.Callback<T>() {
            @Override
            public void onResponse(retrofit2.Call<T> call, Response<T> response) {
                completeFlag = true;
                if (response.isSuccessful()) {
                    // Log.i(TAG, "SUCCESS");
                    successFlag = true;
                    T t = response.body();
                    onSuccess(t);
                    // has next
                    if (next != null)
                        next.start();
                } else {
                    // Log.i(TAG, "FAILURE");
                    successFlag = false;
                    OnAPICallComplete.this.onFailure(new Exception("Response not successful."));
                }
            }

            @Override
            public void onFailure(retrofit2.Call<T> call, Throwable t) {
                // Log.i(TAG, "FAILURE");
                completeFlag = true;
                successFlag = false;
                OnAPICallComplete.this.onFailure(t);
            }

        });

        // set started
        this.startedFlag = true;

    }

    public boolean isSuccess() {
        return successFlag;
    }

    public boolean isComplete() {
        return completeFlag;
    }

    public boolean isStarted() {
        return startedFlag;
    }

    public OnAPICallComplete setNext(OnAPICallComplete next) {
        this.next = next;
        return this;
    }

    public OnAPICallComplete setAlert(Alert alert) {
        this.alert = alert;
        return this;
    }

    public OnAPICallComplete setFailureMessage(String failureMessage) {
        this.failureMessage = failureMessage;
        return this;
    }

    public void run() { }

    public abstract void onSuccess(T t);

    public void onFailure(Throwable t) {

        // Alert user
        String message = failureMessage;
        if (t != null) {
            message +=  '\n' + t.toString();
        }

        // Alert user
        alert.alert(message);

    }

}
