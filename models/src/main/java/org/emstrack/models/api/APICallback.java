package org.emstrack.models.api;

public interface APICallback<T> {
    void onSuccess(T t);
    void onFailure(Throwable t);
}