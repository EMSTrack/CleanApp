package org.emstrack.ambulance.adapters;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.TextView;

import java.util.List;

import org.emstrack.ambulance.R;

public class StatusSpinnerAdapter extends BaseAdapter {

    List<String> status;
    List<Integer> colors;
    Context context;

    public StatusSpinnerAdapter(Context context, List<String> status, List<Integer> colors)
    {
        this.context = context;
        this.status = status;
        this.colors = colors;
        int retrieve [] = context.getResources().getIntArray(R.array.statusColors);
        for(int re:retrieve)
            colors.add(re);
    }

    @Override
    public int getCount() {
        return colors.size();
    }

    @Override
    public Object getItem(int arg0) {
        return colors.get(arg0);
    }

    @Override
    public long getItemId(int arg0) {
        return arg0;
    }

    @Override
    public View getView(int pos, View view, ViewGroup parent)
    {
        LayoutInflater inflater=LayoutInflater.from(context);
        view = inflater.inflate(R.layout.status_spinner_dropdown_item, null);
        TextView textView = (TextView) view.findViewById(R.id.statusSpinnerDropdownItemText);
        textView.setBackgroundColor(colors.get(pos));
        textView.setTextSize(context.getResources().getDimension(R.dimen.statusTextSize));
        textView.setText(status.get(pos));
        return view;
    }

}