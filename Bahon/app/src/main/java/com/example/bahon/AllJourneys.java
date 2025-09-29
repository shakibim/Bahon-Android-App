package com.example.bahon;

import android.app.Dialog;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.os.Bundle;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QuerySnapshot;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public class AllJourneys extends AppCompatActivity {

    private ListView allJourneysListView;
    private FirebaseFirestore db;
    private Dialog loadingDialog;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_all_journeys);

        // Set system bar colors
        getWindow().setStatusBarColor(getResources().getColor(R.color.blue));
        getWindow().setNavigationBarColor(getResources().getColor(R.color.blue));

        // Adjust view padding based on window insets
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        // Back navigation setup
        setupBackNavigation();

        // Initialize Firebase Firestore
        db = FirebaseFirestore.getInstance();
        loadingDialog = new Dialog(this);
        loadingDialog.setContentView(R.layout.dialog_loading);
        loadingDialog.setCancelable(false);
        loadingDialog.getWindow().setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
        // Initialize ListView and adapter with the correct ID
        allJourneysListView = findViewById(R.id.journiesListView);
        List<JourneyHistory> journeyData = new ArrayList<>();
        JourneyHistoryAdapter adapter = new JourneyHistoryAdapter(this, journeyData);
        allJourneysListView.setAdapter(adapter);

        // Fetch journey data from Firestore
        fetchJourneyDataFromFirestore(journeyData, adapter);

    }
    private void showLoading() {
        loadingDialog.show();
    }

    private void hideLoading() {
        if (loadingDialog.isShowing()) {
            loadingDialog.dismiss();
        }
    }
    private void setupBackNavigation() {
        ImageView backArrow = findViewById(R.id.back_arrow);
        backArrow.setOnClickListener(v -> {
            finish();
            overridePendingTransition(android.R.anim.fade_in, android.R.anim.fade_out);
        });
    }

    private void fetchJourneyDataFromFirestore(List<JourneyHistory> journeyData, JourneyHistoryAdapter adapter) {
        showLoading();
        String currentUserId = FirebaseAuth.getInstance().getCurrentUser().getUid();

        db.collection("users")
                .document(currentUserId)
                .collection("journeys")
                .orderBy("exit_time", com.google.firebase.firestore.Query.Direction.DESCENDING)
                .get()
                .addOnCompleteListener(task -> {
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
                                    Double fpkm = document.getDouble("fpkm");
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
                                    String farePerKm = String.format(Locale.getDefault(), "৳%.1f/km", fpkm);

                                    // Format fare and distance
                                    String fare = "৳" + String.format(Locale.getDefault(), "%.2f", fareValue);
                                    String fare2 = "৳" + String.format(Locale.getDefault(), "%.8f", fareValue);
                                    String distance = String.format(Locale.getDefault(), "%.8f KM", distanceTravelled);

                                    String numericPart = distance.replaceAll("[^0-9.]", "");
                                    double distanceMeters = Double.parseDouble(numericPart)*1000;
                                    String distanceValueInMeters = String.format(Locale.getDefault(), "%.2f M", distanceMeters);

                                    // Create detailed journey summary
                                    String journeyDetails =
                                            ""+"■ Entry Point: " + entryPoint + "\n" +
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
                            hideLoading();// Notify adapter of new data
                        } else {
                            Toast.makeText(AllJourneys.this, "No journey data found.", Toast.LENGTH_SHORT).cancel();
                        }
                    } else {
                        task.getException().printStackTrace();
                        Toast.makeText(AllJourneys.this, "Failed to load journey data.", Toast.LENGTH_SHORT).cancel();
                    }
                });
        hideLoading();
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
}
