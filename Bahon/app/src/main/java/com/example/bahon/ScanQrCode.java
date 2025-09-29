package com.example.bahon;

import android.os.Bundle;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;
import com.journeyapps.barcodescanner.BarcodeCallback;
import com.journeyapps.barcodescanner.BarcodeResult;
import com.journeyapps.barcodescanner.BarcodeView;
import androidx.appcompat.app.AppCompatActivity;
import android.view.View;
import android.widget.Toast;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.Query;
public class ScanQrCode extends AppCompatActivity {

    private BarcodeView barcodeView;
    private TextView scanResultTextView;
    private TextView  scanResultTextView2;
    private Button exitButton;

    private FirebaseFirestore firestore;
    private FirebaseDatabase realtimeDatabase;
    private DatabaseReference scannedCardRef;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        getWindow().setStatusBarColor(getResources().getColor(R.color.blue));
        getWindow().setNavigationBarColor(getResources().getColor(R.color.blue));
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_scan_qr_code);
        ImageView backArrow = findViewById(R.id.back_arrow);
        backArrow.setOnClickListener(v -> {
            finish();
            overridePendingTransition(android.R.anim.fade_in, android.R.anim.fade_out);
        });
        // Initialize views
        barcodeView = findViewById(R.id.barcode_view);
        scanResultTextView = findViewById(R.id.scan_result);
        scanResultTextView2=findViewById(R.id.scan_result2);
        exitButton = findViewById(R.id.exit_button);
        exitButton.setVisibility(View.INVISIBLE); // Initially invisible

        String cardNo= "";

        // Initialize Firebase
        firestore = FirebaseFirestore.getInstance();
        realtimeDatabase = FirebaseDatabase.getInstance();
        scannedCardRef = realtimeDatabase.getReference("scanned_card");

        // Set up the BarcodeView
        barcodeView.decodeContinuous(new BarcodeCallback() {
            @Override
            public void barcodeResult(BarcodeResult result) {
                String scannedData = result.getText();

                // Check Firestore for a valid card number
                checkCardNumberInFirestore(scannedData);
            }

            @Override
            public void possibleResultPoints(java.util.List<com.google.zxing.ResultPoint> resultPoints) {
                // Optional: Handle result points (used for visualization of scanned areas)
            }
        });

        exitButton.setOnClickListener(v -> {
            String scannedCard = scanResultTextView2.getText().toString();
            sendCardToRealtimeDatabase(scannedCard);
        });
    }

    private void checkCardNumberInFirestore(String scannedData) {
        Query query = firestore.collection("users").whereEqualTo("card_no", scannedData);
        query.get().addOnCompleteListener(task -> {
            if (task.isSuccessful() && task.getResult() != null && !task.getResult().isEmpty()) {
                // Valid card number found
                DocumentSnapshot document = task.getResult().getDocuments().get(0);
                String userName = document.getString("name");

                scanResultTextView.setText("User: " + userName);
                scanResultTextView2.setText(scannedData);
                exitButton.setVisibility(View.VISIBLE); // Show the button
            } else {
                // Invalid card number
                scanResultTextView.setText("Invalid card number: " + scannedData);
                exitButton.setVisibility(View.INVISIBLE); // Hide the button
            }
        }).addOnFailureListener(e -> {
            Toast.makeText(ScanQrCode.this, "Error checking card number", Toast.LENGTH_SHORT).show();
        });
    }

    private void sendCardToRealtimeDatabase(String scannedCard) {
        if (scannedCard.isEmpty()) {
            Toast.makeText(this, "No card to send!", Toast.LENGTH_SHORT).show();
            return;
        }

        // Push the scanned card as a new document (auto-assigned key)
        DatabaseReference newEntry = scannedCardRef.push();

        // Set the "card_no" field in the new entry
        newEntry.child("card_no").setValue(scannedCard)
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful()) {
                        Toast.makeText(ScanQrCode.this, "Card sent successfully!", Toast.LENGTH_SHORT).show();
                    } else {
                        Toast.makeText(ScanQrCode.this, "Failed to send card", Toast.LENGTH_SHORT).show();
                    }
                })
                .addOnFailureListener(e -> {
                    Toast.makeText(ScanQrCode.this, "Error: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                });
    }

    @Override
    protected void onResume() {
        super.onResume();
        barcodeView.resume();
    }

    @Override
    protected void onPause() {
        super.onPause();
        barcodeView.pause();
    }
}
