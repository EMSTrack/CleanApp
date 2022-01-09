package org.emstrack.ambulance.views;

import static org.emstrack.ambulance.util.DateUtils.formatDateTime;

import android.app.Activity;
import android.content.Context;
import android.util.Log;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.recyclerview.widget.RecyclerView;

import org.emstrack.ambulance.MainActivity;
import org.emstrack.ambulance.R;
import org.emstrack.ambulance.models.AmbulanceAppData;
import org.emstrack.ambulance.services.AmbulanceForegroundService;
import org.emstrack.models.AmbulanceCall;
import org.emstrack.models.Call;
import org.emstrack.models.PriorityCode;
import org.emstrack.models.RadioCode;

import java.text.DateFormat;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.time.format.FormatStyle;
import java.util.Locale;

/**
 * Holds the call data
 * @author Mauricio de Oliveira
 * @since 1/7/2022
 */

public class CallViewHolder extends RecyclerView.ViewHolder {

    private static final String TAG = CallViewHolder.class.getSimpleName();

    private final View callDetailView;

    private final ImageView callSuspendedThumbnail;
    private final ImageView callRequestedThumbnail;
    private final ImageView callCurrentThumbnail;
    private final TextView callUpdatedOn;

    private final TextView callPriorityTextView;
    private final TextView callPriorityPrefixTextView;
    private final TextView callPrioritySuffixTextView;
    private final TextView callRadioCodeText;
    private final TextView callDetailsText;
    private final TextView callNumberOfWaypointsText;
    private final TextView callNumberOfPatientsText;
    private final TextView callNumberOfMessagesText;
    private final ImageView callLoginThumbnail;
    private final ImageView callLogoutThumbnail;
    private final View view;

    public CallViewHolder(Context context, View view) {
        super(view);

        // save view
        this.view = view;

        callSuspendedThumbnail = view.findViewById(R.id.callSuspendedThumbnail);
        callRequestedThumbnail = view.findViewById(R.id.callRequestedThumbnail);
        callCurrentThumbnail = view.findViewById(R.id.callCurrentThumbnail);

        callPriorityTextView = view.findViewById(R.id.callPriorityTextView);
        callPriorityPrefixTextView = view.findViewById(R.id.callPriorityPrefix);
        callPrioritySuffixTextView = view.findViewById(R.id.callPrioritySuffix);

        callUpdatedOn = view.findViewById(R.id.callUpdatedOn);

        callDetailView = view.findViewById(R.id.call_detail);

        callRadioCodeText = callDetailView.findViewById(R.id.callRadioCodeText);
        callDetailsText = callDetailView.findViewById(R.id.callDetailsText);
        callNumberOfWaypointsText = callDetailView.findViewById(R.id.callNumberOfWaypointsText);
        callNumberOfPatientsText = callDetailView.findViewById(R.id.callNumberOfPatientsText);
        callNumberOfMessagesText = callDetailView.findViewById(R.id.callNumberOfMessagesText);

        callLoginThumbnail = callDetailView.findViewById(R.id.callLogin);
        callLogoutThumbnail = callDetailView.findViewById(R.id.callLogout);

    }

    public void toggleDetailView() {

        // toggle visibility of the detail view
        if (callDetailView.getVisibility() == View.VISIBLE) {
            callDetailView.setVisibility(View.GONE);
        } else {
            callDetailView.setVisibility(View.VISIBLE);
        }

    }

