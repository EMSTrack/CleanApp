package org.emstrack.models;

import static android.os.Looper.getMainLooper;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Build;
import android.os.Bundle;
import androidx.localbroadcastmanager.content.LocalBroadcastManager;
import android.util.Log;

import org.emstrack.models.util.BroadcastActions;
import org.emstrack.models.util.BroadcastExtras;
import org.emstrack.models.util.OnServiceComplete;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.annotation.Config;
import org.robolectric.shadows.ShadowLog;
import org.robolectric.shadows.ShadowLooper;

import androidx.annotation.UiThread;
import androidx.test.core.app.ApplicationProvider;

import static junit.framework.Assert.assertFalse;
import static junit.framework.Assert.assertNotNull;
import static junit.framework.TestCase.assertTrue;
import static org.junit.Assert.assertSame;
import static org.robolectric.Shadows.shadowOf;

@RunWith(RobolectricTestRunner.class)
@Config(sdk= Build.VERSION_CODES.R)
public class TestOnServiceComplete {

    static {
        // redirect the Log.x output to stdout. Stdout will be recorded in the test result report
        ShadowLog.stream = System.out;
    }

    private final String TAG = TestOnServiceComplete.class.getSimpleName();

    @Test
    public void useAppContext() {
        // Context of the app under test.
        //Context appContext = InstrumentationRegistry.getTargetContext();
        final Context appContext = ApplicationProvider.getApplicationContext();

        Log.d(TAG, appContext.getPackageName());
        assertNotNull(appContext);

    }

    @Test
    public void shouldGetInstance() {
        final Context context = ApplicationProvider.getApplicationContext();
        LocalBroadcastManager instance = LocalBroadcastManager.getInstance(context);
        assertNotNull(instance);
        assertSame(instance, LocalBroadcastManager.getInstance(context));
    }

    @Test
    public void shouldSendBroadcasts() {
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
        shadowOf(getMainLooper()).idle();
        assertTrue(called[0]);
    }

    @Test
    public void onServiceCompleteTimeoutTest() throws Exception {

        // Context of the app under test.
        final Context appContext = ApplicationProvider.getApplicationContext();
        assertNotNull(appContext);

        // This will fail by timeout
        OnServiceComplete service = new OnServiceComplete(appContext,
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
            }

            @Override
            public void onFailure(Bundle extras) {
                assertTrue(true);
            }

        };

        assertFalse(service.isStarted());
        service.start();
        assertTrue(service.isStarted());
        while (!(service.isComplete() || service.isTimedOut())) {
            Thread.sleep(1000);
            ShadowLooper.runUiThreadTasksIncludingDelayedTasks();
        }
        assertTrue(!service.isComplete());
        assertTrue(!service.isSuccess());
        assertTrue(service.isTimedOut());
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
            public void run() {

                // Broadcast success
                Intent localIntent = new Intent(BroadcastActions.SUCCESS);
                localIntent.putExtra(BroadcastExtras.UUID, getUuid());
                localIntent.putExtra(org.emstrack.models.util.BroadcastExtras.MESSAGE, "PASSED!");
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

        assertFalse(service.isStarted());
        service.start();
        assertTrue(service.isStarted());
        while (!(service.isComplete() || service.isTimedOut())) {
            Thread.sleep(1000);
            ShadowLooper.runUiThreadTasksIncludingDelayedTasks();
        }
        assertTrue(service.isComplete());
        assertTrue(service.isSuccess());
        assertTrue(!service.isTimedOut());
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
            public void run() {

                // Broadcast failure
                Intent localIntent = new Intent(BroadcastActions.FAILURE);
                localIntent.putExtra(BroadcastExtras.UUID, getUuid());
                localIntent.putExtra(org.emstrack.models.util.BroadcastExtras.MESSAGE, "FAILED!");
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

        assertFalse(service.isStarted());
        service.start();
        assertTrue(service.isStarted());
        while (!(service.isComplete() || service.isTimedOut())) {
            Thread.sleep(1000);
            ShadowLooper.runUiThreadTasksIncludingDelayedTasks();
        }
        assertTrue(service.isComplete());
        assertTrue(!service.isSuccess());
        assertTrue(!service.isTimedOut());
    }

    @Test
    @UiThread
    public void onServiceCompleteEnforceUUIDTest() throws Exception {

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
            public void run() {

                // Broadcast success
                Intent localIntent = new Intent(BroadcastActions.SUCCESS);
                localIntent.putExtra(BroadcastExtras.UUID, getUuid() + "NOT");
                localIntent.putExtra(org.emstrack.models.util.BroadcastExtras.MESSAGE, "FAILED");
                instance.sendBroadcast(localIntent);

                Log.d(TAG, "Sent message");

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

        assertFalse(service.isStarted());
        service.start();
        assertTrue(service.isStarted());
        while (!(service.isComplete() || service.isTimedOut())) {
            Thread.sleep(1000);
            ShadowLooper.runUiThreadTasksIncludingDelayedTasks();
        }
        assertTrue(!service.isComplete());
        assertTrue(!service.isSuccess());
        assertTrue(service.isTimedOut());
    }

    @Test
    @UiThread
    public void onServiceCompleteIgnoreUUIDTest() throws Exception {

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
            public void run() {

                // Broadcast success
                Intent localIntent = new Intent(BroadcastActions.SUCCESS);
                localIntent.putExtra(BroadcastExtras.UUID, getUuid() + "NOT");
                localIntent.putExtra(org.emstrack.models.util.BroadcastExtras.MESSAGE, "FAILED");
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

        }
                .setSuccessIdCheck(false);

        assertFalse(service.isStarted());
        service.start();
        assertTrue(service.isStarted());
        while (!(service.isComplete() || service.isTimedOut())) {
            Thread.sleep(1000);
            ShadowLooper.runUiThreadTasksIncludingDelayedTasks();
        }
        assertTrue(service.isComplete());
        assertTrue(service.isSuccess());
        assertTrue(!service.isTimedOut());
    }

}