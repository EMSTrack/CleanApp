package org.emstrack.ambulance.util;

import android.location.Location;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collections;
import java.util.List;

import static org.emstrack.ambulance.util.LatLon.calculateDistanceAndBearing;
import static org.emstrack.ambulance.util.LatLon.stationaryRadius;
import static org.emstrack.ambulance.util.LatLon.stationaryVelocity;

/**
 * Created by mauricio on 3/22/2018.
 */

public class VehicleUpdateFilter {

    private static final String TAG = VehicleUpdateFilter.class.getSimpleName();

    private VehicleUpdate currentVehicleUpdate;
    private List<VehicleUpdate> filteredVehicleUpdates;

    public VehicleUpdateFilter() {
        this(null);
    }

    public VehicleUpdateFilter(VehicleUpdate location) {
        this.currentVehicleUpdate = location;
        this.filteredVehicleUpdates = new ArrayList<>();
    }

    public void setCurrentVehicleUpdate(VehicleUpdate currentVehicleUpdate) {
        this.currentVehicleUpdate = currentVehicleUpdate;
    }

    public VehicleUpdate getCurrentVehicleUpdate() {
        return currentVehicleUpdate;
    }

    public List<VehicleUpdate> getFilteredUpdates() {
        return this.filteredVehicleUpdates;
    }

    public boolean hasUpdates() {
        return this.filteredVehicleUpdates.size() > 0;
    }

    public void reset() {
        this.filteredVehicleUpdates = new ArrayList<>();
    }

    public void sort() {
        Collections.sort(this.filteredVehicleUpdates,
                new VehicleUpdate.SortByAscendingOrder());
    }

    /**
     * Update current position based on a new measurement
     *
     * @param update the updateAmbulance
     */
    private void _update(Location update) {

        // return if null
        if (currentVehicleUpdate.getLocation() == null) {
            // Log.d(TAG, "Null location, skipping...");
            return;
        }

        // elapsed time
        double dt = update.getTime() - currentVehicleUpdate.getTimestamp().getTimeInMillis();

        // Predict next currentAmbulanceUpdate
        // GPSLocation prediction = updateLocation(currentAmbulanceUpdate, bearing, velocity * dt);

        // measure velocity and bearing
        double[] dandb = calculateDistanceAndBearing(currentVehicleUpdate.getLocation(), update);
        double distance = dandb[0];
        double brn = dandb[1];
        double vel = currentVehicleUpdate.getVelocity();
        if (dt > 0)
            vel = distance / dt;

        // ambulanceUpdateFilter velocity
        double Kv = 0.9;
        double velocity = currentVehicleUpdate.getVelocity();
        velocity += Kv * (vel - velocity);
        currentVehicleUpdate.setVelocity(velocity);

        // ambulanceUpdateFilter bearing
        double Kb = 0.9;
        double bearing = currentVehicleUpdate.getBearing();
        bearing += Kb * (brn - bearing);
        currentVehicleUpdate.setBearing(bearing);

        if ((velocity > stationaryVelocity && distance > stationaryRadius) ||
                (velocity <= stationaryVelocity && distance > 3 * stationaryRadius)) {

            // updateAmbulance currentAmbulanceUpdate
            currentVehicleUpdate.setLocation(update);
            currentVehicleUpdate.setTimestamp(update.getTime());

            // add currentAmbulanceUpdate to filtered locations
            filteredVehicleUpdates.add(new VehicleUpdate(currentVehicleUpdate));

        }

        // Log.i(TAG, "velocity = " + velocity + ", distance = " + distance + ", bearing = " + bearing + "(" + update.getBearing() + ")");

    }

    public void update(String status) {

        this.filteredVehicleUpdates.add(new VehicleUpdate(status));

    }

    public void update(String status, Calendar timestamp) {

        this.filteredVehicleUpdates.add(new VehicleUpdate(status, timestamp));

    }

    public void update(Location location) {

        // initialize
        if (this.currentVehicleUpdate == null) {
            // use first record
            this.currentVehicleUpdate = new VehicleUpdate(location);
            return;
        }

        // update records
        _update(location);

    }

    public void update(List<Location> locations) {

        // Fast return if no updates
        if (locations == null || locations.size() == 0)
            return;

        // initialize
        if (currentVehicleUpdate == null)
            // use first record
            currentVehicleUpdate = new VehicleUpdate(locations.get(0));

        // loop through records
        for (Location location : locations)
            _update(location);

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
     * Extended Kalman ambulanceUpdateFilter
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
     * Covariance updateAmbulance:
     *
     * P(tk+ = P(tk+|tk+) = (I - Kk Hk) P(tk+|tk)
     *
     */


}
