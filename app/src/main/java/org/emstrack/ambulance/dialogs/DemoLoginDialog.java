package org.emstrack.ambulance.dialogs;

import android.app.Activity;
import android.content.DialogInterface;
import android.text.method.LinkMovementMethod;
import android.view.View;
import android.widget.TextView;

import androidx.appcompat.app.AlertDialog;

import org.emstrack.ambulance.R;

/**
 * Created by mauricio on 4/26/18.
 */

public class DemoLoginDialog {

    private static final String TAG = DemoLoginDialog.class.getSimpleName();

    public static AlertDialog create(final Activity activity, DialogInterface.OnClickListener onClickListener) {

        // Inflate the about message contents
        View messageView = activity.getLayoutInflater().inflate(R.layout.dialog_demo, null, false);

        // set demo message
        ((TextView) messageView.findViewById(R.id.demo_message))
                .setText(activity.getString(R.string.demoLoginMessage,
                        activity.getString(android.R.string.ok)));

        // enable links
        ((TextView) messageView.findViewById(R.id.demo_url)).setMovementMethod(LinkMovementMethod.getInstance());

        // Logout dialog
        return new AlertDialog.Builder(activity)
                .setTitle(R.string.demoSession)
                .setView(messageView)
                .setPositiveButton(android.R.string.ok, onClickListener)
                .setNegativeButton(android.R.string.cancel, null)
                .create();

    }

}
