package org.emstrack.ambulance.util;

import android.util.SparseArray;

public abstract class SparseArrayUtils {
    public static <T> Iterable<T> iterable(SparseArray<T> sparseArray) {
        return new SparseArrayIterable<>(sparseArray);
    }
}
