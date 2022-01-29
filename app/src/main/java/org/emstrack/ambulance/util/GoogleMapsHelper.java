package org.emstrack.ambulance.util;

import android.content.Context;
import android.graphics.Point;

import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;

import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.model.BitmapDescriptor;
import com.google.android.gms.maps.model.CameraPosition;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.LatLngBounds;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;

import org.emstrack.ambulance.R;
import org.emstrack.models.Ambulance;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

public class GoogleMapsHelper {

    private static final Map<String, BitmapDescriptor> iconBitmapDescriptors = new HashMap<>();

    public static BitmapDescriptor getMarkerBitmapDescriptor(String key) {
        return iconBitmapDescriptors.get(key);
    }

    public static void initializeMarkers(Context context) {

        if (iconBitmapDescriptors.size() == 0) {

            iconBitmapDescriptors.put(
                    "AMBULANCE_CURRENT",
                    new BitmapUtils.BitmapDescriptorFromVectorBuilder(context,
                            R.drawable.ambulance_red)
                            .setBackground(context, R.drawable.ic_oval_regular)
                            .setBackgroundScale(0.09f)
                            .setBackgroundColor(ContextCompat.getColor(context, R.color.bootstrapSecondary))
                            .setScale(0.1f)
                            .setOffset(9,18)
                            .build());

            iconBitmapDescriptors.put(
                    "AMBULANCE_" + Ambulance.STATUS_AVAILABLE,
                    new BitmapUtils.BitmapDescriptorFromVectorBuilder(context,
                            R.drawable.ambulance_green)
                            .setScale(0.1f)
                            .build());

            iconBitmapDescriptors.put(
                    "AMBULANCE_" + Ambulance.STATUS_OUT_OF_SERVICE,
                    new BitmapUtils.BitmapDescriptorFromVectorBuilder(context,
                            R.drawable.ambulance_gray)
                            .setScale(0.1f)
                            .build());

            iconBitmapDescriptors.put(
                    "AMBULANCE_" + Ambulance.STATUS_UNKNOWN,
                    new BitmapUtils.BitmapDescriptorFromVectorBuilder(context,
                            R.drawable.ambulance_gray)
                            .setScale(0.1f)
                            .build());

            iconBitmapDescriptors.put(
                    "AMBULANCE_" + Ambulance.STATUS_AT_BASE,
                    new BitmapUtils.BitmapDescriptorFromVectorBuilder(context,
                            R.drawable.ambulance_green)
                            .setScale(0.1f)
                            .build());

            iconBitmapDescriptors.put(
                    "AMBULANCE_" + Ambulance.STATUS_BASE_BOUND,
                    new BitmapUtils.BitmapDescriptorFromVectorBuilder(context,
                            R.drawable.ambulance_yellow)
                            .setScale(0.1f)
                            .build());

            iconBitmapDescriptors.put(
                    "AMBULANCE_" + Ambulance.STATUS_AT_HOSPITAL,
                    new BitmapUtils.BitmapDescriptorFromVectorBuilder(context,
                            R.drawable.ambulance_orange)
                            .setScale(0.1f)
                            .build());

            iconBitmapDescriptors.put(
                    "AMBULANCE_" + Ambulance.STATUS_HOSPITAL_BOUND,
                    new BitmapUtils.BitmapDescriptorFromVectorBuilder(context,
                            R.drawable.ambulance_orange)
                            .setScale(0.1f)
                            .build());

            iconBitmapDescriptors.put(
                    "AMBULANCE_" + Ambulance.STATUS_AT_PATIENT,
                    new BitmapUtils.BitmapDescriptorFromVectorBuilder(context,
                            R.drawable.ambulance_red)
                            .setScale(0.1f)
                            .build());

            iconBitmapDescriptors.put(
                    "AMBULANCE_" + Ambulance.STATUS_PATIENT_BOUND,
                    new BitmapUtils.BitmapDescriptorFromVectorBuilder(context,
                            R.drawable.ambulance_red)
                            .setScale(0.1f)
                            .build());

            iconBitmapDescriptors.put(
                    "AMBULANCE_" + Ambulance.STATUS_AT_WAYPOINT,
                    new BitmapUtils.BitmapDescriptorFromVectorBuilder(context,
                            R.drawable.ambulance_blue)
                            .setScale(0.1f)
                            .build());

            iconBitmapDescriptors.put(
                    "AMBULANCE_" + Ambulance.STATUS_WAYPOINT_BOUND,
                    new BitmapUtils.BitmapDescriptorFromVectorBuilder(context,
                            R.drawable.ambulance_blue)
                            .setScale(0.1f)
                            .build());

            iconBitmapDescriptors.put(
                    "HOSPITAL",
                    new BitmapUtils.BitmapDescriptorFromVectorBuilder(context,
                            R.drawable.ic_hospital_15)
                            .setColor(ContextCompat.getColor(context, R.color.bootstrapDark))
                            .build());

            iconBitmapDescriptors.put(
                    "BASE",
                    new BitmapUtils.BitmapDescriptorFromVectorBuilder(context,
                            R.drawable.ic_home_solid)
                            .setColor(ContextCompat.getColor(context, R.color.bootstrapDark))
                            .setScale(0.05f)
                            .build());

            iconBitmapDescriptors.put(
                    "WAYPOINT_" + org.emstrack.models.Location.TYPE_BASE,
                    new BitmapUtils.BitmapDescriptorFromVectorBuilder(context,
                            R.drawable.ic_home_15)
                            .setBackground(context, R.drawable.ic_marker_15)
                            .setBackgroundScale(1.5f)
                            .setBackgroundColor(ContextCompat.getColor(context, R.color.bootstrapSuccess))
                            .setColor(ContextCompat.getColor(context, R.color.bootstrapLight))
                            .setScale(0.85f)
                            .setOffset(15, 5)
                            .build());

            iconBitmapDescriptors.put(
                    "WAYPOINT_" + org.emstrack.models.Location.TYPE_AED,
                    new BitmapUtils.BitmapDescriptorFromVectorBuilder(context,
                            R.drawable.ic_heartbeat_solid)
                            .setBackground(context, R.drawable.ic_marker_15)
                            .setBackgroundScale(1.5f)
                            .setBackgroundColor(ContextCompat.getColor(context, R.color.bootstrapInfo))
                            .setColor(ContextCompat.getColor(context, R.color.bootstrapLight))
                            .setOffset(14, 10)
                            .setScale(.03f)
                            .build());

            iconBitmapDescriptors.put(
                    "WAYPOINT_" + org.emstrack.models.Location.TYPE_INCIDENT,
                    new BitmapUtils.BitmapDescriptorFromVectorBuilder(context,
                            R.drawable.ic_car_crash_solid)
                            .setBackground(context, R.drawable.ic_marker_15)
                            .setBackgroundScale(1.5f)
                            .setBackgroundColor(ContextCompat.getColor(context, R.color.bootstrapDanger))
                            .setColor(ContextCompat.getColor(context, R.color.bootstrapLight))
                            .setOffset(14, 10)
                            .setScale(.03f)
                            .build());

            iconBitmapDescriptors.put(
                    "WAYPOINT_" + org.emstrack.models.Location.TYPE_HOSPITAL,
                    new BitmapUtils.BitmapDescriptorFromVectorBuilder(context,
                            R.drawable.ic_hospital_15)
                            .setBackground(context, R.drawable.ic_marker_15)
                            .setBackgroundScale(1.5f)
                            .setBackgroundColor(ContextCompat.getColor(context, R.color.bootstrapWarning))
                            .setColor(ContextCompat.getColor(context, R.color.bootstrapLight))
                            .setOffset(15, 5)
                            .setScale(.85f)
                            .build());

            iconBitmapDescriptors.put(
                    "WAYPOINT_" + org.emstrack.models.Location.TYPE_WAYPOINT,
                    new BitmapUtils.BitmapDescriptorFromVectorBuilder(context,
                            R.drawable.ic_location_arrow)
                            .setBackground(context, R.drawable.ic_marker_15)
                            .setBackgroundScale(1.5f)
                            .setBackgroundColor(ContextCompat.getColor(context, R.color.bootstrapPrimary))
                            .setColor(ContextCompat.getColor(context, R.color.bootstrapLight))
                            .setOffset(17, 10)
                            .setScale(.03f)
                            .build());

            iconBitmapDescriptors.put(
                    "WAYPOINT_" + org.emstrack.models.Location.TYPE_OTHER,
                    new BitmapUtils.BitmapDescriptorFromVectorBuilder(context,
                            R.drawable.ic_location_arrow)
                            .setBackground(context, R.drawable.ic_marker_15)
                            .setBackgroundScale(1.5f)
                            .setBackgroundColor(ContextCompat.getColor(context, R.color.bootstrapPrimary))
                            .setColor(ContextCompat.getColor(context, R.color.bootstrapLight))
                            .setOffset(17, 10)
                            .setScale(.03f)
                            .build());

        }

    }

