package org.emstrack.models;

import android.net.Uri;
import android.os.Build;
import android.util.Log;

import org.emstrack.models.api.APIService;
import org.emstrack.models.api.APIServiceGenerator;
import org.emstrack.models.api.OnAPICallComplete;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.annotation.Config;
import org.robolectric.shadows.ShadowLog;
import org.robolectric.shadows.ShadowLooper;

import java.net.URI;
import java.util.List;

import retrofit2.Response;

import static junit.framework.Assert.assertNotNull;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

@RunWith(RobolectricTestRunner.class)
@Config(sdk= Build.VERSION_CODES.P)
public class TestAPI {

    static {
        // redirect the Log.x output to stdout. Stdout will be recorded in the test result report
        ShadowLog.stream = System.out;
    }

    private final String TAG = TestAPI.class.getSimpleName();

    @Test
    public void testApi() throws Exception {

        Log.d(TAG, "testApi()");

        APIService service = APIServiceGenerator.createService(APIService.class);

        String username = "admin";
        String password = "cruzrojaadmin";
        String serverURI = "https://cruzroja.ucsd.edu/en/";
        Credentials credentials = new Credentials(username, password, serverURI, "");

        retrofit2.Call<Token> callSync = service.getToken(credentials);

        Response<Token> response = callSync.execute();
        Token token = response.body();
        assertTrue(token.getToken().length() > 0);
        Log.d(TAG, "token = " + token.getToken());

        // Inject token
        service = APIServiceGenerator.createService(APIService.class, token.getToken());

        // then execute authenticated api call
        retrofit2.Call<List<Location>> callLocations = service.getLocations();

        Response<List<Location>> locationsResponse = callLocations.execute();
        List<Location> locations = locationsResponse.body();
        Log.d(TAG, "locations = " + locations);
        assertTrue(locations != null);

        // retrieve profile
        retrofit2.Call<Profile> callProfile = service.getProfile(username);

        Response<Profile> profileResponse = callProfile.execute();
        Profile profile = profileResponse.body();
        Log.d(TAG, "profile = " + profile);
        assertTrue(profile != null);

        // token login
        Uri.Builder builder = new Uri.Builder();
        builder.scheme("https")
                .authority("cruzroja.ucsd.edu")
                .appendPath("en")
                .appendPath("guest");
        String url = builder.build().toString();
        Log.d(TAG, "url = " + url);
        TokenLogin tokenLogin = new TokenLogin(url);

        retrofit2.Call<TokenLogin> callTokenLogin =
                service.getTokenLogin("guest", tokenLogin);

        Response<TokenLogin> tokenLoginResponse = callTokenLogin.execute();
        TokenLogin tokenLogin_ = tokenLoginResponse.body();
        assertTrue(tokenLogin_.getToken().length() > 0);
        Log.d(TAG, "token = " + tokenLogin_.getToken());
        assertEquals(tokenLogin_.getUsername(), "guest");
        Log.d(TAG, "url = " + tokenLogin_.getUrl());
        assertEquals(tokenLogin_.getUrl(), "https://cruzroja.ucsd.edu/en/guest");

    }

    @Test
    public void testAsyncApi() throws InterruptedException {

        APIService service = APIServiceGenerator.createService(APIService.class);

        String username = "admin";
        String password = "cruzrojaadmin";
        String serverURI = "https://cruzroja.ucsd.edu/";
        Credentials credentials = new Credentials(username, password, serverURI, "");

        retrofit2.Call<Token> callAsync = service.getToken(credentials);

        OnAPICallComplete api = new OnAPICallComplete<Token>(callAsync) {

            @Override
            public void onSuccess(Token t) {
                assertTrue(t.getToken().length() > 0);
                Log.d(TAG, "token = " + t.getToken());
            }

        };

        api.start();
        while (!api.isComplete()) {
            Thread.sleep(1000);
            ShadowLooper.runUiThreadTasks();
        }
        assertTrue(api.isComplete());

    }

