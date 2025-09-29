package com.example.bahon;

import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;

public class CalculateFare extends AppCompatActivity {

    private EditText latitudeInput, longitudeInput, cardNumberInput;
    private Button submitLatLongButton, submitCardButton;
    private DatabaseReference currentLocationRef, scannedCardRef;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_calculate_fare);

        // Set status bar and navigation bar colors
        getWindow().setStatusBarColor(getResources().getColor(R.color.blue));
        getWindow().setNavigationBarColor(getResources().getColor(R.color.blue));

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        // Back arrow functionality
        ImageView backArrow = findViewById(R.id.back_arrow_sign);
        backArrow.setOnClickListener(v -> {
            finish();
            overridePendingTransition(android.R.anim.fade_in, android.R.anim.fade_out);
        });

        // Initialize Firebase Realtime Database references
        currentLocationRef = FirebaseDatabase.getInstance().getReference("current_location_table");
        scannedCardRef = FirebaseDatabase.getInstance().getReference("scanned_card");

        // Initialize input fields and buttons
        latitudeInput = findViewById(R.id.latitude_input);
        longitudeInput = findViewById(R.id.longitude_input);
        cardNumberInput = findViewById(R.id.card_number_input);
        submitLatLongButton = findViewById(R.id.submit_lat_long);
        submitCardButton = findViewById(R.id.submit_card);

        // Set click listeners
        submitLatLongButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                submitLatLong();
            }
        });

        submitCardButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                submitCardNumber();
            }
        });
    }

    private void submitLatLong() {
        String latitude = latitudeInput.getText().toString().trim();
        String longitude = longitudeInput.getText().toString().trim();

        if (latitude.isEmpty() || longitude.isEmpty()) {
            Toast.makeText(this, "Please enter both latitude and longitude", Toast.LENGTH_SHORT).show();
            return;
        }

        try {
            double lat = Double.parseDouble(latitude);
            double lon = Double.parseDouble(longitude);

            // Create an entry with lat and lon
            DatabaseReference newEntry = currentLocationRef.push(); // Creates a unique key
            newEntry.child("lat").setValue(lat);
            newEntry.child("lon").setValue(lon);

            Toast.makeText(this, "Location submitted successfully", Toast.LENGTH_SHORT).show();
            latitudeInput.setText("");
            longitudeInput.setText("");
        } catch (NumberFormatException e) {
            Toast.makeText(this, "Invalid latitude or longitude", Toast.LENGTH_SHORT).show();
        }
    }

    private void submitCardNumber() {
        String cardNumber = cardNumberInput.getText().toString().trim();

        if (cardNumber.isEmpty()) {
            Toast.makeText(this, "Please enter the card number", Toast.LENGTH_SHORT).show();
            return;
        }

        try {
            // Create an entry with card_no
            DatabaseReference newEntry = scannedCardRef.push(); // Creates a unique key
            newEntry.child("card_no").setValue(cardNumber);

            Toast.makeText(this, "Card number submitted successfully", Toast.LENGTH_SHORT).show();
            cardNumberInput.setText("");
        } catch (Exception e) {
            Toast.makeText(this, "Error submitting card number", Toast.LENGTH_SHORT).show();
        }
    }
}