    public static void clearMarkers(Map<Integer, Marker> map) {
        Iterator<Map.Entry<Integer,Marker>> iterator = map.entrySet().iterator();
        while (iterator.hasNext())
        {
            // retrieveObject entry
            Map.Entry<Integer,Marker> entry = iterator.next();

            // remove from map
            entry.getValue().remove();

            // remove from collection
            iterator.remove();
        }
    }

    public static void centerMap(@NonNull GoogleMap googleMap,
                                 LatLngBounds bounds, int padding,
                                 int xOffset, int yOffset) {

        // center map without offset first
        centerMap(googleMap, bounds, padding);

        // get center
        LatLng center = bounds.getCenter();

        // center map with offset center
        centerMap(googleMap, center, xOffset, yOffset);

    }

    public static void centerMap(@NonNull GoogleMap googleMap,
                                 LatLngBounds bounds, int padding) {

        // move camera
        googleMap.moveCamera(CameraUpdateFactory.newLatLngBounds(bounds, padding));

    }

    public static void centerMap(@NonNull GoogleMap googleMap,
                                 LatLng target, int xOffset, int yOffset) {
        Point center = googleMap.getProjection().toScreenLocation(target);
        center.set(center.x - xOffset, center.y - yOffset);
        googleMap.moveCamera(CameraUpdateFactory.newLatLng(googleMap.getProjection().fromScreenLocation(center)));
    }

