package org.emstrack.models;

import android.content.Context;
import android.os.Build;

import org.emstrack.models.api.APIService;
import org.emstrack.models.api.APIServiceGenerator;
import org.emstrack.models.api.OnAPICallComplete;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.RuntimeEnvironment;
import org.robolectric.annotation.Config;
import org.robolectric.shadows.ShadowLooper;

import java.util.List;

import static org.junit.Assert.assertTrue;

import retrofit2.Response;

@RunWith(RobolectricTestRunner.class)
@Config(sdk= Build.VERSION_CODES.R)
public class TestRetrofit {

    @Test
    public void test_retrofit() throws Exception {

        System.out.println("test_retrofit()");

        APIService service = APIServiceGenerator.createService(APIService.class);

        // retrieve servers
        retrofit2.Call<List<String>> callServers = service.getServers();
        Response<List<String>> serversResponse = callServers.execute();
        List<String> servers = serversResponse.body();
        assertTrue(servers.size() > 0);
        System.out.println("servers = " + servers);

        final Context application = RuntimeEnvironment.getApplication();
        String username = application.getString(R.string.apiUser);
        String password = application.getString(R.string.apiPassword);
        String serverURI = application.getString(R.string.apiServerUrl);
        Credentials credentials = new Credentials(username, password, serverURI, "");

        retrofit2.Call<Token> callSync = service.getToken(credentials);

        Response<Token> response = callSync.execute();
        Token token = response.body();
        assertTrue(token.getToken().length() > 0);
        System.out.println("token = " + token.getToken());

        // Inject token
        service = APIServiceGenerator.createService(APIService.class, token.getToken());

        // then execute authenticated api call
        retrofit2.Call<List<Location>> callLocations = service.getLocations();

        Response<List<Location>> locationsResponse = callLocations.execute();
        List<Location> locations = locationsResponse.body();
        System.out.println("locations = " + locations);
        assertTrue(locations != null);

        // retrieve profile
        retrofit2.Call<Profile> callProfile = service.getProfile(username);

        Response<Profile> profileResponse = callProfile.execute();
        Profile profile = profileResponse.body();
        System.out.println("profile = " + profile);
        assertTrue(profile != null);

    }

    @Test
    public void test_retrofit_with_credentials() throws Exception {

        System.out.println("test_retrofit_with_credentials()");

        final Context application = RuntimeEnvironment.getApplication();
        String username = application.getString(R.string.apiUser);
        String password = application.getString(R.string.apiPassword);
        String serverURI = application.getString(R.string.apiServerUrl);
        Credentials credentials = new Credentials(username, password, serverURI, "");

        // Retrieve token
        APIServiceGenerator.setServerUri(credentials.getApiServerUri());
        APIService service = APIServiceGenerator.createService(APIService.class);
        retrofit2.Call<Token> call = service.getToken(credentials);
        OnAPICallComplete<Token> api = new OnAPICallComplete<Token>(call) {

            @Override
            public void onSuccess(Token token) {
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

    }

}