    public void setCall(Call call, AmbulanceCall ambulanceCall, Activity activity) {

        // get main activity
        MainActivity mainActivity = (MainActivity) activity;

        // get app data
        AmbulanceAppData appData = AmbulanceForegroundService.getAppData();

        switch (ambulanceCall.getStatus()) {
            case AmbulanceCall.STATUS_ACCEPTED:
                // call is current call
                callCurrentThumbnail.setVisibility(View.VISIBLE);
                callRequestedThumbnail.setVisibility(View.GONE);
                callSuspendedThumbnail.setVisibility(View.GONE);
                callDetailView.setVisibility(View.VISIBLE);
                break;
            case AmbulanceCall.STATUS_SUSPENDED:
                // call is suspended
                callCurrentThumbnail.setVisibility(View.GONE);
                callRequestedThumbnail.setVisibility(View.GONE);
                callSuspendedThumbnail.setVisibility(View.VISIBLE);
                callDetailView.setVisibility(View.GONE);
                break;
            case AmbulanceCall.STATUS_REQUESTED:
                // call is requested
                callCurrentThumbnail.setVisibility(View.GONE);
                callRequestedThumbnail.setVisibility(View.VISIBLE);
                callSuspendedThumbnail.setVisibility(View.GONE);
                callDetailView.setVisibility(View.GONE);
                break;
            default:
                Log.d(TAG, "Unknown status = " + ambulanceCall.getStatus());
                break;
        }

        switch (ambulanceCall.getStatus()) {
            case AmbulanceCall.STATUS_ACCEPTED:
                // call is current call
                callLoginThumbnail.setVisibility(View.GONE);
                callLogoutThumbnail.setVisibility(View.VISIBLE);
                callLogoutThumbnail.setOnClickListener(v -> mainActivity.promptEndCallDialog(call.getId()));
                // click navigates to call
                view.setOnClickListener(v -> mainActivity.navigate(R.id.callFragment));
                break;
            case AmbulanceCall.STATUS_SUSPENDED:
                // call is suspended
            case AmbulanceCall.STATUS_REQUESTED:
                // call is requested
                callLogoutThumbnail.setVisibility(View.GONE);
                callLoginThumbnail.setVisibility(View.VISIBLE);
                callLoginThumbnail.setOnClickListener(v -> mainActivity.promptCallAccept(call.getId()));
                // click toggles details
                view.setOnClickListener(v -> toggleDetailView());
                break;
            default:
                Log.d(TAG, "Unknown status = " + ambulanceCall.getStatus());
                break;
        }

        // set call priority
        String priority = call.getPriority();
        callPriorityTextView.setText(priority);

        // set call priority
        int priorityCodeInt = call.getPriorityCode();
        if (priorityCodeInt < 0) {
            callPriorityPrefixTextView.setText("");
            callPrioritySuffixTextView.setText("");
        } else {
            PriorityCode priorityCode = appData.getPriorityCodes().get(priorityCodeInt);
            callPriorityPrefixTextView.setText(String.format(Locale.ENGLISH, "%d-", priorityCode.getPrefix()));
            callPrioritySuffixTextView.setText(String.format("-%s", priorityCode.getSuffix()));
        }

        // set call priority colors
        try {
            callPriorityTextView.setBackgroundColor(mainActivity.getCallPriorityBackgroundColors().get(priority));
            callPriorityTextView.setTextColor(mainActivity.getCallPriorityForegroundColors().get(priority));
        } catch (NullPointerException e) {
            Log.d(TAG, "Could not set colors");
        }

        // set call updated on
        callUpdatedOn.setText(formatDateTime(ambulanceCall.getUpdatedOn(), DateFormat.SHORT));

        // Set radio code
        int radioCodeInt = call.getRadioCode();
        if (radioCodeInt < 0) {
            callRadioCodeText.setText(R.string.unavailable);
        } else {
            RadioCode radioCode = appData.getRadioCodes().get(radioCodeInt);
            callRadioCodeText.setText(String.format(Locale.ENGLISH, "%d: %s", radioCode.getId(), radioCode.getLabel()));
        }

        // set details
        callDetailsText.setText(call.getDetails());

        // set number of waypoints
        callNumberOfWaypointsText.setText(String.valueOf(ambulanceCall.getWaypointSet().size()));

        // set number of patients
        callNumberOfPatientsText.setText(String.valueOf(call.getPatientSet().size()));

        // set number of messages
        callNumberOfMessagesText.setText(String.valueOf(call.getCallnoteSet().size()));
    }

}