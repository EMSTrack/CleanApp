package org.emstrack.models;

import android.content.Context;
import android.support.test.InstrumentationRegistry;
import android.support.test.annotation.UiThreadTest;
import android.support.test.runner.AndroidJUnit4;
import android.util.Log;

import org.emstrack.models.api.APIService;
import org.emstrack.models.api.APIServiceGenerator;
import org.junit.Test;
import org.junit.runner.RunWith;

import retrofit2.Response;

import static org.junit.Assert.*;

/**
 * Instrumented test, which will execute on an Android device.
 *
 * @see <a href="http://d.android.com/tools/testing">Testing documentation</a>
 */
@RunWith(AndroidJUnit4.class)
public class ModelInstrumentedTest {

    private final String TAG = ModelInstrumentedTest.class.getSimpleName();

    @Test
    public void useAppContext() throws Exception {
        // Context of the app under test.
        Context appContext = InstrumentationRegistry.getTargetContext();

        assertEquals("org.emstrack.models.test", appContext.getPackageName());

    }

    @UiThreadTest
    public void test_retrofit() throws Exception {

        Log.d(TAG, "test_retrofit_broadcast()");

        APIService service = APIServiceGenerator.createService(APIService.class);

        Log.d(TAG, "api");

        String username = "admin";
        String password = "cruzrojaadmin";
        String serverURI = "https://cruzroja.ucsd.edu/";
        Credentials credentials = new Credentials(username, password, serverURI, "");

        retrofit2.Call<Token> callSync = service.getToken(credentials);

        Response<Token> response = callSync.execute();
        Token token = response.body();
        assertTrue(token.getToken().length() > 0);
        System.out.println("token = " + token.getToken());

        Log.d(TAG, "token = " + token.getToken());

    }

}
