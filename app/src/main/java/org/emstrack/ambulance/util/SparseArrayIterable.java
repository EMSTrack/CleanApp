package org.emstrack.ambulance.util;

import android.util.SparseArray;

import java.util.Iterator;

public class SparseArrayIterable<T> implements Iterable<T> {
    private final SparseArray<T> sparseArray;

    public SparseArrayIterable(SparseArray<T> sparseArray) {
        this.sparseArray = sparseArray;
    }

    @Override
    public Iterator<T> iterator() {
        return new SparseArrayIterator<>(sparseArray);
    }
}
