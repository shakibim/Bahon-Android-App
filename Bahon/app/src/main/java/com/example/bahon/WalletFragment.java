package com.example.bahon;

import android.app.Dialog;
import android.content.Intent;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.fragment.app.Fragment;

import com.github.mikephil.charting.charts.BarChart;
import com.github.mikephil.charting.components.XAxis;
import com.github.mikephil.charting.components.YAxis;
import com.github.mikephil.charting.data.BarData;
import com.github.mikephil.charting.data.BarDataSet;
import com.github.mikephil.charting.data.BarEntry;
import com.github.mikephil.charting.formatter.IndexAxisValueFormatter;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.Query;
import com.google.firebase.firestore.QueryDocumentSnapshot;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class WalletFragment extends Fragment {

    private BarChart barChart;
    private ListView rechargeListView;
    private Button rechargeButton, seeAllReButton;
    private TextView walletBalanceAmount, rechargeCountTextView;
    private FirebaseFirestore firestore;
    private Dialog loadingDialog;
    private ExecutorService executorService;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_wallet, container, false);

        // Initialize Firestore
        firestore = FirebaseFirestore.getInstance();

        // Initialize the loading dialog
        loadingDialog = new Dialog(requireContext());
        loadingDialog.setContentView(R.layout.dialog_loading);
        loadingDialog.setCancelable(false);
        loadingDialog.getWindow().setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
        // Find views
        walletBalanceAmount = view.findViewById(R.id.walletBalanceAmount);
        rechargeListView = view.findViewById(R.id.rechargeListView);
        rechargeButton = view.findViewById(R.id.recharge_button);
        seeAllReButton = view.findViewById(R.id.seeallr_recharge_button);
        barChart = view.findViewById(R.id.barChart);
        rechargeCountTextView = view.findViewById(R.id.recargeCountin24h);

        executorService = Executors.newFixedThreadPool(5); // 3 threads for 3 functions

        // Execute each function in a separate thread
        showLoading();
        executorService.execute(this::loadWalletBalance);
        executorService.execute(this::loadRechargeHistory);
        executorService.execute(this::setupBarChart);
        executorService.execute(this::loadBarChartData);
        executorService.execute(this::loadRechargeCountInLast24Hours);

        // Set click listeners
        rechargeButton.setOnClickListener(v -> {
            Intent intent = new Intent(getActivity(), Recharge.class);
            startActivity(intent);
            requireActivity().overridePendingTransition(android.R.anim.fade_in, android.R.anim.fade_out);
        });

        seeAllReButton.setOnClickListener(v -> {
            Intent intent = new Intent(getActivity(), AllRecharge.class);
            startActivity(intent);
            requireActivity().overridePendingTransition(android.R.anim.fade_in, android.R.anim.fade_out);
        });

        return view;
    }

    private void showLoading() {
        loadingDialog.show();
    }

    private void hideLoading() {
        if (loadingDialog.isShowing()) {
            loadingDialog.dismiss();
        }
    }

    private void loadWalletBalance() {
        String userId = FirebaseAuth.getInstance().getCurrentUser().getUid();
        firestore.collection("users").document(userId).get()
                .addOnSuccessListener(documentSnapshot -> {
                    if (documentSnapshot.exists()) {
                        Double balance = documentSnapshot.getDouble("balance");
                        String formattedBalance = String.format("%.2f", balance);
                        walletBalanceAmount.setText(balance != null ? "৳ " + formattedBalance : "0.00");
                    } else {
                        walletBalanceAmount.setText("0.00");
                    }
                    hideLoading();
                })
                .addOnFailureListener(e -> {
                    walletBalanceAmount.setText("Failed to load balance");
                    hideLoading();
                });
    }

    // Method to load recharge history from Firestore
    private void loadRechargeHistory() {
        String userId = FirebaseAuth.getInstance().getCurrentUser().getUid();
        firestore.collection("users").document(userId).collection("recharge_history")
                .orderBy("timestamp", Query.Direction.DESCENDING)
                .get()
                .addOnSuccessListener(querySnapshot -> {
                    List<RechargeData> rechargeDataList = new ArrayList<>();
                    SimpleDateFormat sdf = new SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.getDefault());

                    for (QueryDocumentSnapshot doc : querySnapshot) {
                        // Format the timestamp to a readable date
                        Long timestamp = doc.getLong("timestamp");
                        String date = timestamp != null ? sdf.format(new Date(timestamp)) : "N/A";

                        // Get the amount as a double, format it as a string if needed
                        Double amount = doc.getDouble("amount");
                        String amountStr = amount != null ? String.format(Locale.getDefault(), "%.2f", amount) : "0.00";

                        rechargeDataList.add(new RechargeData(date, "৳ " + amountStr));
                    }

                    // Set up the adapter with the loaded data
                    RechargeAdapter adapter = new RechargeAdapter(getContext(), rechargeDataList);
                    rechargeListView.setAdapter(adapter);
                })
                .addOnFailureListener(e -> {
                    // Log error and show a toast message
                    Log.e("RechargeHistory", "Failed to load recharge history", e);
                    Toast.makeText(getContext(), "Failed to load recharge history", Toast.LENGTH_SHORT).show();
                });
    }

    private void setupBarChart() {
        barChart.setDrawBarShadow(false);
        barChart.setDrawValueAboveBar(true);
        barChart.setPinchZoom(false);
        barChart.setDrawGridBackground(false);
        barChart.getDescription().setText("");

        XAxis xAxis = barChart.getXAxis();
        xAxis.setPosition(XAxis.XAxisPosition.BOTTOM);
        xAxis.setDrawGridLines(false);
        xAxis.setGranularity(1f);

        YAxis leftAxis = barChart.getAxisLeft();
        leftAxis.setDrawGridLines(false);
        leftAxis.setAxisMinimum(0f);

        YAxis rightAxis = barChart.getAxisRight();
        rightAxis.setEnabled(false);
    }

    private void loadBarChartData() {
        String userId = FirebaseAuth.getInstance().getCurrentUser().getUid();
        firestore.collection("users").document(userId).collection("recharge_history")
                .get()
                .addOnSuccessListener(querySnapshot -> {
                    Map<String, Float> rechargeData = new HashMap<>();
                    SimpleDateFormat sdf = new SimpleDateFormat("MMM dd", Locale.getDefault());

                    for (QueryDocumentSnapshot doc : querySnapshot) {
                        Long timestamp = doc.getLong("timestamp");
                        Double amount = doc.getDouble("amount");

                        if (timestamp != null && amount != null) {
                            String date = sdf.format(new Date(timestamp));
                            rechargeData.put(date, rechargeData.getOrDefault(date, 0f) + amount.floatValue());
                        }
                    }

                    ArrayList<BarEntry> entries = new ArrayList<>();
                    ArrayList<String> labels = new ArrayList<>();
                    int index = 0;

                    // Sort the data by date (ascending)
                    ArrayList<Map.Entry<String, Float>> sortedEntries = new ArrayList<>(rechargeData.entrySet());
                    sortedEntries.sort((entry1, entry2) -> entry1.getKey().compareTo(entry2.getKey()));

                    for (Map.Entry<String, Float> entry : sortedEntries) {
                        labels.add(entry.getKey()); // Date
                        entries.add(new BarEntry(index++, entry.getValue())); // Amount
                    }

                    BarDataSet dataSet = new BarDataSet(entries, "Recharge Data");
                    dataSet.setColor(Color.parseColor("#1976D2"));
                    BarData data = new BarData(dataSet);

                    barChart.setData(data);
                    barChart.getXAxis().setValueFormatter(new IndexAxisValueFormatter(labels));
                    barChart.invalidate();
                })
                .addOnFailureListener(e -> {
                    Toast.makeText(getContext(), "Failed to load chart data", Toast.LENGTH_SHORT).show();
                });
    }

    private void loadRechargeCountInLast24Hours() {
        String userId = FirebaseAuth.getInstance().getCurrentUser().getUid();
        long currentTime = System.currentTimeMillis();
        long twentyFourHoursAgo = currentTime - (24 * 60 * 60 * 1000); // 24 hours in milliseconds

        // Query Firestore for recharges in the last 24 hours
        firestore.collection("users").document(userId).collection("recharge_history")
                .whereGreaterThanOrEqualTo("timestamp", twentyFourHoursAgo)
                .get()
                .addOnSuccessListener(querySnapshot -> {
                    int rechargeCount = querySnapshot.size(); // Get the number of recharges in last 24 hours
                    // Update the TextView with the count
                    rechargeCountTextView.setText(rechargeCount + " recharges in last 24 hours");
                })
                .addOnFailureListener(e -> {
                    // Handle error
                    rechargeCountTextView.setText("Error fetching recharges");
                });
    }
}
