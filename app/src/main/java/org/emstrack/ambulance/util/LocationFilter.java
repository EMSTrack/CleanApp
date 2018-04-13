package org.emstrack.ambulance.util;

import android.location.Location;
import android.util.Log;

import org.ejml.data.DMatrix5x5;
import org.ejml.simple.SimpleMatrix;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import static org.emstrack.ambulance.util.LatLon.calculateBearing;
import static org.emstrack.ambulance.util.LatLon.calculateDistanceAndBearing;
import static org.emstrack.ambulance.util.LatLon.calculateDistanceHaversine;
import static org.emstrack.ambulance.util.LatLon.stationaryRadius;
import static org.emstrack.ambulance.util.LatLon.stationaryVelocity;
import static org.emstrack.ambulance.util.LatLon.updateLocation;

/**
 * Created by mauricio on 3/22/2018.
 */

public class LocationFilter {

    private static final String TAG = LocationFilter.class.getSimpleName();

    private LocationUpdate location;

    public LocationFilter(LocationUpdate location) {
        this.location = location;
    }

    public void setLocation(LocationUpdate location) {
        this.location = location;
    }

    /**
     * Update current position based on a new measurement
     *
     * @param update the update
     */
    public void update(Location update, List<LocationUpdate> filteredLocations) {

        // elapsed time
        double dt = update.getTime() - location.getTimestamp().getTime();

        // Predict next location
        // Location prediction = updateLocation(location, bearing, velocity * dt);

        // measure velocity and bearing
        double[] dandb = calculateDistanceAndBearing(location.getLocation(), update);
        double distance = dandb[0];
        double brn = dandb[1];
        double vel = location.getVelocity();
        if (dt > 0)
            vel = distance / dt;

        // locationFilter velocity
        double Kv = 0.9;
        double velocity = location.getVelocity();
        velocity += Kv * (vel - velocity);
        location.setVelocity(velocity);

        // locationFilter bearing
        double Kb = 0.9;
        double bearing = location.getBearing();
        bearing += Kb * (brn - bearing);
        location.setBearing(bearing);

        if ((velocity > stationaryVelocity && distance > stationaryRadius) ||
                (velocity <= stationaryVelocity && distance > 3 * stationaryRadius)) {

            // update location
            location.setLocation(update);
            location.setTimestamp(new Date(update.getTime()));

            // add location to filtered locations
            filteredLocations.add(new LocationUpdate(location));

        }

        Log.i(TAG, "velocity = " + velocity + ", distance = " + distance + ", bearing = " + bearing + "(" + update.getBearing() + ")");

}

    public List<LocationUpdate> update(List<Location> locations) {

        // Fast return if no updates
        List<LocationUpdate> filteredLocations = new ArrayList<>();
        if (locations == null || locations.size() == 0)
            return filteredLocations;

        // initialize
        if (location == null)
            // use first record
            location = new LocationUpdate(locations.get(0));

        // loop through records
        for (Location location : locations)
            update(location, filteredLocations);

        return filteredLocations;
    }

    /**
     * Model is that of a constant forward velocity and constant angular velocity
     *
     * xDot(t) = v(t) cos(theta(t))
     * yDot(t) = v(t) cos(theta(t))
     * thetaDot(t) = thetaDot(t)
     * thetaDotDot(t) = 0
     * vDot(t) = 0
     *
     * Discretizing at t+ = tk + dt, t = tk, dt = t+ - t, we obtain the model:
     *
     * x(tk+), y(tk+) = f(x(tk), y(tk), theta(tk-), v(tk) dt)
     * theta(tk+) = theta(tk) + thetaDot(tk) dt
     * thetaDot(tk+) = thetaDot(tk)
     * v(tk+) = v(tk)
     *
     * or
     *
     * X = (x, y, theta, thetaDot, v)
     * X(tk+) = F(X(tk),tk),
     * Z(tk) = G(X(tk),tk)
     *
     * Partials:
     *
     * f1: x(tk+) = x(tk) + dt v(tk) cos(theta(tk))
     * f2: y(tk+) = y(tk) + dt v(tk) sin(theta(tk))
     * f3: theta(tk+) = theta(tk) + thetaDot(tk) dt
     * f4: thetaDot(tk+) = thetaDot(tk)
     * f5: v(tk+) = v(tk)
     *
     * Fk = dF/dx
     *    = [1, 0, -dt*v(tk)*sin(theta(tk)), 0, dt*cos(theta(tk));
     *       0, 1, dt*v(tk)*cos(theta(tk)), 0, dt*sin(theta(tk));
     *       0, 0, 1, dt, 0;
     *       0, 0, 0, 1, 0;
     *       0, 0, 0, 0, 1]
     *
     * g1: z1(tk) = x(tk)
     * g2: z2(tk) = y(tk)
     *
     * Hk = dG/dx
     *    = [1, 0, 0, 0;
     *       0, 1, 0, 0];
     *
     *
     * Extended Kalman locationFilter
     *
     * Prediction:
     *
     * xHat(tk+|tk), yHat(tk+|tk) = f(xHat(tk), yHat(tk), thetaHat(tk), vHat(tk) tk)
     * thetaHat(tk+|tk) = thetaHat(tk) + thetaDotHat(tk) dt
     * thetaDotHat(tk+|tk) = thetaDotHat(tk)
     * vHat(tk+|tk) = vHat(tk)
     *
     * or
     *
     * XHat(tk+|tk) = F(XHat(tk),tk)
     *
     * Covariance prediction:
     *
     * P(tk+|tk) = Fk P(tk) Fk' + Qk
     *
     * At time tk we obtain a measurement ot z(tk) = (x(tk), y(tk))
     *
     * Update:
     *
     * Sk = Hk P(tk|tk) Hk' + Rk
     * Kk = P(tk+|tk) Hk' inv(Sk)
     *
     * XHat(tk+) = XHat(tk+|tk) + K (z(tk) - zHat(tk))
     *
     * Covariance update:
     *
     * P(tk+ = P(tk+|tk+) = (I - Kk Hk) P(tk+|tk)
     *
     */


}
