package org.emstrack.ambulance.util;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.drawable.Drawable;

import androidx.core.content.ContextCompat;

import com.google.android.gms.maps.model.BitmapDescriptor;
import com.google.android.gms.maps.model.BitmapDescriptorFactory;

public class BitmapUtils {

    /*
     * This is from
     * https://stackoverflow.com/questions/42365658/custom-marker-in-google-maps-in-android-with-vector-asset-icon
     */
    public static Bitmap bitmapFromVector(Context context, int vectorResId, double scale) {
        Drawable vectorDrawable = ContextCompat.getDrawable(context, vectorResId);
        int width = Math.round((float) Math.ceil(vectorDrawable.getIntrinsicWidth()*scale));
        int height = Math.round((float) Math.ceil(vectorDrawable.getIntrinsicHeight()*scale));
        vectorDrawable.setBounds(0, 0, width, height);
        return Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888);
    }

    public static Bitmap bitmapFromVector(Context context, int vectorResId) {
        return bitmapFromVector(context, vectorResId, 1);
    }

    public static BitmapDescriptor bitmapDescriptorFromVector(Context context, int vectorResId, double scale) {
        Drawable vectorDrawable = ContextCompat.getDrawable(context, vectorResId);
        int width = Math.round((float) Math.ceil(vectorDrawable.getIntrinsicWidth()*scale));
        int height = Math.round((float) Math.ceil(vectorDrawable.getIntrinsicHeight()*scale));
        vectorDrawable.setBounds(0, 0, width, height);
        Bitmap bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888);
        Canvas canvas = new Canvas(bitmap);
        vectorDrawable.draw(canvas);
        return BitmapDescriptorFactory.fromBitmap(bitmap);
    }

    public static BitmapDescriptor bitmapDescriptorFromVector(Context context, int vectorResId) {
        return bitmapDescriptorFromVector(context, vectorResId, 1);
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