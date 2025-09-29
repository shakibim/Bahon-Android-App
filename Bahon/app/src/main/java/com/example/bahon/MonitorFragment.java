package com.example.bahon;

import android.app.Dialog;
import android.content.Intent;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.Toast;

import androidx.fragment.app.Fragment;

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

public class MonitorFragment extends Fragment {

    private ListView paymentListView;
    private FirebaseFirestore db;
    private Dialog loadingDialog;
    private Button viewPassenger;
    public MonitorFragment() {
        // Required empty public constructor
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        db = FirebaseFirestore.getInstance();
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        View rootView = inflater.inflate(R.layout.fragment_monitor, container, false);

        // Initialize ListView and loading dialog
        paymentListView = rootView.findViewById(R.id.paymentListView);
        loadingDialog = new Dialog(getContext());
        loadingDialog.setContentView(R.layout.dialog_loading);
        loadingDialog.setCancelable(false);
        loadingDialog.getWindow().setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));

        List<JourneyHistory> journeyData = new ArrayList<>();
        JourneyHistoryAdapter adapter = new JourneyHistoryAdapter(getContext(), journeyData);
        paymentListView.setAdapter(adapter);

        // Fetch journey data from Firestore
        fetchJourneyDataFromFirestore(journeyData, adapter);
        viewPassenger = rootView.findViewById(R.id.viewPassenger);
        viewPassenger.setOnClickListener(v -> {
            Intent intent = new Intent(getActivity(), OnBoardPassenger.class);
            startActivity(intent);
            requireActivity().overridePendingTransition(android.R.anim.fade_in, android.R.anim.fade_out);
        });

        return rootView;
    }

    private void showLoading() {
        loadingDialog.show();
    }

    private void hideLoading() {
        if (loadingDialog.isShowing()) {
            loadingDialog.dismiss();
        }
    }

    private void fetchJourneyDataFromFirestore(List<JourneyHistory> journeyData, JourneyHistoryAdapter adapter) {
        showLoading();

        db.collection("users")
                .get() // Get all users
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful()) {
                        QuerySnapshot querySnapshot = task.getResult();
                        if (querySnapshot != null && !querySnapshot.isEmpty()) {
                            for (DocumentSnapshot userDoc : querySnapshot) {
                                String userId = userDoc.getId();
                                String username = userDoc.getString("name");
                                String cardNo = userDoc.getString("card_no");

                                // Get start and end of the current date
                                Calendar calendar = Calendar.getInstance();
                                calendar.set(Calendar.HOUR_OF_DAY, 0);
                                calendar.set(Calendar.MINUTE, 0);
                                calendar.set(Calendar.SECOND, 0);
                                calendar.set(Calendar.MILLISECOND, 0);
                                Date startOfDay = calendar.getTime();

                                calendar.set(Calendar.HOUR_OF_DAY, 23);
                                calendar.set(Calendar.MINUTE, 59);
                                calendar.set(Calendar.SECOND, 59);
                                calendar.set(Calendar.MILLISECOND, 999);
                                Date endOfDay = calendar.getTime();

                                // Fetch journey data for the current date
                                db.collection("users")
                                        .document(userId)
                                        .collection("journeys")
                                        .whereGreaterThanOrEqualTo("exit_time", startOfDay)
                                        .whereLessThanOrEqualTo("exit_time", endOfDay)
                                        .orderBy("exit_time", com.google.firebase.firestore.Query.Direction.DESCENDING)
                                        .get()
                                        .addOnCompleteListener(journeyTask -> {
                                            if (journeyTask.isSuccessful()) {
                                                QuerySnapshot journeySnapshot = journeyTask.getResult();
                                                if (journeySnapshot != null && !journeySnapshot.isEmpty()) {
                                                    for (DocumentSnapshot document : journeySnapshot) {
                                                        try {
                                                            // Extract relevant fields
                                                            String entryPoint = document.getString("entry_point");
                                                            String exitPoint = document.getString("exit_point");
                                                            Double distanceTravelled = document.getDouble("distance_travelled_km");
                                                            Double fareValue = document.getDouble("fare");

                                                            // Extract time fields (entry and exit)
                                                            Long entrySeconds = getNestedLong(document, "entry_time._seconds");
                                                            Long exitTimeMillis = document.getDate("exit_time") != null ? document.getDate("exit_time").getTime() : null;

                                                            // Handle missing or invalid fields
                                                            if (entryPoint == null || exitPoint == null || distanceTravelled == null || fareValue == null || entrySeconds == null || exitTimeMillis == null) {
                                                                continue;
                                                            }

                                                            // Format time
                                                            String entryTime = formatTimestamp(entrySeconds);
                                                            String exitTime = formatExitTime(exitTimeMillis);

                                                            // Total time calculation (difference between entry and exit time)
                                                            long totalTimeMillis = exitTimeMillis - (entrySeconds * 1000); // Convert seconds to milliseconds
                                                            String totalTime = calculateTimeDifference(totalTimeMillis);

                                                            // Fare per KM calculation


                                                            // Format fare and distance
                                                            String fare = "৳" + String.format(Locale.getDefault(), "%.2f", fareValue);
                                                            String fare2 = "৳" + String.format(Locale.getDefault(), "%.8f", fareValue);
                                                            String distance = String.format(Locale.getDefault(), "%.8f KM", distanceTravelled);

                                                            String numericPart = distance.replaceAll("[^0-9.]", "");
                                                            double distanceMeters = Double.parseDouble(numericPart)*1000;
                                                            String distanceValueInMeters = String.format(Locale.getDefault(), "%.2f M", distanceMeters);

                                                            Double fpkm = document.getDouble("fpkm");
                                                            // Fare per KM calculation


                                                            String farePerKm = String.format(Locale.getDefault(), "৳%.1f/km", fpkm);
                                                            // Create detailed journey summary with username and card_no
                                                            String journeyDetails =
                                                                    "" + "■ Username: " + username + "\n" +
                                                                            "■ Card No: " + cardNo + "\n" +
                                                                            "■ Entry Point: " + entryPoint + "\n" +
                                                                            "■ Exit Point: " + exitPoint + "\n" +
                                                                            "■ Total Distance(km): " + distance + "\n" +
                                                                            "■ Total Distance(m): " + distanceValueInMeters + "\n" +
                                                                            "■ Total Time Taken: " + totalTime + "\n" +
                                                                            "■ Fare per KM: " + farePerKm + "\n" +
                                                                            "■ Fare: " + fare2;

                                                            // Add to journey data
                                                            journeyData.add(new JourneyHistory(entryTime + " - " + exitTime, journeyDetails, fare));
                                                        } catch (Exception e) {
                                                            e.printStackTrace(); // Log error if parsing fails
                                                        }
                                                    }
                                                    adapter.notifyDataSetChanged();
                                                } else {
                                                    if (isAdded() && getContext() != null) {
                                                        Toast.makeText(getContext(), "No journey data found for this user.", Toast.LENGTH_SHORT).cancel();
                                                    }
                                                }
                                            } else {
                                                journeyTask.getException().printStackTrace();
                                                Toast.makeText(getContext(), "Failed to load journey data.", Toast.LENGTH_SHORT).cancel();
                                            }
                                        });
                            }
                        } else {
                            Toast.makeText(getContext(), "No users found.", Toast.LENGTH_SHORT).cancel();
                        }
                    } else {
                        task.getException().printStackTrace();
                        Toast.makeText(getContext(), "Failed to load users.", Toast.LENGTH_SHORT).cancel();
                    }
                    hideLoading();
                });
    }

    // Format entry timestamp (seconds) to a readable string
    private String formatTimestamp(Long seconds) {
        Date date = new Date(seconds * 1000); // Convert seconds to milliseconds
        SimpleDateFormat sdf = new SimpleDateFormat("MMMM dd, yyyy h:mm:ss a", Locale.getDefault());
        return sdf.format(date);
    }

    // Format exit timestamp (milliseconds) to "HH:mm:ss"
    private String formatExitTime(long exitTimeMillis) {
        SimpleDateFormat simpleDateFormat = new SimpleDateFormat("HH:mm:ss", Locale.getDefault());
        Date date = new Date(exitTimeMillis);
        return simpleDateFormat.format(date);
    }

    // Helper method to get nested Long values from Firestore document
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

    private String calculateTimeDifference(long timeInMillis) {
        long hours = timeInMillis / (1000 * 60 * 60);
        long minutes = (timeInMillis % (1000 * 60 * 60)) / (1000 * 60);
        long seconds = (timeInMillis % (1000 * 60)) / 1000;

        return String.format("%02d:%02d:%02d", hours, minutes, seconds);
    }

    public void onResume() {
        super.onResume();

        if (paymentListView != null) {
            List<JourneyHistory> journeyData = new ArrayList<>();
            JourneyHistoryAdapter adapter = new JourneyHistoryAdapter(getContext(), journeyData);
            paymentListView.setAdapter(adapter);

            fetchJourneyDataFromFirestore(journeyData, adapter);
        }
    }
}
