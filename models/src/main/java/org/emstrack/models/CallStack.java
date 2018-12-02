package org.emstrack.models;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

public class CallStack implements Iterable {

    private Map<Integer, Call> stack;
    private int currentCallId;

    public class CallStackException extends Exception {

        public CallStackException(String message) {
            super(message);
        }
    }

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

    public CallStack() {
        this.stack = new HashMap<>();
        this.currentCallId = -1;
    }

    public Call put(int id, Call call) {
        return this.stack.put(id, call);
    }

    public Call remove(int id) {
        if (this.currentCallId == id)
            this.currentCallId = -1;
        return this.stack.remove(id);
    }

    public Call get(int id) {
        return this.stack.get(id);
    }

    public boolean contains(int id) {
        return this.stack.containsKey(id);
    }

    public boolean contains(Call call) {
        return contains(call.getId());
    }

    public Call getCurrentCall() {
        return get(this.currentCallId);
    }

    public int getCurrentCallId() {
        return this.currentCallId;
    }

    public void setCurrentCall(int id) throws CallStackException {
        if (!contains(id))
            throw new CallStackException("Call is not in stack.");
        this.currentCallId = id;
    }

    public void setCurrentCall(Call call) throws CallStackException {
        setCurrentCall(call.getId());
    }

    public boolean hasCurrentCall() {
        return this.currentCallId > 0;
    }

    public boolean hasCurrentOrPendingCall() {
        return this.currentCallId >= 0;
    }

    public boolean hasPendingCall() {
        return this.currentCallId == 0;
    }

    public void setPendingCall(boolean flag) {
        if (flag)
            this.currentCallId = 0;
        else
            this.currentCallId = -1;
    }

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

    public Call getNextCall(int ambulanceId) {

        // Try suspended first
        Call call = getNextCall(ambulanceId, AmbulanceCall.STATUS_SUSPENDED);
        if (call != null)
            return call;

        // Then requested
        call = getNextCall(ambulanceId, AmbulanceCall.STATUS_REQUESTED);
        if (call != null)
            return call;

        // Ignore STATUS_COMPLETED, STATUS_DECLINED and STATUS_ONGOING calls
        return null;
    }

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

    public CallStackIterator iterator() {
        return new CallStackIterator();
    }

}
