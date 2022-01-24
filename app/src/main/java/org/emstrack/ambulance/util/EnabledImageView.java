package org.emstrack.ambulance.util;

import android.content.res.Resources;
import android.view.View;
import android.widget.ImageView;

import androidx.annotation.ColorInt;

public class EnabledImageView {

    private final ImageView imageView;
    int disabledColor;
    int enabledColor;

    public EnabledImageView(ImageView imageView, @ColorInt int enabledColor, @ColorInt int disabledColor) {
        this.imageView = imageView;
        this.enabledColor = enabledColor;
        this.disabledColor = disabledColor;
    }

    public ImageView getImageView() {
        return imageView;
    }

    public void setDisabledColor(@ColorInt int disabledColor) {
        this.disabledColor = disabledColor;
    }

    public void setEnabledColor(@ColorInt int enabledColor) {
        this.enabledColor = enabledColor;
    }

    public void setEnabled(boolean enabled) {
        imageView.setEnabled(enabled);
        if (imageView.isEnabled()) {
            imageView.setColorFilter(enabledColor);
        } else {
            imageView.setColorFilter(disabledColor);
        }
    }

    public void setOnClickListener(View.OnClickListener onClickListener) {
        imageView.setOnClickListener(v -> {
            if (v.isEnabled()) {
                onClickListener.onClick(v);
            }
        });

    }
}
