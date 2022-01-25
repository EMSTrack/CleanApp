package org.emstrack.ambulance.services;

import android.app.Service;
import android.content.Intent;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.localbroadcastmanager.content.LocalBroadcastManager;

import org.emstrack.models.util.BroadcastExtras;

/**
 * Created by mauricio on 3/24/2018.
 */

public abstract class BroadcastService extends Service {

    public void sendBroadcastWithUUID(@NonNull Intent intent) {
        sendBroadcastWithUUID(intent, null);
    }

    public void sendBroadcastWithUUID(@NonNull Intent intent, @Nullable String uuid) {

        if (uuid != null)
            // inject uuid
            intent.putExtra(BroadcastExtras.UUID, uuid);

        // broadcast
        getLocalBroadcastManager().sendBroadcast(intent);

    }


    /**
     * Get the LocalBroadcastManager
     *
     * @return The system LocalBroadcastManager
     */
    private LocalBroadcastManager getLocalBroadcastManager() {
        return LocalBroadcastManager.getInstance(this);
    }

}
