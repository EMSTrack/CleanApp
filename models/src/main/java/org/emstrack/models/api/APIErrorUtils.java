package org.emstrack.models.api;

import android.util.Log;

import com.google.gson.Gson;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import java.io.IOException;

import okhttp3.ResponseBody;
import retrofit2.Response;

public class APIErrorUtils {

    public static APIError parseError(Response<?> response) {

        JsonObject jsonObject;
        try {

            // Parse message
            jsonObject = new JsonParser()
                    .parse(response.errorBody().charStream())
                    .getAsJsonObject();
        } catch (Exception e) {
            jsonObject = new JsonObject();
            jsonObject.addProperty("error", e.toString());
        }

        return new APIError(jsonObject);

    }
}