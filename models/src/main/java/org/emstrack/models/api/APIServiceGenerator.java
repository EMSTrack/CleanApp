package org.emstrack.models.api;

import android.os.Build;
import android.support.v4.os.LocaleListCompat;

import com.google.gson.FieldNamingPolicy;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

import org.emstrack.models.gson.ExcludeAnnotationExclusionStrategy;

import java.util.Locale;
import java.util.logging.Level;

import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.logging.HttpLoggingInterceptor;
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
    private static String BASE_URL = "https://cruzroja.ucsd.edu/api/";

    private static Gson gson = new GsonBuilder()
            .setFieldNamingPolicy(FieldNamingPolicy.LOWER_CASE_WITH_UNDERSCORES)
            .setExclusionStrategies(new ExcludeAnnotationExclusionStrategy())
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
     * Set current token
     *
     * @param token the token
     */
    public static void setToken(String token) {
        APIServiceGenerator.token = token;
    }

    /**
     * Set server URI
     *
     * @param serverURI the server URI
     */
    public static void setServerUri(String serverURI) {

        // Update url
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

            // clear interceptors
            httpClient.interceptors().clear();

            // add interceptor to inject Authorization header
            httpClient.addInterceptor( chain -> {
                Request original = chain.request();
                Request request = original.newBuilder()
                        .header("Authorization", "Token " + token)
                        .build();
                return chain.proceed(request);
            });

            // add interceptor to inject accepted languages
            httpClient.addInterceptor(
                    chain -> {
                        Request original = chain.request();
                        Request request = original.newBuilder()
                                .header("Accept-Language", APIServiceGenerator.getLanguage() )
                                .build();
                        return chain.proceed(request);
                    });

            HttpLoggingInterceptor logging = new HttpLoggingInterceptor();
            //logging.setLevel(HttpLoggingInterceptor.Level.BODY);
            logging.setLevel(HttpLoggingInterceptor.Level.BASIC);
            httpClient.addInterceptor(logging);

            builder.client(httpClient.build());
            retrofit = builder.build();

        } else {

            // reset interceptors

            httpClient.interceptors().clear();
            builder.client(httpClient.build());
            retrofit = builder.build();

        }
        return retrofit.create(serviceClass);
    }

    private static String getLanguage() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            return LocaleListCompat.getDefault().toLanguageTags();
        } else {
            return Locale.getDefault().getLanguage();
        }
    }

}
