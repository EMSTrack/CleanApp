package org.emstrack.ambulance.views;

import static org.emstrack.ambulance.util.FormatUtils.formatDate;
import static org.emstrack.ambulance.util.FormatUtils.formatTime;

import android.content.Context;
import android.view.View;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import org.emstrack.ambulance.R;
import org.emstrack.ambulance.adapters.MessageRecyclerAdapter;
import org.emstrack.models.Note;

import java.text.DateFormat;

/**
 * Holds a message
 * @author Mauricio de Oliveira
 * @since 01/02/2022
 */

public class MessageViewHolder extends RecyclerView.ViewHolder {

    private static final String TAG = MessageViewHolder.class.getSimpleName();
    private final TextView messageFrom;
    private final TextView messageText;
    private final TextView messageTimestamp;

    private final int viewType;

    public MessageViewHolder(Context context, View view, int viewType) {
        super(view);
        this.viewType = viewType;
        messageText = view.findViewById(R.id.message_text);
        messageFrom = view.findViewById(R.id.message_from);
        messageTimestamp = view.findViewById(R.id.message_timestamp);
    }

    public void setNote(Note item, @NonNull Context context) {
        // message contents
        if (viewType == MessageRecyclerAdapter.DATE) {
            messageTimestamp.setText(formatDate(item.getUpdatedOn(), DateFormat.MEDIUM));
        } else {
            messageFrom.setText(item.getUpdatedByUsername());
            messageText.setText(item.getComment());
            messageTimestamp.setText(formatTime(item.getUpdatedOn(), DateFormat.SHORT));
        }
    }

}