package org.emstrack.gps;

import com.google.android.gms.location.LocationSettingsResponse;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;

/**
 * Created by mauricio on 2/22/2018.
 */

public interface LocationSettingsCallback extends OnSuccessListener<LocationSettingsResponse>, OnFailureListener {

}
