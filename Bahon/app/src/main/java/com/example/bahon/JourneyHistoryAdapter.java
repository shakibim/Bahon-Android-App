package com.example.bahon;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;

import java.util.List;

public class JourneyHistoryAdapter extends ArrayAdapter<JourneyHistory> {

    public JourneyHistoryAdapter(@NonNull Context context, List<JourneyHistory> historyList) {
        super(context, 0, historyList);
    }

    @NonNull
    @Override
    public View getView(int position, @Nullable View convertView, @NonNull ViewGroup parent) {
        if (convertView == null) {
            convertView = LayoutInflater.from(getContext()).inflate(R.layout.journey_history_item, parent, false);
        }

        JourneyHistory history = getItem(position);

        TextView dateTimeView = convertView.findViewById(R.id.dateTimeTextView);
        TextView journeyDetailsView = convertView.findViewById(R.id.journeyDetailsTextView);
        TextView fareView = convertView.findViewById(R.id.fareTextView);

        dateTimeView.setText(history.getDateTime());
        journeyDetailsView.setText(history.getJourneyDetails());
        fareView.setText(history.getFare());

        return convertView;
    }
}