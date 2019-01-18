package org.emstrack.models.util;

import android.content.Context;
import android.content.Intent;
import android.util.Log;

import org.emstrack.models.api.APICallback;

public class BroadcastCallback<T> implements APICallback<T> {

    final static String TAG = BroadcastCallback.class.getSimpleName();

    private String uuid;
    private APICallback<T> callback;
    private Context context;
    private String successAction;
    private String failureAction;

    public BroadcastCallback(APICallback<T> callback,
                             Context context,
                             String successAction,
                             String failureAction,
                             String uuid) {
        this.callback = callback;
        this.context = context;
        this.successAction = successAction;
        this.failureAction = failureAction;
        this.uuid = uuid;
    }

    public BroadcastCallback(APICallback<T> callback,
                             Context context,
                             String uuid) {
        this(callback, context, BroadcastActions.SUCCESS, BroadcastActions.FAILURE, uuid);
    }

    public String getUuid() {
        return uuid;
    }

    public void setUuid(String uuid) {
        this.uuid = uuid;
    }

    @Override
    public void onSuccess(T t) {
        Log.d(TAG, "onSuccess");
        callback.onSuccess(t);
        broadcastSuccess();
    }

    @Override
    public void onFailure(Throwable t) {
        Log.d(TAG, "onFailure");
        callback.onFailure(t);
        broadcastFailure(t);
    }

    private void broadcastFailure(Throwable t) {

        // Broadcast failure
        Intent localIntent = new Intent(failureAction);
        localIntent.putExtra(BroadcastExtras.UUID, uuid);
        localIntent.putExtra(BroadcastExtras.MESSAGE, t.toString());
        context.sendBroadcast(localIntent);

    }

    private void broadcastSuccess() {

        // Broadcast success
        Intent localIntent = new Intent(successAction);
        localIntent.putExtra(BroadcastExtras.UUID, uuid);
        context.sendBroadcast(localIntent);

    }
}