    public static void centerMap(@NonNull GoogleMap googleMap,
                                 LatLng target, int xOffset, int yOffset,
                                 int animateTimeInMs, GoogleMap.CancelableCallback animateCallback) {

        Point center = googleMap.getProjection().toScreenLocation(target);
        center.set(center.x - xOffset, center.y - yOffset);
        googleMap.animateCamera(CameraUpdateFactory.newLatLng(googleMap.getProjection().fromScreenLocation(center)), animateTimeInMs, animateCallback);
    }

    public static void centerMap(@NonNull GoogleMap googleMap,
                                 LatLng latLng,
                                 float bearing,
                                 float zoomLevel,
                                 int xOffset, int yOffset,
                                 boolean dropMarker,
                                 int animateTimeInMs,
                                 GoogleMap.CancelableCallback animateCallback) {

        float currentZoomLevel = googleMap.getCameraPosition().zoom;
        double scale = 1f;
        if (zoomLevel != currentZoomLevel) {
            // scale offset
            scale = Math.pow(2, currentZoomLevel - zoomLevel);
            xOffset = (int) scale * xOffset;
            yOffset = (int) scale * yOffset;
        }

        Point center = googleMap.getProjection().toScreenLocation(latLng);
        center.set(center.x - xOffset, center.y - yOffset);
        centerMap(googleMap, googleMap.getProjection().fromScreenLocation(center),
                bearing, zoomLevel, dropMarker, animateTimeInMs, animateCallback);
    }

