package org.emstrack.ambulance.dialogs;

import static org.emstrack.ambulance.util.FormatUtils.formatDateTime;

import android.app.Activity;

import androidx.appcompat.app.AlertDialog;

import android.text.method.LinkMovementMethod;
import android.view.View;
import android.widget.TextView;

import org.emstrack.ambulance.BuildConfig;
import org.emstrack.ambulance.R;

import java.text.DateFormat;
import java.util.Calendar;

/**
 * Created by mauricio on 4/26/18.
 */

public class AboutDialog {

    private static final String TAG = AboutDialog.class.getSimpleName();

    public static AlertDialog create(final Activity activity) {

        // Inflate the about message contents
        View messageView = activity.getLayoutInflater().inflate(R.layout.dialog_about, null, false);

        // Set build date
        TextView buildDate = messageView.findViewById(R.id.buildDate);
        Calendar buildDateCalendar = Calendar.getInstance();
        buildDateCalendar.setTimeInMillis(BuildConfig.TIMESTAMP);
        buildDate.setText(formatDateTime(buildDateCalendar, DateFormat.MEDIUM));

        // Set build version
        TextView buildVersion = messageView.findViewById(R.id.buildVersion);
        buildVersion.setText(activity.getString(R.string.app_version).replace('_','.'));

        // enable links
        ((TextView) messageView.findViewById(R.id.about_app_description)).setMovementMethod(LinkMovementMethod.getInstance());
        ((TextView) messageView.findViewById(R.id.about_open_source)).setMovementMethod(LinkMovementMethod.getInstance());
        ((TextView) messageView.findViewById(R.id.about_open_source_icons)).setMovementMethod(LinkMovementMethod.getInstance());
        ((TextView) messageView.findViewById(R.id.about_open_source_servers)).setMovementMethod(LinkMovementMethod.getInstance());

        // Logout dialog
        return new AlertDialog.Builder(activity)
                .setIcon(R.drawable.ambulancelogo)
                .setTitle(R.string.app_name)
                .setView(messageView)
                .setPositiveButton(android.R.string.ok, null)
                .create();

    }

}
