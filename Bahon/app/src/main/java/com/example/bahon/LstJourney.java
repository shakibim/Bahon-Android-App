package com.example.bahon;

import android.app.Dialog;
import android.graphics.Color;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.ColorDrawable;
import android.graphics.drawable.Drawable;
import android.widget.TextView;
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

import org.osmdroid.config.Configuration;
import org.osmdroid.util.GeoPoint;
import org.osmdroid.views.MapView;
import org.osmdroid.views.overlay.Marker;
import org.osmdroid.views.overlay.Polyline;

import android.os.Bundle;
import android.widget.ImageView;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.TimeZone;

public class LstJourney extends AppCompatActivity {

    private MapView mapView;
    private FirebaseAuth auth;
    private FirebaseFirestore db;
    private TextView startPointText, startTimeText, endPointText, endTimeText, distanceFareText;
    private Dialog loadingDialog;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_lst_journey);

        getWindow().setStatusBarColor(getResources().getColor(R.color.blue));
        getWindow().setNavigationBarColor(getResources().getColor(R.color.blue));
        loadingDialog = new Dialog(this);
        loadingDialog.setContentView(R.layout.dialog_loading);
        loadingDialog.setCancelable(false);
        loadingDialog.getWindow().setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        // Set up back arrow functionality
        ImageView backArrow = findViewById(R.id.back_arrow);
        backArrow.setOnClickListener(v -> {
            finish();
            overridePendingTransition(android.R.anim.fade_in, android.R.anim.fade_out);
        });

        // Initialize map configuration
        Configuration.getInstance().setUserAgentValue(getPackageName());
        mapView = findViewById(R.id.osm_map);
        mapView.setMultiTouchControls(true);

        // Initialize Firebase
        auth = FirebaseAuth.getInstance();
        db = FirebaseFirestore.getInstance();
        showLoading();
        // Fetch and draw the last journey path
        fetchAndDrawLastJourney();
        // Initialize views
        startPointText = findViewById(R.id.start_point_text);
        startTimeText = findViewById(R.id.start_time_text4);
        endPointText = findViewById(R.id.current_location_text);
        endTimeText = findViewById(R.id.start_time_text6);
        distanceFareText = findViewById(R.id.start_time_text7);

        // Initialize Firestore
        db = FirebaseFirestore.getInstance();

        // Fetch and display journey data
        fetchJourneyDataFromFirestore();


    }
    private void showLoading() {
        loadingDialog.show();
    }

    private void hideLoading() {
        if (loadingDialog.isShowing()) {
            loadingDialog.dismiss();
        }
    }
    private void fetchAndDrawLastJourney() {
        showLoading();
        String userId = auth.getCurrentUser().getUid();

        db.collection("users")
                .document(userId)
                .collection("journeys")
                .orderBy("exit_time", com.google.firebase.firestore.Query.Direction.DESCENDING)
                .limit(1)
                .get()
                .addOnSuccessListener(querySnapshot -> {
                    if (!querySnapshot.isEmpty()) {
                        querySnapshot.getDocuments().forEach(document -> {
                            // Extract the journey path
                            List<Map<String, Object>> path = (List<Map<String, Object>>) document.get("path");
                            if (path != null && !path.isEmpty()) {
                                ArrayList<GeoPoint> journeyPoints = new ArrayList<>();

                                for (Map<String, Object> point : path) {
                                    try {
                                        double lat = (double) point.get("lat");
                                        double lon = (double) point.get("lon");
                                        journeyPoints.add(new GeoPoint(lat, lon));
                                    } catch (Exception e) {
                                        Toast.makeText(this, "Invalid lat/lon data format.", Toast.LENGTH_SHORT).show();
                                        e.printStackTrace();
                                    }
                                }

                                // Draw the journey path and add markers
                                drawJourneyPathAndMarkers(journeyPoints);
                            } else {
                                Toast.makeText(this, "No path data found for the last journey.", Toast.LENGTH_SHORT).show();
                            }
                        });
                    } else {
                        Toast.makeText(this, "No journey data found.", Toast.LENGTH_SHORT).show();
                    }
                })
                .addOnFailureListener(e -> {
                    Toast.makeText(this, "Failed to fetch journey data.", Toast.LENGTH_SHORT).show();
                    e.printStackTrace();
                });

    }

    private void drawJourneyPathAndMarkers(ArrayList<GeoPoint> journeyPoints) {
        showLoading();
        if (journeyPoints.isEmpty()) {
            Toast.makeText(this, "No journey points to display.", Toast.LENGTH_SHORT).show();
            return;
        }

        // Create a Polyline for the path
        Polyline journeyPath = new Polyline();
        journeyPath.setPoints(journeyPoints);
        journeyPath.setColor(getResources().getColor(R.color.blue)); // Customize the color
        journeyPath.setWidth(8.0f); // Increase the line width

        // Add the polyline to the map
        mapView.getOverlays().add(journeyPath);

        // Add a marker for the start point
        GeoPoint startPoint = journeyPoints.get(0);
        Marker startMarker = new Marker(mapView);
        startMarker.setPosition(startPoint);
        startMarker.setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_BOTTOM);
        startMarker.setIcon(getResources().getDrawable(R.drawable.pin)); // Customize pin drawable
        startMarker.setTitle("Start Point");
        mapView.getOverlays().add(startMarker);

        // Add a marker for the end point
        GeoPoint endPoint = journeyPoints.get(journeyPoints.size() - 1);
        Marker endMarker = new Marker(mapView);
        endMarker.setPosition(endPoint);
        endMarker.setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_BOTTOM);
        endMarker.setIcon(getResources().getDrawable(R.drawable.pin)); // Customize pin drawable
        endMarker.setTitle("End Point");
        mapView.getOverlays().add(endMarker);

        // Center the map on the start point
        mapView.getController().setZoom(15.0);
        mapView.getController().setCenter(startPoint);
        mapView.invalidate(); // Refresh the map to display the line and markers
        hideLoading();
    }

    @Override
    protected void onResume() {
        super.onResume();
        mapView.onResume(); // needed for compass, my location overlays, etc.
    }

    @Override
    protected void onPause() {
        super.onPause();
        mapView.onPause(); // needed for compass, my location overlays, etc.
    }


    private void fetchJourneyDataFromFirestore() {
        showLoading();
        String currentUserId = FirebaseAuth.getInstance().getCurrentUser().getUid();

        db.collection("users")
                .document(currentUserId)
                .collection("journeys")
                .orderBy("exit_time", com.google.firebase.firestore.Query.Direction.DESCENDING)
                .limit(1) // Fetch only the most recent journey
                .get()
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful() && task.getResult() != null) {
                        QuerySnapshot querySnapshot = task.getResult();
                        if (!querySnapshot.isEmpty()) {
                            DocumentSnapshot document = querySnapshot.getDocuments().get(0);

                            String entryPoint = document.getString("entry_point");
                            String exitPoint = document.getString("exit_point");
                            Double distanceTravelled = document.getDouble("distance_travelled_km");
                            Double fare = document.getDouble("fare");

                            Long entrySeconds = document.getLong("entry_time._seconds");
                            Date exitDate = document.getDate("exit_time");

                            if (entryPoint != null) {
                                startPointText.setText("Start Point: " + entryPoint);
                            }
                            if (entrySeconds != null) {
                                startTimeText.setText("Start Time: " + formatTimestamp(entrySeconds));
                            }
                            if (exitPoint != null) {
                                endPointText.setText("End Location: " + exitPoint);
                            }
                            if (exitDate != null) {
                                endTimeText.setText("End Time: " + formatExitTime(exitDate));
                            }
                            if (distanceTravelled != null && fare != null) {
                                distanceFareText.setText(String.format(
                                        Locale.getDefault(),
                                        "Distance: %.3f km, Fare: ৳%.3f",
                                        distanceTravelled, fare));
                            }
                        } else {
                            Toast.makeText(this, "No journey data found.", Toast.LENGTH_SHORT).show();
                            hideLoading();
                        }
                    } else {
                        Toast.makeText(this, "Failed to fetch journey data.", Toast.LENGTH_SHORT).show();
                        hideLoading();
                    }
                });

    }

    private String formatTimestamp(Long seconds) {
        Date date = new Date(seconds * 1000);
        SimpleDateFormat sdf = new SimpleDateFormat("h:mm:ss a", Locale.getDefault());
        return sdf.format(date);
    }

    private String formatExitTime(Date date) {
        SimpleDateFormat sdf = new SimpleDateFormat("h:mm:ss a", Locale.getDefault());
        return sdf.format(date);
    }

}