    public static void centerMap(@NonNull GoogleMap googleMap,
                                 LatLng latLng,
                                 float bearing,
                                 float zoomLevel,
                                 boolean dropMarker,
                                 int animateTimeInMs,
                                 GoogleMap.CancelableCallback animateCallback) {

        if (dropMarker) {
            googleMap.addMarker(new MarkerOptions().position(latLng));
        }


        if (animateTimeInMs > 0) {
            CameraPosition cameraPosition = new CameraPosition.Builder()
                    .target(latLng)
                    .bearing(bearing)
                    .zoom(zoomLevel)
                    .build();
            googleMap.animateCamera(CameraUpdateFactory.newCameraPosition(cameraPosition), animateTimeInMs, animateCallback);
        } else {
            googleMap.moveCamera(
                    CameraUpdateFactory.newCameraPosition(
                            new CameraPosition(latLng, zoomLevel, 0, bearing)));
        }

    }

    public static LatLng addToLatLng(LatLng latLng, double dLatitude, double dLongitude) {

        // add to latitude
        double lat;
        if (dLatitude >= 0) {
            lat = latLng.latitude + (latLng.latitude + dLatitude < 90 ? dLatitude : 0);
        } else {
            lat = latLng.latitude + (latLng.latitude + dLatitude > -90 ? dLatitude : 0);
        }

        // add to longitude
        double lng = latLng.longitude + dLongitude;
        if (lng > 180) {
            lng -= 360;
        } else if (lng < -180) {
            lng += 360;
        }

        return new LatLng(lat, lng);
    }

    public static LatLngBounds padBounds(LatLngBounds bounds, double padding) {

        // get dimensions
        LatLng southeast = new LatLng(bounds.southwest.latitude, bounds.northeast.longitude);
        float height = (float) LatLon.calculateDistanceHaversine(bounds.northeast, southeast);
        float width = (float) LatLon.calculateDistanceHaversine(bounds.southwest, southeast);

        // calculate padding
        double dLatitude = LatLon.distanceToDegrees(padding * height) / 2;
        double dLongitude = LatLon.distanceToDegrees(padding * width) / 2;

        // set new corners
        return new LatLngBounds.Builder()
                .include(addToLatLng(bounds.northeast, dLatitude, dLongitude))
                .include(addToLatLng(bounds.southwest, -dLatitude, -dLongitude))
                .build();

    }

    public static LatLngBounds matchMapBounds(LatLngBounds bounds, int mapHeight, int mapWidth) {

        // get dimensions
        LatLng southeast = new LatLng(bounds.southwest.latitude, bounds.northeast.longitude);
        float height = (float) LatLon.calculateDistanceHaversine(bounds.northeast, southeast);
        float width = (float) LatLon.calculateDistanceHaversine(bounds.southwest, southeast);
        float aspectRatio = height / width;

        float mapAspectRatio = ((float) mapHeight) / mapWidth;

        double dLatitude = 0, dLongitude = 0;
        if (mapAspectRatio >= aspectRatio) {
            // match screen width, add to height (latitude)
            dLatitude = LatLon.distanceToDegrees(width * mapAspectRatio - height) / 2;
        } else {
            // match screen height, add to width (longitude)
            dLongitude = LatLon.distanceToDegrees(height / mapAspectRatio - width) / 2;
        }

        // set new corners
        return new LatLngBounds.Builder()
                .include(addToLatLng(bounds.northeast, dLatitude, dLongitude))
                .include(addToLatLng(bounds.southwest, -dLatitude, -dLongitude))
                .build();

    }

    public static LatLngBounds padAndMatchBounds(LatLngBounds bounds, double padding, int mapHeight, int mapWidth) {

        return matchMapBounds(padBounds(bounds, padding), mapHeight, mapWidth);

    }

}