    @Test
    public void testAsyncApiCascaded() throws InterruptedException {

        APIService service = APIServiceGenerator.createService(APIService.class);

        String username = "admin";
        String password = "cruzrojaadmin";
        String serverURI = "https://cruzroja.ucsd.edu/";
        Credentials credentials = new Credentials(username, password, serverURI, "");

        retrofit2.Call<Token> callAsync = service.getToken(credentials);

        final String[] token = new String[1];
        OnAPICallComplete api = new OnAPICallComplete<Token>(callAsync) {

            @Override
            public void onSuccess(Token t) {
                assertTrue(t.getToken().length() > 0);
                Log.d(TAG, "token = " + t.getToken());
                token[0] = t.getToken();
            }

        };

        api.start();
        while (!api.isComplete()) {
            Thread.sleep(1000);
            ShadowLooper.runUiThreadTasks();
        }
        assertTrue(api.isComplete());

        // Inject token
        service = APIServiceGenerator.createService(APIService.class, token[0]);

        // then execute authenticated cascaded api calls
        retrofit2.Call<List<Location>> callLocations = service.getLocations();
        OnAPICallComplete apiLocations = new OnAPICallComplete<List<Location>>(callLocations) {

            @Override
            public void onSuccess(List<Location> l) {
                assertNotNull(l);
                Log.d(TAG, "locations= " + l);
            }

        };

        retrofit2.Call<Profile> callProfile = service.getProfile(username);
        OnAPICallComplete apiProfile= new OnAPICallComplete<Profile>(callProfile, apiLocations) {

            @Override
            public void onSuccess(Profile p) {
                assertNotNull(p);
                Log.d(TAG, "profile = " + p);
            }

        };

        apiProfile.start();
        while (!(apiProfile.isComplete() && apiLocations.isComplete())) {
            Thread.sleep(1000);
            ShadowLooper.runUiThreadTasks();
        }
        assertTrue(apiProfile.isComplete());
        assertTrue(apiLocations.isComplete());

    }

    @Test
    public void testAsyncApiCascaded2() throws InterruptedException {

        String username = "admin";
        String password = "cruzrojaadmin";
        String serverURI = "https://cruzroja.ucsd.edu/";
        Credentials credentials = new Credentials(username, password, serverURI, "");

        // Retrieve token
        APIServiceGenerator.setServerUri(credentials.getApiServerUri());
        APIService service = APIServiceGenerator.createService(APIService.class);
        retrofit2.Call<Token> call = service.getToken(credentials);
        OnAPICallComplete<Token> api = new OnAPICallComplete<Token>(call) {

            @Override
            public void onSuccess(Token token) {
                APIServiceGenerator.setToken(token.getToken());
                assertTrue(true);
            }

            @Override
            public void onFailure(Throwable t) {
                assertTrue(false);
            }

        };

        api.start();
        while (!api.isComplete()) {
            Thread.sleep(1000);
            ShadowLooper.runUiThreadTasks();
        }
        assertTrue(api.isComplete());
        assertTrue(APIServiceGenerator.getToken().length() > 0);

        // then execute authenticated cascaded api calls
        retrofit2.Call<List<Location>> callLocations = service.getLocations();
        retrofit2.Call<Profile> callProfile = service.getProfile(username);
        OnAPICallComplete apiProfile= new OnAPICallComplete<Profile>(callProfile) {

            @Override
            public void onSuccess(Profile p) {
                assertNotNull(p);
                Log.d(TAG, "profile = " + p);
            }

        };

        OnAPICallComplete apiLocations = new OnAPICallComplete<List<Location>>(callLocations) {

            @Override
            public void onSuccess(List<Location> l) {
                assertNotNull(l);
                Log.d(TAG, "locations= " + l);
            }

        };

        apiProfile.setNext(apiLocations);

        apiProfile.start();
        while (!(apiProfile.isComplete() && apiLocations.isComplete())) {
            Thread.sleep(1000);
            ShadowLooper.runUiThreadTasks();
        }
        assertTrue(apiProfile.isComplete());
        assertTrue(apiLocations.isComplete());

    }

}