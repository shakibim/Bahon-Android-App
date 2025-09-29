package com.example.bahon;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.TextView;

import java.util.List;

public class PassengerAdapter extends BaseAdapter {

    private Context context;
    private List<Passenger> passengers;

    public PassengerAdapter(Context context, List<Passenger> passengers) {
        this.context = context;
        this.passengers = passengers;
    }

    @Override
    public int getCount() {
        return passengers.size();
    }

    @Override
    public Object getItem(int position) {
        return passengers.get(position);
    }

    @Override
    public long getItemId(int position) {
        return position;
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        if (convertView == null) {
            convertView = LayoutInflater.from(context).inflate(R.layout.list_item_passenger, parent, false);
        }

        Passenger passenger = passengers.get(position);

        TextView nameTextView = convertView.findViewById(R.id.passenger_name);
        TextView cardNoTextView = convertView.findViewById(R.id.passenger_card_number);

        nameTextView.setText(" Passenger Name: "+passenger.getName());
        cardNoTextView.setText(" Card Number: "+passenger.getCard_no());

        return convertView;
    }
}