package org.emstrack.ambulance.util;

import android.util.SparseArray;

import java.util.Iterator;

public class SparseArrayIterator<T> implements Iterator<T> {
    private final SparseArray<T> array;
    private int index;

    public SparseArrayIterator(SparseArray<T> array) {
        this.array = array;
    }

    @Override
    public boolean hasNext() {
        return array.size() > index;
    }

    @Override
    public T next() {
        return array.valueAt(index++);
    }

    @Override
    public void remove() {
        array.removeAt(index);
    }

}
