package org.emstrack.ambulance.util;

import static java.lang.Integer.getInteger;

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.view.View;

public class AnimationHelper {

    public static void crossfade(View in, View out, int shortAnimationDuration) {

        // Set the content view to 0% opacity but visible, so that it is visible
        // (but fully transparent) during the animation.
        in.setAlpha(0f);
        in.setVisibility(View.VISIBLE);

        // Animate the content view to 100% opacity, and clear any animation
        // listener set on the view.
        in.animate()
                .alpha(1f)
                .setDuration(shortAnimationDuration)
                .setListener(null);

        // Animate the loading view to 0% opacity. After the animation ends,
        // set its visibility to GONE as an optimization step (it won't
        // participate in layout passes, etc.)
        out.animate()
                .alpha(0f)
                .setDuration(shortAnimationDuration)
                .setListener(new AnimatorListenerAdapter() {
                    @Override
                    public void onAnimationEnd(Animator animation) {
                        out.setVisibility(View.GONE);
                    }
                });
    }

}
