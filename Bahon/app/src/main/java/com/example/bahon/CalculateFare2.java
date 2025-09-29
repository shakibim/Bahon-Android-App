package com.example.bahon;

import android.os.Bundle;
import androidx.appcompat.app.AppCompatActivity;
import android.text.TextUtils;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.ListenerRegistration;

public class CalculateFare2 extends AppCompatActivity {

    private EditText kmInput;
    private TextView fareDisplay;
    private Button calculateFareButton;

    private FirebaseFirestore firestore;
    private double farePerKm = 0.0; // Default value
    private ListenerRegistration firestoreListener;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_calculate_fare2);
        getWindow().setStatusBarColor(getResources().getColor(R.color.blue));
        getWindow().setNavigationBarColor(getResources().getColor(R.color.blue));
        ImageView backArrow = findViewById(R.id.back_arrow3);
        backArrow.setOnClickListener(v -> {
            finish();
            overridePendingTransition(android.R.anim.fade_in, android.R.anim.fade_out);
        });
        // Initialize UI components
        kmInput = findViewById(R.id.km_input);
        fareDisplay = findViewById(R.id.fare_display);
        calculateFareButton = findViewById(R.id.calculate_fare_button);

        // Initialize Firestore
        firestore = FirebaseFirestore.getInstance();

        // Load farePerKm from Firestore
        loadFarePerKm();

        // Set up button click listener
        calculateFareButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                calculateFare();
            }
        });
    }

    private void loadFarePerKm() {
        // Reference to the specific document in Firestore
        DocumentReference fareRef = firestore.collection("farePerKm").document("266565");

        // Add a listener to fetch farePerKm in real-time
        firestoreListener = fareRef.addSnapshotListener((documentSnapshot, e) -> {
            if (e != null) {
                Toast.makeText(CalculateFare2.this, "Error fetching fare: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                return;
            }

            if (documentSnapshot != null && documentSnapshot.exists()) {
                // Retrieve the farePerKm value
                farePerKm = documentSnapshot.getDouble("fpk");
                TextView fare_per_km_label = findViewById(R.id.fare_per_km_label);
                fare_per_km_label.setText("Fare per KM: "+farePerKm+ "BDT");

            } else {
                Toast.makeText(CalculateFare2.this, "Document not found", Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void calculateFare() {
        // Get input from the EditText
        String kmInputValue = kmInput.getText().toString();

        if (TextUtils.isEmpty(kmInputValue)) {
            // Show an error message if input is empty
            Toast.makeText(this, "Please enter a valid distance in KM", Toast.LENGTH_SHORT).show();
            return;
        }

        try {
            // Parse the input to a double
            double distanceInKm = Double.parseDouble(kmInputValue);

            if (distanceInKm < 0) {
                // Handle negative input
                Toast.makeText(this, "Distance cannot be negative", Toast.LENGTH_SHORT).show();
                return;
            }

            // Calculate the fare
            double fare = distanceInKm * farePerKm;

            // Display the calculated fare in the TextView
            fareDisplay.setText(String.format("Fare: %.2f BDT", fare));

        } catch (NumberFormatException e) {
            // Handle invalid input
            Toast.makeText(this, "Invalid input. Please enter a numeric value", Toast.LENGTH_SHORT).show();
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (firestoreListener != null) {
            firestoreListener.remove(); // Remove Firestore listener to avoid memory leaks
        }
    }
}
