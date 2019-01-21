package org.emstrack.models.api;

import com.google.gson.FieldNamingPolicy;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

import org.emstrack.models.Credentials;
import org.emstrack.models.Token;

import okhttp3.OkHttpClient;
import okhttp3.Request;
import retrofit2.Retrofit;
import retrofit2.converter.gson.GsonConverterFactory;

/**
 * Convenience class to build retrofit api service calls
 *
 * @author mauricio
 * @since 1/19/2019
 */
public class APIServiceGenerator {

    private static String token;
    private static Credentials credentials;

    private static String BASE_URL = "https://cruzroja.ucsd.edu/api/";

    private static Gson gson = new GsonBuilder()
            .setFieldNamingPolicy(FieldNamingPolicy.LOWER_CASE_WITH_UNDERSCORES)
            .create();

    private static Retrofit.Builder builder
            = new Retrofit.Builder()
            .baseUrl(BASE_URL)
            .addConverterFactory(GsonConverterFactory.create(gson));

    private static Retrofit retrofit = builder.build();

    private static OkHttpClient.Builder httpClient
            = new OkHttpClient.Builder();

    /**
     * Retrieve current token
     *
     * @return the token
     */
    public static String getToken() {
        return token;
    }

    /**
     * Set user credentials
     *
     * @param credentials the user credentials
     */
    public static void setCredentials(Credentials credentials) {

        // set credentials
        APIServiceGenerator.credentials = credentials;

        // Update url
        String serverURI = credentials.getServerURI();
        if (serverURI != null) {
            // add trailing '/' only if needed
            if (serverURI.charAt(serverURI.length() - 1) != '/')
                serverURI += "/";
            BASE_URL = serverURI + "api/";
            builder.baseUrl(BASE_URL);
            retrofit = builder.build();
        }

    }

    /**
     * Build {@link OnAPICallComplete<Token>} for retrieving token
     *
     * @return the {@link OnAPICallComplete<Token>}
     */
    public static OnAPICallComplete<Token> buildRetrieveToken() {

        // Get token
        APIService service = APIServiceGenerator.createService(APIService.class, null);
        retrofit2.Call<Token> call = service.getToken(credentials);
        return new OnAPICallComplete<Token>(call) {

            @Override
            public void onSuccess(Token token) {
                // save token
                APIServiceGenerator.token = token.getToken();
            }

            @Override
            public void onFailure(Throwable t) throws RuntimeException {
                super.onFailure(t);
                throw new RuntimeException(t);
            }

        };

    }

    /**
     * Create service call
     *
     * @param serviceClass the class of the service
     * @param <S> the type of the service
     * @return the service
     */
    public static <S> S createService(Class<S> serviceClass) {
        return createService(serviceClass, APIServiceGenerator.token);
    }

    /**
     * Create service call
     *
     * @param serviceClass the class of the service
     * @param token the authentication token
     * @param <S> the type of the service
     * @return the service
     */
    public static <S> S createService(Class<S> serviceClass, final String token ) {
        if ( token != null ) {

            // save token
            APIServiceGenerator.token = token;

            // add interceptor to inject Authorization header
            httpClient.interceptors().clear();
            httpClient.addInterceptor( chain -> {
                Request original = chain.request();
                Request request = original.newBuilder()
                        .header("Authorization", "Token " + token)
                        .build();
                return chain.proceed(request);
            });
            builder.client(httpClient.build());
            retrofit = builder.build();

        }
        return retrofit.create(serviceClass);
    }

}
