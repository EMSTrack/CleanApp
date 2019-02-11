package org.emstrack.models;

import android.util.Pair;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

/**
 * A stack for {@link Call}s.
 *
 * <p>
 *     {@link CallStack} stores {@link Call}s in a <code>Map</code> and provide functionality
 *     for filtering and retrieving calls.
 * </p>
 *
 * <p>
 *     One call can be designated as the <em>current call</em>.
 * </p>
 *
 * <p>
 *     The current call can also be set as <em>pending</em>, with such information been
 *     useful when the prospective current call is still waiting for confirmation from users.
 * </p>
 *
 * @author mauricio
 * @since 10/1/2018
 */
public class CallStack implements Iterable {

    private Map<Integer, Call> stack;
    private int currentCallId;

    /**
     * Exception class to report {@link CallStack} exceptions
     */
    public class CallStackException extends Exception {

        /**
         *
         * @param message the message
         */
        public CallStackException(String message) {
            super(message);
        }
    }

    /**
     * Iterator class to traverse {@link CallStack}
     */
    public class CallStackIterator implements Iterator<Map.Entry<Integer, Call>> {

        Iterator<Map.Entry<Integer, Call>> iterator = stack.entrySet().iterator();
        Map.Entry<Integer, Call> current = null;

        @Override
        public Map.Entry<Integer, Call> next() {
            return this.current = this.iterator.next();
        }

        @Override
        public boolean hasNext() {
            return this.iterator.hasNext();
        }

        @Override
        public void remove() {
            if (this.current != null && this.current.getKey() == currentCallId)
                currentCallId = -1;
            this.iterator.remove();
            this.current = null;
        }
    }

    /**
     *
     */
    public CallStack() {
        this.stack = new HashMap<>();
        this.currentCallId = -1;
    }

    /**
     * Add call to call stack
     *
     * @param call the call
     * @return this object
     */
    public Call put(Call call) {
        return this.stack.put(call.getId(), call);
    }

    /**
     * Remove call from stack
     *
     * <p>
     *     If call is the current call clear current call
     * </p>
     *
     * @param id the call id
     * @return this object
     */
    public Call remove(int id) {
        if (this.currentCallId == id)
            this.currentCallId = -1;
        return this.stack.remove(id);
    }

    /**
     * Get call from stack or <code>null</code> if call not in stack
     *
     * @param id the call id
     * @return this object
     */
    public Call get(int id) {
        return this.stack.get(id);
    }

    /**
     * Returns <code>True</code> if call with id <code>id</code> is in the stack
     *
     * @param id the call id
     * @return <code>True</code> or <code>False</code>
     */
    public boolean contains(int id) {
        return this.stack.containsKey(id);
    }

    /**
     * Returns <code>True</code> if call <code>call</code> is in the stack
     *
     * @param call the call
     * @return <code>True</code> or <code>False</code>
     */
    public boolean contains(Call call) {
        return contains(call.getId());
    }

    /**
     * Get call from stack or <code>null</code> if current call hasn't been set yet
     *
     * @return the current call
     */
    public Call getCurrentCall() {
        return get(this.currentCallId);
    }

    /**
     * Get current call id or <code>-1</code> if current call hasn't been set yet
     *
     * @return the current call id
     */
    public int getCurrentCallId() {
        return this.currentCallId;
    }

    /**
     * Set call with id <code>id</code> as current call
     *
     * @param id the call id
     * @throws CallStackException if call is not in stack
     */
    public void setCurrentCall(int id) throws CallStackException {
        if (!contains(id))
            throw new CallStackException("Call is not in stack.");
        this.currentCallId = id;
    }

    /**
     * Set call <code>call</code> as current call
     *
     * @param call the call
     * @throws CallStackException if call is not in stack
     */
    public void setCurrentCall(Call call) throws CallStackException {
        setCurrentCall(call.getId());
    }

    /**
     * Returns <code>True</code> if call with id <code>id</code> is current call
     *
     * @param id the call id
     * @return <code>True</code> or <code>False</code>
     */
    public boolean isCurrentCall(int id) {
        return this.currentCallId > 0 && this.currentCallId == id;
    }

    /**
     * Returns <code>True</code> if current call has been set
     *
     * @return <code>True</code> or <code>False</code>
     */
    public boolean hasCurrentCall() {
        return this.currentCallId > 0;
    }

    /**
     * Returns <code>True</code> if current call has been set or there is a pending call
     *
     * @return <code>True</code> or <code>False</code>
     */
    public boolean hasCurrentOrPendingCall() {
        return this.currentCallId >= 0;
    }

    /**
     * Returns <code>True</code> if there is a pending call
     *
     * @return <code>True</code> or <code>False</code>
     */
    public boolean hasPendingCall() {
        return this.currentCallId == 0;
    }

    /**
     * Set current call as pending
     *
     * @param flag <code>True</code> or <code>False</code>
     */
    public void setPendingCall(boolean flag) {
        if (flag)
            this.currentCallId = 0;
        else
            this.currentCallId = -1;
    }

