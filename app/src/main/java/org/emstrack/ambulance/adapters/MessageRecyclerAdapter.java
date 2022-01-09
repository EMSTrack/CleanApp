package org.emstrack.ambulance.adapters;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import org.emstrack.ambulance.R;
import org.emstrack.ambulance.views.MessageViewHolder;
import org.emstrack.models.DateNote;
import org.emstrack.models.Note;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

/**
 * Connects Equipment data to the RecyclerView (called from EquipmentFragment)
 * @author James Basa
 * @since 2/17/2020
 */

public class MessageRecyclerAdapter extends RecyclerView.Adapter<MessageViewHolder> {

    private static final String TAG = MessageRecyclerAdapter.class.getSimpleName();

    public static final int SELF = 0;
    public static final int OTHER = 1;
    public static final int DATE = 2;

    private final Context context;
    private final String username;
    private final List<Note> messages;

    public MessageRecyclerAdapter(Context context, List<? extends Note> messages, String username) {
        this.context = context;
        this.messages = new ArrayList<>(messages);
        this.username = username;

        // sort and split messages
        splitMessagesByDate();
    }

    private void sortMessages() {
        Collections.sort(messages, new Note.SortAscending());
    }

    private void addBlankAt(int index) {
        Note message = messages.get(index);
        if (message != null) {
            messages.add(index, new DateNote(message));
        }
    }

    public static boolean isSameDay(Calendar calendar1, Calendar calendar2) {
        return calendar1.get(Calendar.YEAR) == calendar2.get(Calendar.YEAR)
                && calendar1.get(Calendar.MONTH) == calendar2.get(Calendar.MONTH)
                && calendar1.get(Calendar.DAY_OF_MONTH) == calendar2.get(Calendar.DAY_OF_MONTH);
    }

    private void splitMessagesByDate() {
        // quick return
        if (messages.size() == 0) {
            return;
        }

        // sort first
        sortMessages();

        // add first blank
        addBlankAt(0);

        for (int i = 2; i < messages.size(); i++) {
            Note message = messages.get(i);
            if (!isSameDay(messages.get(i-1).getUpdatedOn(), message.getUpdatedOn())) {
                // add blank if days are different
                addBlankAt(i);
                i++;
            }
        }

    }

    @Override
    public int getItemViewType(int position) {
        Note item = messages.get(position);
        if (item instanceof DateNote) {
            return DATE;
        } else if (username.equals(item.getUpdatedByUsername())) {
            return SELF;
        }
        return OTHER;
    }

    @NonNull
    @Override
    public MessageViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view;
        if (viewType == SELF) {
            view = LayoutInflater.from(parent.getContext()).inflate(R.layout.right_message_item, parent, false);
        } else if (viewType == DATE) {
            view = LayoutInflater.from(parent.getContext()).inflate(R.layout.date_message_item, parent, false);
        } else { // if (viewType == OTHER) {
            view = LayoutInflater.from(parent.getContext()).inflate(R.layout.left_message_item, parent, false);
        }
        return new MessageViewHolder(context, view, viewType);
    }

    @Override
    public void onBindViewHolder(@NonNull MessageViewHolder holder, int position) {
        Note item = messages.get(position);
        holder.setNote(item, context);
    }

    @Override
    public int getItemCount() {
        return messages.size();
    }

}
