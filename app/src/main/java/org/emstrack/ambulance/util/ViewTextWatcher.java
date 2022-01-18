package org.emstrack.ambulance.util;

import android.text.Editable;
import android.text.TextWatcher;
import android.view.View;

import androidx.annotation.CallSuper;

public class ViewTextWatcher implements TextWatcher {

    private final View view;

    public ViewTextWatcher(View view) {
        this.view = view;
    }

    @CallSuper
    @Override
    public void onTextChanged(CharSequence charSequence, int i, int i1, int i2) {
        view.setEnabled(charSequence.toString().trim().length() != 0);
    }

    @Override
    public void beforeTextChanged(CharSequence charSequence, int i, int i1, int i2) {
    }

    @Override
    public void afterTextChanged(Editable editable) {
    }

}