    /**
     * Get next call in the stack for the ambulance with id <code>ambulanceId</code> and
     * status <code>status</code>
     *
     * @param ambulanceId the ambulance id
     * @param status the ambulance call status (see {@link AmbulanceCall}
     * @return the next call
     */
    public Call getNextCall(int ambulanceId, String status) {
        CallStackIterator iterator = iterator();
        while (iterator.hasNext()) {
            Map.Entry<Integer, Call> entry = iterator.next();
            Call call = entry.getValue();
            if (call.getAmbulanceCall(ambulanceId).getStatus().equals(status))
                return call;
        }
        return null;
    }

    /**
     * Get next call in the stack for the ambulance with id <code>ambulanceId</code>
     *
     * <p>
     *     This function will first return calls with status in the following order:
     * </p>
     *
     * <ul>
     *     <li>{@link AmbulanceCall#STATUS_ACCEPTED}</li>
     *     <li>{@link AmbulanceCall#STATUS_REQUESTED}</li>
     *     <li>{@link AmbulanceCall#STATUS_SUSPENDED}</li>
     * </ul>
     *
     * <p>
     *     It will ignore {@link AmbulanceCall#STATUS_COMPLETED} and
     *     {@link AmbulanceCall#STATUS_DECLINED}.
     * </p>
     *
     * @param ambulanceId the ambulance id
     * @return the next call
     */
    public Call getNextCall(int ambulanceId) {

        // Try accepted first
        Call call = getNextCall(ambulanceId, AmbulanceCall.STATUS_ACCEPTED);
        if (call != null)
            return call;

        // then requested
        call = getNextCall(ambulanceId, AmbulanceCall.STATUS_REQUESTED);
        if (call != null)
            return call;

        /*
        // then suspended
        call = getNextCall(ambulanceId, AmbulanceCall.STATUS_SUSPENDED);
        if (call != null)
            return call;
        */

        // Ignore STATUS_SUSPENDED, STATUS_COMPLETED, and STATUS_DECLINED calls
        return null;
    }

    /**
     * Summarize calls in the stack by call status
     *
     * <p>
     *     The stack summary is a map with the call statuses as key and the number
     *     of calls with that status as value.
     * </p>
     *
     * @return the stack summary
     */
    public Map<String,Integer> summary() {
        // Initialize count
        Map<String, Integer> map = new HashMap<>();
        Iterator<String> iterStatus = Call.statusLabel.keySet().iterator();
        while (iterStatus.hasNext())
            map.put(iterStatus.next(), 0);

        // Loop through call stack
        CallStackIterator iterator = iterator();
        while (iterator.hasNext()) {
            Map.Entry<Integer, Call> entry = iterator.next();
            String status = entry.getValue().getStatus();
            map.put(status, map.get(status) + 1);
        }
        return map;
    }

    /**
     * Summarize calls in the stack for ambulance with id <code>ambulanceId</code>
     * by ambulance call status
     *
     * <p>
     *     The stack summary is a map with the ambulance call statuses as key and the number
     *     of calls with that status as value.
     * </p>

     * @param ambulanceId the ambulance id
     * @return the stack summary
     */
    public Map<String,Integer> summary(int ambulanceId) {
        // Initialize count
        Map<String, Integer> map = new HashMap<>();
        Iterator<String> iterStatus = AmbulanceCall.statusLabel.keySet().iterator();
        while (iterStatus.hasNext())
            map.put(iterStatus.next(), 0);

        // Loop through call stack
        CallStackIterator iterator = iterator();
        while (iterator.hasNext()) {
            Map.Entry<Integer, Call> entry = iterator.next();
            String status = entry.getValue().getAmbulanceCall(ambulanceId).getStatus();
            map.put(status, map.get(status) + 1);
        }
        return map;
    }

    /**
     * Returns a map of calls filtered for ambulance with id <code>ambulanceId</code> and
     * ambulance call status <code>status</code>
     *
     * @param ambulanceId the ambulance id
     * @param status the status
     * @return the map
     */
    public Map<Integer,Pair<Call,AmbulanceCall>> filter(int ambulanceId, String status) {
        // Loop through call stack
        Map<Integer, Pair<Call,AmbulanceCall>> map = new HashMap<>();
        CallStackIterator iterator = iterator();
        while (iterator.hasNext()) {
            Map.Entry<Integer, Call> entry = iterator.next();
            Call call = entry.getValue();
            AmbulanceCall ambulanceCall = call.getAmbulanceCall(ambulanceId);
            if (status == null || ambulanceCall.getStatus().equals(status))
                map.put(entry.getKey(), new Pair<>(call, ambulanceCall));
        }
        return map;
    }

    /**
     * Returns a map of calls filtered for ambulance with id <code>ambulanceId</code>
     *
     * @param ambulanceId the ambulance id
     * @return the map
     */
    public Map<Integer,Pair<Call,AmbulanceCall>> filter(int ambulanceId) {
        return filter(ambulanceId, null);
    }

    /**
     * Return an iterator for the stack
     *
     * @return the iterator
     */
    public CallStackIterator iterator() {
        return new CallStackIterator();
    }

}
