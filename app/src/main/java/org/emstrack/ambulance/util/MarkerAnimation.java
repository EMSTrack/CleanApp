package org.emstrack.ambulance.util;

/* Copyright 2013 Google Inc.
   Licensed under Apache 2.0: http://www.apache.org/licenses/LICENSE-2.0.html */

// inspired on https://gist.github.com/broady/6314689

import android.animation.ObjectAnimator;
import android.animation.PropertyValuesHolder;
import android.animation.TypeEvaluator;
import android.animation.ValueAnimator;
import android.annotation.TargetApi;
import android.os.Build;
import android.os.Handler;
import android.os.SystemClock;
import android.util.Property;
import android.view.animation.AccelerateDecelerateInterpolator;
import android.view.animation.Interpolator;

import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.Marker;

public class MarkerAnimation {

    private static float whichWayToTurn(float currentDirection, float targetDirection) {
        float diff = targetDirection - currentDirection;
        if (Math.abs(diff) == 0) {
            return 0;
        }
        if(diff > 180) {
            return -1;
        } else {
            return 1;
        }
    }

    private static float calculateFinalRotation(float startRotation, float finalRotation) {
        float dAngle = finalRotation - startRotation;
        if (dAngle > 180) {
            return finalRotation - 360;
        } else if (dAngle < -180) {
            return finalRotation + 360;
        }
        return finalRotation;
    }

    public static void animateMarkerToGB(final Marker marker, final LatLng finalPosition, final float finalRotation, final int durationInMs, final LatLngInterpolator latLngInterpolator) {

        final LatLng startPosition = marker.getPosition();

        final Handler handler = new Handler();
        final long start = SystemClock.uptimeMillis();

        final Interpolator interpolator = new AccelerateDecelerateInterpolator();

        final float startRotation = marker.getRotation();
        final float angle = 180 - Math.abs(Math.abs(startRotation - finalRotation) - 180);
        final float right = whichWayToTurn(startRotation, finalRotation);

        handler.post(new Runnable() {
            long elapsed;
            float t;
            float v;

            @Override
            public void run() {
                // Calculate progress using interpolator
                elapsed = SystemClock.uptimeMillis() - start;
                t = elapsed / (1f * durationInMs);
                v = interpolator.getInterpolation(t);

                marker.setPosition(latLngInterpolator.interpolate(v, startPosition, finalPosition));
                marker.setRotation(startRotation + right * v * angle);

                // Repeat till progress is complete.
                if (t < 1) {
                    // Post again 16ms later.
                    handler.postDelayed(this, 16);
                }
            }
        });
    }

    @TargetApi(Build.VERSION_CODES.HONEYCOMB)
    public static void animateMarkerToHC(final Marker marker, final LatLng finalPosition, final float finalRotation, final int durationInMs, final LatLngInterpolator latLngInterpolator) {
        final LatLng startPosition = marker.getPosition();

        final float startRotation = marker.getRotation();
        final float angle = 180 - Math.abs(Math.abs(startRotation - finalRotation) - 180);
        final float right = whichWayToTurn(startRotation, finalRotation);

        ValueAnimator valueAnimator = new ValueAnimator();
        valueAnimator.addUpdateListener(animation -> {
            float v = animation.getAnimatedFraction();
            LatLng newPosition = latLngInterpolator.interpolate(v, startPosition, finalPosition);
            marker.setPosition(newPosition);
            marker.setRotation(startRotation + right * v * angle);
        });
        valueAnimator.setFloatValues(0, 1); // Ignored.
        valueAnimator.setDuration(durationInMs);
        valueAnimator.start();
    }

    @TargetApi(Build.VERSION_CODES.ICE_CREAM_SANDWICH)
    public static ObjectAnimator animateMarkerToICS(Marker marker, LatLng finalPosition, float finalRotation, final int durationInMs, final LatLngInterpolator latLngInterpolator) {
        TypeEvaluator<LatLng> typeEvaluator = (fraction, startValue, endValue) -> latLngInterpolator.interpolate(fraction, startValue, endValue);
        // setup animator
        Property<Marker, LatLng> position = Property.of(Marker.class, LatLng.class, "position");
        PropertyValuesHolder positionProperty = PropertyValuesHolder.ofObject(position, typeEvaluator, finalPosition);
        PropertyValuesHolder rotationProperty = PropertyValuesHolder.ofFloat("rotation",
                calculateFinalRotation(marker.getRotation(), finalRotation));
        ObjectAnimator animator = ObjectAnimator.ofPropertyValuesHolder(marker, positionProperty, rotationProperty);
        animator.setDuration(durationInMs);
        animator.setAutoCancel(true);
        animator.start();

        return animator;
    }
}