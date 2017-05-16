package com.project.cruzroja.hospital;

import com.android.volley.VolleyError;
import com.project.cruzroja.hospital.models.Hospital;

/**
 * Created by Fabian Choi on 5/5/2017.
 * Callback on server's done request
 */

public interface ServerCallback {
    void onSuccess(Hospital result);
    void onFailure(VolleyError error);
}
