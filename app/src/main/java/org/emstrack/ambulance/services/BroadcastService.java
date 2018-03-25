package org.emstrack.ambulance.services;

import android.app.Service;
import android.content.Intent;
import android.support.v4.content.LocalBroadcastManager;

/**
 * Created by mauricio on 3/24/2018.
 */

public abstract class BroadcastService extends Service {

    public void sendBroadcastWithUUID(Intent intent) {
        sendBroadcastWithUUID(intent, null);
    }

    public void sendBroadcastWithUUID(Intent intent, String uuid) {

        if (uuid != null)
            // inject uuid
            intent.putExtra(OnServiceComplete.UUID, uuid);

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
