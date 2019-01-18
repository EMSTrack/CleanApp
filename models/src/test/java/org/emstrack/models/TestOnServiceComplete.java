package org.emstrack.models;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Bundle;
import android.support.v4.content.LocalBroadcastManager;
import android.util.Log;

import org.emstrack.models.util.BroadcastActions;
import org.emstrack.models.util.BroadcastExtras;
import org.emstrack.models.util.OnServiceComplete;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.shadows.ShadowLog;
import org.robolectric.shadows.ShadowLooper;

import androidx.annotation.UiThread;
import androidx.test.core.app.ApplicationProvider;

import static junit.framework.Assert.assertFalse;
import static junit.framework.Assert.assertNotNull;
import static junit.framework.TestCase.assertTrue;
import static org.junit.Assert.assertSame;

@RunWith(RobolectricTestRunner.class)
public class TestOnServiceComplete {

    static {
        // redirect the Log.x output to stdout. Stdout will be recorded in the test result report
        ShadowLog.stream = System.out;
    }

    private final String TAG = TestOnServiceComplete.class.getSimpleName();

    @Test
    public void useAppContext() throws Exception {
        // Context of the app under test.
        //Context appContext = InstrumentationRegistry.getTargetContext();
        final Context appContext = ApplicationProvider.getApplicationContext();

        Log.d(TAG, appContext.getPackageName());
        assertNotNull(appContext);

    }

    @Test
    public void shouldGetInstance() throws Exception {
        final Context context = ApplicationProvider.getApplicationContext();
        LocalBroadcastManager instance = LocalBroadcastManager.getInstance(context);
        assertNotNull(instance);
        assertSame(instance, LocalBroadcastManager.getInstance(context));
    }

    @Test
    public void shouldSendBroadcasts() throws Exception {
        final Context context = ApplicationProvider.getApplicationContext();
        LocalBroadcastManager instance = LocalBroadcastManager.getInstance(context);
        final boolean[] called = {false};
        final BroadcastReceiver receiver = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                called[0] = true;
            }
        };
        instance.registerReceiver(receiver, new IntentFilter("com.foo"));

        instance.sendBroadcast(new Intent("com.bar"));
        assertFalse(called[0]);
        instance.sendBroadcast(new Intent("com.foo"));
        assertTrue(called[0]);
    }

    @Test
    public void onServiceCompleteTimeoutTest() throws Exception {

        // Context of the app under test.
        final Context appContext = ApplicationProvider.getApplicationContext();
        assertNotNull(appContext);

        // This will fail by timeout
        final boolean[] done = {false};
        new OnServiceComplete(appContext,
                BroadcastActions.SUCCESS,
                BroadcastActions.FAILURE,
                null,
                1000) {

            @Override
            public void run() {
                Log.d(TAG, "RUNNING");
            }

            @Override
            public void onSuccess(Bundle extras) {
                assertTrue(false);
                done[0] = true;
            }

            @Override
            public void onFailure(Bundle extras) {
                assertTrue(true);
                done[0] = true;
            }

        };

        ShadowLooper.runUiThreadTasksIncludingDelayedTasks();
        assertTrue(done[0]);
    }

    @Test
    @UiThread
    public void onServiceCompletePassTest() throws Exception {

        // Context of the app under test.
        final Context appContext = ApplicationProvider.getApplicationContext();
        assertNotNull(appContext);
        LocalBroadcastManager instance = LocalBroadcastManager.getInstance(appContext);
        assertNotNull(instance);

        // This will pass because of success message
        OnServiceComplete service = new OnServiceComplete(appContext,
                BroadcastActions.SUCCESS,
                BroadcastActions.FAILURE,
                null,
                1000) {

            @Override
            public void run(String uuid) {

                // Broadcast success
                Intent localIntent = new Intent(BroadcastActions.SUCCESS);
                localIntent.putExtra(BroadcastExtras.UUID, uuid);
                localIntent.putExtra(BroadcastExtras.MESSAGE, "PASSED!");
                instance.sendBroadcast(localIntent);

                Log.d(TAG, "Sent message");

            }

            @Override
            public void onSuccess(Bundle extras) {
                assertTrue(true);
            }

            @Override
            public void onFailure(Bundle extras) {
                assertTrue(false);
            }

        };

        while (!service.isComplete()) {
            Thread.sleep(1000);
            ShadowLooper.runUiThreadTasksIncludingDelayedTasks();
        }
        assertTrue(service.isComplete());
        assertTrue(service.isSuccess());
    }

    @Test
    public void onServiceCompleteFailTest() throws Exception {

        // Context of the app under test.
        //Context appContext = InstrumentationRegistry.getTargetContext();
        final Context appContext = ApplicationProvider.getApplicationContext();
        assertNotNull(appContext);
        LocalBroadcastManager instance = LocalBroadcastManager.getInstance(appContext);
        assertNotNull(instance);

        // This will fail because of failure message
        OnServiceComplete service = new OnServiceComplete(appContext,
                BroadcastActions.SUCCESS,
                BroadcastActions.FAILURE,
                null,
                1000) {

            @Override
            public void run(String uuid) {

                // Broadcast failure
                Intent localIntent = new Intent(BroadcastActions.FAILURE);
                localIntent.putExtra(BroadcastExtras.UUID, uuid);
                localIntent.putExtra(BroadcastExtras.MESSAGE, "FAILED!");
                instance.sendBroadcast(localIntent);

            }

            @Override
            public void onSuccess(Bundle extras) {
                assertTrue(false);
            }

            @Override
            public void onFailure(Bundle extras) {
                assertTrue(true);
            }

        };

        while (!service.isComplete()) {
            Thread.sleep(1000);
            ShadowLooper.runUiThreadTasksIncludingDelayedTasks();
        }
        assertTrue(service.isComplete());
        assertTrue(!service.isSuccess());
    }

}