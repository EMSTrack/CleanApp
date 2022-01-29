package org.emstrack.ambulance.util;

import android.view.View;

import androidx.annotation.CallSuper;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.recyclerview.widget.RecyclerView;

public class ViewHolderWithSelectedPosition<T> extends RecyclerView.ViewHolder {

    public interface OnClick<S> {
        void onClick(@NonNull S entry);
    }

    public ViewHolderWithSelectedPosition(@NonNull View view) {
        super(view);
    }

    @CallSuper
    public void set(@NonNull T entry, @Nullable OnClick<T> onClick) {
        // set click listener
        if (onClick != null) {
            itemView.setOnClickListener(v -> onClick.onClick(entry));
        }
    }

    @CallSuper
    public void setSelected(boolean selected) {
        itemView.setSelected(selected);
    }

}
