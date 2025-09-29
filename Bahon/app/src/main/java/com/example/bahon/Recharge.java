package com.example.bahon;

import android.app.Dialog;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.os.Bundle;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.Window;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.HashMap;
import java.util.Map;

public class Recharge extends AppCompatActivity {

    private FirebaseFirestore firestore;
    private EditText amountInput;
    private Button proceedButton;
    private Dialog loadingDialog;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_recharge);
        Window window = getWindow();
        window.setNavigationBarColor(getResources().getColor(R.color.blue));
        window.setStatusBarColor(getResources().getColor(R.color.blue));
        // Initialize Firestore
        firestore = FirebaseFirestore.getInstance();

        // Initialize UI elements
        amountInput = findViewById(R.id.amount); // Ensure this ID matches the XML layout
        proceedButton = findViewById(R.id.proceed); // Ensure this ID matches the XML layout

        // Set up loading dialog
        setupLoadingDialog();

        // Handle proceed button click
        proceedButton.setOnClickListener(v -> handleRecharge());

        ImageView backArrow = findViewById(R.id.back_arrow);
        backArrow.setOnClickListener(v -> {
            finish();
            overridePendingTransition(android.R.anim.fade_in, android.R.anim.fade_out);
        });
    }

    private void setupLoadingDialog() {
        loadingDialog = new Dialog(this);
        loadingDialog.requestWindowFeature(Window.FEATURE_NO_TITLE);
        loadingDialog.setContentView(R.layout.dialog_loading);
        loadingDialog.setCancelable(false); // Prevent closing by tapping outside
        loadingDialog.getWindow().setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT)); // Transparent background
    }

    private void handleRecharge() {
        String amountStr = amountInput.getText().toString().trim();

        // Check if the amount field is empty
        if (amountStr.isEmpty()) {
            showCustomToast("Please enter an amount",false);
            return;
        }

        double amount;
        try {
            amount = Double.parseDouble(amountStr);
        } catch (NumberFormatException e) {
            showCustomToast("Invalid amount entered",false);
            return;
        }

        // Show the loading dialog
        loadingDialog.show();

        String userId = FirebaseAuth.getInstance().getCurrentUser().getUid();
        DocumentReference userDocRef = firestore.collection("users").document(userId);

        // Update balance in Firestore
        userDocRef.get().addOnSuccessListener(documentSnapshot -> {
            double currentBalance = 0.0;
            if (documentSnapshot.contains("balance")) {
                currentBalance = documentSnapshot.getDouble("balance");
            }
            double newBalance = currentBalance + amount;

            // Update user's balance
            userDocRef.update("balance", newBalance)
                    .addOnSuccessListener(aVoid -> {
                        loadingDialog.dismiss(); // Dismiss the loading dialog
                        showCustomToast("Recharge successful!",true);
                        saveRechargeHistory(userId, amount); // Save recharge history
                    })
                    .addOnFailureListener(e -> {
                        loadingDialog.dismiss(); // Dismiss the loading dialog
                        showCustomToast("Failed to update balance",false);
                    });
        }).addOnFailureListener(e -> {
            loadingDialog.dismiss(); // Dismiss the loading dialog
            showCustomToast("Failed to load balance",false);
        });
    }

    private void saveRechargeHistory(String userId, double amount) {
        // Reference to recharge history subcollection
        DocumentReference historyRef = firestore.collection("users")
                .document(userId)
                .collection("recharge_history")
                .document();

        // Prepare data to save
        Map<String, Object> historyData = new HashMap<>();
        historyData.put("amount", amount);
        historyData.put("timestamp", System.currentTimeMillis()); // Store current time in milliseconds

        // Save recharge history entry
        historyRef.set(historyData);
    }

    private void showCustomToast(String message, boolean isSuccess) {
        LayoutInflater inflater = getLayoutInflater();
        int layoutId = isSuccess ? R.layout.toast_success : R.layout.toast_error;
        View layout = inflater.inflate(layoutId, findViewById(R.id.toast_text));

        TextView text = layout.findViewById(R.id.toast_text);
        text.setText(message);

        Toast toast = new Toast(getApplicationContext());
        toast.setGravity(Gravity.TOP, 0, 100);
        toast.setDuration(Toast.LENGTH_SHORT);
        toast.setView(layout);
        toast.show();
    }


}
