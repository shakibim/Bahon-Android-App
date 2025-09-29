package com.example.bahon;

import android.os.Bundle;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.HashMap;
import java.util.Map;

public class EditProfile extends AppCompatActivity {

    private EditText nameField, phoneField, emailField, addressField, cardNumberField;
    private FirebaseFirestore db;
    private FirebaseUser currentUser;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_edit_profile);

        // Set top and bottom navigation bar colors to blue
        getWindow().setStatusBarColor(getResources().getColor(R.color.blue));
        getWindow().setNavigationBarColor(getResources().getColor(R.color.blue));

        // Initialize Firebase and UI components
        db = FirebaseFirestore.getInstance();
        currentUser = FirebaseAuth.getInstance().getCurrentUser();

        nameField = findViewById(R.id.ename);
        phoneField = findViewById(R.id.ephonenum);
        emailField = findViewById(R.id.eemail);
        addressField = findViewById(R.id.eadd);
        cardNumberField = findViewById(R.id.ecard);

        Button saveButton = findViewById(R.id.save_button);
        Button cancelButton = findViewById(R.id.cancel_button);
        ImageView backArrow = findViewById(R.id.back_arrow);

        // Load user data from Firestore
        loadUserData();

        // Save updated data to Firestore and Firebase Auth
        saveButton.setOnClickListener(v -> updateProfileData());

        // Cancel and finish the activity
        cancelButton.setOnClickListener(v -> finish());

        // Back button
        backArrow.setOnClickListener(v -> finish());
    }

    private void loadUserData() {
        String userId = currentUser.getUid();
        db.collection("users").document(userId).get().addOnSuccessListener(documentSnapshot -> {
            if (documentSnapshot.exists()) {
                nameField.setText(documentSnapshot.getString("name"));
                phoneField.setText(documentSnapshot.getString("phone"));
                emailField.setText(documentSnapshot.getString("email"));
                addressField.setText(documentSnapshot.getString("address"));
                cardNumberField.setText(documentSnapshot.getString("card_no"));
            } else {
                Toast.makeText(this, "User data not found.", Toast.LENGTH_SHORT).show();
            }
        }).addOnFailureListener(e -> Toast.makeText(this, "Failed to load user data.", Toast.LENGTH_SHORT).show());
    }

    private void updateProfileData() {
        String newName = nameField.getText().toString();
        String newPhone = phoneField.getText().toString();
        String newEmail = emailField.getText().toString();
        String newAddress = addressField.getText().toString();
        String newCardNumber = cardNumberField.getText().toString();

        // Update Firestore
        String userId = currentUser.getUid();
        Map<String, Object> updates = new HashMap<>();
        updates.put("name", newName);
        updates.put("phone", newPhone);
        updates.put("email", newEmail);
        updates.put("address", newAddress);
        updates.put("card_no", newCardNumber);

        db.collection("users").document(userId).update(updates).addOnSuccessListener(aVoid -> {
            showCustomToast("Profile updated successfully.", true);
        }).addOnFailureListener(e -> {
            showCustomToast("Failed to update profile.", false);
        });

        // Update Firebase Auth email if changed
        if (!newEmail.equals(currentUser.getEmail())) {
            currentUser.updateEmail(newEmail).addOnCompleteListener(task -> {
                if (task.isSuccessful()) {
                    showCustomToast("Email updated in Auth.", true);
                } else {
                    showCustomToast("Failed to update email in Auth.", false);
                }
            });
        }

        // Update Firebase Auth password if changed (if provided)
         // Placeholder: retrieve new password from a secure input field if applicable
        String newPassword = "";  // Placeholder: retrieve new password from a secure input field if applicable
        if (!newPassword.isEmpty()) {
            currentUser.updatePassword(newPassword).addOnCompleteListener(task -> {
                if (task.isSuccessful()) {
                    showCustomToast("Password updated in Auth.", true);
                } else {
                    showCustomToast("Failed to update password in Auth.", false);
                }
            });
        }
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
