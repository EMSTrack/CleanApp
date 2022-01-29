package org.emstrack.ambulance.util;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import androidx.localbroadcastmanager.content.LocalBroadcastManager;

interface OnReceive {
    void onReceive(Context context, @NonNull Intent intent);
}

public class FragmentWithLocalBroadcastReceiver extends Fragment implements OnReceive {

    private static final String TAG = FragmentWithLocalBroadcastReceiver.class.getSimpleName();

    private IntentFilter filter;
    private BroadcastReceiver receiver;
    private boolean receiverActive;
    private boolean registered;

    public class LocalBroadcastReceiver extends BroadcastReceiver {
        private final OnReceive onReceive;

        LocalBroadcastReceiver(@NonNull OnReceive onReceive) {
            this.onReceive = onReceive;
        }

        @Override
        public void onReceive(Context context, Intent intent) {
            if (registered && receiverActive) {
                if (intent != null) {
                    onReceive.onReceive(context, intent);
                } else {
                    Log.d(TAG, "onReceived: null intent");
                }
            }
        }
    }

    @Override
    public void onReceive(Context context, @NonNull Intent intent) {
        Log.d(TAG, "Intent received: " + intent);
    }

    @Override
    public void onResume() {
        super.onResume();
        registerReceiver();
    }

    @Override
    public void onPause() {
        super.onPause();
        unregisterReceiver();
    }

    public void setupReceiver(@NonNull IntentFilter filter) {
        this.receiver = new LocalBroadcastReceiver(this);
        this.filter = filter;
        this.receiverActive = false;
    }

    public void setupReceiver(@NonNull IntentFilter filter, @NonNull OnReceive onReceive) {
        this.receiver = new LocalBroadcastReceiver(onReceive);
        this.filter = filter;
        this.receiverActive = false;
    }

    public boolean isReceiverActive() {
        return receiverActive;
    }

    public void setReceiverActive(boolean receiverActive) {
        this.receiverActive = receiverActive;
    }

    public boolean isRegistered() {
        return registered;
    }

    public void registerReceiver() {
        // Register receiver
        Log.d(TAG, "Registering receiver");
        getLocalBroadcastManager().registerReceiver(receiver, filter);
        registered = true;
        receiverActive = true;
    }

    public void unregisterReceiver() {
        // Unregister receiver
        if (receiverActive) {
            Log.d(TAG, "Unregistering receiver");
            getLocalBroadcastManager().unregisterReceiver(receiver);
            registered = false;
            receiverActive = false;
        }
    }

    /**
     * Get LocalBroadcastManager
     *
     * @return the LocalBroadcastManager
     */
    public LocalBroadcastManager getLocalBroadcastManager() {
        return LocalBroadcastManager.getInstance(requireContext());
    }

}
