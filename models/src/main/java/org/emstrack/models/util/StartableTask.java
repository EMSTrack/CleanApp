package org.emstrack.models.util;

/**
 * Interface for startable objects
 *
 * <p>
 *     A class implementing <code>StartableTask</code> should be able to be started
 *     by {@link #start} and to set a following task by {@link #setNext}.
 * </p>
 *
 * <p>
 *     The parameter <code>T</code> allows classes to return objects without casting
 *     for easily chaining operations.
 * </p>
 *
 * @param <T> parametrized return type
 *
 * @author mauricio
 * @since 1/20/2019
 */
public interface StartableTask<T> {

    /**
     * Start task
     * @return this object
     */
    T start();

    /**
     * Set next <code>StartableTask</code> to execute
     *
     * @param next the next service
     * @return this object
     */
    T setNext(StartableTask next);

    /**
     * Return whether the method {@link #start} has been called
     *
     * @return <code>True</code> if started
     */
    boolean isStarted();

}
