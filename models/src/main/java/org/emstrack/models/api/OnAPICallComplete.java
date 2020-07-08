package org.emstrack.models.api;

import android.util.Log;

import org.emstrack.models.util.Alert;
import org.emstrack.models.util.StartableTask;

import retrofit2.Call;
import retrofit2.Response;

/**
 * Safely run and chain retrofit api calls
 *
 * <p>
 *     The <code>OnAPICallComplete</code> class provides a convenient way to safely
 *     run an asynchronous retrofit api call.
 * </p>
 *
 * <p>
 *     Typical usage is as follows:
 * </p>
 *
 * <pre>
 * // create object
 * OnAPICallComplete&#60;myclass&#62; service =
 *     new OnAPICallComplete&#60myclass>(call) {
 *
 *     &#64;Override
 *     public void run() {
 *         // code to run before initiating call
 *     }
 *
 *     &#64;Override
 *     public void onSuccess(myclass t) {
 *         // code to run in case the call succeeds
 *     }
 *
 *     &#64;Override
 *     public void onFailure(Throwable t) {
 *         // code to run in case the call fails
 *     }
 *
 * };
 *
 * // start intent and listen to actions
 * service.start();
 * </pre>
 *
 * <p>
 *     The {@link #start()} method runs the method {@link #run()}, then fires the api call
 *     <code>call</code>.
 *  </p>
 *
 *  <p>
 *     The method <code>onSuccess</code> will be called in case the api call is successful.
 *  </p>
 *
 *  <p>
 *     The method <code>onFailure</code> will be called in case the api call is unsuccessful.
 * </p>
 *
 * <p>
 *     API calls can be chained by using the argument <code>next</code> or {@link #setNext}.
 *     For example:
 * </p>
 *
 * <pre>
 * new OnAPICallComplete&#60;myclass1&#62;(call1) {
 *
 *     &#64;Override
 *     public void onSuccess(myclass1 t) {
 *         // code to run in case call1 succeeds
 *     }
 *
 * }.setNext(
 *     new OnAPICallComplete&#60;myclass2&#62;(call2) {
 *
 *         &#64;Override
 *         public void onSuccess(myclass2 t) {
 *            // code to run in case call2 succeeds
 *         }
 *
 *     }
 * )
 * .start();
 * </pre>
 *
 * <p>
 *     will first run <code>call1</code> and then, if <code>call1</code> is successful,
 *     run <code>call2</code>.
 * </p>
 *
 * @author mauricio
 * @since 1/16/2019
 */
public abstract class OnAPICallComplete<T> implements StartableTask<OnAPICallComplete<T>>  {

    /**
     * Exception class to report {@link OnAPICallComplete} exceptions
     */
    public static class Exception extends java.lang.Exception {
        /**
         * @param message the exception message
         */
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
    private StartableTask next;

    /**
     * @param call the call to fire
     */
    public OnAPICallComplete(Call<T> call) {
        this(call, null);
    }

    /**
     *
     * @param call the call to fire
     * @param next the next <code>StartableTask</code> to execute
     */
    public OnAPICallComplete(Call<T> call, StartableTask next) {

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

    /**
     * Run {@link #run()} and start the call
     *
     * @return this object
     */
    @Override
    public OnAPICallComplete<T> start() {

        // Run
        this.run();

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

                    // parse the response body …
                    APIError error = APIErrorUtils.parseError(response);

                    successFlag = false;
                    OnAPICallComplete.this.onFailure(error);
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

        // return this
        return this;
    }

    /**
     * Set next {@link StartableTask} to execute after a successful call
     *
     * <p>
     *     <b>IMPORTANT:</b> The next {@link StartableTask} is not executed if call is unsuccessful.
     * </p>
     *
     * @param next the next {@link StartableTask}
     * @return this object
     */
    @Override
    public OnAPICallComplete<T> setNext(StartableTask next) {
        this.next = next;
        return this;
    }

    /**
     * Return whether the call was successful
     *
     * @return <code>True</code> if successful
     */
    public boolean isSuccess() {
        return successFlag;
    }

    /**
     * Return whether the call was completed (as opposed to aborted or still pending)
     *
     * @return <code>True</code> if completed
     */
    public boolean isComplete() {
        return this.completeFlag;
    }

    /**
     * Return whether the call has started
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
    public OnAPICallComplete setAlert(Alert alert) {
        this.alert = alert;
        return this;
    }

    /**
     * Set the failure message
     * @param failureMessage the failure message
     * @return this object
     */
    public OnAPICallComplete setFailureMessage(String failureMessage) {
        this.failureMessage = failureMessage;
        return this;
    }

    /**
     * Run before firing the call
     */
    public void run() { }

    /**
     * Called if the call succeeds
     *
     * @param t the object resulting of a succesfull call
     */
    public abstract void onSuccess(T t);

    /**
     * Called if the call fails
     *
     * @param t the exception
     */
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
