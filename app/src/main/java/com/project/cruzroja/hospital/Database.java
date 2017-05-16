package com.project.cruzroja.hospital;

import android.content.Context;

import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.JsonObjectRequest;
import com.android.volley.toolbox.Volley;
import com.google.gson.Gson;
import com.project.cruzroja.hospital.models.Hospital;

import org.json.JSONObject;

/**
 * Created by Fabian Choi on 5/5/2017.
 * Database singleton
 */

public class Database {
    private static Database instance;
    private RequestQueue requestQueue;
    private static Context context;

    Database(Context context) {
        Database.context = context;
        requestQueue = getRequestQueue();
    }

    public static synchronized Database getInstance(Context context) {
        if (instance == null) {
            instance = new Database(context);
        }
        return instance;
    }

    private RequestQueue getRequestQueue() {
        if (requestQueue == null) {
            // getApplicationContext() is key, it keeps you from leaking the
            // Activity or BroadcastReceiver if someone passes one in.
            requestQueue = Volley.newRequestQueue(context.getApplicationContext());
        }
        return requestQueue;
    }

    private <T> void addToRequestQueue(Request<T> req) {
        getRequestQueue().add(req);
    }

    public void requestHospital(int id, final ServerCallback callback) {
        // Instantiate the RequestQueue.
        RequestQueue queue = Volley.newRequestQueue(context.getApplicationContext());
        String url = "http://cruzroja.ucsd.edu/ambulances/api/hospitals/" + id;

        JsonObjectRequest jsObjRequest = new JsonObjectRequest
                (Request.Method.GET, url, null, new Response.Listener<JSONObject>() {
                    @Override
                    public void onResponse(JSONObject r) {
                        Gson gson = new Gson();
                        Hospital hospital = gson.fromJson(r.toString(), Hospital.class);
                        callback.onSuccess(hospital);
                    }
                }, new Response.ErrorListener() {
                    @Override
                    public void onErrorResponse(VolleyError error) {
                        callback.onFailure(error);
                    }
                });
        addToRequestQueue(jsObjRequest);
    }
}