package org.emstrack.ambulance.util;

import android.util.SparseArray;

import androidx.annotation.NonNull;

import java.util.Iterator;

public class SparseArrayIterable<T> implements Iterable<T> {
    private final SparseArray<T> sparseArray;

    public SparseArrayIterable(@NonNull SparseArray<T> sparseArray) {
        this.sparseArray = sparseArray;
    }

    @NonNull
    @Override
    public Iterator<T> iterator() {
        return new SparseArrayIterator<>(sparseArray);
    }

    public static <T> Iterable<T> iterable(SparseArray<T> sparseArray) {
        return new SparseArrayIterable<>(sparseArray);
    }

}
