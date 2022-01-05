package org.emstrack.ambulance.views;

import android.content.Context;
import android.view.View;
import android.widget.RelativeLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.RecyclerView;

import org.emstrack.ambulance.R;
import org.emstrack.ambulance.adapters.MessageRecyclerAdapter;
import org.emstrack.models.Note;

import java.text.SimpleDateFormat;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.time.format.FormatStyle;
import java.time.temporal.TemporalAccessor;

/**
 * Holds a message
 * @author Mauricio de Oliveira
 * @since 01/02/2022
 */

public class MessageRecyclerViewViewHolder extends RecyclerView.ViewHolder {

    private static final String TAG = MessageRecyclerViewViewHolder.class.getSimpleName();
    private final View messageLayout;
    private final TextView messageFrom;
    private final TextView messageText;
    private final TextView messageTimestamp;

    private static final SimpleDateFormat timeFormat = new SimpleDateFormat("HH:mm");
    private static final SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd");

    private final int viewType;

    public MessageRecyclerViewViewHolder(Context context, View view, int viewType) {
        super(view);
        this.viewType = viewType;
        messageLayout = view.findViewById(R.id.message_layout);
        messageText = view.findViewById(R.id.message_text);
        messageFrom = view.findViewById(R.id.message_from);
        messageTimestamp = view.findViewById(R.id.message_timestamp);
    }

    public void setNote(Note item, @NonNull Context context) {
        // message contents
        if (viewType == MessageRecyclerAdapter.DATE) {
            String date;
            if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
                DateTimeFormatter dateFormatter = DateTimeFormatter.ofLocalizedDate(FormatStyle.SHORT);
                date = dateFormatter.format(item.getUpdatedOn()
                        .toInstant()
                        .atZone(ZoneId.systemDefault())
                        .toLocalDate());
            } else {
                date = dateFormat.format(item.getUpdatedOn().getTime());
            }
            messageTimestamp.setText(date);
        } else {
            messageFrom.setText(item.getUpdatedByUsername());
            messageText.setText(item.getComment());
            String time;
            if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
                DateTimeFormatter timeFormatter = DateTimeFormatter.ofLocalizedTime(FormatStyle.SHORT);
                time = timeFormatter.format(item.getUpdatedOn()
                        .toInstant()
                        .atZone(ZoneId.systemDefault())
                        .toLocalTime());
            } else {
                time = timeFormat.format(item.getUpdatedOn().getTime());
            }
            messageTimestamp.setText(time);
        }
    }

}