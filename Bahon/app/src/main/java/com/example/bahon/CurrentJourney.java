package com.example.bahon;

import android.os.Bundle;
import android.util.Log;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;

import org.json.JSONObject;
import org.osmdroid.config.Configuration;
import org.osmdroid.util.GeoPoint;
import org.osmdroid.views.MapView;
import org.osmdroid.views.overlay.Marker;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.text.SimpleDateFormat;
import java.util.Locale;
import java.util.concurrent.Executors;

public class CurrentJourney extends AppCompatActivity {

    private TextView entryPointText, startedAtText, currentLocationText;
    private MapView mapView;
    private double latitude, longitude;
    private String entryTime = "N/A", entryLocation = "You are not on a journey";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_current_journey);
        getWindow().setStatusBarColor(getResources().getColor(R.color.blue));
        getWindow().setNavigationBarColor(getResources().getColor(R.color.blue));

        // Initialize views
        entryPointText = findViewById(R.id.entry_location_text);
        startedAtText = findViewById(R.id.start_time_text2);
        currentLocationText = findViewById(R.id.current_location);
        mapView = findViewById(R.id.osm_map);

        Configuration.getInstance().setUserAgentValue(getPackageName());
        mapView.setMultiTouchControls(true);

        ImageView backArrow = findViewById(R.id.back_arrow);
        backArrow.setOnClickListener(v -> finish());

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        // Execute both methods in separate threads
        Executors.newSingleThreadExecutor().execute(this::fetchAndDisplayCurrentLocation);
        checkUserOnboardStatus(new OnUserStatusCheckedListener() {
            @Override
            public void onStatusChecked(boolean isOnboard) {
                if (isOnboard) {
                    Executors.newSingleThreadExecutor().execute(() -> fetchJourneyDetails());
                }
                else{
                    TextView entryLocationText = findViewById(R.id.entry_location_text);
                    TextView startTimeText2 = findViewById(R.id.start_time_text2);

                    entryLocationText.setText("You are not on a journey");
                    startTimeText2.setText("N/A");

                }
            }
        });

    }

    private void checkJourneyStatus() {
        String userId = FirebaseAuth.getInstance().getCurrentUser().getUid();
        DatabaseReference userRef = FirebaseDatabase.getInstance().getReference("users").child(userId);

        userRef.addListenerForSingleValueEvent(new com.google.firebase.database.ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot snapshot) {
                Boolean onJourney = snapshot.child("onjourney").getValue(Boolean.class);

                if (onJourney != null && onJourney) {
                    fetchJourneyDetails();
                } else {
                    entryPointText.setText("You are not on a journey");
                    startedAtText.setText("N/A");
                }
            }

            @Override
            public void onCancelled(DatabaseError error) {
                Log.e("FirebaseError", "Error checking journey status: " + error.getMessage());
            }
        });
    }

    private void fetchJourneyDetails() {
        String userId = FirebaseAuth.getInstance().getCurrentUser().getUid();
        DatabaseReference journeyRef = FirebaseDatabase.getInstance().getReference("processed_scanned_card");

        journeyRef.addListenerForSingleValueEvent(new com.google.firebase.database.ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot snapshot) {
                DataSnapshot latestEntry = null;
                long latestTimestamp = 0;

                for (DataSnapshot child : snapshot.getChildren()) {
                    String recordUid = child.child("uid").getValue(String.class);
                    String status = child.child("status").getValue(String.class);
                    Long timestamp = child.child("timestamp/_seconds").getValue(Long.class);

                    // Filter by user ID and "entry" status
                    if ("entry".equals(status) && userId.equals(recordUid) && timestamp != null) {
                        if (timestamp > latestTimestamp) {
                            latestTimestamp = timestamp;
                            latestEntry = child;
                        }
                    }
                }

                if (latestEntry != null) {
                    latitude = latestEntry.child("location/lat").getValue(Double.class);
                    longitude = latestEntry.child("location/lon").getValue(Double.class);

                    entryTime = formatTimestamp(latestTimestamp);
                    performReverseGeocoding(latitude, longitude, address -> {
                        entryLocation = address;
                        updateUI();
                    });
                } else {
                    entryPointText.setText("No valid entry point found");
                    startedAtText.setText("N/A");
                }
            }

            @Override
            public void onCancelled(DatabaseError error) {
                Log.e("FirebaseError", "Error fetching journey details: " + error.getMessage());
            }
        });
    }


    private void fetchAndDisplayCurrentLocation() {
        DatabaseReference locationRef = FirebaseDatabase.getInstance().getReference("processed_location_table");

        locationRef.orderByChild("entry_no").limitToLast(1).addListenerForSingleValueEvent(new com.google.firebase.database.ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot snapshot) {
                for (DataSnapshot child : snapshot.getChildren()) {
                    double currentLat = child.child("lat").getValue(Double.class);
                    double currentLon = child.child("lon").getValue(Double.class);
                    updateMap(currentLat, currentLon);
                    performReverseGeocoding(currentLat, currentLon, address -> {
                        currentLocationText.setText(address);
                        updateMap(currentLat, currentLon);
                    });
                }
            }

            @Override
            public void onCancelled(DatabaseError error) {
                Log.e("FirebaseError", "Error fetching current location: " + error.getMessage());
            }
        });
    }

    private void performReverseGeocoding(double lat, double lon, ReverseGeocodingCallback callback) {
        Executors.newSingleThreadExecutor().execute(() -> {
            try {
                String urlString = String.format(Locale.getDefault(),
                        "https://nominatim.openstreetmap.org/reverse?format=json&lat=%.4f&lon=%.4f",
                        lat, lon);
                HttpURLConnection conn = (HttpURLConnection) new URL(urlString).openConnection();
                conn.setRequestMethod("GET");
                conn.setRequestProperty("User-Agent", "Mozilla/5.0");

                BufferedReader reader = new BufferedReader(new InputStreamReader(conn.getInputStream()));
                StringBuilder response = new StringBuilder();
                String line;
                while ((line = reader.readLine()) != null) {
                    response.append(line);
                }
                reader.close();

                JSONObject json = new JSONObject(response.toString());
                JSONObject addressObject = json.getJSONObject("address");

                // Build a full address by checking available fields
                StringBuilder addressBuilder = new StringBuilder();
                if (addressObject.has("house_number")) {
                    addressBuilder.append(addressObject.getString("house_number")).append(", ");
                }
                if (addressObject.has("road")) {
                    addressBuilder.append(addressObject.getString("road")).append(", ");
                }
                if (addressObject.has("suburb")) {
                    addressBuilder.append(addressObject.getString("suburb")).append(", ");
                }
                if (addressObject.has("city")) {
                    addressBuilder.append(addressObject.getString("city")).append(", ");
                }
                if (addressObject.has("state")) {
                    addressBuilder.append(addressObject.getString("state")).append(", ");
                }
                if (addressObject.has("postcode")) {
                    addressBuilder.append(addressObject.getString("postcode")).append(", ");
                }
                if (addressObject.has("country")) {
                    addressBuilder.append(addressObject.getString("country"));
                }

                // Remove trailing commas and whitespace
                String fullAddress = addressBuilder.toString().replaceAll(",\\s*$", "");

                runOnUiThread(() -> callback.onAddressRetrieved(fullAddress));
            } catch (Exception e) {
                e.printStackTrace();
                runOnUiThread(() -> callback.onAddressRetrieved("Unknown Location"));
            }
        });
    }

    private void updateUI() {
        entryPointText.setText(entryLocation);
        startedAtText.setText("Started At: " + entryTime);
    }

    private void updateMap(double lat, double lon) {
        GeoPoint point = new GeoPoint(lat, lon);
        mapView.getController().setZoom(15.0);
        mapView.getController().setCenter(point);

        Marker marker = new Marker(mapView);
        marker.setPosition(point);
        marker.setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_BOTTOM);
        mapView.getOverlays().clear();
        mapView.getOverlays().add(marker);
    }

    private String formatTimestamp(long timestamp) {
        return new SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault()).format(timestamp * 1000);
    }

    private interface ReverseGeocodingCallback {
        void onAddressRetrieved(String address);
    }

    private void checkUserOnboardStatus(final OnUserStatusCheckedListener listener) {
        // Get the logged-in user's UID
        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
        if (user != null) {
            String userId = user.getUid();

            // Reference to the user's document in Firestore
            FirebaseFirestore db = FirebaseFirestore.getInstance();
            DocumentReference userDocRef = db.collection("users").document(userId);

            // Fetch the onboard value
            userDocRef.get().addOnCompleteListener(task -> {
                if (task.isSuccessful() && task.getResult() != null) {
                    DocumentSnapshot document = task.getResult();
                    if (document.exists()) {
                        Boolean isOnboard = document.getBoolean("onjourney");
                        // Call the listener with the result
                        listener.onStatusChecked(isOnboard != null && isOnboard);  // true if onboard, false otherwise
                    } else {
                        listener.onStatusChecked(false);  // Document doesn't exist, consider false
                    }
                } else {
                    listener.onStatusChecked(false);  // Query failed, consider false
                }
            });
        } else {
            listener.onStatusChecked(false);  // User is not logged in, consider false
        }
    }

    // Define an interface for callback
    public interface OnUserStatusCheckedListener {
        void onStatusChecked(boolean isOnboard);
    }
}
