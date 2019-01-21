package org.emstrack.models.util;

/**
 * A {@link StartableTask} that simply runs a task
 *
 * <p>
 *     Typical usage is to chain a task to an existing {@link OnServiceComplete}. For example:
 * </p>
 *
 * <pre>
 * // create object
 * OnServiceComplete service =
 *     new OnServiceComplete(context,
 *                           successAction, failureAction,
 *                           intent,
 *                           timeout) {
 *
 *     &#64;Override
 *     public void onSuccess(Bundle extras) {
 *         // code to run in case the service succeeds
 *     }
 *
 * }.setNext( new OnComplete() {
 *
 *     &#64;Override
 *     public void run() {
 *         // code to run after the service succeeds
 *     }
 *
 * });
 *
 * // start intent and listen to actions
 * service.start();
 * </pre>
 *
 * @author mauricio
 * @since 3/21/2018
 */
public abstract class OnComplete implements StartableTask<OnComplete> {

    private StartableTask next;
    private boolean startedFlag;

    /**
     *
     */
    public OnComplete() {
        this(null);
    }

    /**
     *
     * @param next the next {@link StartableTask}
     */
    public OnComplete(StartableTask next) {
        this.startedFlag = false;
        this.next = next;
    }

    /**
     * Run when started
     */
    abstract public void run();

    /**
     * Run {@link #run()} and start next {@link StartableTask}
     *
     * @return this object
     */
    @Override
    public OnComplete start() {

        // Run
        this.run();

        // set started
        this.startedFlag = true;

        // has next
        if (this.next != null)
            this.next.start();

        // return this
        return this;
    }

    /**
     * Set next {@link StartableTask} to execute after a successful action is received
     *
     * <p>
     *     <b>IMPORTANT:</b> The next {@link StartableTask} is alwyas executed.
     * </p>
     *
     * @param next the next {@link StartableTask}
     * @return this object
     */
    @Override
    public OnComplete setNext(StartableTask next) {
        this.next = next;
        return this;
    }

    /**
     * Return whether the method {@link #start} has been called
     *
     * @return <code>True</code> if started
     */
    @Override
    public boolean isStarted() {
        return this.startedFlag;
    }

}
