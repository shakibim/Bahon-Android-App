package com.example.bahon;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.TextView;

import java.util.List;

public class RechargeAdapter extends BaseAdapter {

    private Context context;
    private List<RechargeData> rechargeDataList;

    public RechargeAdapter(Context context, List<RechargeData> rechargeDataList) {
        this.context = context;
        this.rechargeDataList = rechargeDataList;
    }

    @Override
    public int getCount() {
        return rechargeDataList.size();
    }

    @Override
    public Object getItem(int position) {
        return rechargeDataList.get(position);
    }

    @Override
    public long getItemId(int position) {
        return position;
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        if (convertView == null) {
            convertView = LayoutInflater.from(context).inflate(R.layout.recharge_item, parent, false);
        }

        // Get the current item
        RechargeData rechargeData = rechargeDataList.get(position);

        // Find views in the layout and set the data
        View blueSquare = convertView.findViewById(R.id.blue_shape);
        TextView tvDateTime = convertView.findViewById(R.id.tvDateTime);
        TextView tvAmount = convertView.findViewById(R.id.tvAmount);

        // Set the date and amount
        tvDateTime.setText(rechargeData.getDateTime());
        tvAmount.setText(rechargeData.getAmount());

        return convertView;
    }
}
