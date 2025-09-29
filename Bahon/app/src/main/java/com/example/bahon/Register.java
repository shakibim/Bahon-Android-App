package com.example.bahon;
import android.app.Dialog;
import android.content.Intent;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.app.ProgressDialog;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.Window;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.AuthResult;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FirebaseFirestore;
import java.util.HashMap;
import java.util.Map;

public class Register extends AppCompatActivity {
    private EditText nameEditText, phoneEditText, addressEditText, emailEditText, passwordEditText;
    private Button signupButton, loginbtn;
    private FirebaseAuth mAuth;
    private FirebaseFirestore db;
    private Dialog loadingDialog;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_register);

        // Set navigation and status bar colors
        getWindow().setNavigationBarColor(getResources().getColor(R.color.blue));
        getWindow().setStatusBarColor(getResources().getColor(R.color.blue));

        // Initialize Firebase Auth and Firestore
        mAuth = FirebaseAuth.getInstance();
        db = FirebaseFirestore.getInstance();

        // Initialize UI components
        nameEditText = findViewById(R.id.name);
        phoneEditText = findViewById(R.id.pnumber);
        addressEditText = findViewById(R.id.address);
        emailEditText = findViewById(R.id.email);
        passwordEditText = findViewById(R.id.password);
        signupButton = findViewById(R.id.signup_button);
        loginbtn = findViewById(R.id.login_button);

        // Set up the custom loading dialog
        setupLoadingDialog();

        signupButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                registerUser();
            }
        });

        // Set click listener for login button
        loginbtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(Register.this, Login.class);
                startActivity(intent);
                overridePendingTransition(android.R.anim.fade_in, android.R.anim.fade_out);
            }
        });
    }

    private void setupLoadingDialog() {
        loadingDialog = new Dialog(this);
        loadingDialog.requestWindowFeature(Window.FEATURE_NO_TITLE);
        loadingDialog.setContentView(R.layout.dialog_loading);
        loadingDialog.setCancelable(false); // Prevent closing by tapping outside
        loadingDialog.getWindow().setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT)); // Transparent background
    }

    private void registerUser() {
        String name = nameEditText.getText().toString();
        String phone = phoneEditText.getText().toString();
        String address = addressEditText.getText().toString();
        String email = emailEditText.getText().toString();
        String password = passwordEditText.getText().toString();

        if (name.isEmpty() || phone.isEmpty() || address.isEmpty() || email.isEmpty() || password.isEmpty()) {
            showCustomToast("Please fill all the fields", false);
            return;
        }

        // Show loading indicator
        loadingDialog.show();

        mAuth.createUserWithEmailAndPassword(email, password)
                .addOnCompleteListener(this, new OnCompleteListener<AuthResult>() {
                    @Override
                    public void onComplete(@NonNull Task<AuthResult> task) {
                        // Dismiss loading indicator
                        loadingDialog.dismiss();

                        if (task.isSuccessful()) {
                            String userId = mAuth.getCurrentUser().getUid();
                            Map<String, Object> userMap = new HashMap<>();
                            userMap.put("name", name);
                            userMap.put("phone", phone);
                            userMap.put("address", address);
                            userMap.put("email", email);
                            userMap.put("card_no", "000000");
                            userMap.put("userType", "customer");

                            db.collection("users").document(userId)
                                    .set(userMap)
                                    .addOnSuccessListener(aVoid -> showCustomToast("User registered successfully!", true))
                                    .addOnFailureListener(e -> {
                                        Log.w("Firestore Error", "Error adding document", e);
                                        showCustomToast("Failed to save user info", false);
                                    });
                        } else {
                            showCustomToast("Registration failed: " + task.getException().getMessage(), false);
                        }
                    }
                });
    }

    // Helper method to show a custom toast
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