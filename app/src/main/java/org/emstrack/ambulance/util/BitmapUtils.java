package org.emstrack.ambulance.util;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.VectorDrawable;
import android.os.Build;
import android.util.Log;

import androidx.annotation.ColorInt;
import androidx.annotation.RequiresApi;
import androidx.appcompat.content.res.AppCompatResources;
import androidx.core.content.ContextCompat;
import androidx.core.graphics.drawable.DrawableCompat;

import com.google.android.gms.maps.model.BitmapDescriptor;
import com.google.android.gms.maps.model.BitmapDescriptorFactory;

public class BitmapUtils {

    public static final String TAG = BitmapUtils.class.getSimpleName();

    /*
     * Inspired by
     * https://stackoverflow.com/questions/42365658/custom-marker-in-google-maps-in-android-with-vector-asset-icon
     */

    @RequiresApi(api = Build.VERSION_CODES.LOLLIPOP)
    public static class BitmapDescriptorFromVectorBuilder {

        private Drawable backgroundDrawable;
        private float backgroundScale = 1f;
        private boolean setBackgroundColor = false;
        private int backgroundColor = 0;

        private final Drawable foregroundDrawable;
        private float scale = 1f;
        private boolean setColor = false;
        private int color = 0;
        private int left = 0;
        private int top = 0;

        public BitmapDescriptorFromVectorBuilder(Context context, int vectorResId) {
            foregroundDrawable = ContextCompat.getDrawable(context, vectorResId);
        }

        public BitmapDescriptorFromVectorBuilder setBackground(Context context, int vectorResId) {
            backgroundDrawable = ContextCompat.getDrawable(context, vectorResId);
            return this;
        }

        public BitmapDescriptorFromVectorBuilder setBackgroundColor(@ColorInt int backgroundColor) {
            this.backgroundColor = backgroundColor;
            this.setBackgroundColor = true;
            return this;
        }

        public int getBackgroundColor() {
            return backgroundColor;
        }

        public BitmapDescriptorFromVectorBuilder setBackgroundScale(float backgroundScale) {
            this.backgroundScale = backgroundScale;
            return this;
        }

        public float getBackgroundScale() {
            return backgroundScale;
        }

        public BitmapDescriptorFromVectorBuilder setScale(float scale) {
            this.scale = scale;
            return this;
        }

        public float getScale() {
            return scale;
        }

        public BitmapDescriptorFromVectorBuilder setLeft(int left) {
            this.left = left;
            return this;
        }

        public int getLeft() {
            return left;
        }

        public BitmapDescriptorFromVectorBuilder setTop(int top) {
            this.top = top;
            return this;
        }

        public int getTop() {
            return top;
        }

        public BitmapDescriptorFromVectorBuilder setOffset(int left, int top) {
            this.left = left;
            this.top = top;
            return this;
        }

        public BitmapDescriptorFromVectorBuilder setColor(@ColorInt int color) {
            this.color = color;
            this.setColor = true;
            return this;
        }

        public int getColor() {
            return color;
        }

        public BitmapDescriptor build() {

            // throw exception in case of null resource
            if (foregroundDrawable == null) {
                throw new RuntimeException("Requested vector was not found");
            }

            // background
            int backgroundWidth = -1;
            int backgroundHeight = -1;
            if (backgroundDrawable != null) {

                // scale background
                backgroundWidth = backgroundDrawable.getIntrinsicWidth();
                backgroundHeight = backgroundDrawable.getIntrinsicHeight();
                if (backgroundScale != 1f) {
                    backgroundWidth = Math.round((float) Math.ceil(backgroundWidth * backgroundScale));
                    backgroundHeight = Math.round((float) Math.ceil(backgroundHeight * backgroundScale));
                }

                // set bounds
                backgroundDrawable.setBounds(0, 0, backgroundWidth, backgroundHeight);

                // color
                if (setBackgroundColor) {
                    backgroundDrawable.setTint(backgroundColor);
                }

            }

            // scale foreground
            int foregroundWidth = foregroundDrawable.getIntrinsicWidth();
            int foregroundHeight = foregroundDrawable.getIntrinsicHeight();
            if (scale != 1f) {
                foregroundWidth = Math.round((float) Math.ceil(foregroundWidth * scale));
                foregroundHeight = Math.round((float) Math.ceil(foregroundHeight * scale));
            }

            // set bounds
            foregroundDrawable.setBounds(left, top, foregroundWidth + left, foregroundHeight + top);

            // color
            if (setColor) {
                foregroundDrawable.setTint(color);
            }

            // create bitmap
            Bitmap bitmap;
            if (backgroundDrawable != null) {
                bitmap = Bitmap.createBitmap(backgroundWidth, backgroundHeight, Bitmap.Config.ARGB_8888);
                Canvas canvas = new Canvas(bitmap);
                backgroundDrawable.draw(canvas);
                foregroundDrawable.draw(canvas);
            } else {
                bitmap = Bitmap.createBitmap(foregroundWidth, foregroundHeight, Bitmap.Config.ARGB_8888);
                Canvas canvas = new Canvas(bitmap);
                foregroundDrawable.draw(canvas);
            }

            return BitmapDescriptorFactory.fromBitmap(bitmap);
        }
    }

    /**
     * This method converts dp unit to equivalent pixels, depending on device density.
     *
     * @param dp      A value in dp (density independent pixels) unit. Which we need to convert into pixels
     * @param context Context to get resources and device specific display metrics
     * @return A float value to represent px equivalent to dp depending on device density
     */
    public static float dpToPixels(Context context, int dp) {
        return dp * context.getResources().getDisplayMetrics().density;
    }

}