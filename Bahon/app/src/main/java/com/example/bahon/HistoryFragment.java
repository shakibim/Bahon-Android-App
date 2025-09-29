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

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.google.android.gms.tasks.Task;
import com.google.firebase.Timestamp;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QuerySnapshot;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.HashMap;
import com.github.mikephil.charting.charts.LineChart;
import com.github.mikephil.charting.components.XAxis;
import com.github.mikephil.charting.components.YAxis;
import com.github.mikephil.charting.data.Entry;
import com.github.mikephil.charting.data.LineData;
import com.github.mikephil.charting.data.LineDataSet;
import com.github.mikephil.charting.formatter.IndexAxisValueFormatter;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicReference;


public class HistoryFragment extends Fragment {

    private ListView historyListView;
    private Button seeAllHistoryButton;
    private List<JourneyHistory> historyList;
    private JourneyHistoryAdapter adapter;
    private FirebaseFirestore db;
    private LineChart lineChart;
    private Dialog loadingDialog;
    private ExecutorService executorService;


    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_history, container, false);

        // Initialize Firestore
        db = FirebaseFirestore.getInstance();
        loadingDialog = new Dialog(requireContext());
        loadingDialog.setContentView(R.layout.dialog_loading);
        loadingDialog.setCancelable(false);
        loadingDialog.getWindow().setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
        // Initialize ListView and Adapter
        historyListView = view.findViewById(R.id.historyListView);
        seeAllHistoryButton = view.findViewById(R.id.see_all_history_button);
        historyList = new ArrayList<>();
        adapter = new JourneyHistoryAdapter(getContext(), historyList);
        historyListView.setAdapter(adapter);
        // Initialize LineChart
        lineChart = view.findViewById(R.id.spendingLineChart);

        executorService = Executors.newFixedThreadPool(3); // 3 threads for 3 functions

        // Execute each function in a separate thread
        showLoading();
        executorService.execute(this::setupLineChart);
        executorService.execute(this::loadLineChartData);
        executorService.execute(this::fetchJourneyDataFromFirestore);

        // Set up the "See All History" button
        seeAllHistoryButton.setOnClickListener(v -> {
            Intent intent = new Intent(getActivity(), AllJourneys.class);
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

    private void setupLineChart() {

        lineChart.getDescription().setEnabled(false);
        lineChart.setDrawGridBackground(false);
        lineChart.setDrawBorders(false);

        // Configure X-Axis
        XAxis xAxis = lineChart.getXAxis();
        xAxis.setPosition(XAxis.XAxisPosition.BOTTOM);
        xAxis.setDrawGridLines(false);
        xAxis.setGranularity(1f); // one day intervals

        // Configure Y-Axis
        YAxis leftAxis = lineChart.getAxisLeft();
        leftAxis.setDrawGridLines(false);
        lineChart.getAxisRight().setEnabled(false);
        // Disable right Y-axis
    }
    private void loadLineChartData() {

        String userId = FirebaseAuth.getInstance().getCurrentUser().getUid();
        FirebaseFirestore db = FirebaseFirestore.getInstance();

        // Fetching journey data from Firestore
        db.collection("users")
                .document(userId)
                .collection("journeys")
                .get()
                .addOnSuccessListener(querySnapshot -> {
                    Map<String, Float> dailyFareData = new HashMap<>();
                    SimpleDateFormat sdf = new SimpleDateFormat("MMM dd", Locale.getDefault());

                    float totalSpendThisMonth = 0f; // Variable to hold total spend in the current month
                    int journeyCountLast30Days = 0; // Variable to hold number of journeys in the last 30 days
                    long currentTimeMillis = System.currentTimeMillis(); // Current time in milliseconds
                    long thirtyDaysAgoMillis = currentTimeMillis - (30L * 24 * 60 * 60 * 1000); // 30 days ago

                    for (DocumentSnapshot document : querySnapshot) {
                        try {
                            // Extract fare (amount) and entry time
                            Double fareValue = document.getDouble("fare");
                            if (fareValue == null) {
                                fareValue = 0.0;  // Default if fare is null or invalid
                            }

                            Long entryTimeMillis = null;
                            if (document.contains("entry_time._seconds") && document.contains("entry_time._nanoseconds")) {
                                Long seconds = document.getLong("entry_time._seconds");
                                Long nanoseconds = document.getLong("entry_time._nanoseconds");
                                if (seconds != null && nanoseconds != null) {
                                    entryTimeMillis = seconds * 1000 + nanoseconds / 1000000;
                                }
                            }
                            if (entryTimeMillis == null) {
                                continue;
                            }

                            // Get the entry date for the chart
                            String entryDate = sdf.format(new Date(entryTimeMillis));
                            dailyFareData.put(entryDate, dailyFareData.getOrDefault(entryDate, 0f) + fareValue.floatValue());

                            // Check if this journey is in the current month and last 30 days
                            Calendar calendar = Calendar.getInstance();
                            calendar.setTimeInMillis(entryTimeMillis);
                            int currentMonth = Calendar.getInstance().get(Calendar.MONTH);
                            int journeyMonth = calendar.get(Calendar.MONTH);

                            // If the journey is in the current month, add to the total spend
                            if (journeyMonth == currentMonth) {
                                totalSpendThisMonth += fareValue;
                            }

                            // If the journey is within the last 30 days, increment the count
                            if (entryTimeMillis >= thirtyDaysAgoMillis) {
                                journeyCountLast30Days++;
                            }

                        } catch (Exception e) {
                            Log.e("Firebase", "Error parsing journey data: " + document.getId(), e);
                        }
                    }

                    // Update total spending and journey count TextViews
                    TextView totalSpendingTextView = getView().findViewById(R.id.totalSpendingTextView);
                    totalSpendingTextView.setText("৳ " + String.format("%.2f", totalSpendThisMonth));

                    TextView journeysLast30DaysTextView = getView().findViewById(R.id.journeysLast30DaysTextView);
                    journeysLast30DaysTextView.setText(journeyCountLast30Days + " Journeys in Last 30 Days");

                    ArrayList<Entry> entries = new ArrayList<>();
                    ArrayList<String> labels = new ArrayList<>();
                    int index = 0;

                    // Sort the data by date
                    ArrayList<Map.Entry<String, Float>> sortedEntries = new ArrayList<>(dailyFareData.entrySet());
                    sortedEntries.sort((entry1, entry2) -> entry1.getKey().compareTo(entry2.getKey()));

                    // Adding entries to the line chart
                    for (Map.Entry<String, Float> entry : sortedEntries) {
                        labels.add(entry.getKey()); // Date
                        entries.add(new Entry(index++, entry.getValue())); // Fare (amount)
                    }

                    // Set up the LineDataSet
                    LineDataSet dataSet = new LineDataSet(entries, "Daily Journey Fare");
                    dataSet.setColor(Color.parseColor("#1976D2")); // Set the line color
                    dataSet.setLineWidth(2f); // Line width for better visibility
                    dataSet.setDrawCircles(true); // Show circles at each data point
                    dataSet.setCircleColor(Color.parseColor("#1E88E5")); // Circle color
                    dataSet.setCircleRadius(4f); // Circle size
                    dataSet.setDrawValues(false); // Optionally hide values on the data points
                    dataSet.setMode(LineDataSet.Mode.LINEAR); // Linear line for connecting points

                    // Set up the LineData
                    LineData lineData = new LineData(dataSet);

                    // Set the line chart data
                    lineChart.setData(lineData);

                    // Configure X-Axis
                    lineChart.getXAxis().setValueFormatter(new IndexAxisValueFormatter(labels)); // Label the x-axis with dates
                    lineChart.getXAxis().setGranularity(1f); // Ensure no overlapping of x-axis labels
                    lineChart.getXAxis().setPosition(XAxis.XAxisPosition.BOTTOM);

                    // Configure Y-Axis
                    lineChart.getAxisLeft().setTextColor(Color.BLACK); // Set Y-Axis label color to black
                    lineChart.getAxisRight().setEnabled(false); // Disable the right Y-Axis (only left Y-Axis)

                    lineChart.getDescription().setEnabled(false); // Disable the chart description for a cleaner look
                    lineChart.getLegend().setEnabled(true); // Enable the chart legend
                    lineChart.getLegend().setTextColor(Color.BLACK); // Set legend text color to black
                    lineChart.setTouchEnabled(true); // Enable touch gestures

                    // Animate chart if desired
                    lineChart.animateX(1500); // Animate the chart (optional)

                })
                .addOnFailureListener(e -> {
                    Toast.makeText(getContext(), "Failed to load line chart data", Toast.LENGTH_SHORT).show();

                });
    }



    private void fetchJourneyDataFromFirestore() {

        String currentUserId = FirebaseAuth.getInstance().getCurrentUser().getUid();

        db.collection("users")
                .document(currentUserId)
                .collection("journeys")
                .orderBy("exit_time", com.google.firebase.firestore.Query.Direction.DESCENDING)
                .get()
                .addOnCompleteListener(task -> handleFirestoreResponse(task));
    }

    private void handleFirestoreResponse(Task<QuerySnapshot> task) {

        if (task.isSuccessful()) {
            QuerySnapshot querySnapshot = task.getResult();
            if (querySnapshot != null && !querySnapshot.isEmpty()) {
                for (DocumentSnapshot document : querySnapshot) {
                    try {
                        // Extract relevant fields
                        String entryPoint = document.getString("entry_point");
                        String exitPoint = document.getString("exit_point");
                        Double distanceTravelled = document.getDouble("distance_travelled_km");
                        Double fareValue = document.getDouble("fare");

                        // Extract time fields
                        Long entrySeconds = getNestedLong(document, "entry_time._seconds");
                        Long exitTimeMillis = document.getDate("exit_time").getTime();

                        // Handle missing or invalid fields
                        if (entryPoint == null || exitPoint == null || distanceTravelled == null || fareValue == null || entrySeconds == null || exitTimeMillis == null) {
                            Log.e("Firestore", "Missing fields in document: " + document.getId());
                            continue; // Skip this document
                        }

                        // Truncate entry and exit points to 15 characters and add ellipsis
                        entryPoint = truncateWithEllipsis(entryPoint, 30);
                        exitPoint = truncateWithEllipsis(exitPoint, 30);

                        // Format entry and exit times
                        String entryTime = formatTimestamp(entrySeconds);
                        String exitTime = formatExitTime(exitTimeMillis);

                        // Time range
                        String timeRange = entryTime + " - " + exitTime;

                        // Format fare and distance
                        String fare = "৳" + String.format(Locale.getDefault(), "%.1f", fareValue);

                        String distance = String.format(Locale.getDefault(), "%.3f KM", distanceTravelled);

                        // Create journey summary
                        String journeyDetails = entryPoint + " To " + exitPoint + ", " + distance;
                        hideLoading();
                        // Add to history list
                        historyList.add(new JourneyHistory(timeRange, journeyDetails, fare));
                    } catch (Exception e) {
                        Log.e("Firestore", "Error parsing journey data: " + document.getId(), e);
                    }
                }
                // Notify adapter of data changes
                adapter.notifyDataSetChanged();
            } else {
                Toast.makeText(getContext(), "No journey data found.", Toast.LENGTH_SHORT).cancel();
            }
        } else {
            Log.e("Firestore", "Error getting documents: ", task.getException());
            Toast.makeText(getContext(), "Failed to load journey data.", Toast.LENGTH_SHORT).cancel();
        }
        hideLoading();
    }

    private String truncateWithEllipsis(String text, int maxLength) {
        return text.length() > maxLength ? text.substring(0, maxLength) + "..." : text;
    }

    private String formatTimestamp(Long seconds) {
        Date date = new Date(seconds * 1000); // Convert seconds to milliseconds
        SimpleDateFormat sdf = new SimpleDateFormat("MMMM dd, yyyy h:mm:ss a", Locale.getDefault());
        return sdf.format(date);
    }

    private String formatExitTime(long exitTimeMillis) {
        SimpleDateFormat simpleDateFormat = new SimpleDateFormat("HH:mm:ss", Locale.getDefault());
        Date date = new Date(exitTimeMillis);
        return simpleDateFormat.format(date);
    }

    private Long getNestedLong(DocumentSnapshot document, String path) {
        String[] parts = path.split("\\.");
        Object value = document.get(parts[0]);
        for (int i = 1; i < parts.length; i++) {
            if (value instanceof Map) {
                value = ((Map<?, ?>) value).get(parts[i]);
            } else {
                return null;
            }
        }
        return value instanceof Long ? (Long) value : null;
    }
    public void onDestroy() {
        super.onDestroy();
        if (executorService != null) {
            executorService.shutdown(); // Shut down the executor service when the fragment is destroyed
        }
    }

}
